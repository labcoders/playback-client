package club.labcoders.playback;

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import club.labcoders.playback.concurrent.CVar;
import rx.Observable;
import rx.Subscriber;
import timber.log.Timber;

/**
 * Class for wrapping Android's built-in encoding facilities with RxJava.
 */
public class Encoder implements Observable.Operator<byte[], byte[]> {
    MediaCodec codec;

    boolean hasError = false;
    Throwable error = null;

    private boolean codecInitialized;

    public final static String MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC;

    public Encoder() {
        codecInitialized = false;
        Timber.d("Created Encoder.");
    }

    private void initCodec() {
        try {
            codec = MediaCodec.createEncoderByType(MIME_TYPE);
        } catch (IOException e) {
            Timber.e("Could not create MediaCodec with mimetype: " + MIME_TYPE);
        }

        MediaFormat format = MediaFormat.createAudioFormat(MIME_TYPE, RecordingService.SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 128000);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 1000000);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);

        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        codec.start();

        Timber.d("Initialized codec.");
        codecInitialized = true;
    }

    @Override
    public Subscriber<? super byte[]>
    call(Subscriber<? super byte[]> subscriber) {
        MediaCodec.Callback callback;
        final CVar<Integer> bufferIndex = new CVar<Integer>();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        Subscriber sub = new Subscriber<byte[]>(subscriber) {
            @Override
            public void onCompleted() {
                Timber.d("onComplete upstream to encoder called.");
                codec.stop();
                codec.release();
                codecInitialized = false;
                subscriber.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                if(!subscriber.isUnsubscribed()) {
                    subscriber.onError(e);
                }
            }

            @Override
            public void onNext(byte[] bytes) {
                if(subscriber.isUnsubscribed()) {
                    return;
                }
                if (!codecInitialized) {
                    initCodec();
                }
                int pointer = 0;
                int finalSize = 0;
                while (pointer < bytes.length) {
                    int inIdx = -1;
                    int inc = 0;

                    int getInputBufferTries = 3;

                    while (inIdx < 0 && getInputBufferTries > 0) {
                        inIdx = codec.dequeueInputBuffer(1000);
                        if (inIdx >= 0) {
                            ByteBuffer inBuffer = codec.getInputBuffer(inIdx);
                            int capacity = inBuffer.capacity();

                            // inc is number of bytes that we are going
                            // to write into the inputBuffer
                            inc = Math.min(bytes.length - pointer, capacity);

                            inBuffer.put(bytes, pointer, inc);
                            pointer += inc;
                            codec.queueInputBuffer(inIdx, 0, inc, 0, 0);
                        }
                        getInputBufferTries -= 1;
                    }

                    int outIdx;

                    int getOutputBufferTries = 3;
                    while (getOutputBufferTries > 0) {
                        final MediaCodec.BufferInfo info
                                = new MediaCodec.BufferInfo();
                        outIdx = codec.dequeueOutputBuffer(info, 1000);
                        if (outIdx >= 0) {
                            ByteBuffer outBuffer = codec.getOutputBuffer(outIdx);
                            finalSize += outBuffer.remaining();
                            byte[] nextBuffer = new byte[outBuffer.remaining()];
                            outBuffer.get(nextBuffer);
                            subscriber.onNext(nextBuffer);
                            codec.releaseOutputBuffer(outIdx, 0);
                        } else {
                            switch (outIdx) {
                                case -1:
                                    Timber.e("Timed out while waiting for output buffer.");
                                    break;
                                case -2:
                                    Timber.d("Output format changed");
                                    break;
                                case -3:
                                    Timber.d("Output buffers changed (DEPRECATED).");
                                    break;
                                default:
                                    Timber.e("THE END IS NIGH");
                            }
                            getOutputBufferTries -= 1;
                        }
                    }
                }
                Timber.d("Encoded %d bytes, and wrote %d bytes to observer.", bytes.length, finalSize);
            }
        };

//        callback = new MediaCodec.Callback() {
//            @Override
//            public void onInputBufferAvailable(
//                    MediaCodec codec, int index) {
//                try {
//                    Timber.d("Codec thread ID: " + String.valueOf(Thread.currentThread()));
//                    bufferIndex.write(index);
//                }
//                catch(InterruptedException e) {
//                    Timber.e("HOLY SHIT");
//                    hasError = true;
//                    error = e;
//                }
//            }
//
//            @Override
//            public void onOutputBufferAvailable(
//                    MediaCodec codec,
//                    int index,
//                    MediaCodec.BufferInfo info) {
//                if (0 != (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM)) {
//                    subscriber.onNext(codec.getOutputBuffer(index).array());
//                    subscriber.onCompleted();
//                    codec.stop();
//                } else {
//                    subscriber.onNext(codec.getOutputBuffer(index).array());
//                }
//            }
//
//            @Override
//            public void onError(
//                    MediaCodec codec,
//                    MediaCodec.CodecException e) {
//
//            }
//
//            @Override
//            public void onOutputFormatChanged(
//                    MediaCodec codec,
//                    MediaFormat format) {
//
//            }
//        };
//
//        codec.setCallback(callback);

        return sub;
    }
}
