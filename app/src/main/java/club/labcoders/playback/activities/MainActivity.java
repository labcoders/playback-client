package club.labcoders.playback.activities;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
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
import club.labcoders.playback.RecordingMetadataSynchronizer;
import club.labcoders.playback.audio.AudioManager;
import club.labcoders.playback.db.adapters.DbRecordingMetadataCursorAdapter;
import club.labcoders.playback.db.models.DbRecordingMetadata;
import club.labcoders.playback.misc.rx.Fold;
import club.labcoders.playback.misc.fp.Pair;
import club.labcoders.playback.misc.TrivialErrorHandler;
import club.labcoders.playback.services.DatabaseService;
import club.labcoders.playback.services.HttpService;
import club.labcoders.playback.R;
import club.labcoders.playback.data.RecordingMetadata;
import club.labcoders.playback.views.RecordingMetadataViewAdapter;
import club.labcoders.playback.services.RecordingService;
import club.labcoders.playback.api.ApiManager;
import club.labcoders.playback.api.PlaybackApi;
import club.labcoders.playback.api.models.ApiAudioRecording;
import club.labcoders.playback.api.models.Base64Blob;
import club.labcoders.playback.api.models.ApiPing;
import club.labcoders.playback.db.models.DbAudioRecording;
import club.labcoders.playback.db.models.RecordingFormat;
import club.labcoders.playback.misc.Box;
import club.labcoders.playback.misc.fp.Cast;
import club.labcoders.playback.misc.rx.RxServiceBinding;
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

    private CompositeSubscription subscriptions;

    private RecordingMetadataViewAdapter mAdapter;

    public MainActivity() {

    }

    @OnClick(R.id.uploadButton)
    public void onRecordButtonClick() {
        final Subscription sub = observeRecordingService()
                .zipWith(observeHttpService(true), Pair::new)
                .flatMap(p -> {
                    final RecordingService recordingService = p.fst;
                    final HttpService httpService = p.snd;

                    switch (recordingService.getState()) {
                        case MISSING_AUDIO_PERMISSION:
                            requestRecordingPrivilege();
                            break;
                        case RECORDING:
                            return takeSnapshot(recordingService, httpService);
                        case NOT_RECORDING:
                            startRecording(recordingService);
                            break;
                    }

                    return Observable.just(null);
                })
                .subscribe(
                        aVoid -> {},
                        new TrivialErrorHandler()
                );
        subscriptions.add(sub);
    }

    @OnClick(R.id.stopRecordingButton)
    public void onStopRecordingButtonClick() {
        final Subscription sub = observeRecordingService()
                .flatMap(recordingService -> {
                    switch(recordingService.getState()) {
                        case RECORDING:
                            stopRecording();
                            break;
                        default:
                            Timber.e("Stop recording pressed but not recording!");
                    }

                    return Observable.just(null);
                })
                .subscribe(
                        $ -> {},
                        new TrivialErrorHandler()
                );
        subscriptions.add(sub);
    }

    private void stopRecording() {
        final Subscription sub = observeRecordingService()
                .flatMap(recordingService -> {
                    recordingService.stopRecording();
                    return Observable.just(null);
                })
                .subscribe(
                        $ -> {},
                        new TrivialErrorHandler("stopRecording")
                );
        subscriptions.add(sub);
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

    private Observable<Void> takeSnapshot(
            RecordingService recordingService,
            HttpService httpService
    ) {
        double length;

        if (recordingService == null) {
            Toast.makeText(
                    this,
                    "No recording service.",
                    Toast.LENGTH_SHORT
            ).show();
            return Observable.just(null);
        }

        if (httpService == null) {
            Toast.makeText(this, "No HTTP service", Toast.LENGTH_SHORT).show();
            return Observable.just(null);
        }

        if(!recordingService.isRecording()) {
            Toast.makeText(
                    this,
                    "Recording service is not recording.",
                    Toast.LENGTH_SHORT
            ).show();
            return Observable.just(null);
        }

        short[] shorts = recordingService.getBufferedAudio();
        final byte[] rawAudio = getRawAudio(shorts);

        // some global state to pass things between parts of the pipeline
        final Box<Integer> remoteId = new Box<>();
        final Box<DateTime> timestamp = new Box<>();
        final Box<Double> duration = new Box<>();
        final String title = "Untitled";

        return Observable.just(rawAudio)
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
                .doOnNext(id -> Timber.d("inserted local rows id %d", id))
                .doOnNext($ ->
                        Timber.d("Uploaded raw with id %d", remoteId.getValue())
                )
                .doOnNext($ ->
                        Toast.makeText(
                                this,
                                String.format(
                                        "Uploaded row with id %d",
                                        remoteId.getValue()
                                ),
                                Toast.LENGTH_LONG
                        ).show()
                )
                .flatMap($ -> observeHttpService(true))
                .zipWith(observeDatabaseService(true), Pair::new)
                .flatMap(p -> updateMetadataListing(p.fst, p.snd))
                .map($ -> null);
    }

    private Observable<List<RecordingMetadata>> updateMetadataListing(
            HttpService httpService,
            DatabaseService databaseService
    ) {
        final Box<List<RecordingMetadataSynchronizer.Result>> resultsBox
                = new Box<>();
        return new RecordingMetadataSynchronizer(httpService, databaseService)
                .synchronize()
                .doOnNext(results -> Timber.d("Synchronized with server."))
                .doOnNext(resultsBox::setValue)
                .flatMap(results -> observeDatabaseMetadata(
                        databaseService
                ))
                .doOnNext(recordingMetadata -> Timber.d(
                        "Got DB metadata: " +
                                "name '%s' " +
                                "duration %f " +
                                "remote id %d",
                        recordingMetadata.getName(),
                        recordingMetadata.getDuration(),
                        recordingMetadata.getRemoteId()
                ))
                .map(new Cast<DbRecordingMetadata, RecordingMetadata>())
                .lift(Fold.listAccumulator())
                .doOnNext(recordingMetadatas -> Timber.d("folded results"));
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

    private Observable<RecordingService> observeRecordingService() {
        return new RxServiceBinding<RecordingService.RecordingServiceBinder>(
                this,
                new Intent(this, RecordingService.class),
                Service.BIND_IMPORTANT)
                .binder(true)
                .map(RecordingService.RecordingServiceBinder::getService);
    }

    private void updateUiForRecordingState(
            RecordingService.RecordingServiceState recordingServiceState
    ) {
        switch(recordingServiceState) {
            case MISSING_AUDIO_PERMISSION:
                statusText.setText(
                        R.string.recordStatusNoPermission
                );
                stopRecordingButton.setVisibility(View.GONE);
                break;
            case NOT_RECORDING:
                statusText.setText(
                        R.string.recordingStatusNotRecording
                );
                stopRecordingButton.setVisibility(View.GONE);
                break;
            case RECORDING:
                statusText.setText(
                        R.string.statusRecording
                );
                stopRecordingButton.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        subscriptions = new CompositeSubscription();
        setContentView(R.layout.activity_main);
        ButterKnife.setDebug(true);
        ButterKnife.bind(this);

        final Subscription uiSub = observeRecordingService()
                .doOnNext($ -> Timber.d("Got recording service!"))
                .flatMap(RecordingService::observeState)
                .doOnNext(state -> Timber.d(
                        "Got recording state %s",
                        state.toString()
                ))
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(this::updateUiForRecordingState)
                .subscribe();
        subscriptions.add(uiSub);

        final Subscription sub = observeHttpService(true)
                .first()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(
                        this::withHttpService,
                        this::withoutHttpService,
                        () -> Observable.just(null)
                )
                .subscribe(
                        aVoid -> {},
                        new TrivialErrorHandler("MainActivity.onCreate")
                );
        subscriptions.add(sub);

        RecyclerView.LayoutManager layoutManager
                = new LinearLayoutManager(getApplicationContext());
        metadataRecyclerView.setLayoutManager(layoutManager);
        mAdapter = new RecordingMetadataViewAdapter();
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

    private Observable<Void> withoutHttpService(Throwable error) {
        Timber.e("httpService unavailable");
        Toast.makeText(this, "Unable to connect to server.", Toast.LENGTH_LONG)
                .show();
        error.printStackTrace();
        setMetadataListing(new ArrayList<>());
        metadataRecyclerView.setVisibility(View.GONE);
        return Observable.just(null);
    }

    private Observable<Void> withHttpService(
            HttpService httpService
    ) {
        return observeDatabaseService(true)
                .observeOn(Schedulers.io())
                .flatMap(databaseService ->
                        updateMetadataListing(httpService, databaseService)
                )
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(this::setMetadataListing)
                .map($ -> null);
    }

    private Observable<DbRecordingMetadata> observeDatabaseMetadata(
            DatabaseService databaseService) {
        return databaseService.observeSimpleQueryOperation(
                new DbRecordingMetadata.QueryAllOperation(),
                new DbRecordingMetadataCursorAdapter())
                .doOnNext(dbRecordingMetadata -> Timber.d(
                        "Got DB recording metadata:" +
                                "name %s " +
                                "duration %.2f " +
                                "time %s ",
                        dbRecordingMetadata.getName(),
                        dbRecordingMetadata.getDuration(),
                        dbRecordingMetadata.getTimestamp().toString()
                ));
    }

    private void setMetadataListing(List<RecordingMetadata> list) {
        synchronized(mAdapter) {
            mAdapter.getRecordings().clear();
            for (final RecordingMetadata d : list) {
                Timber.d(
                        "Metadata contains: " +
                                "name: %s: " +
                                "duration: %s, " +
                                "timestamp: %s",
                        d.getName(),
                        d.getDuration(),
                        d.getTimestamp()
                );
                mAdapter.getRecordings().add(d);
            }
            mAdapter.notifyDataSetChanged();
        }
        Timber.d("Updated and populated dataset for metadata list with %d items.", list.size());
        Timber.d("Now recycler view contains %d items.", mAdapter.getItemCount());
    }

    @Override
    protected void onDestroy() {
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

    private void startRecording(RecordingService recordingService) {
        try {
            recordingService.startRecording();
        }
        catch(RecordingService.MissingAudioRecordPermissionException $) {
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[] {Manifest.permission.RECORD_AUDIO},
                    RECORD_AUDIO_AND_START_REQUEST
            );
            Timber.d("Requested audio recording permission.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case FINE_LOCATION_PERMISSION_REQUEST:
                // TODO actually do something with this fact
                break;
            case RECORD_AUDIO_AND_START_REQUEST:
                if(grantResults.length > 0 &&
                        grantResults[0] == PERMISSION_GRANTED) {
                    final Subscription sub = observeRecordingService()
                            .doOnNext(this::startRecording)
                            .subscribe(
                                    $ -> {},
                                    new TrivialErrorHandler()
                            );
                    subscriptions.add(sub);
                }
                else {
                    Timber.d("User refused audio recording permission.");
                }
                break;
            case RECORD_AUDIO_REQUEST:
                if(grantResults.length > 0 &&
                        grantResults[0] == PERMISSION_GRANTED) {
                    Timber.d("Yay.");
                    final Subscription sub = observeRecordingService()
                            .map(recordingService -> {
                                recordingService.checkState();
                                return null;
                            })
                            .subscribe(
                                    $ -> {},
                                    new TrivialErrorHandler()
                            );
                    subscriptions.add(sub);
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
