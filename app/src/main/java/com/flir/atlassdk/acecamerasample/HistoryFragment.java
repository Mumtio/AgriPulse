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


        List<Animal> animals = new ArrayList<>();

        List<String> animalIds =
                ScanStorage.getAllAnimalIds(requireContext());

        for (String animalId : animalIds) {

            List<ScanResult> scans =
                    ScanStorage.getScansForAnimal(requireContext(), animalId);

            if (scans.isEmpty()) continue;

            // LAST scan = most recent
            ScanResult last = scans.get(scans.size() - 1);

            animals.add(new Animal(
                    animalId,
                    "Cattle",                 // for now (can improve later)
                    last.temperature,
                    last.status,
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
