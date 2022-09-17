package asis.youtubeconverter.download;

import static asis.youtubeconverter.download.DownloadNotificationService.getNotificationBuilder;
import static asis.youtubeconverter.download.DownloadNotificationService.updateNotification;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.provider.DocumentsContract;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.preference.PreferenceManager;

import com.yausername.ffmpeg.FFmpeg;
import com.yausername.youtubedl_android.DownloadProgressCallback;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import asis.youtubeconverter.BuildConfig;
import asis.youtubeconverter.R;

public class DownloadService extends Service {
    private int activeServiceId;
    private final ArrayList<Integer> activeServiceIds = new ArrayList<>();
    private static final AtomicInteger c = new AtomicInteger(0);

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

        File tmpFile;
        try {
            tmpFile = File.createTempFile("AYTC", null, getCacheDir());
            tmpFile.delete();
            tmpFile.mkdir();
            tmpFile.deleteOnExit();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        request.addOption("--no-mtime");
        request.addOption("-o", tmpFile.getAbsolutePath() + "/%(title)s.%(ext)s");
        request.addOption("-f", "ba");
        request.addOption("-x");
        request.addOption("--audio-format", "mp3");
        request.addOption("--audio-quality", "0");

        createAndRunDownloadTask(request, url, serviceId, tmpFile);
    }

    private void cleanUp(int serviceId, File tmpFile) {
        try {
            FileUtils.deleteDirectory(tmpFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (this.activeServiceId == serviceId) {
            stopForeground(true);
        }
        activeServiceIds.remove(Integer.valueOf(serviceId));

        if (activeServiceIds.isEmpty()) {
            stopSelf();
        }
    }

    private void createAndRunDownloadTask(YoutubeDLRequest request, String url, int serviceId, File tmpFile) {
        //Variables
        final String[] videoTitle = {getString(R.string.download_notification_title)};

        Intent intent = new Intent(getApplicationContext(), DownloadNotificationReceiver.class);
        intent.putExtra("video_url", url);
        intent.putExtra("service_id", serviceId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, serviceId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

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
            NotificationCompat.Builder notificationThreadComplete = getNotificationBuilder(getApplicationContext(), videoTitle[0], 0, false);

            try {
                YoutubeDL.getInstance().init(this);
                FFmpeg.getInstance().init(this);

                VideoInfo streamInfo = YoutubeDL.getInstance().getInfo(url);
                videoTitle[0] = streamInfo.getTitle();
                notification.addAction(R.drawable.ic_download, getText(android.R.string.cancel), pendingIntent);

                YoutubeDL.getInstance().execute(request, url, callback);

                notificationThreadComplete
                        .setContentTitle(videoTitle[0])
                        .setSmallIcon(R.drawable.ic_done)
                        .setContentText(getString(R.string.download_complete));

                moveDownloadedFile(tmpFile);
            } catch (YoutubeDLException | InterruptedException e) {
                if (BuildConfig.DEBUG) e.printStackTrace();

                notificationThreadComplete
                        .setContentTitle(url)
                        .setSmallIcon(R.drawable.ic_close);
                if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                    Intent broadcastIntent = new Intent(getApplicationContext(), ErrorBroadcastReceiver.class);
                    broadcastIntent.putExtra("error_message", getString(R.string.download_error));
                    sendBroadcast(broadcastIntent);

                    notificationThreadComplete.setContentText(getString(R.string.download_failed));
                } else if (e.getMessage() != null && e.getMessage().isEmpty()) {
                    notificationThreadComplete.setContentText(getString(R.string.download_canceled));
                }
            } finally {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH);
                notificationThreadComplete
                        .setProgress(0, 0, false)
                        .setOngoing(false)
                        .setSilent(false);
                updateNotification(notificationThreadComplete.build(), serviceId, getApplicationContext());
                cleanUp(serviceId, tmpFile);
            }
        });
        thread.start();
    }

    private void moveDownloadedFile(File tmpFile) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String downloadDir = sharedPreferences.getString("download_folder", null);

        if (downloadDir != null) {
            Uri treeUri = Uri.parse(downloadDir);
            String docId = DocumentsContract.getTreeDocumentId(treeUri);
            Uri destDir = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId);

            for (File file : tmpFile.listFiles()) {
                try {
                    Uri destUri = DocumentsContract.createDocument(
                            getApplicationContext().getContentResolver(),
                            destDir,
                            "*/*",
                            file.getName()
                    );

                    FileInputStream ins = new FileInputStream(file);
                    OutputStream ops = getApplicationContext().getContentResolver().openOutputStream(destUri);

                    IOUtils.copy(ins, ops);
                    IOUtils.closeQuietly(ops);
                    IOUtils.closeQuietly(ins);
                } catch (IOException ignore) {}
            }
            return;
        }

        for (File file : tmpFile.listFiles()) {
            try {
                FileInputStream ins = new FileInputStream(file);

                File finalFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), file.getName());
                FileOutputStream ops = new FileOutputStream(finalFile);

                IOUtils.copy(ins, ops);
                IOUtils.closeQuietly(ops);
                IOUtils.closeQuietly(ins);
            } catch (IOException ignore) {}
        }
    }
}
