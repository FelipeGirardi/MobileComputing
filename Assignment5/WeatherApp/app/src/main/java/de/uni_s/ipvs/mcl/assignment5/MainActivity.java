package de.uni_s.ipvs.mcl.assignment5;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.renderscript.Sampler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private final String LOG_TAG = "firebase";
    private String[] locationArray;
    private DatabaseReference root;

    private TextView currentTempLabel;
    private TextView averageTempLabel;

    private DatabaseReference currentLocationRef;
    private ValueEventListener currentLocationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        root = database.getReference();

        currentTempLabel = findViewById(R.id.currentTemperature);
        averageTempLabel = findViewById(R.id.temperatureAverage);

        getLocations();
    }

    private void updateText(double temp, double average) {
        String currentTempString = String.format(Locale.US, "%.1f °C", temp);
        String averageTempString = String.format(Locale.US, "%.1f °C", average);

        updateText(currentTempString, averageTempString);
    }

    private void updateText(String temp, String average) {
        currentTempLabel.setText(temp);
        averageTempLabel.setText(average);
    }

    private void updateLocations(List<String> locations) {
        locationArray = new String[locations.size()];
        locationArray = locations.toArray(locationArray);

        Spinner locationSpinner = findViewById(R.id.locationSpinner);
        locationSpinner.setOnItemSelectedListener(this);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, locations);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        locationSpinner.setAdapter(adapter);
    }

    private void getLocations() {
        DatabaseReference locationRef = root.child("location");

        locationRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> locations = new ArrayList<>();
                for(DataSnapshot child: snapshot.getChildren()) {
                    locations.add(child.getKey());
                }
                updateLocations(locations);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(LOG_TAG, "read cancelled: " + error.getMessage());
            }
        });
    }

    private void subscribeToLocation(String location) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String today = dateFormat.format(new Date());
        Log.i(LOG_TAG, today);

        if (currentLocationListener != null) {
            currentLocationRef.removeEventListener(currentLocationListener);
        }

        currentLocationListener = new ValueEventListener() {
            private long valueCount = 0;
            private double average = 0;
            private double latest = 0;

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double temp = 0;
                for (DataSnapshot child: snapshot.getChildren()) {
                    Object tempObject = child.getValue();
                    if (tempObject instanceof Double) {
                        temp = (double) tempObject;
                    }
                    else if (tempObject instanceof String) {
                        temp = Double.parseDouble((String) tempObject);
                    } else {
                        Log.e(LOG_TAG, "unexpected data type for temperature " + tempObject.getClass());
                    }

                    average += (temp - average) / ++valueCount;
                    latest = temp;
                }
                if (valueCount > 0) {
                    updateText(latest, average);
                } else {
                    updateText("-", "-");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

        currentLocationRef = root.child("location").child(location).child(today);
        currentLocationRef.addValueEventListener(currentLocationListener);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        Log.i(LOG_TAG, "location selected: " + locationArray[i]);
        subscribeToLocation(locationArray[i]);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}