package club.labcoders.playback.db.columns;

import android.database.Cursor;

/**
 * Created by jake on 2016-10-11.
 */

public abstract class DoubleColumn implements ReadColumn<Double, Double> {
    @Override
    public Double fromRow(Cursor cursor) {
        return cursor.getDouble(
                Column.Misc.safeGetColumnIndex(cursor, getName())
        );
    }

    @Override
    public Double call(Double aDouble) {
        return aDouble;
    }

    public static class Named extends DoubleColumn {
        private final String name;

        public Named(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
