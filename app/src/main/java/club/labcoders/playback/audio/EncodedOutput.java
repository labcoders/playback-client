package club.labcoders.playback.audio;

import android.media.MediaCodec;

import java.nio.ByteBuffer;

public class EncodedOutput {
    public byte[] byteArray;
    public MediaCodec.BufferInfo bufferInfo;

    public EncodedOutput(byte[] arr, MediaCodec.BufferInfo bufferInfo) {
        this.byteArray = arr;
        this.bufferInfo = bufferInfo;
    }
}
