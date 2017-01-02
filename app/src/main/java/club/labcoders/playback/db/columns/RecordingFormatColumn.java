package club.labcoders.playback.db.columns;

import android.database.Cursor;

import club.labcoders.playback.db.models.RecordingFormat;

public abstract class RecordingFormatColumn extends IntColumn<RecordingFormat> {
    @Override
    public RecordingFormat call(Integer integer) {
        return RecordingFormat.fromInteger(integer);
    }

    public static class Named extends RecordingFormatColumn {
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
