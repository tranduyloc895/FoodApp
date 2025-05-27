package adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appfood.ChatMessage;
import com.example.appfood.R;

import java.util.ArrayList;
import java.util.List;

import api.ModelResponse;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "MessageAdapter";
    private static final int TYPE_USER = 0;
    private static final int TYPE_BOT = 1;
    private static final int TYPE_BOT_WITH_RECIPES = 2;

    private List<ChatMessage> messages = new ArrayList<>();
    private Context context;
    private OnRecipeClickListener recipeClickListener;

    public interface OnRecipeClickListener {
        void onRecipeClick(String recipeId);
    }

    public MessageAdapter(Context context, OnRecipeClickListener listener) {
        this.context = context;
        this.recipeClickListener = listener;
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public List<ChatMessage> getMessages() {
        return new ArrayList<>(messages);
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = new ArrayList<>(messages);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        if (message.getType() == ChatMessage.TYPE_USER) {
            return TYPE_USER;
        } else if (message.hasRecipes()) {
            return TYPE_BOT_WITH_RECIPES;
        } else {
            return TYPE_BOT;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == TYPE_USER) {
            View view = inflater.inflate(R.layout.item_message_user, parent, false);
            return new UserMessageViewHolder(view);
        } else if (viewType == TYPE_BOT_WITH_RECIPES) {
            View view = inflater.inflate(R.layout.item_message_bot_with_recipes, parent, false);
            return new BotRecipeMessageViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_message_bot, parent, false);
            return new BotMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);

        if (holder instanceof UserMessageViewHolder) {
            UserMessageViewHolder userHolder = (UserMessageViewHolder) holder;
            userHolder.messageTextView.setText(message.getMessage());
            userHolder.timeTextView.setText(message.getFormattedTime());
        } else if (holder instanceof BotRecipeMessageViewHolder) {
            BotRecipeMessageViewHolder recipeHolder = (BotRecipeMessageViewHolder) holder;
            recipeHolder.messageTextView.setText(message.getMessage());
            recipeHolder.timeTextView.setText(message.getFormattedTime());

            // Set up recipe recycler view
            LinearLayoutManager layoutManager = new LinearLayoutManager(
                    context, LinearLayoutManager.HORIZONTAL, false);
            recipeHolder.recipeRecyclerView.setLayoutManager(layoutManager);

            RecipeChatCardAdapter adapter = new RecipeChatCardAdapter(
                    context, message.getRecipes(), recipeId -> {
                if (recipeClickListener != null) {
                    recipeClickListener.onRecipeClick(recipeId);
                }
            });

            recipeHolder.recipeRecyclerView.setAdapter(adapter);
        } else if (holder instanceof BotMessageViewHolder) {
            BotMessageViewHolder botHolder = (BotMessageViewHolder) holder;
            botHolder.messageTextView.setText(message.getMessage());
            botHolder.timeTextView.setText(message.getFormattedTime());
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        TextView timeTextView;

        UserMessageViewHolder(View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
        }
    }

    static class BotMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        TextView timeTextView;

        BotMessageViewHolder(View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
        }
    }

    static class BotRecipeMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        TextView timeTextView;
        RecyclerView recipeRecyclerView;

        BotRecipeMessageViewHolder(View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
            recipeRecyclerView = itemView.findViewById(R.id.recipeRecyclerView);
        }
    }
}