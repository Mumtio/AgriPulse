package com.flir.atlassdk.acecamerasample;

enum RiskStatus {
    NORMAL, SUSPECTED, ELEVATED
}

enum TrendStatus {
    IMPROVING, DECLINING, STABLE
}

class Keypoint {
    String name;      // "left_eye", "udder", "hoof_fl" ...
    float x;          // screen or thermal frame coords
    float y;
    float confidence; // 0..1

    Keypoint(String name, float x, float y, float confidence) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.confidence = confidence;
    }
}

class ROIResult {
    String name;         // "Udder", "Eyes", "Hooves"
    double meanTempC;
    double t95TempC;
    double stdDev;
    RiskStatus status;
    String reason;       // "Udder ΔT 2.3°C above ambient"

    ROIResult(String name,
              double meanTempC,
              double t95TempC,
              double stdDev,
              RiskStatus status,
              String reason) {

        this.name = name;
        this.meanTempC = meanTempC;
        this.t95TempC = t95TempC;
        this.stdDev = stdDev;
        this.status = status;
        this.reason = reason;
    }
}
