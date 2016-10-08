package club.labcoders.playback.api.models;

import org.joda.time.DateTime;

/**
 * Represents recording metadata for a recording that is present in the backend.
 */
public class ApiRecordingMetadata {
    private DateTime timestamp;
    private GeographicalPosition location;
    private double duration;
    private int id;

    public DateTime getTimestamp() {
        return timestamp;
    }

    public GeographicalPosition getLocation() {
        return location;
    }

    public double getDuration() {
        return duration;
    }

    public int getID() { return id; }
}
