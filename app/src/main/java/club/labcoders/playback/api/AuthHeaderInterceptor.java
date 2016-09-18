package club.labcoders.playback.api;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthHeaderInterceptor implements Interceptor {

    String sessionToken;
    @Override
    public Response intercept(final Chain chain) throws IOException {
        Request orig = chain.request();
        Request newReq = orig.newBuilder()
                .addHeader("token", sessionToken)
                .method(orig.method(), orig.body())
                .build();

        return chain.proceed(newReq);
    }

    public AuthHeaderInterceptor(String token) {
        sessionToken = token;
    }
}
