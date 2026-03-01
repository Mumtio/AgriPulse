package com.flir.atlassdk.acecamerasample.health;

import com.flir.atlassdk.acecamerasample.thermal.ROITemperature;
import java.util.HashMap;
import java.util.Map;

public class FeverDetector {
    
    // Thresholds in Kelvin
    private static final double UDDER_THRESHOLD = 2.0;  // °C above ambient
    private static final double EYE_THRESHOLD = 1.0;    // °C above ambient
    private static final double HOOF_ASYMMETRY_THRESHOLD = 1.5;  // °C difference
    
    /**
     * Detect fever based on body part temperatures
     */
    public HealthStatus detectFever(Map<String, ROITemperature> bodyParts) {
        // Estimate ambient temperature (use coldest body part as reference)
        double ambient = estimateAmbient(bodyParts);
        
        // Check udder
        ROITemperature udder = bodyParts.get("udder");
        if (udder != null && udder.mean > ambient + UDDER_THRESHOLD) {
            double deltaT = udder.mean - ambient;
            return new HealthStatus(
                "SUSPECTED",
                "Elevated udder temperature (ΔT=" + String.format("%.1f", deltaT) + "K)",
                0.85
            );
        }
        
        // Check eyes
        ROITemperature leftEye = bodyParts.get("left_eye");
        ROITemperature rightEye = bodyParts.get("right_eye");
        if (leftEye != null && rightEye != null) {
            double avgEyeTemp = (leftEye.mean + rightEye.mean) / 2;
            if (avgEyeTemp > ambient + EYE_THRESHOLD) {
                double deltaT = avgEyeTemp - ambient;
                return new HealthStatus(
                    "SUSPECTED",
                    "Elevated eye temperature (ΔT=" + String.format("%.1f", deltaT) + "K)",
                    0.78
                );
            }
        }
        
        // Check hoof asymmetry (front hooves)
        ROITemperature leftFrontHoof = bodyParts.get("left_front_hoof");
        ROITemperature rightFrontHoof = bodyParts.get("right_front_hoof");
        if (leftFrontHoof != null && rightFrontHoof != null) {
            double asymmetry = Math.abs(leftFrontHoof.mean - rightFrontHoof.mean);
            if (asymmetry > HOOF_ASYMMETRY_THRESHOLD) {
                return new HealthStatus(
                    "SUSPECTED",
                    "Front hoof temperature asymmetry (" + String.format("%.1f", asymmetry) + "K)",
                    0.72
                );
            }
        }
        
        // Check hoof asymmetry (rear hooves)
        ROITemperature leftRearHoof = bodyParts.get("left_rear_hoof");
        ROITemperature rightRearHoof = bodyParts.get("right_rear_hoof");
        if (leftRearHoof != null && rightRearHoof != null) {
            double asymmetry = Math.abs(leftRearHoof.mean - rightRearHoof.mean);
            if (asymmetry > HOOF_ASYMMETRY_THRESHOLD) {
                return new HealthStatus(
                    "SUSPECTED",
                    "Rear hoof temperature asymmetry (" + String.format("%.1f", asymmetry) + "K)",
                    0.70
                );
            }
        }
        
        // All checks passed - animal appears healthy
        return new HealthStatus(
            "NORMAL",
            "All body part temperatures within normal range",
            0.90
        );
    }
    
    /**
     * Estimate ambient temperature using coldest body part
     */
    private double estimateAmbient(Map<String, ROITemperature> bodyParts) {
        double minTemp = Double.MAX_VALUE;
        
        for (ROITemperature roi : bodyParts.values()) {
            if (roi.mean < minTemp) {
                minTemp = roi.mean;
            }
        }
        
        // If no valid temperature found, use a default
        return minTemp == Double.MAX_VALUE ? 295.0 : minTemp;  // ~22°C default
    }
}
