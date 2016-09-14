package club.labcoders.playback;

public class AudioRecording {
    public double recordedAt;
    public double recordingLength;
    public byte[] recording;
    public AudioRecording(double ts, double length, byte[] rec) {
        recordedAt = ts;
        recordingLength = length;
        recording = rec;
    }
}