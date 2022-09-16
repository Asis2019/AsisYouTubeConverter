package asis.youtubeconverter;

import static asis.youtubeconverter.Utilities.getDownloadLocationName;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.io.File;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        private SharedPreferences sp;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            sp = getPreferenceScreen().getSharedPreferences();

            Preference downloadFolderPreference = findPreference("download_folder");
            if (downloadFolderPreference != null) {
                downloadFolderPreference.setSummary(getDownloadLocationName(getContext()));

                downloadFolderPreference.setOnPreferenceClickListener((Preference.OnPreferenceClickListener) preference -> {
                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    i.addCategory(Intent.CATEGORY_DEFAULT);
                    startActivityForResult(Intent.createChooser(i, "Choose directory"), 9999);
                    return true;
                });
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            if (requestCode == 9999 && data != null) {
                SharedPreferences.Editor editor = sp.edit();

                String path = data.getDataString();
                File file = new File(path);
                editor.putString("download_folder", file.getAbsolutePath());
                editor.apply();

                Preference downloadFolderPreference = findPreference("download_folder");
                if (downloadFolderPreference != null) {
                    downloadFolderPreference.setSummary(getDownloadLocationName(getContext()));
                }
            }
        }
    }
}