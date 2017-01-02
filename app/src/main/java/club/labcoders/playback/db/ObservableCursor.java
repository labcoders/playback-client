package club.labcoders.playback.db;

import android.database.Cursor;

import rx.Observable;
import timber.log.Timber;

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
    public ObservableCursor(
            Cursor cursor,
            boolean cleanup) {
        this.cursor = cursor;
        this.cleanup = cleanup;
    }

    public <S extends CursorAdapter<T>> Observable<T> observe(S adapter) {
        return Observable.create(subscriber -> {
            try {
                while (cursor.moveToNext()) {
                    Timber.d("next item");
                    final T item = adapter.fromCursor(cursor);
                    subscriber.onNext(item);
                }
            }
            finally {
                if(cleanup)
                    cursor.close();
            }

            Timber.d("complete");
            subscriber.onCompleted();
        });
    }

    public static class Builder<T> {
        private boolean cleanup;

        public Builder() {
            cleanup = true;
        }

        public Builder setCleanup(boolean cleanup) {
            this.cleanup = cleanup;
            return this;
        }

        public ObservableCursor<T> build(Cursor cursor) {
            return new ObservableCursor<T>(cursor, cleanup);
        }
    }
}
