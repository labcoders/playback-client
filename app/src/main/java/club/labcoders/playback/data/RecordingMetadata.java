package club.labcoders.playback.data;

import org.joda.time.DateTime;

import club.labcoders.playback.api.models.GeographicalPosition;

/**
 * Represents recording metadata abstractly.
 */
public interface RecordingMetadata {
    DateTime getTimestamp();
    GeographicalPosition getLocation();
    double getDuration();
    RecordingMetadataLocation getDataLocation();
    int getId();
    String getName();
}
