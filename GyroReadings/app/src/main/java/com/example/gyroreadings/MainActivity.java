package com.example.gyroreadings;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "MainActivity";
    private SensorManager sensorManager;
    Sensor accelerometer;
    Sensor gyroscope;
    Sensor barometric;
    TextView accX, accY, accZ, GyroX, GyroY, GyroZ, PressureX;
    Button record, stop;
    String filename = "pressure.txt";

    File folder = new File(Environment.getDataDirectory() + "/DepthData");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        GyroX = findViewById(R.id.GyroX);
        GyroY = findViewById(R.id.GyroY);
        GyroZ = findViewById(R.id.GyroZ);

        accX = findViewById(R.id.accX);
        accY = findViewById(R.id.accY);
        accZ = findViewById(R.id.accZ);
        record = findViewById(R.id.button);
        stop = findViewById(R.id.stopBtn);

        PressureX = findViewById(R.id.pressureX);

        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Log.d(TAG, "onCreate: Initializing Sensor Services.");
                sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE); /* Got the permission to use the sensor. */
                accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
                barometric = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);

                sensorManager.registerListener(MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                sensorManager.registerListener(MainActivity.this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
                sensorManager.registerListener(MainActivity.this, barometric, sensorManager.SENSOR_DELAY_UI);
                Log.d(TAG, "onCreate: Registered Accelerometer Meter.");
            }
        });
    }
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        String sensorName = sensorEvent.sensor.getName();
        sensorName.replaceAll("\\P{Print}","");
        if(sensorName.contains("Gyroscope")) {
            GyroX.setText(Float.toString(sensorEvent.values[0]));
            GyroY.setText(Float.toString(sensorEvent.values[1]));
            GyroZ.setText(Float.toString(sensorEvent.values[2]));
        }else if (sensorName.contains("Acceleration")){
            accX.setText(Float.toString(sensorEvent.values[0]));
            accY.setText(Float.toString(sensorEvent.values[1]));
            accZ.setText(Float.toString(sensorEvent.values[2]));
        }
        if(sensorEvent.sensor.getType() == Sensor.TYPE_PRESSURE){
            PressureX.setText(Float.toString(sensorEvent.values[0]));
            Log.d(TAG, "Pressure: " + sensorEvent.values[0]);
            try{
                final FileOutputStream pressureWriter = openFileOutput(filename, Context.MODE_PRIVATE);
                pressureWriter.write(Float.toString(sensorEvent.values[0]).getBytes());
                stop.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            pressureWriter.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        sensorManager.unregisterListener(MainActivity.this);
                    }
                });
            }catch (Exception e){
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Error Saving File.", Toast.LENGTH_SHORT).show();
            }
        }else {
            Log.d(TAG, sensorName + ": X: " + sensorEvent.values[0] + " Y: " + sensorEvent.values[1] + " Z: " + sensorEvent.values[2]);
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}
}