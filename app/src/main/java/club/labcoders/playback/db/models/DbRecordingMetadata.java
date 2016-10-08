package club.labcoders.playback.db.models;

import org.joda.time.DateTime;

import club.labcoders.playback.db.CursorAdapter;
import club.labcoders.playback.db.DatabaseTable;
import club.labcoders.playback.db.SimpleQueryOperation;
import club.labcoders.playback.db.adapters.DbRecordingMetadataCursorAdapter;
import club.labcoders.playback.db.tables.RecordingTable;
import club.labcoders.playback.db.TableOperation;


public class DbRecordingMetadata {
    public static final String QUERY
            = "SELECT "
            + "id, remote_id, latitude, longitude, duration, format, name "
            + "FROM recording;";

    public static CursorAdapter<DbRecordingMetadata> getCursorAdapter() {
        return new DbRecordingMetadataCursorAdapter();
    }

    public static Operation getOperation() {
        return new Operation();
    }

    private final int id;
    private final Integer remoteId;
    private final double duration;
    private final Double latitude;
    private final Double longitude;
    private final String name;
    private final DateTime timestamp;
    private final RecordingFormat format;

    public DbRecordingMetadata(
            int id,
            Integer remoteId,
            double duration,
            Double latitude,
            Double longitude,
            String name,
            DateTime timestamp,
            RecordingFormat format) {
        this.id = id;
        this.remoteId = remoteId;
        this.duration = duration;
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
        this.timestamp = timestamp;
        this.format = format;
    }

    public int getId() {
        return id;
    }

    public double getDuration() {
        return duration;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public String getName() {
        return name;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public Integer getRemoteId() {
        return remoteId;
    }

    public RecordingFormat getFormat() {
        return format;
    }

    public static class Operation
            implements TableOperation,
            SimpleQueryOperation<DbRecordingMetadata> {
        @Override
        public String getQueryString() {
            return QUERY;
        }

        @Override
        public DatabaseTable getTable() {
            return new RecordingTable();
        }
    }
}
