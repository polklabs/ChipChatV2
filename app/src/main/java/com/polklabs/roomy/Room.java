package com.polklabs.roomy;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.text.format.DateFormat;

import com.polklabs.roomy.backend.ChatRoom;
import com.polklabs.roomy.backend.Message;
import com.polklabs.roomy.backend.client;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.text.SimpleDateFormat;

public class Room extends AppCompatActivity {

    Context mContext;

    RecyclerView mMessageList;
    EditText mEditText;
    Button mSendButton;

    App appState;

    private List<Message> messageList = new ArrayList<>();
    private MessageAdapter mAdapter;

    private client messageClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);

        appState = (App)getApplication();

        mContext = getApplicationContext();

        setResult(RESULT_OK);

        mMessageList = findViewById(R.id.messageList);
        mEditText = findViewById(R.id.chatBox);
        mSendButton = findViewById(R.id.sendMessage);

        mAdapter = new MessageAdapter(messageList);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        mMessageList.setLayoutManager(mLayoutManager);
        mMessageList.setItemAnimator(new DefaultItemAnimator());
        mMessageList.setAdapter(mAdapter);

        appState.chatRoom.setListener(new ChatRoom.Listener() {
            @Override
            public void setText(String text) {
                int splitLoc = text.indexOf(':');

                SimpleDateFormat df = new SimpleDateFormat("HH:mm");
                String date = df.format(Calendar.getInstance().getTime());

                Message message = new Message(text.substring(splitLoc+1), text.substring(0, splitLoc)+"\t", date);
                messageList.add(message);
                mAdapter.notifyDataSetChanged();
                mMessageList.smoothScrollToPosition(mAdapter.getItemCount()-1);
            }

            @Override
            public void setText2(String text) {
                //On close
            }
        });

        messageClient = new client(appState.chatRoom);
        messageClient.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mEditText.getText().toString().equals("")) {
                    messageClient.messages.add(mEditText.getText().toString());

                    SimpleDateFormat df = new SimpleDateFormat("HH:mm");
                    String date = df.format(Calendar.getInstance().getTime());

                    Message message = new Message(mEditText.getText().toString(), appState.chatRoom.username+"\t", date);
                    messageList.add(message);
                    mAdapter.notifyDataSetChanged();
                    mMessageList.smoothScrollToPosition(mAdapter.getItemCount()-1);
                }
                mEditText.setText("");
            }
        });
    }

    @Override
    protected void onDestroy(){
        messageClient.stop = true;
        super.onDestroy();
    }
}
