package com.example.task1readbeacon;

import static com.example.task1readbeacon.EddystoneScannerService.mScanCallback;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanSettings;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

@SuppressLint("StaticFieldLeak")
public class MainActivity extends AppCompatActivity implements
        ServiceConnection {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final String LOG_TAG = "eddystonescan";
    private static final int EXPIRE_TIMEOUT = 5000;
    private static final int EXPIRE_TASK_PERIOD = 1000;

    private EddystoneScannerService mService;
    private static ArrayList<BeaconData> mAdapterItems;

    private static TextView beaconID;
    private static TextView beaconURL;
    private static TextView beaconVoltage;
    private static TextView beaconTemperature;
    private static TextView beaconDistance;

    private BluetoothLeScanner mBluetoothLeScanner;

    private static final String EDDYSTONE_UUID = "0000feaa-0000-1000-8000-00805f9b34fb";
    private static final ParcelUuid UID_SERVICE =
            ParcelUuid.fromString(EDDYSTONE_UUID);

    @SuppressLint("MissingPermission")
    private void checkPermissions() {
        String[] permissions = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.i(LOG_TAG, "Location denied");
                requestPermissions(permissions, PERMISSION_REQUEST_COARSE_LOCATION);
            } else {
                Log.i(LOG_TAG, "Will get scanner");
                BluetoothManager manager =
                        (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
                mBluetoothLeScanner = manager.getAdapter().getBluetoothLeScanner();

                ScanSettings settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();

                mBluetoothLeScanner.startScan(null, settings, mScanCallback);
                Log.d(TAG, "Scanning started…");
            }
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_COARSE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(LOG_TAG, "Permission granted");
            } else {
                Log.i(LOG_TAG, "Permission denied");
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAdapterItems = new ArrayList<>();
        beaconID = findViewById(R.id.beaconIDText);
        beaconURL = findViewById(R.id.URLText);
        beaconVoltage = findViewById(R.id.voltageText);
        beaconTemperature = findViewById(R.id.temperatureText);
        beaconDistance = findViewById(R.id.distanceText);

        checkPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkBluetoothStatus()) {
            Log.i(LOG_TAG, "Will set service");


            Intent intent = new Intent(this, EddystoneScannerService.class);
            bindService(intent, this, BIND_AUTO_CREATE);

            mHandler.post(mPruneTask);

        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mPruneTask);

        unbindService(this);
    }

    private final Handler mHandler = new Handler();
    private final Runnable mPruneTask = new Runnable() {
        @Override
        public void run() {
            final ArrayList<BeaconData> expiredBeacons = new ArrayList<>();
            final long now = System.currentTimeMillis();
            for (BeaconData beacon : mAdapterItems) {
                long delta = now - beacon.lastDetectedTimestamp;
                if (delta >= EXPIRE_TIMEOUT) {
                    expiredBeacons.add(beacon);
                }
            }

            if (!expiredBeacons.isEmpty()) {
                Log.d(TAG, "Found " + expiredBeacons.size() + " expired");
                mAdapterItems.removeAll(expiredBeacons);
            }

            mHandler.postDelayed(this, EXPIRE_TASK_PERIOD);
        }
    };

    @SuppressLint("MissingPermission")
    private boolean checkBluetoothStatus() {
        BluetoothManager manager =
                (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager.getAdapter();

        if (adapter == null || !adapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            finish();
            return false;
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "No LE Support.", Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }

        return true;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.i(LOG_TAG, "Connected to scanner service");
        mService = ((EddystoneScannerService.LocalBinder) service).getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.d(TAG, "Disconnected from scanner service");
        mService = null;
    }

    // Set beacon ID and distance
    @SuppressLint("SetTextI18n")
    public static void onBeaconIdentifier(String deviceAddress, int rssi, String instanceId, double distance) {
        Log.i(LOG_TAG, "Got beacon");
        final long now = System.currentTimeMillis();
        beaconID.setText(instanceId);
        beaconDistance.setText(distance + " m");
    }

    // Set beacon voltage and temperature
    @SuppressLint("SetTextI18n")
    public static void onBeaconTelemetry(String deviceAddress, float battery, float temperature) {
                beaconVoltage.setText(battery + " mV");
                beaconTemperature.setText(temperature + " ºC");
    }

    // Set beacon URL
    public static void onBeaconURL(String deviceAddress, String url) {
        Log.i(LOG_TAG, "Will display URL");
                beaconURL.setText(String.valueOf(url));
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onDestroy() {
        super.onDestroy();

        mBluetoothLeScanner.stopScan(mScanCallback);
        Log.d(TAG, "Scanning stopped…");
    }


}
