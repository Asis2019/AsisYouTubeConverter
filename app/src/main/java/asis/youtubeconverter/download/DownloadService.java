package asis.youtubeconverter.download;

import static asis.youtubeconverter.Utilities.getDownloadLocationString;
import static asis.youtubeconverter.download.DownloadNotificationService.cancelNotification;
import static asis.youtubeconverter.download.DownloadNotificationService.getNotificationBuilder;
import static asis.youtubeconverter.download.DownloadNotificationService.updateNotification;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import com.yausername.ffmpeg.FFmpeg;
import com.yausername.youtubedl_android.DownloadProgressCallback;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import asis.youtubeconverter.BuildConfig;
import asis.youtubeconverter.R;

public class DownloadService extends Service {
    private int activeServiceId;
    private final ArrayList<Integer> activeServiceIds = new ArrayList<>();
    private static final AtomicInteger c = new AtomicInteger(0);

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startId = getID();
        this.activeServiceId = startId;
        activeServiceIds.add(startId);

        if (intent != null) {
            download(intent.getExtras().getString("video_url"), startId);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static int getID() {
        return c.incrementAndGet();
    }

    private void download(String url, int serviceId) {
        YoutubeDLRequest request = new YoutubeDLRequest(url);
        request.addOption("--no-mtime");
        request.addOption("-o", getDownloadLocationString(this) + "/%(title)s.%(ext)s");
        request.addOption("-f", "ba");
        request.addOption("-x");
        request.addOption("--audio-format", "mp3");
        request.addOption("--audio-quality", "0");

        createAndRunDownloadTask(request, url, serviceId);
    }

    private void cleanUp(int serviceId) {
        if (this.activeServiceId == serviceId) {
            stopForeground(true);
        }
        cancelNotification(getApplicationContext(), serviceId);
        activeServiceIds.remove(Integer.valueOf(serviceId));

        if (activeServiceIds.isEmpty()) {
            stopSelf();
        }
    }

    private void createAndRunDownloadTask(YoutubeDLRequest request, String url, int serviceId) {
        //Variables
        final String[] videoTitle = {"Initializing Download"};

        Intent intent = new Intent(getApplicationContext(), DownloadNotificationReceiver.class);
        intent.putExtra("video_url", url);
        intent.putExtra("service_id", serviceId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), serviceId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notification = getNotificationBuilder(getApplicationContext(), videoTitle[0], 0, true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(url));

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH);
        startForeground(serviceId, notification.build());

        final DownloadProgressCallback callback = (progress, etaInSeconds, line) -> {
            boolean isIndeterminate = line.contains("[ExtractAudio]") || (int) progress <= 0;
            notification
                    .setContentTitle(videoTitle[0])
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(line))
                    .setProgress(100, (int) progress, isIndeterminate);
            updateNotification(notification.build(), serviceId, getApplicationContext());
        };

        //Thread code
        Thread thread = new Thread(() -> {
            try {
                YoutubeDL.getInstance().init(this);
                FFmpeg.getInstance().init(this);

                VideoInfo streamInfo = YoutubeDL.getInstance().getInfo(request);
                videoTitle[0] = streamInfo.getTitle();

                notification.addAction(R.drawable.ic_download, getText(android.R.string.cancel), pendingIntent);
                updateNotification(notification.build(), serviceId, getApplicationContext());

                YoutubeDL.getInstance().execute(request, url, callback);
            } catch (YoutubeDLException | InterruptedException e) {
                if (BuildConfig.DEBUG) e.printStackTrace();

                if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                    Intent broadcastIntent = new Intent(getApplicationContext(), ErrorBroadcastReceiver.class);
                    broadcastIntent.putExtra("error_message", "An error occurred while attempting to download");
                    sendBroadcast(broadcastIntent);
                }
            } finally {
                cleanUp(serviceId);
            }
        });
        thread.start();
    }
}
