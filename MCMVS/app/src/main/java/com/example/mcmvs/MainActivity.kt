package com.example.mcmvs

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.*
import android.preference.PreferenceManager
import android.util.Log
import android.util.SparseIntArray
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.OpenCVLoader
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/*
    Some issues that need resolving.
    05/08/19 => 1. After about 3 mins of capture, the app freezes and there is no error on logcat.
 */

class MainActivity : AppCompatActivity() {

    //val ffmpeg: FFmpeg = FFmpeg.getInstance(this) // trying to compile the library here.

    private val REQUEST_CAMERA_PERMISSION = 1
    private val REQUEST_FILE_WRITE_PERMISSION = 2
    var sensorManager: SensorManager? = null
    var gyroSensor: Sensor? = null
    var accSensor: Sensor? = null
    var sensorReader: SensorReader? = null
    var gyroData: String = ""
    var accData: String = ""
    var bufferedWriter: BufferedWriter? = null
    var isRunning = false
    var stopThread = false

    override fun onCreate(savedInstanceState: Bundle?)  {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        camViewModel = ViewModelProviders.of(this).get(CamViewModel::class.java)
        cameraParams = camViewModel.getCameraParams()
        /*
            Sensors here.
         */
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        accSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorReader = SensorReader(this)

        val dataDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "DataCollection")
        if (!dataDir.exists()) {
            dataDir.mkdir()
        }
        checkAndCreateDirectory()
        // check camera permissions.
        OpenCVLoader.initDebug()
        if (!OpenCVLoader.initDebug()) {
            Logd("OpenCV failed to load!")
        } else {
            Logd("OpenCV loaded successfully!")
        }
        if (checkCameraPermissions()){
            initializeCameras(this)
        }
        /*
            Using a thread based approach.
            https://stackoverflow.com/questions/11687011/run-loop-every-second-java
         */
        val myThread = Thread(Runnable {
            while (!Thread.interrupted())
                try {
                    Thread.sleep(1000)
                    runOnUiThread {
                        twoLens.reset()
                        twoLens.isTwoLensShot = true
                        if(stopThread){
                            return@runOnUiThread
                        }
                        MainActivity.cameraParams.get(dualCamLogicalId).let {
                            if (it?.isOpen == true) {
                                Logd("In onClick. Taking Dual Cam Photo on logical camera: " + dualCamLogicalId)
                                takePicture(this@MainActivity, it)
                                Toast.makeText(applicationContext, "Captured", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
        })
        button.setOnClickListener {
            prepareUIForCapture()
            if(isRunning){
                //myThread.interrupt()
                stopThread = true
                restartActivity()
            }else{
                button.text = "Stop"
                myThread.start()
                stopThread = false
                isRunning = !isRunning
            }
        }
        /*button.setOnClickListener {
            prepareUIForCapture()
            if(isRunning){
                Thread.interrupted()
                restartActivity()
            }else {
                button.text = "Stop"
                Thread(Runnable {
                    while (!Thread.interrupted())
                        try {
                            Thread.sleep(1000)
                            runOnUiThread {
                                twoLens.reset()
                                twoLens.isTwoLensShot = true
                                MainActivity.cameraParams.get(dualCamLogicalId).let {
                                    if (it?.isOpen == true) {
                                        Logd("In onClick. Taking Dual Cam Photo on logical camera: " + dualCamLogicalId)
                                        takePicture(this@MainActivity, it)
                                        Toast.makeText(applicationContext, "Captured", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }
                }).start() // the while thread will start in BG thread
                isRunning = !isRunning
            }
        }*/
        /*button.setOnClickListener {
            prepareUIForCapture()
            if(isRunning){
                handler.removeCallbacksAndMessages(null)
                //Logd("Length of wide: " + MainActivity.wideBitmaps.size)
                //Logd("Length of normal: " + MainActivity.normalBitmaps.size)
                //makeVideoFootage()
                restartActivity()
            }else{
                button.text = "Stop"
                handler.postDelayed(object : Runnable {
                    override fun run(){
                        twoLens.reset()
                        twoLens.isTwoLensShot = true
                        MainActivity.cameraParams.get(dualCamLogicalId).let {
                            if (it?.isOpen == true) {
                                Logd("In onClick. Taking Dual Cam Photo on logical camera: " + dualCamLogicalId)
                                takePicture(this@MainActivity, it)
                                //sensorManager!!.registerListener(sensorReader, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL)
                                //sensorManager!!.registerListener(sensorReader, accSensor, SensorManager.SENSOR_DELAY_NORMAL)
                                //Logd("Image Time: " + twoLens.normalImage!!.timestamp.toString())
                                Toast.makeText(applicationContext, "Captured", Toast.LENGTH_LONG).show()
                            }
                        }
                        //sensorManager
                        //bufferedWriter!!.close()
                        handler.postDelayed(this, 1000)
                    }
                }, 1000)
            }
            isRunning = !isRunning
        }*/
    }

    private fun restartActivity(){
        startActivity(Intent(this@MainActivity, MainActivity::class.java))
    }

    @SuppressLint("NewApi")
    fun checkAndCreateDirectory(){
        var sceneCounterList: MutableList<Int> = mutableListOf<Int>()
        var gpath: String = Environment.getExternalStorageDirectory().absolutePath
        var spath = "Download/DataCollection"
        val fileDir = File(gpath + File.separator + spath)

        val files = fileDir.listFiles()
        for (i in 0..files.size-1){
            if (files[i].isDirectory){
                var name = files[i].name
                if(name.length <= 6) {
                    var localScene = name.takeLast(1).toInt()
                    sceneCounterList.add(localScene)
                }else{
                    var localScene = name.takeLast(2).toInt()
                    sceneCounterList.add(localScene)
                }
            }
            Logd("Directories: " + files[i].name)
        }
        sceneCounterList.sort()
        if(!files.isEmpty()){
            var localSceneCount = sceneCounterList.get(sceneCounterList.size - 1)
            sceneCounter = localSceneCount + 1
            Logd("Directories: " + sceneCounterList.get(sceneCounterList.size - 1))
        }else{
            sceneCounter = 0
        }
    }

    /*fun makeVideoFootage(){
        // TODO: Add the on-the-fly ffmpeg part here.
    }*/

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CAMERA_PERMISSION -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //We now have permission, restart the app
                    val intent = getIntent()
                    finish()
                    startActivity(intent)
                } else {}
                return
            }
            REQUEST_FILE_WRITE_PERMISSION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //We now have permission, restart the app
                    val intent = getIntent()
                    finish()
                    startActivity(intent)
                } else {}
                return
            }
        }
    }

    fun checkCameraPermissions(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION)
            return false
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_FILE_WRITE_PERMISSION)
            return false

        }

        return true
    }

    /*fun initializeSensors(){
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }*/

    private fun startBackgroundThread(params: CameraParams) {
        if (params.backgroundThread == null) {
            params.backgroundThread = HandlerThread("MCMVS").apply {
                this.start()
                params.backgroundHandler = Handler(this.getLooper())
            }
        }
    }

    @SuppressLint("NewApi")
    private fun stopBackgroundThread(params: CameraParams) {
        params.backgroundThread?.quitSafely()
        try {
            params.backgroundThread?.join()
        } catch (e: InterruptedException) {
            Logd( "Interrupted while shutting background thread down: " + e.message)
        }
    }

    override fun onResume() {
        super.onResume()
        Logd( "In onResume")
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        toggleRotationLock(true)

        for (tempCameraParams in cameraParams) {
            if (28 > Build.VERSION.SDK_INT)
                startBackgroundThread(tempCameraParams.value)
        }
    }

    override fun onPause() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        for (tempCameraParams in cameraParams) {
            if (28 > Build.VERSION.SDK_INT)
                stopBackgroundThread(tempCameraParams.value)
        }
        super.onPause()
    }


    fun toggleRotationLock(lockRotation: Boolean = true) {
        //Lock the screen orientation during a test so the camera doesn't get re-initialized mid-capture
        if (lockRotation) {
            val currentOrientation = resources.configuration.orientation
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
            } else {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
            }
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
        }
    }

    fun prepareUIForCapture() {
        // TODO: Clear current bitmaps for memory

        toggleRotationLock(true)
    }
    fun captureFinished() {
        toggleRotationLock(false)
    }

    companion object {
        const val NORMAL_FOCAL_LENGTH: Float = 50f
        //const val GAUSSIAN_BLUR_RADIUS: Float = 25f
        //const val BLUR_SCALE_FACTOR: Float = 0.25f
        const val NO_APERTURE: Float = 0f
        const val FIXED_FOCUS_DISTANCE: Float = 0f
        //const val DISPLAY_BITMAP_SCALE: Float = 0.20f
        val INVALID_FOCAL_LENGTH: Float = Float.MAX_VALUE
        var NUM_CAMERAS = 0
        var dualCamLogicalId = ""
        var logicalCamId = ""
        var wideAngleId = ""
        var normalLensId = ""

        lateinit var camViewModel: CamViewModel
        lateinit var cameraParams: HashMap<String, CameraParams>
        //var counter = 0
        //var wideBitmaps: HashMap<String, Bitmap> = HashMap<String, Bitmap>()
        //var normalBitmaps: HashMap<String, Bitmap> = HashMap<String, Bitmap>()

        val twoLens: TwoLensCoordinator = TwoLensCoordinator()
        val ORIENTATIONS = SparseIntArray()
        var sceneCounter = 0
        //var lastTime = 0
        //const val SAVE_FILE = "saved_photo.jpg"

        init {
            ORIENTATIONS.append(Surface.ROTATION_0, 90)
            ORIENTATIONS.append(Surface.ROTATION_90, 0)
            ORIENTATIONS.append(Surface.ROTATION_180, 270)
            ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }

        fun Logd(message: String) {
            Log.d("MCMVS", message)
        }
    }
}
