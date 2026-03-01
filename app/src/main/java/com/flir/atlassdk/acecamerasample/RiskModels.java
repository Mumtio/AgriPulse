package com.flir.atlassdk.acecamerasample;

// Package-private classes - only accessible within the same package
// These are kept here for backward compatibility with existing code in the main package

enum TrendStatus {
    IMPROVING, DECLINING, STABLE
}

class Keypoint {
    public String name;      // "left_eye", "udder", "hoof_fl" ...
    public float x;          // screen or thermal frame coords
    public float y;
    public float confidence; // 0..1

    public Keypoint(String name, float x, float y, float confidence) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.confidence = confidence;
    }
}

class ROIResult {
    public String name;         // "Udder", "Eyes", "Hooves"
    public double meanTempC;
    public double t95TempC;
    public double stdDev;
    public com.flir.atlassdk.acecamerasample.storage.RiskStatus status;
    public String reason;       // "Udder ΔT 2.3°C above ambient"

    public ROIResult(String name,
              double meanTempC,
              double t95TempC,
              double stdDev,
              com.flir.atlassdk.acecamerasample.storage.RiskStatus status,
              String reason) {

        this.name = name;
        this.meanTempC = meanTempC;
        this.t95TempC = t95TempC;
        this.stdDev = stdDev;
        this.status = status;
        this.reason = reason;
    }
}
