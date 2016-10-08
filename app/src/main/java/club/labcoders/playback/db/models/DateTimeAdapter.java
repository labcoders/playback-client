package club.labcoders.playback.db.models;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Converts JodaTime DateTime objects into milliseconds since epoch in UTC and
 * back.
 * Used to persist DateTime objects to the database.
 */
public class DateTimeAdapter {
    public DateTime fromInteger(long timestamp) {
        return new DateTime(timestamp, DateTimeZone.UTC);
    }

    public long fromDateTime(final DateTime dateTime) {
        return dateTime.toInstant().getMillis();
    }
}
