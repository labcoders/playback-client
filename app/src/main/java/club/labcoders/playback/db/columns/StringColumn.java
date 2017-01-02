package club.labcoders.playback.db.columns;

import android.database.Cursor;

public abstract class StringColumn implements ReadColumn<String, String> {
    @Override
    public String fromRow(Cursor cursor) {
        return cursor.getString(
                Column.Misc.safeGetColumnIndex(cursor, getName())
        );
    }

    @Override
    public String call(String s) {
        return s;
    }

    public static class Named extends StringColumn {
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
