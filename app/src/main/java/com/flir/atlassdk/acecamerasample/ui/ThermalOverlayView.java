package com.flir.atlassdk.acecamerasample.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.flir.atlassdk.acecamerasample.detection.DetectionResult;
import com.flir.atlassdk.acecamerasample.detection.Keypoint;
import com.flir.atlassdk.acecamerasample.health.HealthStatus;
import com.flir.atlassdk.acecamerasample.thermal.ROITemperature;

import java.util.Map;

public class ThermalOverlayView extends View {
    
    private static final String TAG = "ThermalOverlay";
    
    private DetectionResult currentDetection;
    private Map<String, ROITemperature> bodyPartTemps;
    private HealthStatus healthStatus;
    private String statusText = "Initializing...";
    private String tempRangeText = "";
    
    private Paint keypointPaint;
    private Paint roiBoxPaint;
    private Paint textPaint;
    private Paint bannerPaint;
    private Paint labelPaint;
    
    private static final int ROI_SIZE = 20;
    private static final float KEYPOINT_RADIUS = 8f;
    
    public ThermalOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaints();
        Log.d(TAG, "ThermalOverlayView initialized");
    }
    
    private void initPaints() {
        // Keypoint circles
        keypointPaint = new Paint();
        keypointPaint.setStyle(Paint.Style.FILL);
        keypointPaint.setColor(Color.YELLOW);
        keypointPaint.setAntiAlias(true);
        
        // ROI boxes
        roiBoxPaint = new Paint();
        roiBoxPaint.setStyle(Paint.Style.STROKE);
        roiBoxPaint.setStrokeWidth(3f);
        roiBoxPaint.setAntiAlias(true);
        
        // Text labels
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(24f);
        textPaint.setAntiAlias(true);
        textPaint.setShadowLayer(2f, 1f, 1f, Color.BLACK);
        
        // Status banner background
        bannerPaint = new Paint();
        bannerPaint.setColor(Color.argb(180, 0, 0, 0));
        bannerPaint.setStyle(Paint.Style.FILL);
        
        // Small labels
        labelPaint = new Paint();
        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextSize(18f);
        labelPaint.setAntiAlias(true);
        labelPaint.setShadowLayer(2f, 1f, 1f, Color.BLACK);
    }
    
    /**
     * Update detection data
     */
    public void updateDetection(DetectionResult detection, 
                                Map<String, ROITemperature> temps,
                                HealthStatus status) {
        this.currentDetection = detection;
        this.bodyPartTemps = temps;
        this.healthStatus = status;
        
        Log.d(TAG, "Overlay updated - Keypoints: " + 
              (detection != null && detection.keypoints != null ? detection.keypoints.size() : 0) +
              ", Status: " + (status != null ? status.status : "null"));
        
        invalidate();  // Trigger redraw
    }
    
    /**
     * Update status text
     */
    public void updateStatus(String status, String tempRange) {
        this.statusText = status;
        this.tempRangeText = tempRange;
        invalidate();
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (canvas == null) return;
        
        // Draw status banner at top
        drawStatusBanner(canvas);
        
        // Draw detection overlays if available
        if (currentDetection != null && currentDetection.keypoints != null) {
            int keypointCount = currentDetection.keypoints.size();
            drawKeypoints(canvas);
            drawROIBoxes(canvas);
            
            // Log occasionally to verify drawing
            if (keypointCount > 0 && Math.random() < 0.1) {  // 10% of frames
                Log.d(TAG, "Drawing " + keypointCount + " keypoints and ROI boxes");
            }
        }
    }
    
    /**
     * Draw status banner at top
     */
    private void drawStatusBanner(Canvas canvas) {
        int bannerHeight = 120;
        canvas.drawRect(0, 0, getWidth(), bannerHeight, bannerPaint);
        
        // Status text
        canvas.drawText(statusText, 20, 40, textPaint);
        
        // Temperature range
        if (!tempRangeText.isEmpty()) {
            canvas.drawText(tempRangeText, 20, 75, labelPaint);
        }
        
        // Health status if available
        if (healthStatus != null) {
            Paint statusPaint = new Paint(textPaint);
            if ("SUSPECTED".equals(healthStatus.status)) {
                statusPaint.setColor(Color.RED);
            } else {
                statusPaint.setColor(Color.GREEN);
            }
            canvas.drawText(healthStatus.status, 20, 105, statusPaint);
        }
    }
    
    /**
     * Draw keypoint markers
     */
    private void drawKeypoints(Canvas canvas) {
        for (Keypoint kp : currentDetection.keypoints) {
            // Scale coordinates to view size
            float x = scaleX(kp.x);
            float y = scaleY(kp.y);
            
            // Draw keypoint circle
            canvas.drawCircle(x, y, KEYPOINT_RADIUS, keypointPaint);
            
            // Draw label
            String label = kp.name.replace("_", " ");
            canvas.drawText(label, x + 15, y - 5, labelPaint);
            
            // Draw confidence
            String conf = String.format("%.0f%%", kp.confidence * 100);
            canvas.drawText(conf, x + 15, y + 15, labelPaint);
        }
    }
    
    /**
     * Draw ROI bounding boxes
     */
    private void drawROIBoxes(Canvas canvas) {
        if (bodyPartTemps == null) return;
        
        for (Keypoint kp : currentDetection.keypoints) {
            ROITemperature roi = bodyPartTemps.get(kp.name);
            if (roi == null) continue;
            
            // Scale coordinates
            float x = scaleX(kp.x);
            float y = scaleY(kp.y);
            
            // Determine box color based on health status
            int boxColor = getROIColor(kp.name);
            roiBoxPaint.setColor(boxColor);
            
            // Draw ROI box (20x20 pixels scaled)
            float halfSize = scaleX(ROI_SIZE / 2);
            Rect box = new Rect(
                (int)(x - halfSize),
                (int)(y - halfSize),
                (int)(x + halfSize),
                (int)(y + halfSize)
            );
            canvas.drawRect(box, roiBoxPaint);
            
            // Draw temperature below box
            String tempText = String.format("%.1fK", roi.mean);
            canvas.drawText(tempText, x - 20, y + halfSize + 20, labelPaint);
        }
    }
    
    /**
     * Get color for ROI box based on health status
     */
    private int getROIColor(String bodyPart) {
        if (healthStatus == null) {
            return Color.WHITE;  // Scanning
        }
        
        if ("SUSPECTED".equals(healthStatus.status)) {
            // Check if this body part is the problem
            if (healthStatus.reason.toLowerCase().contains(bodyPart.toLowerCase())) {
                return Color.RED;  // Problem area
            }
        }
        
        return Color.GREEN;  // Normal
    }
    
    /**
     * Scale X coordinate from thermal image to view
     */
    private float scaleX(int thermalX) {
        // Thermal image is 640 wide
        return (thermalX / 640f) * getWidth();
    }
    
    /**
     * Scale Y coordinate from thermal image to view
     */
    private float scaleY(int thermalY) {
        // Thermal image is 480 high
        return (thermalY / 480f) * getHeight();
    }
}
