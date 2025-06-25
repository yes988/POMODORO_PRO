package com.example.POMODORO_PRO.ui.task;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.POMODORO_PRO.R;
import com.example.POMODORO_PRO.TaskDatabaseHelper;
import com.example.POMODORO_PRO.databinding.FragmentDashboardBinding;
import com.google.android.material.snackbar.Snackbar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class DashboardFragment extends Fragment {
    private FragmentDashboardBinding binding;
    private TaskDatabaseHelper dbHelper;
    private TaskQuadrantAdapter quadrantAdapter;
    private TaskViewModel taskViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbHelper = new TaskDatabaseHelper(requireContext());
        quadrantAdapter = new TaskQuadrantAdapter(requireContext(), dbHelper);
        taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);

        // Apply animations to main elements
        animateUIElements();
        
        setupTaskInput();
        setupObservers();
        taskViewModel.loadTasks(dbHelper); // Initial load
    }
    
    private void animateUIElements() {
        // Animate title with fade in
        Animation fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in);
        fadeIn.setDuration(800);
        binding.taskTable.startAnimation(fadeIn);
        
        // Animate quadrants with staggered entry
        int delay = 300;
        animateViewWithDelay(binding.layoutDoNow, delay);
        animateViewWithDelay(binding.layoutSchedule, delay + 100);
        animateViewWithDelay(binding.layoutDelegate, delay + 200);
        animateViewWithDelay(binding.layoutDelete, delay + 300);
    }
    
    private void animateViewWithDelay(View view, int delay) {
        view.setAlpha(0f);
        view.setTranslationY(50f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(delay)
                .start();
    }

    private void setupTaskInput() {
        // Priority spinner setup with custom style
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.spinner_item,
                new String[]{"HIGH", "MEDIUM", "LOW"}
        );
        priorityAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        binding.taskPriority.setAdapter(priorityAdapter);

        // Date picker setup with animation
        binding.taskDate.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(
                    requireContext(), R.anim.scale_up));
            showDatePicker();
        });

        // Add task button with improved animation
        binding.btnAddTask.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(
                    requireContext(), R.anim.scale_up));
            addNewTask();
        });
        
        // Add text change listeners for real-time validation
        binding.taskName.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateTaskName();
            }
        });
    }
    
    private boolean validateTaskName() {
        String name = binding.taskName.getText().toString().trim();
        if (name.isEmpty()) {
            binding.taskName.setError("Task name is required");
            return false;
        }
        return true;
    }

    private void setupObservers() {
        taskViewModel.getTaskUpdated().observe(getViewLifecycleOwner(), updated -> {
            if (updated != null && updated) {
                taskViewModel.setTaskUpdated(false);
            }
        });

        // Add this observer
        taskViewModel.getTasks().observe(getViewLifecycleOwner(), tasks -> {
            updateAllQuadrants();
        });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                R.style.DatePickerTheme,
                (view, year, month, day) -> {
                    String date = String.format(Locale.getDefault(),
                            "%02d/%02d/%04d", day, month + 1, year);
                    binding.taskDate.setText(date);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void addNewTask() {
        if (!validateTaskName()) {
            // Shake animation for error
            binding.taskName.startAnimation(AnimationUtils.loadAnimation(
                    requireContext(), R.anim.shake_animation));
            return;
        }
        
        String name = binding.taskName.getText().toString().trim();
        String description = binding.taskDescription.getText().toString().trim();
        String date = binding.taskDate.getText().toString().trim();
        String priority = binding.taskPriority.getSelectedItem().toString();

        if (date.isEmpty()) {
            binding.taskDate.setError("Due date is required");
            binding.taskDate.startAnimation(AnimationUtils.loadAnimation(
                    requireContext(), R.anim.shake_animation));
            return;
        }

        // Validate that the date is not in the past
        if (!isDateValid(date)) {
            binding.taskDate.setError("Due date cannot be in the past");
            binding.taskDate.startAnimation(AnimationUtils.loadAnimation(
                    requireContext(), R.anim.shake_animation));
            return;
        }

        String category = TaskCategorizer.categorizeTask(
                new Task(name, description, date, priority, ""));

        Task newTask = new Task(name, description, date, priority, category);
        long taskId = dbHelper.addTask(newTask);

        if (taskId != -1) {
            updateAllQuadrants();
            clearInputs();
            
            // Show success message with category info
            String message = "Task added to " + getCategoryName(category);
            Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(getResources().getColor(R.color.important_purple))
                    .setTextColor(getResources().getColor(R.color.white))
                    .show();
        } else {
            Toast.makeText(requireContext(),
                    "Failed to add task", Toast.LENGTH_SHORT).show();
        }
    }

    // Helper method to validate if the date is not in the past
    private boolean isDateValid(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            sdf.setLenient(false);
            Date inputDate = sdf.parse(dateStr);
            
            // Set current date to start of day for fair comparison
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            
            // Return true if date is today or in the future
            return !inputDate.before(today.getTime());
        } catch (ParseException e) {
            return false;
        }
    }

    private void updateAllQuadrants() {
        quadrantAdapter.populateQuadrant(binding.layoutDoNow, "A");
        quadrantAdapter.populateQuadrant(binding.layoutSchedule, "B");
        quadrantAdapter.populateQuadrant(binding.layoutDelegate, "C");
        quadrantAdapter.populateQuadrant(binding.layoutDelete, "D");
    }

    private String getCategoryName(String category) {
        switch (category) {
            case "A": return "DO NOW";
            case "B": return "SCHEDULE";
            case "C": return "DELEGATE";
            case "D": return "DELETE";
            default: return "UNCATEGORIZED";
        }
    }

    private void clearInputs() {
        binding.taskName.setText("");
        binding.taskDescription.setText("");
        binding.taskDate.setText("");
        binding.taskPriority.setSelection(0);
        
        // Clear any error states
        binding.taskName.setError(null);
        binding.taskDate.setError(null);
    }
    private void refreshQuadrant() {
        taskViewModel.loadTasks(dbHelper);
        // The LiveData observer will then update the UI
    }
    @Override
    public void onResume() {
        super.onResume();
        taskViewModel.loadTasks(dbHelper); // This will trigger the LiveData update
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}