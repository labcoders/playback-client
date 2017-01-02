package club.labcoders.playback.db.columns;

import android.database.Cursor;

public abstract class IntColumn<J> implements ReadColumn<Integer, J> {
    @Override
    public Integer fromRow(Cursor cursor) {
        return cursor.getInt(
                Column.Misc.safeGetColumnIndex(cursor, getName())
        );
    }
}
