package com.flir.atlassdk.acecamerasample;

public class Animal {
    public String id;
    public String type;
    public double lastTemp;
    public String status;
    public String lastScanTime;

    public Animal(String id, String type, double lastTemp, String status, String lastScanTime) {
        this.id = id;
        this.type = type;
        this.lastTemp = lastTemp;
        this.status = status;
        this.lastScanTime = lastScanTime;
    }
}
