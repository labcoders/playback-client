package club.labcoders.playback;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import club.labcoders.playback.api.models.RecordingMetadata;

public class RecordingMetadataAdapter extends RecyclerView.Adapter<RecordingMetadataAdapter.MetadataViewHolder>{
    List<RecordingMetadata> availableRecordings;
    public RecordingMetadataAdapter(List<RecordingMetadata> list) {
        availableRecordings = list;
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
        holder.duration.setText(Double.toString(met.getDuration()));
        holder.timestamp.setText(met.getTimestamp().toString());
    }

    @Override
    public int getItemCount() {
        return availableRecordings.size();
    }

    public class MetadataViewHolder extends RecyclerView.ViewHolder {
        public TextView duration, label, timestamp;

        public MetadataViewHolder(View itemView) {
            super(itemView);
            duration = (TextView) itemView.findViewById(R.id.duration);
            timestamp = (TextView) itemView.findViewById(R.id.timestamp);
        }
    }
}