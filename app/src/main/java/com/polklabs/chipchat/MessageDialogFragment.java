package com.polklabs.chipchat;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.polklabs.chipchat.backend.ChatRoom;
import com.polklabs.chipchat.backend.Message;
import com.polklabs.chipchat.backend.client;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.InvalidKeyException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

public class MessageDialogFragment extends DialogFragment {

    private client          messageClient;
    private ChatRoom        chatRoom;
    private String          username;
    private Context         mContext;
    private List<Message>   messageList = new ArrayList<>();
    private MessageAdapter  mAdapter;
    private View            v;

    public MessageDialogFragment setMessageList(List<Message> messageList) {
        this.messageList = messageList;
        return this;
    }
    public MessageDialogFragment setmAdapter(MessageAdapter mAdapter) {
        this.mAdapter = mAdapter;
        return this;
    }
    public MessageDialogFragment setMessageClient(client messageClient) {
        this.messageClient = messageClient;
        return this;
    }
    public MessageDialogFragment setChatRoom(ChatRoom chatRoom) {
        this.chatRoom = chatRoom;
        return this;
    }
    public MessageDialogFragment setUsername(String username){
        this.username = username;
        return this;
    }
    public MessageDialogFragment setmContext(Context context){
        this.mContext = context;
        return this;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        v = inflater.inflate(R.layout.private_message, null);

        v.findViewById(R.id.privateMessage).requestFocus();

        builder.setView(v)
                .setPositiveButton("Send", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try{
                            String body = ((EditText)v.findViewById(R.id.privateMessage)).getText().toString();
                            JSONObject message = new JSONObject();
                            message.put("type", "text");
                            message.put("private", true);
                            message.put("sender", chatRoom.username);
                            message.put("body", chatRoom.DE.encryptOnce(body, null, true));

                            JSONObject obj = new JSONObject();
                            obj.put(username, chatRoom.DE.encryptOnce(message.toString(), username, false));

                            message = new JSONObject();
                            message.put("type", "message");
                            message.put("messages", obj);

                            messageClient.justSend.add(message.toString());

                            SimpleDateFormat df = new SimpleDateFormat("HH:mm");
                            String date = df.format(Calendar.getInstance().getTime());
                            Message message1 = new Message(body, chatRoom.username, date);
                            message1.setSentByMe();
                            message1.setPrivate();
                            messageList.add(message1);
                            mAdapter.notifyDataSetChanged();
                        }catch (JSONException | IllegalBlockSizeException | InvalidKeyException | BadPaddingException e){
                            Log.e("ChipChat", "Error with private message", e);
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MessageDialogFragment.this.getDialog().cancel();
                    }
                });

        return builder.create();
    }

}