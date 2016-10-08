package club.labcoders.playback.api.models;

import org.joda.time.DateTime;

public class ApiAuthPong {
    private boolean valid;
    private DateTime timestamp;

    public ApiAuthPong() {
    }

    public boolean isValid() {
        return valid;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }
}
