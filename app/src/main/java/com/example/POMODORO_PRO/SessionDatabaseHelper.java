package com.example.POMODORO_PRO;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.POMODORO_PRO.ui.progress.ProgressItem;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SessionDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "sessions.db";
    private static final int DATABASE_VERSION = 2; // Incremented version

    private static final String TABLE_SESSIONS = "sessions";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_DATE = "date";
    private static final String COLUMN_TASK_NAME = "task_name";
    private static final String COLUMN_ACCUMULATED_TIME = "accumulated_time";
    private static final String COLUMN_TIMESTAMP = "timestamp"; // New column

    public SessionDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_SESSIONS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_DATE + " TEXT NOT NULL, " +
                COLUMN_TASK_NAME + " TEXT NOT NULL, " +
                COLUMN_ACCUMULATED_TIME + " INTEGER NOT NULL, " +
                COLUMN_TIMESTAMP + " INTEGER NOT NULL, " + // Stores timestamp in milliseconds
                "UNIQUE(" + COLUMN_DATE + ", " + COLUMN_TASK_NAME + ") ON CONFLICT REPLACE)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Migration to add timestamp column
            db.execSQL("ALTER TABLE " + TABLE_SESSIONS + " ADD COLUMN " +
                    COLUMN_TIMESTAMP + " INTEGER DEFAULT 0");
            // Convert existing dates to timestamps
            updateExistingTimestamps(db);
        }
    }

    private void updateExistingTimestamps(SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_ID + ", " + COLUMN_DATE +
                " FROM " + TABLE_SESSIONS, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(0);
                String dateStr = cursor.getString(1);
                long timestamp = convertDateToTimestamp(dateStr);

                ContentValues values = new ContentValues();
                values.put(COLUMN_TIMESTAMP, timestamp);
                db.update(TABLE_SESSIONS, values, COLUMN_ID + " = ?",
                        new String[]{String.valueOf(id)});
            } while (cursor.moveToNext());  // Changed from moveNext() to moveToNext()
            cursor.close();
        }
    }

    // Add this constant at the top of your SessionDatabaseHelper
    // Update this line
    private static final SimpleDateFormat DB_DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); // MATCHES ProgressItem now


    // Update the convertDateToTimestamp method
    private long convertDateToTimestamp(String dateStr) {
        try {
            // Try storage format first
            SimpleDateFormat storageFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            return storageFormat.parse(dateStr).getTime();
        } catch (Exception e1) {
            try {
                // Fallback to display format
                SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                return displayFormat.parse(dateStr).getTime();
            } catch (Exception e2) {
                return 0;
            }
        }
    }


    // Add or update a session
    public void addSession(ProgressItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_DATE, item.getDate());
        values.put(COLUMN_TASK_NAME, item.getTaskName());
        values.put(COLUMN_ACCUMULATED_TIME, item.getAccumulatedTime());
        values.put(COLUMN_TIMESTAMP, item.getDateTimestamp());

        db.insertWithOnConflict(TABLE_SESSIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    // Get all sessions grouped by date and task name
    public List<ProgressItem> getAllSessions() {
        return getSessionsBetweenDates(null, null);
    }
    // Add to SessionDatabaseHelper.java
    public boolean deleteSession(String date, String taskName) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = db.delete(TABLE_SESSIONS,
                COLUMN_DATE + " = ? AND " + COLUMN_TASK_NAME + " = ?",
                new String[]{date, taskName});
        db.close();
        return rowsAffected > 0;
    }
    // Get sessions between two dates (inclusive)
    public List<ProgressItem> getSessionsBetweenDates(String startDate, String endDate) {
        List<ProgressItem> sessions = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            String selection = null;
            String[] selectionArgs = null;

            if (startDate != null && endDate != null) {
                // Use exact date matching instead of timestamp range
                if (startDate.equals(endDate)) {
                    // For single day reports, use exact date match
                    selection = COLUMN_DATE + " = ?";
                    selectionArgs = new String[]{startDate};
                } else {
                    // For date ranges, use BETWEEN on the date column
                    selection = COLUMN_DATE + " BETWEEN ? AND ?";
                    selectionArgs = new String[]{startDate, endDate};
                }
            }

            cursor = db.query(
                    TABLE_SESSIONS,
                    new String[]{
                            COLUMN_DATE,
                            COLUMN_TASK_NAME,
                            "SUM(" + COLUMN_ACCUMULATED_TIME + ") as total_time",
                            COLUMN_TIMESTAMP
                    },
                    selection,
                    selectionArgs,
                    COLUMN_DATE + ", " + COLUMN_TASK_NAME,
                    null,
                    COLUMN_TIMESTAMP + " DESC, " + COLUMN_TASK_NAME
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String date = cursor.getString(0);
                    String taskName = cursor.getString(1);
                    int accumulatedTime = cursor.getInt(2);
                    long dateTimestamp = cursor.getLong(3); // Get the timestamp
                    sessions.add(new ProgressItem(date, taskName, accumulatedTime));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return sessions;
    }
    // Add these methods to your SessionDatabaseHelper class

    // Get daily report (for a specific date)
    public List<ProgressItem> getDailyReport(String date) {
        return getSessionsBetweenDates(date, date);
    }

    // Get weekly report (for a specific week)
    public List<ProgressItem> getWeeklyReport(String startDateOfWeek) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdf.parse(startDateOfWeek);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.add(Calendar.DATE, 6); // Add 6 days to get end of week
            String endDateOfWeek = sdf.format(cal.getTime());
            return getSessionsBetweenDates(startDateOfWeek, endDateOfWeek);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // Get monthly report (for a specific month)
    public List<ProgressItem> getMonthlyReport(String yearMonth) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
            Date date = sdf.parse(yearMonth);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);

            // Get first day of month
            cal.set(Calendar.DAY_OF_MONTH, 1);
            String startDate = DB_DATE_FORMAT.format(cal.getTime());

            // Get last day of month
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            String endDate = DB_DATE_FORMAT.format(cal.getTime());

            return getSessionsBetweenDates(startDate, endDate);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // Get all unique task names
    public List<String> getAllTaskNames() {
        List<String> taskNames = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.query(true, TABLE_SESSIONS,
                    new String[]{COLUMN_TASK_NAME},
                    null, null,
                    COLUMN_TASK_NAME,
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    taskNames.add(cursor.getString(0));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return taskNames;
    }
    // Get total time for a specific period
    public int getTotalTimeForPeriod(Long startTimestamp, Long endTimestamp) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            String selection = null;
            String[] selectionArgs = null;

            if (startTimestamp != null && endTimestamp != null) {
                selection = COLUMN_TIMESTAMP + " BETWEEN ? AND ?";
                selectionArgs = new String[]{String.valueOf(startTimestamp),
                        String.valueOf(endTimestamp)};
            }

            cursor = db.query(TABLE_SESSIONS,
                    new String[]{"SUM(" + COLUMN_ACCUMULATED_TIME + ")"},
                    selection,
                    selectionArgs,
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return 0;
    }

    // Get task distribution for a period
    public Map<String, Integer> getTaskDistribution(Long startTimestamp, Long endTimestamp) {
        Map<String, Integer> distribution = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            String selection = null;
            String[] selectionArgs = null;

            if (startTimestamp != null && endTimestamp != null) {
                selection = COLUMN_TIMESTAMP + " BETWEEN ? AND ?";
                selectionArgs = new String[]{String.valueOf(startTimestamp),
                        String.valueOf(endTimestamp)};
            }

            cursor = db.query(TABLE_SESSIONS,
                    new String[]{COLUMN_TASK_NAME, "SUM(" + COLUMN_ACCUMULATED_TIME + ")"},
                    selection,
                    selectionArgs,
                    COLUMN_TASK_NAME,
                    null,
                    "SUM(" + COLUMN_ACCUMULATED_TIME + ") DESC");

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    distribution.put(cursor.getString(0), cursor.getInt(1));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return distribution;
    }

    // Get all available dates with data
    public List<String> getAllDatesWithData() {
        List<String> dates = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.query(true, TABLE_SESSIONS,
                    new String[]{COLUMN_DATE},
                    null, null,
                    COLUMN_DATE,
                    null,
                    COLUMN_TIMESTAMP + " DESC",
                    null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    dates.add(cursor.getString(0));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return dates;
    }

    // Get total session count
    public int getTotalSessionCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM sessions", null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    // Get session count by time of day
    public int getSessionCountByTimeOfDay(int startHour, int endHour) {
        SQLiteDatabase db = this.getReadableDatabase();
        int count = 0;
        
        String query = "SELECT COUNT(*) FROM " + TABLE_SESSIONS + 
                " WHERE strftime('%H', datetime(" + COLUMN_TIMESTAMP + "/1000, 'unixepoch', 'localtime')) >= ? " +
                "AND strftime('%H', datetime(" + COLUMN_TIMESTAMP + "/1000, 'unixepoch', 'localtime')) < ?";
        
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, new String[] {
                    String.format(Locale.US, "%02d", startHour),
                    String.format(Locale.US, "%02d", endHour)
            });
            
            if (cursor != null && cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        
        return count;
    }
    
    // Get total time spent on weekend sessions
    public int getWeekendSessionTime() {
        SQLiteDatabase db = this.getReadableDatabase();
        int totalTime = 0;
        
        String query = "SELECT SUM(" + COLUMN_ACCUMULATED_TIME + ") FROM " + TABLE_SESSIONS + 
                " WHERE strftime('%w', datetime(" + COLUMN_TIMESTAMP + "/1000, 'unixepoch', 'localtime')) IN ('0', '6')";
        
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, null);
            
            if (cursor != null && cursor.moveToFirst() && !cursor.isNull(0)) {
                totalTime = cursor.getInt(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        
        return totalTime;
    }
    
    // Check if there are sessions for a specific date
    public boolean hasSessionsForDate(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        boolean hasData = false;
        
        String query = "SELECT COUNT(*) FROM " + TABLE_SESSIONS + 
                " WHERE " + COLUMN_DATE + " = ?";
        
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, new String[] { date });
            
            if (cursor != null && cursor.moveToFirst()) {
                hasData = cursor.getInt(0) > 0;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        
        return hasData;
    }
    
    // Get the maximum number of different tasks completed in a single day
    public int getMaxDailyTaskVariety() {
        SQLiteDatabase db = this.getReadableDatabase();
        int maxTasks = 0;
        
        String query = "SELECT COUNT(DISTINCT " + COLUMN_TASK_NAME + ") as task_count " +
                "FROM " + TABLE_SESSIONS + " GROUP BY " + COLUMN_DATE + " ORDER BY task_count DESC LIMIT 1";
        
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, null);
            
            if (cursor != null && cursor.moveToFirst() && !cursor.isNull(0)) {
                maxTasks = cursor.getInt(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        
        return maxTasks;
    }
}