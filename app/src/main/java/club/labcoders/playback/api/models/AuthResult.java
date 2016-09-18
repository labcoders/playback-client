package club.labcoders.playback.api.models;

public class AuthResult {
    private boolean success;
    private String token;

    public AuthResult(boolean success, String token) {
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
