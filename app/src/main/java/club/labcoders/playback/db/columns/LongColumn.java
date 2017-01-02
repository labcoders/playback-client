package club.labcoders.playback.db.columns;

import android.database.Cursor;

/**
 * Created by jake on 2016-10-11.
 */

public abstract class LongColumn<J> implements ReadColumn<Long, J> {
    @Override
    public Long fromRow(Cursor cursor) {
        return cursor.getLong(
                Column.Misc.safeGetColumnIndex(cursor, getName())
        );
    }
}
