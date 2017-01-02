package club.labcoders.playback.db.columns;

import android.database.Cursor;

import rx.functions.Func1;

/**
 * A column that can be read from the database.
 *
 * The call() method inherited from Func1 performs the conversion of the
 * basic data to the high-level data, which usually just means wrapping it
 * into a better datatype.
 *
 * @param <D> The basic representation of the data, in the database.
 * @param <J> The high-level representation of the data, in the application.
 */
public interface ReadColumn<D, J> extends Column<D, J>, Func1<D, J> {
    /**
     * Extracts the basic data from a row in the database.
     * @param cursor The cursor, positioned on the row from which to extract
     *               the column data.
     * @return The basic representation of the data.
     */
    D fromRow(Cursor cursor);

    class Misc {
        public static <D, J> J get(ReadColumn<D, J> column, Cursor cursor) {
            return column.call(column.fromRow(cursor));
        }

        /**
         * Extracts the data for a given column from a given row, handling
         * nulls.
         * This method differs from {@link #nullable} in that it will convert
         * the basic representation to the high-level representation via the
         * {@link ReadColumn#call} method if the basic representation is
         * non-null.
         * @param column The column to extract.
         * @param cursor The cursor positioned on the row to extract from.
         * @param <D> The basic representation of the data.
         * @param <J> The high-level representation of the data.
         * @return Null if and only if the value in the database column is null.
         */
        public static <D, J> J getNullable(
                ReadColumn<D, J> column,
                Cursor cursor
        ) {
            final D d = nullable(column, cursor);
            return d == null ? null : column.call(d);
        }

        /**
         * Extracts the data for a given column from a given row, handling
         * nulls.
         * @param column The column to extract.
         * @param cursor The cursor positioned on the row to extract from.
         * @param <D> The basic representation of the data.
         * @param <J> The high-level representation of the data.
         * @return Null if and only if the value in the database column is null.
         */
        public static <D, J> D nullable(
                ReadColumn<D, J> column,
                Cursor cursor
        ) {
            final int index = Column.Misc.safeGetColumnIndex(
                    cursor,
                    column.getName()
            );
            return cursor.isNull(index) ?
                    null : column.fromRow(cursor);
        }
    }
}
