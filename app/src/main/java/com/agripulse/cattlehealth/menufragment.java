package com.agripulse.cattlehealth;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Professional menu screen with grid layout
 */
public class menufragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    public menufragment() {
        // Required empty public constructor
    }

    public static menufragment newInstance(String param1, String param2) {
        menufragment fragment = new menufragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_menu, container, false);
    }

    private void openFragment(Fragment fragment) {
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
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.cardScan).setOnClickListener(v ->
                openFragment(new ScanFragment()));

        view.findViewById(R.id.cardAnalytics).setOnClickListener(v ->
                openFragment(new AnalyticsFragment()));

        view.findViewById(R.id.cardHistory).setOnClickListener(v ->
                openFragment(new HistoryFragment()));

        view.findViewById(R.id.cardSettings).setOnClickListener(v ->
                openFragment(new SettingsFragment()));

        view.findViewById(R.id.buttonExit).setOnClickListener(v ->
                requireActivity().finish());

        // Back button handling
        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        requireActivity()
                                .getSupportFragmentManager()
                                .popBackStack();
                    }
                }
        );
    }
}