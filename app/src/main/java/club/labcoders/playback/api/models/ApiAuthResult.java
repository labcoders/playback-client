package club.labcoders.playback.api.models;

public class ApiAuthResult {
    private boolean success;
    private String token;

    public ApiAuthResult(boolean success, String token) {
        this.success = success;
        this.token = token;
    }

    public boolean getSuccess() {
        return success;
    }
    public String getToken() {
       return token;
   }
}
