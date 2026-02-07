package com.flir.atlassdk.acecamerasample;


import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.flir.atlassdk.acecamerasample.R;

public class ScanDetailFragment extends Fragment {

    private static final String ARG_TEMP = "temp";


    private static final String ARG_STATUS = "status";
    private static final String ARG_TIME = "time";

    public static ScanDetailFragment newInstance(double temp, String status, String time) {
        ScanDetailFragment fragment = new ScanDetailFragment();
        Bundle args = new Bundle();
        args.putDouble(ARG_TEMP, temp);
        args.putString(ARG_STATUS, status);
        args.putString(ARG_TIME, time);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_scan_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton backButton = view.findViewById(R.id.backButton);
        TextView tempText = view.findViewById(R.id.tempText);
        TextView statusText = view.findViewById(R.id.statusText);
        TextView timeText = view.findViewById(R.id.timeText);

        double temp = getArguments().getDouble(ARG_TEMP);
        String status = getArguments().getString(ARG_STATUS);
        String time = getArguments().getString(ARG_TIME);

        tempText.setText(String.format("%.1f °C", temp));
        statusText.setText(status);
        timeText.setText(time);
        applyStatusStyle(tempText, statusText, status);


        backButton.setOnClickListener(v ->
                requireActivity()
                        .getSupportFragmentManager()
                        .popBackStack()
        );

        TextView guidanceText = view.findViewById(R.id.guidanceText);
        guidanceText.setText(getGuidanceForStatus(status));



    }


    private void applyStatusStyle(TextView tempText, TextView statusText, String status) {
        switch (status) {
            case "High":
                tempText.setTextColor(0xFFD32F2F);
                statusText.setTextColor(0xFFD32F2F);
                break;

            case "Elevated":
                tempText.setTextColor(0xFFF9A825);
                statusText.setTextColor(0xFFF9A825);
                break;

            default:
                tempText.setTextColor(0xFF2E7D32);
                statusText.setTextColor(0xFF2E7D32);
                break;
        }
    }

    private String getGuidanceForStatus(String status) {
        switch (status) {
            case "High":
                return "This reading is notably above normal.\n\n"
                        + "It is recommended to closely observe the animal and consider a physical check or veterinary consultation.";

            case "Elevated":
                return "This reading is slightly above normal.\n\n"
                        + "Repeat scanning over the next few hours is advised to track any changes.";

            default:
                return "This reading falls within the normal range.\n\n"
                        + "No immediate action is required at this time.";
        }
    }


}
