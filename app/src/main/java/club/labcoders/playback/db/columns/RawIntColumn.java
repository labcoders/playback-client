package club.labcoders.playback.db.columns;

import android.database.Cursor;

public abstract class RawIntColumn extends IntColumn<Integer> {
    @Override
    public Integer call(Integer integer) {
        return integer;
    }

    public static class Named extends RawIntColumn {
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
