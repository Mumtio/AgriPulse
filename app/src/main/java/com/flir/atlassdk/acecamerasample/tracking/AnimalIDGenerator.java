package com.flir.atlassdk.acecamerasample.tracking;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AnimalIDGenerator {
    
    private static final String PREFS_NAME = "animal_id_prefs";
    private static final String KEY_COUNTER = "id_counter";
    
    private final SharedPreferences prefs;
    
    public AnimalIDGenerator(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Generate next unique animal ID
     * Format: CATTLE_YYYYMMDD_XXX
     */
    public String generateNextID() {
        // Get current date
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.US);
        String dateStr = sdf.format(new Date());
        
        // Get and increment counter
        int counter = prefs.getInt(KEY_COUNTER, 0) + 1;
        prefs.edit().putInt(KEY_COUNTER, counter).apply();
        
        // Format: CATTLE_20260207_001
        return String.format("CATTLE_%s_%03d", dateStr, counter);
    }
    
    /**
     * Reset counter (for testing)
     */
    public void resetCounter() {
        prefs.edit().putInt(KEY_COUNTER, 0).apply();
    }
    
    /**
     * Get current counter value
     */
    public int getCurrentCounter() {
        return prefs.getInt(KEY_COUNTER, 0);
    }
}
