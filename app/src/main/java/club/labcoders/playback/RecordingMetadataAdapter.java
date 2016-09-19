package club.labcoders.playback;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import club.labcoders.playback.api.models.RecordingMetadata;

public class RecordingMetadataAdapter extends RecyclerView.Adapter<RecordingMetadataAdapter.ItemViewHolder>{
    List<RecordingMetadata> availableRecordings;
    public RecordingMetadataAdapter(List<RecordingMetadata> list) {
        availableRecordings = list;
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder {
        public ItemViewHolder(View itemView) {
            super(itemView);
        }
    }
}