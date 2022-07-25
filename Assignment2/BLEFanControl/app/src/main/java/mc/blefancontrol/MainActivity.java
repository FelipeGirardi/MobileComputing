package mc.blefancontrol;

import androidx.annotation.NonNull;
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
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@SuppressLint({"MissingPermission", "SetTextI18n"})
public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    private static final String LOG_TAG = "fancntrl";

    private static final String DEVICE_NAME = "IPVS-LIGHT"; //"rpi-ble";
    private static final UUID SERVICE_UUID = UUID.fromString("00000001-0000-0000-FDFD-FDFDFDFDFDFD");
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("10000001-0000-0000-FDFD-FDFDFDFDFDFD");

    private static final int SCAN_PERIOD = 5000;
    private BluetoothLeScanner bluetoothLeScanner;

    private boolean readyToWrite = false;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic bluetoothCharacteristic;

    private TextView connectionStatusView;
    private TextView intensityView;

    private static final String MSG_STATUS_KEY = "STATUS_TEXT";
    private static final String MSG_EXIT_KEY = "EXIT_TEXT";
    private static final String MSG_INTENSITY_KEY = "INTENSITY_TEXT";

    private final Handler mainHandler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message msg) {
            String message = msg.getData().getString(MSG_STATUS_KEY);
            if (message != null) {
                connectionStatusView.setText(message);
            }
            message = msg.getData().getString(MSG_EXIT_KEY);
            if (message != null) {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                finishAffinity();
            }
            message = msg.getData().getString(MSG_INTENSITY_KEY);
            if (message != null) {
                intensityView.setText(message);
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

    private void setStatus(String status) {
        sendMessage(MSG_STATUS_KEY, status);
    }

    private void setIntensity(int intensity) {
        sendMessage(MSG_INTENSITY_KEY, String.format("%s%%", intensity));
    }

    private void exit(String message) {
        sendMessage(MSG_EXIT_KEY, message);
    }

    private void writeIntensity(int intensity) {
        if (readyToWrite) {
            int value = (int) ((intensity / 100.0) * 65535);

            bluetoothCharacteristic.setValue(value, BluetoothGattCharacteristic.FORMAT_UINT16, 0);
            bluetoothGatt.writeCharacteristic(bluetoothCharacteristic);
            setIntensity(intensity);
        } else {
            Log.w(LOG_TAG, "writeIntensity() called before readyToWrite = true");
        }
    }

    private void checkPermissions() {
        String[] permissions = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION};
        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.i(LOG_TAG, "coarse not granted");
            requestPermissions(permissions, PERMISSION_REQUEST_COARSE_LOCATION);
        } else {
            runAfterPermissionCheck();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectionStatusView = findViewById(R.id.connectionStatus);
        intensityView = findViewById(R.id.intensityValue);
        SeekBar intensitySeekBar = findViewById(R.id.IntensitySeekBar);


        intensitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int latestValue = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                latestValue = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                writeIntensity(latestValue);
            }
        });

        Log.i(LOG_TAG, "=== FAN CONTROL ===");
        checkPermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_COARSE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                runAfterPermissionCheck();
            } else {
                exit("Permission denied");
            }
        }
    }

    private void runAfterPermissionCheck() {
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

                    Log.i(LOG_TAG, String.format("name: %s address: %s", deviceName, dev.getAddress()));
                }

                if (foundScannedDevice == null) {
                    exit(String.format("device %s not found", DEVICE_NAME));
                } else {
                    connectToDevice(foundScannedDevice);
                }
            });
        } else {
            connectToDevice(foundBondedDevice);
        }
    }

    private void scanLeDevices(Consumer<List<BluetoothDevice>> onFinish) {
        Handler handler = new Handler();

        List<BluetoothDevice> leDevices = new ArrayList<>();
        ScanCallback scanCallback = new ScanCallback() {

            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);

                BluetoothDevice bluetoothDevice = result.getDevice();

                if (!leDevices.contains(bluetoothDevice)) {
                    leDevices.add(bluetoothDevice);
                }
            }
        };

        handler.postDelayed(() -> {
            Log.i(LOG_TAG, "stopping scan");
            bluetoothLeScanner.stopScan(scanCallback);
            onFinish.accept(leDevices);
        }, SCAN_PERIOD);

        setStatus("Scanning");
        Log.i(LOG_TAG, "starting scan");
        bluetoothLeScanner.startScan(scanCallback);
    }

    private void connectToDevice(BluetoothDevice device) {
        setStatus("Connecting");

        BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);

                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    setStatus("Connected");
                    Log.i(LOG_TAG, "GATT connected");
                    gatt.discoverServices();
                    bluetoothGatt = gatt;
                }

                if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    setStatus("Disconnected");
                    Log.i(LOG_TAG, "GATT disconnected");
                    bluetoothGatt = null;
                    readyToWrite = false;
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i(LOG_TAG, "services discovered");
                    BluetoothGattService service = gatt.getService(SERVICE_UUID);
                    if (service == null) {
                        exit("could not get service");
                    }
                    assert (service != null);

                    bluetoothCharacteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
                    if (bluetoothCharacteristic == null) {
                        exit("could not get characteristic");
                    }

                    readyToWrite = true;
                    writeIntensity(0);
                } else {
                    Log.e(LOG_TAG, String.format("gatt status: %d", status));
                }
            }
        };

        device.connectGatt(this, false, gattCallback);
    }

}