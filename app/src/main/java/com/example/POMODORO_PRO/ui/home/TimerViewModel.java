package com.example.POMODORO_PRO.ui.home;

import androidx.lifecycle.ViewModel;

public class TimerViewModel extends ViewModel {
    // Timer state
    public long timeLeftInMillis;
    public long totalElapsedTime;
    public boolean isRunning;
    public boolean isPomodoro;
    public int currentSoundId;
    
    // Configuration values
    public int pomodoroTime;
    public int breakTime;
    public int totalStudyTime;
    
    // Sound state
    public boolean isMuted;
    public int currentStudySoundIndex;
    
    // Last update timestamp to calculate elapsed time during orientation changes
    public long lastUpdateTimestamp;
    
    // Flag to track if the ViewModel has been initialized
    public boolean isInitialized = false;
}