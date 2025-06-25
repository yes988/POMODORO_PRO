package com.example.POMODORO_PRO.ui.home;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.POMODORO_PRO.R;
import com.example.POMODORO_PRO.TaskDatabaseHelper;
import com.example.POMODORO_PRO.ui.task.Task;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.POMODORO_PRO.databinding.FragmentHomeBinding;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeFragment extends Fragment {
    private TextView txtPomodoroTime;
    private Spinner taskSpinner;
    private TaskDatabaseHelper dbHelper;
    private FragmentHomeBinding binding;


    private static final String NO_TASK_SELECTED = "No Task Selected";
    private static final String NO_TASKS_AVAILABLE = "No tasks available";
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        taskSpinner = binding.spinner;
        dbHelper = new TaskDatabaseHelper(requireContext());

        loadTaskNames();



        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("PomodoroPrefs", getContext().MODE_PRIVATE);
        int pomodoroTime = sharedPreferences.getInt("POMODORO_TIME", 25);
        txtPomodoroTime = binding.txtPomodoroTime;
        txtPomodoroTime.setText(String.format("Pomodoro Time: %d min", pomodoroTime));

        FloatingActionButton FAbtn_timeSet = binding.FAbtnTimeSet;
        FAbtn_timeSet.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), TimerActivity.class);
            startActivity(intent);
        });

        FloatingActionButton btnStartTimer = binding.FAbtnStartTime;
        btnStartTimer.setOnClickListener(v -> {
            String selectedTask = taskSpinner.getSelectedItem() != null ?
                    taskSpinner.getSelectedItem().toString() : "No Task Selected";



// Then use:
            if (!selectedTask.equals(NO_TASK_SELECTED) && !selectedTask.equals(NO_TASKS_AVAILABLE)) {

                // More robust parsing that handles cases where priority isn't appended
                String selectedItem = taskSpinner.getSelectedItem().toString();
                String taskName;
                if (selectedItem.contains(" (")) {
                    taskName = selectedItem.substring(0, selectedItem.indexOf(" ("));
                } else {
                    taskName = selectedItem;
                }
                checkTaskPriority(taskName);
            }
        });



        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView txtPomodoroTime = view.findViewById(R.id.txtPomodoroTime);

        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("PomodoroPrefs", Context.MODE_PRIVATE);
        int pomodoroTime = sharedPreferences.getInt("pomodoroTime", 25); // Default to 25 if not set

        String timeText = "Pomodoro Time: " + pomodoroTime + " mins";
        txtPomodoroTime.setText(timeText);
    }


    private void loadTaskNames() {
        // Get all tasks from database
        List<Task> tasks = dbHelper.getAllTasks();

        // Filter out completed tasks
        List<Task> unfinishedTasks = new ArrayList<>();
        for (Task task : tasks) {
            if (!task.isCompleted()) {
                unfinishedTasks.add(task);
            }
        }

        // Sort remaining tasks by priority (High to Low)
        Collections.sort(unfinishedTasks, (t1, t2) -> {
            int p1 = getPriorityValue(t1.getPriority());
            int p2 = getPriorityValue(t2.getPriority());
            return Integer.compare(p2, p1); // Descending order
        });

        List<String> taskNames = new ArrayList<>();
        for (Task task : unfinishedTasks) {
            taskNames.add(task.getName() + " (" + task.getPriority() + ")");
        }

        if (taskNames.isEmpty()) {
            taskNames.add(NO_TASKS_AVAILABLE);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                taskNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        taskSpinner.setAdapter(adapter);
    }

    private int getPriorityValue(String priority) {
        if (priority == null) return 0;
        switch (priority.toLowerCase()) {
            case "high": return 3;
            case "medium": return 2;
            case "low": return 1;
            default: return 0;
        }
    }

    private void checkTaskPriority(String selectedTaskName) {
        Task selectedTask = dbHelper.getTaskByName(selectedTaskName);
        if (selectedTask == null) {
            Toast.makeText(getContext(), "Task not found", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Task> allTasks = dbHelper.getAllTasks();
        List<Task> higherPriorityUnfinished = new ArrayList<>();

        for (Task task : allTasks) {
            if (!task.isCompleted() &&
                    isHigherPriority(task.getPriority(), selectedTask.getPriority())) {
                higherPriorityUnfinished.add(task);
            }
        }

        if (!higherPriorityUnfinished.isEmpty()) {
            showPriorityWarningDialog(selectedTask, higherPriorityUnfinished);
        } else {
            startPomodoro(selectedTask.getName());
        }
    }

    private boolean isHigherPriority(String taskPriority, String selectedPriority) {
        // Convert priorities to numerical values for comparison
        int taskVal = getPriorityValue(taskPriority);
        int selectedVal = getPriorityValue(selectedPriority);
        return taskVal > selectedVal;
    }

    private void showPriorityWarningDialog(Task selectedTask, List<Task> higherPriorityTasks) {
        StringBuilder message = new StringBuilder();
        message.append("You have ")
                .append(higherPriorityTasks.size())
                .append(" higher priority tasks unfinished:\n\n");

        for (Task task : higherPriorityTasks) {
            message.append("• ").append(task.getName())
                    .append(" (").append(task.getPriority()).append(")\n");
        }

        message.append("\nAre you sure you want to proceed with ")
                .append(selectedTask.getName())
                .append(" (").append(selectedTask.getPriority()).append(")?");

        new AlertDialog.Builder(requireContext())
                .setTitle("⚠ Priority Warning")
                .setMessage(message.toString())
                .setPositiveButton("Proceed Anyway", (dialog, which) ->
                        startPomodoro(selectedTask.getName()))
                .setNegativeButton("Cancel", null)
                .show();
    }
    @Override
    public void onResume() {
        super.onResume();
        loadTaskNames();
        updatePomodoroTime();
    }

    private void updatePomodoroTime() {
        TextView txtPomodoroTime = getView().findViewById(R.id.txtPomodoroTime);
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("PomodoroPrefs", Context.MODE_PRIVATE);
        int pomodoroTime = sharedPreferences.getInt("pomodoroTime", 25);
        txtPomodoroTime.setText("Pomodoro Time: " + pomodoroTime + " mins");
    }
    private void startPomodoro(String taskName) {
        Intent intent = new Intent(getActivity(), Pomotime.class);
        intent.putExtra("SELECTED_TASK", taskName);
        intent.putExtra("START_IMMEDIATELY", true);  // Add this flag
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}