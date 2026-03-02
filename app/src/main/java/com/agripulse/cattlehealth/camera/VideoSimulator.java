package com.agripulse.cattlehealth.camera;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Video-based simulation for testing without FLIR camera
 * Plays cattle videos from assets/simulation_input folder
 */
public class VideoSimulator {
    private static final String TAG = "VideoSimulator";
    private static final String SIMULATION_FOLDER = "simulation_input";
    
    private Context context;
    private List<String> videoFiles;
    private int currentVideoIndex = 0;
    private MediaMetadataRetriever retriever;
    private long videoDurationUs = 0;
    private long currentPositionUs = 0;
    private boolean isPlaying = false;
    
    public VideoSimulator(Context context) {
        this.context = context;
        this.videoFiles = new ArrayList<>();
        this.retriever = new MediaMetadataRetriever();
        loadVideoFiles();
    }
    
    /**
     * Load all video files from simulation_input folder
     */
    private void loadVideoFiles() {
        try {
            String[] files = context.getAssets().list(SIMULATION_FOLDER);
            if (files != null) {
                for (String file : files) {
                    if (file.endsWith(".mp4") || file.endsWith(".avi") || file.endsWith(".mov")) {
                        videoFiles.add(SIMULATION_FOLDER + "/" + file);
                        Log.d(TAG, "Found simulation video: " + file);
                    }
                }
            }
            
            if (videoFiles.isEmpty()) {
                Log.w(TAG, "No video files found in " + SIMULATION_FOLDER);
                Log.w(TAG, "Please add cattle videos (.mp4, .avi, .mov) to assets/simulation_input/");
            } else {
                Log.d(TAG, "Loaded " + videoFiles.size() + " simulation videos");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error loading video files", e);
        }
    }
    
    /**
     * Start playing the current video
     */
    public boolean start() {
        if (videoFiles.isEmpty()) {
            Log.e(TAG, "No videos available for simulation");
            return false;
        }
        
        try {
            String videoPath = videoFiles.get(currentVideoIndex);
            Log.d(TAG, "Starting video: " + videoPath);
            
            AssetFileDescriptor afd = context.getAssets().openFd(videoPath);
            retriever.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            
            String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            videoDurationUs = Long.parseLong(duration) * 1000; // Convert ms to us
            currentPositionUs = 0;
            isPlaying = true;
            
            Log.d(TAG, "Video started - Duration: " + (videoDurationUs / 1000000) + "s");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting video", e);
            return false;
        }
    }
    
    /**
     * Stop playing
     */
    public void stop() {
        isPlaying = false;
        currentPositionUs = 0;
        Log.d(TAG, "Video stopped");
    }
    
    /**
     * Get current frame as Bitmap
     * Simulates 15 FPS by advancing ~66ms per call (optimized for performance)
     */
    public Bitmap getCurrentFrame() {
        if (!isPlaying || videoFiles.isEmpty()) {
            return null;
        }
        
        try {
            // Get frame at current position with lower quality for better performance
            Bitmap frame = retriever.getFrameAtTime(currentPositionUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            
            // Scale down if too large to reduce memory usage
            if (frame != null && (frame.getWidth() > 800 || frame.getHeight() > 600)) {
                int newWidth = Math.min(frame.getWidth(), 800);
                int newHeight = Math.min(frame.getHeight(), 600);
                Bitmap scaledFrame = Bitmap.createScaledBitmap(frame, newWidth, newHeight, true);
                if (scaledFrame != frame) {
                    frame.recycle(); // Free original bitmap
                }
                frame = scaledFrame;
            }
            
            // Advance position (simulate 15 FPS = 66.67ms per frame)
            currentPositionUs += 66667; // 66.67ms in microseconds
            
            // Loop video if reached end
            if (currentPositionUs >= videoDurationUs) {
                currentPositionUs = 0;
                Log.d(TAG, "Video looped");
            }
            
            return frame;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting frame", e);
            return null;
        }
    }
    
    /**
     * Switch to next video
     */
    public void nextVideo() {
        if (videoFiles.isEmpty()) {
            return;
        }
        
        stop();
        currentVideoIndex = (currentVideoIndex + 1) % videoFiles.size();
        Log.d(TAG, "Switched to video " + (currentVideoIndex + 1) + "/" + videoFiles.size());
        start();
    }
    
    /**
     * Switch to previous video
     */
    public void previousVideo() {
        if (videoFiles.isEmpty()) {
            return;
        }
        
        stop();
        currentVideoIndex = (currentVideoIndex - 1 + videoFiles.size()) % videoFiles.size();
        Log.d(TAG, "Switched to video " + (currentVideoIndex + 1) + "/" + videoFiles.size());
        start();
    }
    
    /**
     * Get current video name
     */
    public String getCurrentVideoName() {
        if (videoFiles.isEmpty()) {
            return "No videos";
        }
        String path = videoFiles.get(currentVideoIndex);
        return path.substring(path.lastIndexOf('/') + 1);
    }
    
    /**
     * Check if videos are available
     */
    public boolean hasVideos() {
        return !videoFiles.isEmpty();
    }
    
    /**
     * Get video count
     */
    public int getVideoCount() {
        return videoFiles.size();
    }
    
    /**
     * Get current video index (1-based)
     */
    public int getCurrentVideoNumber() {
        return videoFiles.isEmpty() ? 0 : currentVideoIndex + 1;
    }
    
    /**
     * Release resources
     */
    public void release() {
        stop();
        try {
            if (retriever != null) {
                retriever.release();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error releasing retriever", e);
        }
    }
    
    /**
     * Generate simulated thermal data for a frame
     * Returns realistic cattle temperature ranges (30-42°C)
     */
    public float[][] generateThermalData(int width, int height) {
        float[][] thermalData = new float[height][width];
        
        // Simulate cattle body temperature (37-39°C core, 30-35°C extremities)
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Add some variation to simulate real thermal patterns
                float baseTemp = 36.5f + (float)(Math.random() * 2.5); // 36.5-39°C
                float noise = (float)(Math.random() * 0.5 - 0.25); // ±0.25°C noise
                thermalData[y][x] = baseTemp + noise;
            }
        }
        
        return thermalData;
    }
}
