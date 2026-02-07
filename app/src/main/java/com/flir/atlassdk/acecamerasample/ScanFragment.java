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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ScanFragment extends Fragment {

    private static final String TEST_ANIMAL_ID = "DC-014";

    private OverlayView overlayView;
    private Button shareButton;
    private ScanResult lastScan;

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

        View overlay = view.findViewById(R.id.scanOverlay);
        overlayView = view.findViewById(R.id.overlayView);

        TextView explanationText = view.findViewById(R.id.textExplanation);
        TextView statusText = view.findViewById(R.id.textScanResult);
        TextView tempText = view.findViewById(R.id.tempText);

        Button scanButton = view.findViewById(R.id.buttonScan);
        shareButton = view.findViewById(R.id.buttonShare);
        ImageButton backButton = view.findViewById(R.id.buttonBack);

        explanationText.setText("");
        statusText.setText("");
        tempText.setText("--.- °C");
        shareButton.setVisibility(View.GONE);

        Animation pulse = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse);
        overlay.startAnimation(pulse);

        scanButton.setOnClickListener(v -> {
            statusText.setText("Analyzing...");
            explanationText.setText("");
            overlay.clearAnimation();

            double temp = 38.0 + Math.random() * 2.0;
            tempText.setText(String.format(Locale.getDefault(), "%.1f °C", temp));

            view.postDelayed(() -> {

                // ===== 1. CREATE SCAN RESULT =====
                String time = new SimpleDateFormat(
                        "dd MMM · hh:mm a", Locale.getDefault()
                ).format(new Date());

                lastScan = new ScanResult(temp, "", time);
                lastScan.keypoints = new ArrayList<>();
                lastScan.rois = new ArrayList<>();

                // ===== 2. DETERMINE STATUS =====
                if (temp < 38.5) {
                    lastScan.status = "Normal";
                    lastScan.riskStatus = RiskStatus.NORMAL;
                    statusText.setText("✓ Normal temperature");
                } else if (temp < 39.5) {
                    lastScan.status = "Elevated";
                    lastScan.riskStatus = RiskStatus.ELEVATED ;
                    statusText.setText("⚠ Slightly elevated");
                } else {
                    lastScan.status = "High";
                    lastScan.riskStatus = RiskStatus.SUSPECTED;
                    statusText.setText("🚨 High temperature detected");
                }

                // ===== 3. MOCK KEYPOINTS =====
                lastScan.keypoints.add(new Keypoint("udder", 540f, 820f, 0.9f));
                lastScan.keypoints.add(new Keypoint("hoof_fl", 360f, 1040f, 0.85f));

                // ===== 4. MOCK ROI =====
                lastScan.rois.add(new ROIResult(
                        "Udder",
                        lastScan.temperature,
                        lastScan.temperature + 0.3,
                        0.2,
                        lastScan.riskStatus,
                        "ΔT above ambient"
                ));

                // ===== 5. EXPLANATION =====
                StringBuilder explanation = new StringBuilder();

                for (ROIResult roi : lastScan.rois) {
                    if (roi.status == RiskStatus.SUSPECTED) {
                        explanation.append("• ")
                                .append(roi.reason)
                                .append("\n");
                    }
                }

                if (explanation.length() == 0) {
                    explanationText.setText("No abnormal patterns detected");
                } else {
                    explanationText.setText("Why?\n" + explanation);
                }

                // ===== 6. OVERLAY UPDATE =====
                overlayView.update(lastScan.keypoints, lastScan.rois);

                // ===== 7. BUILD REPORT =====
                StringBuilder report = new StringBuilder();
                report.append("AgriPulse Thermal Scan Report\n");
                report.append("--------------------------------\n");
                report.append("Animal ID: ").append(TEST_ANIMAL_ID).append("\n");
                report.append("Temperature: ")
                        .append(String.format("%.1f °C", lastScan.temperature)).append("\n");
                report.append("Status: ").append(lastScan.status).append("\n");
                report.append("Time: ").append(lastScan.time).append("\n\n");
                report.append("Findings:\n");

                for (ROIResult roi : lastScan.rois) {
                    report.append("- ")
                            .append(roi.name)
                            .append(": ")
                            .append(String.format("%.1f °C", roi.meanTempC))
                            .append(" (")
                            .append(roi.status)
                            .append(")\n");
                }

                lastScan.reportText = report.toString();

                // ===== 8. SAVE =====
                ScanStorage.saveScan(requireContext(), lastScan, TEST_ANIMAL_ID);

                // ===== 9. SHARE BUTTON =====
                shareButton.setVisibility(
                        lastScan.riskStatus == RiskStatus.SUSPECTED
                                ? View.VISIBLE
                                : View.GONE
                );

                overlay.startAnimation(pulse);

            }, 1500);
        });

        shareButton.setOnClickListener(v -> {
            if (lastScan == null || lastScan.reportText == null) return;

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT,
                    "AgriPulse – Suspected Fever Alert");
            intent.putExtra(Intent.EXTRA_TEXT,
                    lastScan.reportText);

            startActivity(Intent.createChooser(intent, "Share report via"));
        });

        backButton.setOnClickListener(v ->
                requireActivity()
                        .getSupportFragmentManager()
                        .popBackStack()
        );
    }
}
