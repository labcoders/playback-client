package club.labcoders.playback.db.adapters;

import android.database.Cursor;

import club.labcoders.playback.db.CursorAdapter;
import club.labcoders.playback.db.columns.ReadColumn;
import club.labcoders.playback.db.models.DbRecordingMetadata;
import club.labcoders.playback.db.tables.RecordingTable;

public class DbRecordingMetadataCursorAdapter
        implements CursorAdapter<DbRecordingMetadata> {
    private final static RecordingTable table = RecordingTable.INSTANCE;

    @Override
    public DbRecordingMetadata fromCursor(Cursor cursor) {
        return new DbRecordingMetadata(
                ReadColumn.Misc.get(table.ID, cursor),
                ReadColumn.Misc.getNullable(table.REMOTE_ID, cursor),
                ReadColumn.Misc.get(table.DURATION, cursor),
                ReadColumn.Misc.getNullable(table.LATITUDE, cursor),
                ReadColumn.Misc.getNullable(table.LONGITUDE, cursor),
                ReadColumn.Misc.get(table.NAME, cursor),
                ReadColumn.Misc.get(table.RECORDED_AT, cursor),
                ReadColumn.Misc.get(table.FORMAT, cursor)
        );
    }
}
