package club.labcoders.playback.db.tables;

import android.database.sqlite.SQLiteDatabase;

import java.util.Arrays;
import java.util.List;

import club.labcoders.playback.db.DatabaseTable;
import club.labcoders.playback.db.columns.Column;
import club.labcoders.playback.db.columns.RawIntColumn;
import club.labcoders.playback.db.columns.ReadColumn;
import club.labcoders.playback.db.columns.StringColumn;

public class SessionTable implements DatabaseTable {
    public static final SessionTable INSTANCE = new SessionTable();

    private SessionTable() {

    }

    public final String TABLE_NAME = "session";

    public final String SCHEMA
            = "CREATE TABLE IF NOT EXISTS session ("
            + "_id INTEGER PRIMARY KEY, "
            + "token VARCHAR "
            + ");";

    public final ReadColumn<Integer, Integer> ID
            = new RawIntColumn.Named("_id");

    public final ReadColumn<String, String> TOKEN
            = new StringColumn.Named("token");

    public List<Column> columns
            = Arrays.asList(ID, TOKEN);

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
