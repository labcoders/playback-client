package club.labcoders.playback.db.columns;

import android.database.Cursor;

public abstract class BlobColumn implements ReadColumn<byte[], byte[]> {
    @Override
    public byte[] fromRow(Cursor cursor) {
        return cursor.getBlob(
                Column.Misc.safeGetColumnIndex(cursor, getName())
        );
    }

    @Override
    public byte[] call(byte[] bytes) {
        return bytes;
    }

    public static class Named extends BlobColumn {
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
