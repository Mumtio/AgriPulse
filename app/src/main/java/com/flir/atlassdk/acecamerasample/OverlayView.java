package com.flir.atlassdk.acecamerasample;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

public class OverlayView extends View {

    private List<Keypoint> keypoints;
    private List<ROIResult> rois;

    private Paint keypointPaint;
    private Paint roiPaintNormal;
    private Paint roiPaintHigh;
    private Paint textPaint;

    public OverlayView(Context context) {
        super(context);
        init();
    }

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        keypointPaint = new Paint();
        keypointPaint.setColor(0xFFFFFF00);
        keypointPaint.setStyle(Paint.Style.FILL);

        roiPaintNormal = new Paint();
        roiPaintNormal.setColor(0xFF00FF00);
        roiPaintNormal.setStyle(Paint.Style.STROKE);
        roiPaintNormal.setStrokeWidth(4f);

        roiPaintHigh = new Paint();
        roiPaintHigh.setColor(0xFFFF0000);
        roiPaintHigh.setStyle(Paint.Style.STROKE);
        roiPaintHigh.setStrokeWidth(4f);

        textPaint = new Paint();
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(32f);
    }

    public void update(List<Keypoint> keypoints, List<ROIResult> rois) {
        this.keypoints = keypoints;
        this.rois = rois;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (keypoints != null) {
            for (Keypoint kp : keypoints) {
                canvas.drawCircle(kp.x, kp.y, 8f, keypointPaint);
                canvas.drawText(kp.name, kp.x + 10, kp.y - 10, textPaint);
            }
        }

        if (rois != null && keypoints != null) {
            for (ROIResult roi : rois) {

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

                float size = 120f;
                RectF box = new RectF(
                        anchor.x - size / 2,
                        anchor.y - size / 2,
                        anchor.x + size / 2,
                        anchor.y + size / 2
                );

                canvas.drawRect(box, paint);
                canvas.drawText(
                        roi.name + " " + String.format("%.1f°C", roi.meanTempC),
                        box.left,
                        box.top - 10,
                        textPaint
                );
            }
        }
    }
}
