package club.labcoders.playback.db.tables;

import android.database.sqlite.SQLiteDatabase;

import club.labcoders.playback.db.DatabaseTable;

public class SessionTable implements DatabaseTable {
    public static final String TABLE_NAME = "session";

    public static final String SCHEMA
            = "CREATE TABLE IF NOT EXISTS session ("
            + "id INTEGER PRIMARY KEY, "
            + "token VARCHAR "
            + ");";

    public String getSchema() {
        return SCHEMA;
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public void create(SQLiteDatabase db) {
        db.execSQL(SCHEMA);
    }
}
