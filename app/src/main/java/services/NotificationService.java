package services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.appfood.R;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import api.ApiService;
import api.ModelResponse;
import api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationService {
    private static final String TAG = "NotificationService";
    private static final String CHANNEL_ID = "new_notifications_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final long DEFAULT_REFRESH_INTERVAL = 30000; // 30 seconds default

    private static NotificationService instance;
    private final Context context;
    private final Set<String> knownNotificationIds = new HashSet<>();

    // For continuous checking
    private final Handler notificationHandler = new Handler(Looper.getMainLooper());
    private Runnable notificationCheckRunnable;
    private boolean isChecking = false;
    private long refreshInterval = DEFAULT_REFRESH_INTERVAL;
    private String currentToken = null;

    private NotificationService(Context context) {
        this.context = context.getApplicationContext();
        createNotificationChannel();
    }

    public static synchronized NotificationService getInstance(Context context) {
        if (instance == null) {
            instance = new NotificationService(context);
        }
        return instance;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "New Notifications";
            String description = "Notifications about new updates in your feed";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Start continuous checking for notifications
     * @param token The authentication token
     */
    public void startContinuousChecking(String token) {
        if (token == null || token.isEmpty()) {
            Log.d(TAG, "Cannot start continuous checking: No token available");
            return;
        }

        this.currentToken = token;

        // If already checking, stop previous checker
        if (isChecking) {
            stopContinuousChecking();
        }

        isChecking = true;

        // Initial check immediately
        checkForNotifications(token);

        // Setup recurring check
        notificationCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (isChecking && currentToken != null) {
                    Log.d(TAG, "Running periodic notification check");
                    checkForNotifications(currentToken);

                    // Schedule next check
                    notificationHandler.postDelayed(this, refreshInterval);
                }
            }
        };

        // Start the periodic checking
        notificationHandler.postDelayed(notificationCheckRunnable, refreshInterval);
        Log.d(TAG, "Started continuous notification checking with interval: " + refreshInterval + "ms");
    }

    /**
     * Stop continuous checking for notifications
     */
    public void stopContinuousChecking() {
        if (notificationCheckRunnable != null) {
            notificationHandler.removeCallbacks(notificationCheckRunnable);
        }
        isChecking = false;
        Log.d(TAG, "Stopped continuous notification checking");
    }

    /**
     * Set the refresh interval for continuous checking
     * @param milliseconds Time in milliseconds between checks
     */
    public void setRefreshInterval(long milliseconds) {
        this.refreshInterval = milliseconds;

        // If currently checking, restart with new interval
        if (isChecking && currentToken != null) {
            stopContinuousChecking();
            startContinuousChecking(currentToken);
        }
    }

    /**
     * Perform a single check for notifications
     */
    public void checkForNotifications(String token) {
        if (token == null || token.isEmpty()) {
            Log.d(TAG, "No token available for notification check");
            return;
        }

        ApiService apiService = RetrofitClient.getApiService();
        apiService.getNotifications("Bearer " + token)
                .enqueue(new Callback<ModelResponse.NotificationsResponse>() {
                    @Override
                    public void onResponse(Call<ModelResponse.NotificationsResponse> call,
                                           Response<ModelResponse.NotificationsResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ModelResponse.NotificationsResponse notificationsResponse = response.body();

                            if (notificationsResponse.getStatus().equals("success")) {
                                List<ModelResponse.NotificationsResponse.Notification> notifications =
                                        notificationsResponse.getData().getNotifications();

                                checkForNewNotificationsAndNotify(notifications, token);
                            }
                        } else {
                            Log.e(TAG, "API Error: " +
                                    (response.errorBody() != null ? response.errorBody().toString() : "Unknown error"));
                        }
                    }

                    @Override
                    public void onFailure(Call<ModelResponse.NotificationsResponse> call, Throwable t) {
                        Log.e(TAG, "Failed to check notifications", t);
                    }
                });
    }

    private void checkForNewNotificationsAndNotify(
            List<ModelResponse.NotificationsResponse.Notification> notifications, String token) {
        if (notifications == null || notifications.isEmpty()) {
            return;
        }

        boolean hasNewNotifications = false;
        int newNotificationCount = 0;

        // Find notifications that we haven't seen before
        for (ModelResponse.NotificationsResponse.Notification notification : notifications) {
            if (!knownNotificationIds.contains(notification.getId())) {
                if (!notification.isRead()) {  // Only consider unread ones as "new"
                    hasNewNotifications = true;
                    newNotificationCount++;
                }

                // Add to our known list so we don't notify for it again
                knownNotificationIds.add(notification.getId());
            }
        }

        // If we found new notifications, show a device notification
        if (hasNewNotifications) {
            showNewNotification(newNotificationCount);
        }
    }

    private void showNewNotification(int newCount) {
        // Create an intent that opens the app
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (intent == null) {
            return;
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // Create a PendingIntent
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);

        // Build notification
        String contentText = newCount == 1
                ? "You have 1 new notification"
                : "You have " + newCount + " new notifications";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("New Notifications")
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Show notification
        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Notification permission missing", e);
        }
    }

    public void markNotificationAsKnown(String notificationId) {
        knownNotificationIds.add(notificationId);
    }

    public Set<String> getKnownNotificationIds() {
        return knownNotificationIds;
    }

    public boolean isNotificationKnown(String notificationId) {
        return knownNotificationIds.contains(notificationId);
    }

    /**
     * Clear all known notifications (useful for logout)
     */
    public void clearKnownNotifications() {
        knownNotificationIds.clear();
    }
}