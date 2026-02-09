package com.flir.atlassdk.acecamerasample.thermal;

public class ROITemperature {
    public String bodyPart;
    public double t95;      // 95th percentile temperature
    public double mean;     // Average temperature
    public double std;      // Standard deviation
    public int sampleCount; // Number of pixels sampled
    
    public ROITemperature(String bodyPart, double t95, double mean, double std, int sampleCount) {
        this.bodyPart = bodyPart;
        this.t95 = t95;
        this.mean = mean;
        this.std = std;
        this.sampleCount = sampleCount;
    }
    
    @Override
    public String toString() {
        return bodyPart + ": T95=" + String.format("%.2f", t95) + "K, " +
               "Mean=" + String.format("%.2f", mean) + "K, " +
               "Std=" + String.format("%.2f", std) + "K, " +
               "Samples=" + sampleCount;
    }
}
