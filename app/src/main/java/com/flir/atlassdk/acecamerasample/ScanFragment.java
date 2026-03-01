package com.flir.atlassdk.acecamerasample;

import android.content.Intent;
import android.os.Bundle;
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
import java.util.Date;
import java.util.Locale;

public class ScanFragment extends Fragment {

    private static final String TEST_ANIMAL_ID = "DC-014";

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
                String time = new SimpleDateFormat(
                        "dd MMM · hh:mm a", Locale.getDefault()
                ).format(new Date());

                lastScan = new ScanResult(temp, "", time);

                // Determine status
                if (temp < 38.5) {
                    lastScan.status = "Normal";
                    statusText.setText("✓ Normal temperature");
                    statusText.setTextColor(0xFF10B981);
                    shareButton.setVisibility(View.GONE);
                } else if (temp < 39.5) {
                    lastScan.status = "Elevated";
                    statusText.setText("⚠ Slightly elevated");
                    statusText.setTextColor(0xFFFFA726);
                    shareButton.setVisibility(View.VISIBLE);
                } else {
                    lastScan.status = "High";
                    statusText.setText("🚨 High temperature detected");
                    statusText.setTextColor(0xFFEF5350);
                    shareButton.setVisibility(View.VISIBLE);
                }

                // Simple explanation
                if (temp >= 38.5) {
                    explanationText.setText("Temperature above normal range.\nMonitor animal closely.");
                } else {
                    explanationText.setText("No abnormal patterns detected");
                }

                // Save scan
                ScanStorage.saveScan(requireContext(), lastScan, TEST_ANIMAL_ID);

                overlay.startAnimation(pulse);

            }, 1500);
        });

        shareButton.setOnClickListener(v -> {
            if (lastScan == null) return;

            // Build simple report
            StringBuilder report = new StringBuilder();
            report.append("AgriPulse Thermal Scan Report\n");
            report.append("--------------------------------\n");
            report.append("Animal ID: ").append(TEST_ANIMAL_ID).append("\n");
            report.append("Temperature: ")
                    .append(String.format("%.1f °C", lastScan.temperature)).append("\n");
            report.append("Status: ").append(lastScan.status).append("\n");
            report.append("Time: ").append(lastScan.time).append("\n");

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, "AgriPulse Scan Report");
            intent.putExtra(Intent.EXTRA_TEXT, report.toString());

            startActivity(Intent.createChooser(intent, "Share report via"));
        });

        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }
}
