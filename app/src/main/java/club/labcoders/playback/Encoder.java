package club.labcoders.playback;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.nio.ByteBuffer;

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

    public Encoder(final MediaCodec configuredCodec) {
        codec = configuredCodec;
        finished = false;
    }

    @Override
    public Subscriber<? super byte[]>
    call(Subscriber<? super byte[]> subscriber) {
        MediaCodec.Callback callback;
        final CVar<byte[]> encodeIn = new CVar<>();
        final CVar<byte[]> encodeOut = new CVar<>();

        final Subscriber<byte[]> sub =
                new Subscriber<byte[]>(subscriber) {
                    @Override
                    public void onCompleted() {
                        if(!subscriber.isUnsubscribed()) {
                            subscriber.onCompleted();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        if(!subscriber.isUnsubscribed()) {
                            subscriber.onError(e);
                        }
                    }

                    @Override
                    public void onNext(byte[] byteBuffer) {
                        byte[] encoded = null;
                        if(subscriber.isUnsubscribed())
                            return;
                        try {
                            encodeIn.write(byteBuffer);
                            encoded = encodeOut.read();
                        } catch (InterruptedException e) {
                            onError(e);
                        }
                        subscriber.onNext(encoded);
                    }
                };

        callback = new MediaCodec.Callback() {
            @Override
            public void onInputBufferAvailable(
                    MediaCodec codec, int index) {
                final ByteBuffer inputBuffer = codec.getInputBuffer(index);
                int flags = finished ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0;
                try {
                    final byte[] raw = encodeIn.read();
                    inputBuffer.put(raw);
                    codec.queueInputBuffer(index, 0, raw.length, 0, 0);
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
                final
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

        return sub;
    }
}
