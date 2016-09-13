package club.labcoders.playback;

public interface PlaybackService {
    @GET("uploads/{id}")

    @POST("uploads")
    Call<Int> uploadRecording(AudioRecording recording)
    {
    }
}
