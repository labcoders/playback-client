package club.labcoders.playback;

import java.util.List;

import club.labcoders.playback.db.models.DbRecordingMetadata;
import club.labcoders.playback.misc.rx.Fold;
import club.labcoders.playback.services.DatabaseService;
import club.labcoders.playback.services.HttpService;
import rx.Observable;
import timber.log.Timber;

/**
 * Synchronizes metadata between the client and the server.
 */
public class RecordingMetadataSynchronizer {
    private final HttpService http;
    private final DatabaseService db;

    public RecordingMetadataSynchronizer(HttpService http, DatabaseService db) {
        this.http = http;
        this.db = db;
    }

    /**
     * Synchronize the recording metadata we have locally with the server.
     *
     * The overall idea of the synchronization is that the data that we have
     * locally is the most accurate.
     *
     * The synchronization proceeds like this:
     *   * download all metadata from the server
     *   * upsert rows in the DB for the metadata, based on remote ID
     *
     * @return An observable for the result of the synchronization process.
     */
    public Observable<List<Result>> synchronize() {
        return http.getMetadata()
                .flatMap(Observable::from)
                .flatMap(metadata -> db.observeSimpleUpdateOperation(
                        new DbRecordingMetadata.RemoteUpsertOperation(metadata)
                ))
                .doOnNext(aLong -> Timber.d("Upsert metadata %d", aLong))
                .map(aLong -> new Result())
                .lift(Fold.listAccumulator());
    }

    public class Result {
    }
}
