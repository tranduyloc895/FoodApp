package adapter;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.appfood.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import api.ApiService;
import api.ModelResponse;
import api.RetrofitClient;
import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Adapter for displaying comments in a RecyclerView
 */
public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {
    private static final String TAG = "CommentAdapter";
    private static final String DATE_FORMAT_INPUT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final String DATE_FORMAT_OUTPUT = "MMMM d, yyyy - HH:mm";
    private static final String DEFAULT_DATE = "June 12, 2020 - ";

    private final Context context;
    private final List<ModelResponse.CommentResponse.Comment> commentList;
    private final OnCommentActionListener actionListener;

    private String userAvatarUrl;
    private String token;

    // Cache for user data to avoid repeated API calls
    private final Map<String, String> userNameCache = new HashMap<>();
    private final Map<String, String> userAvatarCache = new HashMap<>();

    /**
     * Interface for comment action callbacks
     */
    public interface OnCommentActionListener {
        void onLikeClicked(String commentId, int position);
        void onDislikeClicked(String commentId, int position);
    }

    /**
     * Constructor for the adapter
     *
     * @param context The context
     * @param commentList List of comments to display
     * @param listener Listener for comment actions
     */
    public CommentAdapter(Context context, List<ModelResponse.CommentResponse.Comment> commentList,
                          OnCommentActionListener listener) {
        this.context = context;
        this.commentList = commentList;
        this.actionListener = listener;
    }

    /**
     * Set the current user's avatar URL
     *
     * @param avatarUrl URL of the user's avatar image
     */
    public void setUserAvatarUrl(String avatarUrl) {
        this.userAvatarUrl = avatarUrl;
        notifyDataSetChanged();  // Refresh all items to update avatars
    }

    /**
     * Set the authentication token for API calls
     *
     * @param token Authentication token
     */
    public void setToken(String token) {
        this.token = token;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        ModelResponse.CommentResponse.Comment comment = commentList.get(position);
        String authorId = comment.getAuthor_id();

        bindUserData(holder, authorId, position);
        bindCommentData(holder, comment, position);
    }

    /**
     * Bind user data (name and avatar) to the view holder
     */
    private void bindUserData(CommentViewHolder holder, String authorId, int position) {
        // Check if we already have author's name in cache
        if (userNameCache.containsKey(authorId)) {
            holder.tvUsername.setText(userNameCache.get(authorId));

            // Load author avatar if available
            String authorAvatar = userAvatarCache.get(authorId);
            if (!TextUtils.isEmpty(authorAvatar)) {
                loadAvatar(holder.ivAvatar, authorAvatar);
            } else {
                holder.ivAvatar.setImageResource(R.drawable.ic_profile);
            }
        } else {
            // Set temporary text while loading
            holder.tvUsername.setText("Loading...");
            holder.ivAvatar.setImageResource(R.drawable.ic_profile);

            // Fetch author details from API
            if (token != null) {
                fetchUserDetails(authorId, holder, position);
            } else {
                // Fallback if token is not available
                holder.tvUsername.setText(authorId);
            }
        }
    }

    /**
     * Bind comment data to the view holder
     */
    private void bindCommentData(CommentViewHolder holder, ModelResponse.CommentResponse.Comment comment, int position) {
        // Set comment content
        holder.tvComment.setText(comment.getContent());

        // Format and set date
        holder.tvDate.setText(formatDateTime(comment.getCreatedAt()));

        // Set reaction counts
        holder.tvLikes.setText(String.valueOf(comment.getLikes()));
        holder.tvFire.setText(String.valueOf(comment.getDislikes()));

        // Set click listeners for like and dislike
        holder.tvLikes.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onLikeClicked(comment.getId(), position);
            }
        });

        holder.tvFire.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onDislikeClicked(comment.getId(), position);
            }
        });
    }

    /**
     * Fetch user details using the API
     *
     * @param userId User ID to fetch
     * @param holder ViewHolder to update
     * @param position Position in the RecyclerView
     */
    private void fetchUserDetails(String userId, CommentViewHolder holder, int position) {
        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.UserResponse> call = apiService.getUserById("Bearer " + token, userId);

        call.enqueue(new Callback<ModelResponse.UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.UserResponse> call,
                                   @NonNull Response<ModelResponse.UserResponse> response) {
                if (response.isSuccessful() && response.body() != null &&
                        response.body().getData() != null &&
                        response.body().getData().getUser() != null) {

                    // Get user data
                    ModelResponse.UserResponse.User user = response.body().getData().getUser();
                    String userName = user.getName();
                    String userAvatar = user.getUrlAvatar();

                    // Store in cache
                    userNameCache.put(userId, userName);
                    if (userAvatar != null) {
                        userAvatarCache.put(userId, userAvatar);
                    }

                    // Update UI if the view holder is still visible
                    if (holder.getAdapterPosition() == position) {
                        holder.tvUsername.setText(userName);
                        if (userAvatar != null) {
                            loadAvatar(holder.ivAvatar, userAvatar);
                        }
                    }
                } else {
                    // Fallback on error
                    Log.e(TAG, "Failed to get user details: " + response.code());
                    userNameCache.put(userId, userId); // Use ID as fallback

                    // Update UI with fallback
                    if (holder.getAdapterPosition() == position) {
                        holder.tvUsername.setText(userId);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.UserResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "API call failed: " + t.getMessage());
                userNameCache.put(userId, userId); // Use ID as fallback

                // Update UI with fallback
                if (holder.getAdapterPosition() == position) {
                    holder.tvUsername.setText(userId);
                }
            }
        });
    }

    /**
     * Load avatar image with Glide
     */
    private void loadAvatar(CircleImageView imageView, String imageUrl) {
        Glide.with(context)
                .load(imageUrl)
                .apply(new RequestOptions()
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile))
                .into(imageView);
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    /**
     * Format ISO date string to a more readable format
     *
     * @param dateTimeStr ISO formatted date string
     * @return Formatted date string
     */
    private String formatDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return "";
        }

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat(DATE_FORMAT_INPUT, Locale.US);
            inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            SimpleDateFormat outputFormat = new SimpleDateFormat(DATE_FORMAT_OUTPUT, Locale.US);
            Date date = inputFormat.parse(dateTimeStr);
            return date != null ? outputFormat.format(date) : "";
        } catch (ParseException e) {
            // If parsing fails, return a simpler format
            if (dateTimeStr.contains("T")) {
                String[] parts = dateTimeStr.split("T");
                if (parts.length > 1) {
                    String timePart = parts[1].split("\\.")[0];
                    return DEFAULT_DATE + timePart;
                }
            }
            return dateTimeStr;
        }
    }

    /**
     * ViewHolder class for comment items
     */
    static class CommentViewHolder extends RecyclerView.ViewHolder {
        final CircleImageView ivAvatar;
        final TextView tvUsername, tvDate, tvComment, tvLikes, tvFire;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvComment = itemView.findViewById(R.id.tvComment);
            tvLikes = itemView.findViewById(R.id.tvLikes);
            tvFire = itemView.findViewById(R.id.tvFire);
        }
    }
}