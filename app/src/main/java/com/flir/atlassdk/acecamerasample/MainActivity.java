/*******************************************************************
 * @title FLIR Atlas Android SDK ACE Camera Sample
 * @file MainActivity.java
 * @Author Teledyne FLIR
 *
 * @brief This sample application connects to an ACE camera and renders received images to GLSurfaceView.
 *
 * Copyright 2025:    Teledyne FLIR
 *******************************************************************/
package com.flir.atlassdk.acecamerasample;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.flir.thermalsdk.ErrorCode;
import com.flir.thermalsdk.androidsdk.ThermalSdkAndroid;
import com.flir.thermalsdk.androidsdk.helpers.PermissionHandler;
import com.flir.thermalsdk.image.ColorDistributionSettings;
import com.flir.thermalsdk.image.HistogramEqualizationSettings;
import com.flir.thermalsdk.image.Palette;
import com.flir.thermalsdk.image.PaletteManager;
import com.flir.thermalsdk.image.TemperatureUnit;
import com.flir.thermalsdk.image.ThermalValue;
import com.flir.thermalsdk.image.fusion.Fusion;
import com.flir.thermalsdk.image.fusion.FusionMode;
import com.flir.thermalsdk.image.measurements.MeasurementShapeCollection;
import com.flir.thermalsdk.image.measurements.MeasurementSpot;
import com.flir.thermalsdk.live.Camera;
import com.flir.thermalsdk.live.CameraInformation;
import com.flir.thermalsdk.live.CameraType;
import com.flir.thermalsdk.live.CommunicationInterface;
import com.flir.thermalsdk.live.ConnectParameters;
import com.flir.thermalsdk.live.Identity;
import com.flir.thermalsdk.live.discovery.DiscoveredCamera;
import com.flir.thermalsdk.live.discovery.DiscoveryEventListener;
import com.flir.thermalsdk.live.discovery.DiscoveryFactory;
import com.flir.thermalsdk.live.streaming.Stream;
import com.flir.thermalsdk.log.ThermalLog;
import com.flir.thermalsdk.utils.FileUtils;
import com.flir.thermalsdk.utils.Pair;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import com.flir.thermalsdk.image.measurements.MeasurementSpot;
import com.flir.thermalsdk.image.measurements.MeasurementShapeCollection;

/**
 * This sample application connects to an ACE camera and renders received images to GLSurfaceView.
 * Integrated with AgriPulse backend for thermal analysis and fever detection.
 */
public class MainActivity extends AppCompatActivity {
    private volatile double latestTemperatureC = Double.NaN;

    private static final String TAG = "MainActivity";

    // reference to ACE camera
    private Camera camera;
    // reference to active ACE stream
    private Stream activeStream;
    // user defined palette used to colorize thermal image in desired way
    private Palette currentPalette;
    // user defined fusion mode
    private FusionMode currentFusionMode = FusionMode.THERMAL_ONLY;
    // default ColorDistributionSettings - usually they should be overridden before starting stream if camera provides customized settings
    private ColorDistributionSettings defaultColorSettings = new HistogramEqualizationSettings();

    // delayed surface is required because when the GLSurfaceView is created the camera is not yet created/connected
    private boolean delayedSetSurface;
    private int delayedSurfaceWidth;
    private int delayedSurfaceHeight;

    // request to take a single snapshot from current frame
    private boolean snapshotRequested;

    // enable or disable measurements for live stream
    private boolean enableMeasurements = false;

    // label showing app status, connection status, errors
    private TextView appStatus;

    // GLSurfaceView which is used to render frames incoming from the camera
    private GLSurfaceView glSurfaceView;

    // helper for requesting
    private PermissionHandler permissionHandler;

    // path where snapshot images will be stored
    private String imagesRoot;

    // frame counter for thermal data logging
    private int frameCounter = 0;

    // ===== BACKEND MODULES =====
    // Mock animal detector
    private com.flir.atlassdk.acecamerasample.detection.MockAnimalDetector animalDetector;
    private com.flir.atlassdk.acecamerasample.detection.DetectionResult currentDetection = null;
    
    // Thermal ROI analyzer
    private com.flir.atlassdk.acecamerasample.thermal.ThermalROIAnalyzer roiAnalyzer;
    
    // Fever detector
    private com.flir.atlassdk.acecamerasample.health.FeverDetector feverDetector;
    
    // Scan storage
    private com.flir.atlassdk.acecamerasample.storage.ScanStorage scanStorage;
    
    // Thermal overlay view
    private com.flir.atlassdk.acecamerasample.ui.ThermalOverlayView overlayView;
    
    // Scan exporter
    private com.flir.atlassdk.acecamerasample.export.ScanExporter scanExporter;
    
    // Animal tracker
    private com.flir.atlassdk.acecamerasample.tracking.AnimalTracker animalTracker;
    
    // Batch scan manager
    private com.flir.atlassdk.acecamerasample.batch.BatchScanManager batchScanManager;
    
    // Location tracker
    private com.flir.atlassdk.acecamerasample.location.LocationTracker locationTracker;
    
    // Scan request callback
    private ScanCallback scanCallback;
    private boolean scanRequested = false;
    
    // Callback interface for scan results
    public interface ScanCallback {
        void onScanComplete(com.flir.atlassdk.acecamerasample.storage.ScanRecord result);
        void onScanError(String error);
    }

    // you can easily switch between running the sample on emulator or on real camera by setting aceRealCameraInterface to appropriate value
    // by default we run on a real ACE camera
    private static final CommunicationInterface aceRealCameraInterface = CommunicationInterface.EMULATOR;
        //private static final CommunicationInterface aceRealCameraInterface = CommunicationInterface.ACE;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main_portrait);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                   .beginTransaction()
                    .replace(R.id.fragment_container, new landingfragment())
                    .commit();
        }


        // do not enable OpenCL (pass null), note the OpenGL is enabled by default when NOT running on AVD
        ThermalSdkAndroid.init(getApplicationContext(), ThermalLog.LogLevel.DEBUG);

        // match views to appropriate actions
        setupViews();

        ThermalLog.d(TAG, "SDK version = " + ThermalSdkAndroid.getVersion());
        ThermalLog.d(TAG, "SDK commit = " + ThermalSdkAndroid.getCommitHash());

        // helper for handling permission for accessing Manifest.permission.CAMERA
        permissionHandler = new PermissionHandler(this);

        // path for storing snapshots
        imagesRoot = getApplicationContext().getFilesDir().getAbsolutePath();
        ThermalLog.d(TAG, "Images DIR = " + imagesRoot);

        // after initialization of the SDK (via ThermalSdkAndroid.init) we can access default palettes from PaletteManager
        currentPalette = PaletteManager.getDefaultPalettes().get(0);
        
        // Initialize backend modules
        animalDetector = new com.flir.atlassdk.acecamerasample.detection.MockAnimalDetector();
        roiAnalyzer = new com.flir.atlassdk.acecamerasample.thermal.ThermalROIAnalyzer();
        feverDetector = new com.flir.atlassdk.acecamerasample.health.FeverDetector();
        scanStorage = new com.flir.atlassdk.acecamerasample.storage.ScanStorage(getApplicationContext());
        scanExporter = new com.flir.atlassdk.acecamerasample.export.ScanExporter(this);
        animalTracker = new com.flir.atlassdk.acecamerasample.tracking.AnimalTracker(this, scanStorage);
        batchScanManager = new com.flir.atlassdk.acecamerasample.batch.BatchScanManager();
        locationTracker = new com.flir.atlassdk.acecamerasample.location.LocationTracker(this);
        
        ThermalLog.d(TAG, "Backend modules initialized");
        ThermalLog.d(TAG, "Scan storage: " + scanStorage.getScanCount() + " scans");
        ThermalLog.d(TAG, "Animal tracker: " + animalTracker.getAnimalCount() + " animals");
        
        startDiscoveryAndConnectionAndStream();

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (glSurfaceView != null) {
            glSurfaceView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (glSurfaceView != null) {
            glSurfaceView.onResume();
        }
    }

    @Override
    protected void onStop() {
        // disconnect when the app is stopped
        disconnect();
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionHandler.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // in real app result of the camera permission request could be handled in onRequestPermissionsResult
        // here we ignore onRequestPermissionsResult for simplicity, assuming permission will be granted
    }

    /**
     * Discover, connect and start stream.
     */
    private void startDiscoveryAndConnectionAndStream() {
        DiscoveryFactory.getInstance().scan(new DiscoveryEventListener() {
            @Override
            public void onCameraFound(DiscoveredCamera discoveredCamera) {
                Identity foundIdentity = discoveredCamera.getIdentity();
                // check if we have found the expected camera type on the specified interface
                if (foundIdentity.cameraType == CameraType.ACE && foundIdentity.communicationInterface == aceRealCameraInterface) {
                    // automatically stop discovery when we have found a desired camera and connect to it
                    DiscoveryFactory.getInstance().stop(aceRealCameraInterface);
                    // proceed with connection with the discovered ACE identity
                    doConnect(foundIdentity);
                }
            }

            @Override
            public void onDiscoveryError(CommunicationInterface communicationInterface, ErrorCode error) {
                updateStatusInfo("Discovery error: " + error);
            }
        }, aceRealCameraInterface);
    }

    /**
     * Connect to the specified Identity.
     *
     * @param identity Identity describing the ACE camera
     */
    private void doConnect(Identity identity) {
        // request camera permission, if granted proceed with connection to ACE camera
        boolean permission = permissionHandler.requestCameraPermission(0x09); // use some arbitrary value for a request code
        if (!permission) {
            ThermalLog.e(TAG, "doConnect, failed due to camera permission");
            updateStatusInfo("doConnect, failed due to camera permission");
            return;
        }

        // run the connection and start stream in a separate thread
        new Thread(() -> {
            try {
                updateStatusInfo("Please wait while connecting to " + identity.deviceId);
                ThermalLog.d(TAG, "Connecting to identity: " + identity);
                // create the camera instance for the first time
                if (camera == null) {
                    camera = new Camera();
                }
                // connect to given identity
                camera.connect(
                        identity,
                        error -> updateStatusInfo("Connection error: " + error),
                        new ConnectParameters());

                // print camera information after connecting
                CameraInformation cameraInfo = Objects.requireNonNull(camera.getRemoteControl()).cameraInformation().getSync();
                ThermalLog.d(TAG, "Camera connected: " + cameraInfo);

                updateStatusInfo("Streaming using " + identity.deviceId);
                // start live streaming
                startStream();
            } catch (IOException e) {
                updateStatusInfo("Connection error: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Setup and start ACE stream.
     */
    private void startStream() {
        ThermalLog.d(TAG, "Preparing stream... obtain available camera stream");
        // assume the camera we are using has at least 1 stream
        activeStream = camera.getStreams().get(0);

        // setup GL pipeline for this stream
        ThermalLog.d(TAG, "Preparing stream... glSetupPipeline");
        camera.glSetupPipeline(activeStream, true);

        // if ACE camera provides the custom settings use them instead overriding the defaults for HistogramEqualizationSettings
        HistogramEqualizationSettings customHeq = camera.getCustomHistogramEqualizationSettings();
        if (customHeq != null) {
            ThermalLog.d(TAG, "Set custom camera-specific HistogramEqualizationSettings!");
            defaultColorSettings = customHeq;
        }

        ThermalLog.d(TAG, "Preparing stream... stream starts");
        activeStream.start(
                result -> {
                    // when we received a notification that the image frame is ready we request the GLSurfaceView to redraw it's content
                    glSurfaceView.requestRender();
                },
                error -> ThermalLog.w(TAG, "Stream.start() failed with error: " + error));
    }

    /**
     * Stop active stream and release related resources.
     */
    private void stopStream() {
        if (camera == null) {
            ThermalLog.w(TAG, "stopStream() failed, camera was null");
            return;
        }

        if (activeStream != null) {
            activeStream.stop();
        }
        camera.glTeardownPipeline();
    }

    /**
     * Completely disconnect from the camera, stop the stream and release resources if needed.
     */
    private void disconnect() {
        // stop stream and disconnect camera in a separate thread
        new Thread(() -> {
            ThermalLog.d(TAG, "disconnect()");
            if (camera == null) {
                return;
            }
            stopStream();
            camera.disconnect();
            camera = null;
        }).start();
    }

    /**
     * Helper function for updating app status label with a given status message.
     *
     * @param status message used to update app status label
     */
    private void updateStatusInfo(String status) {
        ThermalLog.d(TAG, "updateStatusInfo(): " + status);

        runOnUiThread(() -> {
            // Prevent crash if the TextView isn't present in the current layout
            if (appStatus != null) {
                appStatus.setText(status);
            } else {
                ThermalLog.w(TAG, "appStatus TextView is NULL. Add labelAppStatus to layout or bind it.");
            }
        });
    }
    
    /**
     * Public method for fragments to request a scan
     */
    public void requestScan(ScanCallback callback) {
        this.scanCallback = callback;
        this.scanRequested = true;
        ThermalLog.d(TAG, "Scan requested by fragment");
    }
    
    /**
     * Show or hide the thermal GLSurfaceView
     */
    public void showThermalView(boolean show) {
        runOnUiThread(() -> {
            if (glSurfaceView != null) {
                glSurfaceView.setVisibility(show ? android.view.View.VISIBLE : android.view.View.GONE);
                ThermalLog.d(TAG, "Thermal view visibility: " + (show ? "VISIBLE" : "GONE"));
            }
        });
    }
    
    /**
     * Getter methods for backend modules
     */
    public com.flir.atlassdk.acecamerasample.tracking.AnimalTracker getAnimalTracker() {
        return animalTracker;
    }
    
    public com.flir.atlassdk.acecamerasample.storage.ScanStorage getScanStorage() {
        return scanStorage;
    }
    
    public com.flir.atlassdk.acecamerasample.export.ScanExporter getScanExporter() {
        return scanExporter;
    }
    
    public com.flir.atlassdk.acecamerasample.location.LocationTracker getLocationTracker() {
        return locationTracker;
    }
    
    public com.flir.atlassdk.acecamerasample.batch.BatchScanManager getBatchScanManager() {
        return batchScanManager;
    }


    /**
     * Bind UI buttons and switches to appropriate actions and setup GLSurfaceView.
     */
    private void setupViews() {
        //appStatus = findViewById(R.id.labelAppStatus);

        //((SwitchMaterial) findViewById(R.id.switchButtonMeasurements)).setOnCheckedChangeListener(
               // (buttonView, isChecked) -> {
               //    enableMeasurements = isChecked;
              //  });

        /*((SwitchMaterial) findViewById(R.id.switchButtonPalette)).setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    if (isChecked) {
                        currentPalette = PaletteManager.getDefaultPalettes().get(1);
                    } else {
                        currentPalette = PaletteManager.getDefaultPalettes().get(0);
                    }
                });*/

       /* ((SwitchMaterial) findViewById(R.id.switchButtonFusionMode)).setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    if (isChecked) {
                        currentFusionMode = FusionMode.VISUAL_ONLY;
                    } else {
                        currentFusionMode = FusionMode.THERMAL_ONLY;
                    }
                });*/


       // findViewById(R.id.buttonSnapshot).setOnClickListener(v -> snapshotRequested = true);

       // findViewById(R.id.buttonStartCamera).setOnClickListener(v -> startDiscoveryAndConnectionAndStream());

        // setup GLSurfaceView
        glSurfaceView = findViewById(R.id.glSurface);
        glSurfaceView.setEGLContextClientVersion(3);
        glSurfaceView.setPreserveEGLContextOnPause(false);
        // set custom renderer, that will handle pushing camera's frame to the view
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    /**
     * Custom renderer, that will handle pushing camera's frame to the view
     */
    private final GLSurfaceView.Renderer renderer = new GLSurfaceView.Renderer() {

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            ThermalLog.d(TAG, "onSurfaceCreated()");
            // not used
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            ThermalLog.d(TAG, "onSurfaceChanged(), width=" + width + ", height=" + height);
            if (camera != null) {
                // if camera instance is already set propagate event to Camera object
                camera.glOnSurfaceChanged(width, height);
                delayedSetSurface = false;
            } else {
                ThermalLog.d(TAG, "Failed to set surface changed, because camera hasn't been created yet");
                // delay setting up the camera.glOnSurfaceChanged until the camera is created and frame is requested to be drawn
                delayedSetSurface = true;
                delayedSurfaceWidth = width;
                delayedSurfaceHeight = height;
            }
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            // propagate event to Camera object if it exists
            if (camera != null) {
                if (!camera.glIsGlContextReady()) {
                    ThermalLog.w(TAG, "Skip processing for onDrawFrame because camera.glIsGlContextReady is NOT ready");
                    return;
                }

                // if the surface was create prior to the camera, we need to setup the camera.glOnSurfaceChanged here instead
                if (delayedSetSurface) {
                    // The onSurfaceChanged was called before we had a camera, so set it now instead
                    camera.glOnSurfaceChanged(delayedSurfaceWidth, delayedSurfaceHeight);
                    delayedSetSurface = false;
                }

                // we can set various thermal image stream parameters via glWithThermalImage() block since we need a thread-safe access to ThermalImage
                camera.glWithThermalImage(thermalImage -> {

                    ThermalLog.d("FLOW", "Frame received");


                    // setup palette
                    thermalImage.setPalette(currentPalette);
                    // setup fusion mode
                    Fusion fusion = thermalImage.getFusion();
                    if (fusion != null) {
                        fusion.setFusionMode(currentFusionMode);
                    }

                    // apply the color mode - by default it is HistogramEqualizationSettings
                    // and if camera provided customized HistogramEqualizationSettings via Camera.getCustomHistogramEqualizationSettings(), it will be used as default
                    // of course user can select any other ColorDistributionSettings
                    // and he can provide own parameters for HistogramEqualizationSettings too, which may be different than default and different than camera-specific settings
                    // in this sample though we only use either default HistogramEqualizationSettings or the customized HistogramEqualizationSettings that camera provides
                    thermalImage.setColorDistributionSettings(defaultColorSettings);

                    // setup measurements
                    if (enableMeasurements) {
                        MeasurementShapeCollection measurements = thermalImage.getMeasurements();
                        List<MeasurementSpot> spots = measurements.getSpots();

                        if (spots.size() < 1) {
                            int w = thermalImage.getWidth();
                            int h = thermalImage.getHeight();
                            measurements.addSpot(w / 2, h / 2); // center spot
                        }

                        spots = measurements.getSpots();
                        int index = 0;
                        for (MeasurementSpot sp : spots) {
                            ThermalValue value = sp.getValue();
                            ThermalLog.d(TAG, "Spot " + index + " temp = " + value.value);
                            index++;
                        }
                    }


                    if (snapshotRequested) {
                        // reset the flag first
                        snapshotRequested = false;

                        // we might set a desired temperature unit before storing an image
                        // this might we useful if for the live stream we display one unit, but for storing image we want another unit
                        thermalImage.setTemperatureUnit(TemperatureUnit.CELSIUS);

                        // we might also use the calculated scale ranges and apply them when storing the image
                        // this way the image will render with fine scale span regardless if we open it with autoscale option enabled or disabled
                        // the drawback is that we override the manual scale setting if there were any
                        // so this approach may not be suitable for certain scenarios
                        Pair<ThermalValue, ThermalValue> range = camera.glGetScaleRange();
                        ThermalLog.d(TAG, "glGetScaleRange when storing image: " + range.first + " - " + range.second);
                        thermalImage.getScale().setRange(range.first, range.second);

                        // define an image path - use a helper function to prepare the file name
                        String snapshotPath = FileUtils.prepareUniqueFileName(imagesRoot, "ACE_", "jpg");
                        try {
                            // save the snapshot at the given path
                            thermalImage.saveAs(snapshotPath);
                            ThermalLog.d(TAG, "Snapshot stored under: " + snapshotPath);
                        } catch (IOException e) {
                            ThermalLog.e(TAG, "Unable to take snapshot: " + e.getMessage());
                        }
                    }
                    
                    // Process scan request from fragments
                    if (scanRequested) {
                        scanRequested = false;
                        processScan(thermalImage);
                    }
                });

                // request the camera to push the frame buffer for drawing on the GLSurfaceView
                camera.glOnDrawFrame();

            }
        }
    };
    
    /**
     * Process a scan request - extract thermal data, detect animal, analyze ROIs, detect fever
     */
    private void processScan(com.flir.thermalsdk.image.ThermalImage thermalImage) {
        try {
            ThermalLog.d(TAG, "Processing scan...");
            
            // 1. Get thermal image dimensions
            int width = thermalImage.getWidth();
            int height = thermalImage.getHeight();
            Pair<ThermalValue, ThermalValue> range = camera.glGetScaleRange();
            
            ThermalLog.d(TAG, "Thermal image: " + width + "x" + height + ", range: " + range.first + " to " + range.second);
            
            // 2. Run mock animal detection
            com.flir.atlassdk.acecamerasample.detection.DetectionResult detection = 
                animalDetector.detectAnimal(width, height);
            
            ThermalLog.d(TAG, "Detection: " + detection.species + " (conf=" + detection.overallConfidence + ")");
            ThermalLog.d(TAG, "Keypoints: " + detection.keypoints.size());
            
            // 3. Extract ROI temperatures for each keypoint
            java.util.Map<String, com.flir.atlassdk.acecamerasample.thermal.ROITemperature> bodyPartTemps = 
                new java.util.HashMap<>();
            
            for (com.flir.atlassdk.acecamerasample.detection.Keypoint kp : detection.keypoints) {
                com.flir.atlassdk.acecamerasample.thermal.ROITemperature roiTemp = 
                    roiAnalyzer.analyzeROI(thermalImage, kp);
                bodyPartTemps.put(kp.name, roiTemp);
                ThermalLog.d(TAG, "ROI " + kp.name + ": " + roiTemp.toString());
            }
            
            // 4. Detect fever
            com.flir.atlassdk.acecamerasample.health.HealthStatus healthStatus = 
                feverDetector.detectFever(bodyPartTemps);
            
            ThermalLog.d(TAG, "Health status: " + healthStatus.toString());
            
            // 5. Get location
            com.flir.atlassdk.acecamerasample.location.LocationTracker.LocationData location = 
                locationTracker.getCurrentLocation();
            
            // 6. Generate animal ID
            String animalId = animalTracker.generateNextAnimalID();
            
            // 7. Convert ROI temperatures to storage format
            java.util.Map<String, com.flir.atlassdk.acecamerasample.storage.ScanRecord.BodyPartData> bodyParts = 
                new java.util.HashMap<>();
            
            for (java.util.Map.Entry<String, com.flir.atlassdk.acecamerasample.thermal.ROITemperature> entry : 
                 bodyPartTemps.entrySet()) {
                com.flir.atlassdk.acecamerasample.thermal.ROITemperature roi = entry.getValue();
                bodyParts.put(entry.getKey(), 
                    new com.flir.atlassdk.acecamerasample.storage.ScanRecord.BodyPartData(
                        roi.bodyPart, roi.t95, roi.mean, roi.std, roi.sampleCount
                    ));
            }
            
            // 8. Create scan record
            com.flir.atlassdk.acecamerasample.storage.ScanRecord record = 
                new com.flir.atlassdk.acecamerasample.storage.ScanRecord(
                    0,  // Will be assigned by storage
                    System.currentTimeMillis(),
                    animalId,
                    detection.species,
                    bodyParts,
                    healthStatus.status,
                    healthStatus.reason,
                    healthStatus.confidence,
                    null,  // No thermal snapshot path for now
                    location.latitude,
                    location.longitude
                );
            
            // 9. Populate visual data for UI (keypoints and ROIs)
            record.keypoints = new java.util.ArrayList<>();
            for (com.flir.atlassdk.acecamerasample.detection.Keypoint kp : detection.keypoints) {
                record.keypoints.add(kp);
            }
            
            record.rois = new java.util.ArrayList<>();
            for (java.util.Map.Entry<String, com.flir.atlassdk.acecamerasample.thermal.ROITemperature> entry : 
                 bodyPartTemps.entrySet()) {
                com.flir.atlassdk.acecamerasample.thermal.ROITemperature roi = entry.getValue();
                record.rois.add(new com.flir.atlassdk.acecamerasample.storage.ScanRecord.ROIResult(
                    roi.bodyPart,
                    roi.mean - 273.15,  // Convert Kelvin to Celsius
                    roi.t95 - 273.15,
                    roi.std,
                    healthStatus.status.equals("SUSPECTED") ? 
                        com.flir.atlassdk.acecamerasample.storage.RiskStatus.SUSPECTED : 
                        com.flir.atlassdk.acecamerasample.storage.RiskStatus.NORMAL,
                    healthStatus.reason
                ));
            }
            
            // 10. Save scan to storage
            long scanId = scanStorage.saveScan(record);
            record.scanId = scanId;
            
            ThermalLog.d(TAG, "Scan saved with ID: " + scanId);
            
            // 11. Update animal profile
            animalTracker.updateProfile(record);
            
            // 12. Generate report text
            record.reportText = com.flir.atlassdk.acecamerasample.export.ReportFormatter
                .formatTextReport(record);
            
            // 13. Callback to UI on main thread
            final com.flir.atlassdk.acecamerasample.storage.ScanRecord finalRecord = record;
            if (scanCallback != null) {
                runOnUiThread(() -> {
                    scanCallback.onScanComplete(finalRecord);
                    ThermalLog.d(TAG, "Scan callback completed");
                });
            }
            
        } catch (Exception e) {
            ThermalLog.e(TAG, "Error processing scan: " + e.getMessage());
            e.printStackTrace();
            
            if (scanCallback != null) {
                final String errorMsg = e.getMessage();
                runOnUiThread(() -> scanCallback.onScanError(errorMsg));
            }
        }
    }

}
