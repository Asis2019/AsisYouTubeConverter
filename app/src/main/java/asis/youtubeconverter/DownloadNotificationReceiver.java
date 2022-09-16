package asis.youtubeconverter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.yausername.youtubedl_android.YoutubeDL;

public class DownloadNotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        YoutubeDL.getInstance().destroyProcessById(bundle.getString("video_url"));
        DownloadNotificationService.cancelNotification(context, bundle.getInt("notification_id"));
    }
}
