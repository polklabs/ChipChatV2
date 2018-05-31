package com.polklabs.roomy;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.polklabs.roomy.backend.ChatRoom;

public class MainActivity extends AppCompatActivity {

    private static final String APP_NAME = "Roomy";

    private App appState;
    private Context mContext;

    private LinearLayout mPopularList;
    private LinearLayout mLocalList;
    private Button mPopularButton;
    private Button mLocalButton;

    private View.OnClickListener roomClick;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Save in global state
        appState = ((App)this.getApplication());
        mContext = getApplicationContext();

        //View Objects
        mPopularList = findViewById(R.id.popularList);
        mPopularButton = findViewById(R.id.buttonPopular);

        mLocalList = findViewById(R.id.localList);
        mLocalButton = findViewById(R.id.buttonLocal);

        mPopularButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPopularList.setVisibility(View.VISIBLE);
                mLocalList.setVisibility(View.GONE);
                mPopularButton.setBackgroundColor(ContextCompat.getColor(mContext, R.color.light_blue));
                mLocalButton.setBackgroundColor(ContextCompat.getColor(mContext, R.color.light_grey));
            }
        });
        mLocalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPopularList.setVisibility(View.GONE);
                mLocalList.setVisibility(View.VISIBLE);
                mPopularButton.setBackgroundColor(ContextCompat.getColor(mContext, R.color.light_grey));
                mLocalButton.setBackgroundColor(ContextCompat.getColor(mContext, R.color.light_blue));
            }
        });
        mPopularButton.callOnClick();

        roomClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startConnect(v);
            }
        };

        appState.chatRoom = new ChatRoom(new ChatRoom.Listener() {
            @Override
            public void setText(String text) {
                createList(text, true);
            }
            @Override
            public void setText2(String text){
                createList(text, false);
            }
        }, "init");
        appState.chatRoom.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, PreRoom.class);
                intent.putExtra("name", "");
                intent.putExtra("pass", "");
                startActivity(intent);
            }
        });
    }

    private void startConnect(View v){
        LinearLayout l = (LinearLayout) v;
        Toast toast = Toast.makeText(this, l.getContentDescription(), Toast.LENGTH_SHORT);
        toast.show();
        Intent intent = new Intent(mContext, PreRoom.class);
        intent.putExtra("name", l.getContentDescription());
        intent.putExtra("pass", "");
        startActivity(intent);
        //Start activity
    }

    private void createList(String text, boolean isPopular){
        Log.d(APP_NAME, "Create list: "+text);

        LinearLayout mList = mPopularList;
        if(!isPopular){
            mList = mLocalList;
        }

        final float scale = mContext.getResources().getDisplayMetrics().density;
        mList.removeAllViews();

        if(text.equals("")){
            TextView none = new TextView(mContext);
            none.setText(R.string.no_chat_room);
            none.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            int pixels = (int)(16*scale+0.5f);
            none.setPadding(pixels, pixels, pixels, pixels);
            mList.addView(none);
            return;
        }

        String[] splitRooms = text.split(";");
        boolean odd = false;
        for(String room : splitRooms){
            String[] tempSplit = room.split(" ");

            int pixels = (int)(18*scale+0.5f);
            LinearLayout.LayoutParams paramsText = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            LinearLayout.LayoutParams paramsImage = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, pixels, 1.0f);

            //Setup Layout to hold text
            LinearLayout newLayout = new LinearLayout(mContext);
            newLayout.setHapticFeedbackEnabled(true);
            newLayout.setOrientation(LinearLayout.HORIZONTAL);
            newLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f));
            newLayout.setContentDescription(tempSplit[0]);
            pixels = (int)(16*scale+0.5f);
            newLayout.setPadding(pixels,pixels,pixels,pixels);
            if(odd){
                newLayout.setBackgroundColor(ContextCompat.getColor(mContext, R.color.light_grey));
            }
            newLayout.setOnClickListener(roomClick);
            mList.addView(newLayout);

            TextView textName = new TextView(mContext);
            ImageView imageLock = new ImageView(mContext);
            TextView textUsers = new TextView(mContext);

            textName.setText(tempSplit[0]);
            textName.setLayoutParams(paramsText);
            textName.setTextSize(18);

            if(tempSplit[2].equals("L")) {
                imageLock.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_lock_lock));
            }
            imageLock.setScaleType(ImageView.ScaleType.FIT_END);
            imageLock.setLayoutParams(paramsImage);

            textUsers.setText("Users: "+tempSplit[1]);
            textUsers.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
            textUsers.setLayoutParams(paramsText);

            newLayout.addView(textName);
            newLayout.addView(imageLock);
            newLayout.addView(textUsers);

            odd = !odd;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            appState.chatRoom = new ChatRoom(new ChatRoom.Listener() {
                @Override
                public void setText(String text) {
                    createList(text, true);
                }
                @Override
                public void setText2(String text){
                    createList(text, false);
                }
            }, "init");
            appState.chatRoom.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            Log.d(APP_NAME, "Refresh");
            return true;
        }        //noinspection SimplifiableIfStatement


        return super.onOptionsItemSelected(item);
    }
}
