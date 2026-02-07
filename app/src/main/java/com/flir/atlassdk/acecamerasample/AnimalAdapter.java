package com.flir.atlassdk.acecamerasample;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.flir.atlassdk.acecamerasample.Animal;
import com.flir.atlassdk.acecamerasample.R;

import java.util.List;

public class AnimalAdapter extends RecyclerView.Adapter<AnimalAdapter.ViewHolder> {

    private final List<Animal> animals;
    private final OnAnimalClickListener listener;


    public interface OnAnimalClickListener {
        void onAnimalClick(Animal animal);
    }


    public AnimalAdapter(List<Animal> animals,OnAnimalClickListener listener) {
        this.animals = animals;
        this.listener = listener;
    }

    private void applyStatusStyle(View indicator, TextView statusText, String status) {
        switch (status) {
            case "High":
                indicator.setBackgroundResource(R.drawable.status_dot_red);
                statusText.setTextColor(0xFFD32F2F);
                break;

            case "Elevated":
                indicator.setBackgroundResource(R.drawable.status_dot_amber);
                statusText.setTextColor(0xFFF9A825);
                break;

            default:
                indicator.setBackgroundResource(R.drawable.status_dot_green);
                statusText.setTextColor(0xFF2E7D32);
                break;
        }
    }



    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_animal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Animal animal = animals.get(position);

        holder.animalId.setText("Animal ID: " + animal.id);
        holder.animalType.setText(animal.type);
        holder.lastScan.setText("Last scan: " + animal.lastScanTime);
        holder.temp.setText(String.format("%.1f °C", animal.lastTemp));
        holder.status.setText(animal.status);
        applyStatusStyle(holder.statusIndicator, holder.status, animal.status);
        holder.itemView.setOnClickListener(v -> listener.onAnimalClick(animal));

    }

    @Override
    public int getItemCount() {
        return animals.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView animalId, animalType, lastScan, temp, status;
        View statusIndicator;
        ViewHolder(View itemView) {
            super(itemView);

            animalId = itemView.findViewById(R.id.animalIdText);
            statusIndicator = itemView.findViewById(R.id.statusIndicator);

            animalType = itemView.findViewById(R.id.animalTypeText);
            lastScan = itemView.findViewById(R.id.lastScanText);
            temp = itemView.findViewById(R.id.tempText);
            status = itemView.findViewById(R.id.statusText);
        }
    }



}
