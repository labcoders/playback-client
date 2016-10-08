package club.labcoders.playback.db.adapters;

import android.database.Cursor;

import club.labcoders.playback.db.CursorAdapter;
import club.labcoders.playback.db.models.DbAudioRecording;
import club.labcoders.playback.db.models.RecordingFormat;

public class DbAudioRecordingCursorAdapter
        implements CursorAdapter<DbAudioRecording> {
    @Override
    public DbAudioRecording fromCursor(Cursor cursor) {
        return new DbAudioRecording(
                cursor.getBlob(cursor.getColumnIndex("recording")),
                cursor.getInt(cursor.getColumnIndex("id")),
                cursor.getDouble(cursor.getColumnIndex("duration")),
                cursor.getDouble(cursor.getColumnIndex("latitude")),
                cursor.getDouble(cursor.getColumnIndex("longitude")),
                RecordingFormat.fromInteger(
                        cursor.getInt(cursor.getColumnIndex("format"))
                )
        );
    }
}
