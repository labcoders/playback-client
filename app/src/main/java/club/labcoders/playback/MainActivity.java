package club.labcoders.playback;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.joda.time.DateTime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import club.labcoders.playback.api.ApiManager;
import club.labcoders.playback.api.PlaybackApi;
import club.labcoders.playback.api.models.AudioRecording;
import club.labcoders.playback.api.models.Base64Blob;
import club.labcoders.playback.api.models.GeographicalPosition;
import club.labcoders.playback.api.models.Ping;
import club.labcoders.playback.concurrent.CVar;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.media.MediaFormat.KEY_BIT_RATE;
import static android.media.MediaFormat.KEY_CHANNEL_COUNT;
import static android.media.MediaFormat.KEY_MIME;
import static android.media.MediaFormat.KEY_SAMPLE_RATE;

public class MainActivity extends AppCompatActivity {
    private static final int FINE_LOCATION_PERMISSION_REQUEST = 0;

    @BindView(R.id.statusText)
    TextView statusText;

    @BindView(R.id.uploadButton)
    Button recordButton;

    RecordingService recordingService;
    HttpService httpService;
    private boolean recordingServiceIsBound;
    private CompositeSubscription subscriptions;
    private boolean httpServiceIsBound;
    private boolean canUseFineLocation;

    public MainActivity() {
        recordingServiceIsBound = false;
        httpServiceIsBound = false;
    }

    @OnClick(R.id.uploadButton)
    public void onRecordButtonClick() throws IOException, InterruptedException {
        double length;
        Location loc;
        Base64Blob blob;
        DateTime date;

        AudioRecording recording;

        if (recordingService == null) {
            Toast.makeText(this, "No recording service.", Toast.LENGTH_SHORT).show();
        } else if (httpService == null) {
            Toast.makeText(this, "No HTTP service", Toast.LENGTH_SHORT).show();
        } else {
            short[] shorts = recordingService.getBufferedAudio();

            String mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension("ogg");

            MediaCodec codec = MediaCodec.createEncoderByType(mimetype);
            MediaFormat format = new MediaFormat();
            format.setString(MediaFormat.KEY_MIME, mimetype);
            format.setInteger(KEY_BIT_RATE, 128);
            format.setInteger(KEY_SAMPLE_RATE, recordingService.SAMPLE_RATE);
            format.setInteger(KEY_CHANNEL_COUNT, 1);

            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

            Encoder enc = new Encoder(codec);

            // Calculate length of audio
            length = shorts.length / recordingService.SAMPLE_RATE;

            ByteBuffer buf = ByteBuffer.allocate(shorts.length * 2);
            for (short s : shorts) {
                buf.putShort(s);
            }

            byte[] rawAudio = buf.array();
            ByteArrayOutputStream compressedAudio = new ByteArrayOutputStream():

            CVar<Void> signal = new CVar();

            Observable.just(rawAudio).lift(enc).doOnNext(compressed -> {
                try {
                    compressedAudio.write(compressed);
                } catch (IOException e) {
                    Timber.e("Fuck")
                }
            }).doOnCompleted(() -> {
                try {
                    signal.write(null);
                } catch (InterruptedException e) {
                    Timber.e("Jesus fucking christ")
                }
            });

            // Resynchronize.
            signal.read();

            // Construct blob.
            blob = new Base64Blob(compressedAudio.toByteArray());

            // Get current date
            date = DateTime.now();

            if (canUseFineLocation) {
                LocationManager lcm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

                //noinspection ResourceType
                // Get location if we can.
                loc = lcm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                if (loc == null) {
                    recording = new AudioRecording(date, length, blob);
                } else {
                    GeographicalPosition g = new GeographicalPosition(loc.getLatitude(), loc.getLongitude());

                    recording = new AudioRecording(date, length, blob, g);
                }
            } else {
                recording = new AudioRecording(date, length, blob);
            }

            Observable<Integer> uploadResult = httpService.upload(recording);

            uploadResult.subscribe(uploadID -> {
                Toast.makeText(this, "Successfully uploaded. Id is : " + uploadID + ".", Toast.LENGTH_SHORT).show();
                Timber.d("Uploaded recording with id " + uploadID);
            });;
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
        bindService(recordingIntent, recordingConnection, Context.BIND_IMPORTANT);
        Timber.d("Bound recording service.");
        recordingServiceIsBound = true;

        Intent httpIntent = new Intent(this, HttpService.class);
        startService(httpIntent);
        Timber.d("Started HTTP service.");
        bindService(httpIntent, httpConnection, Context.BIND_IMPORTANT);
        Timber.d("Bound HTTP service");
        httpServiceIsBound = true;

        // Check if we have fine location permissions, and set a flag to show that we do/don't.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_PERMISSION_REQUEST);
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        if(recordingServiceIsBound) {
            unbindService(recordingConnection);
            recordingServiceIsBound = false;
        }


        if(httpServiceIsBound) {
            unbindService(httpConnection);
            httpServiceIsBound = false;
        }
        subscriptions.unsubscribe();
        super.onDestroy();
    }

    final ServiceConnection httpConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            final HttpService.HttpServiceBinder binder = (HttpService.HttpServiceBinder) service;
            httpService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            httpService = null;
        }
    };

    final ServiceConnection recordingConnection = new ServiceConnection() {
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 0:
                if (grantResults[0] == PERMISSION_GRANTED) {
                    canUseFineLocation = true;
                } else {
                    canUseFineLocation = false;
                }
                break;
            default:
                throw new RuntimeException("Unexpected request code " + requestCode + " for permission request");
        }
    }
}
