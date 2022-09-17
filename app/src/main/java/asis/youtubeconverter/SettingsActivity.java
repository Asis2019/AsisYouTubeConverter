package asis.youtubeconverter;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

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
        private final int OPEN_DIRECTORY_REQUEST_CODE = 42070;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            sp = getPreferenceScreen().getSharedPreferences();

            Preference downloadFolderPreference = findPreference("download_folder");
            if (downloadFolderPreference != null) {
                updatePreferenceSummery();
                downloadFolderPreference.setOnPreferenceClickListener((Preference.OnPreferenceClickListener) preference -> {
                    openDirectoryChooser();
                    return true;
                });
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            if (requestCode == OPEN_DIRECTORY_REQUEST_CODE && data != null && resultCode == Activity.RESULT_OK) {
                Uri uri = data.getData();
                getContext().getContentResolver().takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                );
                updateDefaultDownloadLocation(uri.toString());
            }
        }

        private void updateDefaultDownloadLocation(String path) {
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("download_folder", path).apply();
            updatePreferenceSummery();
        }

        private void updatePreferenceSummery() {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            String downloadDir = sharedPreferences.getString("download_folder", null);

            Preference downloadFolderPreference = findPreference("download_folder");
            if (downloadFolderPreference != null) {
                if (downloadDir == null) {
                    downloadFolderPreference.setSummary(getString(R.string.default_download_path));
                } else {
                    String docId = DocumentsContract.getTreeDocumentId(Uri.parse(downloadDir));
                    downloadFolderPreference.setSummary(docId);
                }

            }
        }

        private void openDirectoryChooser() {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(intent, OPEN_DIRECTORY_REQUEST_CODE);
        }
    }
}