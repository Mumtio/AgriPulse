package com.flir.atlassdk.acecamerasample.batch;

import com.flir.atlassdk.acecamerasample.storage.ScanRecord;
import java.util.ArrayList;
import java.util.List;

public class BatchResult {
    
    public long batchId;
    public long timestamp;
    public int totalAnimals;
    public int normalCount;
    public int suspectedCount;
    public List<ScanRecord> scans;
    
    public BatchResult() {
        this.scans = new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Add scan to batch
     */
    public void addScan(ScanRecord scan) {
        scans.add(scan);
        totalAnimals = scans.size();
        
        if ("NORMAL".equals(scan.overallStatus)) {
            normalCount++;
        } else if ("SUSPECTED".equals(scan.overallStatus)) {
            suspectedCount++;
        }
    }
    
    /**
     * Get suspected rate
     */
    public double getSuspectedRate() {
        if (totalAnimals == 0) return 0.0;
        return (double) suspectedCount / totalAnimals;
    }
    
    @Override
    public String toString() {
        return String.format("Batch #%d: %d animals (%d normal, %d suspected) - %.0f%% suspected",
            batchId, totalAnimals, normalCount, suspectedCount, getSuspectedRate() * 100);
    }
}
