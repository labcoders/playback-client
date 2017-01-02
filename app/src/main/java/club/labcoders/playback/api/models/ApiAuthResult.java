package club.labcoders.playback.api.models;

import club.labcoders.playback.data.SessionToken;

public class ApiAuthResult implements SessionToken {
    private boolean success;
    private String token;

    public ApiAuthResult(boolean success, String token) {
        this.success = success;
        this.token = token;
    }

    public boolean getSuccess() {
        return success;
    }

    @Override
    public String getToken() {
       return token;
   }
}
