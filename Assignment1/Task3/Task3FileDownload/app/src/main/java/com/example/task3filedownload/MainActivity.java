package com.example.task3filedownload;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener{
    private TextView currentStatus;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                String filePath = bundle.getString(DownloadFileService.FILEPATH);
                int result = bundle.getInt(DownloadFileService.RESULT);
                if (result == RESULT_OK) {
                    currentStatus.setText("Download finished");
                    Toast.makeText(MainActivity.this,
                            "Download complete.",
                            Toast.LENGTH_SHORT).show();
                } else {
                    currentStatus.setText("Download failed");
                    Toast.makeText(MainActivity.this, "Download failed.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button downloadButton = (Button)findViewById(R.id.downloadButton);
        currentStatus = (TextView) findViewById(R.id.currentStatus);
        registerReceiver(receiver, new IntentFilter(
                DownloadFileService.NOTIFICATION)); // register broadcast receiver
        downloadButton.setOnClickListener(this);  // set download button listener
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);  // unregister receiver when closing app
    }

    public void onClick(View view) {
        currentStatus.setText("Download in progress...");
        Intent intent = new Intent(this, DownloadFileService.class);  // create intent for download
        intent.putExtra(DownloadFileService.FILENAME, "woods.jpg");
        intent.putExtra(DownloadFileService.URL,
                "https://storage.googleapis.com/pod_public/1300/88149.jpg");
        startService(intent);  // call service to begin downloading
    }

}