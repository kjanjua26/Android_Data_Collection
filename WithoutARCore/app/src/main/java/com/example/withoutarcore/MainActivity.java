/*
    This application does the following tasks.

    1. Collects the video feed.
    2. Collects data from different sensors such as: gyroscope, acceleration, barometer.
    3. Dumps the data in a CSV file in format (timestamp, time, val0, val1, ...).
    4. The video resolution is set to 720p.
 */
package com.example.withoutarcore;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    Camera camera;
    FrameLayout frameLayout;
    ShowCamera showCamera;
    Button btnCapture;
    MediaRecorder mediaRecorder;
    Chronometer chronometer;
    private boolean isRecording = false;
    private long pauseOffset;

    BufferedWriter bufferedWriter;

    private SensorManager sensorManager;
    Sensor accelerometer;
    Sensor gyroscope;
    Sensor barometric;

    private static final String TAG = "MainActivity";
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        frameLayout = findViewById(R.id.frameLayout);
        chronometer = findViewById(R.id.chronometer);
        camera = Camera.open();
        showCamera = new ShowCamera(this, camera);
        frameLayout.addView(showCamera);
        btnCapture = findViewById(R.id.capture);
        /*
            To save the data, we declare the file path.
         */
        File dataStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "MyCameraApp");
        if(!dataStorageDir.exists()){
            dataStorageDir.mkdir();
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = "data_" + timeStamp + ".csv";
        File fileDir = new File(dataStorageDir.getPath()  + File.separator + fileName);
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(fileDir));
        } catch (IOException e) {
            e.printStackTrace();
        }

        btnCapture.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                //captureImage(view);
                recordVideo(view);
            }
        });
    }
    Camera.PictureCallback pictureCallback = new Camera.PictureCallback(){
        @Override
        public void onPictureTaken(byte[] data, Camera camera){
            File pictureFile = getOutputMediaFile(MEDIA_TYPE_VIDEO);
            if(pictureFile == null){
                Toast.makeText(MainActivity.this, "Error! Check Storage Permission.", Toast.LENGTH_SHORT).show();
                return;
            }
            try{
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            }catch(FileNotFoundException e){
                e.printStackTrace();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    };
    public void captureImage(View view){
        if (camera != null){
            camera.takePicture(null, null, pictureCallback);
        }
    }
    public static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    public static File getOutputMediaFile(int type){
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "MyCameraApp");
        if(!mediaStorageDir.exists()){
            if(!mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "Failed To Create Directory!");
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }
    private boolean prepareVideoRecorder(){
        mediaRecorder = new MediaRecorder();
        camera.unlock();
        mediaRecorder.setCamera(camera);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P)); // setting the quality to 720p.
        mediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());
        mediaRecorder.setPreviewDisplay(showCamera.holder.getSurface());
        try {
            mediaRecorder.prepare();
        }catch (IllegalStateException e){
            e.printStackTrace();
            releaseMediaRecorder();
            return false;
        }catch (IOException e){
            e.printStackTrace();
            releaseMediaRecorder();
            return false;
        }
        return true;
    }
    public void recordVideo(View view){
        if(isRecording){
            mediaRecorder.stop();
            releaseMediaRecorder();
            camera.lock();
            btnCapture.setText("Capture");
            isRecording = false;
            stopChronometer(view);
            /*
                Close the file once the recording is stopped.
             */
            try {
                bufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else{
            if(prepareVideoRecorder()){
                mediaRecorder.start();
                btnCapture.setText("Stop");
                isRecording = true;
                startChronometer(view);
                try {
                    bufferedWriter.write("Time,SensorID,Payload" + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE); /* Got the permission to use the sensor. */
                accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
                barometric = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);

                sensorManager.registerListener(MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                sensorManager.registerListener(MainActivity.this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
                sensorManager.registerListener(MainActivity.this, barometric, sensorManager.SENSOR_DELAY_NORMAL);
            }
            else{
                releaseMediaRecorder();
                Toast.makeText(this, "Media Recorder Didn't Work", Toast.LENGTH_SHORT).show();
            }
        }
    }
    public void releaseMediaRecorder(){
        if(mediaRecorder != null){
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
            camera.lock();
        }
    }
    public void releaseCamera(){
        if (camera != null){
            camera.release();
            camera = null;
        }
    }
    public void startChronometer(View view){
        chronometer.setBase(SystemClock.elapsedRealtime() - pauseOffset);
        chronometer.start();
    }
    public void stopChronometer(View view){
        chronometer.setBase(SystemClock.elapsedRealtime());
        pauseOffset = 0;
        chronometer.stop();
    }
    public void writeData(String data){
        try{
            bufferedWriter.write(data);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @Override
    @SuppressLint("NewApi")
    /*
        Sensor IDs are for identification.
        For Gyroscope, the sensorID is 1.
        For Acceleration, the sensorID is 2.
        For Pressure, the sensorID is 3.
     */
    /*
        Testing the truncation of timeInMillis at the moment.
     */
    public void onSensorChanged(SensorEvent sensorEvent) {
        String sensorName = sensorEvent.sensor.getName();
        sensorName.replaceAll("\\P{Print}","");
        if(sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE){
            int sensorID = 1;
            String gyrodata = Float.toString(sensorEvent.values[0]) + ";" + Float.toString(sensorEvent.values[1]) + ";"
                    + Float.toString(sensorEvent.values[2]);

            double timeInMillis = ((System.currentTimeMillis()
                    + (sensorEvent.timestamp - SystemClock.elapsedRealtimeNanos()) / 1e6) / 1e9);

            String toWrite = Double.toString(timeInMillis) + "," + sensorID + "," + gyrodata + "\n";
            writeData(toWrite);
            Log.d(TAG, "To Write: " + toWrite);
        }else if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            int sensorID = 2;
            String accData = Float.toString(sensorEvent.values[0]) + ";" + Float.toString(sensorEvent.values[1]) + ";"
                    + Float.toString(sensorEvent.values[2]);

            double timeInMillis = ((System.currentTimeMillis()
                    + (sensorEvent.timestamp - SystemClock.elapsedRealtimeNanos()) / 1e6) / 1e9);

            String toWrite = Double.toString(timeInMillis) + "," + sensorID + "," + accData + "\n";
            writeData(toWrite);
            Log.d(TAG, "To Write: " + toWrite);
        }else if (sensorEvent.sensor.getType() == Sensor.TYPE_PRESSURE){
            int sensorID = 3;
            String pressureData = Float.toString(sensorEvent.values[0]);

            double timeInMillis = ((System.currentTimeMillis()
                    + (sensorEvent.timestamp - SystemClock.elapsedRealtimeNanos()) / 1e6) / 1e9);

            String toWrite = Double.toString(timeInMillis) + "," + sensorID + "," + pressureData + "\n";
            writeData(toWrite);
            Log.d(TAG, "To Write: " + toWrite);
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}
}