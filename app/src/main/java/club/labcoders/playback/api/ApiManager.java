package club.labcoders.playback.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.joda.time.DateTime;

import club.labcoders.playback.api.codecs.Base64BlobDeserializer;
import club.labcoders.playback.api.codecs.Base64BlobSerializer;
import club.labcoders.playback.api.codecs.DateTimeDeserializer;
import club.labcoders.playback.api.codecs.DateTimeSerializer;
import club.labcoders.playback.api.models.Base64Blob;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

/**
 * Singleton class for holding global API settings.
 */
public class ApiManager {
    private static final String BASE_URL = Url.BASE_URL;

    private static ApiManager instance;

    public static void initialize(String token) {
        instance = new ApiManager(token);
    }

    public static ApiManager getInstance() {
        if(instance == null) {
            Timber.e("Tried to get ApiManager before initialization.");
            return null;
        }

        return instance;
    }

    private final PlaybackApi api;
    private final Retrofit adapter;

    /**
     * Constructor is private, for singleton.
     */
    private ApiManager(String tok) {
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
                .serializeNulls()
                .create();

        final OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new GzipInterceptor())
                .addInterceptor(new AuthHeaderInterceptor(tok))
                .build();

        adapter = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .client(client)
                .build();

        api = adapter.create(PlaybackApi.class);
    }

    public PlaybackApi getApi() {
        return api;
    }
}
