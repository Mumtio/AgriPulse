package com.flir.atlassdk.acecamerasample;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.flir.atlassdk.acecamerasample.storage.ScanRecord;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.util.ArrayList;
import java.util.List;

public class AnalyticsFragment extends Fragment {





    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_analytics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView herdStats = view.findViewById(R.id.textHerdStats);
        TextView tempTrend = view.findViewById(R.id.textTempTrend);
        TextView hotZones = view.findViewById(R.id.textHotZones);

        // Get backend modules from MainActivity
        MainActivity mainActivity = (MainActivity) requireActivity();
        com.flir.atlassdk.acecamerasample.storage.ScanStorage scanStorage = mainActivity.getScanStorage();
        com.flir.atlassdk.acecamerasample.tracking.AnimalTracker animalTracker = mainActivity.getAnimalTracker();

        List<String> animalIds = animalTracker.getAllAnimalIds();

        if (animalIds.isEmpty()) {
            herdStats.setText("No scan data available.");
            tempTrend.setText("No temperature trends yet.");
            hotZones.setText("No hot zones detected.");
            return;
        }

        int normal = 0, elevated = 0, high = 0;
        List<Double> allTemps = new ArrayList<>();

        for (String id : animalIds) {
            List<ScanRecord> scans = scanStorage.getScansForAnimal(id);

            if (scans.isEmpty()) continue;

            ScanRecord last = scans.get(scans.size() - 1);

            allTemps.add(last.temperature);

            // Handle null status
            String status = last.status != null ? last.status : "Normal";
            
            switch (status) {
                case "Normal":
                    normal++;
                    break;
                case "Elevated":
                    elevated++;
                    break;
                case "High":
                    high++;
                    break;
            }
        }

        int total = normal + elevated + high;

        herdStats.setText(
                "Normal: " + percent(normal, total) + "%\n" +
                        "Monitor: " + percent(elevated, total) + "%\n" +
                        "High Risk: " + percent(high, total) + "%"
        );

        tempTrend.setText(buildTrendText(allTemps));
        hotZones.setText(buildHotZoneText(animalIds, scanStorage));


        PieChart pieChart = view.findViewById(R.id.herdPieChart);

        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(normal, "Normal"));
        entries.add(new PieEntry(elevated, "Monitor"));
        entries.add(new PieEntry(high, "High Risk"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(
                Color.parseColor("#66BB6A"),
                Color.parseColor("#FFA726"),
                Color.parseColor("#EF5350")
        );

        PieData data = new PieData(dataSet);
        data.setValueTextColor(Color.WHITE);
        data.setValueTextSize(12f);

        pieChart.setData(data);
        pieChart.invalidate();
    }

    private int percent(int value, int total) {
        return total == 0 ? 0 : (value * 100 / total);
    }

    private String buildTrendText(List<Double> temps) {
        if (temps.size() < 2) {
            return "Not enough data for trend analysis.";
        }

        double first = temps.get(0);
        double last = temps.get(temps.size() - 1);
        double diff = last - first;

        String arrow = diff >= 0 ? "↑" : "↓";
        return "Average temperature " + arrow + " "
                + String.format("%.2f", Math.abs(diff)) + "°C";
    }

    private String buildHotZoneText(List<String> animalIds, 
                                    com.flir.atlassdk.acecamerasample.storage.ScanStorage scanStorage) {
        StringBuilder builder = new StringBuilder();

        for (String id : animalIds) {
            List<ScanRecord> scans = scanStorage.getScansForAnimal(id);

            if (scans.isEmpty()) continue;

            ScanRecord last = scans.get(scans.size() - 1);

            if ("High".equals(last.status)) {
                builder.append("• Animal ")
                        .append(id)
                        .append(" — High\n");
            }
        }

        if (builder.length() == 0) {
            return "No hot zones detected.";
        }

        return builder.toString();
    }


}
