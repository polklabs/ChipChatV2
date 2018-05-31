package com.polklabs.roomy;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.polklabs.roomy.backend.Message;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MyViewHolder> {

    private List<Message> messageList;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView message, user, time;

        public MyViewHolder(View view) {
            super(view);
            message = view.findViewById(R.id.message);
            user = view.findViewById(R.id.user);
            time = view.findViewById(R.id.time);
        }
    }


    public MessageAdapter(List<Message> messageList) {
        this.messageList = messageList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.message_list_row, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Message movie = messageList.get(position);
        holder.message.setText(movie.getMessage());
        holder.user.setText(movie.getUser());
        holder.time.setText(movie.getTime());
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }
}