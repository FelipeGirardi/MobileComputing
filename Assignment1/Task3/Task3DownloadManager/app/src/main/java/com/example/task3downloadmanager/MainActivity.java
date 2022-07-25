package com.example.task3downloadmanager;

import androidx.appcompat.app.AppCompatActivity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private Button downloadButton;
    private long downloadId;

    private BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);  // retrieve download ID from the Intent
            if (downloadId == id) {
                Toast.makeText(MainActivity.this, "Download finished.", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("File Downloader");
        downloadButton = findViewById(R.id.downloadButton);
        registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));  // registers broadcast receiver for the moment when download is finished

        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                executeDownload();
            }
        });
    }

    private void executeDownload() {
        String url = "https://effigis.com/wp-content/uploads/2015/02/DigitalGlobe_WorldView2_50cm_8bit_Pansharpened_RGB_DRA_Rome_Italy_2009DEC10_8bits_sub_r_1.jpg";  // image 25MB in size
        String fileName = "satellite-bild-rom.jpg";
        File file = new File(fileName);  // creates file with a name

        DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);  // instantiates download manager with download service
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url))  // creates download request
                .setDestinationUri(Uri.fromFile(file))
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                fileName)  // saves downloaded file in Downloads
                .setTitle(fileName) // set file name
                .setMimeType("application/jpg");  // sets file type

        downloadId = downloadManager.enqueue(request);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(onDownloadComplete);
    }
}