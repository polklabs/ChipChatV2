package com.polklabs.chipchat;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.polklabs.chipchat.backend.ChatRoom;

import org.json.JSONArray;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class PreRoom extends AppCompatActivity {

    AutoCompleteTextView mRoom;
    AutoCompleteTextView mUsername;
    AutoCompleteTextView mPassword;
    CheckBox mLocal;
    CheckBox mPopular;
    Button mJoin;
    ScrollView mJoinForm;
    ProgressBar mProgressbar;
    TextView mLocation;

    String tRoom = "";
    String tPassword = "";
    String tState = "";
    String tCity = "";
    boolean bPassword = true;

    Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pre_room);

        mContext = getApplicationContext();

        mRoom = findViewById(R.id.name);
        mUsername = findViewById(R.id.privateMessage);
        mPassword = findViewById(R.id.password);
        mLocal = findViewById(R.id.localButton);
        mPopular = findViewById(R.id.listedButton);
        mJoin = findViewById(R.id.joinButton);
        mProgressbar = findViewById(R.id.progressBar);
        mJoinForm = findViewById(R.id.joinForm);
        mLocation = findViewById(R.id.location);

        final App appState = ((App)getApplication());

        Intent intent = getIntent();
        if (intent != null) {
            tRoom = intent.getStringExtra("name");
            tPassword = intent.getStringExtra("pass");
            bPassword = intent.getBooleanExtra("hasPass", true);
            mLocal.setChecked(intent.getBooleanExtra("isLocal", false));
            mPopular.setChecked(intent.getBooleanExtra("isLocal", false));
        }

        mLocal.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    mPopular.setChecked(true);
                    mPopular.setEnabled(false);
                }else{
                    mPopular.setChecked(false);
                    mPopular.setEnabled(true);
                }
            }
        });

        if (!tRoom.equals("")) {
            mRoom.setText(tRoom);
            mRoom.setVisibility(View.GONE);
            mLocal.setVisibility(View.GONE);
            mPopular.setVisibility(View.GONE);
            if(getActionBar() != null)
                getActionBar().setTitle("Join Room: " + tRoom);
            if(getSupportActionBar() != null)
                getSupportActionBar().setTitle("Join Room: " + tRoom);

            mPassword.setCompletionHint("Password");
            mPassword.setHint("Password");

            if (!tPassword.equals("") || !bPassword) {
                mPassword.setText(tPassword);
                mPassword.setVisibility(View.GONE);
            }
        }

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
            Location location = null;
            if(locationManager != null)
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
            List<Address> addresses;
            try{
                if(location != null) {
                    addresses = gcd.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    if (addresses.size() > 0) {
                        tState = addresses.get(0).getAdminArea();
                        tCity = addresses.get(0).getLocality();
                    }
                }
            }catch(IOException e){
                tCity = "";
                tState = "";
            }
        }

        mLocation.setText((tState+", "+tCity));
        if(tState.equals("") || tCity.equals("")){
            mLocation.setText(("Unknown, Unknown"));
        }

        mJoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                if(imm != null)
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                mPassword.setError(null);
                mUsername.setError(null);
                mRoom.setError(null);

                String roomName = mRoom.getText().toString();

                String localCheck = "false";
                if(mLocal.isChecked() && !tCity.equals("") && !tState.equals("")) {
                    localCheck = "true";
                }else{
                    tCity = "";
                    tState = "";
                }
                String popCheck = "false";
                if(mPopular.isChecked()) popCheck = "true";

                appState.chatRoom = new ChatRoom(new ChatRoom.Listener() {
                    @Override
                    public void setList(boolean popular, JSONArray list) { }
                    @Override
                    public void publishMessage(String sender, String body, boolean isPrivate) { }
                    @Override
                    public void publishImage(String sender, String data) { }

                    @Override
                    public void publishText(String text) {
                        if(text.equals("DONE")){
                            Intent newIntent = new Intent(mContext, Room.class);
                            startActivity(newIntent);
                            finish();
                        }
                    }

                    @Override
                    public void returnText(String text) {
                        //Called if error
                        showProgress(false);
                        switch(appState.chatRoom.errorType){
                            case 1:
                                mRoom.setError(text);
                                break;
                            case 2:
                                mPassword.setError(text);
                                break;
                            case 3:
                                Toast.makeText(mContext, text, Toast.LENGTH_LONG).show();
                                break;
                            case 4:
                                mUsername.setError(text);
                                break;
                            case 5:
                                mUsername.setError(text);
                                break;
                            default:
                                Toast.makeText(mContext, text, Toast.LENGTH_LONG).show();
                                break;
                        }
                    }
                }, "join", tState, tCity, roomName, mPassword.getText().toString(), mUsername.getText().toString(), localCheck, popCheck);
                showProgress(true);
                appState.chatRoom.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });
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

        mJoinForm.setVisibility(show ? View.GONE : View.VISIBLE);
        mJoinForm.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mJoinForm.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        mProgressbar.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressbar.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressbar.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }
}
