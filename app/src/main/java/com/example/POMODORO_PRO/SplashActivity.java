package com.example.POMODORO_PRO;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION = 2500; // 2.5 seconds
    private CardView logoContainer;
    private ImageView appLogo;
    private TextView appName;
    private View loadingProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize views
        logoContainer = findViewById(R.id.logoContainer);
        appLogo = findViewById(R.id.appLogo);
        appName = findViewById(R.id.appName);
        loadingProgress = findViewById(R.id.loadingProgress);

        // Set initial states for animations
        logoContainer.setScaleX(0.1f);
        logoContainer.setScaleY(0.1f);
        logoContainer.setAlpha(0f);
        
        appLogo.setScaleX(0.6f);
        appLogo.setScaleY(0.6f);
        appLogo.setAlpha(0f);
        
        appName.setAlpha(0f);
        appName.setTranslationY(50f);
        
        loadingProgress.setAlpha(0f);
        loadingProgress.setScaleX(0.5f);

        // Start animations after a short delay
        new Handler(Looper.getMainLooper()).postDelayed(this::startAnimations, 300);

        // Navigate to main activity after splash duration
        new Handler(Looper.getMainLooper()).postDelayed(this::navigateToMainActivity, SPLASH_DURATION);
    }

    private void startAnimations() {
        // Container animation
        ObjectAnimator containerScaleX = ObjectAnimator.ofFloat(logoContainer, "scaleX", 0.1f, 1f);
        ObjectAnimator containerScaleY = ObjectAnimator.ofFloat(logoContainer, "scaleY", 0.1f, 1f);
        ObjectAnimator containerAlpha = ObjectAnimator.ofFloat(logoContainer, "alpha", 0f, 1f);
        
        AnimatorSet containerAnimSet = new AnimatorSet();
        containerAnimSet.playTogether(containerScaleX, containerScaleY, containerAlpha);
        containerAnimSet.setDuration(700);
        containerAnimSet.setInterpolator(new OvershootInterpolator(0.7f));
        
        // Logo animation
        ObjectAnimator logoScaleX = ObjectAnimator.ofFloat(appLogo, "scaleX", 0.6f, 1f);
        ObjectAnimator logoScaleY = ObjectAnimator.ofFloat(appLogo, "scaleY", 0.6f, 1f);
        ObjectAnimator logoAlpha = ObjectAnimator.ofFloat(appLogo, "alpha", 0f, 1f);
        
        AnimatorSet logoAnimSet = new AnimatorSet();
        logoAnimSet.playTogether(logoScaleX, logoScaleY, logoAlpha);
        logoAnimSet.setDuration(600);
        logoAnimSet.setStartDelay(300);
        logoAnimSet.setInterpolator(new AnticipateOvershootInterpolator(1.5f));
        
        // Start the minute hand animation in the logo
        logoAnimSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {}

            @Override
            public void onAnimationEnd(Animator animation) {
                // Start the vector animation when the scale/alpha animation completes
                Drawable drawable = appLogo.getDrawable();
                if (drawable instanceof Animatable) {
                    ((Animatable) drawable).start();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {}

            @Override
            public void onAnimationRepeat(Animator animation) {}
        });
        
        // App name animation
        ObjectAnimator nameAlpha = ObjectAnimator.ofFloat(appName, "alpha", 0f, 1f);
        ObjectAnimator nameTranslationY = ObjectAnimator.ofFloat(appName, "translationY", 50f, 0f);
        
        AnimatorSet nameAnimSet = new AnimatorSet();
        nameAnimSet.playTogether(nameAlpha, nameTranslationY);
        nameAnimSet.setDuration(600);
        nameAnimSet.setStartDelay(700);
        nameAnimSet.setInterpolator(new AccelerateDecelerateInterpolator());
        
        // Progress bar animation
        ObjectAnimator progressAlpha = ObjectAnimator.ofFloat(loadingProgress, "alpha", 0f, 1f);
        ObjectAnimator progressScaleX = ObjectAnimator.ofFloat(loadingProgress, "scaleX", 0.5f, 1f);
        
        AnimatorSet progressAnimSet = new AnimatorSet();
        progressAnimSet.playTogether(progressAlpha, progressScaleX);
        progressAnimSet.setDuration(400);
        progressAnimSet.setStartDelay(1000);
        
        // Play all animations together
        AnimatorSet allAnimSet = new AnimatorSet();
        allAnimSet.playTogether(containerAnimSet, logoAnimSet, nameAnimSet, progressAnimSet);
        allAnimSet.start();
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }
} 