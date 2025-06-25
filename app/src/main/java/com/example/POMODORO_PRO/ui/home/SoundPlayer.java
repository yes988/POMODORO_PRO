package com.example.POMODORO_PRO.ui.home;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

public class SoundPlayer {
    private MediaPlayer mediaPlayer;
    private Context context;
    private int currentSoundId = 0;
    private int[] studySounds;
    private int breakSound;  // Changed from breakSounds
    private boolean isStudyMode = true;
    private int currentIndex = 0;  // Single index for both modes

    public SoundPlayer(Context context, int[] studySounds, int breakSound) {
        this.context = context;
        this.studySounds = studySounds;
        this.breakSound = breakSound;  // Changed from breakSounds
        Log.d("SoundPlayer", "Study sounds: " + java.util.Arrays.toString(studySounds));
        Log.d("SoundPlayer", "Break sound: " + breakSound);
    }

    public void playSound(int soundResId) {
        if (mediaPlayer != null) {
            if (currentSoundId == soundResId && mediaPlayer.isPlaying()) {
                Log.d("SoundPlayer", "Same sound already playing, skipping");
                return;
            }
            stopSound();
        }
        try {
            Log.d("SoundPlayer", "Playing sound with ID: " + soundResId);
            mediaPlayer = MediaPlayer.create(context, soundResId);
            if (mediaPlayer == null) {
                Log.e("SoundPlayer", "MediaPlayer.create() returned null for resId: " + soundResId);
                return;
            }
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
            currentSoundId = soundResId;
            Log.d("SoundPlayer", "Successfully started playing sound: " + soundResId);
        } catch (Exception e) {
            Log.e("SoundPlayer", "Error playing sound", e);
        }
    }

    public void playCurrentStudySound() {
        if (studySounds == null || studySounds.length == 0) {
            Log.w("SoundPlayer", "No study sounds available");
            return;
        }
        playSound(studySounds[currentIndex]);
    }

    public void playBreakSound() {
        playSound(breakSound);  // Just use the single break sound
    }

    public void switchToNextSound() {
        if (studySounds == null || studySounds.length == 0) return;
        currentIndex = (currentIndex + 1) % studySounds.length;
        Log.d("SoundPlayer", "Switched to sound index: " + currentIndex);
        if (isPlaying() && isStudyMode) {
            playCurrentStudySound();
        }
    }

    public String getCurrentSoundName() {
        if (studySounds == null || studySounds.length == 0) return "None";
        if (isStudyMode) {
            switch (currentIndex) {
                case 0: return "Lo-Fi Music";
                case 1: return "Thunder Sounds";
                default: return "Study Sound " + (currentIndex + 1);
            }
        } else {
            return "Tea Time Music";
        }
    }

    public void stopSound() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                    Log.d("SoundPlayer", "Stopped playing sound: " + currentSoundId);
                }
                mediaPlayer.release();
            } catch (Exception e) {
                Log.e("SoundPlayer", "Error stopping sound", e);
            }
            mediaPlayer = null;
            currentSoundId = 0;
        }
    }

    // In SoundPlayer:
    public void setStudyMode(boolean studyMode) {
        if (this.isStudyMode != studyMode) {
            this.isStudyMode = studyMode;
            // Reset index when changing modes
            currentIndex = 0;
            // Stop any currently playing sound
            stopSound();
        }
    }
    public boolean getStudyMode() {
        return isStudyMode;
    }
    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }
    public void setVolume(float volume) {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volume, volume);
        }
    }
    public void release() {
        stopSound();
    }
    public String getCurrentSessionType() {
        return isStudyMode ? "Study" : "Break";
    }

    public int getCurrentSoundIndex() {
        return currentIndex;
    }

    public void setCurrentSoundIndex(int index) {
        if (studySounds != null && index >= 0 && index < studySounds.length) {
            currentIndex = index;
            if (isPlaying() && isStudyMode) {
                playCurrentStudySound();
            }
        }
    }
}