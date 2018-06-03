package com.polklabs.roomy;

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
import android.util.Log;
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

import com.polklabs.roomy.backend.ChatRoom;

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
    String tState = "Unknown";
    String tCity = "Unknown";
    boolean bPassword = true;

    Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pre_room);

        mContext = getApplicationContext();

        mRoom = findViewById(R.id.name);
        mUsername = findViewById(R.id.username);
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
            try {
                getActionBar().setTitle("Join Room: " + tRoom);
            }catch(NullPointerException e){
                Log.d("Roomy", "Could not set title.");
            }
            try {
                getSupportActionBar().setTitle("Join Room: " + tRoom);
            }catch(NullPointerException e){
                Log.d("Roomy", "Could not set title.");
            }

            mPassword.setCompletionHint("Password");
            mPassword.setHint("Password");

            if (!tPassword.equals("") || !bPassword) {
                mPassword.setText(tPassword);
                mPassword.setVisibility(View.GONE);
            }
        }

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
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
                tCity = "Unknown";
                tState = "Unknown";
            }
        }

        mLocation.setText((tState+", "+tCity));

        mJoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                mPassword.setError(null);
                mUsername.setError(null);
                mRoom.setError(null);

                String roomName = mRoom.getText().toString();

                String localCheck = "N";
                if(mLocal.isChecked() && !tCity.equals("Unknown") && !tState.equals("Unknown")) {
                    localCheck = "Y";
                }else{
                    tCity = "Unknown";
                    tState = "Unknown";
                }
                String popCheck = "N";
                if(mPopular.isChecked()) popCheck = "Y";

                appState.chatRoom = new ChatRoom(new ChatRoom.Listener() {
                    @Override
                    public void setText(String text) {
                        if(text.equals("DONE")){
                            //Start next thing
                            Intent newIntent = new Intent(mContext, Room.class);
                            startActivityForResult(newIntent, 120);
                        }
                    }

                    @Override
                    public void setText2(String text) {
                        //Called if error
                        showProgress(false);
                        String errorM = appState.chatRoom.errorMessage;
                        switch(appState.chatRoom.errorType){
                            case 1:
                                mRoom.setError(errorM);
                                break;
                            case 2:
                                mPassword.setError(errorM);
                                break;
                            case 3:
                                Toast toast = Toast.makeText(mContext, errorM, Toast.LENGTH_LONG);
                                toast.show();
                                break;
                            case 4:
                                mUsername.setError(errorM);
                                break;
                            case 5:
                                mUsername.setError(errorM);
                                break;
                                default:
                                    toast = Toast.makeText(mContext, errorM, Toast.LENGTH_LONG);
                                    toast.show();
                                    break;
                        }
                    }
                }, "join", tState, tCity, roomName, mPassword.getText().toString(), mUsername.getText().toString(), popCheck+localCheck);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 120){
            finish();
        }else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
