package com.flir.atlassdk.acecamerasample.storage;

import android.content.Context;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Demo class showing how to use ScanStorage
 * Person A can use these examples for UI integration
 */
public class ScanStorageDemo {
    
    private static final String TAG = "ScanStorageDemo";
    
    /**
     * Example: Get all scans and print them
     */
    public static void printAllScans(Context context) {
        ScanStorage storage = new ScanStorage(context);
        List<ScanRecord> scans = storage.getAllScans();
        
        Log.d(TAG, "=== All Scans (" + scans.size() + ") ===");
        
        for (ScanRecord scan : scans) {
            printScan(scan);
        }
    }
    
    /**
     * Example: Get scans for a specific animal
     */
    public static void printScansForAnimal(Context context, String animalId) {
        ScanStorage storage = new ScanStorage(context);
        List<ScanRecord> scans = storage.getScansForAnimal(animalId);
        
        Log.d(TAG, "=== Scans for " + animalId + " (" + scans.size() + ") ===");
        
        for (ScanRecord scan : scans) {
            printScan(scan);
        }
    }
    
    /**
     * Example: Get scans from today
     */
    public static void printTodaysScans(Context context) {
        ScanStorage storage = new ScanStorage(context);
        
        // Get start of today (midnight)
        long startOfDay = getStartOfDay(System.currentTimeMillis());
        long endOfDay = startOfDay + 24 * 60 * 60 * 1000;
        
        List<ScanRecord> scans = storage.getScansInRange(startOfDay, endOfDay);
        
        Log.d(TAG, "=== Today's Scans (" + scans.size() + ") ===");
        
        for (ScanRecord scan : scans) {
            printScan(scan);
        }
    }
    
    /**
     * Example: Get latest scan
     */
    public static ScanRecord getLatestScan(Context context) {
        ScanStorage storage = new ScanStorage(context);
        List<ScanRecord> scans = storage.getAllScans();
        
        if (scans.isEmpty()) {
            return null;
        }
        
        // Find most recent scan
        ScanRecord latest = scans.get(0);
        for (ScanRecord scan : scans) {
            if (scan.timestamp > latest.timestamp) {
                latest = scan;
            }
        }
        
        return latest;
    }
    
    /**
     * Example: Count suspected vs normal scans
     */
    public static void printScanStatistics(Context context) {
        ScanStorage storage = new ScanStorage(context);
        List<ScanRecord> scans = storage.getAllScans();
        
        int normalCount = 0;
        int suspectedCount = 0;
        
        for (ScanRecord scan : scans) {
            if ("NORMAL".equals(scan.overallStatus)) {
                normalCount++;
            } else if ("SUSPECTED".equals(scan.overallStatus)) {
                suspectedCount++;
            }
        }
        
        Log.d(TAG, "=== Scan Statistics ===");
        Log.d(TAG, "Total scans: " + scans.size());
        Log.d(TAG, "Normal: " + normalCount);
        Log.d(TAG, "Suspected: " + suspectedCount);
        Log.d(TAG, "Suspected rate: " + String.format("%.1f%%", 
            (suspectedCount * 100.0 / Math.max(1, scans.size()))));
    }
    
    /**
     * Print a single scan record
     */
    private static void printScan(ScanRecord scan) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        String dateStr = sdf.format(new Date(scan.timestamp));
        
        Log.d(TAG, "Scan #" + scan.scanId + " - " + dateStr);
        Log.d(TAG, "  Animal: " + scan.animalId + " (" + scan.species + ")");
        Log.d(TAG, "  Status: " + scan.overallStatus + " (" + 
              String.format("%.0f%%", scan.confidence * 100) + ")");
        Log.d(TAG, "  Reason: " + scan.statusReason);
        Log.d(TAG, "  Body parts: " + scan.bodyParts.size());
        
        for (ScanRecord.BodyPartData part : scan.bodyParts.values()) {
            Log.d(TAG, "    " + part.name + ": T95=" + 
                  String.format("%.2f", part.t95) + "K, Mean=" + 
                  String.format("%.2f", part.mean) + "K");
        }
    }
    
    /**
     * Get start of day (midnight) for a timestamp
     */
    private static long getStartOfDay(long timestamp) {
        long dayInMillis = 24 * 60 * 60 * 1000;
        return (timestamp / dayInMillis) * dayInMillis;
    }
}
