package club.labcoders.playback.db.models;

import club.labcoders.playback.db.CursorAdapter;
import club.labcoders.playback.db.DatabaseTable;
import club.labcoders.playback.db.SimpleQueryOperation;
import club.labcoders.playback.db.adapters.DbSessionTokenCursorAdapter;
import club.labcoders.playback.db.tables.SessionTable;
import club.labcoders.playback.db.TableOperation;

public class DbSessionToken {
    public static final String QUERY
            = "SELECT token FROM session WHERE id = 1";

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

    public String getToken() {
        return token;
    }

    public static class Operation
            implements TableOperation, SimpleQueryOperation<DbSessionToken> {
        @Override
        public DatabaseTable getTable() {
            return new SessionTable();
        }

        @Override
        public String getQueryString() {
            return QUERY;
        }
    }
}
