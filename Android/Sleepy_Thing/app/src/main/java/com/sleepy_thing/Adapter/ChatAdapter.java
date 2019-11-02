package com.sleepy_thing.Adapter;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sleepy_thing.Base.Chat;
import com.sleepy_thing.R;

import java.util.List;

import static com.sleepy_thing.MainActivity.Baby_self;
import static com.sleepy_thing.MainActivity.Huyou_dev;

public class ChatAdapter extends RecyclerView.Adapter {

    private static final String TAG = "ChatAdapter";

    public static final int SELF_CHAT = 0;
    public static final int FRIEND_CHAT = 1;

    private Context context;

    private List<Chat> list;

    public ChatAdapter(Context context, List<Chat> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        Log.i("CHAT","list len = "+list.size());
        if (viewType==SELF_CHAT) {
            return new RightHolder(LayoutInflater.from(context).inflate(R.layout.adapter_chat_right, viewGroup, false));
        } else {
            return new LeftHolder(LayoutInflater.from(context).inflate(R.layout.adapter_chat_left, viewGroup, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        if (viewHolder instanceof RightHolder) {
            ((RightHolder) viewHolder).tvText.setText(list.get(i).getText());
        } else {
            ((LeftHolder) viewHolder).tvText.setText(list.get(i).getText());
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public int getItemViewType(int position)
    {
        if (list.get(position).getUid() == Baby_self.getUid()) {
            return SELF_CHAT;
        } else {
            return FRIEND_CHAT;
        }
    }

    class LeftHolder extends RecyclerView.ViewHolder {

        TextView tvText;
        ImageView ivLeft;

        public LeftHolder(@NonNull View itemView) {
            super(itemView);
            tvText = itemView.findViewById(R.id.tvLeftChatText);
            ivLeft = itemView.findViewById(R.id.ivLeft);
            ivLeft.setImageBitmap(BitmapFactory.decodeFile(Huyou_dev.getUheadPath()));
        }
    }

    class RightHolder extends RecyclerView.ViewHolder {

        TextView tvText;
        ImageView ivRight;

        public RightHolder(@NonNull View itemView) {
            super(itemView);
            tvText = itemView.findViewById(R.id.tvRightChatText);
            ivRight = itemView.findViewById(R.id.ivRight);
            ivRight.setImageBitmap(BitmapFactory.decodeFile(Baby_self.getUheadPath()));
        }
    }

}
