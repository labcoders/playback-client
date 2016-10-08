package club.labcoders.playback.api.models;

import org.joda.time.DateTime;

/**
 * Created by jake on 2016-09-16.
 */

public class ApiPong {
    private DateTime pong;

    public ApiPong() {
    }

    public DateTime getPong() {
        return pong;
    }

    public void setPong(DateTime pong) {
        this.pong = pong;
    }
}
