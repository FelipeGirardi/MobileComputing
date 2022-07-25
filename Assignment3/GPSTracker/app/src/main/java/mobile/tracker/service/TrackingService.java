package mobile.tracker.service;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import mobile.tracker.AppConfig;
import mobile.tracker.GPXWriter;

public class TrackingService extends Service {
    private ITrackingServiceCallback callback;
    private TrackingServiceStatus status = TrackingServiceStatus.NOT_RUNNING;
    private GPXWriter gpxWriter;
    private BlockingQueue<Location> locations;
    private boolean running = true;

    private int locationCount = 0;

    public class TrackingServiceBinder extends Binder {
        public TrackingService getService() {
            return TrackingService.this;
        }
    }

    public TrackingServiceStatus getStatus() {
        return status;
    }

    public void setCallback(ITrackingServiceCallback callback) {
        this.callback = callback;
    }

    public void removeCallback() {
        callback = null;
    }

    private void changeServiceStatus(TrackingServiceStatus status) {
        this.status = status;

        if (callback != null)
            callback.onServiceStatusChange(status);
    }

    private void incrementLocationCount() {
        locationCount += 1;

        if (callback != null)
            callback.onNewLocationCount(locationCount);
    }

    private final Thread thread = new Thread() {

        @Override
        public void run() {
            Location location;

            while (running) {
                try {
                    location = locations.poll(100, TimeUnit.MILLISECONDS);
                    if (location != null) {
                        gpxWriter.writeLocation(location);

                        incrementLocationCount();
                    }
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };


    private final LocationListener locationListener = new LocationListener() {

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.i(AppConfig.TAG, "LocationListener: onStatusChanged");
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
        }

        @Override
        public void onLocationChanged(@NonNull Location location) {
            locations.add(location);
        }
    };


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(AppConfig.TAG, "TrackingService: onStartCommand");

        if (status == TrackingServiceStatus.RUNNING) {
            Log.i(AppConfig.TAG, "TrackingService: already running, skipping");
            return Service.START_STICKY;
        }

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(AppConfig.TAG, "service: ACCESS_FINE_LOCATION not granted when starting service");
        }

        locationCount -= 1;
        incrementLocationCount();

        thread.start();

        Date currentTime = Calendar.getInstance().getTime();

        File path = getExternalFilesDir(null);
        String filename = String.format("%s.gpx", currentTime);

        File file = new File(path, filename);
        Log.i(AppConfig.TAG, "file path: " + file.getAbsolutePath());

        gpxWriter = new GPXWriter(file);
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 5000, 10, locationListener);

        changeServiceStatus(TrackingServiceStatus.RUNNING);

        return Service.START_STICKY;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(AppConfig.TAG, "TrackingService: onBind");

        return new TrackingServiceBinder();
    }

    @Override
    public void onCreate() {
        Log.i(AppConfig.TAG, "service: onCreate");

        locations = new ArrayBlockingQueue<>(20);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.i(AppConfig.TAG, "TrackingService: onDestroy");
        Log.i(AppConfig.TAG, "TrackingService: saving trace");

        running = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            gpxWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        changeServiceStatus(TrackingServiceStatus.NOT_RUNNING);
    }
}
