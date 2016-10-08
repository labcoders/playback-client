package club.labcoders.playback.db;

import android.database.Cursor;

import rx.Observable;

/**
 * Creates an obervable that emits the results of a database query, captured
 * in a cursor.
 * @param <T> The type of data emitted by the cursor
 */
public class ObservableCursor<T> {
    private final Cursor cursor;
    private final boolean cleanup;

    /**
     * Initialize the observable cursor with a given cursor, and to
     * perform cursor cleanup on both error and success.
     * @param cursor The cursor whose data will be iterated
     */
    public ObservableCursor(final Cursor cursor) {
        this.cursor = cursor;
        this.cleanup = true;
    }

    /**
     * Initialize the observable cursor with a given cursor, and to
     * perform cleanup on both error and success according to the given boolean.
     * on both error
     * @param cursor The cursor whose data will be iterated
     * @param cleanup Whether to cleanup on error and success
     */
    public ObservableCursor(final Cursor cursor, boolean cleanup) {
        this.cursor = cursor;
        this.cleanup = cleanup;
    }

    public <S extends CursorAdapter<T>> Observable<T> observe(S adapter) {
        return Observable.create(subscriber -> {
            try {
                while (cursor.moveToNext()) {
                    subscriber.onNext(adapter.fromCursor(cursor));
                }
            }
            finally {
                if(cleanup)
                    cursor.close();
            }
            subscriber.onCompleted();
        });
    }
}
