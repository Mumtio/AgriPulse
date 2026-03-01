package com.flir.atlassdk.acecamerasample.detection;

import android.graphics.Rect;

public class MockAnimalDetector {
    
    /**
     * Mock detection - returns hardcoded keypoints for a cattle in side view
     * In real implementation, this would run YOLOv8-pose model
     */
    public DetectionResult detectAnimal(int imageWidth, int imageHeight) {
        DetectionResult result = new DetectionResult();
        
        // Scale keypoints to image size (assuming 720x960 base)
        float scaleX = imageWidth / 720.0f;
        float scaleY = imageHeight / 960.0f;
        
        // Add keypoints (scaled to image size)
        result.keypoints.add(new Keypoint("left_eye", 
            (int)(200 * scaleX), (int)(150 * scaleY), 0.92f));
        result.keypoints.add(new Keypoint("right_eye", 
            (int)(400 * scaleX), (int)(150 * scaleY), 0.91f));
        result.keypoints.add(new Keypoint("udder", 
            (int)(300 * scaleX), (int)(400 * scaleY), 0.88f));
        result.keypoints.add(new Keypoint("left_front_hoof", 
            (int)(250 * scaleX), (int)(550 * scaleY), 0.85f));
        result.keypoints.add(new Keypoint("right_front_hoof", 
            (int)(350 * scaleX), (int)(550 * scaleY), 0.87f));
        result.keypoints.add(new Keypoint("left_rear_hoof", 
            (int)(270 * scaleX), (int)(580 * scaleY), 0.83f));
        result.keypoints.add(new Keypoint("right_rear_hoof", 
            (int)(330 * scaleX), (int)(580 * scaleY), 0.84f));
        
        // Calculate bounding box from keypoints
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        
        for (Keypoint kp : result.keypoints) {
            if (kp.x < minX) minX = kp.x;
            if (kp.y < minY) minY = kp.y;
            if (kp.x > maxX) maxX = kp.x;
            if (kp.y > maxY) maxY = kp.y;
        }
        
        // Add padding to bounding box
        int padding = (int)(20 * scaleX);
        result.boundingBox = new Rect(
            Math.max(0, minX - padding),
            Math.max(0, minY - padding),
            Math.min(imageWidth, maxX + padding),
            Math.min(imageHeight, maxY + padding)
        );
        
        // Calculate overall confidence (average of all keypoints)
        float sum = 0;
        for (Keypoint kp : result.keypoints) {
            sum += kp.confidence;
        }
        result.overallConfidence = sum / result.keypoints.size();
        
        result.species = "cattle";
        
        return result;
    }
    
    /**
     * Detect multiple animals (for batch scanning demo)
     * Returns 2-3 mock animals with different positions
     */
    public java.util.List<DetectionResult> detectMultipleAnimals(int imageWidth, int imageHeight) {
        java.util.List<DetectionResult> results = new java.util.ArrayList<>();
        
        float scaleX = imageWidth / 720.0f;
        float scaleY = imageHeight / 960.0f;
        
        // Animal 1 - Left side
        DetectionResult animal1 = new DetectionResult();
        animal1.species = "cattle";
        animal1.overallConfidence = 0.89f;
        
        int offsetX1 = -150;
        animal1.keypoints.add(new Keypoint("left_eye", (int)((200 + offsetX1) * scaleX), (int)(150 * scaleY), 0.92f));
        animal1.keypoints.add(new Keypoint("right_eye", (int)((400 + offsetX1) * scaleX), (int)(150 * scaleY), 0.91f));
        animal1.keypoints.add(new Keypoint("udder", (int)((300 + offsetX1) * scaleX), (int)(400 * scaleY), 0.88f));
        animal1.keypoints.add(new Keypoint("left_front_hoof", (int)((250 + offsetX1) * scaleX), (int)(550 * scaleY), 0.85f));
        animal1.keypoints.add(new Keypoint("right_front_hoof", (int)((350 + offsetX1) * scaleX), (int)(550 * scaleY), 0.87f));
        animal1.keypoints.add(new Keypoint("left_rear_hoof", (int)((270 + offsetX1) * scaleX), (int)(580 * scaleY), 0.83f));
        animal1.keypoints.add(new Keypoint("right_rear_hoof", (int)((330 + offsetX1) * scaleX), (int)(580 * scaleY), 0.84f));
        
        results.add(animal1);
        
        // Animal 2 - Right side
        DetectionResult animal2 = new DetectionResult();
        animal2.species = "cattle";
        animal2.overallConfidence = 0.86f;
        
        int offsetX2 = 150;
        animal2.keypoints.add(new Keypoint("left_eye", (int)((200 + offsetX2) * scaleX), (int)(150 * scaleY), 0.90f));
        animal2.keypoints.add(new Keypoint("right_eye", (int)((400 + offsetX2) * scaleX), (int)(150 * scaleY), 0.89f));
        animal2.keypoints.add(new Keypoint("udder", (int)((300 + offsetX2) * scaleX), (int)(400 * scaleY), 0.86f));
        animal2.keypoints.add(new Keypoint("left_front_hoof", (int)((250 + offsetX2) * scaleX), (int)(550 * scaleY), 0.84f));
        animal2.keypoints.add(new Keypoint("right_front_hoof", (int)((350 + offsetX2) * scaleX), (int)(550 * scaleY), 0.85f));
        animal2.keypoints.add(new Keypoint("left_rear_hoof", (int)((270 + offsetX2) * scaleX), (int)(580 * scaleY), 0.82f));
        animal2.keypoints.add(new Keypoint("right_rear_hoof", (int)((330 + offsetX2) * scaleX), (int)(580 * scaleY), 0.83f));
        
        results.add(animal2);
        
        // Randomly add 3rd animal (50% chance)
        if (Math.random() > 0.5) {
            DetectionResult animal3 = new DetectionResult();
            animal3.species = "cattle";
            animal3.overallConfidence = 0.82f;
            
            animal3.keypoints.add(new Keypoint("left_eye", (int)(200 * scaleX), (int)(150 * scaleY), 0.88f));
            animal3.keypoints.add(new Keypoint("right_eye", (int)(400 * scaleX), (int)(150 * scaleY), 0.87f));
            animal3.keypoints.add(new Keypoint("udder", (int)(300 * scaleX), (int)(400 * scaleY), 0.84f));
            animal3.keypoints.add(new Keypoint("left_front_hoof", (int)(250 * scaleX), (int)(550 * scaleY), 0.82f));
            animal3.keypoints.add(new Keypoint("right_front_hoof", (int)(350 * scaleX), (int)(550 * scaleY), 0.83f));
            animal3.keypoints.add(new Keypoint("left_rear_hoof", (int)(270 * scaleX), (int)(580 * scaleY), 0.80f));
            animal3.keypoints.add(new Keypoint("right_rear_hoof", (int)(330 * scaleX), (int)(580 * scaleY), 0.81f));
            
            results.add(animal3);
        }
        
        return results;
    }
}
