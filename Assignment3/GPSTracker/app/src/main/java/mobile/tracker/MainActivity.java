package mobile.tracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import mobile.tracker.service.TrackingService;
import mobile.tracker.service.TrackingServiceCallback;
import mobile.tracker.service.TrackingServiceStatus;


public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private TextView serviceStatusText;
    private TextView locationCountText;
    private Button serviceControlButton;

    private final String MSG_STATUS_KEY = "STATUS_TEXT";
    private final String MSG_LOCATION_COUNT_KEY = "LOCATION_COUNT_TEXT";

    private TrackingService trackingService;
    private boolean serviceBound = false;


    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            TrackingService.TrackingServiceBinder binder = (TrackingService.TrackingServiceBinder) iBinder;
            trackingService = binder.getService();
            serviceBound = true;

            afterServiceBound();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            serviceBound = false;
        }
    };


    private final Handler mainHandler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(@NonNull Message msg) {
            String message = msg.getData().getString(MSG_STATUS_KEY);
            if (message != null) {
                if (message.equals("running")) {
                    serviceStatusText.setText("Service is running");
                    serviceControlButton.setText("stop");
                } else if (message.equals("not running")) {
                    serviceStatusText.setText("Service is not running");
                    serviceControlButton.setText("start");
                }
            }

            message = msg.getData().getString(MSG_LOCATION_COUNT_KEY);
            if (message != null) {
                locationCountText.setText(message);
            }
        }
    };

    private void sendMessage(String key, String message) {
        Message msg = mainHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString(key, message);
        msg.setData(bundle);
        mainHandler.sendMessage(msg);
    }

    public void setServiceStatusText(TrackingServiceStatus status) {
        if (status == TrackingServiceStatus.RUNNING) {
            sendMessage(MSG_STATUS_KEY, "running");
        }
        if (status == TrackingServiceStatus.NOT_RUNNING) {
            sendMessage(MSG_STATUS_KEY, "not running");
        }
    }

    public void setLocationCountText(int count) {
        sendMessage(MSG_LOCATION_COUNT_KEY, String.valueOf(count));
    }

    private final TrackingServiceCallback serviceCallback = new TrackingServiceCallback() {

        @Override
        public void onServiceStatusChange(TrackingServiceStatus status) {
            Log.d(AppConfig.TAG, "service status changed: " + status.toString());

            setServiceStatusText(status);
        }

        @Override
        public void onNewLocationCount(int count) {
            Log.d(AppConfig.TAG, "location count changed");
            setLocationCountText(count);
        }
    };

    private final View.OnClickListener serviceControlListener = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            if (serviceBound) {
                Intent intent = new Intent(MainActivity.this, TrackingService.class);
                switch (trackingService.getStatus()) {
                    case RUNNING:
                        stopService(intent);
                        unbindService(serviceConnection);
                        break;
                    case NOT_RUNNING:
                        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
                        startService(intent);
                        break;
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serviceStatusText = findViewById(R.id.serviceStatusLabel);
        serviceControlButton = findViewById(R.id.serviceControlButton);
        locationCountText = findViewById(R.id.locationCountText);

        //Handler mainHandler = new Handler(Looper.getMainLooper());

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.i(AppConfig.TAG, "fine location permission not granted, requesting...");
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
        } else {
            afterPermissionsGranted();
        }
    }


    private void afterPermissionsGranted() {
        Log.i(AppConfig.TAG, "afterPermissionGranted");
        //Intent intent = new Intent(this, TrackingService.class);
        //startService(intent);

        Intent bindingIntent = new Intent(this, TrackingService.class);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void afterServiceBound() {
        setServiceStatusText(trackingService.getStatus());
        trackingService.setCallback(serviceCallback);
        serviceControlButton.setOnClickListener(serviceControlListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (serviceBound) {
            trackingService.removeCallback();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_FINE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                afterPermissionsGranted();
            }
        }
    }
}