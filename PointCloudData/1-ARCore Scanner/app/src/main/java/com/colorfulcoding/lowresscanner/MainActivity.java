package com.colorfulcoding.lowresscanner;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.CamcorderProfile;
import android.media.Image;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.PointCloud;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.ux.ArFragment;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class MainActivity extends AppCompatActivity implements SensorEventListener, PopupMenu.OnMenuItemClickListener {
    private final String TAG = "MainActivity";
    private final float MIN_DIST_THRESHOLD = 0.01f; // 1cm
    private ArFragment fragment;
    private TextView debugText;
    private WorldToScreenTranslator worldToScreenTranslator;
    private VideoRecorder videoRecorder;
    private List<Float[]> cloudPoints;
    private List<Integer[]> colorPoints;
    private double PCtimeStamp;
    private SensorManager sensorManager;
    Sensor accelerometer;
    Sensor gyroscope;
    Sensor barometric;

    boolean gyroSwitchPref;
    boolean accSwitchPref;
    boolean presSwitchPref;
    boolean arCoreSwitchPref;
    boolean isStart = false;

    Button startBtn;

    FileWriter writer;
    BufferedWriter bufferedWriter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /*
            Data Writing.
         */
        videoRecorder = new VideoRecorder();
        fragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);
        /*
            Remove the plane rendering.
         */
        fragment.getArSceneView().getPlaneRenderer().setEnabled(false);
        startBtn = findViewById(R.id.save_but);
        videoRecorder.setSceneView(fragment.getArSceneView());
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        gyroSwitchPref = sharedPrefs.getBoolean("gyroPref", false);
        accSwitchPref = sharedPrefs.getBoolean("accPref", false);
        presSwitchPref = sharedPrefs.getBoolean("presPref", false);
        arCoreSwitchPref = sharedPrefs.getBoolean("arcorePref", false);

        int orientation = getResources().getConfiguration().orientation;
        videoRecorder.setVideoQuality(CamcorderProfile.QUALITY_720P, orientation);
        debugText = findViewById(R.id.text_debug);
        worldToScreenTranslator = new WorldToScreenTranslator();
        cloudPoints = new ArrayList<>();
        colorPoints = new ArrayList<>();
        startBtn.setOnClickListener(view -> {
            if(isStart) {
                saveData(view);
                stopARCore();
            }else{
                startBtn.setText("Stop");
                caller(view);
            }
            isStart = !isStart;
        });
    }

    private boolean scanning = false;
    private boolean recording = false;

    public void showMenu(View v){
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.setOnMenuItemClickListener(MainActivity.this);
        popupMenu.inflate(R.menu.mymenu);
        popupMenu.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item){
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
        return true;
    }

    private void caller(View v){
        // TODO: Requires code clean up.

        File dataDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "PointCloudData");
        if (!dataDir.exists()) {
            dataDir.mkdir();
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName_pc = "data_" + timeStamp + ".pcl";
        String fileName_sensor = "data_" + timeStamp + ".csv";
        File fileDir = new File(dataDir.getPath() + File.separator + fileName_pc);
        File sensorFileDir = new File(dataDir.getPath() + File.separator + fileName_sensor);

        // Case # 01: If either acc, gyro or pressure is selected AND arcore is selected, write both files.
        if(gyroSwitchPref || accSwitchPref || presSwitchPref && arCoreSwitchPref) {
            try {
                writer = new FileWriter(fileDir);
                bufferedWriter = new BufferedWriter(new FileWriter(sensorFileDir));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Case # 02: If only ARCore is selected, no need to create a sensor writer.
        else if(!gyroSwitchPref && !accSwitchPref && !presSwitchPref && arCoreSwitchPref){
            try {
                writer = new FileWriter(fileDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE); /* Got the permission to use the sensor. */
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        barometric = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);

        if(accSwitchPref && presSwitchPref && gyroSwitchPref){
            sensorManager.registerListener(MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager.registerListener(MainActivity.this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager.registerListener(MainActivity.this, barometric, sensorManager.SENSOR_DELAY_NORMAL);
        }else if (accSwitchPref && gyroSwitchPref){
            sensorManager.registerListener(MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager.registerListener(MainActivity.this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        }else if (accSwitchPref && presSwitchPref){
            sensorManager.registerListener(MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager.registerListener(MainActivity.this, barometric, sensorManager.SENSOR_DELAY_NORMAL);
        }else if (gyroSwitchPref && presSwitchPref){
            sensorManager.registerListener(MainActivity.this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager.registerListener(MainActivity.this, barometric, sensorManager.SENSOR_DELAY_NORMAL);
        }

        recording = videoRecorder.onToggleRecord();
        if(scanning){
            scanning = false;
            recording = false;
            return;
        }
        if(fragment.getArSceneView().getSession() == null){
            Toast.makeText(this, "No session found", Toast.LENGTH_SHORT);
            return;
        }

        if(fragment.getArSceneView().getArFrame() == null){
            Toast.makeText(this, "No frame found!", Toast.LENGTH_SHORT);
        }
        scanning = true;
        recording = true;
        /*
            For rendering the point cloud points. Stopped now.
         */
        //PointCloudNode pcNode = new PointCloudNode(getApplicationContext());
        //fragment.getArSceneView().getScene().addChild(pcNode);
        if(arCoreSwitchPref) {
            fragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
                if (!scanning) return;

                PointCloud pc = fragment.getArSceneView().getArFrame().acquirePointCloud();
                //pcNode.update(pc);
                try {
                    fragment.getPlaneDiscoveryController().hide();
                    fragment.getPlaneDiscoveryController().setInstructionView(null);
                    FloatBuffer points = pc.getPoints();
                    Log.i(TAG, "" + points.limit());

                    for (int i = 0; i < points.limit(); i += 4) {
                        float[] w = new float[]{points.get(i), points.get(i + 1), points.get(i + 2)};
                        Optional<Float> minDist = cloudPoints.stream()
                                .map(vec -> this.squaredDistance(vec, w))
                                .min((d1, d2) -> d1 - d2 < 0 ? -1 : 1);
                        if (minDist.orElse(1000f) < MIN_DIST_THRESHOLD * MIN_DIST_THRESHOLD) {
                            continue;
                        }

                        int[] color = getScreenPixel(w);
                        if (color == null || color.length != 3)
                            continue;
                        PCtimeStamp = pc.getTimestamp() / 1e+9;
                        cloudPoints.add(new Float[]{points.get(i), points.get(i + 1), points.get(i + 2)});
                        debugText.setText("" + cloudPoints.size() + " points scanned.");
                        colorPoints.add(new Integer[]{color[0], color[1], color[2]});
                        return;
                    }
                    pc.release();
                } catch (NotYetAvailableException e) {
                    Log.e(TAG, e.getMessage());
                }
            });
        }
    }
    private float squaredDistance(Float[] v, float[] w){
        float sumSquare = 0;
        if(v.length != w.length) return -1;
        for(int i =0 ; i < v.length; i++){
            sumSquare += (v[i] - w[i]) * ((v[i] - w[i]));
        }
        return sumSquare;
    }
    int[] getScreenPixel(float[] worldPos) throws NotYetAvailableException {
        Image img = fragment.getArSceneView().getArFrame().acquireCameraImage();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        double[] pos2D = worldToScreenTranslator.worldToScreen(img.getWidth(), img.getHeight(), fragment.getArSceneView().getArFrame().getCamera(), worldPos);
        Bitmap bmp = imageToBitmap(img);
        img.close();
        if(pos2D[0] < 0 || pos2D[0] > bmp.getWidth() || pos2D[1] < 0 || pos2D[1] > bmp.getHeight()){
            return null;
        }

        int pixel = bmp.getPixel((int) pos2D[0], (int) pos2D[1]);
        int r = Color.red(pixel);
        int g = Color.green(pixel);
        int b = Color.blue(pixel);

        return new int[]{r,g,b};
    }

    private Bitmap imageToBitmap(Image image){
        int width = image.getWidth();
        int height = image.getHeight();

        byte[] nv21;
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        nv21 = new byte[ySize + uSize + vSize];
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, os);
        byte[] jpegByteArray = os.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpegByteArray, 0, jpegByteArray.length);

        Matrix matrix = new Matrix();
        matrix.setRotate(90);

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
    private void saveData(View v){
        //fragment.getArSceneView().setupSession(null);
        createCSVFromFeaturePoints(v);
        videoRecorder.onToggleRecord();
        try {
            bufferedWriter.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    private void createCSVFromFeaturePoints(View v){
        String dataPoints = "";
        for (int i = 0; i < cloudPoints.size(); i++) {
            dataPoints += Arrays.toString(cloudPoints.get(i)) + "," + Arrays.toString(colorPoints.get(i)) + ",";
            dataPoints += "\n";
        }
        dataPoints = dataPoints.replace("[", "").replace("]", "");
        saveCSVToFile(dataPoints);
        Log.d(TAG, "Datapoints: " + dataPoints);
        /*
            Stop the AR session here and restart the activity here.
         */
        stopARCore();
    }
    private void saveCSVToFile(String data){
        try {
            writer.append(Double.toString(PCtimeStamp) + ",");
            writer.append(data + "\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void writeData(String data){
        try{
            bufferedWriter.write(data);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void stopARCore(){
        fragment.getArSceneView().destroy();
        startActivity(new Intent(MainActivity.this, MainActivity.class));
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        String sensorName = sensorEvent.sensor.getName();
        sensorName.replaceAll("\\P{Print}","");
        if(sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE){
            int sensorID = 4;
            String gyrodata = Float.toString(sensorEvent.values[0]) + "," + Float.toString(sensorEvent.values[1]) + ","
                    + Float.toString(sensorEvent.values[2]);

            //double timeInMillis = ((System.currentTimeMillis()
            //        + (sensorEvent.timestamp - SystemClock.elapsedRealtimeNanos()) / 1e6) / 1e9);

            double sensorTime = sensorEvent.timestamp/1e+9;

            String toWrite = Double.toString(sensorTime) + "," + sensorID + "," + gyrodata + "\n";
            writeData(toWrite);
            Log.d(TAG, "To Write: " + toWrite + " Test Time: " + sensorTime);
        }else if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            int sensorID = 3;
            String accData = Float.toString(sensorEvent.values[0]) + "," + Float.toString(sensorEvent.values[1]) + ","
                    + Float.toString(sensorEvent.values[2]);

            //double timeInMillis = ((System.currentTimeMillis()
            //       + (sensorEvent.timestamp - SystemClock.elapsedRealtimeNanos()) / 1e6) / 1e9);
            double sensorTime = sensorEvent.timestamp/1e+9;

            String toWrite = Double.toString(sensorTime) + "," + sensorID + "," + accData + "\n";
            writeData(toWrite);
            Log.d(TAG, "To Write: " + toWrite + " Test Time: " + sensorTime);
        }else if (sensorEvent.sensor.getType() == Sensor.TYPE_PRESSURE){
            int sensorID = 6;
            String pressureData = Float.toString(sensorEvent.values[0]);

            //double timeInMillis = ((System.currentTimeMillis()
            //       + (sensorEvent.timestamp - SystemClock.elapsedRealtimeNanos()) / 1e6) / 1e9);
            double sensorTime = sensorEvent.timestamp/1e+9; // using the time in nanosecond at which this happened.

            String toWrite = Double.toString(sensorTime) + "," + sensorID + "," + pressureData + "\n";
            writeData(toWrite);
            Log.d(TAG, "To Write: " + toWrite + " Test Time: " + sensorTime);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}
}
