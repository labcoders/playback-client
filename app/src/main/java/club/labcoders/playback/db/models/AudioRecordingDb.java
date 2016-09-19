package club.labcoders.playback.db.models;

import android.database.Cursor;

/**
 * Created by jake on 2016-09-18.
 */

public class AudioRecordingDb {
    private final byte[] recording;
    private final int id;
    private final double duration;
    private final Double latitude;
    private final Double longitude;

    public static final String QUERY
            = "SELECT id, recording, latitude, longitude, duration "
            + "FROM recording;";

    public static AudioRecordingDb fromCursor(final Cursor cursor) {
        return new AudioRecordingDb(
                cursor.getBlob(cursor.getColumnIndex("recording")),
                cursor.getInt(cursor.getColumnIndex("id")),
                cursor.getDouble(cursor.getColumnIndex("duration")),
                cursor.getDouble(cursor.getColumnIndex("latitude")),
                cursor.getDouble(cursor.getColumnIndex("longitude"))
        );
    }

    public AudioRecordingDb(
            byte[] recording,
            int id,
            double duration,
            Double latitude,
            Double longitude) {
        this.recording = recording;
        this.id = id;
        this.duration = duration;
        this.latitude = latitude;
        this.longitude = longitude;
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
}
