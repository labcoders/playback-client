package club.labcoders.playback.api;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

public class LoggingInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        final Request request = chain.request();

        Timber.d(
                "%s %s",
                request.method(),
                request.url().toString()
        );

        return chain.proceed(request);
    }
}
