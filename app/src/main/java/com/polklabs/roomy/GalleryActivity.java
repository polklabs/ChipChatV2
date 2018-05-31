package com.polklabs.roomy;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.polklabs.roomy.gallery.LoadImageTask;

import java.io.File;
import java.util.ArrayList;

public class GalleryActivity extends AppCompatActivity {

    public static int size = 300;

    private TableLayout mLayout;
    private TableRow mRow;
    private TextView imagePath;

    private int count = 0;
    private int MAX_COUNT = 4;

    private String[] paths = {Environment.getExternalStorageDirectory().toString()};

    private String exclude = Environment.getExternalStorageDirectory().toString()+"/Android";

    private ArrayList<ImageView> images = new ArrayList<>();

    private String pathString;
    private String nameString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent path = new Intent(GalleryActivity.this, Room.class);
                path.putExtra("path", pathString);
                path.putExtra("name", nameString);
                setResult(RESULT_OK, path);
                finish();
            }
        });

        //Get permission for storage access
        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        }
        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }

        mLayout = findViewById(R.id.imageLayout);
        imagePath = findViewById(R.id.path);

        Point screen = new Point();
        getWindowManager().getDefaultDisplay().getSize(screen);

        int MAX_COUNT_PORTRAIT = 4;
        int MAX_COUNT_LANDSCAPE = 7;
        if(getResources().getConfiguration().orientation == 1)
            MAX_COUNT = MAX_COUNT_PORTRAIT;
        else
            MAX_COUNT = MAX_COUNT_LANDSCAPE;

        size = (screen.x/MAX_COUNT);
    }

    @Override
    protected void onResume(){
        super.onResume();

        for(String s : paths){
            addImages(s);
        }

        final App appState = ((App)this.getApplication());

        for(final ImageView image : images){
            final String contentDesc = image.getContentDescription().toString();

            if(appState.mBitmapCache.containsKey(contentDesc)){
                image.setImageBitmap(appState.mBitmapCache.get(contentDesc));
                image.invalidate();
                image.refreshDrawableState();
            }else{
                new LoadImageTask(new LoadImageTask.Listener() {
                    @Override
                    public void onImageLoad(Bitmap bitmap) {
                        appState.mBitmapCache.put(contentDesc, bitmap);
                        image.setImageBitmap(bitmap);
                        image.invalidate();
                        image.refreshDrawableState();
                    }

                    @Override
                    public void onImageLoadError() {
                        image.setVisibility(View.GONE);
                    }
                }, contentDesc).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
            }
        }

    }

    private void addImages(String directory){
        //Log.d("File", directory);

        File root = new File(directory);
        File[] list = root.listFiles();

        if(list == null) return;

        //Log.d("File", String.format("%d", list.length));

        for(File file : list){
            if(file.isDirectory()) {
                if (file.getAbsolutePath().contains(exclude) || file.isHidden())
                    Log.d("File", "True");
                else
                    addImages(file.getAbsolutePath());
            }else{
                String extension = "";
                int i = file.getName().lastIndexOf('.');
                if(i >= 0){ extension = file.getName().substring(i+1);}

                if(extension.toUpperCase().matches("^.*?(JPEG|JPG|PNG|GIF|AVI|MP4).*$")){
                    if(count == MAX_COUNT) count = 0;
                    if(count == 0){
                        mRow = new TableRow(GalleryActivity.this);
                        mRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                        mRow.setGravity(Gravity.CENTER);
                        mLayout.addView(mRow);
                    }

                    ImageView temp = new ImageView(GalleryActivity.this);
                    File imgFile = new File(file.getAbsolutePath());
                    if(imgFile.exists()) {
                        temp.setContentDescription(imgFile.getAbsolutePath());
                        temp.setTag(imgFile.getName());
                        temp.setLayoutParams(new TableRow.LayoutParams(size, size));
                        temp.setScaleType((ImageView.ScaleType.CENTER_CROP));
                        temp.setOnClickListener(onClick());
                        images.add(temp);
                        mRow.addView(temp);
                        count++;
                    }
                }
            }
        }
    }

    private View.OnClickListener onClick(){
        return new View.OnClickListener(){
            @Override
            public void onClick(View v){
                ImageView img = (ImageView) v;

                pathString = img.getContentDescription().toString();
                nameString = img.getTag().toString();
                imagePath.setText(img.getContentDescription().toString());
            }
        };
    }
}
