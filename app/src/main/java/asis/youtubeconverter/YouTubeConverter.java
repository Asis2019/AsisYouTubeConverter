package asis.youtubeconverter;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

import asis.youtubeconverter.download.DownloadNotificationService;

public class YouTubeConverter extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        NotificationChannel notificationChannel = new NotificationChannel(
                DownloadNotificationService.DOWNLOAD_CHANNEL_ID,
                "Downloads",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        notificationChannel.setDescription("Used for displaying the downloading progress of downloads");

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(notificationChannel);
    }
}
