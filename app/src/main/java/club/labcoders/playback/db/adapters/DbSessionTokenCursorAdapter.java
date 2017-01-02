package club.labcoders.playback.db.adapters;

import android.database.Cursor;

import club.labcoders.playback.db.CursorAdapter;
import club.labcoders.playback.db.columns.ReadColumn;
import club.labcoders.playback.db.models.DbSessionToken;
import club.labcoders.playback.db.tables.SessionTable;

public class DbSessionTokenCursorAdapter
        implements CursorAdapter<DbSessionToken> {
    private static final SessionTable TABLE = SessionTable.INSTANCE;

    @Override
    public DbSessionToken fromCursor(Cursor cursor) {
        return new DbSessionToken(
                ReadColumn.Misc.get(TABLE.TOKEN, cursor)
        );
    }
}
