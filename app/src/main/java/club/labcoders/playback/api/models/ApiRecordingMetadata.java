package club.labcoders.playback.api.models;

import org.joda.time.DateTime;

import club.labcoders.playback.data.RecordingMetadata;
import club.labcoders.playback.data.RecordingMetadataLocation;

/**
 * Represents recording metadata for a recording that is present in the backend.
 */
public class ApiRecordingMetadata implements RecordingMetadata {
    private final DateTime timestamp;
    private final GeographicalPosition location;
    private final double duration;
    private final int id;
    private final String name;
    private int remoteId;

    public ApiRecordingMetadata(DateTime timestamp, GeographicalPosition
            location, double duration, int id, String name) {
        this.timestamp = timestamp;
        this.location = location;
        this.duration = duration;
        this.id = id;
        this.name = name;
    }

    @Override
    public DateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public GeographicalPosition getLocation() {
        return location;
    }

    @Override
    public double getDuration() {
        return duration;
    }

    @Override
    public RecordingMetadataLocation getDataLocation() {
        return RecordingMetadataLocation.REMOTE;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public int getRemoteId() {
        return remoteId;
    }
}
