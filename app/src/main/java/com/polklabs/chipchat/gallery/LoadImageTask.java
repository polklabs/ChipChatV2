package com.polklabs.chipchat.gallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import com.polklabs.chipchat.GalleryActivity;

//This AsyncTask allows images to be loaded away from the UI thread
public class LoadImageTask extends AsyncTask<String, Void, Bitmap> {

    //Interface ------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------

    //Listener used to update UI
    public interface Listener{
        void onImageLoad(final Bitmap bitmap);
        void onImageLoadError();
    }

    //Private Variables ----------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private Listener listener;
    private String text;

    //Constructor ----------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------

    public LoadImageTask(final Listener listener, String text){
        this.listener = listener;
        this.text = text;
    }

    //Overridden methods ---------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------

    //Loads a bitmap from a path string
    @Override
    protected Bitmap doInBackground(String... params){
        //Loads bitmap size
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(text, options);

        //Adjust sample size
        options.inSampleSize = calculateInSampleSize(options, GalleryActivity.size/5, GalleryActivity.size/5);

        //Return sampled bitmap
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(text, options);
    }

    //Allows the view to be updated as images load
    @Override
    protected void onPostExecute(Bitmap result){
        if(isCancelled()) return;

        if(null != result){
            listener.onImageLoad(result);
        } else {
            listener.onImageLoadError();
        }
    }

    //Private Methods ------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------

    //Calculates the sample size to load images faster
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight){
        //Height and Width of input image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        //Adjust the sample size to fit reqHeight/reqWidth
        if(height > reqHeight || width > reqWidth){
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth){
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
