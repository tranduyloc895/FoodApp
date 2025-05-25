package adapter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.appfood.R;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import api.ModelResponse;
import fragment.SavedRecipeFragment;

public class NotificationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_NOTIFICATION = 1;

    private List<Object> items = new ArrayList<>();
    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(ModelResponse.NotificationsResponse.Notification notification);
        void onNotificationDeleteClick(ModelResponse.NotificationsResponse.Notification notification, int position);
    }

    public NotificationAdapter(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    public void setNotifications(List<ModelResponse.NotificationsResponse.Notification> notifications, String filter) {
        items.clear();

        // Filter notifications based on the selected tab
        List<ModelResponse.NotificationsResponse.Notification> filteredList = new ArrayList<>();
        for (ModelResponse.NotificationsResponse.Notification notification : notifications) {
            if (filter.equals("All") ||
                    (filter.equals("Read") && notification.isRead()) ||
                    (filter.equals("Unread") && !notification.isRead())) {
                filteredList.add(notification);
            }
        }

        // Group notifications by date category
        Map<String, List<ModelResponse.NotificationsResponse.Notification>> groupedNotifications = new HashMap<>();

        for (ModelResponse.NotificationsResponse.Notification notification : filteredList) {
            String category = notification.getDateCategory();
            if (!groupedNotifications.containsKey(category)) {
                groupedNotifications.put(category, new ArrayList<>());
            }
            groupedNotifications.get(category).add(notification);
        }

        // Add headers and notifications to the items list
        List<String> categories = new ArrayList<>(groupedNotifications.keySet());
        // Sort categories - Today, Yesterday, then dates in descending order
        categories.sort((c1, c2) -> {
            if (c1.equals("Today")) return -1;
            if (c2.equals("Today")) return 1;
            if (c1.equals("Yesterday")) return -1;
            if (c2.equals("Yesterday")) return 1;
            return c2.compareTo(c1); // Descending date order
        });

        for (String category : categories) {
            items.add(category); // Add header
            items.addAll(groupedNotifications.get(category)); // Add notifications
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_date_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notification, parent, false);
            return new NotificationViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);

        if (holder instanceof HeaderViewHolder && item instanceof String) {
            ((HeaderViewHolder) holder).bind((String) item);
        } else if (holder instanceof NotificationViewHolder &&
                item instanceof ModelResponse.NotificationsResponse.Notification) {
            ((NotificationViewHolder) holder).bind(
                    (ModelResponse.NotificationsResponse.Notification) item, position);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? VIEW_TYPE_HEADER : VIEW_TYPE_NOTIFICATION;
    }

    public void removeNotification(int position) {
        if (position >= 0 && position < items.size()) {
            // Check if this is a notification and not a header
            if (items.get(position) instanceof ModelResponse.NotificationsResponse.Notification) {
                items.remove(position);
                notifyItemRemoved(position);

                // Check if we need to remove a header that has no more notifications
                checkAndRemoveEmptyHeaders();
            }
        }
    }

    private void checkAndRemoveEmptyHeaders() {
        int position = 0;
        while (position < items.size()) {
            if (items.get(position) instanceof String) {
                String header = (String) items.get(position);

                // Check if this header has any notifications following it
                boolean hasNotifications = false;
                int nextPos = position + 1;
                while (nextPos < items.size() && !(items.get(nextPos) instanceof String)) {
                    hasNotifications = true;
                    break;
                }

                // If no notifications, remove the header
                if (!hasNotifications) {
                    items.remove(position);
                    notifyItemRemoved(position);
                    // Don't increment position since we removed an item
                } else {
                    position++;
                }
            } else {
                position++;
            }
        }
    }

    class HeaderViewHolder extends RecyclerView.ViewHolder {
        private TextView tvDateHeader;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateHeader = itemView.findViewById(R.id.tvDateHeader);
        }

        void bind(String header) {
            tvDateHeader.setText(header);
        }
    }

    class NotificationViewHolder extends RecyclerView.ViewHolder {
        private TextView tvNotificationTitle;
        private TextView tvNotificationMessage;
        private TextView tvTime;
        private View ivUnread;
        private TextView tvDelete;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNotificationTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvNotificationMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            ivUnread = itemView.findViewById(R.id.ivUnread);
            tvDelete = itemView.findViewById(R.id.tvDelete);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION &&
                        items.get(position) instanceof ModelResponse.NotificationsResponse.Notification) {
                    ModelResponse.NotificationsResponse.Notification notification =
                            (ModelResponse.NotificationsResponse.Notification) items.get(position);
                    listener.onNotificationClick(notification);
                }
            });

            // Set up delete button click listener
            if (tvDelete != null) {
                tvDelete.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION &&
                            items.get(position) instanceof ModelResponse.NotificationsResponse.Notification) {
                        ModelResponse.NotificationsResponse.Notification notification =
                                (ModelResponse.NotificationsResponse.Notification) items.get(position);
                        listener.onNotificationDeleteClick(notification, position);
                    }
                });
            }
        }

        void bind(ModelResponse.NotificationsResponse.Notification notification, int position) {
            // Display notification type as title
            tvNotificationTitle.setText(getNotificationTitle(notification));

            // Display notification content
            tvNotificationMessage.setText(notification.getContent());

            // Display time
            tvTime.setText(notification.getFormattedTime());

            // Show/hide unread indicator
            ivUnread.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);
        }

        private String getNotificationTitle(ModelResponse.NotificationsResponse.Notification notification) {
            switch(notification.getType()) {
                case "COMMENT":
                    return "New Comment";
                case "RATING":
                    return "New Rating";
                case "LIKE":
                    return "New Like";
                case "FOLLOW":
                    return "New Follower";
                default:
                    return "Notification";
            }
        }
    }
}