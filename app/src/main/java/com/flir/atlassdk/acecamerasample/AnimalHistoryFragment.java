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

import com.flir.atlassdk.acecamerasample.storage.ScanRecord;

import java.util.ArrayList;
import java.util.List;

public class AnimalHistoryFragment extends Fragment {

    private static final String ARG_ID = "animal_id";
    boolean rising = false;
    private static final String ARG_TYPE = "animal_type";

    private ScanHistoryAdapter adapter;
    private List<ScanRecord> scans;

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
        TextView animalInfo = view.findViewById(R.id.animalInfo);
        RecyclerView recyclerView = view.findViewById(R.id.scanRecyclerView);

        String animalId = getArguments().getString(ARG_ID);
        String animalType = getArguments().getString(ARG_TYPE);

        animalInfo.setText("Animal ID: " + animalId + " · " + animalType);

        backButton.setOnClickListener(v ->
                requireActivity()
                        .getSupportFragmentManager()
                        .popBackStack()
        );

        // Initialize list + adapter ONCE
        scans = new ArrayList<>();

        adapter = new ScanHistoryAdapter(scans, scan -> {
            // Ensure status is not null
            String displayStatus = scan.status;
            if (displayStatus == null) {
                displayStatus = "SUSPECTED".equals(scan.overallStatus) ? "High" : "Normal";
            }
            
            Fragment fragment = ScanDetailFragment.newInstance(
                    scan.temperature,
                    displayStatus,
                    scan.time
            );

            if (scans.size() >= 2) {
                ScanRecord prev = scans.get(scans.size() - 2);
                ScanRecord curr = scans.get(scans.size() - 1);

                rising = curr.temperature - prev.temperature >= 0.5;
            }
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();

        String animalId = getArguments().getString(ARG_ID);

        // Get backend storage from MainActivity
        MainActivity mainActivity = (MainActivity) requireActivity();
        com.flir.atlassdk.acecamerasample.storage.ScanStorage scanStorage = mainActivity.getScanStorage();

        scans.clear();
        scans.addAll(scanStorage.getScansForAnimal(animalId));

        adapter.notifyDataSetChanged();
    }
}
