package club.labcoders.playback;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Queue;

import club.labcoders.playback.concurrent.CVar;
import rx.Observable;
import rx.Subscriber;
import timber.log.Timber;

/**
 * Class for wrapping Android's built-in encoding facilities with RxJava.
 */
public class Encoder implements Observable.Operator<byte[], byte[]> {
    final MediaCodec codec;

    boolean hasError = false;
    Throwable error = null;

    private boolean finished;
    private boolean codecStarted;

    public Encoder(final MediaCodec configuredCodec) {
        codec = configuredCodec;
        finished = false;
        codecStarted = false;
    }

    public class ExposedSubscriber  extends Subscriber<byte[]> {
        private Subscriber<? super byte[]> subscriber;
        private CVar<Integer> bufferIndex;

        public ExposedSubscriber(Subscriber<? super byte[]> sub, CVar<Integer> cvar) {
            subscriber = sub;
            bufferIndex = cvar;
        }

        public Subscriber<? super byte[]> getSubscriber() {
            return subscriber;
        }

        @Override
        public void onCompleted() {
            try {
                int idx = bufferIndex.read();
                codec.queueInputBuffer(idx, 0, 0, 0, codec.BUFFER_FLAG_END_OF_STREAM);
            } catch (InterruptedException e) {
                onError(e);
            }
        }

        @Override
        public void onError(Throwable e) {
            if(!subscriber.isUnsubscribed()) {
                subscriber.onError(e);
            }
        }

        @Override
        public void onNext(byte[] bytes) {
            if (!codecStarted) {
                codec.start();
                codecStarted = true;
            }
            byte[] encoded = null;
            if(subscriber.isUnsubscribed()) {
                return;
            }
            try {
                int pointer = 0;
                while (pointer < bytes.length) {
                    int idx = bufferIndex.read();
                    ByteBuffer buffer = codec.getInputBuffer(idx);
                    int capacity = buffer.capacity();
                    int inc = Math.min(bytes.length - pointer, capacity);

                    buffer.put(bytes, pointer, inc);
                    codec.queueInputBuffer(idx, 0, inc, 0, 0);

                    pointer += inc;
                }
            } catch (InterruptedException e) {
                onError(e);
            }
        }
    }

    @Override
    public Subscriber<? super byte[]>
    call(Subscriber<? super byte[]> subscriber) {
        MediaCodec.Callback callback;
        final CVar<Integer> bufferIndex = new CVar<Integer>();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        ExposedSubscriber exp = new ExposedSubscriber(subscriber, bufferIndex);

        callback = new MediaCodec.Callback() {
            @Override
            public void onInputBufferAvailable(
                    MediaCodec codec, int index) {
                try {
                    bufferIndex.write(index);
                }
                catch(InterruptedException e) {
                    Timber.e("HOLY SHIT");
                    hasError = true;
                    error = e;
                }
            }

            @Override
            public void onOutputBufferAvailable(
                    MediaCodec codec,
                    int index,
                    MediaCodec.BufferInfo info) {
                if (0 != (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM)) {
                    exp.getSubscriber().onNext(codec.getOutputBuffer(index).array());
                    exp.getSubscriber().onCompleted();
                    codec.stop();
                } else {
                    exp.getSubscriber().onNext(codec.getOutputBuffer(index).array());
                }
            }

            @Override
            public void onError(
                    MediaCodec codec,
                    MediaCodec.CodecException e) {

            }

            @Override
            public void onOutputFormatChanged(
                    MediaCodec codec,
                    MediaFormat format) {

            }
        };

        codec.setCallback(callback);

        return exp;
    }
}
