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
import rx.Observable;
import timber.log.Timber;

public class HttpService extends Service {
    private PlaybackApi api;

    public HttpService() {
        api = ApiManager.getInstance().getApi();
        Timber.d("Http service constructor called");
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
        Timber.d("Destroyed HTTP service.");
        super.onDestroy();
    }
}
