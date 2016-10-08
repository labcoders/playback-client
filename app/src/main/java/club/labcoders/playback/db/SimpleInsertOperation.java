package club.labcoders.playback.db;

import android.database.sqlite.SQLiteDatabase;

public interface SimpleInsertOperation {
    long insert(SQLiteDatabase db);
}
