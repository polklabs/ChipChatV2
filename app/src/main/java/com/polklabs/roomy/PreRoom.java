package com.polklabs.roomy;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.Toast;

import com.polklabs.roomy.backend.ChatRoom;

public class PreRoom extends AppCompatActivity {

    AutoCompleteTextView mRoom;
    AutoCompleteTextView mUsername;
    AutoCompleteTextView mPassword;
    CheckBox mLocal;
    CheckBox mPopular;
    Button mJoin;
    ScrollView mJoinForm;

    ProgressBar mProgressbar;

    String tRoom = "";
    String tPassword = "";

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

        final App appState = ((App)getApplication());

        Intent intent = getIntent();
        if (intent != null) {
            tRoom = intent.getStringExtra("name");
            tPassword = intent.getStringExtra("pass");
        }

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

            if (!tPassword.equals("")) {
                mPassword.setText(tPassword);
                mPassword.setVisibility(View.GONE);
            }
        }

        mJoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                String localCheck = "N";
                if(mLocal.isChecked()) localCheck = "Y";
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
                        Toast toast = Toast.makeText(mContext, text, Toast.LENGTH_LONG);
                        toast.show();
                        showProgress(false);
                    }
                }, "join", mRoom.getText().toString(), mPassword.getText().toString(), mUsername.getText().toString(), popCheck+localCheck);
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
