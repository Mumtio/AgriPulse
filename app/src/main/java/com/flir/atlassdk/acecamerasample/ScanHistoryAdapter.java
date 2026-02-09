package com.flir.atlassdk.acecamerasample;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.flir.atlassdk.acecamerasample.storage.ScanRecord;

import java.util.List;

public class ScanHistoryAdapter extends RecyclerView.Adapter<ScanHistoryAdapter.ViewHolder> {

    private final List<ScanRecord> scans;
    private final OnScanClickListener listener;


    public interface OnScanClickListener {

        void onScanClick(ScanRecord scan);

    }


    public ScanHistoryAdapter(List<ScanRecord> scans,OnScanClickListener listener) {
        this.scans = scans;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_scan_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScanRecord scan = scans.get(position);

        // Ensure status is not null
        String displayStatus = scan.status;
        if (displayStatus == null) {
            displayStatus = "SUSPECTED".equals(scan.overallStatus) ? "High" : "Normal";
        }

        holder.temp.setText(String.format("%.1f °C", scan.temperature));
        holder.time.setText(scan.time);
        holder.status.setText(displayStatus);

        applyStatusStyle(holder.statusIndicator, holder.status, displayStatus);
        holder.itemView.setOnClickListener(v -> listener.onScanClick(scan));

    }

    @Override
    public int getItemCount() {
        return scans.size();
    }

    // same status logic as animal list
    private void applyStatusStyle(View indicator, TextView statusText, String status) {
        switch (status) {
            case "High":
                indicator.setBackgroundResource(R.drawable.status_dot_red);
                statusText.setTextColor(0xFFD32F2F);
                break;

            case "Elevated":
                indicator.setBackgroundResource(R.drawable.status_dot_amber);
                statusText.setTextColor(0xFFF9A825);
                break;

            default:
                indicator.setBackgroundResource(R.drawable.status_dot_green);
                statusText.setTextColor(0xFF2E7D32);
                break;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView temp, time, status;
        View statusIndicator;

        ViewHolder(View itemView) {
            super(itemView);
            temp = itemView.findViewById(R.id.tempText);
            time = itemView.findViewById(R.id.timeText);
            status = itemView.findViewById(R.id.statusText);
            statusIndicator = itemView.findViewById(R.id.statusIndicator);
        }
    }
}
