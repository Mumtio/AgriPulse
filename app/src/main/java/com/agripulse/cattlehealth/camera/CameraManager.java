package com.agripulse.cattlehealth.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;

import com.flir.thermalsdk.androidsdk.ThermalSdkAndroid;
import com.flir.thermalsdk.live.CommunicationInterface;
import com.flir.thermalsdk.live.Identity;
import com.flir.thermalsdk.live.ConnectParameters;
import com.flir.thermalsdk.live.discovery.DiscoveryEventListener;
import com.flir.thermalsdk.live.discovery.DiscoveryFactory;
import com.flir.thermalsdk.live.Camera;

import java.io.IOException;

/**
 * Camera Manager - Auto-detects FLIR camera and falls back to simulation
 * 
 * Automatically detects FLIR camera on startup (3-second timeout).
 * Falls back to simulation mode if no camera found.
 * 
 * Pipeline: Live video → Toast alert → Capture frame → Backend detection → Extract temps → Diagnose
 */
public class CameraManager {
    
    private static final String TAG = "CameraManager";
    private static final int DISCOVERY_TIMEOUT_MS = 3000; // 3 seconds
    
    private Context context;
    private boolean isRealCamera = false;
    private boolean isInitialized = false;
    
    private CameraMode currentMode = CameraMode.UNKNOWN;
    private CameraStatusListener statusListener;
    
    // FLIR SDK objects
    private Camera camera;
    private Identity cameraIdentity;
    private Handler timeoutHandler = new Handler(Looper.getMainLooper());
    
    // Video simulation
    private VideoSimulator videoSimulator;
    
    public enum CameraMode {
        UNKNOWN,
        REAL_FLIR,      // Real FLIR camera detected
        SIMULATION      // Simulation mode (no camera)
    }
    
    public interface CameraStatusListener {
        void onCameraDetected(CameraMode mode);
        void onCameraConnected();
        void onCameraDisconnected();
        void onError(String error);
    }
    
    public CameraManager(Context context) {
        this.context = context;
    }
    
    /**
     * Initialize camera system - auto-detects FLIR camera
     */
    public void initialize(CameraStatusListener listener) {
        this.statusListener = listener;
        
        Log.d(TAG, "Initializing camera system...");
        
        try {
            // Initialize FLIR SDK
            ThermalSdkAndroid.init(context);
            
            // Start camera discovery with timeout
            discoverCamera();
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize FLIR SDK: " + e.getMessage(), e);
            // Fall back to simulation mode instead of crashing
            switchToSimulation();
        }
    }
    
    /**
     * Discover FLIR camera with timeout
     */
    private void discoverCamera() {
        Log.d(TAG, "Starting camera discovery (timeout: " + DISCOVERY_TIMEOUT_MS + "ms)...");
        
        // Set timeout to fall back to simulation
        timeoutHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isInitialized) {
                    Log.d(TAG, "Camera discovery timeout - switching to simulation");
                    switchToSimulation();
                }
            }
        }, DISCOVERY_TIMEOUT_MS);
        
        // Start discovery
        DiscoveryFactory.getInstance().scan(discoveryEventListener, CommunicationInterface.USB);
    }
    
    /**
     * Discovery event listener
     */
    private final DiscoveryEventListener discoveryEventListener = new DiscoveryEventListener() {
        @Override
        public void onCameraFound(com.flir.thermalsdk.live.discovery.DiscoveredCamera discoveredCamera) {
            Identity identity = discoveredCamera.getIdentity();
            Log.d(TAG, "FLIR Camera found: " + identity);
            
            // Cancel timeout
            timeoutHandler.removeCallbacksAndMessages(null);
            
            // Connect to camera
            cameraIdentity = identity;
            connectToCamera(identity);
        }

        @Override
        public void onDiscoveryError(CommunicationInterface communicationInterface, com.flir.thermalsdk.ErrorCode errorCode) {
            Log.e(TAG, "Discovery error: " + errorCode);
            if (!isInitialized) {
                switchToSimulation();
            }
        }
    };
    
    /**
     * Connect to discovered camera
     */
    private void connectToCamera(Identity identity) {
        Log.d(TAG, "Connecting to camera...");
        
        try {
            camera = new Camera();
            camera.connect(
                identity,
                error -> {
                    Log.e(TAG, "Connection error: " + error);
                    if (statusListener != null) {
                        statusListener.onError("Connection error: " + error);
                    }
                    switchToSimulation();
                },
                new ConnectParameters()
            );
            
            // Connection successful
            onCameraConnected();
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to connect to camera: " + e.getMessage());
            switchToSimulation();
        }
    }
    
    /**
     * Called when camera is connected
     */
    private void onCameraConnected() {
        isRealCamera = true;
        isInitialized = true;
        currentMode = CameraMode.REAL_FLIR;
        
        Log.d(TAG, "Camera connected successfully");
        
        if (statusListener != null) {
            statusListener.onCameraDetected(CameraMode.REAL_FLIR);
            statusListener.onCameraConnected();
        }
    }
    
    /**
     * Switch to simulation mode
     */
    private void switchToSimulation() {
        Log.d(TAG, "Switching to SIMULATION mode");
        
        isRealCamera = false;
        isInitialized = true;
        currentMode = CameraMode.SIMULATION;
        
        // Initialize video simulator
        videoSimulator = new VideoSimulator(context);
        if (videoSimulator.hasVideos()) {
            videoSimulator.start();
            Log.d(TAG, "Video simulation started with " + videoSimulator.getVideoCount() + " videos");
        } else {
            Log.w(TAG, "No simulation videos found - using static simulation");
        }
        
        if (statusListener != null) {
            statusListener.onCameraDetected(CameraMode.SIMULATION);
        }
    }
    
    /**
     * Capture current frame
     */
    public Bitmap captureFrame() {
        if (isRealCamera) {
            return captureRealFrame();
        } else {
            return captureSimulatedFrame();
        }
    }
    
    /**
     * Capture thermal data
     */
    public float[][] captureThermalData() {
        if (isRealCamera) {
            return captureRealThermalData();
        } else {
            return captureSimulatedThermalData();
        }
    }
    
    /**
     * Get ambient temperature
     */
    public float getAmbientTemperature() {
        return 20.0f; // Default
    }
    
    /**
     * Get relative humidity
     */
    public float getRelativeHumidity() {
        return 50.0f; // Default
    }
    
    /**
     * Get camera instance (for advanced usage)
     */
    public Camera getCamera() {
        return camera;
    }
    
    // ========== REAL CAMERA METHODS ==========
    
    private Bitmap captureRealFrame() {
        if (camera == null) {
            Log.w(TAG, "Camera not available - using simulation");
            return captureSimulatedFrame();
        }
        
        try {
            // For real implementation, you would use camera.glWithThermalImage()
            // to access thermal data. This is a placeholder.
            Log.d(TAG, "Capturing real frame (placeholder)");
            
            // TODO: Implement real frame capture using camera.glWithThermalImage()
            // For now, return simulated frame
            return captureSimulatedFrame();
            
        } catch (Exception e) {
            Log.e(TAG, "Error capturing real frame: " + e.getMessage());
            return captureSimulatedFrame();
        }
    }
    
    private float[][] captureRealThermalData() {
        if (camera == null) {
            Log.w(TAG, "Camera not available - using simulation");
            return captureSimulatedThermalData();
        }
        
        try {
            // For real implementation, you would use camera.glWithThermalImage()
            // to access thermal data. This is a placeholder.
            Log.d(TAG, "Capturing real thermal data (placeholder)");
            
            // TODO: Implement real thermal data capture using camera.glWithThermalImage()
            // For now, return simulated data
            return captureSimulatedThermalData();
            
        } catch (Exception e) {
            Log.e(TAG, "Error capturing real thermal data: " + e.getMessage());
            return captureSimulatedThermalData();
        }
    }
    
    // ========== SIMULATION METHODS ==========
    
    private Bitmap captureSimulatedFrame() {
        if (videoSimulator != null && videoSimulator.hasVideos()) {
            Bitmap frame = videoSimulator.getCurrentFrame();
            if (frame != null) {
                Log.d(TAG, "Captured video frame from: " + videoSimulator.getCurrentVideoName());
                return frame;
            }
        }
        
        // Fallback to static simulation
        Bitmap bitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(0xFF808080);
        Log.d(TAG, "Captured static simulated frame");
        return bitmap;
    }
    
    private float[][] captureSimulatedThermalData() {
        if (videoSimulator != null && videoSimulator.hasVideos()) {
            // Generate thermal data based on current video frame
            Log.d(TAG, "Generating thermal data for video frame");
            return videoSimulator.generateThermalData(640, 480);
        }
        
        // Fallback to static thermal simulation
        Log.d(TAG, "Generating static simulated thermal data");
        return com.agripulse.cattlehealth.thermal.ThermalExtractor
            .simulateThermalData(640, 480, "healthy");
    }
    
    // ========== PUBLIC GETTERS ==========
    
    public boolean isRealCamera() {
        return isRealCamera;
    }
    
    public boolean isInitialized() {
        return isInitialized;
    }
    
    public CameraMode getCurrentMode() {
        return currentMode;
    }
    
    public String getModeDescription() {
        switch (currentMode) {
            case REAL_FLIR:
                return "FLIR Camera Connected";
            case SIMULATION:
                if (videoSimulator != null && videoSimulator.hasVideos()) {
                    return "Video Simulation (" + videoSimulator.getCurrentVideoNumber() + 
                           "/" + videoSimulator.getVideoCount() + ": " + 
                           videoSimulator.getCurrentVideoName() + ")";
                } else {
                    return "Static Simulation (No Videos)";
                }
            default:
                return "Initializing...";
        }
    }
    
    /**
     * Disconnect and cleanup
     */
    public void disconnect() {
        Log.d(TAG, "Disconnecting camera...");
        
        if (camera != null) {
            try {
                camera.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error disconnecting camera: " + e.getMessage());
            }
            camera = null;
        }
        
        // Cleanup video simulator
        if (videoSimulator != null) {
            videoSimulator.release();
            videoSimulator = null;
        }
        
        isInitialized = false;
        isRealCamera = false;
        currentMode = CameraMode.UNKNOWN;
        
        Log.d(TAG, "Camera disconnected");
    }
    
    // ========== VIDEO SIMULATION CONTROLS ==========
    
    /**
     * Switch to next simulation video
     */
    public void nextSimulationVideo() {
        if (videoSimulator != null && currentMode == CameraMode.SIMULATION) {
            videoSimulator.nextVideo();
            Log.d(TAG, "Switched to next video: " + videoSimulator.getCurrentVideoName());
        }
    }
    
    /**
     * Switch to previous simulation video
     */
    public void previousSimulationVideo() {
        if (videoSimulator != null && currentMode == CameraMode.SIMULATION) {
            videoSimulator.previousVideo();
            Log.d(TAG, "Switched to previous video: " + videoSimulator.getCurrentVideoName());
        }
    }
    
    /**
     * Get current simulation video info
     */
    public String getSimulationVideoInfo() {
        if (videoSimulator != null && videoSimulator.hasVideos()) {
            return videoSimulator.getCurrentVideoNumber() + "/" + 
                   videoSimulator.getVideoCount() + ": " + 
                   videoSimulator.getCurrentVideoName();
        }
        return "No videos available";
    }
    
    /**
     * Check if video simulation is available
     */
    public boolean hasSimulationVideos() {
        return videoSimulator != null && videoSimulator.hasVideos();
    }
}
