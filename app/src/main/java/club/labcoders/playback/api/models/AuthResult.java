package club.labcoders.playback.api.models;

public class AuthResult {
    private boolean success;
    private String token;

    public boolean wasSuccess() {
        return success;
    }

   public String getToken() {
       return token;
   }
}
