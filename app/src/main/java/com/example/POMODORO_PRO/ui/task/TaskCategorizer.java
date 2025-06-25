package com.example.POMODORO_PRO.ui.task;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TaskCategorizer {
    private static final int URGENT_DAYS_THRESHOLD = 2;

    public static String categorizeTask(Task task) {
        long daysRemaining = calculateDaysRemaining(task.getDate());
        String priority = task.getPriority();

        // Implement Eisenhower Matrix logic
        boolean isUrgent = daysRemaining <= URGENT_DAYS_THRESHOLD;
        boolean isImportant = "HIGH".equals(priority);

        if (isImportant && isUrgent) return "A"; // DO NOW
        if (isImportant) return "B";            // SCHEDULE
        if (isUrgent) return "C";               // DELEGATE
        return "D";                             // DELETE
    }

    private static long calculateDaysRemaining(String dueDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        try {
            Date due = sdf.parse(dueDate);
            Date today = new Date();
            return TimeUnit.DAYS.convert(due.getTime() - today.getTime(),
                    TimeUnit.MILLISECONDS);
        } catch (ParseException e) {
            return Long.MAX_VALUE; // If date is invalid, treat as not urgent
        }
    }
}
