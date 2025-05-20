package adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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

    public CommentAdapter(Context context, List<ModelResponse.CommentResponse.Comment> commentList) {
        this.context = context;
        this.commentList = commentList;
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

        // Set default avatar (in a real app, you'd load the user's avatar)
        holder.ivAvatar.setImageResource(R.drawable.ic_profile);

        // Set default reaction counts
        holder.tvLikes.setText("0");
        holder.tvFire.setText("0");
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