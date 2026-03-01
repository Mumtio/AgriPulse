package com.flir.atlassdk.acecamerasample;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {

    private static final String PREFS_NAME = "AgriPulseSettings";
    private static final String KEY_SOUND_ALERTS = "sound_alerts";
    private static final String KEY_AUTO_SAVE = "auto_save";
    private static final String KEY_VIBRATION = "vibration";

    private SharedPreferences prefs;

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

        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Back button
        ImageButton backButton = view.findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        // Switches
        SwitchCompat soundAlertsSwitch = view.findViewById(R.id.soundAlertsSwitch);
        SwitchCompat autoSaveSwitch = view.findViewById(R.id.autoSaveSwitch);
        SwitchCompat vibrationSwitch = view.findViewById(R.id.vibrationSwitch);

        // Load saved preferences
        soundAlertsSwitch.setChecked(prefs.getBoolean(KEY_SOUND_ALERTS, true));
        autoSaveSwitch.setChecked(prefs.getBoolean(KEY_AUTO_SAVE, true));
        vibrationSwitch.setChecked(prefs.getBoolean(KEY_VIBRATION, false));

        // Save preferences on change
        soundAlertsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_SOUND_ALERTS, isChecked).apply();
            showToast(isChecked ? "Sound alerts enabled" : "Sound alerts disabled");
        });

        autoSaveSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_AUTO_SAVE, isChecked).apply();
            showToast(isChecked ? "Auto-save enabled" : "Auto-save disabled");
        });

        vibrationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_VIBRATION, isChecked).apply();
            showToast(isChecked ? "Vibration enabled" : "Vibration disabled");
        });

        // Export data button
        View exportDataButton = view.findViewById(R.id.exportDataButton);
        exportDataButton.setOnClickListener(v -> {
            showToast("Export feature coming soon");
        });

        // Clear data button
        View clearDataButton = view.findViewById(R.id.clearDataButton);
        clearDataButton.setOnClickListener(v -> {
            // Clear scan storage
            requireContext().getSharedPreferences("scan_storage", Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply();
            showToast("All scan data cleared");
        });
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}
