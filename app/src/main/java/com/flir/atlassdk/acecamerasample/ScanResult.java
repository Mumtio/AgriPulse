package com.flir.atlassdk.acecamerasample;

import java.util.ArrayList;
import java.util.List;

class ScanResult {

    // ===== EXISTING UI FIELDS (KEEP WORKING) =====
    double temperature;     // e.g. 39.7
    String status;          // "High", "Normal"
    String time;            // "12 Feb · 6:45 AM"

    // ===== STRONG INTERNAL REPRESENTATION =====
    RiskStatus riskStatus;  // NORMAL / SUSPECTED
    long timestampMillis;  // for sorting & trends
    double ambientTempC;   // used for ΔT reasoning

    // ===== ADVANCED DATA (USED STEP BY STEP) =====
    List<Keypoint> keypoints = new ArrayList<>();
    List<ROIResult> rois = new ArrayList<>();

    Double latitude = null;
    Double longitude = null;

    String reportText = null;   // share with vet
    String csvPath = null;      // exported CSV

    // ===== CONSTRUCTOR (BACKWARD SAFE) =====
    ScanResult(double temperature, String status, String time) {
        this.temperature = temperature;
        this.status = status;
        this.time = time;

        // auto-map old string status to enum
        this.riskStatus =
                status.equalsIgnoreCase("High")
                        ? RiskStatus.SUSPECTED
                        : RiskStatus.NORMAL;

        this.timestampMillis = System.currentTimeMillis();
        this.ambientTempC = temperature - 2.0; // temporary default
    }
}
