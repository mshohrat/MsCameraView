package com.github.demo;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;

/**
 * Created by Meysam on 12/13/2017.
 */

public class DemoApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        //LeakCanary.install(this);
    }
}
