package com.flir.atlassdk.acecamerasample;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.flir.atlassdk.acecamerasample.storage.ScanRecord;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_history, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.animalRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Get backend modules from MainActivity
        MainActivity mainActivity = (MainActivity) requireActivity();
        com.flir.atlassdk.acecamerasample.storage.ScanStorage scanStorage = mainActivity.getScanStorage();
        com.flir.atlassdk.acecamerasample.tracking.AnimalTracker animalTracker = mainActivity.getAnimalTracker();

        List<Animal> animals = new ArrayList<>();

        // Get all animal IDs from tracker
        List<String> animalIds = animalTracker.getAllAnimalIds();

        for (String animalId : animalIds) {
            // Get animal profile from tracker
            com.flir.atlassdk.acecamerasample.tracking.AnimalProfile profile = 
                animalTracker.getProfile(animalId);
            
            if (profile == null || profile.totalScans == 0) continue;

            // Get scans for this animal from storage
            List<ScanRecord> scans = scanStorage.getScansForAnimal(animalId);

            if (scans.isEmpty()) continue;

            // LAST scan = most recent
            ScanRecord last = scans.get(scans.size() - 1);

            // Ensure status is not null - map from overallStatus if needed
            String displayStatus = last.status;
            if (displayStatus == null) {
                displayStatus = "SUSPECTED".equals(last.overallStatus) ? "High" : "Normal";
            }

            animals.add(new Animal(
                    animalId,
                    last.species != null ? last.species : "Cattle",  // Use real species from detection
                    last.temperature,
                    displayStatus,
                    last.time
            ));
        }

        AnimalAdapter adapter = new AnimalAdapter(animals, animal -> {
            Fragment fragment = AnimalHistoryFragment.newInstance(animal.id, animal.type);

            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        recyclerView.setAdapter(adapter);

    }

}
