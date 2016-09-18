package club.labcoders.playback.api.models;

import org.joda.time.DateTime;

/**
 * Created by chokboy on 9/18/16.
 */

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
