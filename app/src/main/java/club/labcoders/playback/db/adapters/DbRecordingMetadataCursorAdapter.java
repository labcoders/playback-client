package club.labcoders.playback.db.adapters;

import android.database.Cursor;

import club.labcoders.playback.db.CursorAdapter;
import club.labcoders.playback.db.models.DateTimeAdapter;
import club.labcoders.playback.db.models.DbRecordingMetadata;
import club.labcoders.playback.db.models.RecordingFormat;

public class DbRecordingMetadataCursorAdapter
        implements CursorAdapter<DbRecordingMetadata> {
    @Override
    public DbRecordingMetadata fromCursor(Cursor cursor) {
        final int remoteIdColumn = cursor.getColumnIndex("remote_id");
        final int latitudeColumn = cursor.getColumnIndex("latitude");
        final int longitudeColumn = cursor.getColumnIndex("longitude");

        final boolean remoteIdIsNull = cursor.isNull(remoteIdColumn);
        final boolean latitudeIsNull = cursor.isNull(latitudeColumn);
        final boolean longitudeIsNull = cursor.isNull(longitudeColumn);

        return new DbRecordingMetadata(
                cursor.getInt(cursor.getColumnIndex("id")),
                remoteIdIsNull ?
                        null :
                        cursor.getInt(remoteIdColumn),
                cursor.getDouble(cursor.getColumnIndex("duration")),
                latitudeIsNull ?
                        null :
                        cursor.getDouble(latitudeColumn),
                longitudeIsNull ?
                        null :
                        cursor.getDouble(longitudeColumn),
                cursor.getString(cursor.getColumnIndex("name")),
                new DateTimeAdapter().fromInteger(cursor.getLong(
                        cursor.getColumnIndex("timestamp")
                )),
                RecordingFormat.fromInteger(
                        cursor.getInt(cursor.getColumnIndex("format"))
                )
        );
    }
}
