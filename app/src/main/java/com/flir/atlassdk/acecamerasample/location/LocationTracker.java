package com.flir.atlassdk.acecamerasample.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;
import androidx.core.app.ActivityCompat;

public class LocationTracker {
    
    private static final String TAG = "LocationTracker";
    private final Context context;
    private final LocationManager locationManager;
    
    public LocationTracker(Context context) {
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }
    
    /**
     * Get current location (simple, no callbacks)
     * Returns mock location if permission denied
     */
    public LocationData getCurrentLocation() {
        // Check permission
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permission not granted, using mock location");
            return getMockLocation();
        }
        
        try {
            // Try GPS first
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            
            // Fallback to network
            if (location == null) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            
            if (location != null) {
                Log.d(TAG, "Got location: " + location.getLatitude() + ", " + location.getLongitude());
                return new LocationData(location.getLatitude(), location.getLongitude(), true);
            } else {
                Log.w(TAG, "No location available, using mock");
                return getMockLocation();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting location: " + e.getMessage());
            return getMockLocation();
        }
    }
    
    /**
     * Get mock location (for demo/testing)
     */
    private LocationData getMockLocation() {
        // Mock farm location (somewhere in California)
        return new LocationData(36.7783, -119.4179, false);
    }
    
    /**
     * Format location as string
     */
    public static String formatLocation(double latitude, double longitude) {
        String latDir = latitude >= 0 ? "N" : "S";
        String lonDir = longitude >= 0 ? "E" : "W";
        
        return String.format("%.4f°%s, %.4f°%s", 
            Math.abs(latitude), latDir, Math.abs(longitude), lonDir);
    }
    
    /**
     * Location data container
     */
    public static class LocationData {
        public double latitude;
        public double longitude;
        public boolean isReal;  // false if mock
        
        public LocationData(double latitude, double longitude, boolean isReal) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.isReal = isReal;
        }
        
        @Override
        public String toString() {
            return formatLocation(latitude, longitude) + (isReal ? "" : " (mock)");
        }
    }
}
