package com.chqqc.zhync.chaodalatitudeandl.application;

import android.app.Application;

/**
 * Created by Time on 17/3/28.
 */


import android.app.Application;

import io.realm.Realm;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Realm.init(this);
    }
}
