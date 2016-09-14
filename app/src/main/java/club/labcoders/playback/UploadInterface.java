package club.labcoders.playback;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface UploadInterface {
    @GET("uploads/{id}")
    Call<AudioRecording> getRecording(@Path("id") int id);

    @POST("uploads")
    Call<Integer> uploadRecording(@Body AudioRecording recording);
}