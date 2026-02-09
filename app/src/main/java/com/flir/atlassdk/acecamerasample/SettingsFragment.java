package com.flir.atlassdk.acecamerasample;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsFragment extends Fragment {

    private SharedPreferences prefs;
    private static final String PREF_BATCH_MODE = "batch_mode_enabled";
    private static final String PREF_LOCATION = "location_enabled";

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get shared preferences
        prefs = requireActivity().getSharedPreferences("AgriPulseSettings", 
            android.content.Context.MODE_PRIVATE);

        // Get backend modules
        MainActivity mainActivity = (MainActivity) requireActivity();
        com.flir.atlassdk.acecamerasample.storage.ScanStorage scanStorage = 
            mainActivity.getScanStorage();
        com.flir.atlassdk.acecamerasample.tracking.AnimalTracker animalTracker = 
            mainActivity.getAnimalTracker();

        // Setup switches
        SwitchMaterial switchBatchMode = view.findViewById(R.id.switchBatchMode);
        SwitchMaterial switchLocation = view.findViewById(R.id.switchLocation);

        // Load saved preferences
        switchBatchMode.setChecked(prefs.getBoolean(PREF_BATCH_MODE, false));
        switchLocation.setChecked(prefs.getBoolean(PREF_LOCATION, true));

        // Save preferences on change
        switchBatchMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(PREF_BATCH_MODE, isChecked).apply();
        });

        switchLocation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(PREF_LOCATION, isChecked).apply();
        });

        // Display storage statistics
        TextView textStorageStats = view.findViewById(R.id.textStorageStats);
        int totalScans = scanStorage.getScanCount();
        int totalAnimals = animalTracker.getAnimalCount();

        textStorageStats.setText(
            "Total Scans: " + totalScans + "\n" +
            "Animals Tracked: " + totalAnimals + "\n" +
            "Storage: " + formatStorageSize(totalScans)
        );
    }

    private String formatStorageSize(int scanCount) {
        // Rough estimate: ~5KB per scan
        long bytes = scanCount * 5 * 1024L;
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }
}
