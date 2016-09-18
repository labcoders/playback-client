package club.labcoders.playback.api;

import club.labcoders.playback.api.models.AuthPing;
import club.labcoders.playback.api.models.AuthPong;
import club.labcoders.playback.api.models.AuthResult;
import club.labcoders.playback.api.models.AuthenticationRequest;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import rx.Observable;

public interface AuthApi {
    @POST("/auth/ping")
    Observable<AuthPong> ping(@Body AuthPing ping);

    @POST("/auth")
    Observable<AuthResult> auth(@Body AuthenticationRequest authenticationRequest);
}
