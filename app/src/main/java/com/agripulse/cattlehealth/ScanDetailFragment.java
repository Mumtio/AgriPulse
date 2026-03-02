package com.agripulse.cattlehealth;


import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.agripulse.cattlehealth.R;
import com.agripulse.cattlehealth.api.ApiService;

import java.util.Map;

public class ScanDetailFragment extends Fragment {

    private static final String ARG_SCAN_ID = "scan_id";
    private static final String ARG_TEMP = "temp";
    private static final String ARG_STATUS = "status";
    private static final String ARG_TIME = "time";

    public static ScanDetailFragment newInstance(String scanId) {
        ScanDetailFragment fragment = new ScanDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SCAN_ID, scanId);
        fragment.setArguments(args);
        return fragment;
    }

    public static ScanDetailFragment newInstance(double temp, String status, String time) {
        ScanDetailFragment fragment = new ScanDetailFragment();
        Bundle args = new Bundle();
        args.putDouble(ARG_TEMP, temp);
        args.putString(ARG_STATUS, status);
        args.putString(ARG_TIME, time);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_scan_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton backButton = view.findViewById(R.id.backButton);
        View statusIndicator = view.findViewById(R.id.statusIndicator);
        TextView tempText = view.findViewById(R.id.tempText);
        TextView statusText = view.findViewById(R.id.statusText);
        TextView timeText = view.findViewById(R.id.timeText);
        TextView guidanceText = view.findViewById(R.id.guidanceText);

        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        // Check if we have a scan ID to fetch detailed data
        String scanId = getArguments().getString(ARG_SCAN_ID);
        if (scanId != null) {
            loadScanDetails(scanId, statusIndicator, tempText, statusText, timeText, guidanceText);
        } else {
            // Use provided data (legacy mode)
            double temp = getArguments().getDouble(ARG_TEMP);
            String status = getArguments().getString(ARG_STATUS);
            String time = getArguments().getString(ARG_TIME);

            displayScanData(temp, status, time, statusIndicator, tempText, statusText, timeText, guidanceText);
        }
    }
    
    private void loadScanDetails(String scanId, View statusIndicator, TextView tempText, 
                                TextView statusText, TextView timeText, TextView guidanceText) {
        // Show loading state
        tempText.setText("Loading...");
        statusText.setText("Loading...");
        timeText.setText("Loading...");
        guidanceText.setText("Fetching scan details...");
        
        ApiService.getInstance().getScanDetails(scanId, new ApiService.ScanDetailsCallback() {
            @Override
            public void onSuccess(ApiService.ScanDetails details) {
                if (getActivity() == null) return;
                
                getActivity().runOnUiThread(() -> {
                    // Calculate average temperature from all body parts
                    float avgTemp = 0.0f;
                    StringBuilder tempDetails = new StringBuilder();
                    
                    if (details.temperatures != null && !details.temperatures.isEmpty()) {
                        float sum = 0.0f;
                        float maxTemp = 0.0f;
                        
                        for (Map.Entry<String, Float> entry : details.temperatures.entrySet()) {
                            float temp = entry.getValue();
                            sum += temp;
                            maxTemp = Math.max(maxTemp, temp);
                            
                            tempDetails.append(entry.getKey()).append(": ")
                                      .append(String.format("%.1f°C", temp)).append("\n");
                        }
                        avgTemp = sum / details.temperatures.size();
                        
                        // Show max temperature as primary reading
                        avgTemp = maxTemp;
                    } else {
                        avgTemp = 38.0f; // Default
                        tempDetails.append("No thermal data available");
                    }
                    
                    // Map diagnosis status to display status
                    String displayStatus = mapDiagnosisStatus(details.diagnosisStatus, avgTemp);
                    
                    // Enhanced guidance with body part details and recommendations
                    StringBuilder enhancedGuidance = new StringBuilder();
                    enhancedGuidance.append("Body Part Temperatures:\n");
                    enhancedGuidance.append(tempDetails.toString()).append("\n");
                    
                    if (details.recommendations != null && !details.recommendations.isEmpty()) {
                        enhancedGuidance.append("Recommendations:\n");
                        for (String recommendation : details.recommendations) {
                            enhancedGuidance.append("• ").append(recommendation).append("\n");
                        }
                    } else {
                        enhancedGuidance.append(getGuidanceForStatus(displayStatus));
                    }
                    
                    displayScanData(avgTemp, displayStatus, formatTimestamp(details.timestamp), 
                                  statusIndicator, tempText, statusText, timeText, guidanceText);
                    
                    // Update guidance with detailed information
                    guidanceText.setText(enhancedGuidance.toString());
                });
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() == null) return;
                
                getActivity().runOnUiThread(() -> {
                    tempText.setText("--.-°C");
                    statusText.setText("Error");
                    timeText.setText("Failed to load");
                    guidanceText.setText("Failed to load scan details: " + error);
                });
            }
        });
    }
    
    private void displayScanData(double temp, String status, String time, View statusIndicator, 
                               TextView tempText, TextView statusText, TextView timeText, TextView guidanceText) {
        tempText.setText(String.format("%.1f°C", temp));
        statusText.setText(status);
        timeText.setText(time);
        
        if (guidanceText.getText().toString().equals("Fetching scan details...")) {
            guidanceText.setText(getGuidanceForStatus(status));
        }

        applyStatusStyle(statusIndicator, tempText, statusText, status);
    }
    
    private String formatTimestamp(String isoTimestamp) {
        try {
            // Convert ISO timestamp to readable format
            // For now, return a simple format - could be enhanced with proper date parsing
            return "Recent scan";
        } catch (Exception e) {
            return "Unknown time";
        }
    }
    
    private String mapDiagnosisStatus(String diagnosisStatus, float temperature) {
        if (diagnosisStatus != null) {
            switch (diagnosisStatus.toLowerCase()) {
                case "healthy":
                    return "Normal";
                case "attention_needed":
                    return "Attention Needed";
                default:
                    break;
            }
        }
        
        // Fall back to temperature-based status
        if (temperature >= 39.5) {
            return "High";
        } else if (temperature >= 38.8) {
            return "Elevated";
        } else {
            return "Normal";
        }
    }


    private void applyStatusStyle(View indicator, TextView tempText, TextView statusText, String status) {
        switch (status) {
            case "High":
                indicator.setBackgroundResource(R.drawable.status_dot_red);
                tempText.setTextColor(0xFFD32F2F);
                statusText.setBackgroundResource(R.drawable.status_badge_red);
                statusText.setText("High Risk");
                break;

            case "Elevated":
                indicator.setBackgroundResource(R.drawable.status_dot_amber);
                tempText.setTextColor(0xFFF57C00);
                statusText.setBackgroundResource(R.drawable.status_badge_amber);
                statusText.setText("Elevated");
                break;

            default:
                indicator.setBackgroundResource(R.drawable.status_dot_green);
                tempText.setTextColor(0xFF2E7D32);
                statusText.setBackgroundResource(R.drawable.status_badge_green);
                statusText.setText("Normal");
                break;
        }
    }

    private String getGuidanceForStatus(String status) {
        switch (status) {
            case "Attention Needed":
                return "This reading indicates potential health concerns.\n\n"
                        + "Recommended actions:\n"
                        + "• Monitor closely over next 24 hours\n"
                        + "• Check for signs of mastitis, lameness, or fever\n"
                        + "• Consider veterinary consultation if symptoms persist";

            case "High":
                return "This reading is notably above normal.\n\n"
                        + "Immediate actions recommended:\n"
                        + "• Isolate animal for closer observation\n"
                        + "• Check for visible symptoms (swelling, limping, discharge)\n"
                        + "• Contact veterinarian for professional assessment";

            case "Elevated":
                return "This reading is slightly above normal.\n\n"
                        + "Recommended actions:\n"
                        + "• Repeat scanning in 2-4 hours\n"
                        + "• Monitor eating and drinking behavior\n"
                        + "• Watch for changes in activity level";

            default:
                return "This reading falls within the normal range.\n\n"
                        + "Current status:\n"
                        + "• No immediate action required\n"
                        + "• Continue regular monitoring schedule\n"
                        + "• Maintain standard care routine";
        }
    }


}
