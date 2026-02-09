package com.flir.atlassdk.acecamerasample.thermal;

import com.flir.atlassdk.acecamerasample.detection.Keypoint;
import com.flir.thermalsdk.image.ThermalImage;
import com.flir.thermalsdk.image.ThermalValue;
import com.flir.thermalsdk.image.TemperatureUnit;
import com.flir.thermalsdk.image.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ThermalROIAnalyzer {
    
    private static final int ROI_SIZE = 20; // 20x20 pixel ROI
    
    /**
     * Extract temperature statistics from ROI around a keypoint
     */
    public ROITemperature analyzeROI(ThermalImage thermalImage, Keypoint keypoint) {
        List<ThermalValue> temperatureValues = new ArrayList<>();
        
        int width = thermalImage.getWidth();
        int height = thermalImage.getHeight();
        
        // Extract temperatures from ROI
        int halfSize = ROI_SIZE / 2;
        for (int dy = -halfSize; dy < halfSize; dy++) {
            for (int dx = -halfSize; dx < halfSize; dx++) {
                int x = keypoint.x + dx;
                int y = keypoint.y + dy;
                
                // Check bounds
                if (x >= 0 && x < width && y >= 0 && y < height) {
                    try {
                        // Get temperature at this pixel using FLIR SDK Point
                        Point p = new Point(x, y);
                        ThermalValue temp = thermalImage.getValueAt(p);
                        temperatureValues.add(temp);
                    } catch (Exception e) {
                        // Skip invalid pixels
                    }
                }
            }
        }
        
        if (temperatureValues.isEmpty()) {
            return new ROITemperature(keypoint.name, 0, 0, 0, 0);
        }
        
        // Convert ThermalValue to double for calculations
        // Parse the string representation (e.g., "296.5K" -> 296.5)
        List<Double> temperatures = new ArrayList<>();
        for (ThermalValue tv : temperatureValues) {
            try {
                String str = tv.toString();
                // Remove the 'K' suffix and parse
                String numStr = str.replace("K", "").replace("°C", "").replace("°F", "").trim();
                temperatures.add(Double.parseDouble(numStr));
            } catch (Exception e) {
                // Skip invalid values
            }
        }
        
        if (temperatures.isEmpty()) {
            return new ROITemperature(keypoint.name, 0, 0, 0, 0);
        }
        
        // Calculate statistics
        Collections.sort(temperatures);
        
        double t95 = calculatePercentile(temperatures, 0.95);
        double mean = calculateMean(temperatures);
        double std = calculateStd(temperatures, mean);
        
        return new ROITemperature(keypoint.name, t95, mean, std, temperatures.size());
    }
    
    /**
     * Calculate percentile (e.g., 0.95 for 95th percentile)
     */
    private double calculatePercentile(List<Double> sortedValues, double percentile) {
        if (sortedValues.isEmpty()) return 0;
        int index = (int) Math.ceil(sortedValues.size() * percentile) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(index);
    }
    
    /**
     * Calculate mean (average)
     */
    private double calculateMean(List<Double> values) {
        if (values.isEmpty()) return 0;
        double sum = 0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.size();
    }
    
    /**
     * Calculate standard deviation
     */
    private double calculateStd(List<Double> values, double mean) {
        if (values.size() < 2) return 0;
        double sumSquaredDiff = 0;
        for (double v : values) {
            double diff = v - mean;
            sumSquaredDiff += diff * diff;
        }
        return Math.sqrt(sumSquaredDiff / (values.size() - 1));
    }
}
