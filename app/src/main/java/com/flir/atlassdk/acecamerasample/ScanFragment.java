package com.flir.atlassdk.acecamerasample;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.flir.atlassdk.acecamerasample.storage.ScanRecord;
import com.flir.atlassdk.acecamerasample.storage.RiskStatus;

import java.io.File;
import java.util.Locale;

public class ScanFragment extends Fragment {

    private OverlayViewFrontend overlayView;
    private Button shareButton;
    private Button detailsButton;
    private View secondaryActions;
    private View statusIndicator;
    private ScanRecord lastScan;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_scan_new, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Show thermal camera view when entering scan screen
        MainActivity mainActivity = (MainActivity) requireActivity();
        mainActivity.showThermalView(true);

        View overlay = view.findViewById(R.id.scanOverlay);
        overlayView = view.findViewById(R.id.overlayView);

        TextView explanationText = view.findViewById(R.id.textExplanation);
        TextView statusText = view.findViewById(R.id.textScanResult);
        TextView tempText = view.findViewById(R.id.tempText);

        Button scanButton = view.findViewById(R.id.buttonScan);
        shareButton = view.findViewById(R.id.buttonShare);
        detailsButton = view.findViewById(R.id.buttonDetails);
        secondaryActions = view.findViewById(R.id.secondaryActions);
        statusIndicator = view.findViewById(R.id.statusIndicator);
        ImageButton backButton = view.findViewById(R.id.buttonBack);

        explanationText.setText("");
        statusText.setText("Ready to scan");
        tempText.setText("--.- °C");
        secondaryActions.setVisibility(View.GONE);

        Animation pulse = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse);
        overlay.startAnimation(pulse);

        scanButton.setOnClickListener(v -> {
            // Disable button during scan
            scanButton.setEnabled(false);
            scanButton.setText("SCANNING...");
            
            statusText.setText("Analyzing thermal data...");
            explanationText.setText("");
            tempText.setText("--.- °C");
            overlay.clearAnimation();
            
            // Request scan from MainActivity backend
            mainActivity.requestScan(new MainActivity.ScanCallback() {
                @Override
                public void onScanComplete(ScanRecord result) {
                    // Re-enable button
                    scanButton.setEnabled(true);
                    scanButton.setText("SCAN");
                    
                    // Store result
                    lastScan = result;
                    
                    Log.d("ScanFragment", "Scan complete: " + result.animalId + 
                          ", temp=" + result.temperature + "°C, status=" + result.overallStatus);
                    
                    // Update temperature display
                    tempText.setText(String.format(Locale.getDefault(), 
                        "%.1f °C", result.temperature));
                    
                    // Update status based on backend result
                    if (result.overallStatus.equals("SUSPECTED")) {
                        statusText.setText("🚨 " + result.statusReason);
                        statusText.setTextColor(0xFFEF4444);  // Red
                        statusIndicator.setBackgroundResource(R.drawable.status_dot_red);
                        secondaryActions.setVisibility(View.VISIBLE);
                    } else {
                        statusText.setText("✓ Normal temperature");
                        statusText.setTextColor(0xFF10B981);  // Green
                        statusIndicator.setBackgroundResource(R.drawable.status_dot_green);
                        secondaryActions.setVisibility(View.VISIBLE);
                    }
                    
                    // Build explanation from ROIs
                    StringBuilder explanation = new StringBuilder();
                    if (result.rois != null && !result.rois.isEmpty()) {
                        for (ScanRecord.ROIResult roi : result.rois) {
                            if (roi.status == RiskStatus.SUSPECTED) {
                                explanation.append("• ")
                                    .append(roi.reason)
                                    .append("\n");
                            }
                        }
                    }
                    
                    if (explanation.length() == 0) {
                        explanationText.setText("No abnormal patterns detected");
                    } else {
                        explanationText.setText("Why?\n" + explanation);
                    }
                    
                    // Update overlay with real keypoints and ROIs
                    if (result.keypoints != null && result.rois != null) {
                        overlayView.update(result.keypoints, result.rois);
                        Log.d("ScanFragment", "Overlay updated: " + result.keypoints.size() + 
                              " keypoints, " + result.rois.size() + " ROIs");
                    }
                    
                    // Show location info in explanation if available
                    if (result.latitude != 0 && result.longitude != 0) {
                        String locationStr = String.format(Locale.US, 
                            "\n📍 Location: %.4f°, %.4f°", 
                            result.latitude, result.longitude);
                        explanationText.append(locationStr);
                    }
                    
                    // Restart animation
                    overlay.startAnimation(pulse);
                    
                    Log.d("ScanFragment", "UI update complete");
                }
                
                @Override
                public void onScanError(String error) {
                    // Re-enable button
                    scanButton.setEnabled(true);
                    scanButton.setText("SCAN");
                    
                    Log.e("ScanFragment", "Scan error: " + error);
                    statusText.setText("Error: " + error);
                    statusText.setTextColor(0xFFEF5350);  // Red
                    explanationText.setText("Please try again");
                    tempText.setText("--.- °C");
                    overlay.startAnimation(pulse);
                }
            });
        });

        shareButton.setOnClickListener(v -> {
            if (lastScan == null) {
                Log.w("ScanFragment", "No scan data to share");
                return;
            }

            // Show export options dialog
            showExportDialog(lastScan);
        });

        detailsButton.setOnClickListener(v -> {
            if (lastScan == null) {
                Log.w("ScanFragment", "No scan data to show");
                return;
            }

            // Show detailed information dialog
            showDetailsDialog(lastScan);
        });

        backButton.setOnClickListener(v ->
                requireActivity()
                        .getSupportFragmentManager()
                        .popBackStack()
        );
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Hide thermal view when leaving scan screen
        MainActivity mainActivity = (MainActivity) requireActivity();
        mainActivity.showThermalView(false);
    }
    
    /**
     * Show export options dialog
     */
    private void showExportDialog(ScanRecord scan) {
        MainActivity mainActivity = (MainActivity) requireActivity();
        com.flir.atlassdk.acecamerasample.export.ScanExporter exporter = mainActivity.getScanExporter();
        
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Share Scan Report");
        builder.setMessage("Choose export format:");
        
        // Option 1: Share via apps (text + attachment)
        builder.setPositiveButton("Share via Apps", (dialog, which) -> {
            try {
                exporter.shareViaIntent(scan);
                Toast.makeText(requireContext(), "Opening share menu...", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e("ScanFragment", "Share error: " + e.getMessage());
                Toast.makeText(requireContext(), "Share failed: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        });
        
        // Option 2: Export to CSV
        builder.setNeutralButton("Export CSV", (dialog, which) -> {
            try {
                File csvFile = exporter.exportToCSV(scan);
                if (csvFile != null) {
                    Toast.makeText(requireContext(), 
                        "CSV exported: " + csvFile.getName(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(requireContext(), "CSV export failed", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e("ScanFragment", "CSV export error: " + e.getMessage());
                Toast.makeText(requireContext(), "Export failed: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        });
        
        // Option 3: Export to Text
        builder.setNegativeButton("Export Text", (dialog, which) -> {
            try {
                File textFile = exporter.exportToText(scan);
                if (textFile != null) {
                    Toast.makeText(requireContext(), 
                        "Report exported: " + textFile.getName(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(requireContext(), "Text export failed", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e("ScanFragment", "Text export error: " + e.getMessage());
                Toast.makeText(requireContext(), "Export failed: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.show();
    }
    
    /**
     * Show detailed scan information dialog
     */
    private void showDetailsDialog(ScanRecord scan) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Scan Details - " + scan.animalId);
        
        StringBuilder details = new StringBuilder();
        
        // Animal Info
        details.append("═══ ANIMAL INFO ═══\n");
        details.append("ID: ").append(scan.animalId).append("\n");
        details.append("Species: ").append(scan.species != null ? scan.species : "Cattle").append("\n");
        details.append("Scan Time: ").append(new java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", 
            Locale.US).format(new java.util.Date(scan.time))).append("\n\n");
        
        // Location Info
        details.append("═══ LOCATION ═══\n");
        if (scan.latitude != 0 && scan.longitude != 0) {
            details.append(String.format(Locale.US, "Latitude: %.6f°\n", scan.latitude));
            details.append(String.format(Locale.US, "Longitude: %.6f°\n", scan.longitude));
        } else {
            details.append("Location not available\n");
        }
        details.append("\n");
        
        // Health Status
        details.append("═══ HEALTH STATUS ═══\n");
        details.append("Overall: ").append(scan.overallStatus).append("\n");
        details.append("Confidence: ").append(String.format(Locale.US, "%.1f%%", 
            scan.confidence * 100)).append("\n");
        if (scan.statusReason != null && !scan.statusReason.isEmpty()) {
            details.append("Reason: ").append(scan.statusReason).append("\n");
        }
        details.append("\n");
        
        // Body Part Analysis
        details.append("═══ BODY PART ANALYSIS ═══\n");
        if (scan.rois != null && !scan.rois.isEmpty()) {
            for (ScanRecord.ROIResult roi : scan.rois) {
                details.append("\n").append(roi.name.toUpperCase()).append(":\n");
                details.append(String.format(Locale.US, "  Temp: %.1f°C\n", roi.meanTempC));
                details.append(String.format(Locale.US, "  Peak: %.1f°C\n", roi.t95TempC));
                details.append(String.format(Locale.US, "  Std Dev: %.2f\n", roi.stdDev));
                details.append("  Status: ").append(roi.status).append("\n");
                if (roi.reason != null && !roi.reason.isEmpty()) {
                    details.append("  Note: ").append(roi.reason).append("\n");
                }
            }
        } else {
            details.append("No detailed body part data available\n");
        }
        
        builder.setMessage(details.toString());
        builder.setPositiveButton("Close", null);
        builder.setNeutralButton("Copy to Clipboard", (dialog, which) -> {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) 
                requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText(
                "Scan Details", details.toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(requireContext(), "Details copied to clipboard", 
                Toast.LENGTH_SHORT).show();
        });
        
        builder.show();
    }
}
