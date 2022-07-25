package com.example.task1readbeacon;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;

import java.nio.charset.StandardCharsets;

// Beacon data model
public class BeaconData {
    private static final String TAG = BeaconData.class.getSimpleName();

    public String deviceAddress;
    public String id;
    public int latestRssi;
    public long lastDetectedTimestamp;

    public float voltage;
    public float temperature;
    public String url;
    public double distance;

    public BeaconData(String address, int rssi, String identifier, long timestamp) {
        this.deviceAddress = address;
        this.latestRssi = rssi;
        this.id = identifier;
        this.lastDetectedTimestamp = timestamp;

        this.voltage = -1f;
        this.temperature = -1f;
        this.url = "";
        this.distance = 0.0;
    }

    public void update(String address, int rssi, long timestamp) {
        this.deviceAddress = address;
        this.latestRssi = rssi;
        this.lastDetectedTimestamp = timestamp;
    }

    @SuppressLint("DefaultLocale")
    @NonNull
    @Override
    public String toString() {
        return String.format("%s\n%ddBm    voltage: %s    Temperature: %s",
                id, latestRssi,
                voltage < 0f ? "N/A" : String.format("%.1fV", voltage),
                temperature < 0f ? "N/A" : String.format("%.1fC", temperature));
    }

    // Parse the instance id out of a UID packet
    public static String getInstanceId(byte[] data) {
        StringBuilder sb = new StringBuilder();

        //UID packets are always 18 bytes in length
        //Parse out the last 6 bytes for the id
        int packetLength = 18;
        int offset = packetLength - 6;
        for (int i=offset; i < packetLength; i++) {
            sb.append(Integer.toHexString(data[i] & 0xFF));
        }

        return sb.toString();
    }

    // Get the voltage from a TLM packet
    public static float getTlmVoltage(byte[] data) {
        byte version = data[1];
        if (version != 0) {
            Log.w(TAG, "Unknown telemetry version");
            return -1;
        }
        int voltage = (data[2] & 0xFF) << 8;
        voltage += (data[3] & 0xFF);

        return voltage / 1000f;
    }

    // Get the temperature from a TLM packet
    public static float getTlmTemperature(byte[] data) {
        byte version = data[1];
        if (version != 0) {
            Log.w(TAG, "Unknown telemetry version");
            return -1;
        }

        if (data[4] == (byte)0x80 && data[5] == (byte)0x00) {
            Log.w(TAG, "Temperature not supported");
            return -1;
        }

        int temp = (data[4] << 8);
        temp += (data[5] & 0xFF);

        return temp / 256f;
    }

    public static String getURL(byte[] data) {
        return new String(data, StandardCharsets.UTF_8);
    }

    // Equation to get beacon distance based on RSSI
    public static double getDistance(int rssi) {
        return Math.pow(10d, ((double) -59 - rssi) / (10 * 2));
    }
}
