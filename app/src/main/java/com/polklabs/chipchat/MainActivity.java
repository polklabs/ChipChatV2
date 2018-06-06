package com.polklabs.chipchat;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
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
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.polklabs.chipchat.backend.ChatRoom;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String APP_NAME = "Roomy";

    private App appState;
    private Context mContext;

    private ScrollView mListSpace;
    private LinearLayout mPopularList;
    private LinearLayout mLocalList;
    private Button mPopularButton;
    private Button mLocalButton;
    private ProgressBar mProgressBar;
    private TextView mLocation;

    private View.OnClickListener roomClick;

    private LocationManager locationManager;

    private String tState = "";
    private String tCity = "";

    //NFC-------------------------------------------------------------------------------------------
    private NfcAdapter mNfcAdapter;

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
        mListSpace = findViewById(R.id.ListSpace);
        mPopularList = findViewById(R.id.popularList);
        mPopularButton = findViewById(R.id.buttonPopular);

        mLocalList = findViewById(R.id.localList);
        mLocalButton = findViewById(R.id.buttonLocal);
        mProgressBar = findViewById(R.id.progressBar2);
        mLocation = findViewById(R.id.location);

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

        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        }

        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        Refresh();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(appState.chatRoom.errorType != 6) {
                    Intent intent = new Intent(mContext, PreRoom.class);
                    intent.putExtra("name", "");
                    intent.putExtra("pass", "");
                    startActivity(intent);
                }else{
                    Toast toast = Toast.makeText(mContext, "Server Offline", Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        });

        //NFC----------------
        initNFC();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Refresh();
    }

    private void startConnect(View v){
        LinearLayout l = (LinearLayout) v;
        try {
            JSONObject roomObj = new JSONObject(l.getContentDescription().toString());

            String name = roomObj.getString("name");
            if(name.contains(":")){
                name = name.split(":", 2)[1];
            }

            Intent intent = new Intent(mContext, PreRoom.class);
            intent.putExtra("name", name);
            intent.putExtra("pass", "");
            intent.putExtra("hasPass", roomObj.getBoolean("lock"));
            if (l.getParent() == mLocalList) {
                intent.putExtra("isLocal", true);
            }
            startActivity(intent);
        }catch (JSONException e){}
    }

    private void createList(JSONArray list, boolean isPopular){
        LinearLayout mList = mPopularList;
        if(!isPopular) mList = mLocalList;

        final float scale = mContext.getResources().getDisplayMetrics().density;
        mList.removeAllViews();

        if(isPopular)mPopularButton.setText(R.string.default_popular);
        else mLocalButton.setText(R.string.default_local);

        if(list.length() <= 0){
            TextView none = new TextView(mContext);

            if(isPopular) none.setText(R.string.no_pop_chat_room);
            else none.setText(R.string.no_loc_chat_room);

            if(appState.chatRoom.errorType == 7) {
                none.setText("Could not connect to the server.\nPlease try again.");
                none.setTextSize(24);
            }

            none.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            int pixels = (int)(16*scale+0.5f);
            none.setPadding(pixels, pixels, pixels, pixels);
            mList.addView(none);
            return;
        }

        if(isPopular)mPopularButton.setText(("Popular ("+list.length()+")"));
        else mLocalButton.setText((" Local ("+list.length()+") "));

        boolean odd = false;
        for(int i = 0; i < list.length(); i++){
            try {
                String actualName;
                String contentDesc = list.getJSONObject(i).toString();
                if (!isPopular) {
                    actualName = (list.getJSONObject(i).getString("name").split(":")[1]);
                } else {
                    actualName = list.getJSONObject(i).getString("name");
                }

                int pixels = (int) (18 * scale + 0.5f);
                LinearLayout.LayoutParams paramsText = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
                LinearLayout.LayoutParams paramsImage = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, pixels, 1.0f);

                //Setup Layout to hold text
                LinearLayout newLayout = new LinearLayout(mContext);
                newLayout.setHapticFeedbackEnabled(true);
                newLayout.setOrientation(LinearLayout.HORIZONTAL);
                newLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f));
                newLayout.setContentDescription(contentDesc);
                pixels = (int) (16 * scale + 0.5f);
                newLayout.setPadding(pixels, pixels, pixels, pixels);
                if (odd) {
                    newLayout.setBackgroundColor(ContextCompat.getColor(mContext, R.color.light_grey));
                }
                newLayout.setOnClickListener(roomClick);
                mList.addView(newLayout);

                TextView textName = new TextView(mContext);
                ImageView imageLock = new ImageView(mContext);
                TextView textUsers = new TextView(mContext);

                textName.setText(actualName);
                textName.setLayoutParams(paramsText);
                textName.setTextSize(18);

                if (list.getJSONObject(i).getBoolean("lock")) {
                    imageLock.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_lock_lock));
                }
                imageLock.setScaleType(ImageView.ScaleType.FIT_END);
                imageLock.setLayoutParams(paramsImage);

                textUsers.setText(("Users: " + list.getJSONObject(i).getInt("size")));
                textUsers.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
                textUsers.setLayoutParams(paramsText);

                newLayout.addView(textName);
                newLayout.addView(imageLock);
                newLayout.addView(textUsers);

                odd = !odd;
            }catch (JSONException e){
                Log.d("ChipChat", e.toString());
            }
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
            Refresh();
            return true;
        }        //noinspection SimplifiableIfStatement


        return super.onOptionsItemSelected(item);
    }

    private void Refresh(){
        showProgress(true);
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
            List<Address> addresses;
            try{
                addresses = gcd.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                if(addresses.size() > 0){
                    tState = addresses.get(0).getAdminArea();
                    tCity = addresses.get(0).getLocality();
                }
            }catch(IOException e){
                tCity = "";
                tState = "";
            }
        }

        mLocation.setText((tState+", "+tCity));

        appState.chatRoom = new ChatRoom(new ChatRoom.Listener() {
            @Override
            public void publishText(String text) { }
            @Override
            public void returnText(String text){
                if(!text.equals("")) {
                    Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show();
                    createList(new JSONArray(), true);
                    createList(new JSONArray(), false);
                }
                showProgress(false);
            }

            @Override
            public void setList(boolean popular, JSONArray list) {
                createList(list, popular);
                //showProgress(false);
            }
            @Override
            public void publishMessage(String sender, String body) { }
            @Override
            public void publishImage(String sender, String data) { }
        }, "init", tState, tCity);
        appState.chatRoom.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        Log.d(APP_NAME, "Refresh");
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mListSpace.setVisibility(show ? View.GONE : View.VISIBLE);
        mListSpace.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mListSpace.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressBar.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    //NFC-------------------------------------------
    private void initNFC(){
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
    }

    private void readFromNFC(Ndef ndef) {

        try {
            ndef.connect();
            NdefMessage ndefMessage = ndef.getNdefMessage();
            String message = new String(ndefMessage.getRecords()[0].getPayload());
            //message = message.substring(3);
            String[] parts = message.split(";");
            String chatroom_name = parts[0];
            String chatroom_password = parts[1];
            String need_password = parts[2];
            String set_local = parts[3];
            chatroom_password = chatroom_password.replace("/", "");
            Log.d("NFC", "Chatroom name: "+ chatroom_name);
            Log.d("NFC", "Chatroom password: "+chatroom_password);
            Log.d("NFC", "needs password: "+need_password);
            Log.d("NFC", "Is local: "+set_local);
            ndef.close();

            Intent intent = new Intent(mContext, PreRoom.class);
            intent.putExtra("name", chatroom_name);
            intent.putExtra("pass", chatroom_password);
            intent.putExtra("hasPass", Boolean.parseBoolean(need_password));
            intent.putExtra("isLocal", Boolean.parseBoolean(set_local));

            startActivity(intent);

        } catch (IOException | FormatException e) {
            e.printStackTrace();

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        IntentFilter techDetected = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        IntentFilter[] nfcIntentFilter = new IntentFilter[]{techDetected,tagDetected,ndefDetected};

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        if(mNfcAdapter!= null)
            mNfcAdapter.enableForegroundDispatch(this, pendingIntent, nfcIntentFilter, null);

    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mNfcAdapter!= null)
            mNfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent){
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        Log.d("NFC", "onNewIntent: "+intent.getAction());

        if(tag != null) {
            Toast.makeText(this, "Message detected.", Toast.LENGTH_SHORT).show();
            Ndef ndef = Ndef.get(tag);
            readFromNFC(ndef);
        }
    }
}
