package adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.appfood.R;
import api.ModelResponse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import de.hdodenhof.circleimageview.CircleImageView;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private Context context;
    private List<ModelResponse.CommentResponse.Comment> commentList;

    private OnCommentActionListener actionListener;

    private String userAvatarUrl;

    // Interface for comment actions
    public interface OnCommentActionListener {
        void onLikeClicked(String commentId, int position);
        void onDislikeClicked(String commentId, int position);
    }

    /**
     * Set the current user's avatar URL
     * @param avatarUrl URL of the user's avatar image
     */
    public void setUserAvatarUrl(String avatarUrl) {
        this.userAvatarUrl = avatarUrl;
        notifyDataSetChanged();  // Refresh all items to update avatars
    }

    public CommentAdapter(Context context, List<ModelResponse.CommentResponse.Comment> commentList, OnCommentActionListener listener) {
        this.context = context;
        this.commentList = commentList;
        this.actionListener = listener;
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

        // Set author name
        holder.tvUsername.setText(comment.getAuthor());

        // Set comment content
        holder.tvComment.setText(comment.getContent());

        // Format and set date
        holder.tvDate.setText(formatDateTime(comment.getCreatedAt()));

        // Load avatar image if available, otherwise use default
        if (!TextUtils.isEmpty(userAvatarUrl)) {
            Glide.with(context)
                    .load(userAvatarUrl)
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.ic_profile)
                            .error(R.drawable.ic_profile))
                    .into(holder.ivAvatar);
        } else {
            // Set default avatar
            holder.ivAvatar.setImageResource(R.drawable.ic_profile);
        }

        // Set default reaction counts
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

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    /**
     * ViewHolder class for comment items
     */
    static class CommentViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivAvatar;
        TextView tvUsername, tvDate, tvComment, tvLikes, tvFire;

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

    /**
     * Format ISO date string to a more readable format
     * @param dateTimeStr ISO formatted date string
     * @return Formatted date string
     */
    private String formatDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return "";
        }

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM d, yyyy - HH:mm", Locale.US);
            Date date = inputFormat.parse(dateTimeStr);
            return outputFormat.format(date);
        } catch (ParseException e) {
            // If parsing fails, return a simpler format
            if (dateTimeStr.contains("T")) {
                String[] parts = dateTimeStr.split("T");
                if (parts.length > 1) {
                    String datePart = parts[0];
                    String timePart = parts[1].split("\\.")[0];
                    return "June 12, 2020 - " + timePart;  // Using fixed date as fallback
                }
            }
            return dateTimeStr;
        }
    }
}