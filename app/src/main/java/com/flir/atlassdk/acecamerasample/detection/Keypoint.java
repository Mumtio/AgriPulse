package com.flir.atlassdk.acecamerasample.detection;

public class Keypoint {
    public String name;
    public int x;
    public int y;
    public float confidence;
    
    public Keypoint(String name, int x, int y, float confidence) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.confidence = confidence;
    }
    
    @Override
    public String toString() {
        return name + " (" + x + "," + y + ") conf=" + String.format("%.2f", confidence);
    }
}
