package club.labcoders.playback.db;

import android.database.sqlite.SQLiteDatabase;

/**
 * Created by jake on 2016-10-08.
 */
public interface DatabaseTable {
    void create(SQLiteDatabase db);
    String getSchema();
    String getTableName();
}
