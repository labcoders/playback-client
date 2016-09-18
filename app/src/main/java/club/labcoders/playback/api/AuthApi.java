package club.labcoders.playback.api;

import club.labcoders.playback.api.models.AuthPong;
import club.labcoders.playback.api.models.AuthResult;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import rx.Observable;

public interface AuthApi {
    @POST("/auth/ping")
    Observable<AuthPong> ping(@Body String token);

    @POST("/auth")
    Observable<AuthResult> auth(@Query("username") String username, @Query("password") String password);
}
