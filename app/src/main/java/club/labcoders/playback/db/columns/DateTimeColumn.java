package club.labcoders.playback.db.columns;

import org.joda.time.DateTime;

import club.labcoders.playback.db.models.DateTimeAdapter;

public abstract class DateTimeColumn extends LongColumn<DateTime> {
    private final DateTimeAdapter adapter;

    public DateTimeColumn() {
        adapter = new DateTimeAdapter();
    }

    @Override
    public DateTime call(Long ts) {
        return adapter.fromInteger(ts);
    }

    public static class Named extends DateTimeColumn {
        private final String name;

        public Named(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
