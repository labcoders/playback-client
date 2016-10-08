package club.labcoders.playback;

import org.joda.time.DateTime;

import club.labcoders.playback.api.models.ApiRecordingMetadata;
import club.labcoders.playback.api.models.GeographicalPosition;
import club.labcoders.playback.db.models.DbRecordingMetadata;

/**
 * Represents recording metadata abstractly.
 */
public class RecordingMetadata {
    private DateTime timestamp;
    private GeographicalPosition location;
    private double duration;
    private int id;
    private RecordingMetadataLocation dataLocation;

    public static RecordingMetadata from(
            final ApiRecordingMetadata metadata) {
        return new RecordingMetadata(
                metadata.getTimestamp(),
                metadata.getLocation(),
                metadata.getDuration(),
                metadata.getID(),
                RecordingMetadataLocation.REMOTE
        );
    }

    public static RecordingMetadata from(
            final DbRecordingMetadata metadata) {
        return new RecordingMetadata(
                metadata.getTimestamp(),
                new GeographicalPosition(
                        metadata.getLatitude(),
                        metadata.getLongitude()
                ),
                metadata.getDuration(),
                metadata.getId(),
                RecordingMetadataLocation.LOCAL
        );
    }

    protected RecordingMetadata(
            DateTime timestamp,
            GeographicalPosition location,
            double duration,
            int id,
            RecordingMetadataLocation dataLocation) {
        this.timestamp = timestamp;
        this.location = location;
        this.duration = duration;
        this.id = id;
        this.dataLocation = dataLocation;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(DateTime timestamp) {
        this.timestamp = timestamp;
    }

    public GeographicalPosition getLocation() {
        return location;
    }

    public void setLocation(GeographicalPosition location) {
        this.location = location;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public RecordingMetadataLocation getDataLocation() {
        return dataLocation;
    }

    public void setDataLocation(RecordingMetadataLocation dataLocation) {
        this.dataLocation = dataLocation;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /**
     * Determines the location of the recording whose metadata this object
     * represents.
     */
    public enum RecordingMetadataLocation {
        REMOTE,
        LOCAL,
    }
}
