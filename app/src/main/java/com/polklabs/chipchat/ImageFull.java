package com.polklabs.chipchat;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

import static com.polklabs.chipchat.gallery.LoadImageTask.calculateInSampleSize;

public class ImageFull extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_full);

        Intent intent = getIntent();

        Bitmap bitmap;

        if(intent.getBooleanExtra("self", false)) {
            bitmap = decodeSelf(intent.getStringExtra("path"));
        }else {
            bitmap = BitmapFactory.decodeByteArray(intent.getByteArrayExtra("image"), 0, intent.getByteArrayExtra("image").length);
        }
        if(bitmap != null) {
            ((ImageView) findViewById(R.id.fullImage)).setImageBitmap(bitmap);
        }

        if(getActionBar() != null)
            getActionBar().setTitle(intent.getStringExtra("from"));
    }

    private Bitmap decodeSelf(String path){
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        //Adjust sample size
        options.inSampleSize = calculateInSampleSize(options, GalleryActivity.size, GalleryActivity.size);

        //Return sampled bitmap
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

}
