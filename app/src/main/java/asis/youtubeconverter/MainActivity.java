package asis.youtubeconverter;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;

import asis.youtubeconverter.download.DownloadService;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private EditText editTextVideoUrl;
    private Button downloadButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawer);

        initDrawer();
        initViews();
        checkAndRequestStoragePermission();

        downloadButton.setOnClickListener(view -> download());

        TextInputLayout textInputLayout = findViewById(R.id.edittext_layout);
        textInputLayout.setEndIconOnClickListener(view -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = clipboard.getPrimaryClip();
            editTextVideoUrl.setText(clip.getItemAt(0).getText().toString());
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (itemId == R.id.action_version) {
            showVersionInfoDialog();
        } else if (itemId == R.id.action_update) {
            update();
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void initDrawer() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.Drawer);
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void initViews() {
        editTextVideoUrl = findViewById(R.id.etUrl);
        downloadButton = findViewById(R.id.action_downloadb);
    }

    private void showVersionInfoDialog() {
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle(R.string.app_name);
        alertDialog.setMessage(String.format(getString(R.string.version_message), BuildConfig.VERSION_NAME));
        alertDialog.setIcon(R.mipmap.ic_launcher);
        alertDialog.show();
    }

    private void checkAndRequestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    private void download() {
        String url = editTextVideoUrl.getText().toString().trim();
        if (TextUtils.isEmpty(url)) {
            Snackbar.make(editTextVideoUrl, R.string.url_required, Snackbar.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, DownloadService.class);
        intent.putExtra("video_url", url);
        startForegroundService(intent);
    }

    private void update() {
        try {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            YoutubeDL.getInstance().init(this);
            YoutubeDL.UpdateStatus status = YoutubeDL.getInstance().updateYoutubeDL(this, YoutubeDL.UpdateChannel._STABLE);
            if (status == YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE) {
                Toast.makeText(MainActivity.this, R.string.ytdlp_already_updated, Toast.LENGTH_LONG).show();
            } else if (status == YoutubeDL.UpdateStatus.DONE) {
                Toast.makeText(MainActivity.this, R.string.ytdlp_updated, Toast.LENGTH_LONG).show();
            }
        } catch (YoutubeDLException e) {
            Toast.makeText(MainActivity.this, R.string.ytdlp_update_failed, Toast.LENGTH_LONG).show();
        }
    }
}