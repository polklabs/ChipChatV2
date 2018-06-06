package com.polklabs.chipchat;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.polklabs.chipchat.backend.ChatRoom;
import com.polklabs.chipchat.backend.Message;
import com.polklabs.chipchat.backend.client;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.polklabs.chipchat.gallery.LoadImageTask.calculateInSampleSize;

public class Room extends AppCompatActivity{

    Context mContext;

    RecyclerView mMessageList;
    EditText mEditText;
    Button mSendButton;
    Button mAddImage;

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
        mAddImage = findViewById(R.id.addImage);

        mAdapter = new MessageAdapter(messageList);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        mMessageList.setLayoutManager(mLayoutManager);
        mMessageList.setItemAnimator(new DefaultItemAnimator());
        mMessageList.setAdapter(mAdapter);

        appState.chatRoom.setListener(new ChatRoom.Listener() {
            @Override
            public void publishText(String text) { }

            @Override
            public void returnText(String text) {
                //On close
            }

            @Override
            public void setList(boolean popular, JSONArray list) { }

            @Override
            public void publishMessage(String sender, String body) {
                SimpleDateFormat df = new SimpleDateFormat("HH:mm");
                String date = df.format(Calendar.getInstance().getTime());

                Message message = new Message(body, sender+"\t", date);
                messageList.add(message);
                mAdapter.notifyDataSetChanged();
                mMessageList.smoothScrollToPosition(mAdapter.getItemCount()-1);
            }

            @Override
            public void publishImage(String sender, String data) {
                SimpleDateFormat df = new SimpleDateFormat("HH:mm");
                String date = df.format(Calendar.getInstance().getTime());

                Message message = new Message(data, sender+"\t", date);
                messageList.add(message);
                mAdapter.notifyDataSetChanged();
                mMessageList.smoothScrollToPosition(mAdapter.getItemCount()-1);
            }
        });

        appState.chatRoom.loadedRoom = true;

        messageClient = new client(appState.chatRoom);
        messageClient.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }catch(NullPointerException e){}

                if(!mEditText.getText().toString().equals("")) {
                    messageClient.messages.add(mEditText.getText().toString());

                    SimpleDateFormat df = new SimpleDateFormat("HH:mm");
                    String date = df.format(Calendar.getInstance().getTime());

                    Message message = new Message(mEditText.getText().toString(), appState.chatRoom.username+"\t", date);
                    message.setSentByMe();
                    messageList.add(message);
                    mAdapter.notifyDataSetChanged();
                    mMessageList.smoothScrollToPosition(mAdapter.getItemCount()-1);
                }
                mEditText.setText("");
            }
        });

        mAddImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent1 = new Intent(mContext, GalleryActivity.class);
                startActivityForResult(intent1, 12);
            }
        });
    }

    @Override
    protected void onDestroy(){
        messageClient.stop = true;
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 12){
            if(resultCode == RESULT_OK){
                String bitmapPath = data.getStringExtra("path");
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(bitmapPath, options);

                //Adjust sample size
                options.inSampleSize = calculateInSampleSize(options, GalleryActivity.size/5, GalleryActivity.size/5);

                //Return sampled bitmap
                options.inJustDecodeBounds = false;
                messageClient.images.add(BitmapFactory.decodeFile(bitmapPath, options));

                SimpleDateFormat df = new SimpleDateFormat("HH:mm");
                String date = df.format(Calendar.getInstance().getTime());

                Message message = new Message("Sent image: "+bitmapPath, appState.chatRoom.username+"\t", date);
                message.setSentByMe();
                messageList.add(message);
                mAdapter.notifyDataSetChanged();
                mMessageList.smoothScrollToPosition(mAdapter.getItemCount()-1);
            }
        }else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
