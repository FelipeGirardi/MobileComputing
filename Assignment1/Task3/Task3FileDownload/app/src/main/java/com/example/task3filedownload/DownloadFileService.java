package com.example.task3filedownload;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

public class DownloadFileService extends IntentService {
    public DownloadFileService() {
        super("DownloadFileService");
    }

    public static final String FILENAME = "filename";
    public static final String FILEPATH = "filepath";
    public static final String URL = "url";
    public static final String NOTIFICATION = "notification";
    public static final String RESULT = "result";
    private int result = Activity.RESULT_CANCELED;

    // broadcast result of download to show to user
    private void broadcastResult(String filePath, int result) {
        Intent intent = new Intent(NOTIFICATION);
        intent.putExtra(FILEPATH, filePath);
        intent.putExtra(RESULT, result);
        sendBroadcast(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String urlPath = intent.getStringExtra(URL);
        String fileName = intent.getStringExtra(FILENAME);
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                fileName);  // create file to be saved
        if (file.exists()) {  // delete duplicate file
            file.delete();
        }

        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {  // executes download stream
            URL url = new URL(urlPath);
            inputStream = url.openConnection().getInputStream();
            InputStreamReader inputReader = new InputStreamReader(inputStream);
            outputStream = new FileOutputStream(file.getPath());
            int data = -1;
            while ((data = inputReader.read()) != -1) {
                outputStream.write(data);  // write output data in file
            }
            result = Activity.RESULT_OK;  // finished download successfully

        } catch (Exception exc) {
            exc.printStackTrace();
        } finally {  // close input and output streams
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException exc) {
                    exc.printStackTrace();
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException exc) {
                    exc.printStackTrace();
                }
            }
        }
        broadcastResult(file.getAbsolutePath(), result);
    }
}