package asis.youtubeconverter.download;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.yausername.youtubedl_android.YoutubeDL;

public class DownloadNotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        YoutubeDL.getInstance().destroyProcessById(intent.getStringExtra("video_url"));
    }
}
