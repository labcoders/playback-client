package club.labcoders.playback.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.joda.time.DateTime;

import club.labcoders.playback.api.codecs.DateTimeDeserializer;
import club.labcoders.playback.api.codecs.DateTimeSerializer;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class AuthManager {
    private final Gson gson;
    private static AuthManager instance;
    private static Retrofit authAdapter;
    private static AuthApi auth;

    private final String BASE_URL = Url.BASE_URL;

    public static void initialize() {
        if (!(instance == null)) {
            return;
        }

        instance = new AuthManager();
    }

    public static AuthManager getInstance() {
        if (instance == null) {
            initialize();
        }
        return instance;
    }

    private AuthManager() {
        gson = new GsonBuilder()
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

        authAdapter = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();
        auth = authAdapter.create(AuthApi.class);

    }

    public AuthApi getApi() {
        return auth;
    }
}
