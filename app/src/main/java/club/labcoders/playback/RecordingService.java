package club.labcoders.playback;

import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Binder;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

public class RecordingService extends Service {
    /**
     * Milliseconds between runs of the buffer GC, which removes chunks from
     * the {@link #audioBufferQueue}.
     */
    public static final long GC_PERIOD = 1000;

    /**
     * Milliseconds between emissions by the {@link #recordingState} observable.
     */
    public static final long STATE_POLL_PERIOD = 250;

    /**
     * Number of seconds in a record snapshot.
     * <p>
     * TODO: make this a configurable parameter of the application.
     */
    public static final int DEFAULT_CHUNK_LENGTH = 10;
    AudioRecord audioRecord;

    /**
     * Sample rate of the audio recording.
     * <p>
     * 44.1kHz 16-bit PCM is a standard supported by all Android devices.
     */
    public static final int SAMPLE_RATE = 44100;

    /**
     * The size of the audio buffers used internally by Android in our audio
     * recorder.
     */
    public final int bufferSize;

    /**
     * The size of the buffer that we fill when polling the microphone.
     * <p>
     * Remark: per the docs, this must be *smaller* than {@link #bufferSize}!
     */
    public final int chunkBufferSize;

    /**
     * A hot observable connected to the microphone. This observable obtains
     * a value when the service is started. It emits buffers containing the
     * raw samples read from the microphone.
     */
    public final Observable<short[]> audioStream;

    /**
     * A hot observable connected to the microphone. It emits the recording
     * state of the microphone every {@link #STATE_POLL_PERIOD} milliseconds.
     */
    public final Observable<Integer> recordingState;

    /**
     * The duration, in seconds, of an audio snapshot created by
     * {@link #getBufferedAudio()}.
     */
    public final int snapshotLengthSeconds;

    /**
     * The duration, in samples, of an audio snapshot created by
     * {@link #getBufferedAudio()}.
     */
    public final int snapshotLengthSamples;

    /**
     * The queue used internally to hold buffers emitted by the
     * {@link #audioStream} observable. The internal garbage collector
     * periodically pops as many buffers as necessary from this queue in
     * order to keep the length of the queue below
     * {@link #maxAudioBufferQueueLength}.
     */
    public final Queue<short[]> audioBufferQueue;

    /**
     * A lock used for read access to the queue.
     *
     * The queue is wait-free. The microphone can continue to push data onto
     * it as much as it likes, since it is the only producer for it. However,
     * there are two consumers for the queue. One consumer is the garbage
     * collection process that drops old buffers when the queue's length
     * exceeds the configured duration. Another consumer begins when the
     * linearization process occurs. Consequently, when linearization is
     * occurring, we do not want the garbage collection process to run, as
     * that process may drop some of the buffers that we have yet to linearize.
     *
     * {@see #queueGc()}
     * {@see #audioBufferQueue}
     */
    public final ReentrantLock queueLock;

    /**
     * Approximates the length of the audioBufferQueue. The queue is no shorter
     * than this amount at any given time.
     */
    private AtomicInteger audioBufferQueueLength;

    /**
     * The maximum length of the audio buffer queue, at which point it holds
     * approximately (within <50ms on most devices)
     * {@link #snapshotLengthSeconds} seconds of audio.
     */
    public final int maxAudioBufferQueueLength;

    private final CompositeSubscription subscriptions;


    public RecordingService() {
        snapshotLengthSeconds = DEFAULT_CHUNK_LENGTH;
        snapshotLengthSamples = SAMPLE_RATE * snapshotLengthSeconds;
        audioBufferQueue = new ConcurrentLinkedQueue<>();
        audioBufferQueueLength = new AtomicInteger(0);
        queueLock = new ReentrantLock(true);
        subscriptions = new CompositeSubscription();

        bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );
        chunkBufferSize = bufferSize / 2;
        maxAudioBufferQueueLength = snapshotLengthSamples / chunkBufferSize;

        audioStream = Observable.create(subscriber -> {
            initAudioRecord();

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                subscriber.onError(new RuntimeException("Failed to initialize" +
                        " AudioRecord"));
                return;
            }

            audioRecord.startRecording();
            while (audioRecord.getRecordingState() == AudioRecord
                    .RECORDSTATE_RECORDING) {
                short[] audioBuffer = new short[bufferSize / 2];
                audioRecord.read(audioBuffer, 0, audioBuffer.length);
                subscriber.onNext(audioBuffer);
            }

            subscriber.onCompleted();
        });

        recordingState = Observable.create(subscriber -> {
            while (audioRecord.getState() != AudioRecord.STATE_UNINITIALIZED) {
                subscriber.onNext(audioRecord.getRecordingState());
                try {
                    Thread.sleep(STATE_POLL_PERIOD);
                } catch (InterruptedException e) {
                    subscriber.onError(e);
                }
            }
            Timber.d("Recording state monitor died.");
            subscriber.onCompleted();
        });
    }

    /**
     * Merges a given number of linked buffers in the audioBufferQueue into a
     * single array. New buffers may be added to the queue concurrently;
     * these buffers will not be added to the queue.
     *
     * @return A single array with the merged contents of all the linked
     * buffers in the audioBufferQueue.
     */
    private short[] linearizeQueue(final int maxMergedBuffers) {
        short[] result = new short[maxMergedBuffers * chunkBufferSize];
        int processedBufferCount = 0;
        Timber.d("Starting audio buffer queue linearization.");
        try {
            Timber.d("Locking audio buffer queue.");
            queueLock.lock();
            final long startTime = System.nanoTime();
            Timber.d("Doing pre-linearization GC.");
            queueGc();
            for (short[] buffer : audioBufferQueue) {
                if (processedBufferCount == maxMergedBuffers)
                    break;
                final int writeOffset = processedBufferCount * chunkBufferSize;
                System.arraycopy(buffer, 0, result, writeOffset,
                        chunkBufferSize);
            }
            final long endTime = System.nanoTime();
            final long delta = (endTime - startTime) / (long) 1e3; // microseconds

            Timber.d("Audio buffer queue linearized in %d microseconds.",
                    delta);
        } finally {
            if (queueLock.isHeldByCurrentThread())
                queueLock.unlock();
        }
        return result;
    }

    /**
     * Returns a buffer of the last {@link #snapshotLengthSeconds} seconds of
     * audio.
     *
     * @return The recorded audio, if any.
     */
    public short[] getBufferedAudio() {
        // snapshotLengthSamples is the number of samples that make up a
        // piece of
        // audio with length snapshotLengthSeconds; dividing by chunkBufferSize
        // gives us the number of buffers we need to pop to get a sample
        // array whose represented audio has that length in seconds.
        return linearizeQueue(maxAudioBufferQueueLength);
    }

    /**
     * Initializes the {@link AudioRecord} object internal to the
     * RecordingService.
     */
    private void initAudioRecord() {
        if (audioRecord == null)
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
            );
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final Subscription producer = audioStream
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(shorts -> {
                    audioBufferQueue.add(shorts);
                    final int count = audioBufferQueueLength.incrementAndGet();
                });
        subscriptions.add(producer);

        final Observable<Void> gcTimer = Observable.create(subscriber -> {
            while (true) {
                subscriber.onNext(null);
                try {
                    Thread.sleep(GC_PERIOD);
                } catch (InterruptedException e) {
                    subscriber.onError(e);
                    return;
                }
            }
        });
        subscriptions.add(
                gcTimer.subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe(aVoid -> queueGc())
        );
        return START_STICKY;
    }

    /**
     * Perform a garbage collection of the audio buffer queue.
     * <p>
     * This will acquire the queue lock, so that other garbage collection
     * processes or linearization processes cannot be executing concurrently.
     */
    private void queueGc() {
        int collected = 0;
        while (audioBufferQueueLength.get() >
                maxAudioBufferQueueLength) {
            try {
                queueLock.lock();
                audioBufferQueue.remove();
                audioBufferQueueLength.decrementAndGet();
                collected++;
            } finally {
                queueLock.unlock();
            }
        }
        if (collected > 0) {
            Timber.d("Garbage collected %d chunks.",
                    collected);
        }
    }

    @Override
    public void onDestroy() {
        subscriptions.unsubscribe();
        audioRecord.stop();
        audioRecord.release();
    }

    @Override
    public RecordingServiceBinder onBind(Intent intent) {
        return new RecordingServiceBinder();
    }

    class RecordingServiceBinder extends Binder {
        RecordingService getService() {
            return RecordingService.this;
        }
    }
}
