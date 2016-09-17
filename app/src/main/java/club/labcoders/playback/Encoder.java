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
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 10000);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);

        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        codec.start();

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
                if (!codecInitialized) {
                    initCodec();
                }
                if(subscriber.isUnsubscribed()) {
                    return;
                }
                int pointer = 0;
                while (pointer < bytes.length) {
                    int inIdx = -1;
                    int inc = 0;

                    while (inIdx < 0) {
                        inIdx = codec.dequeueInputBuffer(1000);
                        if (inIdx >= 0) {
                            ByteBuffer inBuffer = codec.getInputBuffer(inIdx);
                            int capacity = inBuffer.capacity();
                            inc = Math.min(bytes.length - pointer, capacity);

                            inBuffer.put(bytes, pointer, inc);
                            codec.queueInputBuffer(inIdx, 0, inc, 0, 0);
                        }
                        Timber.d("In index: " + inIdx);
                    }

                    int outIdx = -1;

                    while (outIdx < 0) {
                        outIdx = codec.dequeueOutputBuffer(new MediaCodec.BufferInfo(), 1000);
                        Timber.d("out index: " + outIdx);
                        if (outIdx >= 0) {
                            Timber.d("la");
                            ByteBuffer outBuffer = codec.getOutputBuffer(outIdx);
                            Timber.d("de");
                            subscriber.onNext(outBuffer.order(ByteOrder.nativeOrder()).array());
                            Timber.d("emitted an outBuffer");
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
                        }
                    }
                    pointer += inc;
                }
                Timber.d("Finished writing " + bytes.length + " to observer.");
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
