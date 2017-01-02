package club.labcoders.playback.db.tables;

import android.database.sqlite.SQLiteDatabase;

import org.joda.time.DateTime;

import club.labcoders.playback.db.DatabaseTable;
import club.labcoders.playback.db.TableOperation;
import club.labcoders.playback.db.columns.BlobColumn;
import club.labcoders.playback.db.columns.Column;
import club.labcoders.playback.db.columns.DateTimeColumn;
import club.labcoders.playback.db.columns.DoubleColumn;
import club.labcoders.playback.db.columns.RawIntColumn;
import club.labcoders.playback.db.columns.ReadColumn;
import club.labcoders.playback.db.columns.RecordingFormatColumn;
import club.labcoders.playback.db.columns.StringColumn;
import club.labcoders.playback.db.models.RecordingFormat;

public class RecordingTable implements DatabaseTable {
    public static final RecordingTable INSTANCE = new RecordingTable();

    private RecordingTable() {

    }

    public final String TABLE_NAME = "recording";

    public final String SCHEMA
            = "CREATE TABLE IF NOT EXISTS recording ("
            + "_id INTEGER PRIMARY KEY, "
            + "remote_id INTEGER UNIQUE, "
            + "recording BLOB, "
            + "latitude REAL, "
            + "longitude REAL, "
            + "duration REAL, "
            + "format INTEGER NOT NULL, "
            + "name VARCHAR, "
            + "recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP);";

    public final ReadColumn<Integer, Integer> ID
            = new RawIntColumn.Named("_id");
    public final ReadColumn<Integer, Integer> REMOTE_ID
            = new RawIntColumn.Named("remote_id");
    public final ReadColumn<Double, Double> LATITUDE
            = new DoubleColumn.Named("latitude");
    public final ReadColumn<Double, Double> LONGITUDE
            = new DoubleColumn.Named("longitude");
    public final ReadColumn<Double, Double> DURATION
            = new DoubleColumn.Named("duration");
    public final ReadColumn<byte[], byte[]> RECORDING
            = new BlobColumn.Named("recording");
    public final ReadColumn<String, String> NAME
            = new StringColumn.Named("name");
    public final ReadColumn<Integer, RecordingFormat> FORMAT
            = new RecordingFormatColumn.Named("format");
    public final ReadColumn<Long, DateTime> RECORDED_AT
            = new DateTimeColumn.Named("recorded_at");

    public String getSchema() {
        return SCHEMA;
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    public void create(final SQLiteDatabase db) {
        db.execSQL(SCHEMA);
    }

    public static class RecordingTableOperation
            implements TableOperation {
        @Override
        public DatabaseTable getTable() {
            return RecordingTable.INSTANCE;
        }
    }
}
