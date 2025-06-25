package com.example.POMODORO_PRO.ui.task;
import static androidx.navigation.fragment.FragmentKt.findNavController;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.app.DatePickerDialog;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Calendar;

import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;

import com.example.POMODORO_PRO.MainActivity;
import com.example.POMODORO_PRO.R;
import com.example.POMODORO_PRO.TaskDatabaseHelper;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TaskDetailsActivity extends AppCompatActivity {

    private EditText taskName, taskDescription, taskDueDate;
    private MaterialButton btnEdit, btnDelete;
    private Spinner taskPrioritySpinner;
    private CardView descriptionCard, dueDateCard, priorityCard, actionButtonsCard;

    private FloatingActionButton btnDone;
    private TaskDatabaseHelper taskDatabase;
    private Task currentTask;
    private int taskId;
    private CollapsingToolbarLayout collapsingToolbar;

    private TaskViewModel taskViewModel;
    private boolean isEditing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_details);
        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        
        collapsingToolbar = findViewById(R.id.collapsingToolbar);

        // Initialize UI components
        taskName = findViewById(R.id.taskName);
        taskDescription = findViewById(R.id.taskDescription);
        taskDueDate = findViewById(R.id.taskDueDate);
        taskPrioritySpinner = findViewById(R.id.taskPrioritySpinner);
        
        // Initialize cards for animations
        descriptionCard = findViewById(R.id.descriptionCard);
        dueDateCard = findViewById(R.id.dueDateCard);
        priorityCard = findViewById(R.id.priorityCard);
        actionButtonsCard = findViewById(R.id.actionButtonsCard);
        
        // Initialize buttons
        ImageButton btnReturn = findViewById(R.id.btn_return_dashboard);
        btnEdit = findViewById(R.id.IMGbtn_edit);
        btnDelete = findViewById(R.id.IMGbtn_del);
        btnDone = findViewById(R.id.FAbtn_done);

        // Setup spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.priority_array,
                R.layout.spinner_item_white);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        taskPrioritySpinner.setAdapter(adapter);
        
        // Disable spinner initially - it should only be enabled in edit mode
        taskPrioritySpinner.setEnabled(false);
        taskPrioritySpinner.setClickable(false);

        taskDatabase = new TaskDatabaseHelper(this);

        // Get task ID from intent
        taskId = getIntent().getIntExtra("TASK_ID", -1);

        // Load task details from database
        loadTaskDetails(taskId);
        
        // Animate UI elements
        animateUIElements();

        // Set button actions
        btnReturn.setOnClickListener(v -> finish());
        btnEdit.setOnClickListener(view -> toggleEditMode());
        btnDone.setOnClickListener(view -> saveChanges());
        btnDelete.setOnClickListener(view -> deleteTask());
    }
    
    private void animateUIElements() {
        // Staggered animation for cards
        CardView[] cards = new CardView[] {
            actionButtonsCard, descriptionCard, dueDateCard, priorityCard
        };
        
        for (int i = 0; i < cards.length; i++) {
            CardView card = cards[i];
            card.setAlpha(0f);
            card.setTranslationY(100f);
            
            card.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setStartDelay(100 + (i * 100))
                .setInterpolator(new DecelerateInterpolator())
                .start();
        }
        
        // Animate FAB with a slight bounce
        btnDone.setScaleX(0f);
        btnDone.setScaleY(0f);
        new Handler().postDelayed(() -> {
            if (btnDone.getVisibility() == View.VISIBLE) {
                AnimatorSet set = new AnimatorSet();
                ObjectAnimator scaleX = ObjectAnimator.ofFloat(btnDone, "scaleX", 0f, 1.1f, 1f);
                ObjectAnimator scaleY = ObjectAnimator.ofFloat(btnDone, "scaleY", 0f, 1.1f, 1f);
                set.playTogether(scaleX, scaleY);
                set.setDuration(300);
                set.start();
            }
        }, 600);
    }

    private void loadTaskDetails(int taskId) {
        if (taskId != -1) {
            currentTask = taskDatabase.getTaskById(taskId);
            if (currentTask != null) {
                taskName.setText(currentTask.getName());
                taskDescription.setText(currentTask.getDescription());
                taskDueDate.setText(currentTask.getDate());
                
                // Update collapsing toolbar title
                collapsingToolbar.setTitle(currentTask.getName());
                
                // Set spinner to current priority
                String[] priorities = getResources().getStringArray(R.array.priority_array);
                for (int i = 0; i < priorities.length; i++) {
                    if (priorities[i].equals(currentTask.getPriority())) {
                        taskPrioritySpinner.setSelection(i);
                        break;
                    }
                }
            } else {
                Log.e("TaskDetailsActivity", "Task not found!");
                Snackbar.make(findViewById(android.R.id.content), 
                        "Task not found", Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    private void toggleEditMode() {
        isEditing = !isEditing;

        if (isEditing) {
            // Enable editing
            taskName.setEnabled(true);
            taskDescription.setEnabled(true);
            taskDueDate.setEnabled(true);
            taskPrioritySpinner.setEnabled(true);

            taskDueDate.setOnClickListener(view -> showDatePicker());

            // Change background color to indicate editing mode
            taskName.setBackgroundResource(R.drawable.edit_text_background);
            taskDescription.setBackgroundResource(R.drawable.edit_text_background);
            taskDueDate.setBackgroundResource(R.drawable.edit_text_background);
            taskPrioritySpinner.setBackgroundResource(R.drawable.spinner_background);

            // Show Done button with animation
            btnDone.setVisibility(View.VISIBLE);
            AnimatorSet set = new AnimatorSet();
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(btnDone, "scaleX", 0f, 1.1f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(btnDone, "scaleY", 0f, 1.1f, 1f);
            set.playTogether(scaleX, scaleY);
            set.setDuration(300);
            set.start();
            
            // Update button text
            btnEdit.setText("Cancel");
            
            // Show editing message
            Snackbar.make(findViewById(android.R.id.content), 
                    "Editing task...", Snackbar.LENGTH_SHORT).show();
        } else {
            disableEditing();
            
            // Update button text back
            btnEdit.setText("Edit");
        }
    }

    private void saveChanges() {
        if (currentTask != null) {
            // Get current values
            String newName = taskName.getText().toString();
            String newDescription = taskDescription.getText().toString();
            String newDueDate = taskDueDate.getText().toString();
            String newPriority = taskPrioritySpinner.getSelectedItem().toString();

            // Validate input
            if (newName.trim().isEmpty()) {
                Snackbar.make(findViewById(android.R.id.content), 
                        "Task name cannot be empty", Snackbar.LENGTH_SHORT).show();
                return;
            }
            
            // Validate that the due date is not in the past
            if (!isDateValid(newDueDate)) {
                Snackbar.make(findViewById(android.R.id.content), 
                        "Due date cannot be in the past", Snackbar.LENGTH_SHORT).show();
                return;
            }

            // Calculate new category
            String newCategory = calculateCategory(newDueDate, newPriority);

            // Update task object
            currentTask.setName(newName);
            currentTask.setDescription(newDescription);
            currentTask.setDueDate(newDueDate);
            currentTask.setPriority(newPriority);
            currentTask.setCategory(newCategory);

            // Update task in database
            boolean updateSuccess = taskDatabase.updateTask(currentTask);

            if (updateSuccess) {
                // Update UI
                collapsingToolbar.setTitle(newName);
                
                // Show success message
                Snackbar.make(findViewById(android.R.id.content), 
                        "Task updated successfully!", Snackbar.LENGTH_SHORT).show();
                
                taskViewModel.setTaskUpdated(true);
                
                // Disable editing mode
                disableEditing();
                btnEdit.setText("Edit");
                isEditing = false;
            } else {
                Snackbar.make(findViewById(android.R.id.content), 
                        "Failed to update task", Snackbar.LENGTH_SHORT).show();
            }
        }
    }
    
    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date date = sdf.parse(taskDueDate.getText().toString());
            calendar.setTime(date);
        } catch (ParseException ignored) {}

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d",
                            selectedDay, selectedMonth + 1, selectedYear);
                    taskDueDate.setText(selectedDate);
                },
                year, month, day);
        datePickerDialog.show();
    }

    private String calculateCategory(String dueDate, String priority) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date due = sdf.parse(dueDate);
            Date today = new Date();

            long daysRemaining = TimeUnit.DAYS.convert(due.getTime() - today.getTime(), TimeUnit.MILLISECONDS);

            if (priority.equals("HIGH") && daysRemaining <= 2) {
                return "A";
            } else if (priority.equals("HIGH")) {
                return "B";
            } else if (daysRemaining <= 2) {
                return "C";
            } else {
                return "D";
            }
        } catch (ParseException e) {
            return currentTask.getCategory(); // Keep current category if date parsing fails
        }
    }
    
    private void disableEditing() {
        // Disable editing
        taskName.setEnabled(false);
        taskDescription.setEnabled(false);
        taskDueDate.setEnabled(false);
        taskPrioritySpinner.setEnabled(false);
        taskDueDate.setOnClickListener(null);

        // Reset background
        taskName.setBackgroundColor(Color.TRANSPARENT);
        taskDescription.setBackgroundColor(Color.TRANSPARENT);
        taskDueDate.setBackgroundColor(Color.TRANSPARENT);
        taskPrioritySpinner.setBackgroundColor(Color.TRANSPARENT);

        // Hide Done button with animation
        if (btnDone.getVisibility() == View.VISIBLE) {
            btnDone.animate()
                    .scaleX(0f)
                    .scaleY(0f)
                    .setDuration(300)
                    .withEndAction(() -> btnDone.setVisibility(View.GONE))
                    .start();
        }

        // Reload original values if needed
        if (currentTask != null) {
            taskName.setText(currentTask.getName());
            taskDescription.setText(currentTask.getDescription());
            taskDueDate.setText(currentTask.getDate());
        }
    }

    private void deleteTask() {
        if (currentTask != null) {
            // Show confirmation Snackbar
            Snackbar.make(findViewById(android.R.id.content), 
                    "Delete this task?", Snackbar.LENGTH_LONG)
                    .setAction("DELETE", v -> {
                        // Delete task and return to main activity
                        taskDatabase.deleteTask(currentTask.getId());
                        taskViewModel.setTaskUpdated(true);
                        
                        Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show();
                        
                        Intent intent = new Intent(this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    })
                    .setActionTextColor(getResources().getColor(R.color.error_color))
                    .show();
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
}
