package com.flir.atlassdk.acecamerasample;

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

        List<Animal> animals = new ArrayList<>();
        List<String> animalIds = ScanStorage.getAllAnimalIds(requireContext());

        for (String animalId : animalIds) {
            List<ScanResult> scans = ScanStorage.getScansForAnimal(requireContext(), animalId);

            if (scans.isEmpty()) continue;

            // LAST scan = most recent
            ScanResult last = scans.get(scans.size() - 1);

            animals.add(new Animal(
                    animalId,
                    "Cattle",
                    last.temperature,
                    last.status,
                    last.time
            ));
        }

        // Add sample data for demonstration if no real data exists
        if (animals.isEmpty()) {
            animals.add(new Animal("DC-014", "Dairy Cattle", 39.7, "High", "Today · 6:45 AM"));
            animals.add(new Animal("DC-023", "Dairy Cattle", 39.2, "Elevated", "Today · 7:12 AM"));
            animals.add(new Animal("DC-008", "Dairy Cattle", 38.3, "Normal", "Today · 8:30 AM"));
            animals.add(new Animal("DC-041", "Dairy Cattle", 38.1, "Normal", "Yesterday · 5:20 PM"));
            animals.add(new Animal("DC-056", "Dairy Cattle", 39.8, "High", "Yesterday · 6:15 PM"));
        }

        // Always show the list since we have sample data
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
    }
}
