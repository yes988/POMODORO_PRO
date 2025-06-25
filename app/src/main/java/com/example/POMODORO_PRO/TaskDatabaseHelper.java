package com.example.POMODORO_PRO;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.POMODORO_PRO.ui.task.Task;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class TaskDatabaseHelper extends SQLiteOpenHelper {
    // Database constants
    private static final String DATABASE_NAME = "tasks.db";
    private static final int DATABASE_VERSION = 4;  // Incremented for new changes

    // Table and column constants
    private static final String TABLE_TASKS = "tasks";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_DESCRIPTION = "description";
    private static final String COLUMN_DUE_DATE = "due_date";
    private static final String COLUMN_PRIORITY = "priority";
    private static final String COLUMN_CATEGORY = "category";
    private static final String COLUMN_COMPLETED = "completed";
    private static final String COLUMN_CREATED_AT = "created_at";  // New column for sorting
    private static final String COLUMN_COMPLETED_DATE = "completed_date"; // New column for tracking completion date

    // SQL statements
    private static final String CREATE_TASKS_TABLE = "CREATE TABLE " + TABLE_TASKS + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_NAME + " TEXT NOT NULL, " +
            COLUMN_DESCRIPTION + " TEXT, " +
            COLUMN_DUE_DATE + " TEXT NOT NULL, " +
            COLUMN_PRIORITY + " TEXT NOT NULL, " +
            COLUMN_CATEGORY + " TEXT NOT NULL, " +
            COLUMN_COMPLETED + " INTEGER DEFAULT 0, " +
            COLUMN_CREATED_AT + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            COLUMN_COMPLETED_DATE + " TEXT)";

    // Indexes for better performance
    private static final String CREATE_CATEGORY_INDEX =
            "CREATE INDEX idx_category ON " + TABLE_TASKS + "(" + COLUMN_CATEGORY + ")";
    private static final String CREATE_PRIORITY_INDEX =
            "CREATE INDEX idx_priority ON " + TABLE_TASKS + "(" + COLUMN_PRIORITY + ")";

    public TaskDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TASKS_TABLE);
        db.execSQL(CREATE_CATEGORY_INDEX);
        db.execSQL(CREATE_PRIORITY_INDEX);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle database migrations properly
        if (oldVersion < 2) {
            // Add completed column
            db.execSQL("ALTER TABLE " + TABLE_TASKS +
                    " ADD COLUMN " + COLUMN_COMPLETED + " INTEGER DEFAULT 0");
        }
        if (oldVersion < 3) {
            // First add the column without default
            db.execSQL("ALTER TABLE " + TABLE_TASKS +
                    " ADD COLUMN " + COLUMN_CREATED_AT + " TIMESTAMP");

            // Then update all existing rows
            db.execSQL("UPDATE " + TABLE_TASKS +
                    " SET " + COLUMN_CREATED_AT + " = datetime('now')");
        }
        if (oldVersion < 4) {
            // Add completed_date column
            db.execSQL("ALTER TABLE " + TABLE_TASKS +
                    " ADD COLUMN " + COLUMN_COMPLETED_DATE + " TEXT");
        }
    }

    // Database operations with proper transaction handling
    public long addTask(@NonNull Task task) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_NAME, task.getName());
        values.put(COLUMN_DESCRIPTION, task.getDescription());
        values.put(COLUMN_DUE_DATE, task.getDate());
        values.put(COLUMN_PRIORITY, task.getPriority());
        values.put(COLUMN_CATEGORY, task.getCategory());
        values.put(COLUMN_COMPLETED, task.isCompleted() ? 1 : 0);

        try {
            db.beginTransaction();
            long id = db.insert(TABLE_TASKS, null, values);
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public List<Task> getAllTasks() {
        return getTasksWithQuery("SELECT * FROM " + TABLE_TASKS +
                " ORDER BY " + COLUMN_CREATED_AT + " DESC");
    }

    public List<Task> getTasksByCategory(String category) {
        return getTasksWithQuery("SELECT * FROM " + TABLE_TASKS +
                        " WHERE " + COLUMN_CATEGORY + " = ?" +
                        " ORDER BY " + COLUMN_PRIORITY + " DESC, " +
                        COLUMN_DUE_DATE + " ASC",
                new String[]{category});
    }

    private List<Task> getTasksWithQuery(String query, String... selectionArgs) {
        List<Task> taskList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        try (Cursor cursor = db.rawQuery(query, selectionArgs)) {
            if (cursor.moveToFirst()) {
                do {
                    taskList.add(createTaskFromCursor(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
        return taskList;
    }

    @NonNull
    private Task createTaskFromCursor(@NonNull Cursor cursor) {
        return new Task(
                cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DUE_DATE)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRIORITY)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)),
                cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_COMPLETED)) == 1
        );
    }

    public boolean deleteTask(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.beginTransaction();
            int rowsDeleted = db.delete(TABLE_TASKS, COLUMN_ID + " = ?",
                    new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
            return rowsDeleted > 0;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public boolean updateTaskCompletion(int taskId, boolean completed) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_COMPLETED, completed ? 1 : 0);
        
        // If task is marked as completed, store the current date
        if (completed) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String currentDate = dateFormat.format(new Date());
            values.put(COLUMN_COMPLETED_DATE, currentDate);
        } else {
            // If unchecked, clear the completion date
            values.putNull(COLUMN_COMPLETED_DATE);
        }

        try {
            db.beginTransaction();
            int rowsAffected = db.update(TABLE_TASKS, values, COLUMN_ID + " = ?",
                    new String[]{String.valueOf(taskId)});
            db.setTransactionSuccessful();
            return rowsAffected > 0;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    /**
     * Removes all completed tasks from days before today.
     * This is used to automatically clean up completed tasks each day.
     *
     * @return Number of tasks removed
     */
    public int purgeCompletedTasksFromPreviousDays() {
        SQLiteDatabase db = this.getWritableDatabase();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = dateFormat.format(new Date());
        
        try {
            db.beginTransaction();
            // Delete tasks that are completed and have a completion date before today
            int rowsDeleted = db.delete(
                TABLE_TASKS, 
                COLUMN_COMPLETED + " = 1 AND " + COLUMN_COMPLETED_DATE + " < ?",
                new String[]{today}
            );
            db.setTransactionSuccessful();
            return rowsDeleted;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    @Nullable
    public Task getTaskById(int taskId) {
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.query(TABLE_TASKS, null, COLUMN_ID + " = ?",
                new String[]{String.valueOf(taskId)}, null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                return createTaskFromCursor(cursor);
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            db.close();
        }
    }

    public boolean updateTask(@NonNull Task task) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_NAME, task.getName());
        values.put(COLUMN_DESCRIPTION, task.getDescription());
        values.put(COLUMN_DUE_DATE, task.getDate());
        values.put(COLUMN_PRIORITY, task.getPriority());
        values.put(COLUMN_CATEGORY, task.getCategory());
        values.put(COLUMN_COMPLETED, task.isCompleted() ? 1 : 0);
        
        // Update completion date if task is completed
        if (task.isCompleted()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String currentDate = dateFormat.format(new Date());
            values.put(COLUMN_COMPLETED_DATE, currentDate);
        } else {
            values.putNull(COLUMN_COMPLETED_DATE);
        }

        try {
            db.beginTransaction();
            int rowsAffected = db.update(TABLE_TASKS, values, COLUMN_ID + " = ?",
                    new String[]{String.valueOf(task.getId())});
            db.setTransactionSuccessful();
            return rowsAffected > 0;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    @Nullable
    public Task getTaskByName(@NonNull String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.query(TABLE_TASKS, null, COLUMN_NAME + " = ?",
                new String[]{name}, null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                return createTaskFromCursor(cursor);
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            db.close();
        }
    }
}