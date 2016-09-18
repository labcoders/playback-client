package club.labcoders.playback.api.models;

import org.joda.time.DateTime;

public class AuthPong {
    private boolean valid;
    private DateTime timestamp;

    public AuthPong() {
    }

    public boolean isValid() {
        return valid;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }
}
