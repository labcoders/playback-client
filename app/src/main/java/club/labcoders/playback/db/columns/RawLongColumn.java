package club.labcoders.playback.db.columns;

import android.database.Cursor;

public abstract class RawLongColumn extends LongColumn<Long> {
    @Override
    public Long call(Long aLong) {
        return aLong;
    }

    public static class Named extends RawLongColumn {
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
