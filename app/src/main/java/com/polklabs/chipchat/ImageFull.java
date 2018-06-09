package com.polklabs.chipchat;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

public class ImageFull extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_full);

        Intent intent = getIntent();
        Bitmap bitmap = BitmapFactory.decodeByteArray(intent.getByteArrayExtra("image"), 0, intent.getByteArrayExtra("image").length);

        ((ImageView)findViewById(R.id.fullImage)).setImageBitmap(bitmap);

        if(getActionBar() != null)
            getActionBar().setTitle("Image from: "+intent.getStringExtra("from"));
    }

}
