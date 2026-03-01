package com.flir.atlassdk.acecamerasample;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.flir.atlassdk.acecamerasample.detection.Keypoint;
import com.flir.atlassdk.acecamerasample.storage.ScanRecord;
import com.flir.atlassdk.acecamerasample.storage.RiskStatus;

import java.util.List;

public class OverlayViewFrontend extends View {

    private List<Keypoint> keypoints;
    private List<ScanRecord.ROIResult> rois;

    private Paint keypointPaint;
    private Paint roiPaintNormal;
    private Paint roiPaintHigh;
    private Paint textPaint;

    public OverlayViewFrontend(Context context) {
        super(context);
        init();
    }

    public OverlayViewFrontend(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OverlayViewFrontend(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        keypointPaint = new Paint();
        keypointPaint.setColor(0xFFFFFFFF);  // White
        keypointPaint.setStyle(Paint.Style.FILL);
        keypointPaint.setAntiAlias(true);
        keypointPaint.setShadowLayer(8f, 0f, 0f, 0xFF000000);  // Shadow for depth

        roiPaintNormal = new Paint();
        roiPaintNormal.setColor(0xFF66BB6A);  // Material Green
        roiPaintNormal.setStyle(Paint.Style.STROKE);
        roiPaintNormal.setStrokeWidth(6f);
        roiPaintNormal.setAntiAlias(true);
        roiPaintNormal.setShadowLayer(4f, 0f, 0f, 0x88000000);

        roiPaintHigh = new Paint();
        roiPaintHigh.setColor(0xFFEF5350);  // Material Red
        roiPaintHigh.setStyle(Paint.Style.STROKE);
        roiPaintHigh.setStrokeWidth(6f);
        roiPaintHigh.setAntiAlias(true);
        roiPaintHigh.setShadowLayer(4f, 0f, 0f, 0x88000000);

        textPaint = new Paint();
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(36f);
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
        textPaint.setShadowLayer(6f, 0f, 2f, 0xFF000000);  // Text shadow
    }

    public void update(List<Keypoint> keypoints, List<ScanRecord.ROIResult> rois) {
        this.keypoints = keypoints;
        this.rois = rois;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (keypoints != null) {
            for (Keypoint kp : keypoints) {
                // Scale coordinates from thermal (640x480) to view size
                float scaledX = scaleX(kp.x);
                float scaledY = scaleY(kp.y);
                
                // Draw keypoint with glow effect
                canvas.drawCircle(scaledX, scaledY, 12f, keypointPaint);
                
                // Draw label with background
                String label = kp.name.toUpperCase();
                float textWidth = textPaint.measureText(label);
                android.graphics.RectF labelBg = new android.graphics.RectF(
                    scaledX + 15, scaledY - 25,
                    scaledX + 25 + textWidth, scaledY - 5
                );
                
                Paint bgPaint = new Paint();
                bgPaint.setColor(0xDD000000);
                bgPaint.setAntiAlias(true);
                canvas.drawRoundRect(labelBg, 8f, 8f, bgPaint);
                
                canvas.drawText(label, scaledX + 20, scaledY - 10, textPaint);
            }
        }

        if (rois != null && keypoints != null) {
            for (ScanRecord.ROIResult roi : rois) {

                Keypoint anchor = null;
                for (Keypoint kp : keypoints) {
                    if (kp.name.equalsIgnoreCase(roi.name.toLowerCase())) {
                        anchor = kp;
                        break;
                    }
                }
                if (anchor == null) continue;

                Paint paint =
                        roi.status == RiskStatus.SUSPECTED
                                ? roiPaintHigh
                                : roiPaintNormal;

                // Scale coordinates
                float scaledX = scaleX(anchor.x);
                float scaledY = scaleY(anchor.y);
                float size = 140f;
                
                RectF box = new RectF(
                        scaledX - size / 2,
                        scaledY - size / 2,
                        scaledX + size / 2,
                        scaledY + size / 2
                );

                // Draw rounded rectangle for modern look
                canvas.drawRoundRect(box, 16f, 16f, paint);
                
                // Draw temperature label with background
                String tempLabel = String.format("%.1f°C", roi.meanTempC);
                float tempWidth = textPaint.measureText(tempLabel);
                android.graphics.RectF tempBg = new android.graphics.RectF(
                    box.left, box.top - 50,
                    box.left + tempWidth + 20, box.top - 10
                );
                
                Paint tempBgPaint = new Paint();
                tempBgPaint.setColor(roi.status == RiskStatus.SUSPECTED ? 0xDDEF5350 : 0xDD66BB6A);
                tempBgPaint.setAntiAlias(true);
                canvas.drawRoundRect(tempBg, 12f, 12f, tempBgPaint);
                
                canvas.drawText(tempLabel, box.left + 10, box.top - 20, textPaint);
            }
        }
    }
    
    /**
     * Scale X coordinate from thermal image (640 wide) to view width
     */
    private float scaleX(float thermalX) {
        if (getWidth() == 0) return thermalX;
        return (thermalX / 640f) * getWidth();
    }
    
    /**
     * Scale Y coordinate from thermal image (480 high) to view height
     */
    private float scaleY(float thermalY) {
        if (getHeight() == 0) return thermalY;
        return (thermalY / 480f) * getHeight();
    }
}
