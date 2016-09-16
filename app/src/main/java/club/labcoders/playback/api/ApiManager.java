package club.labcoders.playback.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.joda.time.DateTime;

import club.labcoders.playback.api.codecs.Base64BlobDeserializer;
import club.labcoders.playback.api.codecs.Base64BlobSerializer;
import club.labcoders.playback.api.codecs.DateTimeDeserializer;
import club.labcoders.playback.api.codecs.DateTimeSerializer;
import club.labcoders.playback.api.models.Base64Blob;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton class for holding global API settings.
 */
public class ApiManager {
    private static final String BASE_URL = "http://10.0.3.2:8082";

    private static ApiManager instance;

    public static ApiManager getInstance() {
        if(instance == null) {
            instance = new ApiManager();
        }

        return instance;
    }

    private final PlaybackApi api;
    private final Retrofit adapter;

    /**
     * Constructor is private, for singleton.
     */
    private ApiManager() {
        final Gson gson = new GsonBuilder()
                .registerTypeAdapter(
                        Base64Blob.class,
                        new Base64BlobSerializer()
                )
                .registerTypeAdapter(
                        Base64Blob.class,
                        new Base64BlobDeserializer()
                )
                .registerTypeAdapter(
                        DateTime.class,
                        new DateTimeSerializer()
                )
                .registerTypeAdapter(
                        DateTime.class,
                        new DateTimeDeserializer()
                )
                .create();

        adapter = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();

        api = adapter.create(PlaybackApi.class);
    }

    public PlaybackApi getApi() {
        return api;
    }
}
