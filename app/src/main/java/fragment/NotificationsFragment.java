package fragment;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.appfood.MainRecipe;
import com.example.appfood.R;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import adapter.NotificationAdapter;

import api.ModelResponse;
import api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import api.ApiService;

import android.os.Handler;
import android.os.Looper;

public class NotificationsFragment extends Fragment implements NotificationAdapter.OnNotificationClickListener {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private TabLayout tabLayout;
    private SwipeRefreshLayout swipeRefresh;
    private List<ModelResponse.NotificationsResponse.Notification> allNotifications = new ArrayList<>();
    private Handler notificationHandler = new Handler(Looper.getMainLooper());
    private Runnable notificationRunnable;
    private boolean isAutoRefreshEnabled = true;
    private long refreshInterval = 10000; // 10 seconds refresh interval

    // Notification channel ID
    private static final String CHANNEL_ID = "new_notifications_channel";
    private static final int NOTIFICATION_ID = 1001;

    // Store notification IDs to track new ones
    private Set<String> knownNotificationIds = new HashSet<>();

    String token;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get token from arguments first
        if (getArguments() != null) {
            token = getArguments().getString("token");
        }

        // Create notification channel for Android 8.0+
        createNotificationChannel();
    }

    /**
     * Creates a notification channel for Android 8.0+
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "New Notifications";
            String description = "Notifications about new updates in your feed";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            // Register the channel with the system
            NotificationManager notificationManager = requireActivity().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewNotifications);
        tabLayout = view.findViewById(R.id.tabLayout);

        // Setup SwipeRefreshLayout properly
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        if (swipeRefresh == null) {
            // If not found, create a new one and wrap the recyclerView
            ViewGroup parent = (ViewGroup) recyclerView.getParent();
            int index = parent.indexOfChild(recyclerView);

            // Remove recyclerView
            parent.removeView(recyclerView);

            // Create and add swipeRefresh
            swipeRefresh = new SwipeRefreshLayout(requireContext());
            parent.addView(swipeRefresh, index, recyclerView.getLayoutParams());

            // Add recyclerView to swipeRefresh
            swipeRefresh.addView(recyclerView);
        }
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new NotificationAdapter(this);
        recyclerView.setAdapter(adapter);

        // Setup tabs
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterNotifications(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Not needed
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Not needed
            }
        });

        // Setup swipe refresh
        swipeRefresh.setOnRefreshListener(this::fetchNotifications);

        // Initial data load
        fetchNotifications();
    }

    @Override
    public void onResume() {
        super.onResume();
        startAutoRefresh();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAutoRefresh();
    }

    private void startAutoRefresh() {
        if (isAutoRefreshEnabled) {
            notificationRunnable = new Runnable() {
                @Override
                public void run() {
                    // Only fetch if fragment is still attached to prevent crashes
                    if (isAdded() && isVisible()) {
                        refreshNotificationsInBackground();
                    }
                    // Re-run this after delay
                    notificationHandler.postDelayed(this, refreshInterval);
                }
            };
            notificationHandler.postDelayed(notificationRunnable, refreshInterval);
        }
    }

    private void stopAutoRefresh() {
        if (notificationRunnable != null) {
            notificationHandler.removeCallbacks(notificationRunnable);
        }
    }

    // This method refreshes data without showing the loading indicator
    private void refreshNotificationsInBackground() {
        if (token == null || token.isEmpty()) {
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
                                List<ModelResponse.NotificationsResponse.Notification> newNotifications =
                                        notificationsResponse.getData().getNotifications();

                                // Check for new notifications and send device notification if needed
                                checkForNewNotificationsAndNotify(newNotifications);

                                // Check if notifications have changed
                                if (notificationsHaveChanged(allNotifications, newNotifications)) {
                                    Log.d("NotificationsFragment", "Notifications updated in background");
                                    allNotifications = newNotifications;
                                    filterNotifications(tabLayout.getSelectedTabPosition());
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ModelResponse.NotificationsResponse> call, Throwable t) {
                        // Failed silently in background, no need to show error to user
                        Log.e("NotificationsFragment", "Background refresh failed", t);
                    }
                });
    }

    /**
     * Checks for new notifications and sends a device notification if new ones are found
     */
    private void checkForNewNotificationsAndNotify(List<ModelResponse.NotificationsResponse.Notification> newNotifications) {
        if (newNotifications == null || newNotifications.isEmpty()) {
            return;
        }

        boolean hasNewNotifications = false;
        int newNotificationCount = 0;

        // Find notifications that we haven't seen before
        for (ModelResponse.NotificationsResponse.Notification notification : newNotifications) {
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
        if (hasNewNotifications && isAdded()) {
            showNewNotification(newNotificationCount);
        }
    }

    /**
     * Shows a simple notification with a static message
     */
    private void showNewNotification(int newCount) {
        // Create an intent that opens the app
        Intent intent = requireActivity().getPackageManager()
                .getLaunchIntentForPackage(requireActivity().getPackageName());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // Create a PendingIntent that will launch the app when the notification is tapped
        PendingIntent pendingIntent = PendingIntent.getActivity(requireContext(), 0, intent,
                PendingIntent.FLAG_IMMUTABLE);

        // Create the notification with a simple static message
        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)  // Make sure this icon exists in your drawable folder
                .setContentTitle("New Notifications")
                .setContentText("You have new notifications!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);  // Removes the notification when tapped

        // Show the notification
        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(requireContext());
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException e) {
            Log.e("NotificationsFragment", "Notification permission missing", e);
        }
    }

    // Helper method to detect changes in notifications
    private boolean notificationsHaveChanged(
            List<ModelResponse.NotificationsResponse.Notification> oldList,
            List<ModelResponse.NotificationsResponse.Notification> newList) {

        if (oldList.size() != newList.size()) {
            return true;
        }

        // Compare notifications for changes
        for (int i = 0; i < oldList.size(); i++) {
            ModelResponse.NotificationsResponse.Notification oldNotif = oldList.get(i);
            ModelResponse.NotificationsResponse.Notification newNotif = newList.get(i);

            if (!oldNotif.getId().equals(newNotif.getId()) ||
                    oldNotif.isRead() != newNotif.isRead()) {
                return true;
            }
        }

        return false;
    }

    private void fetchNotifications() {
        // Show loading indicator
        swipeRefresh.setRefreshing(true);

        if (token == null || token.isEmpty()) {
            Toast.makeText(requireContext(), "Please log in to view notifications", Toast.LENGTH_SHORT).show();
            swipeRefresh.setRefreshing(false);
            return;
        }

        ApiService apiService = RetrofitClient.getApiService();

        // Make API call
        apiService.getNotifications("Bearer " + token)
                .enqueue(new Callback<ModelResponse.NotificationsResponse>() {
                    @Override
                    public void onResponse(Call<ModelResponse.NotificationsResponse> call,
                                           Response<ModelResponse.NotificationsResponse> response) {
                        swipeRefresh.setRefreshing(false);

                        if (response.isSuccessful() && response.body() != null) {
                            ModelResponse.NotificationsResponse notificationsResponse = response.body();

                            if (notificationsResponse.getStatus().equals("success")) {
                                List<ModelResponse.NotificationsResponse.Notification> newNotifications =
                                        notificationsResponse.getData().getNotifications();

                                // Initial load - record IDs but don't notify
                                if (allNotifications.isEmpty()) {
                                    for (ModelResponse.NotificationsResponse.Notification notification : newNotifications) {
                                        knownNotificationIds.add(notification.getId());
                                    }
                                } else {
                                    // Not initial load - check for new notifications
                                    checkForNewNotificationsAndNotify(newNotifications);
                                }

                                // Save all notifications
                                allNotifications = newNotifications;

                                // Update UI based on current tab
                                filterNotifications(tabLayout.getSelectedTabPosition());
                            } else {
                                Toast.makeText(requireContext(), "Failed to load notifications",
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(requireContext(), "Failed to load notifications",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ModelResponse.NotificationsResponse> call, Throwable t) {
                        swipeRefresh.setRefreshing(false);
                        Toast.makeText(requireContext(), "Network error: " + t.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void filterNotifications(int tabPosition) {
        String filter;
        switch (tabPosition) {
            case 1:
                filter = "Read";
                break;
            case 2:
                filter = "Unread";
                break;
            default:
                filter = "All";
                break;
        }

        adapter.setNotifications(allNotifications, filter);
    }

    @Override
    public void onNotificationClick(ModelResponse.NotificationsResponse.Notification notification) {
        // Mark notification as read (if not already)
        if (!notification.isRead()) {
            markNotificationAsRead(notification.getId());
        }

        // Handle navigation based on notification type and reference
        if (notification.getReferenceType().equals("RECIPE") && notification.getReferenceId() != null) {
            navigateToRecipeDetail(notification.getReferenceId());
        }
    }

    @Override
    public void onNotificationDeleteClick(ModelResponse.NotificationsResponse.Notification notification, int position) {
        // Show confirmation dialog before deleting
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Notification")
                .setMessage("Are you sure you want to delete this notification?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteNotification(notification.getId(), position);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Delete a notification
     */
    private void deleteNotification(String notificationId, int position) {
        if (token == null || token.isEmpty()) {
            Toast.makeText(requireContext(), "Authentication required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Find the notification index in the allNotifications list
        int notificationIndex = -1;
        for (int i = 0; i < allNotifications.size(); i++) {
            if (allNotifications.get(i).getId().equals(notificationId)) {
                notificationIndex = i;
                break;
            }
        }

        if (notificationIndex == -1) {
            Log.e("NotificationsFragment", "Notification not found for deletion: " + notificationId);
            return;
        }

        // Store index for later use
        final int finalNotificationIndex = notificationIndex;

        // Optimistically update UI (remove from adapter first)
        adapter.removeNotification(position);

        // Also remove from our main list
        ModelResponse.NotificationsResponse.Notification removedNotification =
                allNotifications.remove(finalNotificationIndex);

        // Make API call to delete the notification
        ApiService apiService = RetrofitClient.getApiService();
        apiService.deleteNotification("Bearer " + token, notificationId)
                .enqueue(new Callback<ModelResponse.readNotificationResponse>() {
                    @Override
                    public void onResponse(Call<ModelResponse.readNotificationResponse> call,
                                           Response<ModelResponse.readNotificationResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            if (response.body().getStatus().equals("success")) {
                                Log.d("NotificationsFragment", "Notification deleted: " + notificationId);
                                // Already removed from UI optimistically, nothing more to do

                                // Show a brief success message
                                if (isAdded()) {
                                    Toast.makeText(requireContext(), "Notification deleted",
                                            Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Log.e("NotificationsFragment", "Failed to delete notification: " +
                                        (response.body().getMessage() != null ?
                                                response.body().getMessage() : "Unknown error"));

                                // Restore notification to our list since deletion failed
                                restoreNotification(removedNotification, finalNotificationIndex);

                                if (isAdded()) {
                                    Toast.makeText(requireContext(), "Failed to delete notification",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        } else {
                            Log.e("NotificationsFragment", "Error deleting notification: " +
                                    (response.errorBody() != null ?
                                            response.errorBody().toString() : "Unknown error"));

                            // Restore notification to our list since deletion failed
                            restoreNotification(removedNotification, finalNotificationIndex);

                            if (isAdded()) {
                                Toast.makeText(requireContext(), "Failed to delete notification",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ModelResponse.readNotificationResponse> call, Throwable t) {
                        Log.e("NotificationsFragment", "Network error deleting notification", t);

                        // Restore notification to our list since deletion failed
                        restoreNotification(removedNotification, finalNotificationIndex);

                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Network error: " + t.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /**
     * Restore a notification to the list if deletion fails
     */
    private void restoreNotification(ModelResponse.NotificationsResponse.Notification notification, int index) {
        if (notification == null) return;

        // Add back to our main list
        if (index >= 0 && index <= allNotifications.size()) {
            allNotifications.add(index, notification);
        } else {
            allNotifications.add(notification);
        }

        // Update UI
        filterNotifications(tabLayout.getSelectedTabPosition());
    }

    private void markNotificationAsRead(String notificationId) {
        if (token == null || token.isEmpty()) {
            Toast.makeText(requireContext(), "Authentication required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Find the notification index in the list
        int notificationIndex = -1;
        for (int i = 0; i < allNotifications.size(); i++) {
            if (allNotifications.get(i).getId().equals(notificationId)) {
                notificationIndex = i;
                break;
            }
        }

        if (notificationIndex == -1) {
            Log.e("NotificationsFragment", "Notification not found: " + notificationId);
            return;
        }

        // Store the index as a final variable so it can be used in inner class
        final int finalNotificationIndex = notificationIndex;

        // Optimistically update UI first for better user experience
        allNotifications.get(finalNotificationIndex).setRead(true);
        filterNotifications(tabLayout.getSelectedTabPosition());

        // Call API to mark notification as read
        ApiService apiService = RetrofitClient.getApiService();
        apiService.markNotificationAsRead("Bearer " + token, notificationId)
                .enqueue(new Callback<ModelResponse.readNotificationResponse>() {
                    @Override
                    public void onResponse(Call<ModelResponse.readNotificationResponse> call,
                                           Response<ModelResponse.readNotificationResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            if (response.body().getStatus().equals("success")) {
                                Log.d("NotificationsFragment", "Notification marked as read: " + notificationId);

                                // Refresh notifications to get updated data
                                refreshNotificationsInBackground();
                            } else {
                                Log.e("NotificationsFragment", "Failed to mark notification as read: " +
                                        (response.body().getMessage() != null ? response.body().getMessage() : "Unknown error"));

                                // Revert the optimistic update if the server request failed
                                if (finalNotificationIndex < allNotifications.size()) {
                                    allNotifications.get(finalNotificationIndex).setRead(false);
                                    filterNotifications(tabLayout.getSelectedTabPosition());
                                }
                            }
                        } else {
                            Log.e("NotificationsFragment", "Error marking notification as read: " +
                                    (response.errorBody() != null ? response.errorBody().toString() : "Unknown error"));

                            // Revert the optimistic update if the server request failed
                            if (finalNotificationIndex < allNotifications.size()) {
                                allNotifications.get(finalNotificationIndex).setRead(false);
                                filterNotifications(tabLayout.getSelectedTabPosition());
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ModelResponse.readNotificationResponse> call, Throwable t) {
                        Log.e("NotificationsFragment", "Network error marking notification as read", t);

                        // Revert the optimistic update if the network request failed
                        if (finalNotificationIndex < allNotifications.size()) {
                            allNotifications.get(finalNotificationIndex).setRead(false);
                            filterNotifications(tabLayout.getSelectedTabPosition());
                        }

                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Network error: " + t.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void navigateToRecipeDetail(String recipeId) {
        Intent intent = new Intent(requireContext(), MainRecipe.class);
        intent.putExtra("recipe_id", recipeId);
        intent.putExtra("token", token);
        startActivity(intent);
    }

    // Add this method to support checking for new notifications periodically
    public void checkForNewNotifications() {
        fetchNotifications();
    }

    // Method to enable/disable auto-refresh
    public void setAutoRefreshEnabled(boolean enabled) {
        this.isAutoRefreshEnabled = enabled;
        if (enabled) {
            startAutoRefresh();
        } else {
            stopAutoRefresh();
        }
    }

    // Method to manually set refresh interval
    public void setRefreshInterval(long milliseconds) {
        this.refreshInterval = milliseconds;
        // Restart the handler with new interval if it's running
        if (isAutoRefreshEnabled && notificationRunnable != null) {
            stopAutoRefresh();
            startAutoRefresh();
        }
    }

    // Clear notification tracking when fragment is destroyed
    @Override
    public void onDestroy() {
        super.onDestroy();
        knownNotificationIds.clear();
    }

    public static NotificationsFragment newInstance(String token) {
        NotificationsFragment fragment = new NotificationsFragment();
        Bundle args = new Bundle();
        args.putString("token", token);
        fragment.setArguments(args);
        Log.d("NotificationsFragment", "newInstance called with token: " +
                (token != null ? token.substring(0, Math.min(10, token.length())) + "..." : "null"));
        return fragment;
    }

    // Interface for token retrieval from activity
    public interface TokenProvider {
        String getToken();
    }
}