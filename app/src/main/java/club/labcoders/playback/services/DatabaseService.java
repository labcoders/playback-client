package club.labcoders.playback.services;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import club.labcoders.playback.api.models.ApiRecordingMetadata;
import club.labcoders.playback.db.CursorAdapter;
import club.labcoders.playback.db.DatabaseTable;
import club.labcoders.playback.db.ObservableCursor;
import club.labcoders.playback.db.SimpleInsertOperation;
import club.labcoders.playback.db.SimpleQueryOperation;
import club.labcoders.playback.db.SimpleUpdateOperation;
import club.labcoders.playback.db.models.DbAudioRecording;
import club.labcoders.playback.db.models.DbSessionToken;
import club.labcoders.playback.db.tables.RecordingTable;
import club.labcoders.playback.db.tables.SessionTable;
import rx.Observable;
import timber.log.Timber;

public class DatabaseService extends Service {
    private SQLiteDatabase db;

    private DatabaseTable[] TABLES = {
            RecordingTable.INSTANCE,
            SessionTable.INSTANCE
    };

    public DatabaseService() {
    }

    private void createTables() {
        for(final DatabaseTable table : TABLES) {
            table.create(db);
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

    public <S> Observable<S> observeSimpleQueryOperation(
            SimpleQueryOperation<S> query,
            CursorAdapter<S> adapter) {

        @SuppressLint("Recycle") // it is closed by the ObservableCursor
        final Cursor cursor = db.rawQuery(query.getQueryString(), null);
        return new ObservableCursor.Builder<S>().build(cursor).observe(adapter);
    }

    /**
     * Performs a {@link SimpleInsertOperation}, and returns the ID of the
     * inserted row.
     * @param operation The insert operation to perform.
     * @return The ID of the inserted row.
     */
    public Observable<Long> observeSimpleInsertOperation(
            SimpleInsertOperation operation
    ) {
        return Observable.just(operation.insert(db));
    }

    public Observable<Long> observeSimpleUpdateOperation(
            SimpleUpdateOperation operation
    ) {
        return Observable.just(operation.update(db));
    }

    public Observable<DbSessionToken> getToken() {
        return observeSimpleQueryOperation(
            DbSessionToken.getOperation(), DbSessionToken.getCursorAdapter()
        );
    }

    public Observable<Long> upsertToken(String token) {
        return observeSimpleUpdateOperation(
                new DbSessionToken.UpsertOperation(token))
                .doOnNext(affectedRows -> Timber.d(
                        "upsertToken: updated %d row(s) in the db.",
                        affectedRows)
                );
    }

    public Observable<DbAudioRecording> getRecordings() {
        return observeSimpleQueryOperation(
                DbAudioRecording.getOperation(),
                DbAudioRecording.getCursorAdapter()
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
