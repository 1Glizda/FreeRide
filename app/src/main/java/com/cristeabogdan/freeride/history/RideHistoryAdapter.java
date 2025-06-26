package com.cristeabogdan.freeride.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.cristeabogdan.freeride.R;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RideHistoryAdapter
        extends RecyclerView.Adapter<RideHistoryAdapter.ViewHolder> {

    private final List<RideHistoryItem> list;
    private final DecimalFormat df = new DecimalFormat("#.##");
    private final SimpleDateFormat dateFmt =
            new SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault());

    public RideHistoryAdapter(List<RideHistoryItem> list) {
        this.list = list;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ride_history, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        RideHistoryItem m = list.get(pos);

        double dist = m.getDistance() != null ? m.getDistance() : 0.0;
        double dur  = m.getDuration() != null ? m.getDuration() : 0.0;
        double fare = m.getFare()     != null ? m.getFare()     : 0.0;

        h.amountTv.setText("$" + df.format(fare));
        h.detailsTv.setText(df.format(dist) + " km • " + df.format(dur) + " min");

        if (m.getTimestamp() != null) {
            h.dateTv.setText(dateFmt.format(new Date(m.getTimestamp())));
        } else {
            h.dateTv.setText("Unknown date");
        }
    }

    @Override public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView amountTv, detailsTv, dateTv;
        ViewHolder(View v) {
            super(v);
            amountTv  = v.findViewById(R.id.amountTextView);
            detailsTv = v.findViewById(R.id.tripDetailsTextView);
            dateTv    = v.findViewById(R.id.dateTextView);
        }
    }
}
