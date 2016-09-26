package club.labcoders.playback;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.List;

import club.labcoders.playback.api.models.RecordingMetadata;

public class RecordingMetadataAdapter extends RecyclerView.Adapter<RecordingMetadataAdapter.MetadataViewHolder>{
    List<RecordingMetadata> availableRecordings;
    DateTimeFormatter format;
    public RecordingMetadataAdapter(List<RecordingMetadata> list, Context ctx) {
        availableRecordings = list;
        format = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm");
    }

    @Override
    public MetadataViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.metadata_layout, parent, false);

        return new MetadataViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MetadataViewHolder holder, int position) {
        RecordingMetadata met = availableRecordings.get(position);
        String durationString = DateUtils.formatElapsedTime((long) met.getDuration());
        String timestampString = format.print(met.getTimestamp());

        holder.duration.setText(durationString);
        holder.timestamp.setText(timestampString);
        holder.id.setText(String.valueOf(met.getID()));
    }

    @Override
    public int getItemCount() {
        return availableRecordings.size();
    }

    public class MetadataViewHolder extends RecyclerView.ViewHolder {
        public TextView duration, id, timestamp;

        public MetadataViewHolder(View itemView) {
            super(itemView);
            duration = (TextView) itemView.findViewById(R.id.duration);
            timestamp = (TextView) itemView.findViewById(R.id.timestamp);
            id = (TextView) itemView.findViewById(R.id.id);
        }
    }
}