package club.labcoders.playback.services;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioRecord;
import android.os.Binder;
import android.support.v4.content.ContextCompat;

import java.nio.ShortBuffer;
import java.util.concurrent.locks.ReentrantLock;

import club.labcoders.playback.audio.AudioManager;
import club.labcoders.playback.data.CircularShortBuffer;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

public class RecordingService extends Service {
    /**
     * Milliseconds between runs of the buffer GC, which removes chunks from
     * the {@link #audioBufferQueue}.
     */
    // public static final long GC_PERIOD = 1000;

    /**
     * Milliseconds between emissions by the {@link #recordingServiceStateSubject} observable.
     */
    public static final long STATE_POLL_PERIOD = 250;

    /**
     * Number of seconds in a record snapshot.
     * <p>
     * TODO: make this a configurable parameter of the application.
     */
    public static final int DEFAULT_SNAPSHOT_LENGTH = 60;

    /**
     * The queue used internally to hold buffers emitted by the
     * {@link #audioStream} observable. The internal garbage collector
     * periodically pops as many buffers as necessary from this queue in
     * order to keep the length of the queue below
     * {@link #maxAudioBufferQueueLength}.
     */
    // public final Queue<short[]> audioBufferQueue;

    /**
     * Circular buffer that we use to hold the received short
     * arrays from the microphone.
     */
    public CircularShortBuffer audioCircularBuffer;

    /**
     * A lock used for read access to the circular short buffer.
     */
    private final ReentrantLock queueLock;

    /**
     * The maximum length of the audio buffer queue, at which point it holds
     * approximately (within <50ms on most devices)
     * {@link #snapshotLengthSeconds} seconds of audio.
     */
    private int maxAudioBufferQueueLength;
    private CompositeSubscription subscriptions;

    /**
     * The size of the buffer that we fill when polling the microphone.
     * <p>
     * Remark: per the docs, this must be *smaller* than the buffer size in
     * {@link AudioManager}.
     */
    public int chunkBufferSize;

    /**
     * A hot observable connected to the microphone. This observable obtains
     * a value when the service is started. It emits buffers containing the
     * raw samples read from the microphone.
     */
    public Observable<short[]> audioStream;

    /**
     * A cold observable related to the microphone. It emits the recording
     * state of the recording service every time the state changes.
     */
    private BehaviorSubject<RecordingServiceState> recordingServiceStateSubject;

    /**
     * The duration, in seconds, of an audio snapshot created by
     * {@link #getBufferedAudio()}.
     */
    public int snapshotLengthSeconds;

    /**
     * The duration, in samples, of an audio snapshot created by
     * {@link #getBufferedAudio()}.
     */
    public int snapshotLengthSamples;

    AudioRecord audioRecord;

    /**
     * Approximates the length of the audioBufferQueue. The queue is no shorter
     * than this amount at any given time.
     */
    // private AtomicInteger audioBufferQueueLength;

    private RecordingServiceState recordingServiceState;

    public RecordingService() {
        // audioBufferQueue = new ConcurrentLinkedQueue<>();
        queueLock = new ReentrantLock(true);
        subscriptions = new CompositeSubscription();
        // audioBufferQueueLength = new AtomicInteger(0);
        recordingServiceStateSubject = BehaviorSubject.create();
        setRecordingState(RecordingServiceState.NOT_RECORDING);
    }

    private void setRecordingState(RecordingServiceState state) {
        recordingServiceState = state;
        publishCurrentState();
    }

    private void publishCurrentState() {
        recordingServiceStateSubject.onNext(recordingServiceState);
    }

    public RecordingServiceState getState() {
        return recordingServiceState;
    }

    private AudioManager getAudioManager() {
        return AudioManager.getInstance();
    }

    private boolean hasAudioPermission() {
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onCreate() {
        Timber.d("Creating recording service");
    }

    /**
     * Merges a given number of linked buffers in the audioBufferQueue into a
     * single array. New buffers may be added to the queue concurrently;
     * these buffers will not be added to the queue.
     *
     * @return A single array with the merged contents of all the linked
     * buffers in the audioBufferQueue. Returns null if the recording service
     * is not recording.
     */
    public synchronized short[] getBufferedAudio() {
        if(getState() != RecordingServiceState.RECORDING)
            return null;

        Timber.d("Starting audio buffer queue linearization.");
        ShortBuffer result = null;
        int processedShorts = 0;
        try {
            Timber.d("Locking audio buffer queue.");
            queueLock.lock();
            Timber.d("Doing pre-linearization GC.");
            // queueGc();
            final int bufCount = audioCircularBuffer.getSize();
            result = ShortBuffer.allocate(bufCount * chunkBufferSize);
            final long startTime = System.nanoTime();
            for (short[] buffer : audioCircularBuffer.toArray()) {
                result.put(buffer);
                processedShorts += buffer.length;
            }
            final long endTime = System.nanoTime();
            final long delta = (endTime - startTime) / (long) 1e3; // microseconds

            Timber.d("Audio buffer queue linearized in %d microseconds.",
                    delta);
        } finally {
            if (queueLock.isHeldByCurrentThread())
                queueLock.unlock();
        }
        final short[] finalResult = new short[processedShorts];
        System.arraycopy(
                result.array(),
                0,
                finalResult,
                0,
                finalResult.length
        );
        return finalResult;
    }

    /**
     * Gets the recording state of the underlying AudioRecord object. This
     * should generally be "true" if {@link #getState()} returns
     * {@link RecordingServiceState#RECORDING}.
     *
     * @return Whether the underlying {@link AudioRecord} object is recording.
     */
    public boolean isRecording() {
        return audioRecord != null &&
                audioRecord.getRecordingState()
                        == AudioRecord.RECORDSTATE_RECORDING;
    }

    public Observable<RecordingServiceState> observeState() {
        return recordingServiceStateSubject.asObservable();
    }

    public void initializeRecording()
            throws MissingAudioRecordPermissionException {
        if(!hasAudioPermission()) {
            stopRecording();
            setRecordingState(RecordingServiceState.MISSING_AUDIO_PERMISSION);
            throw new MissingAudioRecordPermissionException();
        }

        if(getState() == RecordingServiceState.RECORDING)
            return;

        if(audioRecord == null)
            audioRecord = getAudioManager().newAudioRecord();
    }

    /**
     * Starts recording. This causes a state transition from
     * {@link RecordingServiceState#NOT_RECORDING} to
     * {@link RecordingServiceState#RECORDING}.
     *
     * If the service is already recording, then calling this method is a no-op.
     *
     * @throws MissingAudioRecordPermissionException
     */
    public void startRecording() throws MissingAudioRecordPermissionException {
        initializeRecording();

        if(audioRecord != null
                && audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            Timber.d("Already recording; ignoring repeated request.");
            return;
        }

        Timber.d("Starting RecordingService.");

        subscriptions = new CompositeSubscription();

        snapshotLengthSeconds = DEFAULT_SNAPSHOT_LENGTH;
        snapshotLengthSamples
                = snapshotLengthSeconds * getAudioManager().getSampleRate();
        chunkBufferSize
                = getAudioManager().getBufferSize() / getAudioManager().getBytesPerSample();
        maxAudioBufferQueueLength
                = snapshotLengthSamples / chunkBufferSize;
        audioCircularBuffer = new CircularShortBuffer(maxAudioBufferQueueLength);

        Timber.d(
                "Configured RecordingService: " +
                        "snapshot length %d sec, %d samples " +
                        "chunk buffer size %d, so %d chunks max in ABQ",
                snapshotLengthSeconds,
                snapshotLengthSamples,
                chunkBufferSize,
                maxAudioBufferQueueLength
        );

        audioStream = Observable.create(subscriber -> {
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                subscriber.onError(
                        new RuntimeException(
                                "Failed to initialize AudioRecord"
                        )
                );
                return;
            }

            audioRecord.startRecording();
            long lastTime = 0;
            long thisTime = 0;
            while (audioRecord.getRecordingState()
                    == AudioRecord.RECORDSTATE_RECORDING) {
                lastTime = thisTime;
                thisTime = System.nanoTime();

                final short[] audioBuffer = new short[chunkBufferSize];
                final int readOut = audioRecord.read(
                        audioBuffer,
                        0,
                        audioBuffer.length
                );
                switch(readOut) {
                    case AudioRecord.ERROR_INVALID_OPERATION:
                        Timber.e(
                                "Failed to read: AudioRecord not initialized."
                        );
                        subscriber.onError(
                                new RuntimeException(
                                        "AudioRecord not initialized"
                                )
                        );
                        return;
                    case AudioRecord.ERROR_DEAD_OBJECT:
                        Timber.e(
                                "Failed to read: AudioRecord died."
                        );
                        subscriber.onError(
                                new RuntimeException("AudioRecord died.")
                        );
                        return;
                    case AudioRecord.ERROR_BAD_VALUE:
                        Timber.e(
                                "Failed to read: bad values."
                        );
                        subscriber.onError(
                                new RuntimeException("AudioRecord bad values")
                        );
                        return;
                    case AudioRecord.ERROR:
                        Timber.e(
                                "Failed to read: generic error."
                        );
                        subscriber.onError(
                                new RuntimeException(
                                        "AudioRecord generic error."
                                )
                        );
                        return;
                }
                if (readOut == chunkBufferSize) {
                    subscriber.onNext(audioBuffer);
                } else {
                    final short[] finalBuffer = new short[readOut];
                    System.arraycopy(audioBuffer, 0, finalBuffer, 0, readOut);
                    subscriber.onNext(finalBuffer);
                }
            }

            Timber.d("Recording stream ended.");
            subscriber.onCompleted();
        });

        final Subscription producer = audioStream
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(shorts -> {
                    // audioBufferQueue.add(shorts);
                    try {
                        queueLock.lock();
                        audioCircularBuffer.add(shorts);
                    } finally {
                        if (queueLock.isHeldByCurrentThread()) {
                            queueLock.unlock();
                        }
                    }
                    // audioBufferQueueLength.incrementAndGet();
                });
        subscriptions.add(producer);

        // final Observable<Void> gcTimer = Observable.create(subscriber -> {
        //     while (true) {
        //         subscriber.onNext(null);
        //         try {
        //             Thread.sleep(GC_PERIOD);
        //         } catch (InterruptedException e) {
        //             subscriber.onError(e);
        //             return;
        //         }
        //     }
        // });

        // subscriptions.add(
        //         gcTimer.subscribeOn(Schedulers.io())
        //                 .observeOn(Schedulers.io())
        //                 .doOnNext(aVoid -> Timber.d("GC tick."))
        //                 .subscribe(aVoid -> queueGc())
        // );

        setRecordingState(RecordingServiceState.RECORDING);
    }

    public void stopRecording() {
        if(subscriptions != null) {
            subscriptions.unsubscribe();
            subscriptions = null;
        }
        if(audioRecord != null) {
            audioRecord.release();
            audioRecord = null;
        }
        if(audioCircularBuffer != null) {
            audioCircularBuffer.reset();
        }
        setRecordingState(RecordingServiceState.NOT_RECORDING);
    }

    @Override
    public synchronized int onStartCommand(
            Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    /**
     * Perform a garbage collection of the audio buffer queue.
     * <p>
     * This will acquire the queue lock, so that other garbage collection
     * processes or linearization processes cannot be executing concurrently.
     */
    // private void queueGc() {
    //     int collected = 0;
    //     while (audioBufferQueueLength.get() >
    //             maxAudioBufferQueueLength) {
    //         try {
    //             queueLock.lock();
    //             audioBufferQueue.remove();
    //             audioBufferQueueLength.decrementAndGet();
    //             collected++;
    //         } finally {
    //             queueLock.unlock();
    //         }
    //     }
    // }

    @Override
    public void onDestroy() {
        subscriptions.unsubscribe();
        audioRecord.stop();
        audioRecord.release();
        Timber.d("RecordingService destroyed.");
    }

    @Override
    public RecordingServiceBinder onBind(Intent intent) {
        return new RecordingServiceBinder();
    }

    public void checkState() {
        if(!hasAudioPermission()) {
            setRecordingState(RecordingServiceState.MISSING_AUDIO_PERMISSION);
        }
        else {
            setRecordingState(
                    isRecording()
                            ? RecordingServiceState.RECORDING
                            : RecordingServiceState.NOT_RECORDING

            );
        }
    }

    public class RecordingServiceBinder extends Binder {
        public RecordingService getService() {
            return RecordingService.this;
        }
    }

    public static class MissingAudioRecordPermissionException
            extends Exception {
    }

    public enum RecordingServiceState {
        RECORDING,
        NOT_RECORDING,
        MISSING_AUDIO_PERMISSION,
    }
}
