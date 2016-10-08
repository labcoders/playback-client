package club.labcoders.playback;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.joda.time.DateTime;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import club.labcoders.playback.api.ApiManager;
import club.labcoders.playback.api.PlaybackApi;
import club.labcoders.playback.api.models.AudioRecording;
import club.labcoders.playback.api.models.Base64Blob;
import club.labcoders.playback.api.models.Ping;
import club.labcoders.playback.api.models.RecordingMetadata;
import club.labcoders.playback.misc.BufferOperator;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity {
    private static final int FINE_LOCATION_PERMISSION_REQUEST = 0;
    private static final int RECORD_AUDIO_REQUEST = 1;

    @BindView(R.id.statusText)
    TextView statusText;

    @BindView(R.id.uploadButton)
    Button recordButton;

    @BindView(R.id.availableRecordings)
    RecyclerView metadataRecyclerView;

    RecordingService recordingService;
    HttpService httpService;
    private boolean recordingServiceIsBound;
    private CompositeSubscription subscriptions;
    private boolean httpServiceIsBound;
    private boolean canUseFineLocation;

    private List<RecordingMetadata> availableRecordings;
    private RecordingMetadataAdapter mAdapter;

    public MainActivity() {
        recordingServiceIsBound = false;
        httpServiceIsBound = false;
    }

    @OnClick(R.id.uploadButton)
    public void onRecordButtonClick() {
        switch(recordingService.getState()) {
            case MISSING_AUDIO_PERMISSION:

        }
        double length;

        if (recordingService == null) {
            Toast.makeText(
                    this,
                    "No recording service.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (httpService == null) {
            Toast.makeText(this, "No HTTP service", Toast.LENGTH_SHORT).show();
            return;
        }

        if(!recordingService.isRecording()) {
            Toast.makeText(
                    this,
                    "Recording service is not recording.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        short[] shorts = recordingService.getBufferedAudio();

        ByteBuffer buf = ByteBuffer.allocate(shorts.length * 2);
        buf.order(ByteOrder.nativeOrder());
        for (short s : shorts) {
            buf.putShort(s);
        }

        byte[] rawAudio = buf.array();

        Encoder enc = new Encoder();

        Observable.just(rawAudio)
                .doOnNext(
                        bytes -> {
                            final File f = new File(
                                    Environment.getExternalStorageDirectory(),
                                    "temp.pcm"
                            );
                            Timber.d("dumping pcm to %s", f.toString());
                            try(final FileOutputStream fos
                                = new FileOutputStream(f)) {
                                fos.write(bytes);
                            }
                            catch(FileNotFoundException e) {
                                Timber.e("File not found.");
                            }
                            catch(IOException e) {
                                Timber.e("io error");
                            }
                        }
                )
//                    .lift(enc)
//                    .map(encodedOutput -> encodedOutput.byteArray)
                .lift(new BufferOperator())
//                    .lift(new MonoMuxingOperator(enc))
                .flatMap(
                        bytes -> {
                            Timber.d(
                                    "Got muxed byte buffer length %d",
                                    bytes.length
                            );

                            final File f = new File(
                                    Environment.getExternalStorageDirectory(),
                                    "temp.mp4"
                            );
                            Timber.d("Dumping to %s.", f.getAbsolutePath());
                            try (final FileOutputStream fos
                                         = new FileOutputStream(f)) {
                                fos.write(bytes);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            final AudioRecording rec = new AudioRecording(
                                    DateTime.now(),
                                    rawAudio.length
                                            / AudioManager.getInstance().getBytesPerSample()
                                            / AudioManager.getInstance().getSampleRate(),
                                    new Base64Blob(bytes)
                            );

                            return httpService.upload(rec);
                        }
                )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        id -> {
                            Timber.d("Uploaded raw with id " +
                                    "%d", id);
                            Toast.makeText(
                                    MainActivity.this,
                                    String.format(
                                            "Uploaded row with id %d",
                                            id
                                    ),
                                    Toast.LENGTH_LONG
                            ) .show();
                        },
                        err -> {
                            Timber.e("Failed to upload recording.");
                            err.printStackTrace();
                        }
                );
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
                        }, err -> {
                            Toast.makeText(
                                    this,
                                    "Could not connect to server.",
                                    Toast.LENGTH_SHORT
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

        availableRecordings = new ArrayList<>();

        RecyclerView.LayoutManager layoutManager
                = new LinearLayoutManager(getApplicationContext());
        metadataRecyclerView.setLayoutManager(layoutManager);

        mAdapter = new RecordingMetadataAdapter(availableRecordings, this);
        metadataRecyclerView.setAdapter(mAdapter);

        // Check if we have fine location permissions, and set a flag to show that we do/don't.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_PERMISSION_REQUEST);
        }
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

    private void requestRecordingPrivilege() {
        final int grant = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
        );
        if(grant == PackageManager.PERMISSION_DENIED) {
            
        }
    }

    private void startRecording() {
        try {
            if(recordingService == null) {
                Timber.e(
                        "Cannot start recording, because recordingService is " +
                                "null"
                );
                return;
            }

            recordingService.startRecording();
        }
        catch(RecordingService.MissingAudioRecordPermissionException e) {
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[] {Manifest.permission.RECORD_AUDIO},
                    RECORD_AUDIO_REQUEST
            );
            Timber.d("Requested audio recording permission.");
        }
    }

    final ServiceConnection httpConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            final HttpService.HttpServiceBinder binder = (HttpService.HttpServiceBinder) service;
            httpService = binder.getService();
            Timber.d("Assigned httpService");

            // Populate the recycler view.
            httpService.getMetadata()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(list -> {
                        Timber.d(list.toString());
                        for (RecordingMetadata d : list) {
                            Timber.d("Metadata contains: duration: %s, timestamp: %s", d.getDuration(), d.getTimestamp());
                            availableRecordings.add(d);
                        }
                        mAdapter.notifyDataSetChanged();
                        Timber.d("Updated and populated dataset for metadata list with %d items.", list.size());
                        Timber.d("Now recycler view contains %d items.", mAdapter.getItemCount());
                    },
                            err -> {
                                Timber.e("Error while retrieving recording metadata");
                                err.printStackTrace();
                            });

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Timber.d("Http service disconnected");
            httpService = null;
        }
    };

    final ServiceConnection recordingConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            final RecordingService.RecordingServiceBinder binder = (RecordingService.RecordingServiceBinder)service;
            recordingService = binder.getService();
            Timber.d("Assigned recording service");

            final Subscription sub = recordingService.observeState()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            state -> {
                                switch(state) {
                                    case RECORDING:
                                        statusText.setText(R.string.recordingStatusRecording);
                                        recordButton.setText(R.string.recordButtonTakeSnapshot);
                                        break;
                                    case NOT_RECORDING:
                                        statusText.setText(R.string.recordingStatusNotRecording);
                                        recordButton.setText(
                                                R.string.recordButtonStartRecording
                                        );
                                        break;
                                    case MISSING_AUDIO_PERMISSION:
                                        statusText.setText(
                                                R.string.recordStatusNoPermission
                                        );
                                        recordButton.setText(
                                                R.string.recordButtonPermitRecording
                                        );
                                        break;
                                    default:
                                        Timber.wtf(
                                                "Exhaustive case analysis failed"
                                        );
                                        throw new RuntimeException("Oh dear.");
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

            startRecording();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Timber.d("Recording service disconnected");
            recordingService = null;
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case FINE_LOCATION_PERMISSION_REQUEST:
                canUseFineLocation =  (grantResults[0] == PERMISSION_GRANTED);
                break;
            case RECORD_AUDIO_REQUEST:
                if(grantResults[0] == PERMISSION_GRANTED) {
                    startRecording();
                }
                else {
                    Timber.d("User refused audio recording permission.");
                }
                break;
            default:
                throw new RuntimeException("Unexpected request code " + requestCode + " for permission request");
        }
    }
}
