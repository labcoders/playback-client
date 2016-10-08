package club.labcoders.playback.api.models;

import org.joda.time.DateTime;

public class ApiPing {
    private DateTime ping;

    public ApiPing() {
    }

    public ApiPing(DateTime ping) {
        this.ping = ping;
    }

    public DateTime getPing() {
        return ping;
    }

    public void setPing(DateTime ping) {
        this.ping = ping;
    }
}
