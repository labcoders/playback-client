package club.labcoders.playback.db.models;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import org.joda.time.DateTime;

import java.util.Arrays;

import club.labcoders.playback.api.models.ApiRecordingMetadata;
import club.labcoders.playback.data.RecordingMetadata;
import club.labcoders.playback.data.RecordingMetadataLocation;
import club.labcoders.playback.api.models.GeographicalPosition;
import club.labcoders.playback.db.CursorAdapter;
import club.labcoders.playback.db.SimpleQueryOperation;
import club.labcoders.playback.db.SimpleUpdateOperation;
import club.labcoders.playback.db.adapters.DbRecordingMetadataCursorAdapter;
import club.labcoders.playback.db.columns.Column;
import club.labcoders.playback.db.tables.RecordingTable;


public class DbRecordingMetadata implements RecordingMetadata {
    public static final RecordingTable TABLE = RecordingTable.INSTANCE;

    public static final Column[] COLUMNS = new Column[]{
            TABLE.ID,
            TABLE.REMOTE_ID,
            TABLE.LATITUDE,
            TABLE.LONGITUDE,
            TABLE.DURATION,
            TABLE.FORMAT,
            TABLE.NAME,
            TABLE.RECORDED_AT
    };

    private static final String COLUMNS_STRING
            = Column.Misc.select(Arrays.asList(COLUMNS));

    /**
     * A query string to select all recordings.
     */
    public static final String QUERY
            = "SELECT "
            + COLUMNS_STRING
            + "FROM recording";

    public static final String QUERY_NOT_UPLOADED
            = "SELECT "
            + COLUMNS_STRING
            + "FROM recording "
            + "WHERE remote_id IS NULL";

    public static final String QUERY_UPLOADED
            = "SELECT "
            + COLUMNS_STRING
            + "FROM recording "
            + "WHERE remote_id IS NOT NULL ";

    /**
     * A query string to select all recordings that we have local audio for.
     */
    public static final String QUERY_LOCAL_AUDIO
            = "SELECT "
            + COLUMNS_STRING
            + "FROM recording "
            + "WHERE recording IS NOT NULL";

    /**
     * A query string to select all recordings that are persisted remotely,
     * but for which we do not have local audio.
     */
    public static final String QUERY_REMOTE_AUDIO
            = "SELECT "
            + COLUMNS_STRING
            + "WHERE remote_id IS NOT NULL AND recording IS NULL ";

    public static CursorAdapter<DbRecordingMetadata> getCursorAdapter() {
        return new DbRecordingMetadataCursorAdapter();
    }

    public static QueryAllOperation getOperation() {
        return new QueryAllOperation();
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

    @Override
    public RecordingMetadataLocation getDataLocation() {
        return RecordingMetadataLocation.LOCAL;
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

    @Override
    public GeographicalPosition getLocation() {
        return new GeographicalPosition(latitude, longitude);
    }

    public Integer getRemoteId() {
        return remoteId;
    }

    public RecordingFormat getFormat() {
        return format;
    }

    public static class QueryAllOperation
            extends RecordingTable.RecordingTableOperation
            implements SimpleQueryOperation<DbRecordingMetadata> {
        @Override
        public String getQueryString() {
            return QUERY;
        }
    }

    public static class QueryLocalAudioOperation
            extends RecordingTable.RecordingTableOperation
            implements SimpleQueryOperation<DbRecordingMetadata> {
        @Override
        public String getQueryString() {
            return QUERY_LOCAL_AUDIO;
        }
    }

    public static class QueryRemoteAudioOperation
            extends RecordingTable.RecordingTableOperation
            implements SimpleQueryOperation<DbRecordingMetadata> {
        @Override
        public String getQueryString() {
            return QUERY_REMOTE_AUDIO;
        }
    }

    public static class QueryNotUploadedOperation
            extends RecordingTable.RecordingTableOperation
            implements SimpleQueryOperation<DbRecordingMetadata> {
        @Override
        public String getQueryString() {
            return QUERY_NOT_UPLOADED;
        }
    }

    public static class QueryUploadedOperation
            extends RecordingTable.RecordingTableOperation
            implements SimpleQueryOperation<DbRecordingMetadata> {
        @Override
        public String getQueryString() {
            return QUERY_UPLOADED;
        }
    }

    public static class RemoteUpsertOperation
            extends RecordingTable.RecordingTableOperation
            implements SimpleUpdateOperation {
        final ApiRecordingMetadata metadata;

        public RemoteUpsertOperation(ApiRecordingMetadata metadata) {
            this.metadata = metadata;
        }

        @Override
        public long update(SQLiteDatabase db) {
            final ContentValues vals = new ContentValues();
            vals.put("name", metadata.getName());
            if(metadata.getLocation() != null) {
                vals.put("latitude", metadata.getLocation().getLatitude());
                vals.put("longitude", metadata.getLocation().getLongitude());
            }
            vals.put("duration", metadata.getDuration());

            final DateTimeAdapter adapter = new DateTimeAdapter();
            vals.put(
                    "recorded_at",
                    adapter.fromDateTime(metadata.getTimestamp())
            );

            vals.put("remote_id", metadata.getRemoteId());
            vals.put("format", RecordingFormat.MP3_FORMAT.toInteger());

            return db.insertWithOnConflict(
                    getTable().getTableName(),
                    null,
                    vals,
                    SQLiteDatabase.CONFLICT_REPLACE
            );
        }
    }
}
