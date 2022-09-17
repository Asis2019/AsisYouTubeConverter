package asis.youtubeconverter.download;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import asis.youtubeconverter.R;

public class DownloadNotificationService {
    public static final String DOWNLOAD_CHANNEL_ID = "download_channel";

    public static NotificationCompat.Builder getNotificationBuilder(Context context, String videoTitle, int progress, boolean indeterminate) {
        return new NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_download)
                .setContentTitle(videoTitle)
                .setOngoing(true)
                .setProgress(100, progress, indeterminate)
                .setSilent(true)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary));
    }

    public static void updateNotification(Notification notification, int notificationId, Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationId, notification);
    }
}
