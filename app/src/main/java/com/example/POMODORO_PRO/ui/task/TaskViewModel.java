package com.example.POMODORO_PRO.ui.task;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.POMODORO_PRO.TaskDatabaseHelper;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskViewModel extends ViewModel {
    private final MutableLiveData<Boolean> taskUpdated = new MutableLiveData<>();
    private final MutableLiveData<List<Task>> tasksLiveData = new MutableLiveData<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Single, properly implemented setter for taskUpdated
    public void setTaskUpdated(boolean updated) {
        taskUpdated.postValue(updated); // Use postValue for background thread safety
    }

    public LiveData<Boolean> getTaskUpdated() {
        return taskUpdated;
    }

    public LiveData<List<Task>> getTasks() {
        return tasksLiveData;
    }

    public void loadTasks(TaskDatabaseHelper dbHelper) {
        executor.execute(() -> {
            List<Task> tasks = dbHelper.getAllTasks();
            tasksLiveData.postValue(tasks);
        });
    }

    public void addTask(TaskDatabaseHelper dbHelper, Task task) {
        executor.execute(() -> {
            long taskId = dbHelper.addTask(task);
            if (taskId != -1) {
                loadTasks(dbHelper); // Refresh the task list
                setTaskUpdated(true); // Notify observers
            }
        });
    }

    public void updateTask(TaskDatabaseHelper dbHelper, Task task) {
        executor.execute(() -> {
            boolean success = dbHelper.updateTask(task);
            if (success) {
                loadTasks(dbHelper); // Refresh the task list
                setTaskUpdated(true); // Notify observers
            }
        });
    }

    public void deleteTask(TaskDatabaseHelper dbHelper, int taskId) {
        executor.execute(() -> {
            dbHelper.deleteTask(taskId);
            loadTasks(dbHelper); // Refresh the task list
            setTaskUpdated(true); // Notify observers
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown(); // Clean up the executor when ViewModel is no longer used
    }
}