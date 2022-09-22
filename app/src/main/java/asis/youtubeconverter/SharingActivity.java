package asis.youtubeconverter;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import asis.youtubeconverter.download.DownloadService;

public class SharingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent externalIntent = getIntent();
        if (!Intent.ACTION_SEND.equals(externalIntent.getAction())) {
            finish();
        }

        String url = externalIntent.getStringExtra(Intent.EXTRA_TEXT);
        if (url != null) {
            Intent intent = new Intent(this, DownloadService.class);
            intent.putExtra("video_url", url);
            startForegroundService(intent);
        } else {
            Toast.makeText(this, getString(R.string.download_error), Toast.LENGTH_LONG).show();
        }

        finish();
    }

}
