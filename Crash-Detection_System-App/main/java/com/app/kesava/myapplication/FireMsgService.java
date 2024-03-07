package com.app.kesava.myapplication;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FireMsgService extends FirebaseMessagingService {

    static private NotificationManager mManager;

    public static final String CHANNEL_ID = "personal_notifications";
    public static final int notification_id = 001;

    public static String target_token = "N/A";


    @Override
    public void onNewToken(String token) {
        Log.i("FireBaseMessage","fire token: " + token);
        FirebaseDatabase.getInstance().getReference("MobileToken").setValue(token);
    }

    public void CreateNotificationChannel(){

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){

            CharSequence name = "Notification";
            String description = "Notification from collision alert.";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel notificationChannel = new NotificationChannel(getResources().getString(R.string.alert_notification_channel_1),name,importance);

            notificationChannel.setDescription(description);

            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            notificationManager.createNotificationChannel(notificationChannel);

        }

    }


    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d("FireBaseMessage", "notification received ["+remoteMessage.getNotification().getBody()+"]");

        CreateNotificationChannel();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, getResources().getString(R.string.alert_notification_channel_1));

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setSmallIcon(R.drawable.alert_marker);
        } else {
            builder.setSmallIcon(R.drawable.alert_marker);
        }


        builder.setContentTitle(remoteMessage.getNotification().getTitle());
        builder.setContentText(remoteMessage.getNotification().getBody());
        builder.setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(notification_id, builder.build());



    }
}
