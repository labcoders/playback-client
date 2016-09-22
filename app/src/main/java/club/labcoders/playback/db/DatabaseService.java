package club.labcoders.playback.db;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;

import club.labcoders.playback.db.models.AudioRecordingDb;
import rx.Observable;
import timber.log.Timber;

public class DatabaseService extends Service {
    private SQLiteDatabase db;

    private String[] SCHEMAS = {
            "CREATE TABLE IF NOT EXISTS session (" +
                    "id INTEGER PRIMARY KEY, " +
                    "token VARCHAR " +
                    ");",
            "CREATE TABLE IF NOT EXISTS recording (" +
                    "id INTEGER PRIMARY KEY, " +
                    "recording BLOB NOT NULL, " +
                    "latitude REAL, " +
                    "longitude REAL, " +
                    "duration REAL NOT NULL, " +
                    "recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                    ");",
    };

    public DatabaseService() {
    }

    private void createTables() {
        for(final String schema : SCHEMAS) {
            db.execSQL(schema);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        db = openOrCreateDatabase("playback", Context.MODE_PRIVATE, null);
        createTables();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Retrieves the token from the database, if any.
     * @return The token, or null if there is none.
     */
    public Observable<String> getToken() {
        return Observable.create(
                subscriber -> {
                    try (
                            final Cursor cur = db.rawQuery(
                                    "select token from session where id = 1;",
                                    null
                            )

                    ) {
                        if(cur.moveToNext()) {
                            subscriber.onNext(
                                    cur.getString(cur.getColumnIndex("token"))
                            );
                        }
                        else {
                            subscriber.onNext(null);
                        }
                        subscriber.onCompleted();
                    }
                }
        );
    }

    public void upsertToken(String token) {
        ContentValues vals = new ContentValues(1);
        vals.put("id", 1);
        vals.put("token", token);
        final long affectedRows
                = db.insertWithOnConflict("session", null, vals, SQLiteDatabase.CONFLICT_REPLACE);
        Timber.d("upsertToken: updated %d row(s) in the db.", affectedRows);
    }

    public Observable<AudioRecordingDb> getRecordings() {
        final String q = AudioRecordingDb.QUERY;
        return Observable.create(
                subscriber -> {
                    try(final Cursor cur = db.rawQuery(q, null)) {
                        while(cur.moveToNext()) {
                            subscriber.onNext(
                                    AudioRecordingDb.fromCursor(cur)
                            );
                        }
                        subscriber.onCompleted();
                    }
                }
        );
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new DatabaseServiceBinder();
    }

    public class DatabaseServiceBinder extends Binder {
        public DatabaseService getService() {
            return DatabaseService.this;
        }
    }
}
