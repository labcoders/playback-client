package club.labcoders.playback.api;

import java.util.List;

import club.labcoders.playback.api.models.ApiAudioRecording;
import club.labcoders.playback.api.models.ApiPing;
import club.labcoders.playback.api.models.ApiPong;
import club.labcoders.playback.api.models.ApiRecordingMetadata;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import rx.Observable;

public interface PlaybackApi {
    @GET("uploads/{id}")
    Observable<ApiAudioRecording> getRecording(@Path("id") int id);

    @POST("uploads")
    Observable<Integer> uploadRecording(@Body ApiAudioRecording recording);

    @POST("ping")
    Observable<ApiPong> postPing(@Body ApiPing ping);

    @GET("metadata")
    Observable<List<ApiRecordingMetadata>> getMetadata();
}