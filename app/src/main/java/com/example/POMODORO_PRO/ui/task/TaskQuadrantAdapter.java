package com.example.POMODORO_PRO.ui.task;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.POMODORO_PRO.R;
import com.example.POMODORO_PRO.TaskDatabaseHelper;

import java.util.List;

public class TaskQuadrantAdapter {
    private final Context context;
    private final TaskDatabaseHelper dbHelper;

    public TaskQuadrantAdapter(Context context, TaskDatabaseHelper dbHelper) {
        this.context = context;
        this.dbHelper = dbHelper;
    }

    public void populateQuadrant(LinearLayout quadrantLayout, String category) {
        quadrantLayout.removeAllViews();
        List<Task> tasks = dbHelper.getTasksByCategory(category);

        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            View taskView = createTaskView(task);
            quadrantLayout.addView(taskView);
            
            // Apply animation with staggered delay
            taskView.startAnimation(AnimationUtils.loadAnimation(
                    context, R.anim.task_item_slide_in));
            taskView.setAlpha(0f);
            taskView.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setStartDelay(i * 50)
                    .start();
        }
    }

    private View createTaskView(Task task) {
        View view = LayoutInflater.from(context).inflate(R.layout.task_item, null);
        CheckBox checkBox = view.findViewById(R.id.taskCheckbox);
        TextView nameView = view.findViewById(R.id.taskName);
        ImageButton btnViewTask = view.findViewById(R.id.btnViewTask);

        // Set task data
        nameView.setText(task.getName());
        checkBox.setChecked(task.isCompleted());

        // Restore strikethrough state based on completion
        updateTaskViewState(task, checkBox, nameView);

        // Completion toggle with animation
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            dbHelper.updateTaskCompletion(task.getId(), isChecked);
            task.setCompleted(isChecked);
            
            // Apply completion animation
            view.startAnimation(AnimationUtils.loadAnimation(
                    context, R.anim.task_complete_animation));
            
            updateTaskViewState(task, checkBox, nameView);
            
            if (isChecked) {
                Toast.makeText(context, "Task completed!", Toast.LENGTH_SHORT).show();
            }
        });

        // Click listener for details
        view.setOnClickListener(v -> {
            v.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> {
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start();
                    
                    Intent intent = new Intent(context, TaskDetailsActivity.class);
                    intent.putExtra("TASK_ID", task.getId());
                    context.startActivity(intent);
                })
                .start();
        });
        
        // View details button
        btnViewTask.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(
                    context, R.anim.scale_up));
            
            Intent intent = new Intent(context, TaskDetailsActivity.class);
            intent.putExtra("TASK_ID", task.getId());
            context.startActivity(intent);
        });

        // Long click to delete with animation
        view.setOnLongClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(
                    context, R.anim.shake_animation));
            
            new AlertDialog.Builder(context)
                    .setTitle("Delete Task")
                    .setMessage("Are you sure you want to delete this task?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        // Fade out animation before removal
                        view.animate()
                                .alpha(0f)
                                .setDuration(300)
                                .withEndAction(() -> {
                                    dbHelper.deleteTask(task.getId());
                                    populateQuadrant((LinearLayout) v.getParent(), task.getCategory());
                                })
                                .start();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });

        return view;
    }
    
    private void updateTaskViewState(Task task, CheckBox checkBox, TextView nameView) {
        checkBox.setChecked(task.isCompleted());
        if (task.isCompleted()) {
            nameView.setPaintFlags(nameView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            nameView.setAlpha(0.6f);
        } else {
            nameView.setPaintFlags(nameView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            nameView.setAlpha(1.0f);
        }
    }
}
