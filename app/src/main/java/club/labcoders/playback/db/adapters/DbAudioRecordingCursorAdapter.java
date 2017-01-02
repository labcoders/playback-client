package club.labcoders.playback.db.adapters;

import android.database.Cursor;

import club.labcoders.playback.db.CursorAdapter;
import club.labcoders.playback.db.columns.ReadColumn;
import club.labcoders.playback.db.models.DbAudioRecording;
import club.labcoders.playback.db.models.RecordingFormat;
import club.labcoders.playback.db.tables.RecordingTable;

public class DbAudioRecordingCursorAdapter
        implements CursorAdapter<DbAudioRecording> {
    public static final RecordingTable TABLE = RecordingTable.INSTANCE;

    @Override
    public DbAudioRecording fromCursor(Cursor cursor) {
        return new DbAudioRecording(
                ReadColumn.Misc.get(TABLE.RECORDING, cursor),
                ReadColumn.Misc.get(TABLE.ID, cursor),
                ReadColumn.Misc.get(TABLE.DURATION, cursor),
                ReadColumn.Misc.getNullable(TABLE.LATITUDE, cursor),
                ReadColumn.Misc.getNullable(TABLE.LONGITUDE, cursor),
                ReadColumn.Misc.getNullable(TABLE.FORMAT, cursor)
        );
    }
}
