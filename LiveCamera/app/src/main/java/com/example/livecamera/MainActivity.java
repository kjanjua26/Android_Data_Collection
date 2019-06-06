package com.example.livecamera;

import android.hardware.Camera;
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

public class MainActivity extends AppCompatActivity {

    Camera camera;
    FrameLayout frameLayout;
    ShowCamera showCamera;
    Button btnCapture;
    MediaRecorder mediaRecorder;
    Chronometer chronometer;
    private boolean isRecording = false;
    private long pauseOffset;

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
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "MyCameraApp");
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
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
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
        }
        else{
            if(prepareVideoRecorder()){
                mediaRecorder.start();
                btnCapture.setText("Stop");
                isRecording = true;
                startChronometer(view);
                writeData("Testing The Writing!");
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
            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "MyCameraApp");
            if(!mediaStorageDir.exists()){
                mediaStorageDir.mkdir();
            }
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = "data_" + timeStamp + ".txt";
            File fileDir = new File(mediaStorageDir.getPath()  + File.separator + fileName);
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(fileDir));
            bufferedWriter.write(data);
            bufferedWriter.close();
            Toast.makeText(getBaseContext(), "File saved at: " + fileDir, Toast.LENGTH_LONG).show();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
