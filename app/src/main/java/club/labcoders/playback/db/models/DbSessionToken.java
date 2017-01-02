package club.labcoders.playback.db.models;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import java.util.Arrays;

import club.labcoders.playback.data.SessionToken;
import club.labcoders.playback.db.CursorAdapter;
import club.labcoders.playback.db.DatabaseTable;
import club.labcoders.playback.db.SimpleQueryOperation;
import club.labcoders.playback.db.SimpleUpdateOperation;
import club.labcoders.playback.db.adapters.DbSessionTokenCursorAdapter;
import club.labcoders.playback.db.columns.Column;
import club.labcoders.playback.db.tables.SessionTable;
import club.labcoders.playback.db.TableOperation;

public class DbSessionToken implements SessionToken {
    public static final SessionTable TABLE = SessionTable.INSTANCE;

    public static final String QUERY
            = "SELECT "
            + Column.Misc.select(
                    Arrays.asList(new Column[] {TABLE.TOKEN})
            )
            + "FROM session "
            + "WHERE " + TABLE.ID.getName() + " = 1"
            ;

    public static Operation getOperation() {
        return new Operation();
    }

    public static CursorAdapter<DbSessionToken> getCursorAdapter() {
        return new DbSessionTokenCursorAdapter();
    }

    private final String token;

    public DbSessionToken(String token) {
        this.token = token;
    }

    @Override
    public String getToken() {
        return token;
    }

    public static class Operation
            implements TableOperation, SimpleQueryOperation<DbSessionToken> {
        @Override
        public DatabaseTable getTable() {
            return TABLE;
        }

        @Override
        public String getQueryString() {
            return QUERY;
        }
    }

    public static class UpsertOperation
            implements TableOperation, SimpleUpdateOperation {
        private final String token;

        public UpsertOperation(String token) {
            this.token = token;
        }

        @Override
        public long update(SQLiteDatabase db) {
            final ContentValues vals = new ContentValues();
            vals.put(TABLE.ID.getName(), 1);
            vals.put(TABLE.TOKEN.getName(), token);
            return db.insertWithOnConflict(
                    TABLE.getTableName(),
                    null,
                    vals,
                    db.CONFLICT_REPLACE
            );
        }

        @Override
        public DatabaseTable getTable() {
            return TABLE;
        }
    }
}
