package com.agripulse.cattlehealth;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.agripulse.cattlehealth.api.ApiService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SettingsFragment extends Fragment {

    private static final String PREFS_NAME = "AgriPulseSettings";
    private static final String KEY_SOUND_ALERTS = "sound_alerts";
    private static final String KEY_AUTO_SAVE = "auto_save";
    private static final String KEY_VIBRATION = "vibration";
    private static final String KEY_TEMP_NORMAL_MAX = "temp_normal_max";
    private static final String KEY_TEMP_ELEVATED_MAX = "temp_elevated_max";

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

        setupTemperatureThresholds(view);
        setupScanSettings(view);
        setupDataManagement(view);
    }
    
    private void setupTemperatureThresholds(View view) {
        // Temperature threshold displays (read-only for now, could be made editable)
        TextView normalThreshold = view.findViewById(R.id.normalThresholdText);
        TextView elevatedThreshold = view.findViewById(R.id.elevatedThresholdText);
        TextView highRiskThreshold = view.findViewById(R.id.highRiskThresholdText);
        
        // Load saved thresholds or use defaults
        float normalMax = prefs.getFloat(KEY_TEMP_NORMAL_MAX, 38.5f);
        float elevatedMax = prefs.getFloat(KEY_TEMP_ELEVATED_MAX, 39.5f);
        
        if (normalThreshold != null) {
            normalThreshold.setText("< " + normalMax + "°C");
        }
        if (elevatedThreshold != null) {
            elevatedThreshold.setText(normalMax + " - " + elevatedMax + "°C");
        }
        if (highRiskThreshold != null) {
            highRiskThreshold.setText("> " + elevatedMax + "°C");
        }
        
        // Make thresholds clickable to adjust (simple increment/decrement)
        if (normalThreshold != null) {
            normalThreshold.setOnClickListener(v -> adjustThreshold("normal", normalMax, view));
        }
        if (elevatedThreshold != null) {
            elevatedThreshold.setOnClickListener(v -> adjustThreshold("elevated", elevatedMax, view));
        }
    }
    
    private void adjustThreshold(String type, float currentValue, View parentView) {
        // Simple threshold adjustment - could be enhanced with a dialog
        float newValue;
        if ("normal".equals(type)) {
            newValue = currentValue == 38.5f ? 38.0f : (currentValue == 38.0f ? 39.0f : 38.5f);
            prefs.edit().putFloat(KEY_TEMP_NORMAL_MAX, newValue).apply();
            showToast("Normal threshold set to < " + newValue + "°C");
        } else if ("elevated".equals(type)) {
            newValue = currentValue == 39.5f ? 39.0f : (currentValue == 39.0f ? 40.0f : 39.5f);
            prefs.edit().putFloat(KEY_TEMP_ELEVATED_MAX, newValue).apply();
            showToast("Elevated threshold set to > " + newValue + "°C");
        }
        
        // Refresh the display
        setupTemperatureThresholds(parentView);
    }
    
    private void setupScanSettings(View view) {
        // Switches
        SwitchCompat soundAlertsSwitch = view.findViewById(R.id.soundAlertsSwitch);
        SwitchCompat autoSaveSwitch = view.findViewById(R.id.autoSaveSwitch);
        SwitchCompat vibrationSwitch = view.findViewById(R.id.vibrationSwitch);

        // Load saved preferences
        if (soundAlertsSwitch != null) {
            soundAlertsSwitch.setChecked(prefs.getBoolean(KEY_SOUND_ALERTS, true));
            soundAlertsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean(KEY_SOUND_ALERTS, isChecked).apply();
                showToast(isChecked ? "Sound alerts enabled" : "Sound alerts disabled");
            });
        }

        if (autoSaveSwitch != null) {
            autoSaveSwitch.setChecked(prefs.getBoolean(KEY_AUTO_SAVE, true));
            autoSaveSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean(KEY_AUTO_SAVE, isChecked).apply();
                showToast(isChecked ? "Auto-save enabled" : "Auto-save disabled");
            });
        }

        if (vibrationSwitch != null) {
            vibrationSwitch.setChecked(prefs.getBoolean(KEY_VIBRATION, false));
            vibrationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean(KEY_VIBRATION, isChecked).apply();
                showToast(isChecked ? "Vibration enabled" : "Vibration disabled");
            });
        }
    }
    
    private void setupDataManagement(View view) {
        // Export data button
        View exportDataButton = view.findViewById(R.id.exportDataButton);
        if (exportDataButton != null) {
            exportDataButton.setOnClickListener(v -> exportAllData());
            
            // Long press for backend connection test
            exportDataButton.setOnLongClickListener(v -> {
                testBackendConnection();
                return true;
            });
        }

        // Clear data button
        View clearDataButton = view.findViewById(R.id.clearDataButton);
        if (clearDataButton != null) {
            clearDataButton.setOnClickListener(v -> clearAllData());
        }
    }
    
    private void exportAllData() {
        showToast("Exporting scan data...");
        
        // Get all animals and their scan data from backend
        ApiService.getInstance().getAnimals(new ApiService.AnimalsCallback() {
            @Override
            public void onSuccess(List<ApiService.AnimalInfo> animals) {
                if (getActivity() == null) return;
                
                getActivity().runOnUiThread(() -> {
                    exportDataToFile(animals);
                });
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() == null) return;
                
                getActivity().runOnUiThread(() -> {
                    // Fall back to local data export
                    exportLocalData();
                });
            }
        });
    }
    
    private void exportDataToFile(List<ApiService.AnimalInfo> animals) {
        try {
            // Create export file
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String filename = "AgriPulse_Export_" + timestamp + ".csv";
            
            File exportDir = new File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "AgriPulse");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }
            
            File exportFile = new File(exportDir, filename);
            FileWriter writer = new FileWriter(exportFile);
            
            // Write CSV header
            writer.append("Animal_ID,Tag_ID,Name,Breed,Age,Total_Scans,Export_Date\n");
            
            // Write animal data
            String exportDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            for (ApiService.AnimalInfo animal : animals) {
                writer.append(animal.id).append(",")
                      .append(animal.tagId != null ? animal.tagId : "").append(",")
                      .append(animal.name != null ? animal.name : "").append(",")
                      .append(animal.breed != null ? animal.breed : "").append(",")
                      .append(String.valueOf(animal.age)).append(",")
                      .append(String.valueOf(animal.scanCount)).append(",")
                      .append(exportDate).append("\n");
            }
            
            writer.close();
            
            // Share the file
            shareExportFile(exportFile);
            
            showToast("✓ Exported " + animals.size() + " animals to " + filename);
            
        } catch (IOException e) {
            showToast("✗ Export failed: " + e.getMessage());
        }
    }
    
    private void exportLocalData() {
        // Export local scan storage as fallback
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String filename = "AgriPulse_Local_Export_" + timestamp + ".txt";
            
            File exportDir = new File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "AgriPulse");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }
            
            File exportFile = new File(exportDir, filename);
            FileWriter writer = new FileWriter(exportFile);
            
            writer.append("AgriPulse Local Data Export\n");
            writer.append("Generated: ").append(new Date().toString()).append("\n\n");
            
            List<String> animalIds = ScanStorage.getAllAnimalIds(requireContext());
            writer.append("Animals: ").append(String.valueOf(animalIds.size())).append("\n\n");
            
            for (String animalId : animalIds) {
                List<ScanResult> scans = ScanStorage.getScansForAnimal(requireContext(), animalId);
                writer.append("Animal: ").append(animalId).append(" (").append(String.valueOf(scans.size())).append(" scans)\n");
                
                for (ScanResult scan : scans) {
                    writer.append("  - ").append(scan.time)
                          .append(": ").append(String.format("%.1f°C", scan.temperature))
                          .append(" (").append(scan.status).append(")\n");
                }
                writer.append("\n");
            }
            
            writer.close();
            shareExportFile(exportFile);
            showToast("✓ Exported local data to " + filename);
            
        } catch (IOException e) {
            showToast("✗ Local export failed: " + e.getMessage());
        }
    }
    
    private void shareExportFile(File file) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/*");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "AgriPulse Cattle Health Export");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Cattle health monitoring data exported from AgriPulse app.");
        
        // Note: For file sharing, you'd need to use FileProvider in a production app
        // For now, just show the file path
        showToast("Export saved to: " + file.getAbsolutePath());
    }
    
    private void clearAllData() {
        // Show confirmation dialog (simplified)
        showToast("Clearing local data...");
        
        // Clear local storage
        requireContext().getSharedPreferences("scan_storage", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
        
        // Clear app cache
        try {
            File cacheDir = requireContext().getCacheDir();
            if (cacheDir != null && cacheDir.isDirectory()) {
                deleteRecursive(cacheDir);
            }
        } catch (Exception e) {
            // Ignore cache clear errors
        }
        
        showToast("✓ Local scan data cleared");
        // Note: Backend data is preserved for data integrity
    }
    
    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        fileOrDirectory.delete();
    }
    
    private void testBackendConnection() {
        showToast("Testing backend connection...");
        
        ApiService.getInstance().checkHealth(new ApiService.HealthCallback() {
            @Override
            public void onSuccess(String message) {
                if (getActivity() == null) return;
                
                getActivity().runOnUiThread(() -> {
                    showToast("✓ Backend connected: " + message);
                    
                    // Also test animals endpoint
                    ApiService.getInstance().getAnimals(new ApiService.AnimalsCallback() {
                        @Override
                        public void onSuccess(List<ApiService.AnimalInfo> animals) {
                            if (getActivity() == null) return;
                            
                            getActivity().runOnUiThread(() -> {
                                int totalScans = 0;
                                for (ApiService.AnimalInfo animal : animals) {
                                    totalScans += animal.scanCount;
                                }
                                showToast("✓ Database: " + animals.size() + " animals, " + totalScans + " scans");
                            });
                        }
                        
                        @Override
                        public void onError(String error) {
                            if (getActivity() == null) return;
                            getActivity().runOnUiThread(() -> showToast("⚠ Database error: " + error));
                        }
                    });
                });
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() == null) return;
                
                getActivity().runOnUiThread(() -> {
                    showToast("✗ Backend error: " + error);
                });
            }
        });
    }
    
    // Public methods for other fragments to access settings
    public static boolean isSoundAlertsEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_SOUND_ALERTS, true);
    }
    
    public static boolean isAutoSaveEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_AUTO_SAVE, true);
    }
    
    public static boolean isVibrationEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_VIBRATION, false);
    }
    
    public static float getNormalThresholdMax(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getFloat(KEY_TEMP_NORMAL_MAX, 38.5f);
    }
    
    public static float getElevatedThresholdMax(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getFloat(KEY_TEMP_ELEVATED_MAX, 39.5f);
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}
