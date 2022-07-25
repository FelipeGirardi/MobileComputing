package com.example.bleweatherapp;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.function.Consumer;

@SuppressLint({"MissingPermission", "SetTextI18n"})
@RequiresApi(api = Build.VERSION_CODES.N)
public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final String LOG_TAG = "weathercntrl";
    private static final String DEVICE_NAME = "IPVSWeather";
    private static final int SCAN_PERIOD = 3000;
    private static final UUID SERVICE_UUID = UUID.fromString("00000002-0000-0000-FDFD-FDFDFDFDFDFD");
    private static final UUID TEMP_CHARACTERISTIC_UUID = UUID.fromString("00002a1c-0000-1000-8000-00805f9b34fb");
    private static final UUID HUMID_CHARACTERISTIC_UUID = UUID.fromString("00002a6f-0000-1000-8000-00805f9b34fb");
    private static final String TEMP_VALUE_KEY = "TEMPERATURE_TEXT";
    private static final String HUMID_VALUE_KEY = "HUMIDITY_TEXT";
    private static final String READ_DATA_AVAILABLE = "Read data available";
    private static final String DID_READ_DATA_TEMP = "Did read temperature data";
    private static final String DID_READ_DATA_HUMID = "Did read humidity data";

    private boolean connected = false;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic temperatureCharacteristic;
    private BluetoothGattCharacteristic humidityCharacteristic;
    private List<BluetoothGattCharacteristic> characs = new ArrayList<BluetoothGattCharacteristic>();

    private TextView temperatureValue;
    private TextView humidityValue;

    private final Handler uiHandler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message msg) {
            String message = msg.getData().getString(TEMP_VALUE_KEY);
            if (message != null) {
                temperatureValue.setText(message);
            }
            message = msg.getData().getString(HUMID_VALUE_KEY);
            if (message != null) {
                humidityValue.setText(message);
            }
        }
    };

    private void sendMessage(String key, String message) {
        Message msg = uiHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString(key, message);
        msg.setData(bundle);
        uiHandler.sendMessage(msg);
    }

    private void setTemperature(String temperature) {
        sendMessage(TEMP_VALUE_KEY, String.format("%sºC", temperature));
    }

    private void setHumidity(String humidity) {
        sendMessage(HUMID_VALUE_KEY, String.format("%s%%", humidity));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        temperatureValue = findViewById(R.id.tempNumber);
        humidityValue = findViewById(R.id.humidityNumber);

        Log.i(LOG_TAG, "Checking permissions");
        checkPermissions();
    }

    private void checkPermissions() {
        String[] permissions = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION};
        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.i(LOG_TAG, "Location not granted");
            requestPermissions(permissions, PERMISSION_REQUEST_COARSE_LOCATION);
        } else {
            scanDevices();
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_COARSE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scanDevices();
            } else {
                Log.i(LOG_TAG, "Permission denied");
            }
        }
    }

    private void scanDevices() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        BluetoothDevice foundBondedDevice = null;
        for (BluetoothDevice dev : bluetoothAdapter.getBondedDevices()) {
            String deviceName = dev.getName();
            if (deviceName != null && deviceName.equals(DEVICE_NAME)) {
                foundBondedDevice = dev;
                Log.i(LOG_TAG, String.format("found %s in bonded devices", DEVICE_NAME));
            }
        }

        if (foundBondedDevice == null) {
            this.scanLeDevices((devices) -> {
                BluetoothDevice foundScannedDevice = null;
                for (BluetoothDevice dev : devices) {
                    String deviceName = dev.getName();
                    if (deviceName == null) {
                        deviceName = "N/A";
                    }

                    if (deviceName.equals(DEVICE_NAME)) {
                        foundScannedDevice = dev;
                    }

                    Log.i(LOG_TAG, String.format("Device name: %s, address: %s", deviceName, dev.getAddress()));
                }

                if (foundScannedDevice == null) {
                    Log.i(LOG_TAG, String.format("Device %s not found", DEVICE_NAME));
                } else {
                    connectToDevice(foundScannedDevice);
                }
            });
        } else {
            connectToDevice(foundBondedDevice);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void scanLeDevices(Consumer<List<BluetoothDevice>> onFinish) {
        Handler handler = new Handler();

        List<BluetoothDevice> leDevices = new ArrayList<>();
        ScanCallback scanCallback = new ScanCallback() {

            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                final int rssi = result.getRssi();

                BluetoothDevice bluetoothDevice = result.getDevice();

                if (!leDevices.contains(bluetoothDevice)) {
                    leDevices.add(bluetoothDevice);
                }
            }
        };

        handler.postDelayed(() -> {
            Log.i(LOG_TAG, "Stopping scan");
            bluetoothLeScanner.stopScan(scanCallback);
            onFinish.accept(leDevices);
        }, SCAN_PERIOD);

        Log.i(LOG_TAG, "Starting scan");
        bluetoothLeScanner.startScan(scanCallback);
    }

    private void connectToDevice(BluetoothDevice device) {
        BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);

                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(LOG_TAG, "GATT connected");
                    gatt.discoverServices();
                    bluetoothGatt = gatt;
                }

                if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(LOG_TAG, "GATT disconnected");
                    bluetoothGatt = null;
                    connected = false;
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i(LOG_TAG, "Services discovered");
                    BluetoothGattService service = gatt.getService(SERVICE_UUID);

                    if (service == null) {
                        Log.i(LOG_TAG, "Could not get service");
                    } assert(service != null);

                    temperatureCharacteristic = service.getCharacteristic(TEMP_CHARACTERISTIC_UUID);
                    if (temperatureCharacteristic == null) {
                        Log.i(LOG_TAG, "Could not get temperature characteristic");
                    }
                    Log.i(LOG_TAG, "Did get temp charac");
                    humidityCharacteristic = service.getCharacteristic(HUMID_CHARACTERISTIC_UUID);
                    if (humidityCharacteristic == null) {
                        Log.i(LOG_TAG, "Could not get humidity characteristic");
                    }
                    Log.i(LOG_TAG, "Did get humidity charac");

                    characs.add(temperatureCharacteristic);
                    characs.add(humidityCharacteristic);

                    connected = true;
                    readValues();
                    Timer timer = new Timer();
                    TimerTask myTask = new TimerTask() {
                        @Override
                        public void run() {
                            readValues();
                        }
                    };

                    timer.schedule(myTask, 2000, 2000);
                } else {
                    Log.e(LOG_TAG, String.format("gatt status: %d", status));
                }
            }

            @Override
            public void onCharacteristicRead(
                    BluetoothGatt gatt,
                    BluetoothGattCharacteristic characteristic,
                    int status
            ) {
                Log.i(LOG_TAG, "onCharacRead");
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    broadcastUpdate(characteristic);
                }
            }
        };

        device.connectGatt(this, false, gattCallback);
    }

    private void readValues() {
        Log.i(LOG_TAG, "Will read values");
        if (connected) {
            bluetoothGatt.readCharacteristic(characs.get(characs.size() - 1));
        }
    }

    private void broadcastUpdate(BluetoothGattCharacteristic characteristic) {
        Log.i(LOG_TAG, "Will broadcast update");
        Intent intent = new Intent(MainActivity.READ_DATA_AVAILABLE);
        UUID characteristicUUID = characteristic.getUuid();
        Log.i(LOG_TAG, characteristicUUID.toString());

        // update text with new temperature or humidity data
        if (characteristicUUID.equals(TEMP_CHARACTERISTIC_UUID)) {
            Log.i(LOG_TAG, "Entered temp broadcast");
            float temperatureData;
            if (characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT, 1) == null) {
                Log.i(LOG_TAG, "Temperature is null");
            } else {
                temperatureData = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT, 1);
                Log.d(LOG_TAG, String.format("Received temperature: %f", temperatureData));
                intent.putExtra(DID_READ_DATA_TEMP, temperatureData);
                String tempString = String.valueOf(temperatureData);
                setTemperature(tempString);
            }
        } else if (characteristicUUID.equals(HUMID_CHARACTERISTIC_UUID)) {
            Log.i(LOG_TAG, "Entered humid broadcast");
            int humidData;
            if (characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1) == null) {
                Log.i(LOG_TAG, "Humidity is null");
            } else {
                humidData = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1);
                Log.d(LOG_TAG, String.format("Received humidity: %d", humidData));
                intent.putExtra(DID_READ_DATA_HUMID, humidData);
                String humidString = String.valueOf(humidData);
                setHumidity(humidString);
            }
            sendBroadcast(intent);

            characs.remove(characs.get(characs.size() - 1));

            if (characs.size() > 0) {
                readValues();
            } else {
                bluetoothGatt.disconnect();
            }
        }
    }
}