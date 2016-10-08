package club.labcoders.playback.db.tables;

import android.database.sqlite.SQLiteDatabase;

import club.labcoders.playback.db.DatabaseTable;

public class RecordingTable implements DatabaseTable {
    public static final String TABLE_NAME = "recording";

    public static final String SCHEMA
            = "CREATE TABLE IF NOT EXISTS recording ("
            + "id INTEGER PRIMARY KEY, "
            + "remote_id INTEGER, "
            + "recording BLOB NOT NULL, "
            + "latitude REAL, "
            + "longitude REAL, "
            + "duration REAL NOT NULL, "
            + "format INTEGER NOT NULL, "
            + "name VARCHAR, "
            + "recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP);";

    public String getSchema() {
        return SCHEMA;
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    public void create(final SQLiteDatabase db) {
        db.execSQL(SCHEMA);
    }
}
