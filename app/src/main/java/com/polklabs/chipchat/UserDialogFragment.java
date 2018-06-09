package com.polklabs.chipchat;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import com.polklabs.chipchat.backend.ChatRoom;
import com.polklabs.chipchat.backend.Message;
import com.polklabs.chipchat.backend.client;

import java.util.ArrayList;
import java.util.List;

public class UserDialogFragment extends DialogFragment {

    private client          messageClient;
    private ChatRoom        chatRoom;
    private String          username;
    private Context         mContext;
    private List<Message>   messageList = new ArrayList<>();
    private MessageAdapter  mAdapter;

    public UserDialogFragment setMessageList(List<Message> messageList) {
        this.messageList = messageList;
        return this;
    }
    public UserDialogFragment setmAdapter(MessageAdapter mAdapter) {
        this.mAdapter = mAdapter;
        return this;
    }
    public UserDialogFragment setMessageClient(client messageClient) {
        this.messageClient = messageClient;
        return this;
    }
    public UserDialogFragment setChatRoom(ChatRoom chatRoom) {
        this.chatRoom = chatRoom;
        return this;
    }
    public UserDialogFragment setUsername(String username){
        this.username = username;
        return this;
    }
    public UserDialogFragment setmContext(Context context){
        this.mContext = context;
        return this;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(username).setItems(R.array.options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch(which){
                    case 0:
                        messageClient.commands.add("kick "+username);
                        break;
                    case 1:
                        MessageDialogFragment privMessage = new MessageDialogFragment();
                        privMessage.setmContext(mContext)
                                .setUsername(username)
                                .setChatRoom(chatRoom)
                                .setmAdapter(mAdapter)
                                .setMessageList(messageList)
                                .setMessageClient(messageClient);
                        privMessage.show(getFragmentManager(), "User Message");
                        break;
                }
            }
        });
        return builder.create();
    }

}
