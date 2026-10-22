package com.bookman.app;

import android.app.Application;
import com.bookman.app.push.ReminderNotifications;
import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class BookmanApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ReminderNotifications.createChannel(this);
    }
}
