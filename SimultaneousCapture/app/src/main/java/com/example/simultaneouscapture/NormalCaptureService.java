package com.example.simultaneouscapture;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Service;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;

import android.os.Environment;
import android.os.IBinder;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

public class NormalCaptureService extends Service {
    private final String TAG = "MainActivity";
    boolean safeToCapture = true;
    private SurfaceHolder sHolder;
    private Camera mCamera;
    private Parameters parameters;
    @Override
    public void onCreate(){
        super.onCreate();
    }
    @Override
    public void onStart(Intent intent, int startId) {
        mCamera = Camera.open();
        SurfaceView sv = new SurfaceView(getApplicationContext());
        SurfaceTexture surfaceTexture = new SurfaceTexture(10);

        try {
            mCamera.setPreviewTexture(surfaceTexture);
            parameters = mCamera.getParameters();
            mCamera.setParameters(parameters);
            if(safeToCapture) {
                mCamera.startPreview();
                mCamera.takePicture(null, null, mCall);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        sHolder = sv.getHolder();
        sHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    Camera.PictureCallback mCall = new Camera.PictureCallback(){
        public void onPictureTaken(byte[] data, Camera camera){
            safeToCapture = false;
            FileOutputStream outStream = null;
            try{
                File myDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "RGBCapture");
                if(!myDirectory.exists()) {
                    myDirectory.mkdirs();
                }
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                outStream = new FileOutputStream(myDirectory + "/" + timeStamp + ".jpg");
                outStream.write(data);
                outStream.close();
                Toast.makeText(NormalCaptureService.this, "Captured!", Toast.LENGTH_SHORT).show();
                mCamera.release();
                mCamera = null;
            } catch (FileNotFoundException e){
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            }
            safeToCapture = true;
        }
    };
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
