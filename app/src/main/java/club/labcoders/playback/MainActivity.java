package club.labcoders.playback;

import android.Manifest;
import android.app.Service;
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
import android.view.View;
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
import club.labcoders.playback.api.models.ApiAudioRecording;
import club.labcoders.playback.api.models.Base64Blob;
import club.labcoders.playback.api.models.ApiPing;
import club.labcoders.playback.api.models.ApiRecordingMetadata;
import club.labcoders.playback.db.DatabaseService;
import club.labcoders.playback.db.models.DbAudioRecording;
import club.labcoders.playback.db.models.RecordingFormat;
import club.labcoders.playback.misc.Box;
import club.labcoders.playback.misc.BufferOperator;
import club.labcoders.playback.misc.Map;
import club.labcoders.playback.misc.RxServiceBinding;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity {
    private static final int FINE_LOCATION_PERMISSION_REQUEST = 0;
    private static final int RECORD_AUDIO_AND_START_REQUEST = 1;
    private static final int RECORD_AUDIO_REQUEST = 2;

    @BindView(R.id.statusText)
    TextView statusText;

    @BindView(R.id.uploadButton)
    Button recordButton;

    @BindView(R.id.availableRecordings)
    RecyclerView metadataRecyclerView;

    @BindView(R.id.stopRecordingButton)
    Button stopRecordingButton;

    RecordingService recordingService;
    HttpService httpService;
    private boolean recordingServiceIsBound;
    private CompositeSubscription subscriptions;
    private boolean httpServiceIsBound;
    private boolean canUseFineLocation;

    private List<RecordingMetadata> availableRecordings;
    private RecordingMetadataViewAdapter mAdapter;

    public MainActivity() {
        recordingServiceIsBound = false;
        httpServiceIsBound = false;
    }

    @OnClick(R.id.uploadButton)
    public void onRecordButtonClick() {
        switch (recordingService.getState()) {
            case MISSING_AUDIO_PERMISSION:
                requestRecordingPrivilege();
                break;
            case RECORDING:
                takeSnapshot();
                break;
            case NOT_RECORDING:
                startRecording();
                break;
        }
    }

    @OnClick(R.id.stopRecordingButton)
    public void onStopRecordingButtonClick() {
        switch(recordingService.getState()) {
            case RECORDING:
                stopRecording();
                break;
            default:
                Timber.e("Stop recording pressed but not recording!");
        }
    }

    private void stopRecording() {
        recordingService.stopRecording();
    }

    private byte[] getRawAudio(short[] audioSamples) {
        ByteBuffer buf = ByteBuffer.allocate(audioSamples.length * 2);
        buf.order(ByteOrder.nativeOrder());
        for (short s : audioSamples) {
            buf.putShort(s);
        }
        return buf.array();
    }

    private void dumpBytesToFile(byte[] data) {
        final File f = new File(
                Environment.getExternalStorageDirectory(),
                "temp.pcm"
        );
        Timber.d("dumping pcm to %s", f.toString());
        try(final FileOutputStream fos
                    = new FileOutputStream(f)) {
            fos.write(data);
        }
        catch(FileNotFoundException e) {
            Timber.e("File not found.");
        }
        catch(IOException e) {
            Timber.e("io error");
        }
    }

    private void takeSnapshot() {
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
        final byte[] rawAudio = getRawAudio(shorts);

        // some global state to pass things between parts of the pipeline
        final Box<Integer> remoteId = new Box<>();
        final Box<DateTime> timestamp = new Box<>();
        final Box<Double> duration = new Box<>();
        final String title = "Untitled";

        Observable.just(rawAudio)
                .doOnNext(this::dumpBytesToFile)
                .flatMap(bytes -> {
                            timestamp.setValue(DateTime.now());
                    final double audioLength
                            = rawAudio.length
                            / AudioManager.getInstance().getBytesPerSample()
                            / (double) AudioManager.getInstance()
                            .getSampleRate();
                    duration.setValue(audioLength);
                    final ApiAudioRecording rec = new ApiAudioRecording(
                            DateTime.now(),
                            audioLength,
                            new Base64Blob(bytes),
                            title
                    );

                    return httpService.upload(rec);
                })
                .flatMap(id -> {
                    remoteId.setValue(id);
                    return observeDatabaseService(true);
                })
                .flatMap(databaseService -> databaseService
                        .observeSimpleInsertOperation(
                                new DbAudioRecording
                                        .InsertOperationBuilder()
                                        .setDuration(duration.getValue())
                                        .setRecording(rawAudio)
                                        .setFormat(
                                                RecordingFormat
                                                        .PCM_S16LE_FORMAT
                                        )
                                        .setLatitude(null)
                                        .setLongitude(null)
                                        .setName("Untitled")
                                        .setRemoteId(remoteId.getValue())
                                        .build()
                        )
                )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        id -> {
                            Timber.d("Inserted local rows id %d", id);
                            Timber.d("Uploaded raw with id " +
                                    "%d", remoteId.getValue());
                            Toast.makeText(
                                    MainActivity.this,
                                    String.format(
                                            "Uploaded row with id %d",
                                            remoteId.getValue()
                                    ),
                                    Toast.LENGTH_LONG
                            ).show();

                            updateMetadataListing();
                        },
                        err -> {
                            Timber.e("Failed to upload recording.");
                            err.printStackTrace();
                        }
                );
    }

    private Observable<DatabaseService> observeDatabaseService(
            boolean cleanup
    ) {
        return new RxServiceBinding<DatabaseService.DatabaseServiceBinder>(
                this,
                new Intent(
                        this,
                        DatabaseService.class
                ),
                Service.BIND_AUTO_CREATE)
                .binder(true)
                .map(DatabaseService.DatabaseServiceBinder::getService);
    }

    @OnClick(R.id.pingButton)
    public void onPingButtonClick() {
        final PlaybackApi api = ApiManager.getInstance().getApi();
        final DateTime now = DateTime.now();
        Timber.d("Now is: %s.", now);
        subscriptions.add(
                api.postPing(new ApiPing(now))
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
        final Subscription sub = observeHttpService(true)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::withHttpService,
                        error -> withoutHttpService(error)
                );

        Timber.d("Bound HTTP service");
        httpServiceIsBound = true;

        availableRecordings = new ArrayList<>();

        RecyclerView.LayoutManager layoutManager
                = new LinearLayoutManager(getApplicationContext());
        metadataRecyclerView.setLayoutManager(layoutManager);

        mAdapter = new RecordingMetadataViewAdapter(availableRecordings, this);
        metadataRecyclerView.setAdapter(mAdapter);

        // Check if we have fine location permissions, and set a flag to show that we do/don't.
        final int fineGrant = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        );
        final int coarseGrant = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
        );
        if (fineGrant != PERMISSION_GRANTED && coarseGrant != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    FINE_LOCATION_PERMISSION_REQUEST
            );
        }
    }

    private Observable<HttpService> observeHttpService(boolean cleanup) {
        return new RxServiceBinding<HttpService.HttpServiceBinder>(
                this,
                new Intent(
                        this,
                        HttpService.class
                ),
                Context.BIND_AUTO_CREATE)
                .binder(true)
                .map(HttpService.HttpServiceBinder::getService);
    }

    private void withoutHttpService(Throwable error) {
        Timber.e("httpService unavailable");
        Toast.makeText(this, "Unable to connect to server.", Toast.LENGTH_LONG)
                .show();
        error.printStackTrace();
        setMetadataListing(new ArrayList<>());
        metadataRecyclerView.setVisibility(View.GONE);
    }

    private void withHttpService(HttpService httpService) {
        if(httpService == null && this.httpService == null) {
            Timber.e("Called withHttpService, but have no httpservice");
            throw new RuntimeException("wtf yolo");
        }
        if(this.httpService == null)
            this.httpService = httpService;
        else if(httpService == null)
            Timber.d("Reusing stored httpservice in withHttpService");

        updateMetadataListing();
    }

    private void updateMetadataListing() {
        // Populate the recycler view.
        final Box<Subscription> box = new Box<>();
        final Subscription sub = this.httpService.getMetadata()
                .lift(new Map<ApiRecordingMetadata, RecordingMetadata>(
                        RecordingMetadata::from
                ))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::setMetadataListing,
                        err -> {
                            Timber.e("Error while retrieving recording metadata");
                            err.printStackTrace();
                            box.getValue().unsubscribe();
                        }
                );
        box.setValue(sub);
        subscriptions.add(sub);
    }

    private void setMetadataListing(List<RecordingMetadata> list) {
        Timber.d(list.toString());
        availableRecordings.clear();
        for (final RecordingMetadata d : list) {
            Timber.d("Metadata contains: duration: %s, timestamp: %s", d.getDuration(), d.getTimestamp());
            availableRecordings.add(d);
        }
        mAdapter.notifyDataSetChanged();
        Timber.d("Updated and populated dataset for metadata list with %d items.", list.size());
        Timber.d("Now recycler view contains %d items.", mAdapter.getItemCount());
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
            ActivityCompat.requestPermissions(
                    this,
                    new String[] {Manifest.permission.RECORD_AUDIO},
                    RECORD_AUDIO_REQUEST
            );
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
                    RECORD_AUDIO_AND_START_REQUEST
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
                                        stopRecordingButton.setVisibility(
                                                View.VISIBLE
                                        );
                                        break;
                                    case NOT_RECORDING:
                                        statusText.setText(R.string.recordingStatusNotRecording);
                                        recordButton.setText(
                                                R.string.recordButtonStartRecording
                                        );
                                        stopRecordingButton.setVisibility(
                                                View.GONE
                                        );
                                        break;
                                    case MISSING_AUDIO_PERMISSION:
                                        statusText.setText(
                                                R.string.recordStatusNoPermission
                                        );
                                        recordButton.setText(
                                                R.string.recordButtonPermitRecording
                                        );
                                        stopRecordingButton.setVisibility(
                                                View.GONE
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
            case RECORD_AUDIO_AND_START_REQUEST:
                if(grantResults.length > 0 &&
                        grantResults[0] == PERMISSION_GRANTED) {
                    startRecording();
                }
                else {
                    Timber.d("User refused audio recording permission.");
                }
                break;
            case RECORD_AUDIO_REQUEST:
                if(grantResults.length > 0 &&
                        grantResults[0] == PERMISSION_GRANTED) {
                    Timber.d("Yay.");
                    recordingService.checkState();
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
