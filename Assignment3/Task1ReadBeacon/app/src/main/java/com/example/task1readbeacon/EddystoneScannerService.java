package com.example.task1readbeacon;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;


public class EddystoneScannerService extends Service {
    private static final String TAG =
            EddystoneScannerService.class.getSimpleName();

    // Eddystone service parcel UUID
    private static final ParcelUuid UID_SERVICE =
            ParcelUuid.fromString("0000feaa-0000-1000-8000-00805f9b34fb");
    // Eddystone frame types
    private static final byte TYPE_UID = 0x00;
    private static final byte TYPE_URL = 0x10;
    private static final byte TYPE_TLM = 0x20;
    
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    
    private final LocalBinder mBinder = new LocalBinder();
    public class LocalBinder extends Binder {
        public EddystoneScannerService getService() {
            return EddystoneScannerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // Process beacon ID data
    private static void processUidPacket(String deviceAddress, int rssi, String id, double distance) {
        Log.d(TAG, "Eddystone(" + deviceAddress + ") id = " + id + ", distance = " + distance);
        MainActivity.onBeaconIdentifier(deviceAddress, rssi, id, distance);

    }

    // Process beacon battery (voltage) and temperature data
    private static void processTlmPacket(String deviceAddress, float battery, float temp) {
        Log.d(TAG, "Eddystone(" + deviceAddress + ") battery = " + battery
                    + ", temp = " + temp);
        MainActivity.onBeaconTelemetry(deviceAddress, battery, temp);

    }

    // Process beacon URL data
    private static void processUrlPacket(String deviceAddress, String url) {
        Log.d(TAG, "Eddystone(" + deviceAddress + ") url = " + url);
        MainActivity.onBeaconURL(deviceAddress, url);

    }

    // Scan result callback
    static final ScanCallback mScanCallback = new ScanCallback() {
        private final Handler mCallbackHandler =
                new Handler(Looper.getMainLooper());

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            processResult(result);
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.w(TAG, "Scan Error Code: " + errorCode);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                processResult(result);
            }
        }

        private void processResult(ScanResult result) {
            byte[] data = result.getScanRecord().getServiceData(UID_SERVICE);
            if (data == null) {
                Log.w(TAG, "Invalid Eddystone scan result.");
                return;
            }

            final String deviceAddress = result.getDevice().getAddress();
            final int rssi = result.getRssi();
            byte frameType = data[0];
            Log.i(TAG, "FRAME TYPE = " + String.valueOf(frameType));
            switch (frameType) {
                // Beacon ID and distance
                case TYPE_UID:
                    Log.i(TAG, "Will get ID");
                    final String id = BeaconData.getInstanceId(data);
                    final double distance = BeaconData.getDistance(rssi);
                    mCallbackHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            processUidPacket(deviceAddress, rssi, id, distance);
                        }
                    });
                    break;

                // Voltage and temperature
                case TYPE_TLM:
                    Log.i(TAG, "Will get voltage and temperature");
                    final float battery = BeaconData.getTlmVoltage(data);
                    final float temp = BeaconData.getTlmTemperature(data);
                    mCallbackHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            processTlmPacket(deviceAddress, battery, temp);
                        }
                    });
                    break;

                    // Beacon URL
                case TYPE_URL:
                    Log.i(TAG, "Will get URL");
                    final String url = BeaconData.getURL(data);
                    mCallbackHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            processUrlPacket(deviceAddress, url);
                        }
                    });
                    break;
                default:
                    Log.w(TAG, "Invalid Eddystone scan result.");
            }
        }
    };
}
