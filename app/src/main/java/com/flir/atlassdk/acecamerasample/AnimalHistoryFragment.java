package com.flir.atlassdk.acecamerasample;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AnimalHistoryFragment extends Fragment {

    private static final String ARG_ID = "animal_id";
    boolean rising = false;
    private static final String ARG_TYPE = "animal_type";

    private ScanHistoryAdapter adapter;
    private List<ScanResult> scans;

    public static AnimalHistoryFragment newInstance(String id, String type) {
        AnimalHistoryFragment fragment = new AnimalHistoryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ID, id);
        args.putString(ARG_TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_animal_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton backButton = view.findViewById(R.id.backButton);
        TextView animalIdText = view.findViewById(R.id.animalIdText);
        TextView animalTypeText = view.findViewById(R.id.animalTypeText);
        TextView totalScansText = view.findViewById(R.id.totalScansText);
        TextView lastTempText = view.findViewById(R.id.lastTempText);
        TextView trendText = view.findViewById(R.id.trendText);
        RecyclerView recyclerView = view.findViewById(R.id.scanRecyclerView);

        String animalId = getArguments().getString(ARG_ID);
        String animalType = getArguments().getString(ARG_TYPE);

        animalIdText.setText(animalId);
        animalTypeText.setText(animalType);

        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        // Initialize list + adapter ONCE
        scans = new ArrayList<>();

        adapter = new ScanHistoryAdapter(scans, scan -> {
            Fragment fragment = ScanDetailFragment.newInstance(
                    scan.temperature,
                    scan.status,
                    scan.time
            );

            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        // Load scans and update stats
        loadScansAndUpdateStats(animalId, totalScansText, lastTempText, trendText);
    }

    private void loadScansAndUpdateStats(String animalId, TextView totalScansText, 
                                         TextView lastTempText, TextView trendText) {
        scans.clear();
        scans.addAll(ScanStorage.getScansForAnimal(requireContext(), animalId));

        // Add sample data if no real data exists
        if (scans.isEmpty()) {
            scans.add(new ScanResult(38.2, "Normal", "3 days ago · 8:15 AM"));
            scans.add(new ScanResult(38.5, "Normal", "2 days ago · 7:30 AM"));
            scans.add(new ScanResult(39.1, "Elevated", "Yesterday · 6:45 AM"));
            scans.add(new ScanResult(39.4, "Elevated", "Yesterday · 5:20 PM"));
            scans.add(new ScanResult(39.7, "High", "Today · 6:45 AM"));
        }

        // Update stats
        totalScansText.setText(String.valueOf(scans.size()));

        if (!scans.isEmpty()) {
            ScanResult lastScan = scans.get(scans.size() - 1);
            lastTempText.setText(String.format("%.1f°C", lastScan.temperature));

            // Apply color to last temp based on status
            switch (lastScan.status) {
                case "High":
                    lastTempText.setTextColor(0xFFD32F2F);
                    break;
                case "Elevated":
                    lastTempText.setTextColor(0xFFF57C00);
                    break;
                default:
                    lastTempText.setTextColor(0xFF2E7D32);
                    break;
            }

            // Calculate trend
            if (scans.size() >= 2) {
                ScanResult prev = scans.get(scans.size() - 2);
                ScanResult curr = scans.get(scans.size() - 1);
                double diff = curr.temperature - prev.temperature;

                if (diff >= 0.5) {
                    trendText.setText("↑ Rising");
                    trendText.setTextColor(0xFFD32F2F);
                    rising = true;
                } else if (diff <= -0.5) {
                    trendText.setText("↓ Falling");
                    trendText.setTextColor(0xFF2E7D32);
                    rising = false;
                } else {
                    trendText.setText("→ Stable");
                    trendText.setTextColor(0xFF757575);
                    rising = false;
                }
            } else {
                trendText.setText("→ Stable");
                trendText.setTextColor(0xFF757575);
            }
        }

        // Notify adapter after data is loaded
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        String animalId = getArguments().getString(ARG_ID);
        TextView totalScansText = getView().findViewById(R.id.totalScansText);
        TextView lastTempText = getView().findViewById(R.id.lastTempText);
        TextView trendText = getView().findViewById(R.id.trendText);

        loadScansAndUpdateStats(animalId, totalScansText, lastTempText, trendText);
    }
}
