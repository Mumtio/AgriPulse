package com.agripulse.cattlehealth;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.agripulse.cattlehealth.api.ApiService;
import com.agripulse.cattlehealth.camera.CameraManager;
import com.agripulse.cattlehealth.thermal.ThermalExtractor;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class ScanFragment extends Fragment {

    private static final String TAG = "ScanFragment";
    private static final String TEST_ANIMAL_ID = "COW001"; // Changed to match backend test data

    private Button shareButton;
    private ScanResult lastScan;
    private TextView statusText;
    private TextView explanationText;
    private TextView tempText;
    // private TextView cameraModeText; // Commented out - not in layout
    private View overlay;
    
    // Video controls
    private LinearLayout videoControls;
    private TextView videoInfoText;
    private ImageButton buttonPrevVideo;
    private ImageButton buttonNextVideo;
    private ImageView videoPreview;
    
    // Store current scan data
    private String currentScanId;
    private ApiService.DiagnosisResponse currentDiagnosis;
    
    // NEW: Camera Manager for auto-detection
    private CameraManager cameraManager;
    
    // Video preview update
    private Handler videoUpdateHandler = new Handler(Looper.getMainLooper());
    private Runnable videoUpdateRunnable;
    private boolean isVideoPlaying = false;
    
    // Auto-scan system
    private Handler autoScanHandler = new Handler(Looper.getMainLooper());
    private Runnable autoScanRunnable;
    private boolean isAutoScanEnabled = false;
    private static final int AUTO_SCAN_INTERVAL_MS = 15000; // 15 seconds

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_scan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        overlay = view.findViewById(R.id.scanOverlay);

        explanationText = view.findViewById(R.id.textExplanation);
        statusText = view.findViewById(R.id.textScanResult);
        tempText = view.findViewById(R.id.tempText);
        // cameraModeText = view.findViewById(R.id.cameraModeText); // Commented out - not in layout

        Button scanButton = view.findViewById(R.id.buttonScan);
        shareButton = view.findViewById(R.id.buttonShare);
        ImageButton backButton = view.findViewById(R.id.buttonBack);
        
        // Video controls
        videoControls = view.findViewById(R.id.videoControls);
        videoInfoText = view.findViewById(R.id.videoInfoText);
        buttonPrevVideo = view.findViewById(R.id.buttonPrevVideo);
        buttonNextVideo = view.findViewById(R.id.buttonNextVideo);
        videoPreview = view.findViewById(R.id.videoPreview);

        explanationText.setText("Initializing camera system...");
        statusText.setText("");
        tempText.setText("--.- °C");
        shareButton.setVisibility(View.GONE);
        videoControls.setVisibility(View.GONE);

        Animation pulse = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse);
        overlay.startAnimation(pulse);

        // NEW: Initialize Camera Manager
        initializeCamera();

        // Test backend connection on load
        testBackendConnection();

        scanButton.setOnClickListener(v -> {
            // Manual scan - full analysis including diagnosis
            performScan();
        });

        shareButton.setOnClickListener(v -> {
            shareScanReport();
        });

        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
        
        // Video control listeners
        buttonPrevVideo.setOnClickListener(v -> {
            if (cameraManager != null) {
                cameraManager.previousSimulationVideo();
                updateVideoInfo();
            }
        });
        
        buttonNextVideo.setOnClickListener(v -> {
            if (cameraManager != null) {
                cameraManager.nextSimulationVideo();
                updateVideoInfo();
            }
        });
    }
    
    /**
     * NEW: Initialize camera system with auto-detection
     */
    private void initializeCamera() {
        try {
            cameraManager = new CameraManager(requireContext());
            
            cameraManager.initialize(new CameraManager.CameraStatusListener() {
                @Override
                public void onCameraDetected(CameraManager.CameraMode mode) {
                    Log.d(TAG, "Camera mode detected: " + mode);
                    
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            String modeText = cameraManager.getModeDescription();
                        
                            // Show/hide video controls based on mode
                            if (mode == CameraManager.CameraMode.SIMULATION && cameraManager.hasSimulationVideos()) {
                                videoControls.setVisibility(View.VISIBLE);
                                videoPreview.setVisibility(View.VISIBLE);
                                updateVideoInfo();
                                startVideoPreview();
                                startAutoScan();
                            } else {
                                videoControls.setVisibility(View.GONE);
                                videoPreview.setVisibility(View.GONE);
                                stopVideoPreview();
                                stopAutoScan();
                            }
                        
                            // Show toast
                            String message = mode == CameraManager.CameraMode.REAL_FLIR
                                ? "✓ FLIR Camera Connected - Auto-scanning enabled"
                                : (cameraManager.hasSimulationVideos() 
                                    ? "⚠ Video Simulation - Auto-scanning every 15s" 
                                    : "⚠ Static Simulation (No Videos)");
                        
                            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                        });
                    }
                }
            
            @Override
            public void onCameraConnected() {
                Log.d(TAG, "Camera connected");
            }
            
            @Override
            public void onCameraDisconnected() {
                Log.d(TAG, "Camera disconnected");
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), 
                            "Camera disconnected. Switching to simulation.", 
                            Toast.LENGTH_SHORT).show();
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Camera error: " + error);
            }
        });
        
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize camera", e);
            // Create a fallback camera manager in simulation mode
            cameraManager = new CameraManager(requireContext());
            Toast.makeText(requireContext(), 
                "Camera initialization failed - using simulation mode", 
                Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Test backend connection
     */
    private void testBackendConnection() {
        ApiService.getInstance().checkHealth(new ApiService.HealthCallback() {
            @Override
            public void onSuccess(String message) {
                Log.d(TAG, "Backend connected: " + message);
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Backend connection failed: " + error);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), 
                            "Backend not available. Using simulated mode.", 
                            Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
    
    /**
     * Perform complete scan workflow
     * Step 1: Capture image and thermal data
     * Step 2: Send to backend for analysis
     * Step 3: Extract temperatures
     * Step 4: Send for diagnosis
     * Step 5: Display results
     */
    private void performScan() {
        Log.d(TAG, "Starting scan workflow...");
        
        // Update UI
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                statusText.setText("Checking backend...");
                explanationText.setText("Preparing analysis");
                tempText.setText("--.- °C");
                shareButton.setVisibility(View.GONE);
                overlay.clearAnimation();
            });
        }
        
        // First check if backend is awake (Render free tier sleeps after inactivity)
        ApiService.getInstance().checkHealth(new ApiService.HealthCallback() {
            @Override
            public void onSuccess(String message) {
                Log.d(TAG, "Backend is ready: " + message);
                proceedWithScan();
            }
            
            @Override
            public void onError(String error) {
                Log.w(TAG, "Backend health check failed, proceeding anyway: " + error);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        statusText.setText("Waking backend...");
                        explanationText.setText("Starting analysis server (may take 30s)");
                    });
                }
                // Wait a moment then proceed (backend might be cold starting)
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    proceedWithScan();
                }, 3000);
            }
        });
    }
    
    private void proceedWithScan() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                statusText.setText("Capturing...");
                explanationText.setText("Analyzing thermal image");
            });
        }
        
        // Step 1: Capture frame and thermal data
        File imageFile = captureFrame();
        float[][] thermalData = captureThermalData();
        
        if (imageFile == null) {
            showError("Failed to capture image file");
            return;
        }
        
        if (thermalData == null) {
            showError("Failed to capture thermal data");
            return;
        }
        
        Log.d(TAG, "Captured image: " + imageFile.getAbsolutePath());
        Log.d(TAG, "Image file size: " + imageFile.length() + " bytes");
        Log.d(TAG, "Thermal data size: " + thermalData.length + "x" + thermalData[0].length);
        
        // Update UI
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                statusText.setText("Analyzing body parts...");
            });
        }
        
        // Step 2: Send to backend for analysis
        ApiService.getInstance().analyzeImage(
            imageFile,
            thermalData,
            TEST_ANIMAL_ID,
            new ApiService.AnalyzeCallback() {
                @Override
                public void onSuccess(ApiService.AnalyzeResponse response) {
                    Log.d(TAG, "=== ANALYZE SUCCESS CALLBACK ===");
                    Log.d(TAG, "Analysis successful. Scan ID: " + response.scanId);
                    Log.d(TAG, "Body parts detected: " + (response.bodyParts != null ? response.bodyParts.size() : 0));
                    Log.d(TAG, "Thermal data: " + (response.thermalData != null ? response.thermalData.size() : 0));
                    Log.d(TAG, "Diagnosis: " + (response.diagnosis != null ? response.diagnosis.status : "null"));
                    
                    currentScanId = response.scanId;
                    
                    // Check if we already have diagnosis from the backend
                    if (response.diagnosis != null) {
                        Log.d(TAG, "Using diagnosis from backend response");
                        currentDiagnosis = response.diagnosis;
                        
                        // Calculate average temperature for display
                        float avgTemp = calculateAverageTemperature(response.thermalData);
                        
                        // Update UI
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                tempText.setText(String.format(Locale.getDefault(), "%.1f °C", avgTemp));
                            });
                        }
                        
                        // Display results directly
                        displayResults(response.diagnosis, avgTemp, response.bodyParts.size());
                        
                        // Auto-navigate to analytics after successful manual scan
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (getActivity() != null) {
                                Log.d(TAG, "Navigating to animal history page...");
                                navigateToAnalytics();
                            }
                        }, 3000); // Wait 3 seconds to show results
                        
                        return; // Skip the separate diagnosis step
                    }
                    
                    // Fallback: Extract temperatures and do separate diagnosis (old workflow)
                    Log.d(TAG, "No diagnosis in response, doing separate diagnosis step");
                    
                    // Step 3: Extract temperatures from thermal data
                    Map<String, ApiService.TemperatureStats> temperatures = 
                        ThermalExtractor.extractTemperatures(thermalData, response.bodyParts);
                    
                    Log.d(TAG, "Extracted temperatures for " + temperatures.size() + " body parts");
                    
                    // Calculate average temperature for display
                    float avgTemp = calculateAverageTemperature(temperatures);
                    
                    // Update UI
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            tempText.setText(String.format(Locale.getDefault(), "%.1f °C", avgTemp));
                            statusText.setText("Diagnosing health...");
                        });
                    }
                    
                    // Step 4: Send for diagnosis (with environmental data)
                    float ambientTemp = getAmbientTemperature();
                    float humidity = getRelativeHumidity();
                    
                    ApiService.getInstance().diagnose(
                        response.scanId,
                        TEST_ANIMAL_ID,
                        temperatures,
                        ambientTemp,  // NEW: Environmental data
                        humidity,     // NEW: Environmental data
                        new ApiService.DiagnoseCallback() {
                            @Override
                            public void onSuccess(ApiService.DiagnosisResponse diagnosis) {
                                Log.d(TAG, "Diagnosis successful. Status: " + diagnosis.status);
                                Log.d(TAG, "Alerts: " + diagnosis.alerts.length);
                                Log.d(TAG, "Recommendations: " + diagnosis.recommendations.length);
                                
                                currentDiagnosis = diagnosis;
                                
                                // Step 5: Display results
                                displayResults(diagnosis, avgTemp, response.bodyParts.size());
                                
                                // Auto-navigate to analytics after successful manual scan
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    if (getActivity() != null) {
                                        Log.d(TAG, "Navigating to animal history page...");
                                        navigateToAnalytics();
                                    }
                                }, 3000); // Wait 3 seconds to show results
                            }
                            
                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "Diagnosis error: " + error);
                                showError("Diagnosis failed: " + error);
                            }
                        }
                    );
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Analysis error: " + error);
                    showError("Analysis failed: " + error);
                }
            }
        );
    }
    
    /**
     * Capture current frame from camera (auto-detects real vs simulation)
     */
    private File captureFrame() {
        try {
            // NEW: Use CameraManager (auto-detects mode)
            Bitmap bitmap = cameraManager.captureFrame();
            
            if (bitmap == null) {
                Log.e(TAG, "Failed to capture frame - bitmap is null");
                return null;
            }
            
            Log.d(TAG, "Bitmap captured: " + bitmap.getWidth() + "x" + bitmap.getHeight());
            
            // Save to file
            File cacheDir = requireContext().getCacheDir();
            File imageFile = new File(cacheDir, "captured_frame_" + System.currentTimeMillis() + ".jpg");
            
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();
            
            Log.d(TAG, "Image file created: " + imageFile.getAbsolutePath());
            Log.d(TAG, "File exists: " + imageFile.exists());
            Log.d(TAG, "File size: " + imageFile.length() + " bytes");
            
            if (!imageFile.exists() || imageFile.length() == 0) {
                Log.e(TAG, "Image file is empty or doesn't exist!");
                return null;
            }
            
            if (!bitmap.isRecycled()) {
                bitmap.recycle();
            }
            
            Log.d(TAG, "Captured frame successfully (Mode: " + cameraManager.getCurrentMode() + ")");
            return imageFile;
            
        } catch (Exception e) {
            Log.e(TAG, "Error capturing frame", e);
            return null;
        }
    }
    
    /**
     * Capture thermal data from camera (auto-detects real vs simulation)
     */
    private float[][] captureThermalData() {
        // NEW: Use CameraManager (auto-detects mode)
        float[][] thermalData = cameraManager.captureThermalData();
        
        if (thermalData != null) {
            Log.d(TAG, "Captured thermal data: " + thermalData.length + "x" + thermalData[0].length +
                  " (Mode: " + cameraManager.getCurrentMode() + ")");
        }
        
        return thermalData;
    }
    
    /**
     * Get environmental data for diagnosis
     */
    private float getAmbientTemperature() {
        return cameraManager.getAmbientTemperature();
    }
    
    private float getRelativeHumidity() {
        return cameraManager.getRelativeHumidity();
    }
    
    /**
     * Calculate average temperature from all body parts
     */
    private float calculateAverageTemperature(Map<String, ApiService.TemperatureStats> temperatures) {
        if (temperatures.isEmpty()) {
            return 0;
        }
        
        float sum = 0;
        for (ApiService.TemperatureStats stats : temperatures.values()) {
            sum += stats.tempMean;
        }
        
        return sum / temperatures.size();
    }
    
    /**
     * Display diagnosis results in UI
     */
    private void displayResults(ApiService.DiagnosisResponse diagnosis, float avgTemp, int bodyPartsCount) {
        if (getActivity() == null) return;
        
        getActivity().runOnUiThread(() -> {
            String time = new SimpleDateFormat(
                "dd MMM · hh:mm a", Locale.getDefault()
            ).format(new Date());
            
            lastScan = new ScanResult(avgTemp, "", time);
            
            // Use settings-based thresholds for status determination
            float normalMax = SettingsFragment.getNormalThresholdMax(requireContext());
            float elevatedMax = SettingsFragment.getElevatedThresholdMax(requireContext());
            
            // Display status based on diagnosis and temperature thresholds
            if ("healthy".equals(diagnosis.status)) {
                lastScan.status = "Healthy";
                statusText.setText("✓ Healthy");
                statusText.setTextColor(0xFF10B981); // Green
                shareButton.setVisibility(View.GONE);
            } else {
                // Check temperature thresholds for more specific status
                if (avgTemp > elevatedMax) {
                    lastScan.status = "High Risk";
                    statusText.setText("🚨 High Risk");
                    statusText.setTextColor(0xFFEF5350); // Red
                } else if (avgTemp > normalMax) {
                    lastScan.status = "Attention Needed";
                    statusText.setText("⚠ Attention Needed");
                    statusText.setTextColor(0xFFFFA726); // Orange
                } else {
                    lastScan.status = "Monitor";
                    statusText.setText("👁 Monitor");
                    statusText.setTextColor(0xFF42A5F5); // Blue
                }
                shareButton.setVisibility(View.VISIBLE);
            }
            
            // Display recommendations
            if (diagnosis.recommendations.length > 0) {
                StringBuilder recommendations = new StringBuilder();
                for (int i = 0; i < Math.min(2, diagnosis.recommendations.length); i++) {
                    if (i > 0) recommendations.append("\n\n");
                    recommendations.append(diagnosis.recommendations[i]);
                }
                explanationText.setText(recommendations.toString());
            } else {
                explanationText.setText("No abnormal patterns detected.\nDetected " + bodyPartsCount + " body parts.");
            }
            
            // Save scan if auto-save is enabled
            if (SettingsFragment.isAutoSaveEnabled(requireContext())) {
                ScanStorage.saveScan(requireContext(), lastScan, TEST_ANIMAL_ID);
            }
            
            // Play sound alert if enabled and there are issues
            if (SettingsFragment.isSoundAlertsEnabled(requireContext()) && 
                diagnosis.alerts != null && diagnosis.alerts.length > 0) {
                // Could add sound playback here
                android.util.Log.d(TAG, "Sound alert triggered for " + diagnosis.alerts.length + " alerts");
            }
            
            // Vibrate if enabled and there are issues
            if (SettingsFragment.isVibrationEnabled(requireContext()) && 
                diagnosis.alerts != null && diagnosis.alerts.length > 0) {
                android.os.Vibrator vibrator = (android.os.Vibrator) 
                    requireContext().getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null) {
                    vibrator.vibrate(500); // 500ms vibration
                }
            }
            
            // Restart animation
            Animation pulse = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse);
            overlay.startAnimation(pulse);
            
            Log.d(TAG, "Results displayed successfully with settings-based thresholds");
        });
    }
    
    /**
     * Show error message
     */
    private void showError(String error) {
        if (getActivity() == null) return;
        
        getActivity().runOnUiThread(() -> {
            statusText.setText("Error");
            statusText.setTextColor(0xFFEF5350); // Red
            explanationText.setText(error);
            
            Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            
            // Restart animation
            Animation pulse = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse);
            overlay.startAnimation(pulse);
        });
    }
    
    /**
     * Share scan report
     */
    private void shareScanReport() {
        if (lastScan == null || currentDiagnosis == null) {
            Toast.makeText(getContext(), "No scan data to share", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build detailed report
        StringBuilder report = new StringBuilder();
        report.append("🐄 FLIR Cattle Health Monitoring Report\n");
        report.append("==========================================\n\n");
        report.append("Animal ID: ").append(TEST_ANIMAL_ID).append("\n");
        report.append("Scan ID: ").append(currentScanId).append("\n");
        report.append("Time: ").append(lastScan.time).append("\n");
        report.append("Average Temperature: ")
            .append(String.format(Locale.getDefault(), "%.1f °C", lastScan.temperature))
            .append("\n\n");
        
        report.append("Status: ").append(lastScan.status).append("\n\n");
        
        if (currentDiagnosis.alerts.length > 0) {
            report.append("Alerts:\n");
            for (String alert : currentDiagnosis.alerts) {
                report.append("  • ").append(alert).append("\n");
            }
            report.append("\n");
        }
        
        if (currentDiagnosis.recommendations.length > 0) {
            report.append("Recommendations:\n");
            for (int i = 0; i < currentDiagnosis.recommendations.length; i++) {
                report.append("  ").append(i + 1).append(". ")
                    .append(currentDiagnosis.recommendations[i]).append("\n");
            }
        }
        
        report.append("\n==========================================\n");
        report.append("Generated by FLIR Cattle Health Monitoring System\n");

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Cattle Health Scan Report - " + TEST_ANIMAL_ID);
        intent.putExtra(Intent.EXTRA_TEXT, report.toString());

        startActivity(Intent.createChooser(intent, "Share report via"));
    }
    
    /**
     * Start video preview loop
     */
    private void startVideoPreview() {
        if (isVideoPlaying) {
            return;
        }
        
        isVideoPlaying = true;
        videoUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isVideoPlaying && cameraManager != null) {
                    Bitmap frame = cameraManager.captureFrame();
                    if (frame != null && videoPreview != null) {
                        videoPreview.setImageBitmap(frame);
                    }
                    
                    // Update at ~15 FPS (66ms delay) to reduce CPU load
                    videoUpdateHandler.postDelayed(this, 66);
                }
            }
        };
        videoUpdateHandler.post(videoUpdateRunnable);
        Log.d(TAG, "Video preview started");
    }
    
    /**
     * Stop video preview loop
     */
    private void stopVideoPreview() {
        isVideoPlaying = false;
        if (videoUpdateRunnable != null) {
            videoUpdateHandler.removeCallbacks(videoUpdateRunnable);
        }
        Log.d(TAG, "Video preview stopped");
    }
    
    /**
     * Start auto-scan system - captures and analyzes frames every 15 seconds
     */
    private void startAutoScan() {
        if (isAutoScanEnabled) {
            return;
        }
        
        isAutoScanEnabled = true;
        autoScanRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAutoScanEnabled && cameraManager != null) {
                    Log.d(TAG, "Auto-scan: Capturing frame for analysis");
                    
                    // Update UI to show auto-scan
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            statusText.setText("Auto-scanning...");
                            explanationText.setText("Analyzing current frame");
                        });
                    }
                    
                    // Perform automatic scan
                    performAutoScan();
                    
                    // Schedule next auto-scan
                    autoScanHandler.postDelayed(this, AUTO_SCAN_INTERVAL_MS);
                }
            }
        };
        
        // Start first auto-scan after 5 seconds (give time for video to start)
        autoScanHandler.postDelayed(autoScanRunnable, 5000);
        Log.d(TAG, "Auto-scan started - will scan every " + (AUTO_SCAN_INTERVAL_MS / 1000) + " seconds");
    }
    
    /**
     * Stop auto-scan system
     */
    private void stopAutoScan() {
        isAutoScanEnabled = false;
        if (autoScanRunnable != null) {
            autoScanHandler.removeCallbacks(autoScanRunnable);
        }
        Log.d(TAG, "Auto-scan stopped");
    }
    
    /**
     * Perform automatic scan without user interaction
     */
    private void performAutoScan() {
        // Capture current frame and thermal data
        File imageFile = captureFrame();
        float[][] thermalData = captureThermalData();
        
        if (imageFile == null || thermalData == null) {
            Log.w(TAG, "Auto-scan: Failed to capture data");
            return;
        }
        
        Log.d(TAG, "Auto-scan: Captured " + imageFile.length() + " byte image");
        
        // Send to backend for analysis
        ApiService.getInstance().analyzeImage(imageFile, thermalData, TEST_ANIMAL_ID, new ApiService.AnalyzeCallback() {
            @Override
            public void onSuccess(ApiService.AnalyzeResponse response) {
                Log.d(TAG, "Auto-scan: Analysis successful - " + response.scanId);
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        int bodyPartsCount = response.bodyParts != null ? response.bodyParts.size() : 0;
                        String thermalInfo = response.thermalData != null ? 
                            " | " + response.thermalData.size() + " thermal readings" : "";
                        String diagnosisInfo = response.diagnosis != null ? 
                            " | Status: " + response.diagnosis.status : "";
                        
                        statusText.setText("✓ Complete Analysis");
                        explanationText.setText("Found " + bodyPartsCount + " body parts" + 
                                              thermalInfo + diagnosisInfo);
                        
                        // Show temperature if available
                        if (response.thermalData != null && !response.thermalData.isEmpty()) {
                            // Find highest temperature for display
                            float maxTemp = 0;
                            for (Map.Entry<String, ApiService.TemperatureStats> entry : response.thermalData.entrySet()) {
                                maxTemp = Math.max(maxTemp, entry.getValue().tempMax);
                            }
                            tempText.setText(String.format("%.1f °C", maxTemp));
                            
                            // Check for alerts and play sound/vibration if enabled
                            if (response.diagnosis != null && response.diagnosis.alerts != null && 
                                response.diagnosis.alerts.length > 0) {
                                
                                // Play sound alert if enabled
                                if (SettingsFragment.isSoundAlertsEnabled(requireContext())) {
                                    // Could add sound alert here
                                }
                                
                                // Vibrate if enabled
                                if (SettingsFragment.isVibrationEnabled(requireContext())) {
                                    android.os.Vibrator vibrator = (android.os.Vibrator) 
                                        requireContext().getSystemService(Context.VIBRATOR_SERVICE);
                                    if (vibrator != null) {
                                        vibrator.vibrate(500); // 500ms vibration
                                    }
                                }
                            }
                        }
                        
                        // Clear status after 5 seconds
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (statusText != null) {
                                statusText.setText("");
                                explanationText.setText("Monitoring cattle health...");
                                tempText.setText("--.- °C");
                            }
                        }, 5000);
                    });
                }
                
                // Store scan data
                currentScanId = response.scanId;
                
                // Auto-scan only does detection, not full diagnosis
                // Full diagnosis can be triggered manually with SCAN button
            }
            
            @Override
            public void onError(String error) {
                Log.w(TAG, "Auto-scan: Analysis failed - " + error);
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        statusText.setText("⚠ Scan Failed");
                        explanationText.setText("Will retry in " + (AUTO_SCAN_INTERVAL_MS / 1000) + "s");
                        
                        // Clear status after 3 seconds
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (statusText != null) {
                                statusText.setText("");
                                explanationText.setText("Monitoring cattle health...");
                            }
                        }, 3000);
                    });
                }
            }
        });
    }
    
    /**
     * Navigate to animal history page for the specific animal
     */
    private void navigateToAnalytics() {
        try {
            Log.d(TAG, "=== NAVIGATION DEBUG ===");
            Log.d(TAG, "Attempting to navigate to AnimalHistoryFragment for " + TEST_ANIMAL_ID);
            
            // Navigate directly to the specific animal's history page (this is correct behavior)
            Fragment animalHistoryFragment = AnimalHistoryFragment.newInstance(TEST_ANIMAL_ID, "Dairy Cattle");
            
            Log.d(TAG, "Created AnimalHistoryFragment instance: " + animalHistoryFragment.getClass().getSimpleName());
            
            requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out,
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
                .replace(R.id.fragment_container, animalHistoryFragment)
                .addToBackStack(null)
                .commit();
                
            Log.d(TAG, "✓ Navigation transaction committed to AnimalHistoryFragment for " + TEST_ANIMAL_ID);
        } catch (Exception e) {
            Log.e(TAG, "✗ Failed to navigate to analytics", e);
        }
    }
    
    /**
     * Update video info display
     */
    private void updateVideoInfo() {
        if (cameraManager != null && videoInfoText != null) {
            String info = cameraManager.getSimulationVideoInfo();
            videoInfoText.setText(info);
        }
    }
    
    // ========== LIFECYCLE METHODS ==========
    
    @Override
    public void onResume() {
        super.onResume();
        
        // Reinitialize camera if needed
        if (cameraManager != null && !cameraManager.isInitialized()) {
            initializeCamera();
        }
        
        // Restart video preview and auto-scan if in simulation mode
        if (cameraManager != null && 
            cameraManager.getCurrentMode() == CameraManager.CameraMode.SIMULATION && 
            cameraManager.hasSimulationVideos()) {
            startVideoPreview();
            startAutoScan();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        
        // Stop video preview and auto-scan to save resources
        stopVideoPreview();
        stopAutoScan();
        
        // Disconnect camera to save resources
        if (cameraManager != null) {
            cameraManager.disconnect();
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Stop video preview and auto-scan
        stopVideoPreview();
        stopAutoScan();
        
        // Cleanup
        if (cameraManager != null) {
            cameraManager.disconnect();
            cameraManager = null;
        }
    }
}
