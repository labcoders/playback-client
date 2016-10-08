package club.labcoders.playback.api;

import club.labcoders.playback.api.models.ApiAuthPing;
import club.labcoders.playback.api.models.ApiAuthPong;
import club.labcoders.playback.api.models.ApiAuthResult;
import club.labcoders.playback.api.models.ApiAuthenticationRequest;
import retrofit2.http.Body;
import retrofit2.http.POST;
import rx.Observable;

public interface AuthApi {
    @POST("/auth/ping")
    Observable<ApiAuthPong> ping(@Body ApiAuthPing ping);

    @POST("/auth")
    Observable<ApiAuthResult> auth(@Body ApiAuthenticationRequest authenticationRequest);
}
