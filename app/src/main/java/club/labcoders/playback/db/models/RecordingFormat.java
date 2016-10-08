package club.labcoders.playback.db.models;

import timber.log.Timber;

public enum RecordingFormat {
    PCM_S16LE_FORMAT, MP3_FORMAT;

    public static RecordingFormat fromInteger(int i) {
        switch(i) {
            case 0:
                return PCM_S16LE_FORMAT;
            case 1:
                return MP3_FORMAT;
            default:
                throw new InvalidRecordingFormatCode(i);
        }
    }

    public int toInteger() {
        switch(this) {
            case PCM_S16LE_FORMAT:
                return 0;
            case MP3_FORMAT:
                return 1;
            default:
                Timber.wtf("Exhaustive case analysis failed.");
                throw new InvalidRecordingFormatCode(-1);
        }
    }

    public static class InvalidRecordingFormatCode extends RuntimeException {
        private final int formatCode;

        public InvalidRecordingFormatCode(int i) {
            formatCode = i;
        }

        public int getFormatCode() {
            return formatCode;
        }
    }
}
