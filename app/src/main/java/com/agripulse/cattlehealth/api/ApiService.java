package com.agripulse.cattlehealth.api;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API Service for communicating with Python backend
 * Follows the workflow: Frontend → Backend → Frontend
 */
public class ApiService {
    
    private static final String TAG = "ApiService";
    private static final String BASE_URL = "https://agripulse-backend-xvvz.onrender.com";
    // Production backend on Render
    
    private static ApiService instance;
    
    private ApiService() {}
    
    public static synchronized ApiService getInstance() {
        if (instance == null) {
            instance = new ApiService();
        }
        return instance;
    }
    
    /**
     * Step 1: Upload image and thermal data to backend
     * Backend will run Grounding DINO and return body part coordinates
     */
    public interface AnalyzeCallback {
        void onSuccess(AnalyzeResponse response);
        void onError(String error);
    }
    
    public void analyzeImage(File imageFile, float[][] thermalData, String animalId, AnalyzeCallback callback) {
        new Thread(() -> {
            try {
                Log.d(TAG, "=== Starting image upload with OkHttp ===");
                Log.d(TAG, "Image file: " + imageFile.getAbsolutePath());
                Log.d(TAG, "File exists: " + imageFile.exists());
                Log.d(TAG, "File size: " + imageFile.length() + " bytes");
                Log.d(TAG, "File readable: " + imageFile.canRead());
                
                // Use OkHttp for reliable multipart upload
                okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                    .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)  // Increased from 30s
                    .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)    // Increased from 30s  
                    .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)    // Increased from 30s
                    .build();
                
                // Build multipart request body
                okhttp3.MultipartBody.Builder bodyBuilder = new okhttp3.MultipartBody.Builder()
                    .setType(okhttp3.MultipartBody.FORM);
                
                // Add image file
                okhttp3.RequestBody imageBody = okhttp3.RequestBody.create(
                    imageFile,
                    okhttp3.MediaType.parse("image/jpeg")
                );
                bodyBuilder.addFormDataPart("image", imageFile.getName(), imageBody);
                Log.d(TAG, "Added image file to multipart body");
                
                // Add animal ID if provided
                if (animalId != null) {
                    bodyBuilder.addFormDataPart("animal_id", animalId);
                    Log.d(TAG, "Added animal_id: " + animalId);
                }
                
                okhttp3.RequestBody requestBody = bodyBuilder.build();
                
                // Build request
                okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(BASE_URL + "/api/analyze")
                    .post(requestBody)
                    .build();
                
                Log.d(TAG, "Sending request to: " + BASE_URL + "/api/analyze");
                
                // Execute request
                okhttp3.Response response = client.newCall(request).execute();
                int responseCode = response.code();
                Log.d(TAG, "Response code: " + responseCode);
                
                if (responseCode == 200) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Success response: " + responseBody);
                    Log.d(TAG, "Response length: " + responseBody.length());
                    AnalyzeResponse analyzeResponse = parseAnalyzeResponse(responseBody);
                    Log.d(TAG, "Parsed response - Scan ID: " + analyzeResponse.scanId);
                    Log.d(TAG, "Body parts: " + (analyzeResponse.bodyParts != null ? analyzeResponse.bodyParts.size() : 0));
                    Log.d(TAG, "Thermal data: " + (analyzeResponse.thermalData != null ? analyzeResponse.thermalData.size() : 0));
                    Log.d(TAG, "Diagnosis: " + (analyzeResponse.diagnosis != null ? analyzeResponse.diagnosis.status : "null"));
                    callback.onSuccess(analyzeResponse);
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    Log.e(TAG, "Error response: " + errorBody);
                    callback.onError("HTTP " + responseCode + ": " + errorBody);
                }
                
                response.close();
                
            } catch (Exception e) {
                Log.e(TAG, "Error analyzing image", e);
                callback.onError(e.getMessage());
            }
        }).start();
    }
    
    /**
     * Step 2: Send temperature data for diagnosis
     * Backend will diagnose health and store in database
     */
    public interface DiagnoseCallback {
        void onSuccess(DiagnosisResponse response);
        void onError(String error);
    }
    
    public void diagnose(String scanId, String animalId, Map<String, TemperatureStats> temperatures, 
                        float ambientTemp, float humidity, DiagnoseCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "/api/diagnose");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                
                JSONObject json = new JSONObject();
                json.put("scan_id", scanId);
                json.put("animal_id", animalId);
                json.put("ambient_temp", ambientTemp);      // NEW: Environmental data
                json.put("relative_humidity", humidity);    // NEW: Environmental data
                json.put("use_baseline", true);             // NEW: Enable baseline tracking
                
                JSONObject tempsJson = new JSONObject();
                for (Map.Entry<String, TemperatureStats> entry : temperatures.entrySet()) {
                    JSONObject tempJson = new JSONObject();
                    tempJson.put("temp_mean", entry.getValue().tempMean);
                    tempJson.put("temp_max", entry.getValue().tempMax);
                    tempJson.put("temp_min", entry.getValue().tempMin);
                    tempJson.put("temp_std", entry.getValue().tempStd);
                    tempsJson.put(entry.getKey(), tempJson);
                }
                json.put("temperatures", tempsJson);
                
                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes(json.toString());
                dos.flush();
                dos.close();
                
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String response = readResponse(conn);
                    DiagnosisResponse diagnosisResponse = parseDiagnosisResponse(response);
                    callback.onSuccess(diagnosisResponse);
                } else {
                    String error = readError(conn);
                    callback.onError("HTTP " + responseCode + ": " + error);
                }
                
                conn.disconnect();
                
            } catch (Exception e) {
                Log.e(TAG, "Error diagnosing", e);
                callback.onError(e.getMessage());
            }
        }).start();
    }
    
    /**
     * Health check endpoint
     */
    public interface HealthCallback {
        void onSuccess(String message);
        void onError(String error);
    }
    
    public void checkHealth(HealthCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "/health");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String response = readResponse(conn);
                    JSONObject json = new JSONObject(response);
                    callback.onSuccess(json.getString("message"));
                } else {
                    callback.onError("HTTP " + responseCode);
                }
                
                conn.disconnect();
                
            } catch (Exception e) {
                Log.e(TAG, "Error checking health", e);
                callback.onError(e.getMessage());
            }
        }).start();
    }
    
    // Helper methods
    
    private String thermalDataToJson(float[][] thermalData) throws JSONException {
        JSONObject json = new JSONObject();
        JSONArray array = new JSONArray();
        
        for (float[] row : thermalData) {
            JSONArray rowArray = new JSONArray();
            for (float value : row) {
                rowArray.put(value);
            }
            array.put(rowArray);
        }
        
        json.put("thermal_array", array);
        json.put("width", thermalData[0].length);
        json.put("height", thermalData.length);
        json.put("timestamp", System.currentTimeMillis());
        
        return json.toString();
    }
    
    private String readResponse(HttpURLConnection conn) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        return response.toString();
    }
    
    private String readError(HttpURLConnection conn) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        StringBuilder error = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            error.append(line);
        }
        reader.close();
        return error.toString();
    }
    
    private AnalyzeResponse parseAnalyzeResponse(String response) throws JSONException {
        Log.d(TAG, "Parsing analyze response...");
        JSONObject json = new JSONObject(response);
        AnalyzeResponse analyzeResponse = new AnalyzeResponse();
        
        try {
            analyzeResponse.scanId = json.getString("scan_id");
            analyzeResponse.timestamp = json.getString("timestamp");
            Log.d(TAG, "Basic fields parsed - Scan ID: " + analyzeResponse.scanId);
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing basic fields", e);
            throw e;
        }
        
        // Parse body parts
        try {
            if (json.has("body_parts")) {
                JSONObject bodyParts = json.getJSONObject("body_parts");
                analyzeResponse.bodyParts = new HashMap<>();
                
                JSONArray names = bodyParts.names();
                if (names != null) {
                    for (int i = 0; i < names.length(); i++) {
                        String partName = names.getString(i);
                        JSONArray bbox = bodyParts.getJSONArray(partName);
                        int[] coords = new int[4];
                        for (int j = 0; j < 4; j++) {
                            coords[j] = bbox.getInt(j);
                        }
                        analyzeResponse.bodyParts.put(partName, coords);
                    }
                }
                Log.d(TAG, "Body parts parsed: " + analyzeResponse.bodyParts.size());
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing body parts", e);
        }
        
        // Parse thermal data
        try {
            if (json.has("thermal_data")) {
                JSONObject thermalData = json.getJSONObject("thermal_data");
                analyzeResponse.thermalData = new HashMap<>();
                
                JSONArray names = thermalData.names();
                if (names != null) {
                    for (int i = 0; i < names.length(); i++) {
                        String partName = names.getString(i);
                        JSONObject tempStats = thermalData.getJSONObject(partName);
                        
                        TemperatureStats stats = new TemperatureStats(
                            (float) tempStats.getDouble("temp_mean"),
                            (float) tempStats.getDouble("temp_max"),
                            (float) tempStats.getDouble("temp_min"),
                            (float) tempStats.getDouble("temp_std")
                        );
                        analyzeResponse.thermalData.put(partName, stats);
                    }
                }
                Log.d(TAG, "Thermal data parsed: " + analyzeResponse.thermalData.size());
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing thermal data", e);
        }
        
        // Parse diagnosis
        try {
            if (json.has("diagnosis")) {
                JSONObject diagnosisJson = json.getJSONObject("diagnosis");
                DiagnosisResponse diagnosis = new DiagnosisResponse();
                diagnosis.status = diagnosisJson.getString("status");
                
                // Parse alerts
                JSONArray alerts = diagnosisJson.getJSONArray("alerts");
                diagnosis.alerts = new String[alerts.length()];
                for (int i = 0; i < alerts.length(); i++) {
                    diagnosis.alerts[i] = alerts.getString(i);
                }
                
                // Parse recommendations
                JSONArray recommendations = diagnosisJson.getJSONArray("recommendations");
                diagnosis.recommendations = new String[recommendations.length()];
                for (int i = 0; i < recommendations.length(); i++) {
                    diagnosis.recommendations[i] = recommendations.getString(i);
                }
                
                analyzeResponse.diagnosis = diagnosis;
                Log.d(TAG, "Diagnosis parsed - Status: " + diagnosis.status + ", Alerts: " + diagnosis.alerts.length);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing diagnosis", e);
        }
        
        Log.d(TAG, "Response parsing complete");
        return analyzeResponse;
    }
    
    private DiagnosisResponse parseDiagnosisResponse(String response) throws JSONException {
        JSONObject json = new JSONObject(response);
        DiagnosisResponse diagnosisResponse = new DiagnosisResponse();
        
        JSONObject diagnosis = json.getJSONObject("diagnosis");
        diagnosisResponse.status = diagnosis.getString("status");
        
        // Parse alerts
        JSONArray alerts = diagnosis.getJSONArray("alerts");
        diagnosisResponse.alerts = new String[alerts.length()];
        for (int i = 0; i < alerts.length(); i++) {
            JSONObject alert = alerts.getJSONObject(i);
            diagnosisResponse.alerts[i] = alert.getString("part") + ": " + alert.getString("issue");
        }
        
        // Parse recommendations
        JSONArray recommendations = diagnosis.getJSONArray("recommendations");
        diagnosisResponse.recommendations = new String[recommendations.length()];
        for (int i = 0; i < recommendations.length(); i++) {
            diagnosisResponse.recommendations[i] = recommendations.getString(i);
        }
        
        return diagnosisResponse;
    }
    
    /**
     * Get all animals from backend database
     */
    public void getAnimals(AnimalsCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "/api/animals");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String response = readResponse(conn);
                    List<AnimalInfo> animals = parseAnimalsResponse(response);
                    callback.onSuccess(animals);
                } else {
                    String error = readError(conn);
                    callback.onError("HTTP " + responseCode + ": " + error);
                }
                
                conn.disconnect();
                
            } catch (Exception e) {
                Log.e(TAG, "Error fetching animals", e);
                callback.onError(e.getMessage());
            }
        }).start();
    }
    
    /**
     * Get scan history for a specific animal
     */
    public void getAnimalScans(String animalId, AnimalScansCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "/api/animals/" + animalId + "/scans");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String response = readResponse(conn);
                    List<ScanInfo> scans = parseScansResponse(response);
                    callback.onSuccess(scans);
                } else {
                    String error = readError(conn);
                    callback.onError("HTTP " + responseCode + ": " + error);
                }
                
                conn.disconnect();
                
            } catch (Exception e) {
                Log.e(TAG, "Error fetching animal scans", e);
                callback.onError(e.getMessage());
            }
        }).start();
    }
    
    /**
     * Get detailed scan information
     */
    public void getScanDetails(String scanId, ScanDetailsCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "/api/scans/" + scanId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String response = readResponse(conn);
                    ScanDetails details = parseScanDetailsResponse(response);
                    callback.onSuccess(details);
                } else {
                    String error = readError(conn);
                    callback.onError("HTTP " + responseCode + ": " + error);
                }
                
                conn.disconnect();
                
            } catch (Exception e) {
                Log.e(TAG, "Error fetching scan details", e);
                callback.onError(e.getMessage());
            }
        }).start();
    }
    
    private List<AnimalInfo> parseAnimalsResponse(String response) throws JSONException {
        JSONObject json = new JSONObject(response);
        JSONArray animalsArray = json.getJSONArray("animals");
        
        List<AnimalInfo> animals = new ArrayList<>();
        for (int i = 0; i < animalsArray.length(); i++) {
            JSONObject animalJson = animalsArray.getJSONObject(i);
            AnimalInfo animal = new AnimalInfo();
            animal.id = animalJson.getString("id");
            animal.tagId = animalJson.optString("tag_id", "");
            animal.name = animalJson.optString("name", "");
            animal.breed = animalJson.optString("breed", "Cattle");
            animal.age = animalJson.optInt("age", 0);
            animal.scanCount = animalJson.optInt("scan_count", 0);
            animals.add(animal);
        }
        
        return animals;
    }
    
    private List<ScanInfo> parseScansResponse(String response) throws JSONException {
        JSONObject json = new JSONObject(response);
        JSONArray scansArray = json.getJSONArray("scans");
        
        List<ScanInfo> scans = new ArrayList<>();
        for (int i = 0; i < scansArray.length(); i++) {
            JSONObject scanJson = scansArray.getJSONObject(i);
            ScanInfo scan = new ScanInfo();
            scan.id = scanJson.getString("id");
            scan.timestamp = scanJson.getString("timestamp");
            scan.status = scanJson.optString("status", "unknown");
            scan.alertCount = scanJson.optInt("alert_count", 0);
            scans.add(scan);
        }
        
        return scans;
    }
    
    private ScanDetails parseScanDetailsResponse(String response) throws JSONException {
        JSONObject json = new JSONObject(response);
        JSONObject scanJson = json.getJSONObject("scan");
        
        ScanDetails details = new ScanDetails();
        details.id = scanJson.getString("id");
        details.timestamp = scanJson.getString("timestamp");
        details.animalId = scanJson.optString("animal_id", "");
        details.status = scanJson.optString("status", "unknown");
        
        // Parse detections
        if (scanJson.has("detections")) {
            JSONArray detectionsArray = scanJson.getJSONArray("detections");
            details.detections = new ArrayList<>();
            for (int i = 0; i < detectionsArray.length(); i++) {
                JSONObject detection = detectionsArray.getJSONObject(i);
                details.detections.add(detection.getString("label"));
            }
        }
        
        // Parse temperatures
        if (scanJson.has("temperatures")) {
            JSONArray tempsArray = scanJson.getJSONArray("temperatures");
            details.temperatures = new HashMap<>();
            for (int i = 0; i < tempsArray.length(); i++) {
                JSONObject temp = tempsArray.getJSONObject(i);
                String part = temp.getString("body_part");
                float mean = (float) temp.getDouble("temp_mean");
                details.temperatures.put(part, mean);
            }
        }
        
        // Parse diagnosis
        if (scanJson.has("diagnosis")) {
            JSONObject diagnosisJson = scanJson.getJSONObject("diagnosis");
            details.diagnosisStatus = diagnosisJson.optString("status", "unknown");
            
            if (diagnosisJson.has("recommendations")) {
                JSONArray recsArray = diagnosisJson.getJSONArray("recommendations");
                details.recommendations = new ArrayList<>();
                for (int i = 0; i < recsArray.length(); i++) {
                    details.recommendations.add(recsArray.getString(i));
                }
            }
        }
        
        return details;
    }
    
    // Callback interfaces
    
    public interface AnimalsCallback {
        void onSuccess(List<AnimalInfo> animals);
        void onError(String error);
    }
    
    public interface AnimalScansCallback {
        void onSuccess(List<ScanInfo> scans);
        void onError(String error);
    }
    
    public interface ScanDetailsCallback {
        void onSuccess(ScanDetails details);
        void onError(String error);
    }
    
    // Response classes
    
    public static class AnimalInfo {
        public String id;
        public String tagId;
        public String name;
        public String breed;
        public int age;
        public int scanCount;
    }
    
    public static class ScanInfo {
        public String id;
        public String timestamp;
        public String status;
        public int alertCount;
    }
    
    public static class ScanDetails {
        public String id;
        public String timestamp;
        public String animalId;
        public String status;
        public List<String> detections;
        public Map<String, Float> temperatures;
        public String diagnosisStatus;
        public List<String> recommendations;
    }
    
    // Response classes
    
    public static class AnalyzeResponse {
        public String scanId;
        public String timestamp;
        public Map<String, int[]> bodyParts; // part_name -> [x1, y1, x2, y2]
        public Map<String, TemperatureStats> thermalData; // part_name -> thermal stats
        public DiagnosisResponse diagnosis; // health diagnosis
    }
    
    public static class DiagnosisResponse {
        public String status; // "healthy" or "attention_needed"
        public String[] alerts;
        public String[] recommendations;
    }
    
    public static class TemperatureStats {
        public float tempMean;
        public float tempMax;
        public float tempMin;
        public float tempStd;
        
        public TemperatureStats(float mean, float max, float min, float std) {
            this.tempMean = mean;
            this.tempMax = max;
            this.tempMin = min;
            this.tempStd = std;
        }
    }
}
