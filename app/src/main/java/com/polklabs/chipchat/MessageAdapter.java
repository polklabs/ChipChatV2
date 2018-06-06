package com.polklabs.chipchat;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.polklabs.chipchat.backend.Message;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter {

    private static final int VIEW_TYPE_SERVER = 0;
    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;
    //private String Server;


    private List<Message> messageList;
    //private String me;
/*
    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView message, user, time;

        public MyViewHolder(View view) {
            super(view);
            message = view.findViewById(R.id.message);
            user = view.findViewById(R.id.user);
            time = view.findViewById(R.id.time);
        }
    }
*/

    public MessageAdapter(List<Message> messageList) {
        this.messageList = messageList;
        //me = user;
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    //returns view type to use depending on who sent message
    @Override
    public int getItemViewType(int position){
        Message message = (Message) messageList.get(position);
        //spagootle code

        //end of spagootle code
        Log.d("Roomy", message.getUser());
        Log.d("Roomy", String.valueOf( message.getUser().equals(messageList.get(0).getUser())));
        if(message.isSentByMe()){
            return VIEW_TYPE_MESSAGE_SENT;
        }
        else if(message.getUser().equals( messageList.get(0).getUser()))
        {
            return VIEW_TYPE_SERVER;
        }
        else{
            return VIEW_TYPE_MESSAGE_RECEIVED;
        }
    }

    //uses appropriate layout given view type
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        View view;

        if(viewType == VIEW_TYPE_MESSAGE_SENT)
        {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.message_list_row_2, parent, false);
            return new SentMessageHolder(view);
        }
        else if(viewType == VIEW_TYPE_MESSAGE_RECEIVED)
        {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.message_list_row, parent, false);
            return new ReceivedMessageHolder(view);
        }
        else if(viewType == VIEW_TYPE_SERVER)
        {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.server_message, parent, false);
            return new ServerMessageHolder(view);
        }

        return null;
    }



    /*
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.message_list_row, parent, false);

        return new MyViewHolder(itemView);
    }
    */


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Message movie = messageList.get(position);

        switch(holder.getItemViewType())
        {
            case VIEW_TYPE_MESSAGE_SENT:
                ((SentMessageHolder) holder).bind(movie);
                break;

            case VIEW_TYPE_MESSAGE_RECEIVED:
                ((ReceivedMessageHolder) holder).bind(movie);
                break;
            case VIEW_TYPE_SERVER:
                ((ServerMessageHolder) holder).bind(movie);
        }

    }




    private class ReceivedMessageHolder extends RecyclerView.ViewHolder{
        TextView message, user, time;

        ReceivedMessageHolder(View view){
            super(view);
            message = view.findViewById(R.id.message);
            user = view.findViewById(R.id.user);
            time = view.findViewById(R.id.time);
        }

        void bind(Message boundedMessage)
        {
            message.setText(boundedMessage.getMessage());
            time.setText(boundedMessage.getTime());
            user.setText(boundedMessage.getUser());
        }
    }

    private class SentMessageHolder extends RecyclerView.ViewHolder{
        TextView message, time, user;

        SentMessageHolder(View view)
        {
            super(view);
            message = view.findViewById(R.id.message);
            time = view.findViewById(R.id.time);
            user = view.findViewById(R.id.user);
        }

        void bind(Message boundedMessage)
        {
            message.setText(boundedMessage.getMessage());
            time.setText(boundedMessage.getTime());
            user.setText("Me ");
        }
    }

    private class ServerMessageHolder extends RecyclerView.ViewHolder{
        TextView message, time;

        ServerMessageHolder(View view)
        {
            super(view);
            message = view.findViewById(R.id.message);
            time = view.findViewById(R.id.time);
        }

        void bind(Message boundedMessage)
        {
            message.setText(boundedMessage.getMessage());
            time.setText(boundedMessage.getTime());
        }
    }
}