package asis.youtubeconverter;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

import com.yausername.ffmpeg.FFmpeg;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;

public class YouTubeConverter extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        try {
            YoutubeDL.getInstance().init(this);
            FFmpeg.getInstance().init(this);
        } catch (YoutubeDLException e) {
            e.printStackTrace();
        }

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
