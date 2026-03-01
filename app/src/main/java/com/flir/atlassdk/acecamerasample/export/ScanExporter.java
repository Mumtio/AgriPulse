package com.flir.atlassdk.acecamerasample.export;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import androidx.core.content.FileProvider;

import com.flir.atlassdk.acecamerasample.storage.ScanRecord;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ScanExporter {
    
    private static final String TAG = "ScanExporter";
    private final Context context;
    
    public ScanExporter(Context context) {
        this.context = context;
    }
    
    /**
     * Export scan to CSV file
     */
    public File exportToCSV(ScanRecord scan) {
        try {
            // Create exports directory
            File exportsDir = new File(context.getFilesDir(), "exports");
            if (!exportsDir.exists()) {
                exportsDir.mkdirs();
            }
            
            // Create CSV file
            String filename = "scan_" + scan.scanId + "_" + scan.animalId + ".csv";
            File csvFile = new File(exportsDir, filename);
            
            // Write CSV content
            try (FileWriter writer = new FileWriter(csvFile)) {
                writer.write(ReportFormatter.formatCSV(scan));
            }
            
            Log.d(TAG, "Exported CSV: " + csvFile.getAbsolutePath());
            return csvFile;
            
        } catch (IOException e) {
            Log.e(TAG, "Error exporting CSV: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Export scan to text file
     */
    public File exportToText(ScanRecord scan) {
        try {
            // Create exports directory
            File exportsDir = new File(context.getFilesDir(), "exports");
            if (!exportsDir.exists()) {
                exportsDir.mkdirs();
            }
            
            // Create text file
            String filename = "report_" + scan.scanId + "_" + scan.animalId + ".txt";
            File textFile = new File(exportsDir, filename);
            
            // Write text content with UTF-8 encoding
            try (FileWriter writer = new FileWriter(textFile, java.nio.charset.StandardCharsets.UTF_8)) {
                writer.write(ReportFormatter.formatTextReport(scan));
            }
            
            Log.d(TAG, "Exported text report: " + textFile.getAbsolutePath());
            return textFile;
            
        } catch (IOException e) {
            Log.e(TAG, "Error exporting text: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Share scan via Android Intent (email, WhatsApp, etc.)
     */
    public void shareViaIntent(ScanRecord scan) {
        try {
            // Export to text file first
            File reportFile = exportToText(scan);
            if (reportFile == null) {
                Log.e(TAG, "Failed to create report file");
                return;
            }
            
            // Get URI for file (using FileProvider for security)
            Uri fileUri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                reportFile
            );
            
            // Create share intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, 
                "AgriPulse Scan Report - " + scan.animalId);
            shareIntent.putExtra(Intent.EXTRA_TEXT, 
                getShareMessage(scan));
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            // Show chooser
            Intent chooser = Intent.createChooser(shareIntent, "Share scan report via");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(chooser);
            
            Log.d(TAG, "Share intent launched");
            
        } catch (Exception e) {
            Log.e(TAG, "Error sharing: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get share message text
     */
    private String getShareMessage(ScanRecord scan) {
        StringBuilder message = new StringBuilder();
        
        message.append("AgriPulse Scan Alert\n\n");
        message.append("Animal: ").append(scan.animalId).append("\n");
        message.append("Status: ").append(scan.overallStatus).append("\n");
        
        if ("SUSPECTED".equals(scan.overallStatus)) {
            message.append("\n[!] ATTENTION REQUIRED\n");
            message.append(scan.statusReason).append("\n\n");
            message.append("Please review the attached detailed report.\n");
        } else {
            message.append("\n[OK] No immediate concerns\n\n");
        }
        
        message.append("Detailed report attached.\n");
        
        return message.toString();
    }
}
