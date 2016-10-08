package club.labcoders.playback.api.models;

import org.joda.time.DateTime;

public class ApiAudioRecording {
    private DateTime timestamp;
    private double duration;
    private Base64Blob recording;
    private GeographicalPosition location;
    private String name;

    public ApiAudioRecording(DateTime timestamp, double duration, Base64Blob recording, GeographicalPosition location, String name) {
        this.timestamp = timestamp;
        this.duration = duration;
        this.recording = recording;
        this.location = location;
        this.name = name;
    }

    public ApiAudioRecording(DateTime timestamp, double duration, Base64Blob recording, String name) {
        this.timestamp = timestamp;
        this.recording = recording;
        this.duration = duration;
        this.name = name;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(DateTime timestamp) {
        this.timestamp = timestamp;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public Base64Blob getRecording() {
        return recording;
    }

    public void setRecording(Base64Blob recording) {
        this.recording = recording;
    }

    public GeographicalPosition getLocation() {
        return location;
    }

    public void setLocation(GeographicalPosition location) {
        this.location = location;
    }
}