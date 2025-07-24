package com.andro2.qiblatime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.NotificationManager;

import androidx.core.app.NotificationCompat;

public class PrayerAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String prayerName = intent.getStringExtra("prayerName");
        int notificationId = intent.getIntExtra("notificationId", 0);

        NotificationCompat.Builder builder = NotificationHelper.buildNotification(context, prayerName);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(notificationId, builder.build());
    }
}
