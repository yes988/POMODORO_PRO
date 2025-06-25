package com.example.POMODORO_PRO.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import com.example.POMODORO_PRO.R;

public class NotificationHelper {
    private static final String CHANNEL_ID = "POMODORO_TIMER";
    private static final int NOTIFICATION_ID = 1;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001; // ✅ Define request code

    // ✅ Step 1: Create Notification Channel
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Pomodoro Timer";
            String description = "Notifies when a Pomodoro session changes.";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            // 🔹 Fix: Check if notificationManager is null before using it
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    // ✅ Step 2: Request Notification Permission (for Android 13+)
    public static void requestNotificationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission(activity)) {
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION);
            }
        }
    }

    @SuppressLint("MissingPermission")
    public static void sendNotification(Context context, String message, boolean isSuccess) {
        // ✅ Step 3: Check for notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission(context)) {
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(getNotificationIcon())
                .setContentTitle("Pomodoro Timer")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        // Add sound based on success/failure
        if (isSuccess) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setVibrate(new long[]{0, 300, 200, 300});
        } else {
            builder.setSound(Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.failure));
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    // Keep the original method for backward compatibility
    @SuppressLint("MissingPermission")
    public static void sendNotification(Context context, String message) {
        sendNotification(context, message, true); // Default to success notification
    }

    // Keep the original method for backward compatibility

    // ✅ Step 4: Helper function to check for POST_NOTIFICATIONS permission
    private static boolean hasNotificationPermission(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED;
    }

    // ✅ Step 5: Handle missing notification icon properly
    private static int getNotificationIcon() {
        return R.drawable.ic_timer; // Ensure this icon exists, or replace with a valid one
    }

    // ✅ Step 6: Handle permission request result in Activity (if needed)
    public static void onRequestPermissionsResult(int requestCode, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // ✅ Permission granted - You can now send notifications
            } else {
                // ❌ Permission denied - Handle accordingly (e.g., show a message)
            }
        }
    }
}
