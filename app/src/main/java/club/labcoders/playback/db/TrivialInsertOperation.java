package club.labcoders.playback.db;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

public class TrivialInsertOperation implements SimpleInsertOperation {
    private final String tableName;
    private final ContentValues values;

    public TrivialInsertOperation(String tableName, ContentValues values) {
        this.tableName = tableName;
        this.values = values;
    }

    @Override
    public long insert(SQLiteDatabase db) {
        return db.insert(tableName, null, values);
    }
}