package com.flir.atlassdk.acecamerasample.detection;

import android.graphics.Rect;
import java.util.ArrayList;
import java.util.List;

public class DetectionResult {
    public List<Keypoint> keypoints;
    public Rect boundingBox;
    public float overallConfidence;
    public String species;
    
    public DetectionResult() {
        this.keypoints = new ArrayList<>();
        this.overallConfidence = 0.0f;
        this.species = "cattle";
    }
    
    public Keypoint getKeypoint(String name) {
        for (Keypoint kp : keypoints) {
            if (kp.name.equals(name)) {
                return kp;
            }
        }
        return null;
    }
}
