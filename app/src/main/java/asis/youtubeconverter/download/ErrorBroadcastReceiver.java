package asis.youtubeconverter.download;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class ErrorBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String errorMessage = intent.getStringExtra("error_message");
        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
    }
}
