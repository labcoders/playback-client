package club.labcoders.playback.api.models;

/**
 * Created by jake on 2016-09-18.
 */

public class ApiAuthPing {
    private String token;

    public ApiAuthPing(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
