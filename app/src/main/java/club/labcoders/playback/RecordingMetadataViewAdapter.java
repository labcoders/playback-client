package club.labcoders.playback;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import club.labcoders.playback.api.models.ApiRecordingMetadata;

public class RecordingMetadataViewAdapter extends RecyclerView.Adapter<RecordingMetadataViewAdapter.MetadataViewHolder>{
    List<RecordingMetadata> availableRecordings;
    DateTimeFormatter format;
    public RecordingMetadataViewAdapter(List<RecordingMetadata> list, Context ctx) {
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
        final RecordingMetadata metadata = availableRecordings.get(position);
        holder.setFromRecordingMetadata(metadata);
    }

    @Override
    public int getItemCount() {
        return availableRecordings.size();
    }

    public class MetadataViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.duration)
        public TextView duration;

        @BindView(R.id.id)
        public TextView id;

        @BindView(R.id.timestamp)
        public TextView timestamp;

        public MetadataViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void setFromRecordingMetadata(final RecordingMetadata metadata) {
            String durationString = DateUtils.formatElapsedTime(
                    (long)metadata.getDuration()
            );
            String timestampString = format.print(metadata.getTimestamp());

            duration.setText(durationString);
            timestamp.setText(timestampString);
            id.setText(String.valueOf(metadata.getId()));
        }
    }
}