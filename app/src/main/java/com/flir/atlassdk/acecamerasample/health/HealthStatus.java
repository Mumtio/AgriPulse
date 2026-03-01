package com.flir.atlassdk.acecamerasample.health;

public class HealthStatus {
    public String status;      // "NORMAL" or "SUSPECTED"
    public String reason;      // Why it's suspected
    public double confidence;  // 0.0 to 1.0
    
    public HealthStatus(String status, String reason, double confidence) {
        this.status = status;
        this.reason = reason;
        this.confidence = confidence;
    }
    
    @Override
    public String toString() {
        return status + " (" + String.format("%.0f", confidence * 100) + "%) - " + reason;
    }
}
