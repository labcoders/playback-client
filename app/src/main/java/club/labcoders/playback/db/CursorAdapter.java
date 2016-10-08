package club.labcoders.playback.db;

import android.database.Cursor;

/**
 * Created by jake on 2016-10-08.
 */

public interface CursorAdapter<T> {
    T fromCursor(Cursor cursor);
}
