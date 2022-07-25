package de.uni_s.ipvs.mcl.assignment5;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private static final String LOG_TAG = "FirebaseLog";
    private TextView cityName;
    private TextView temperature;
    private TextView timestamp;
    private Spinner citySpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // create views
        cityName = findViewById(R.id.cityName);
        temperature = findViewById(R.id.temperature);
        timestamp = findViewById(R.id.timestamp);
        Spinner citySpinner = (Spinner) findViewById(R.id.citySpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.cities_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        citySpinner.setAdapter(adapter);
        citySpinner.setOnItemSelectedListener(this);

        // get references
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference ref = db.getReference();

        // write to DB
        writeToDB(ref);
    }

    private void writeToDB(DatabaseReference ref) {
        // get current date/timestamp
        Long milliseconds = System.currentTimeMillis();
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date resultDate = new Date(milliseconds);
        String dateString = sdf.format(resultDate);
        String timestampString = String.valueOf(milliseconds);

        // get/create reference to timestamp and write value
        DatabaseReference cityRef = ref.child("location").child("Stuttgart").child(dateString).child(timestampString);
        cityRef.setValue(31.777);
    }

    private void readFromDB(DatabaseReference ref, String cityString) {
        DatabaseReference cityRef = ref.child("location").child(cityString);

        cityRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // reads map in format $timestamp:$temperature
                Map<String, Map<String, Double>> dataMap = (Map<String, Map<String, Double>>) dataSnapshot.getValue();
                Log.i(LOG_TAG, String.valueOf(dataMap));
                if (dataMap == null) {
                    temperature.setText("---");
                    timestamp.setText("---");
                    return;
                }
                // gets most recent date date
                Set<String> dateKeys = dataMap.keySet();
                TreeSet<String> sortedDateKeys = new TreeSet<>(dateKeys);
                String dateString = sortedDateKeys.last();

                // gets last update (most recent timestamp and temperature)
                Map<String, Double> timeTempMap = dataMap.get(dateString);
                if (timeTempMap == null) {
                    temperature.setText("---");
                    timestamp.setText("---");
                }

                Set<String> timestampKeys = timeTempMap.keySet();
                TreeSet<String> sortedTimestampKeys = new TreeSet<>(timestampKeys);
                String timestampString = sortedTimestampKeys.last();
                Object tempString = timeTempMap.get(timestampString);

                // sets temperature and date/timestamp texts
                DecimalFormat df = new DecimalFormat("#.#");
                temperature.setText(df.format(tempString) + "ºC");
                timestamp.setText(dateString + "\n\n" + timestampString);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.i(LOG_TAG, error.getMessage());
            }
        });
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        String cityString = adapterView.getItemAtPosition(i).toString();
        cityName.setText(cityString);

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference ref = db.getReference();
        readFromDB(ref, cityString);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
    }
}