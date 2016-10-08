package club.labcoders.playback.db.adapters;

import android.database.Cursor;

import club.labcoders.playback.db.CursorAdapter;
import club.labcoders.playback.db.models.DbSessionToken;

public class DbSessionTokenCursorAdapter
        implements CursorAdapter<DbSessionToken> {
    @Override
    public DbSessionToken fromCursor(Cursor cursor) {
        return new DbSessionToken(
                cursor.getString(cursor.getColumnIndex("token"))
        );
    }
}
