package com.example.cryptotrader;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

/**
 * App class is a controller for sending notifications.
 * */
public class App extends Application {
    public static final String CHANNEL_1_ID = "Order Updates";
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    /**
     * from Google Android studio tutorials, a straight forward implentation of a notification channel,
     * set as a default to Order Updates.
     */
    private void createNotificationChannel() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_1_ID,
                    CHANNEL_1_ID,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("This is Order Updates Channel");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
}
