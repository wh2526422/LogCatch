package com.xbh.logcatch;

import android.app.Application;
import android.content.Intent;


public class CatchLogApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Intent service = new Intent(this, CatchLogService.class);
        startService(service);
    }
}
