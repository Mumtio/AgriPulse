package com.flir.atlassdk.acecamerasample.tracking;

import com.flir.atlassdk.acecamerasample.storage.ScanRecord;
import java.util.ArrayList;
import java.util.List;

public class AnimalProfile {
    
    public String animalId;
    public String species;
    public long firstSeenTimestamp;
    public long lastSeenTimestamp;
    public int totalScans;
    public int normalScans;
    public int suspectedScans;
    public List<ScanRecord> scanHistory;
    
    public AnimalProfile(String animalId) {
        this.animalId = animalId;
        this.scanHistory = new ArrayList<>();
    }
    
    /**
     * Add scan to profile
     */
    public void addScan(ScanRecord scan) {
        scanHistory.add(scan);
        
        // Update statistics
        totalScans = scanHistory.size();
        
        if ("NORMAL".equals(scan.overallStatus)) {
            normalScans++;
        } else if ("SUSPECTED".equals(scan.overallStatus)) {
            suspectedScans++;
        }
        
        // Update timestamps
        if (firstSeenTimestamp == 0 || scan.timestamp < firstSeenTimestamp) {
            firstSeenTimestamp = scan.timestamp;
        }
        if (scan.timestamp > lastSeenTimestamp) {
            lastSeenTimestamp = scan.timestamp;
        }
        
        // Update species
        if (species == null) {
            species = scan.species;
        }
    }
    
    /**
     * Get health trend
     */
    public String getHealthTrend() {
        if (scanHistory.size() < 2) {
            return "INSUFFICIENT_DATA";
        }
        
        // Compare last 2 scans
        ScanRecord recent = scanHistory.get(scanHistory.size() - 1);
        ScanRecord previous = scanHistory.get(scanHistory.size() - 2);
        
        boolean recentSuspected = "SUSPECTED".equals(recent.overallStatus);
        boolean previousSuspected = "SUSPECTED".equals(previous.overallStatus);
        
        if (!recentSuspected && previousSuspected) {
            return "IMPROVING";
        } else if (recentSuspected && !previousSuspected) {
            return "DECLINING";
        } else if (recentSuspected && previousSuspected) {
            return "PERSISTENT_ISSUE";
        } else {
            return "STABLE";
        }
    }
    
    /**
     * Get suspected rate
     */
    public double getSuspectedRate() {
        if (totalScans == 0) return 0.0;
        return (double) suspectedScans / totalScans;
    }
    
    @Override
    public String toString() {
        return String.format("Animal %s: %d scans (%d normal, %d suspected), Trend: %s",
            animalId, totalScans, normalScans, suspectedScans, getHealthTrend());
    }
}
