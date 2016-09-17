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
public class Encoder {
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

    public byte[] encode(byte[] inputBytes) {
        if (!codecInitialized) {
            initCodec();
        }

        int pointer = 0;

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        while (pointer < inputBytes.length) {
            Timber.d("Current pointer position: " + pointer + ". Upstream byte array length: " + inputBytes.length);
            int inIdx = -1;
            int inc = 0;

            while (inIdx < 0) {
                inIdx = codec.dequeueInputBuffer(1000000);
                if (inIdx >= 0) {
                    ByteBuffer inBuffer = codec.getInputBuffer(inIdx);
                    int capacity = inBuffer.capacity();
                    inc = Math.min(inputBytes.length - pointer, capacity);

                    inBuffer.put(inputBytes, pointer, inc);
                    codec.queueInputBuffer(inIdx, 0, inc, 0, 0);
                }
                Timber.d("In index: %d", inIdx);
            }

            int outIdx = -1;

            while (outIdx < 0) {
                final MediaCodec.BufferInfo info
                        = new MediaCodec.BufferInfo();
                outIdx = codec.dequeueOutputBuffer(info, 1000000);
                Timber.d("out index: " + outIdx);
                if (outIdx >= 0) {
                    ByteBuffer outBuffer = codec.getOutputBuffer(outIdx);
                    Timber.d("Received an outBuffer.");
                    byte[] nextBuffer = new byte[outBuffer.remaining()];
                    outBuffer.get(nextBuffer);
                    Timber.d(
                            "Converted to byte array with length %d",
                            nextBuffer.length
                    );

                    try {
                        output.write(nextBuffer);
                        Timber.d("Wrote %d bytes to output byte array stream.", nextBuffer.length);
                    } catch (IOException e) {
                        Timber.e("Could not write newly encoded audio to output byte array stream.");
                        e.printStackTrace();
                    }

                    codec.releaseOutputBuffer(outIdx, 0);
                    Timber.d("Dequeued output buffer.");
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
        codec.stop();
        codec.release();
        codecInitialized = false;

        Timber.d("Completed encoding.");
        return output.toByteArray();
    }
}
