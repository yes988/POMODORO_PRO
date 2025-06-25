package com.example.POMODORO_PRO.ui.report;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.example.POMODORO_PRO.R;
import com.example.POMODORO_PRO.SessionDatabaseHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class AchievementManager {
    private static final String PREFS_NAME = "AchievementPrefs";
    private static final String LAST_CHECK_DATE = "last_check_date";
    private SharedPreferences sharedPreferences;
    private List<Achievement> allAchievements;
    private SessionDatabaseHelper dbHelper;
    private Context context;
    private List<String> newlyUnlockedAchievements = new ArrayList<>();

    public AchievementManager(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        dbHelper = new SessionDatabaseHelper(context);
        initializeAchievements();
        checkAchievements();
    }

    private void initializeAchievements() {
        allAchievements = new ArrayList<>();

        // Time-based achievements
        allAchievements.add(new Achievement("first_session", "First Step", "Complete your first Pomodoro session", R.drawable.ic_first_step));
        allAchievements.add(new Achievement("one_hour", "Hour Power", "Complete 1 hour of focused work", R.drawable.ic_hour_power));
        allAchievements.add(new Achievement("five_hours", "Half-Day Hero", "Complete 5 hours of focused work", R.drawable.ic_half_day));
        allAchievements.add(new Achievement("ten_hours", "Daily Master", "Complete 10 hours of focused work", R.drawable.ic_daily_master));

        // Consistency achievements
        allAchievements.add(new Achievement("three_day_streak", "3-Day Streak", "Complete sessions for 3 consecutive days", R.drawable.ic_streak_3));
        allAchievements.add(new Achievement("seven_day_streak", "7-Day Streak", "Complete sessions for 7 consecutive days", R.drawable.ic_streak_7));
        allAchievements.add(new Achievement("thirty_day_streak", "Month Master", "Complete sessions for 30 consecutive days", R.drawable.ic_streak_30));

        // Task-specific achievements
        allAchievements.add(new Achievement("task_specialist", "Task Specialist", "Focus on a single task for 2 hours", R.drawable.ic_task_specialist));
        allAchievements.add(new Achievement("multitasker", "Multitasker", "Work on 5 different tasks in one day", R.drawable.ic_multitasker));
        
        // Advanced achievements
        allAchievements.add(new Achievement("night_owl", "Night Owl", "Complete 3 sessions after 10 PM", R.drawable.ic_night_owl));
        allAchievements.add(new Achievement("early_bird", "Early Bird", "Complete 3 sessions before 8 AM", R.drawable.ic_early_bird));
        allAchievements.add(new Achievement("weekend_warrior", "Weekend Warrior", "Complete 5 hours on weekends", R.drawable.ic_weekend_warrior));

        // Load unlocked status from preferences
        for (Achievement achievement : allAchievements) {
            achievement.setUnlocked(sharedPreferences.getBoolean(achievement.getId(), false));
        }
    }

    public void checkAchievements() {
        // Clear newly unlocked list before checking
        newlyUnlockedAchievements.clear();
        
        // Check first session achievement
        int totalSessions = dbHelper.getTotalSessionCount();
        unlockIfNot("first_session", totalSessions >= 1);

        // Check time-based achievements
        int totalTime = dbHelper.getTotalTimeForPeriod(null, null);
        unlockIfNot("one_hour", totalTime >= 3600);
        unlockIfNot("five_hours", totalTime >= 18000);
        unlockIfNot("ten_hours", totalTime >= 36000);

        // Check streak achievements
        checkStreakAchievements();

        // Check task-specific achievements
        checkTaskAchievements();
        
        // Check time-of-day achievements
        checkTimeOfDayAchievements();
        
        // Show notifications for newly unlocked achievements
        showUnlockNotifications();
        
        // Update last check date
        updateLastCheckDate();
    }
    
    private void checkTimeOfDayAchievements() {
        // Night Owl: Sessions after 10 PM
        int nightSessions = dbHelper.getSessionCountByTimeOfDay(22, 24); // 10 PM to midnight
        unlockIfNot("night_owl", nightSessions >= 3);
        
        // Early Bird: Sessions before 8 AM
        int morningSessions = dbHelper.getSessionCountByTimeOfDay(5, 8); // 5 AM to 8 AM
        unlockIfNot("early_bird", morningSessions >= 3);
        
        // Weekend Warrior: 5 hours on weekends
        int weekendTime = dbHelper.getWeekendSessionTime();
        unlockIfNot("weekend_warrior", weekendTime >= 18000); // 5 hours in seconds
    }

    private void checkStreakAchievements() {
        // Get the current streak from the database or preferences
        int currentStreak = getCurrentStreak();

        unlockIfNot("three_day_streak", currentStreak >= 3);
        unlockIfNot("seven_day_streak", currentStreak >= 7);
        unlockIfNot("thirty_day_streak", currentStreak >= 30);
    }

    private void checkTaskAchievements() {
        // Check for task specialist (2 hours on one task)
        Map<String, Integer> taskDistribution = dbHelper.getTaskDistribution(null, null);
        for (int time : taskDistribution.values()) {
            if (time >= 7200) { // 2 hours in seconds
                unlockIfNot("task_specialist", true);
                break;
            }
        }

        // Check for multitasker (5 different tasks in one day)
        int maxDailyTasks = dbHelper.getMaxDailyTaskVariety();
        unlockIfNot("multitasker", maxDailyTasks >= 5);
    }

    private void unlockIfNot(String achievementId, boolean condition) {
        if (condition) {
            for (Achievement achievement : allAchievements) {
                if (achievement.getId().equals(achievementId) && !achievement.isUnlocked()) {
                    achievement.setUnlocked(true);
                    sharedPreferences.edit().putBoolean(achievementId, true).apply();
                    newlyUnlockedAchievements.add(achievementId);
                    break;
                }
            }
        }
    }
    
    private void showUnlockNotifications() {
        if (newlyUnlockedAchievements.isEmpty() || !(context instanceof Activity)) {
            return;
        }
        
        Activity activity = (Activity) context;
        
        // Show notifications with a delay between each
        new Handler(Looper.getMainLooper()).post(() -> {
            for (int i = 0; i < newlyUnlockedAchievements.size(); i++) {
                final int index = i;
                new Handler().postDelayed(() -> {
                    String achievementId = newlyUnlockedAchievements.get(index);
                    Achievement achievement = getAchievementById(achievementId);
                    if (achievement != null) {
                        showAchievementToast(activity, achievement);
                    }
                }, i * 2500); // Show each notification 2.5 seconds apart
            }
        });
    }
    
    private void showAchievementToast(Activity activity, Achievement achievement) {
        // Create a custom toast layout
        View toastView = LayoutInflater.from(context).inflate(R.layout.achievement_unlocked_toast, 
                (ViewGroup) activity.findViewById(android.R.id.content).getRootView(), false);
        
        // Set achievement details
        ImageView icon = toastView.findViewById(R.id.toastAchievementIcon);
        TextView title = toastView.findViewById(R.id.toastAchievementTitle);
        TextView description = toastView.findViewById(R.id.toastAchievementDescription);
        CardView cardView = toastView.findViewById(R.id.toastCardView);
        
        icon.setImageResource(achievement.getIconResId());
        title.setText(achievement.getName());
        description.setText(achievement.getDescription());
        
        // Create and show the toast
        Toast toast = new Toast(context);
        toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 100);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(toastView);
        
        // Animate the toast
        Animation slideIn = AnimationUtils.loadAnimation(context, R.anim.achievement_toast_slide_in);
        cardView.startAnimation(slideIn);
        
        toast.show();
    }
    
    private Achievement getAchievementById(String id) {
        for (Achievement achievement : allAchievements) {
            if (achievement.getId().equals(id)) {
                return achievement;
            }
        }
        return null;
    }
    
    private void updateLastCheckDate() {
        sharedPreferences.edit().putLong(LAST_CHECK_DATE, System.currentTimeMillis()).apply();
    }

    public List<Achievement> getAllAchievements() {
        return allAchievements;
    }

    public List<Achievement> getUnlockedAchievements() {
        List<Achievement> unlocked = new ArrayList<>();
        for (Achievement achievement : allAchievements) {
            if (achievement.isUnlocked()) {
                unlocked.add(achievement);
            }
        }
        return unlocked;
    }

    public List<Achievement> getLockedAchievements() {
        List<Achievement> locked = new ArrayList<>();
        for (Achievement achievement : allAchievements) {
            if (!achievement.isUnlocked()) {
                locked.add(achievement);
            }
        }
        return locked;
    }
    
    public int getUnlockedCount() {
        return getUnlockedAchievements().size();
    }
    
    public int getTotalAchievementCount() {
        return allAchievements.size();
    }
    
    public float getCompletionPercentage() {
        return (float) getUnlockedCount() / getTotalAchievementCount() * 100f;
    }

    private int getCurrentStreak() {
        // Get the last check date
        long lastCheckTime = sharedPreferences.getLong(LAST_CHECK_DATE, 0);
        Date lastCheckDate = new Date(lastCheckTime);
        Date today = new Date();
        
        // If last check was more than 1 day ago, reset streak
        if (daysBetween(lastCheckDate, today) > 1) {
            sharedPreferences.edit().putInt("current_streak", 0).apply();
            return 0;
        }
        
        // Check if there was a session today
        Calendar cal = Calendar.getInstance();
        String todayStr = String.format("%04d-%02d-%02d", 
                cal.get(Calendar.YEAR), 
                cal.get(Calendar.MONTH) + 1, 
                cal.get(Calendar.DAY_OF_MONTH));
        
        boolean hadSessionToday = dbHelper.hasSessionsForDate(todayStr);
        
        // If had session today, increment streak
        int currentStreak = sharedPreferences.getInt("current_streak", 0);
        if (hadSessionToday && daysBetween(lastCheckDate, today) == 1) {
            currentStreak++;
            sharedPreferences.edit().putInt("current_streak", currentStreak).apply();
        }
        
        return currentStreak;
    }
    
    private int daysBetween(Date d1, Date d2) {
        return (int) ((d2.getTime() - d1.getTime()) / (1000 * 60 * 60 * 24));
    }
}