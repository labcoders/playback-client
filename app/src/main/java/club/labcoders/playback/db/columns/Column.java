package club.labcoders.playback.db.columns;

import android.database.Cursor;

import java.util.Arrays;
import java.util.List;

import club.labcoders.playback.misc.Map;
import club.labcoders.playback.misc.StringJoiner;

/**
 * A column in the database.
 * @param <D> The basic representation of the data, in the database.
 * @param <J> The high-level representation of the data, in the application.
 */
public interface Column<D, J> {
    String getName();

    class Misc {
        private static final StringJoiner commaJoiner = new StringJoiner(", ");
        private static final Map<Column, String> columnName
                = new Map<>(Column::getName);

        public static String select(List<Column> columns) {
            return commaJoiner.join(columnName.call(columns)) + " ";
        }

        public static int safeGetColumnIndex(Cursor cursor, String column) {
            final int result = cursor.getColumnIndex(column);
            if(result == -1) {
                throw new NoSuchColumnException(column);
            }
            return result;
        }
    }

    class NoSuchColumnException extends RuntimeException {
        private final String column;

        public NoSuchColumnException(String column) {
            this.column = column;
        }

        @Override
        public String toString() {
            return String.format("No such column %s.", column);
        }
    }
}
