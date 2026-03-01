package com.flir.atlassdk.acecamerasample.storage;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple JSON-based storage for scan records
 */
public class ScanStorage {
    
    private static final String TAG = "ScanStorage";
    private static final String SCANS_FILE = "scans.json";
    private static final String THERMAL_IMAGES_DIR = "thermal_snapshots";
    
    private final Context context;
    private final Gson gson;
    private long nextScanId = 1;
    
    public ScanStorage(Context context) {
        this.context = context;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        
        // Create thermal images directory
        File thermalDir = new File(context.getFilesDir(), THERMAL_IMAGES_DIR);
        if (!thermalDir.exists()) {
            thermalDir.mkdirs();
        }
        
        // Load existing scans to determine next ID
        List<ScanRecord> existing = loadAllScans();
        if (!existing.isEmpty()) {
            for (ScanRecord record : existing) {
                if (record.scanId >= nextScanId) {
                    nextScanId = record.scanId + 1;
                }
            }
        }
    }
    
    /**
     * Save a new scan record
     */
    public long saveScan(ScanRecord record) {
        try {
            // Assign scan ID
            record.scanId = nextScanId++;
            record.timestamp = System.currentTimeMillis();
            
            // Load existing scans
            List<ScanRecord> scans = loadAllScans();
            
            // Add new scan
            scans.add(record);
            
            // Save to file
            File scansFile = new File(context.getFilesDir(), SCANS_FILE);
            try (FileWriter writer = new FileWriter(scansFile)) {
                gson.toJson(scans, writer);
            }
            
            Log.d(TAG, "Saved scan #" + record.scanId);
            return record.scanId;
            
        } catch (IOException e) {
            Log.e(TAG, "Error saving scan: " + e.getMessage());
            return -1;
        }
    }
    
    /**
     * Get a specific scan by ID
     */
    public ScanRecord getScan(long scanId) {
        List<ScanRecord> scans = loadAllScans();
        for (ScanRecord record : scans) {
            if (record.scanId == scanId) {
                return record;
            }
        }
        return null;
    }
    
    /**
     * Get all scans
     */
    public List<ScanRecord> getAllScans() {
        return loadAllScans();
    }
    
    /**
     * Get scans in date range
     */
    public List<ScanRecord> getScansInRange(long startTime, long endTime) {
        List<ScanRecord> allScans = loadAllScans();
        List<ScanRecord> filtered = new ArrayList<>();
        
        for (ScanRecord record : allScans) {
            if (record.timestamp >= startTime && record.timestamp <= endTime) {
                filtered.add(record);
            }
        }
        
        return filtered;
    }
    
    /**
     * Get scans for a specific animal
     */
    public List<ScanRecord> getScansForAnimal(String animalId) {
        List<ScanRecord> allScans = loadAllScans();
        List<ScanRecord> filtered = new ArrayList<>();
        
        for (ScanRecord record : allScans) {
            if (animalId.equals(record.animalId)) {
                filtered.add(record);
            }
        }
        
        return filtered;
    }
    
    /**
     * Delete a scan
     */
    public boolean deleteScan(long scanId) {
        try {
            List<ScanRecord> scans = loadAllScans();
            ScanRecord toDelete = null;
            
            for (ScanRecord record : scans) {
                if (record.scanId == scanId) {
                    toDelete = record;
                    break;
                }
            }
            
            if (toDelete != null) {
                scans.remove(toDelete);
                
                // Delete thermal snapshot if exists
                if (toDelete.thermalSnapshotPath != null) {
                    File snapshot = new File(toDelete.thermalSnapshotPath);
                    if (snapshot.exists()) {
                        snapshot.delete();
                    }
                }
                
                // Save updated list
                File scansFile = new File(context.getFilesDir(), SCANS_FILE);
                try (FileWriter writer = new FileWriter(scansFile)) {
                    gson.toJson(scans, writer);
                }
                
                Log.d(TAG, "Deleted scan #" + scanId);
                return true;
            }
            
            return false;
            
        } catch (IOException e) {
            Log.e(TAG, "Error deleting scan: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get path for saving thermal snapshot
     */
    public String getThermalSnapshotPath(long scanId) {
        File thermalDir = new File(context.getFilesDir(), THERMAL_IMAGES_DIR);
        return new File(thermalDir, "thermal_" + scanId + ".png").getAbsolutePath();
    }
    
    /**
     * Get total number of scans
     */
    public int getScanCount() {
        return loadAllScans().size();
    }
    
    /**
     * Clear all scans (for testing)
     */
    public void clearAllScans() {
        try {
            File scansFile = new File(context.getFilesDir(), SCANS_FILE);
            if (scansFile.exists()) {
                scansFile.delete();
            }
            
            // Delete all thermal snapshots
            File thermalDir = new File(context.getFilesDir(), THERMAL_IMAGES_DIR);
            if (thermalDir.exists()) {
                File[] files = thermalDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        file.delete();
                    }
                }
            }
            
            nextScanId = 1;
            Log.d(TAG, "Cleared all scans");
            
        } catch (Exception e) {
            Log.e(TAG, "Error clearing scans: " + e.getMessage());
        }
    }
    
    /**
     * Load all scans from JSON file
     */
    private List<ScanRecord> loadAllScans() {
        File scansFile = new File(context.getFilesDir(), SCANS_FILE);
        
        if (!scansFile.exists()) {
            return new ArrayList<>();
        }
        
        try (FileReader reader = new FileReader(scansFile)) {
            Type listType = new TypeToken<List<ScanRecord>>(){}.getType();
            List<ScanRecord> scans = gson.fromJson(reader, listType);
            return scans != null ? scans : new ArrayList<>();
        } catch (IOException e) {
            Log.e(TAG, "Error loading scans: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}
