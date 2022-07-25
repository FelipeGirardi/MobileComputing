package mc.sensorinfo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private SensorService sensorService;
    private boolean serviceBound = false;
    private final Handler handler = new Handler();
    private final String[] sensorPeriodValues = new String[] {"200ms", "500ms", "1s", "2s"};
    private final List<TextView> gyroViews = new ArrayList<>();
    private final List<TextView> accelViews = new ArrayList<>();
    private TextView stepView;
    private SensorService.SensorValues sensorValues;

    int sensorSampleRate = 200;

    private final Runnable runnable = new Runnable() {

        @Override
        public void run() {
            if (serviceBound) {
                if (sensorValues == null) {
                    sensorValues = sensorService.getSensorValues();
                }

                int i = 0;
                for (TextView textView: gyroViews) {
                    textView.setText(String.valueOf(sensorValues.gyro[i++]));
                }

                i = 0;
                for (TextView textView: accelViews) {
                    textView.setText(String.valueOf(sensorValues.accel[i++]));
                }

                stepView.setText(String.valueOf(sensorValues.stepCount));
            }

            handler.postDelayed(runnable, sensorSampleRate);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.relative_layout);

        gyroViews.add(findViewById(R.id.gyroscopeValueX));
        gyroViews.add(findViewById(R.id.gyroscopeValueY));
        gyroViews.add(findViewById(R.id.gyroscopeValueZ));

        accelViews.add(findViewById(R.id.accelValueX));
        accelViews.add(findViewById(R.id.accelValueY));
        accelViews.add(findViewById(R.id.accelValueZ));

        stepView = findViewById(R.id.stepCountValue);

        // bind to sensor service
        Intent intent = new Intent(this, SensorService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);

        handler.post(runnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindService(connection);
        serviceBound= false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.sensor_menu, menu);

        MenuItem item = menu.findItem(R.id.spinner);
        Spinner spinner = (Spinner) item.getActionView();
        spinner.setOnItemSelectedListener(this);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.my_spinner_item, sensorPeriodValues);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        return true;
    }

    private final ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            SensorService.SensorBinder binder = (SensorService.SensorBinder) iBinder;
            sensorService = binder.getService();
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            serviceBound = false;
        }
    };

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
        switch (position) {
            case 0:
                sensorSampleRate = 200;
                break;
            case 1:
                sensorSampleRate = 500;
                break;
            case 2:
                sensorSampleRate = 1000;
                break;
            case 3:
                sensorSampleRate = 2000;
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {}
}