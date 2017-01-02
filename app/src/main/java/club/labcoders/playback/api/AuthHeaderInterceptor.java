package club.labcoders.playback.api;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

public class AuthHeaderInterceptor implements Interceptor {
    private final String sessionToken;

    public AuthHeaderInterceptor(String token) {
        sessionToken = token;
    }

    @Override
    public Response intercept(final Chain chain) throws IOException {
        Request orig = chain.request();
        Request newReq = orig.newBuilder()
                .addHeader("Authorization", "oToke " + sessionToken)
                .method(orig.method(), orig.body())
                .build();
        return chain.proceed(newReq);
    }
}
