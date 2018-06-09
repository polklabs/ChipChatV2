package com.polklabs.chipchat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;

import com.polklabs.chipchat.gallery.LoadImageTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;

import static com.polklabs.chipchat.gallery.LoadImageTask.calculateInSampleSize;

public class GalleryActivity  extends AppCompatActivity{

    public static int size = 300;

    private TableLayout mLayout;
    private TableRow mRow;

    private int count = 0;
    private int MAX_COUNT = 4;

    private String[] paths = {Environment.getExternalStorageDirectory().toString()};

    private String exclude = Environment.getExternalStorageDirectory().toString()+"/Android";

    private ArrayList<ImageView> images = new ArrayList<>();

    private String pathString = "";
    private String nameString = "";
    private View lastImage;

    private ArrayList<LoadImageTask> task;

    private App appState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        FloatingActionButton fab = findViewById(R.id.fab);
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
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED || ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }

        mLayout = findViewById(R.id.imageLayout);
        mLayout.removeAllViews();

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
    protected void onDestroy(){
        super.onDestroy();
        appState.mBitmapCache.clear();
    }

    @Override
    protected void onResume() {
        super.onResume();

        task = new ArrayList<>();

        for(String s : paths){
            addImages(s);
        }

        appState = ((App)this.getApplication());

        for(final ImageView image : images){
            final String contentDesc = image.getContentDescription().toString();

            if(appState.mBitmapCache.containsKey(contentDesc)){
                image.setImageBitmap(appState.mBitmapCache.get(contentDesc));
                image.invalidate();
                image.refreshDrawableState();
            }else{
                task.add(new LoadImageTask(new LoadImageTask.Listener() {
                    @Override
                    public void onImageLoad(Bitmap bitmap) {
                        appState.mBitmapCache.put(contentDesc, bitmap);
                        image.setImageBitmap(bitmap);
                        image.invalidate();
                        image.refreshDrawableState();
                        task.remove(0);
                    }

                    @Override
                    public void onImageLoadError() {
                        image.setVisibility(View.GONE);
                    }
                }, contentDesc));
                task.get(task.size()-1).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
            }
        }
    }

    private void addImages(String directory){
        //Log.d("File", directory);

        File root = new File(directory);
        File[] list = root.listFiles();

        if(list == null) return;

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

                if(extension.toUpperCase().matches("^.*?(JPEG|JPG|PNG|GIF).*$")){
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
                        temp.setOnLongClickListener(longClick());
                        images.add(temp);
                        mRow.addView(temp);
                        count++;
                    }
                }
            }
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        for(LoadImageTask singleTask : task){
            singleTask.cancel(true);
        }
    }

    private View.OnClickListener onClick(){
        return new View.OnClickListener(){
            @Override
            public void onClick(View v){
                ImageView img = (ImageView) v;

                String oldPath = pathString;

                if(lastImage != null){
                    pathString = "";
                    nameString = "";
                    lastImage.setForeground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.overlay_null));
                }

                if(oldPath.equals(img.getContentDescription().toString())){
                    pathString = "";
                    nameString = "";
                    img.setForeground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.overlay_null));
                }else {
                    pathString = img.getContentDescription().toString();
                    nameString = img.getTag().toString();
                    img.setForeground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.overlay_blue));
                }
                img.invalidate();
                img.refreshDrawableState();
                lastImage = v;
            }
        };
    }

    private View.OnLongClickListener longClick(){
        return new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {


                Intent intent = new Intent(getApplicationContext(), ImageFull.class);
                intent.putExtra("self", true);
                intent.putExtra("path", v.getContentDescription());
                intent.putExtra("from", "Preview");
                startActivity(intent);

                return true;
            }
        };
    }
}
