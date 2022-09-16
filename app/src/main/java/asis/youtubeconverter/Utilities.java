package asis.youtubeconverter;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import java.io.File;

public class Utilities {
    @NonNull
    public static File getDownloadLocation(Context context) {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File defaultDirectory = new File(downloadsDir, "AsisYouTubeConverter");

        //TODO: Fix downloads not working when using selected URI.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String storageLocation = sharedPreferences.getString("download_folder", defaultDirectory.getAbsolutePath());
        File downloadLocation = new File(storageLocation);

        if (!downloadLocation.exists()) {
            downloadLocation.mkdir();
        }
        return defaultDirectory;
    }

    @NonNull
    public static String getDownloadLocationString(Context context) {
        File downloadLocation = getDownloadLocation(context);
        return downloadLocation.getAbsolutePath();
    }

    public static String getDownloadLocationName(Context context) {
        String baseName = Uri.decode(getDownloadLocation(context).getName());
        if (baseName.contains("primary:")) {
            baseName = baseName.replace("primary:", "");
        }
        return baseName;
    }
}
