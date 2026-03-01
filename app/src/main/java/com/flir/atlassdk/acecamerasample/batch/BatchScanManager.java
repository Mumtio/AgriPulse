package com.flir.atlassdk.acecamerasample.batch;

import android.util.Log;
import com.flir.atlassdk.acecamerasample.detection.DetectionResult;
import com.flir.atlassdk.acecamerasample.storage.ScanRecord;
import java.util.ArrayList;
import java.util.List;

public class BatchScanManager {
    
    private static final String TAG = "BatchScanManager";
    
    private boolean batchModeActive = false;
    private List<DetectionResult> detectionQueue;
    private int currentAnimalIndex = 0;
    private BatchResult currentBatch;
    private long batchIdCounter = 1;
    
    public BatchScanManager() {
        this.detectionQueue = new ArrayList<>();
    }
    
    /**
     * Start batch scan with multiple animals
     */
    public void startBatchScan(List<DetectionResult> detections) {
        this.detectionQueue = new ArrayList<>(detections);
        this.currentAnimalIndex = 0;
        this.batchModeActive = true;
        this.currentBatch = new BatchResult();
        this.currentBatch.batchId = batchIdCounter++;
        
        Log.d(TAG, "Batch scan started: " + detectionQueue.size() + " animals detected");
    }
    
    /**
     * Get next animal to scan
     */
    public DetectionResult getNextAnimal() {
        if (!batchModeActive || currentAnimalIndex >= detectionQueue.size()) {
            return null;
        }
        
        DetectionResult result = detectionQueue.get(currentAnimalIndex);
        currentAnimalIndex++;
        
        Log.d(TAG, "Processing animal " + currentAnimalIndex + " of " + detectionQueue.size());
        
        return result;
    }
    
    /**
     * Add completed scan to batch
     */
    public void addScanToBatch(ScanRecord scan) {
        if (currentBatch != null) {
            currentBatch.addScan(scan);
            Log.d(TAG, "Added scan to batch: " + scan.animalId);
        }
    }
    
    /**
     * Check if batch is complete
     */
    public boolean isBatchComplete() {
        return batchModeActive && currentAnimalIndex >= detectionQueue.size();
    }
    
    /**
     * Finish batch and get results
     */
    public BatchResult finishBatch() {
        batchModeActive = false;
        BatchResult result = currentBatch;
        
        Log.d(TAG, "Batch scan complete: " + result.toString());
        
        // Reset for next batch
        detectionQueue.clear();
        currentAnimalIndex = 0;
        currentBatch = null;
        
        return result;
    }
    
    /**
     * Get current progress
     */
    public String getProgress() {
        if (!batchModeActive) {
            return "No batch scan active";
        }
        return String.format("Animal %d of %d", currentAnimalIndex, detectionQueue.size());
    }
    
    /**
     * Check if batch mode is active
     */
    public boolean isBatchModeActive() {
        return batchModeActive;
    }
    
    /**
     * Get total animals in current batch
     */
    public int getTotalAnimals() {
        return detectionQueue.size();
    }
    
    /**
     * Get current animal index (1-based)
     */
    public int getCurrentAnimalNumber() {
        return currentAnimalIndex;
    }
}
