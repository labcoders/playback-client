package club.labcoders.playback;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioRecord;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.joda.time.DateTime;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import club.labcoders.playback.api.ApiManager;
import club.labcoders.playback.api.PlaybackApi;
import club.labcoders.playback.api.models.Ping;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {
    @BindView(R.id.statusText)
    TextView statusText;

    @BindView(R.id.recordButton)
    Button recordButton;

    RecordingService recordingService;
    private boolean recordingServiceIsBound;
    private CompositeSubscription subscriptions;

    public MainActivity() {
        recordingServiceIsBound = false;
    }

    @OnClick(R.id.recordButton)
    public void onRecordButtonClick() {
        if(recordingService == null) {
            Toast.makeText(this, "No recording service.", Toast.LENGTH_SHORT).show();
        }
        else {
            final Subscription sub = Observable.defer(() -> Observable.just(recordingService.getBufferedAudio()))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(audio -> {
                        Toast.makeText(
                                this,
                                String.format("Recorded %d samples.", audio.length),
                                Toast.LENGTH_SHORT
                        ).show();
                    });
            subscriptions.add(sub);
        }
    }

    @OnClick(R.id.pingButton)
    public void onPingButtonClick() {
        final PlaybackApi api = ApiManager.getInstance().getApi();
        final DateTime now = DateTime.now();
        Timber.d("Now is: %s.", now);
        subscriptions.add(
                api.postPing(new Ping(now))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(pong -> {
                            final DateTime then = pong.getPong();
                            Timber.d("Now became: %s", then.toString());
                            Toast.makeText(
                                    MainActivity.this,
                                    String.format(
                                            "Got pong %s!",
                                            then.toString()
                                    ),
                                    Toast.LENGTH_LONG
                            ).show();
                        })
        );
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        subscriptions = new CompositeSubscription();
        setContentView(R.layout.activity_main);
        ButterKnife.setDebug(true);
        ButterKnife.bind(this);

        Timber.plant(new Timber.DebugTree());

        Intent recordingIntent = new Intent(this, RecordingService.class);
        startService(recordingIntent);
        Timber.d("Started recording service.");
        bindService(recordingIntent, connection, Context.BIND_IMPORTANT);
        Timber.d("Bound recording service.");
        recordingServiceIsBound = true;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        if(recordingServiceIsBound) {
            unbindService(connection);
            recordingServiceIsBound = false;
        }
        subscriptions.unsubscribe();
        super.onDestroy();
    }

    final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            final RecordingService.RecordingServiceBinder binder = (RecordingService.RecordingServiceBinder)service;
            recordingService = binder.getService();

            final Subscription sub = recordingService.recordingState
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            state -> {
                                if(state == AudioRecord.RECORDSTATE_RECORDING) {
                                    statusText.setText(R.string.statusRecording);
                                }
                                else {
                                    statusText.setText(R.string.statusNotRecording);
                                }
                            },
                            error -> {
                                statusText.setText(error.toString());
                            },
                            () -> {
                                statusText.setText(R.string.statusPlaceholder);
                            }
                    );
            subscriptions.add(sub);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            recordingService = null;
        }
    };
}
