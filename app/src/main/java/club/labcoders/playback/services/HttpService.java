package club.labcoders.playback.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;

import java.util.List;

import club.labcoders.playback.data.SessionToken;
import club.labcoders.playback.api.ApiManager;
import club.labcoders.playback.api.PlaybackApi;
import club.labcoders.playback.api.models.ApiAudioRecording;
import club.labcoders.playback.api.models.ApiRecordingMetadata;
import club.labcoders.playback.misc.rx.RxServiceBinding;
import club.labcoders.playback.misc.TrivialErrorHandler;
import rx.Observable;
import rx.Subscription;
import rx.subjects.BehaviorSubject;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

public class HttpService extends Service {
    // private PlaybackApi apiSubject;
    private CompositeSubscription subscriptions;
    private BehaviorSubject<PlaybackApi> apiSubject;

    public HttpService() {
        Timber.d("Http service constructor called");
        subscriptions = new CompositeSubscription();
        apiSubject = BehaviorSubject.create();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.d("HTTP Service onCreate called");
        final Subscription sub = observeDatabaseService()
                .doOnNext($ -> Timber.d("Getting token."))
                .flatMap(DatabaseService::getToken)
                .filter(sessionToken -> {
                    if(sessionToken == null) {
                        Timber.d("No token; not initializing apiSubject.");
                        return false;
                    }
                    else {
                        Timber.d("Got token!");
                        return true;
                    }
                })
                .doOnNext(sessionToken -> {
                    Timber.d("Got token; initializing apiSubject.");
                    ApiManager.initialize(sessionToken.getToken());
                    //noinspection ConstantConditions
                    apiSubject.onNext(ApiManager.getInstance().getApi());
                })
                .subscribe(
                        $ -> {},
                        new TrivialErrorHandler("HttpService.onCreate")
                );
        subscriptions.add(sub);
    }

    public Observable<DatabaseService> observeDatabaseService() {
        return new RxServiceBinding<DatabaseService.DatabaseServiceBinder>(
                this,
                new Intent(this, DatabaseService.class),
                Service.BIND_AUTO_CREATE)
                .binder(true)
                .map(DatabaseService.DatabaseServiceBinder::getService);
    }

    public Observable<Integer> upload(ApiAudioRecording rec) {
        Timber.d("Recording upload started.");
        return apiSubject.flatMap(api -> api.uploadRecording(rec));
    }

    public Observable<ApiAudioRecording> getAudioRecording(Integer id) {
        Timber.d("Recoding retrieval started.");
        return apiSubject.flatMap(api -> api.getRecording(id));
    }

    public Observable<List<ApiRecordingMetadata>> getMetadata() {
        Timber.d("Metadata retrieval started.");
        return apiSubject.flatMap(PlaybackApi::getMetadata);
    }

    @NonNull
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
