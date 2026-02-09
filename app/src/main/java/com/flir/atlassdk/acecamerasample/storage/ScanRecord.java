package com.flir.atlassdk.acecamerasample.storage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Unified ScanRecord class - combines frontend UI needs with backend data storage
 * Replaces both old ScanResult (frontend) and old ScanRecord (backend)
 */
public class ScanRecord {
    
    // ===== CORE BACKEND FIELDS =====
    public long scanId;
    public long timestamp;
    public String animalId;
    public String species;
    public Map<String, BodyPartData> bodyParts;
    public String overallStatus;      // "NORMAL" or "SUSPECTED"
    public String statusReason;
    public double confidence;
    public String thermalSnapshotPath;
    public double latitude;
    public double longitude;
    
    // ===== UI FIELDS (for frontend compatibility) =====
    public double temperature;     // Main display temperature (Celsius)
    public String status;          // "Normal", "High", "Elevated" (for UI)
    public String time;            // Formatted time string "12 Feb · 6:45 AM"
    public RiskStatus riskStatus;  // NORMAL / SUSPECTED / ELEVATED
    public double ambientTempC;    // Ambient temperature
    
    // ===== VISUAL DATA (for overlays) =====
    public List<com.flir.atlassdk.acecamerasample.detection.Keypoint> keypoints = new ArrayList<>();
    public List<ROIResult> rois = new ArrayList<>();
    
    // ===== EXPORT DATA =====
    public String reportText;      // Formatted text report
    public String csvPath;         // Path to exported CSV
    
    /**
     * Default constructor for JSON deserialization
     */
    public ScanRecord() {
        this.keypoints = new ArrayList<>();
        this.rois = new ArrayList<>();
        this.status = "Normal";  // Default status
        this.overallStatus = "NORMAL";  // Default overall status
    }
    
    /**
     * Full constructor for backend creation
     */
    public ScanRecord(long scanId, long timestamp, String animalId, String species,
                     Map<String, BodyPartData> bodyParts, String overallStatus,
                     String statusReason, double confidence, String thermalSnapshotPath,
                     double latitude, double longitude) {
        this.scanId = scanId;
        this.timestamp = timestamp;
        this.animalId = animalId;
        this.species = species;
        this.bodyParts = bodyParts;
        this.overallStatus = overallStatus;
        this.statusReason = statusReason;
        this.confidence = confidence;
        this.thermalSnapshotPath = thermalSnapshotPath;
        this.latitude = latitude;
        this.longitude = longitude;
        
        // Auto-populate UI fields
        this.temperature = calculateMainTemp();
        this.status = mapStatusToUI(overallStatus);
        this.time = formatTime(timestamp);
        this.riskStatus = mapStatusToRisk(overallStatus);
        this.ambientTempC = estimateAmbient();
        
        // Initialize lists
        this.keypoints = new ArrayList<>();
        this.rois = new ArrayList<>();
    }
    
    /**
     * Simple constructor for frontend compatibility (backward compatible with old ScanResult)
     */
    public ScanRecord(double temperature, String status, String time) {
        this.temperature = temperature;
        this.status = status;
        this.time = time;
        
        // Auto-map to backend fields
        this.overallStatus = status.equalsIgnoreCase("High") ? "SUSPECTED" : "NORMAL";
        this.riskStatus = status.equalsIgnoreCase("High") ? RiskStatus.SUSPECTED : RiskStatus.NORMAL;
        this.timestampMillis = System.currentTimeMillis();
        this.timestamp = this.timestampMillis;
        this.ambientTempC = temperature - 2.0;
        
        // Initialize lists
        this.keypoints = new ArrayList<>();
        this.rois = new ArrayList<>();
    }
    
    /**
     * Calculate main display temperature from body parts
     */
    private double calculateMainTemp() {
        if (bodyParts == null || bodyParts.isEmpty()) {
            return 0.0;
        }
        
        // Prefer udder temperature as main display
        BodyPartData udder = bodyParts.get("udder");
        if (udder != null) {
            return kelvinToCelsius(udder.mean);
        }
        
        // Fallback to first body part
        BodyPartData first = bodyParts.values().iterator().next();
        return kelvinToCelsius(first.mean);
    }
    
    /**
     * Map backend status to UI status
     */
    private String mapStatusToUI(String backendStatus) {
        if ("SUSPECTED".equals(backendStatus)) {
            return "High";
        }
        return "Normal";
    }
    
    /**
     * Map backend status to RiskStatus enum
     */
    private RiskStatus mapStatusToRisk(String backendStatus) {
        if ("SUSPECTED".equals(backendStatus)) {
            return RiskStatus.SUSPECTED;
        }
        return RiskStatus.NORMAL;
    }
    
    /**
     * Format timestamp to readable string
     */
    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM · hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
    
    /**
     * Estimate ambient temperature
     */
    private double estimateAmbient() {
        if (bodyParts == null || bodyParts.isEmpty()) {
            return 20.0; // Default room temp
        }
        
        // Use coldest body part as ambient estimate
        double minTemp = Double.MAX_VALUE;
        for (BodyPartData part : bodyParts.values()) {
            double tempC = kelvinToCelsius(part.mean);
            if (tempC < minTemp) {
                minTemp = tempC;
            }
        }
        
        return minTemp - 5.0; // Ambient is typically 5°C below coldest body part
    }
    
    /**
     * Convert Kelvin to Celsius
     */
    private double kelvinToCelsius(double kelvin) {
        return kelvin - 273.15;
    }
    
    /**
     * Body part temperature data
     */
    public static class BodyPartData {
        public String name;
        public double t95;      // 95th percentile temperature (Kelvin)
        public double mean;     // Mean temperature (Kelvin)
        public double std;      // Standard deviation
        public int sampleCount; // Number of samples analyzed
        
        public BodyPartData() {
            // Default constructor for JSON deserialization
        }
        
        public BodyPartData(String name, double t95, double mean, double std, int sampleCount) {
            this.name = name;
            this.t95 = t95;
            this.mean = mean;
            this.std = std;
            this.sampleCount = sampleCount;
        }
        
        /**
         * Get mean temperature in Celsius
         */
        public double getMeanCelsius() {
            return mean - 273.15;
        }
        
        /**
         * Get T95 temperature in Celsius
         */
        public double getT95Celsius() {
            return t95 - 273.15;
        }
    }
    
    /**
     * ROI Result for visual display
     */
    public static class ROIResult {
        public String name;         // "Udder", "Eyes", "Hooves"
        public double meanTempC;    // Mean temperature in Celsius
        public double t95TempC;     // 95th percentile in Celsius
        public double stdDev;       // Standard deviation
        public RiskStatus status;   // NORMAL / SUSPECTED / ELEVATED
        public String reason;       // "Udder ΔT 2.3°C above ambient"
        
        public ROIResult(String name, double meanTempC, double t95TempC, 
                        double stdDev, RiskStatus status, String reason) {
            this.name = name;
            this.meanTempC = meanTempC;
            this.t95TempC = t95TempC;
            this.stdDev = stdDev;
            this.status = status;
            this.reason = reason;
        }
    }
    
    // Backward compatibility field (some old code might use this)
    private long timestampMillis;
    
    public long getTimestampMillis() {
        return timestamp;
    }
    
    public void setTimestampMillis(long timestampMillis) {
        this.timestamp = timestampMillis;
        this.timestampMillis = timestampMillis;
    }
}
