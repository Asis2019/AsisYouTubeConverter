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
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import asis.youtubeconverter.BuildConfig;
import asis.youtubeconverter.R;
import kotlin.Unit;
import kotlin.jvm.functions.Function3;

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

        File tmpFolder;
        try {
            tmpFolder = File.createTempFile("AYTC", null, getCacheDir());
            tmpFolder.delete();
            tmpFolder.mkdir();
            tmpFolder.deleteOnExit();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        request.addOption("--no-check-certificate");
        request.addOption("--no-mtime");
        request.addOption("-o", tmpFolder.getAbsolutePath() + "/%(title)s.%(ext)s");
        request.addOption("-f", "ba");
        request.addOption("-x");
        request.addOption("--audio-format", "mp3");
        request.addOption("--audio-quality", "0");

        createAndRunDownloadTask(request, url, serviceId, tmpFolder);
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

    private void createAndRunDownloadTask(YoutubeDLRequest request, String url, int serviceId, File tmpFolder) {
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

        final Function3<Float, Long, String, Unit> callback = (progress, etaInSeconds, line) -> {
            boolean isIndeterminate = line.contains("[ExtractAudio]") || progress <= 0;
            notification
                    .setContentTitle(videoTitle[0])
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(line))
                    .setProgress(100, progress.intValue(), isIndeterminate);
            updateNotification(notification.build(), serviceId, getApplicationContext());
            return null;
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

                moveDownloadedFile(tmpFolder);
            } catch (YoutubeDLException | InterruptedException | YoutubeDL.CanceledException e) {
                if (BuildConfig.DEBUG) e.printStackTrace();

                notificationThreadComplete
                        .setContentTitle(url)
                        .setSmallIcon(R.drawable.ic_close);
                if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                    Intent broadcastIntent = new Intent(getApplicationContext(), ErrorBroadcastReceiver.class);
                    broadcastIntent.putExtra("error_message", getString(R.string.download_error));
                    sendBroadcast(broadcastIntent);

                    notificationThreadComplete.setContentText(getString(R.string.download_failed));
                    notificationThreadComplete.setStyle(new NotificationCompat
                            .BigTextStyle().bigText(getString(R.string.download_failed) + getString(R.string.download_failed_description)));
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
                cleanUp(serviceId, tmpFolder);
            }
        });
        thread.start();
    }

    private void moveDownloadedFile(File tmpFolder) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String downloadDir = sharedPreferences.getString("download_folder", null);

        File[] files = tmpFolder.listFiles();
        if (files == null) return;

        for (File file : files) {
            try {
                FileInputStream ins = new FileInputStream(file);
                OutputStream ops;

                if (downloadDir != null) {
                    ops = getOutputStream(downloadDir, file);
                } else {
                    File finalFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), file.getName());
                    ops = new FileOutputStream(finalFile);
                }

                IOUtils.copy(ins, ops);
                IOUtils.closeQuietly(ops);
                IOUtils.closeQuietly(ins);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private OutputStream getOutputStream(String downloadDirUri, File file) throws FileNotFoundException {
        Uri treeUri = Uri.parse(downloadDirUri);
        String docId = DocumentsContract.getTreeDocumentId(treeUri);
        Uri destDir = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId);

        Uri destUri = DocumentsContract.createDocument(
                getApplicationContext().getContentResolver(),
                destDir,
                "*/*",
                file.getName()
        );

        return getApplicationContext().getContentResolver().openOutputStream(destUri);
    }
}
