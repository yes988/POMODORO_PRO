package com.example.POMODORO_PRO.ui.home;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.POMODORO_PRO.MainActivity;
import com.example.POMODORO_PRO.R;
import com.example.POMODORO_PRO.SessionDatabaseHelper;
import com.example.POMODORO_PRO.ui.NotificationHelper;
import com.example.POMODORO_PRO.ui.progress.ProgressAdapter;
import com.example.POMODORO_PRO.ui.progress.ProgressItem;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Pomotime extends AppCompatActivity {
    private TimerViewModel viewModel;
    // UI Components
    private TextView textCountdown, textSessionType, textTaskSelected, progressLabel, progressPercentText;
    private MaterialButton btnPauseResume, btnStop;
    private ImageButton imgBtn_music, btnSwitchMusic;
    private ProgressBar progressCount;
    private ImageView gifImageView;
    private CardView timerCard;

    // Timer Variables
    private CountDownTimer countDownTimer;
    private boolean isRunning = false;
    private boolean isPomodoro = true;
    private long timeLeftInMillis;
    private long totalElapsedTime = 0;
    private int totalStudyTime;
    private int pomodoroTime, breakTime;

    // Developer mode variables
    private boolean isDevMode = false;
    private long timeMultiplier = 1000; // Normal speed (1 real second = 1 app second)
    private static final long DEV_MODE_MULTIPLIER = 10; // 10x speed for testing

    // Add these constants at the top of your Pomotime class
    private static final int[] STUDY_SOUNDS = {
            R.raw.lofi,      // Focused study music
            R.raw.thunder    // Background white noise
    };

    // Make sure you have different sounds for breaks
    private static final int BREAK_SOUND = R.raw.tea_time; // Single int

    private SoundPlayer soundPlayer;

    // Database
    private SessionDatabaseHelper dbHelper;
    private ProgressAdapter progressAdapter;

    private boolean isMuted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pomodoro_sess);

        viewModel = new ViewModelProvider(this).get(TimerViewModel.class);

        try {
            // Initialize with saved values or defaults
            if (viewModel.timeLeftInMillis == 0) {
                viewModel.timeLeftInMillis = pomodoroTime;
            }

            if (savedInstanceState != null) {
                timeLeftInMillis = savedInstanceState.getLong("timeLeft", pomodoroTime);
                totalElapsedTime = savedInstanceState.getLong("elapsedTime", 0);
                isRunning = savedInstanceState.getBoolean("isRunning", false);
                isPomodoro = savedInstanceState.getBoolean("isPomodoro", true);
            } else {
                timeLeftInMillis = pomodoroTime;
            }

            // Initialize views and components first
            initViews();
            setupDatabase();
            loadSettings();
            setupTimer();

            // Initialize sound player AFTER all other setup
            soundPlayer = new SoundPlayer(this, STUDY_SOUNDS, BREAK_SOUND);

            // Setup click listeners AFTER sound player is ready
            setupClickListeners();
            setupMusicSwitcher();
            setupMusicControls();
            updateMusicButtonIcon();
            
            // Apply entry animations
            animateUIElements();

            // Check for auto-start LAST
            boolean startImmediately = getIntent().getBooleanExtra("START_IMMEDIATELY", false);
            if (startImmediately) {
                new Handler().postDelayed(() -> {
                    try {
                        startTimer();
                        // Add delay to ensure sound player is ready
                        if (soundPlayer != null) {
                            soundPlayer.playCurrentStudySound();
                            updateMusicButtonIcon();
                        }
                    } catch (Exception e) {
                        Log.e("Pomotime", "Error auto-starting timer: " + e.getMessage());
                        Toast.makeText(this, "Error starting timer, please try again", Toast.LENGTH_SHORT).show();
                    }
                }, 1000); // Delay startup to allow animations to complete
            }
        } catch (Exception e) {
            Log.e("Pomotime", "Error in onCreate: " + e.getMessage());
            Toast.makeText(this, "An error occurred during setup. Please restart the app.", Toast.LENGTH_LONG).show();
        }
    }
    
    private void animateUIElements() {
        try {
            // Animate title with fade in and slide down
            Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
            if (textSessionType != null) textSessionType.startAnimation(fadeIn);
            if (textTaskSelected != null) textTaskSelected.startAnimation(fadeIn);
            
            // Animate timer card with scale in
            if (timerCard != null) {
                timerCard.setScaleX(0.8f);
                timerCard.setScaleY(0.8f);
                timerCard.setAlpha(0f);
                timerCard.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setDuration(800)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            }
            
            // Apply pulse animation to timer
            if (gifImageView != null) {
                Animation pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse);
                gifImageView.startAnimation(pulseAnimation);
            }
            
            // Animate progress section with slide up
            Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
            if (progressLabel != null) progressLabel.startAnimation(slideUp);
            if (progressCount != null) progressCount.startAnimation(slideUp);
            
            // Animate buttons with bounce effect
            new Handler().postDelayed(() -> {
                try {
                    Animation buttonBounce = AnimationUtils.loadAnimation(this, R.anim.button_bounce);
                    if (btnPauseResume != null) btnPauseResume.startAnimation(buttonBounce);
                    if (btnStop != null) btnStop.startAnimation(buttonBounce);
                    if (btnSwitchMusic != null) btnSwitchMusic.startAnimation(buttonBounce);
                    if (imgBtn_music != null) imgBtn_music.startAnimation(buttonBounce);
                } catch (Exception e) {
                    Log.e("Pomotime", "Error animating buttons: " + e.getMessage());
                }
            }, 500);
        } catch (Exception e) {
            Log.e("Pomotime", "Error in animateUIElements: " + e.getMessage());
            // Continue execution even if animations fail
        }
    }
    
    private void initViews() {
        progressCount = findViewById(R.id.progressCount);
        textCountdown = findViewById(R.id.textCount);
        textSessionType = findViewById(R.id.textPomodoroTitle);
        textTaskSelected = findViewById(R.id.txtTask_Selected);
        btnPauseResume = findViewById(R.id.btnPauseResume);
        btnStop = findViewById(R.id.btnStop);
        gifImageView = findViewById(R.id.gifImageView);
        imgBtn_music = findViewById(R.id.imgBtn_music);
        btnSwitchMusic = findViewById(R.id.btnSwitchMusic);
        timerCard = findViewById(R.id.timerCard);
        progressLabel = findViewById(R.id.progressLabel);
        progressCount.setMax(100);

        Glide.with(this).load(R.drawable.tenor).into(gifImageView);
    }
    private void toggleDevMode() {
        isDevMode = !isDevMode;
        timeMultiplier = isDevMode ? DEV_MODE_MULTIPLIER : 1000;

        String message = "Developer mode " + (isDevMode ? "enabled (10x speed)" : "disabled");
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        // If timer is running, restart it with new speed
        if (isRunning) {
            pauseTimer();
            startTimer();
        }
    }

    private void setupDatabase() {
        dbHelper = new SessionDatabaseHelper(this);
        progressAdapter = new ProgressAdapter(new ArrayList<>());
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences("PomodoroPrefs", MODE_PRIVATE);

        // Load times from preferences
        pomodoroTime = prefs.getInt("pomodoroTime", 25) * 60 * 1000;
        breakTime = prefs.getInt("shortBreak", 5) * 60 * 1000;
        totalStudyTime = prefs.getInt("studyTime", 120) * 60 * 1000;

        String taskName = getIntent().getStringExtra("SELECTED_TASK");
        textSessionType.setText(taskName != null ? taskName : "Pomodoro Session");
        textTaskSelected.setText(textSessionType.getText().toString());
    }

    private void setupTimer() {
        timeLeftInMillis = pomodoroTime;
        updateDisplay();
        // Set the button text based on whether timer is running
        btnPauseResume.setText(isRunning ? "Pause" : "Start");
    }

    private void setupClickListeners() {
        btnPauseResume.setOnClickListener(v -> toggleTimer());
        btnStop.setOnClickListener(v -> showEndConfirmationDialog());
    }

    private void toggleTimer() {
        if (isRunning) {
            pauseTimer();
            
            // Add button animation for pause
            Animation buttonBounce = AnimationUtils.loadAnimation(this, R.anim.button_bounce);
            btnPauseResume.startAnimation(buttonBounce);
        } else {
            // Add button animation for start
            Animation buttonBounce = AnimationUtils.loadAnimation(this, R.anim.button_bounce);
            btnPauseResume.startAnimation(buttonBounce);
            
            startTimer();
        }
    }

    // Modified startTimer() to include music auto-start
    private void startTimer() {
        countDownTimer = new CountDownTimer(timeLeftInMillis, timeMultiplier) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                totalElapsedTime += timeMultiplier;
                updateDisplay();
            }

            @Override
            public void onFinish() {
                switchSession();
            }
        }.start();

        isRunning = true;
        btnPauseResume.setText("Pause");
        btnPauseResume.setIcon(ContextCompat.getDrawable(this, android.R.drawable.ic_media_pause));
        NotificationHelper.sendNotification(this, "Timer started!");

        // Auto-start music when timer starts
        handleAutoStartMusic();
        
        // Add pulse animation to timer
        Animation currentAnimation = gifImageView.getAnimation();
        if (currentAnimation == null || !currentAnimation.hasStarted() || currentAnimation.hasEnded()) {
            Animation pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse);
            gifImageView.startAnimation(pulseAnimation);
        }
    }

    private void pauseTimer() {
        try {
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
            isRunning = false;
            if (btnPauseResume != null) {
                btnPauseResume.setText("Resume");
                btnPauseResume.setIcon(ContextCompat.getDrawable(this, android.R.drawable.ic_media_play));
            }
            NotificationHelper.sendNotification(this, "Timer paused");
            
            // Stop pulse animation
            if (gifImageView != null) {
                gifImageView.clearAnimation();
            }
        } catch (Exception e) {
            Log.e("Pomotime", "Error in pauseTimer: " + e.getMessage());
        }
    }

    private void setupMusicSwitcher() {
        btnSwitchMusic.setOnClickListener(v -> {
            if (isPomodoro) {
                soundPlayer.switchToNextSound();
                Toast.makeText(this,
                        "Now playing: " + soundPlayer.getCurrentSoundName(),
                        Toast.LENGTH_SHORT).show();
                updateMusicButtonIcon();
            } else {
                Toast.makeText(this,
                        "Can only switch music during study sessions",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void updateDisplay() {
        updateSessionTimer();
        updateTotalTimer();
        updateProgressBar();
    }

    private void updateSessionTimer() {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeftInMillis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeLeftInMillis) % 60;
        String prefix = isPomodoro ? "Focus: " : "Break: ";
        textSessionType.setText(String.format(Locale.getDefault(),
                "%s%02d:%02d", prefix, minutes, seconds));
    }

    private void updateTotalTimer() {
        long hours = TimeUnit.MILLISECONDS.toHours(totalElapsedTime);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(totalElapsedTime) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(totalElapsedTime) % 60;
        textCountdown.setText(String.format(Locale.getDefault(),
                "%02d:%02d:%02d", hours, minutes, seconds));
    }

    private void updateProgressBar() {
        int progress = (int) ((totalElapsedTime * 100f) / totalStudyTime);
        
        // Animate progress change
        ValueAnimator animator = ValueAnimator.ofInt(progressCount.getProgress(), Math.min(progress, 100));
        animator.setDuration(300);
        animator.addUpdateListener(animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            progressCount.setProgress(animatedValue);
            
            // Update percentage text if it exists
            if (findViewById(R.id.progressPercentText) != null) {
                progressPercentText = findViewById(R.id.progressPercentText);
                progressPercentText.setText(animatedValue + "% Completed");
            }
        });
        animator.start();

        if (progress >= 100) {
            completeStudySession();
        }
    }

    // Updated switchSession method
    private void switchSession() {
        try {
            if (soundPlayer != null) {
                soundPlayer.stopSound();
            }

            // Handle session type change first
            isPomodoro = !isPomodoro;
            if (soundPlayer != null) {
                soundPlayer.setStudyMode(isPomodoro);
            }

            // Check if timerCard is available
            if (timerCard != null) {
                // Create fade out animation for smooth transition
                ObjectAnimator fadeOut = ObjectAnimator.ofFloat(timerCard, "alpha", 1f, 0.3f);
                fadeOut.setDuration(400);
                
                // Create fade in animation
                ObjectAnimator fadeIn = ObjectAnimator.ofFloat(timerCard, "alpha", 0.3f, 1f);
                fadeIn.setDuration(400);
                
                // Create scale down/up instead of rotation to avoid mirroring
                ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(timerCard, "scaleX", 1f, 0.85f);
                ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(timerCard, "scaleY", 1f, 0.85f);
                ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(timerCard, "scaleX", 0.85f, 1f);
                ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(timerCard, "scaleY", 0.85f, 1f);
                
                // Add subtle translation up/down for more dynamic effect
                ObjectAnimator moveDown = ObjectAnimator.ofFloat(timerCard, "translationY", 0f, 30f);
                ObjectAnimator moveUp = ObjectAnimator.ofFloat(timerCard, "translationY", 30f, 0f);
                
                scaleDownX.setDuration(400);
                scaleDownY.setDuration(400);
                scaleUpX.setDuration(400);
                scaleUpY.setDuration(400);
                moveDown.setDuration(400);
                moveUp.setDuration(400);
                
                // Set interpolators for smoother animation
                scaleDownX.setInterpolator(new DecelerateInterpolator());
                scaleDownY.setInterpolator(new DecelerateInterpolator());
                scaleUpX.setInterpolator(new AccelerateDecelerateInterpolator());
                scaleUpY.setInterpolator(new AccelerateDecelerateInterpolator());
                
                // Play animations in sequence
                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.play(fadeOut).with(scaleDownX).with(scaleDownY).with(moveDown);
                animatorSet.play(fadeIn).with(scaleUpX).with(scaleUpY).with(moveUp).after(fadeOut);
                
                // Listen for animation completion
                animatorSet.start();
            }
            
            // Prepare new GIF before showing
            final int newGifResource = isPomodoro ? R.drawable.tenor : R.drawable.teabreak;
            
            // Update UI on animation mid-point
            new Handler().postDelayed(() -> {
                try {
                    // Update UI immediately
                    if (isPomodoro) {
                        timeLeftInMillis = pomodoroTime;
                    } else {
                        timeLeftInMillis = breakTime;
                    }
                    
                    // Smoothly transition the GIF
                    if (gifImageView != null) {
                        // Crossfade the GIF change
                        Glide.with(this)
                            .load(newGifResource)
                            .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade(300))
                            .into(gifImageView);
                    }

                    // Start timer after UI update
                    startTimer();

                    // Handle sound with slight delay
                    if (isPomodoro) {
                        handleAutoStartMusic();
                    } else {
                        handleAutoStartBreakMusic();
                    }
                    playSessionSwitchNotificationSound();
                    updateMusicButtonIcon();
                    
                    // Update timer text color based on mode
                    int textColor = ContextCompat.getColor(this, 
                            isPomodoro ? R.color.primary_color : R.color.break_color);
                    if (textCountdown != null) {
                        textCountdown.setTextColor(textColor);
                    }
                } catch (Exception e) {
                    Log.e("Pomotime", "Error updating session UI: " + e.getMessage());
                }
            }, 350); // Mid-point of animation
        } catch (Exception e) {
            Log.e("Pomotime", "Error in switchSession: " + e.getMessage());
            // Fallback to basic session switch if animation fails
            isPomodoro = !isPomodoro;
            timeLeftInMillis = isPomodoro ? pomodoroTime : breakTime;
            startTimer();
        }
    }

    // Add this method to handle break music auto-start
    // Fixed handleAutoStartBreakMusic method
    private void handleAutoStartBreakMusic() {
        SharedPreferences prefs = getSharedPreferences("PomodoroPrefs", MODE_PRIVATE);
        boolean autoStartMusic = prefs.getBoolean("autoStartMusic", true);

        Log.d("MusicAutoStart", "handleAutoStartBreakMusic called - autoStart: " + autoStartMusic +
                ", isPomodoro: " + isPomodoro + ", soundPlayer study mode: " + soundPlayer.getCurrentSessionType());

        if (autoStartMusic && soundPlayer != null && !isPomodoro) { // Extra check: !isPomodoro
            try {
                // Add small delay to ensure setStudyMode has taken effect
                new Handler().postDelayed(() -> {
                    Log.d("MusicAutoStart", "Actually starting break music");
                    soundPlayer.playBreakSound();
                    updateMusicButtonIcon();

                    // Show toast message
                    Toast.makeText(this,
                            "Now playing: " + soundPlayer.getCurrentSoundName(),
                            Toast.LENGTH_SHORT).show();
                }, 50);

            } catch (Exception e) {
                Log.e("SoundError", "Failed to auto-start break music", e);
            }
        }
    }

    // Fixed handleAutoStartMusic method for study sessions
    private void handleAutoStartMusic() {
        SharedPreferences prefs = getSharedPreferences("PomodoroPrefs", MODE_PRIVATE);
        boolean autoStartMusic = prefs.getBoolean("autoStartMusic", true);

        Log.d("MusicAutoStart", "handleAutoStartMusic called - autoStart: " + autoStartMusic +
                ", isPomodoro: " + isPomodoro + ", soundPlayer study mode: " + soundPlayer.getCurrentSessionType());

        if (autoStartMusic && soundPlayer != null && isPomodoro) { // Extra check: isPomodoro
            try {
                // Add small delay to ensure setStudyMode has taken effect
                new Handler().postDelayed(() -> {
                    Log.d("MusicAutoStart", "Actually starting study music");
                    soundPlayer.playCurrentStudySound();
                    updateMusicButtonIcon();

                    // Show toast message
                    Toast.makeText(this,
                            "Now playing: " + soundPlayer.getCurrentSoundName(),
                            Toast.LENGTH_SHORT).show();
                }, 50);

            } catch (Exception e) {
                Log.e("SoundError", "Failed to auto-start study music", e);
            }
        }
    }

    private void completeStudySession() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isRunning = false;

        textSessionType.setText("Study Completed!");
        textCountdown.setText("00:00:00");
        
        // Animate button visibility change
        btnPauseResume.animate()
            .alpha(0f)
            .scaleX(0f)
            .scaleY(0f)
            .setDuration(500)
            .withEndAction(() -> btnPauseResume.setVisibility(View.GONE))
            .start();

        // Change the Stop button to Complete with animation
        btnStop.setText("Complete");
        btnStop.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.green_complete));
        btnStop.setIcon(ContextCompat.getDrawable(this, android.R.drawable.ic_menu_set_as));
        
        // Apply celebration animation
        Animation bounceAnimation = AnimationUtils.loadAnimation(this, R.anim.button_bounce);
        btnStop.startAnimation(bounceAnimation);
        
        btnStop.setOnClickListener(v -> navigateToMainActivity());

        playSuccessSound();
        NotificationHelper.sendNotification(this, "Congrats! Study session completed!", true);
        recordSessionData();
        
        // Animate completion
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(timerCard, "scaleX", 1f, 1.1f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(timerCard, "scaleY", 1f, 1.1f, 1f);
        
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY);
        animatorSet.setDuration(1000);
        animatorSet.start();
    }

    private void showEndConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("End Session")
                .setMessage("Are you sure you want to end this session?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    playFailureSound();
                    if (totalElapsedTime > 0) {
                        recordSessionData();
                    }
                    NotificationHelper.sendNotification(this, "Session stopped early", false);
                    navigateToMainActivity();
                })
                .setNegativeButton("No", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
    private void playSuccessSound() {
        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.success);
        mediaPlayer.setOnCompletionListener(mp -> {
            mp.release();
        });
        mediaPlayer.start();
    }

    private void playFailureSound() {
        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.failure);
        mediaPlayer.setOnCompletionListener(mp -> {
            mp.release();
        });
        mediaPlayer.start();
    }
    private void resetTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        timeLeftInMillis = pomodoroTime;
        totalElapsedTime = 0;
        isPomodoro = true;
        isRunning = false;
        
        btnPauseResume.setText("Start");
        btnPauseResume.setIcon(ContextCompat.getDrawable(this, android.R.drawable.ic_media_play));
        
        // Restore button with animation if hidden
        if (btnPauseResume.getVisibility() != View.VISIBLE) {
            btnPauseResume.setVisibility(View.VISIBLE);
            btnPauseResume.setAlpha(0f);
            btnPauseResume.setScaleX(0f);
            btnPauseResume.setScaleY(0f);
            
            btnPauseResume.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .start();
        }

        // Reset the Stop button with animation
        Animation bounceAnimation = AnimationUtils.loadAnimation(this, R.anim.button_bounce);
        btnStop.setText("Stop");
        btnStop.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.red));
        btnStop.setIcon(ContextCompat.getDrawable(this, android.R.drawable.ic_menu_close_clear_cancel));
        btnStop.startAnimation(bounceAnimation);
        btnStop.setOnClickListener(v -> showEndConfirmationDialog());

        updateDisplay();
    }

    private void recordSessionData() {
        String date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        String taskName = getIntent().getStringExtra("SELECTED_TASK");

        Log.d("SessionDebug", "Recording - Task: " + taskName +
                " Total ms: " + totalElapsedTime +
                " Converted sec: " + (totalElapsedTime/1000));

        if (taskName != null && totalElapsedTime > 0) {
            // Convert milliseconds to seconds for storage
            int accumulatedTime = (int) (totalElapsedTime / 1000);

            // Create ProgressItem and add to database
            ProgressItem sessionItem = new ProgressItem(date, taskName, accumulatedTime);
            dbHelper.addSession(sessionItem);

            refreshRecyclerView();
        }

    }

    private void refreshRecyclerView() {
        if (progressAdapter != null) {
            List<ProgressItem> updatedList = dbHelper.getAllSessions();
            progressAdapter.updateData(updatedList);
        }
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        try {
            super.onSaveInstanceState(outState);
            outState.putLong("timeLeft", timeLeftInMillis);
            outState.putLong("elapsedTime", totalElapsedTime);
            outState.putBoolean("isRunning", isRunning);
            outState.putBoolean("isPomodoro", isPomodoro);
            
            // Save to ViewModel as well for redundancy
            if (viewModel != null) {
                viewModel.timeLeftInMillis = timeLeftInMillis;
                viewModel.totalElapsedTime = totalElapsedTime;
                viewModel.isRunning = isRunning;
                viewModel.isPomodoro = isPomodoro;
                viewModel.isInitialized = true;
            }
        } catch (Exception e) {
            Log.e("Pomotime", "Error saving instance state: " + e.getMessage());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            // Save current timestamp to calculate elapsed time
            if (viewModel != null) {
                viewModel.lastUpdateTimestamp = System.currentTimeMillis();
            }
            
            // Save current state if we're running
            if (isRunning && countDownTimer != null) {
                countDownTimer.cancel();
            }
        } catch (Exception e) {
            Log.e("Pomotime", "Error in onPause: " + e.getMessage());
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        try {
            // Check if we need to restore a running timer
            if (viewModel != null && viewModel.isInitialized && viewModel.isRunning) {
                // Calculate elapsed time while paused
                long elapsedWhilePaused = 0;
                if (viewModel.lastUpdateTimestamp > 0) {
                    elapsedWhilePaused = System.currentTimeMillis() - viewModel.lastUpdateTimestamp;
                }
                
                // Adjust remaining time
                if (elapsedWhilePaused > 0 && elapsedWhilePaused < timeLeftInMillis) {
                    timeLeftInMillis -= elapsedWhilePaused;
                }
                
                // Restart timer if it was running
                if (isRunning) {
                    startTimer();
                }
            }
        } catch (Exception e) {
            Log.e("Pomotime", "Error in onResume: " + e.getMessage());
        }
    }

    // Simplified music control in Pomotime
    private void setupMusicControls() {
        imgBtn_music.setOnClickListener(v -> {
            if (soundPlayer.isPlaying()) {
                soundPlayer.stopSound();
            } else {
                // Use the appropriate play method based on session type
                if (isPomodoro) {
                    soundPlayer.playCurrentStudySound();
                } else {
                    soundPlayer.playBreakSound();
                }
            }
            updateMusicButtonIcon();
        });
    }

    private void updateMusicButtonIcon() {
        if (soundPlayer != null) {
            int iconRes = soundPlayer.isPlaying() ? R.drawable.ic_sound_on : R.drawable.ic_sound_off;
            imgBtn_music.setImageResource(iconRes);
        }
    }

    private void stopSound() {
        if (soundPlayer != null) {
            soundPlayer.stopSound();
            updateMusicButtonIcon();
        }
    }
    @Override
    public void onBackPressed() {
        if (isRunning) {
            showEndConfirmationDialog();
        } else {
            // Add exit animation
            overridePendingTransition(R.anim.fade_in, R.anim.slide_out_down);
            super.onBackPressed();
        }
    }
    public void openSettings(View view) {
        Intent intent = new Intent(this, TimerActivity.class);
        startActivity(intent);
    }
    @Override
    protected void onDestroy() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        
        // Clear all animations
        if (gifImageView != null) {
            gifImageView.clearAnimation();
        }
        
        if (soundPlayer != null) {
            soundPlayer.stopSound();
        }
        
        super.onDestroy();
    }

    private void playSessionSwitchNotificationSound() {
        try {
            MediaPlayer mp = MediaPlayer.create(this, R.raw.bell); // Use your bell sound resource
            if (mp != null) {
                mp.setOnCompletionListener(MediaPlayer::release);
                mp.start();
            }
        } catch (Exception e) {
            Log.e("Pomotime", "Failed to play bell notification sound", e);
        }
    }
}


