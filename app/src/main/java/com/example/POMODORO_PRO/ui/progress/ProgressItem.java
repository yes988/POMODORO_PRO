package com.example.POMODORO_PRO.ui.progress;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProgressItem {
    private String date;  // Stored as String (e.g., "2023-11-15")
    private String taskName;
    private int accumulatedTime;
    private long timestamp;  // Store the timestamp directly
    private transient long dateTimestamp;  // Transient to avoid serialization

    // Add this constant to both classes
    public static final String STORAGE_DATE_FORMAT = "yyyy-MM-dd";
    public static final String DISPLAY_DATE_FORMAT = "dd/MM/yyyy";
    private static final SimpleDateFormat storageFormat =
            new SimpleDateFormat(STORAGE_DATE_FORMAT, Locale.getDefault());
    private static final SimpleDateFormat displayFormat =
            new SimpleDateFormat(DISPLAY_DATE_FORMAT, Locale.getDefault());
    public ProgressItem(String date, String taskName, int accumulatedTime) {
        // Ensure date is in storage format
        try {
            // If date is in display format, convert it to storage format
            if (date.matches("\\d{2}/\\d{2}/\\d{4}")) {
                Date parsedDate = displayFormat.parse(date);
                this.date = storageFormat.format(parsedDate);
            } else {
                this.date = date; // Assume it's already in storage format
            }
        } catch (ParseException e) {
            this.date = date; // Fallback to original if parsing fails
        }

        this.taskName = taskName;
        this.accumulatedTime = accumulatedTime;
        this.dateTimestamp = convertDateToTimestamp(this.date);
    }

    private long convertDateToTimestamp(String dateStr) {
        try {
            Date date = storageFormat.parse(dateStr);
            return date != null ? date.getTime() : 0;
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }



    public long getDateTimestamp() {
        if (dateTimestamp == 0) {
            dateTimestamp = convertDateToTimestamp(date);
        }
        return dateTimestamp;
    }



    public String getDate() { return date; }
    public String getTaskName() { return taskName; }
    public int getAccumulatedTime() { return accumulatedTime; }

    public void setAccumulatedTime(int time) {
        this.accumulatedTime = time;
    }

    public String formatTime() {
        int minutes = accumulatedTime / 60;
        int seconds = accumulatedTime % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
    public String getDisplayDate() {
        try {
            SimpleDateFormat storageFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date date = storageFormat.parse(this.date);
            return date != null ? displayFormat.format(date) : this.date;
        } catch (ParseException e) {
            return this.date;
        }
    }
    public void combineWith(ProgressItem other) {
        if (this.date.equals(other.date) && this.taskName.equals(other.taskName)) {
            this.accumulatedTime += other.accumulatedTime;
        }
    }


    public boolean isCompleted() {
        // Consider a task completed if it has at least one Pomodoro session (25 minutes)
        // You can adjust this threshold as needed for your app
        return accumulatedTime >= 25 * 60; // 25 minutes in seconds
    }

    // Add method to calculate completion percentage
    public float getCompletionPercentage(int targetTime) {
        if (targetTime <= 0) {
            // Default target time is one Pomodoro (25 minutes)
            targetTime = 25 * 60;
        }
        return Math.min(100f, (accumulatedTime * 100f) / targetTime);
    }
}
