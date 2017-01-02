package club.labcoders.playback.db;

import android.database.sqlite.SQLiteDatabase;

public interface SimpleUpdateOperation {
    long update(SQLiteDatabase db);
}
