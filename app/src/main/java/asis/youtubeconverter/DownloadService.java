package asis.youtubeconverter;

import static asis.youtubeconverter.DownloadNotificationService.cancelNotification;
import static asis.youtubeconverter.DownloadNotificationService.getNotificationBuilder;
import static asis.youtubeconverter.DownloadNotificationService.showNotification;
import static asis.youtubeconverter.DownloadNotificationService.updateNotification;
import static asis.youtubeconverter.Utilities.getDownloadLocationString;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.yausername.youtubedl_android.DownloadProgressCallback;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import java.util.concurrent.atomic.AtomicInteger;

public class DownloadService extends Service {
    private static final AtomicInteger c = new AtomicInteger(0);

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        download(intent.getExtras().getString("video_url"));
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

    private void download(String url) {
        YoutubeDLRequest request = new YoutubeDLRequest(url);
        request.addOption("--no-mtime");
        request.addOption("-o", getDownloadLocationString(this) + "/%(title)s.%(ext)s");
        request.addOption("-f", "ba");
        request.addOption("-x");
        request.addOption("--audio-format", "mp3");
        request.addOption("--audio-quality", "0");

        createAndRunDownloadTask(request, url);
    }

    private void cleanUp() {
        stopSelf();
    }

    private void createAndRunDownloadTask(YoutubeDLRequest request, String url) {
        Thread thread = new Thread(new Runnable() {
            private final int serviceId = getID();
            private String videoTitle = "Downloading";
            private PendingIntent pendingIntent;

            private final DownloadProgressCallback callback = (progress, etaInSeconds, line) -> {
                boolean isIndeterminate = line.contains("[ExtractAudio]") || (int) progress <= 0;

                Notification notification = getNotificationBuilder(getApplicationContext(), "", (int) progress, isIndeterminate)
                        .setContentTitle(videoTitle)
                        .setContentText("ETA " + etaInSeconds)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(line))
                        .addAction(
                                R.drawable.ic_download,
                                getText(android.R.string.cancel),
                                this.pendingIntent
                        )
                        .build();
                updateNotification(notification, this.serviceId, getApplicationContext());
            };

            @Override
            public void run() {
                Intent intent = new Intent(getApplicationContext(), DownloadNotificationReceiver.class);
                intent.putExtra("video_url", url);
                intent.putExtra("notification_id", this.serviceId);
                pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), this.serviceId, intent, PendingIntent.FLAG_IMMUTABLE);

                try {
                    Notification notification = getNotificationBuilder(getApplicationContext(), this.videoTitle, 0, true)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(url))
                            .addAction(
                                    R.drawable.ic_download,
                                    getText(android.R.string.cancel),
                                    this.pendingIntent
                            )
                            .build();
                    showNotification(getApplicationContext(), notification, this.serviceId);

                    VideoInfo streamInfo = YoutubeDL.getInstance().getInfo(url);
                    this.videoTitle = streamInfo.getTitle();

                    YoutubeDL.getInstance().execute(request, url, callback);
                } catch (YoutubeDLException | InterruptedException ignore) {
                } finally {
                    cancelNotification(getApplicationContext(), this.serviceId);
                    cleanUp();
                }
            }
        });
        thread.start();
    }
}
