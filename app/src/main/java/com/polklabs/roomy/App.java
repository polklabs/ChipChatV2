package com.polklabs.roomy;

import android.app.Application;
import android.graphics.Bitmap;
import com.polklabs.roomy.backend.ChatRoom;
import java.util.HashMap;
import java.util.Map;

public class App extends Application{
    ChatRoom chatRoom;
    Map<String, Bitmap> mBitmapCache = new HashMap<>();
}
