package com.flir.atlassdk.acecamerasample;

import android.content.Context;
import android.content.SharedPreferences;

import com.flir.atlassdk.acecamerasample.storage.ScanRecord;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ScanStorageFrontend {

    private static final String PREF_NAME = "scan_storage";
    private static final String KEY_SCANS = "scans";

    public static void saveScan(Context context, ScanRecord scan, String animalId) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        JSONArray array = new JSONArray();

        try {
            String existing = prefs.getString(KEY_SCANS, "[]");
            array = new JSONArray(existing);

            JSONObject obj = new JSONObject();
            obj.put("animalId", animalId);
            obj.put("temperature", scan.temperature);
            obj.put("status", scan.status);
            obj.put("time", scan.time);

            array.put(obj);

            prefs.edit().putString(KEY_SCANS, array.toString()).apply();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<ScanRecord> getScansForAnimal(Context context, String animalId) {
        List<ScanRecord> list = new ArrayList<>();

        SharedPreferences prefs =
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        try {
            JSONArray array = new JSONArray(prefs.getString(KEY_SCANS, "[]"));

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);

                if (!obj.getString("animalId").equals(animalId)) continue;

                list.add(new ScanRecord(
                        obj.getDouble("temperature"),
                        obj.getString("status"),
                        obj.getString("time")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }


    public static List<String> getAllAnimalIds(Context context) {
        List<String> ids = new ArrayList<>();

        SharedPreferences prefs =
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        try {
            JSONArray array = new JSONArray(prefs.getString(KEY_SCANS, "[]"));

            for (int i = 0; i < array.length(); i++) {
                String id = array.getJSONObject(i).getString("animalId");
                if (!ids.contains(id)) {
                    ids.add(id);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ids;
    }

}
