package club.labcoders.playback;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.util.List;

import club.labcoders.playback.api.ApiManager;
import club.labcoders.playback.api.PlaybackApi;
import club.labcoders.playback.api.models.AudioRecording;
import club.labcoders.playback.api.models.RecordingMetadata;
import club.labcoders.playback.db.DatabaseService;
import rx.Observable;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

public class HttpService extends Service {
    private PlaybackApi api;
    private CompositeSubscription subscriptions;

    public HttpService() {
        Timber.d("Http service constructor called");
        subscriptions = new CompositeSubscription();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.d("HTTP Service onCreate called");
        final Subscription sub = new RxServiceBinding<DatabaseService.DatabaseServiceBinder>(
                this,
                new Intent(this, DatabaseService.class),
                Service.BIND_AUTO_CREATE
        )
                .binder(true)
                .map(DatabaseService.DatabaseServiceBinder::getService)
                .flatMap(DatabaseService::getToken)
                .subscribe(s -> {
                    if(s == null) {
                        Timber.d("No token; not initializing api.");
                        api = null;
                    }
                    Timber.d("Got token; initializing api.");
                    ApiManager.initialize(s);
                    api = ApiManager.getInstance().getApi();
                });
        subscriptions.add(sub);
    }

    public Observable<Integer> upload(AudioRecording rec) {
        Timber.d("Recording upload started.");
        return api.uploadRecording(rec);
    }

    public Observable<AudioRecording> getAudioRecording(Integer id) {
        Timber.d("Recoding retrieval started.");
        return api.getRecording(id);
    }

    public Observable<List<RecordingMetadata>> getMetadata() {
        Timber.d("Metadata retrieval started.");
        return api.getMetadata();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new HttpServiceBinder();
    }

    public class HttpServiceBinder extends Binder {
        public HttpService getService() { return HttpService.this; }
    }

    @Override
    public void onDestroy() {
        subscriptions.unsubscribe();
        Timber.d("Destroyed HTTP service.");
        super.onDestroy();
    }
}
