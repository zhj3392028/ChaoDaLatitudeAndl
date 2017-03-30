package com.chqqc.zhync.chaodalatitudeandl.handle;

import android.os.Handler;
import android.os.Looper;

import com.chqqc.zhync.chaodalatitudeandl.MainActivity;

/**
 * Created by Time on 17/3/28.
 */

public class MainHandler extends Handler {
    private static volatile MainHandler instance;
    public static MainHandler getInstance(){
        if (null == instance) {
            synchronized (MainHandler.class){
                if (null ==instance) {
                    instance = new MainHandler();
                }
            }
        }
        return instance;
    }
    private MainHandler(){
        super(Looper.getMainLooper());
    }
}
