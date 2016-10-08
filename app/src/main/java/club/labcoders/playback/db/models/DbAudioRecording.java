package club.labcoders.playback.db.models;

import android.content.ContentValues;

import club.labcoders.playback.db.TrivialInsertOperation;
import club.labcoders.playback.db.CursorAdapter;
import club.labcoders.playback.db.DatabaseTable;
import club.labcoders.playback.db.SimpleQueryOperation;
import club.labcoders.playback.db.adapters.DbAudioRecordingCursorAdapter;
import club.labcoders.playback.db.tables.RecordingTable;
import club.labcoders.playback.db.TableOperation;


public class DbAudioRecording {
    public static final String QUERY
            = "SELECT id, recording, latitude, longitude, duration, format "
            + "FROM recording;";

    public static Operation getOperation() {
        return new Operation();
    }

    public static CursorAdapter<DbAudioRecording> getCursorAdapter() {
        return new DbAudioRecordingCursorAdapter();
    }

    private final byte[] recording;
    private final int id;
    private final double duration;
    private final Double latitude;
    private final Double longitude;
    private final RecordingFormat format;

    public DbAudioRecording(
            byte[] recording,
            int id,
            double duration,
            Double latitude,
            Double longitude,
            RecordingFormat format) {
        this.recording = recording;
        this.id = id;
        this.duration = duration;
        this.latitude = latitude;
        this.longitude = longitude;
        this.format = format;
    }

    public byte[] getRecording() {
        return recording;
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

    public RecordingFormat getFormat() {
        return format;
    }

    public static class Operation
            implements TableOperation, SimpleQueryOperation<DbAudioRecording> {

        @Override
        public String getQueryString() {
            return QUERY;
        }

        @Override
        public DatabaseTable getTable() {
            return new RecordingTable();
        }
    }

    public static class InsertOperation
            extends TrivialInsertOperation
            implements TableOperation {

        public InsertOperation(ContentValues values) {
            super("recording", values); // feels bad, man
        }

        @Override
        public DatabaseTable getTable() {
            return new RecordingTable();
        }
    }

    public static class InsertOperationBuilder
            implements TableOperation {
        private byte[] recording;
        private Double duration;
        private Double latitude;
        private Double longitude;
        private RecordingFormat format;
        private String name;
        private Integer remoteId;

        @Override
        public DatabaseTable getTable() {
            return new RecordingTable();
        }

        public InsertOperation build() {
            final ContentValues values = new ContentValues();
            values.put("name", name);
            values.put("format", format.toInteger());
            values.put("longitude", longitude);
            values.put("latitude", latitude);
            values.put("duration", duration);
            values.put("recording", recording);
            values.put("remote_id", remoteId);
            return new InsertOperation(values);
        }

        public InsertOperationBuilder setRecording(byte[] recording) {
            this.recording = recording;
            return this;
        }

        public InsertOperationBuilder setDuration(Double duration) {
            this.duration = duration;
            return this;
        }

        public InsertOperationBuilder setLatitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public InsertOperationBuilder setLongitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public InsertOperationBuilder setFormat(RecordingFormat format) {
            this.format = format;
            return this;
        }

        public InsertOperationBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public InsertOperationBuilder setRemoteId(Integer remoteId) {
            this.remoteId = remoteId;
            return this;
        }
    }
}
