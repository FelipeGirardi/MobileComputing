package mc.sensorinfo;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.Random;

// https://developer.android.com/guide/components/bound-services#java
// https://developer.android.com/guide/topics/sensors/sensors_overview
public class SensorService extends Service implements SensorEventListener {
    private final IBinder binder = new SensorBinder();
    private SensorManager sensorManager;
    private Sensor gyroSensor;
    private Sensor accelSensor;
    private Sensor stepCounterSensor;

    private final SensorValues sensorValues = new SensorValues();

    public static class SensorValues {
        public float[] accel;
        public float[] gyro;
        public int stepCount = 0;

        public SensorValues() {
            accel = new float[3];
            gyro = new float[3];
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor == accelSensor) {
            sensorValues.accel = sensorEvent.values;
        }

        if (sensorEvent.sensor == gyroSensor) {
            sensorValues.gyro = sensorEvent.values;
        }

        if (sensorEvent.sensor == stepCounterSensor) {
           sensorValues.stepCount = (int) sensorEvent.values[0];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { }

    public class SensorBinder extends Binder {
        SensorService getService() {
            return SensorService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        sensorManager.unregisterListener(this);
    }

    public SensorValues getSensorValues() {
        return sensorValues;
    }
}
