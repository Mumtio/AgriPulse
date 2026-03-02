package com.agripulse.cattlehealth.thermal;

import com.agripulse.cattlehealth.api.ApiService;

import java.util.HashMap;
import java.util.Map;

/**
 * Extracts temperature statistics from thermal data using body part coordinates
 * This runs on the frontend after receiving coordinates from backend
 */
public class ThermalExtractor {
    
    /**
     * Extract temperatures for all body parts
     * 
     * @param thermalData Full thermal array from FLIR camera
     * @param bodyParts Body part coordinates from backend
     * @return Map of body part name to temperature statistics
     */
    public static Map<String, ApiService.TemperatureStats> extractTemperatures(
            float[][] thermalData,
            Map<String, int[]> bodyParts) {
        
        Map<String, ApiService.TemperatureStats> temperatures = new HashMap<>();
        
        for (Map.Entry<String, int[]> entry : bodyParts.entrySet()) {
            String partName = entry.getKey();
            int[] bbox = entry.getValue();
            
            ApiService.TemperatureStats stats = extractRegionTemperature(thermalData, bbox);
            temperatures.put(partName, stats);
        }
        
        return temperatures;
    }
    
    /**
     * Extract temperature statistics for a specific region
     * 
     * @param thermalData Full thermal array
     * @param bbox Bounding box [x1, y1, x2, y2]
     * @return Temperature statistics
     */
    public static ApiService.TemperatureStats extractRegionTemperature(
            float[][] thermalData,
            int[] bbox) {
        
        int x1 = bbox[0];
        int y1 = bbox[1];
        int x2 = bbox[2];
        int y2 = bbox[3];
        
        // Ensure coordinates are within bounds
        int height = thermalData.length;
        int width = thermalData[0].length;
        
        x1 = Math.max(0, Math.min(x1, width - 1));
        y1 = Math.max(0, Math.min(y1, height - 1));
        x2 = Math.max(0, Math.min(x2, width));
        y2 = Math.max(0, Math.min(y2, height));
        
        // Extract region
        float sum = 0;
        float max = Float.MIN_VALUE;
        float min = Float.MAX_VALUE;
        int count = 0;
        
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                float temp = thermalData[y][x];
                sum += temp;
                max = Math.max(max, temp);
                min = Math.min(min, temp);
                count++;
            }
        }
        
        if (count == 0) {
            return new ApiService.TemperatureStats(0, 0, 0, 0);
        }
        
        float mean = sum / count;
        
        // Calculate standard deviation
        float sumSquaredDiff = 0;
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                float diff = thermalData[y][x] - mean;
                sumSquaredDiff += diff * diff;
            }
        }
        float std = (float) Math.sqrt(sumSquaredDiff / count);
        
        return new ApiService.TemperatureStats(mean, max, min, std);
    }
    
    /**
     * Simulate thermal data for testing (when no real FLIR camera)
     * 
     * @param width Width of thermal array
     * @param height Height of thermal array
     * @return Simulated thermal data
     */
    public static float[][] simulateThermalData(int width, int height) {
        return simulateThermalData(width, height, "healthy");
    }
    
    /**
     * Simulate thermal data with specific health scenario
     * 
     * @param width Width of thermal array
     * @param height Height of thermal array
     * @param scenario Health scenario: "healthy", "mastitis", "lameness", "fever"
     * @return Simulated thermal data
     */
    public static float[][] simulateThermalData(int width, int height, String scenario) {
        float[][] thermalData = new float[height][width];
        
        // Base temperature around 34°C (skin surface temp)
        float baseTemp = 34.0f;
        
        // Generate base thermal pattern
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Add natural variation
                thermalData[y][x] = baseTemp + (float) (Math.random() * 1.0 - 0.5);
                
                // Warmer in center (body core)
                float distFromCenter = (float) Math.sqrt(
                    Math.pow(x - width/2.0, 2) + Math.pow(y - height/2.0, 2)
                );
                thermalData[y][x] += 1.0f * (float) Math.exp(-distFromCenter / 200.0);
                
                // Cooler at extremities
                if (y < height / 4) {
                    thermalData[y][x] -= 2.0f; // Head area
                }
                if (y > 3 * height / 4) {
                    thermalData[y][x] -= 3.0f; // Legs
                }
            }
        }
        
        // Add scenario-specific patterns
        switch (scenario.toLowerCase()) {
            case "mastitis":
                addMastitisPattern(thermalData, width, height);
                break;
            case "lameness":
                addLamenessPattern(thermalData, width, height);
                break;
            case "fever":
                addFeverPattern(thermalData, width, height);
                break;
            case "healthy":
            default:
                // No additional pattern
                break;
        }
        
        // Ensure realistic range (27-38°C)
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                thermalData[y][x] = Math.max(27.0f, Math.min(38.0f, thermalData[y][x]));
            }
        }
        
        return thermalData;
    }
    
    /**
     * Add mastitis pattern (elevated udder temperature)
     */
    private static void addMastitisPattern(float[][] thermal, int width, int height) {
        int udderYStart = (int) (height * 0.6);
        int udderYEnd = (int) (height * 0.8);
        int udderXStart = (int) (width * 0.35);
        int udderXEnd = (int) (width * 0.65);
        
        float elevation = 3.0f; // +3°C above normal
        
        for (int y = udderYStart; y < udderYEnd; y++) {
            for (int x = udderXStart; x < udderXEnd; x++) {
                int centerY = (udderYStart + udderYEnd) / 2;
                int centerX = (udderXStart + udderXEnd) / 2;
                float dist = (float) Math.sqrt(
                    Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2)
                );
                thermal[y][x] += elevation * (float) Math.exp(-dist / 50.0);
            }
        }
    }
    
    /**
     * Add lameness pattern (hoof asymmetry)
     */
    private static void addLamenessPattern(float[][] thermal, int width, int height) {
        int hoofYStart = (int) (height * 0.85);
        int hoofYEnd = height;
        int rightHoofXStart = (int) (width * 0.6);
        int rightHoofXEnd = (int) (width * 0.8);
        
        float elevation = 3.5f; // +3.5°C on affected hoof
        
        for (int y = hoofYStart; y < hoofYEnd; y++) {
            for (int x = rightHoofXStart; x < rightHoofXEnd; x++) {
                int centerY = (hoofYStart + hoofYEnd) / 2;
                int centerX = (rightHoofXStart + rightHoofXEnd) / 2;
                float dist = (float) Math.sqrt(
                    Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2)
                );
                thermal[y][x] += elevation * (float) Math.exp(-dist / 30.0);
            }
        }
    }
    
    /**
     * Add fever pattern (elevated eye/head temperature)
     */
    private static void addFeverPattern(float[][] thermal, int width, int height) {
        int headYStart = (int) (height * 0.1);
        int headYEnd = (int) (height * 0.3);
        int headXStart = (int) (width * 0.4);
        int headXEnd = (int) (width * 0.6);
        
        float elevation = 2.0f; // +2°C fever
        
        for (int y = headYStart; y < headYEnd; y++) {
            for (int x = headXStart; x < headXEnd; x++) {
                int centerY = (headYStart + headYEnd) / 2;
                int centerX = (headXStart + headXEnd) / 2;
                float dist = (float) Math.sqrt(
                    Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2)
                );
                thermal[y][x] += elevation * (float) Math.exp(-dist / 40.0);
            }
        }
    }
}
