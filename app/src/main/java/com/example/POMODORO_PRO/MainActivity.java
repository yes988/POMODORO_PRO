package com.example.POMODORO_PRO;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.example.POMODORO_PRO.databinding.ActivityMainBinding;
import com.example.POMODORO_PRO.ui.NotificationHelper;
import com.example.POMODORO_PRO.ui.home.HomeFragment;
import com.example.POMODORO_PRO.ui.task.TaskViewModel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private TaskViewModel taskViewModel;
    private TaskDatabaseHelper taskDatabaseHelper;

    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    NotificationHelper.sendNotification(this, "Congrats! You have completed your study session!");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            NotificationHelper.createNotificationChannel(this);
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            // Initialize ViewModel first
            taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);

            taskDatabaseHelper = new TaskDatabaseHelper(this);

            // Clean up completed tasks from previous days
            cleanupCompletedTasks();

            // Then setup navigation
            setupNavigation();

        } catch (Exception e) {
            // Handle initialization errors gracefully
            Toast.makeText(this, "App initialization failed", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setupNavigation() {
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_dashboard,
                R.id.navigation_home,
                R.id.navigation_calender,
                R.id.navigation_progress,
                R.id.navigation_report)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }

    private void loadHomeFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment_activity_main, new HomeFragment())
                .commit();
    }
    // In MainActivity.java
    public TaskViewModel getViewModel() {
        return new ViewModelProvider(this).get(TaskViewModel.class);
    }
    private void checkFragmentStatus() {
        Fragment myFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_main);
        if (myFragment != null && myFragment.isAdded()) {
            // The fragment is attached, you can safely interact with it
        }
    }

    /**
     * Cleans up completed tasks from previous days
     */
    private void cleanupCompletedTasks() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            int tasksRemoved = taskDatabaseHelper.purgeCompletedTasksFromPreviousDays();
            if (tasksRemoved > 0) {
                runOnUiThread(() -> {
                    Toast.makeText(this, 
                        tasksRemoved + " completed tasks removed", 
                        Toast.LENGTH_SHORT).show();
                });
            }
        });
        executor.shutdown();
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (taskDatabaseHelper != null) {
            taskDatabaseHelper.close();
        }
    }


}
