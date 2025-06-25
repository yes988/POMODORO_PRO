package com.example.POMODORO_PRO.ui.home;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.POMODORO_PRO.MainActivity;
import com.example.POMODORO_PRO.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class TimerActivity extends AppCompatActivity {
    private EditText editPomodoroTime, editShortBreak, editStudyHours, editStudyMinutes;
    private FloatingActionButton FAbtn_SaveSettings;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.timer_set);

        // Initialize views
        editPomodoroTime = findViewById(R.id.editPomodoroTime);
        editShortBreak = findViewById(R.id.editShortBreak);
        editStudyHours = findViewById(R.id.textCount);
        editStudyMinutes = findViewById(R.id.editStudyMinutes);
        FAbtn_SaveSettings = findViewById(R.id.FAbtn_SaveSettings);

        sharedPreferences = getSharedPreferences("PomodoroPrefs", MODE_PRIVATE);
        loadSettings();

        FAbtn_SaveSettings.setOnClickListener(v -> saveSettings());

        ImageButton btnBackToHome = findViewById(R.id.btnBackToHome);
        btnBackToHome.setOnClickListener(v -> {
            // Return to HomeFragment in MainActivity
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("LOAD_HOME_FRAGMENT", true);
            startActivity(intent);
            finish();
        });
    }

    private void loadSettings() {
        int pomodoroTime = sharedPreferences.getInt("pomodoroTime", 25); // Default 25 mins
        int shortBreak = sharedPreferences.getInt("shortBreak", 5); // Default 5 mins
        int totalStudyMinutes = sharedPreferences.getInt("studyTime", 120); // Default 2 hours (120 mins)

        // Convert total minutes to hours and minutes
        int studyHours = totalStudyMinutes / 60;
        int studyMinutes = totalStudyMinutes % 60;

        editPomodoroTime.setText(String.valueOf(pomodoroTime));
        editShortBreak.setText(String.valueOf(shortBreak));
        editStudyHours.setText(String.valueOf(studyHours));
        editStudyMinutes.setText(String.valueOf(studyMinutes));
    }

    private void saveSettings() {
        try {
            // Get values from input fields
            int pomodoroTime = Integer.parseInt(editPomodoroTime.getText().toString());
            int shortBreak = Integer.parseInt(editShortBreak.getText().toString());
            int studyHours = Integer.parseInt(editStudyHours.getText().toString());
            int studyMinutes = Integer.parseInt(editStudyMinutes.getText().toString());

            // Input validation
            if (pomodoroTime < 1 || shortBreak < 1) {
                Toast.makeText(this, "Pomodoro and break times must be at least 1 minute",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (studyHours < 0 || studyMinutes < 0 || studyMinutes > 59) {
                Toast.makeText(this, "Please enter valid time (0+ hours, 0-59 minutes)",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            int totalStudyMinutes = (studyHours * 60) + studyMinutes;
            if (totalStudyMinutes < pomodoroTime) {
                Toast.makeText(this, "Total study time should be longer than pomodoro time",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Save to SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("pomodoroTime", pomodoroTime);
            editor.putInt("shortBreak", shortBreak);
            editor.putInt("studyTime", totalStudyMinutes);
            editor.apply();

            // Return to MainActivity
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("LOAD_HOME_FRAGMENT", true);
            startActivity(intent);
            finish();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
        }
    }
}