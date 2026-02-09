package com.flir.atlassdk.acecamerasample.tracking;

import android.content.Context;
import android.util.Log;
import com.flir.atlassdk.acecamerasample.storage.ScanRecord;
import com.flir.atlassdk.acecamerasample.storage.ScanStorage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnimalTracker {
    
    private static final String TAG = "AnimalTracker";
    
    private final ScanStorage scanStorage;
    private final AnimalIDGenerator idGenerator;
    private Map<String, AnimalProfile> profiles;
    
    public AnimalTracker(Context context, ScanStorage scanStorage) {
        this.scanStorage = scanStorage;
        this.idGenerator = new AnimalIDGenerator(context);
        this.profiles = new HashMap<>();
        
        // Load existing profiles
        loadProfiles();
    }
    
    /**
     * Load all animal profiles from scan history
     */
    private void loadProfiles() {
        List<ScanRecord> allScans = scanStorage.getAllScans();
        
        for (ScanRecord scan : allScans) {
            String animalId = scan.animalId;
            
            if (!profiles.containsKey(animalId)) {
                profiles.put(animalId, new AnimalProfile(animalId));
            }
            
            profiles.get(animalId).addScan(scan);
        }
        
        Log.d(TAG, "Loaded " + profiles.size() + " animal profiles");
    }
    
    /**
     * Get or create profile for animal
     */
    public AnimalProfile getProfile(String animalId) {
        if (!profiles.containsKey(animalId)) {
            profiles.put(animalId, new AnimalProfile(animalId));
        }
        return profiles.get(animalId);
    }
    
    /**
     * Get all animal profiles
     */
    public Map<String, AnimalProfile> getAllProfiles() {
        return profiles;
    }
    
    /**
     * Get all animal IDs
     */
    public List<String> getAllAnimalIds() {
        return new java.util.ArrayList<>(profiles.keySet());
    }
    
    /**
     * Get total number of tracked animals
     */
    public int getAnimalCount() {
        return profiles.size();
    }
    
    /**
     * Generate next animal ID
     */
    public String generateNextAnimalID() {
        return idGenerator.generateNextID();
    }
    
    /**
     * Update profile with new scan
     */
    public void updateProfile(ScanRecord scan) {
        AnimalProfile profile = getProfile(scan.animalId);
        profile.addScan(scan);
        
        Log.d(TAG, "Updated profile: " + profile.toString());
    }
    
    /**
     * Get animals with suspected status
     */
    public Map<String, AnimalProfile> getSuspectedAnimals() {
        Map<String, AnimalProfile> suspected = new HashMap<>();
        
        for (Map.Entry<String, AnimalProfile> entry : profiles.entrySet()) {
            AnimalProfile profile = entry.getValue();
            if (profile.suspectedScans > 0) {
                suspected.put(entry.getKey(), profile);
            }
        }
        
        return suspected;
    }
    
    /**
     * Print summary of all animals
     */
    public void printSummary() {
        Log.d(TAG, "=== Animal Tracking Summary ===");
        Log.d(TAG, "Total animals tracked: " + getAnimalCount());
        
        for (AnimalProfile profile : profiles.values()) {
            Log.d(TAG, profile.toString());
        }
        
        Log.d(TAG, "Animals with suspected status: " + getSuspectedAnimals().size());
    }
}
