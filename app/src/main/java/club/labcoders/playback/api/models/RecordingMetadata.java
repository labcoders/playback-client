package club.labcoders.playback.api.models;

import org.joda.time.DateTime;

public class RecordingMetadata {
    private DateTime timestamp;
    private GeographicalPosition location;
    private double duration;

    public DateTime getTimestamp() {
        return timestamp;
    }

    public GeographicalPosition getLocation() {
        return location;
    }

    public double getDuration() {
        return duration;
    }
}