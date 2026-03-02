package com.agripulse.cattlehealth;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

/**
 * Professional landing screen with smooth animations
 */
public class landingfragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    public landingfragment() {
        // Required empty public constructor
    }

    public static landingfragment newInstance(String param1, String param2) {
        landingfragment fragment = new landingfragment();
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
        return inflater.inflate(R.layout.fragment_landing, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get views
        View brandingSection = view.findViewById(R.id.brandingSection);
        View featureSection = view.findViewById(R.id.featureSection);
        View ctaSection = view.findViewById(R.id.ctaSection);
        View buttonStart = view.findViewById(R.id.buttonStart);

        // Set initial states for animation
        brandingSection.setAlpha(0f);
        brandingSection.setTranslationY(-50f);
        
        featureSection.setAlpha(0f);
        featureSection.setTranslationX(-30f);
        
        ctaSection.setAlpha(0f);
        ctaSection.setTranslationY(50f);

        // Animate branding section (logo + title)
        ObjectAnimator brandingAlpha = ObjectAnimator.ofFloat(brandingSection, "alpha", 0f, 1f);
        ObjectAnimator brandingTranslate = ObjectAnimator.ofFloat(brandingSection, "translationY", -50f, 0f);
        brandingAlpha.setDuration(800);
        brandingTranslate.setDuration(800);
        brandingAlpha.setInterpolator(new DecelerateInterpolator());
        brandingTranslate.setInterpolator(new DecelerateInterpolator());

        AnimatorSet brandingSet = new AnimatorSet();
        brandingSet.playTogether(brandingAlpha, brandingTranslate);
        brandingSet.setStartDelay(200);
        brandingSet.start();

        // Animate feature section
        ObjectAnimator featureAlpha = ObjectAnimator.ofFloat(featureSection, "alpha", 0f, 1f);
        ObjectAnimator featureTranslate = ObjectAnimator.ofFloat(featureSection, "translationX", -30f, 0f);
        featureAlpha.setDuration(600);
        featureTranslate.setDuration(600);
        featureAlpha.setInterpolator(new DecelerateInterpolator());
        featureTranslate.setInterpolator(new DecelerateInterpolator());

        AnimatorSet featureSet = new AnimatorSet();
        featureSet.playTogether(featureAlpha, featureTranslate);
        featureSet.setStartDelay(600);
        featureSet.start();

        // Animate CTA section (button)
        ObjectAnimator ctaAlpha = ObjectAnimator.ofFloat(ctaSection, "alpha", 0f, 1f);
        ObjectAnimator ctaTranslate = ObjectAnimator.ofFloat(ctaSection, "translationY", 50f, 0f);
        ctaAlpha.setDuration(600);
        ctaTranslate.setDuration(600);
        ctaAlpha.setInterpolator(new DecelerateInterpolator());
        ctaTranslate.setInterpolator(new DecelerateInterpolator());

        AnimatorSet ctaSet = new AnimatorSet();
        ctaSet.playTogether(ctaAlpha, ctaTranslate);
        ctaSet.setStartDelay(1000);
        ctaSet.start();

        // Button click with scale animation
        buttonStart.setOnClickListener(v -> {
            // Scale animation on click
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(v, "scaleX", 1f, 0.95f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(v, "scaleY", 1f, 0.95f, 1f);
            scaleX.setDuration(150);
            scaleY.setDuration(150);
            scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
            scaleY.setInterpolator(new AccelerateDecelerateInterpolator());

            AnimatorSet clickSet = new AnimatorSet();
            clickSet.playTogether(scaleX, scaleY);
            clickSet.start();

            // Navigate after animation
            v.postDelayed(() -> {
                requireActivity()
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(
                                android.R.anim.fade_in,
                                android.R.anim.fade_out
                        )
                        .replace(R.id.fragment_container, new menufragment())
                        .addToBackStack(null)
                        .commit();
            }, 200);
        });
    }
}