package com.agripulse.cattlehealth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.agripulse.cattlehealth.api.ApiService;

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

        // Back button
        ImageButton backButton = view.findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        RecyclerView recyclerView = view.findViewById(R.id.animalRecyclerView);
        View emptyState = view.findViewById(R.id.emptyState);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Fetch animals from backend database
        fetchAnimalsFromBackend(recyclerView, emptyState);
    }
    
    private void fetchAnimalsFromBackend(RecyclerView recyclerView, View emptyState) {
        // Show loading state
        emptyState.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        
        ApiService.getInstance().getAnimals(new ApiService.AnimalsCallback() {
            @Override
            public void onSuccess(List<ApiService.AnimalInfo> animalInfos) {
                if (getActivity() == null) return;
                
                getActivity().runOnUiThread(() -> {
                    List<Animal> animals = new ArrayList<>();
                    
                    // Convert backend data to Animal objects
                    for (ApiService.AnimalInfo info : animalInfos) {
                        // Get last scan status (will be fetched separately if needed)
                        animals.add(new Animal(
                            info.tagId.isEmpty() ? info.id : info.tagId,
                            info.breed,
                            0.0, // Temperature will be from last scan
                            "Unknown", // Status will be from last scan
                            "Scans: " + info.scanCount
                        ));
                    }
                    
                    // If no backend data, show sample data
                    if (animals.isEmpty()) {
                        // Try local storage
                        List<String> animalIds = ScanStorage.getAllAnimalIds(requireContext());
                        
                        for (String animalId : animalIds) {
                            List<ScanResult> scans = ScanStorage.getScansForAnimal(requireContext(), animalId);
                            if (!scans.isEmpty()) {
                                ScanResult last = scans.get(scans.size() - 1);
                                animals.add(new Animal(
                                    animalId,
                                    "Cattle",
                                    last.temperature,
                                    last.status,
                                    last.time
                                ));
                            }
                        }
                        
                        // If still empty, show sample data
                        if (animals.isEmpty()) {
                            animals.add(new Animal("COW001", "Dairy Cattle", 38.3, "Healthy", "Sample Data"));
                            animals.add(new Animal("COW002", "Dairy Cattle", 39.2, "Attention Needed", "Sample Data"));
                            animals.add(new Animal("COW003", "Dairy Cattle", 38.1, "Healthy", "Sample Data"));
                            animals.add(new Animal("COW004", "Dairy Cattle", 39.8, "Urgent", "Sample Data"));
                        }
                    }
                    
                    // Show list
                    emptyState.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    
                    AnimalAdapter adapter = new AnimalAdapter(animals, animal -> {
                        Fragment fragment = AnimalHistoryFragment.newInstance(animal.id, animal.type);
                        
                        requireActivity()
                            .getSupportFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(
                                android.R.anim.fade_in,
                                android.R.anim.fade_out,
                                android.R.anim.fade_in,
                                android.R.anim.fade_out
                            )
                            .replace(R.id.fragment_container, fragment)
                            .addToBackStack(null)
                            .commit();
                    });
                    
                    recyclerView.setAdapter(adapter);
                });
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() == null) return;
                
                getActivity().runOnUiThread(() -> {
                    // Fall back to local storage on error
                    List<Animal> animals = new ArrayList<>();
                    List<String> animalIds = ScanStorage.getAllAnimalIds(requireContext());
                    
                    for (String animalId : animalIds) {
                        List<ScanResult> scans = ScanStorage.getScansForAnimal(requireContext(), animalId);
                        if (!scans.isEmpty()) {
                            ScanResult last = scans.get(scans.size() - 1);
                            animals.add(new Animal(
                                animalId,
                                "Cattle",
                                last.temperature,
                                last.status,
                                last.time
                            ));
                        }
                    }
                    
                    // Show sample data if no local data
                    if (animals.isEmpty()) {
                        animals.add(new Animal("COW001", "Dairy Cattle", 38.3, "Healthy", "Sample Data"));
                        animals.add(new Animal("COW002", "Dairy Cattle", 39.2, "Attention Needed", "Sample Data"));
                        animals.add(new Animal("COW003", "Dairy Cattle", 38.1, "Healthy", "Sample Data"));
                        animals.add(new Animal("COW004", "Dairy Cattle", 39.8, "Urgent", "Sample Data"));
                    }
                    
                    emptyState.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    
                    AnimalAdapter adapter = new AnimalAdapter(animals, animal -> {
                        Fragment fragment = AnimalHistoryFragment.newInstance(animal.id, animal.type);
                        
                        requireActivity()
                            .getSupportFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(
                                android.R.anim.fade_in,
                                android.R.anim.fade_out,
                                android.R.anim.fade_in,
                                android.R.anim.fade_out
                            )
                            .replace(R.id.fragment_container, fragment)
                            .addToBackStack(null)
                            .commit();
                    });
                    
                    recyclerView.setAdapter(adapter);
                });
            }
        });
    }
}
