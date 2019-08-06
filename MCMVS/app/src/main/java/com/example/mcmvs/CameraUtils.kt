package com.example.mcmvs

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Build
import android.util.Log
import android.view.Surface
import androidx.appcompat.app.AppCompatActivity
import com.example.mcmvs.CameraStateCallback
import com.example.mcmvs.FocusCaptureSessionCallback
import com.example.mcmvs.MainActivity.Companion
import com.example.mcmvs.MainActivity.Companion.Logd
import com.example.mcmvs.MainActivity.Companion.ORIENTATIONS
import com.example.mcmvs.MainActivity.Companion.normalLensId
import com.example.mcmvs.MainActivity.Companion.wideAngleId
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


fun initializeCameras(activity: MainActivity) {
    val manager = activity.getSystemService(AppCompatActivity.CAMERA_SERVICE) as CameraManager
    try {
        MainActivity.NUM_CAMERAS = manager.cameraIdList.size

        for (cameraId in manager.cameraIdList) {
            val tempCameraParams = CameraParams().apply {

                val cameraChars = manager.getCameraCharacteristics(cameraId)
                val cameraCapabilities = cameraChars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                for (capability in cameraCapabilities) {
                    when (capability) {
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA -> hasMulti = true
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR -> hasManualControl = true
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT -> hasDepth = true
                    }
                }

                MainActivity.Logd("Camera " + cameraId + " of " + MainActivity.NUM_CAMERAS)

                id = cameraId
                isOpen = false
                hasFlash = cameraChars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
                isFront = CameraCharacteristics.LENS_FACING_FRONT == cameraChars.get(CameraCharacteristics.LENS_FACING)
                characteristics = cameraChars
                focalLengths = cameraChars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                smallestFocalLength = smallestFocalLength(focalLengths)
                minDeltaFromNormal = focalLengthMinDeltaFromNormal(focalLengths)

                apertures = cameraChars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)
                largestAperture = largestAperture(apertures)
                minFocusDistance = cameraChars.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)

                if (Build.VERSION.SDK_INT >= 28) {
                    canSync = cameraChars.get(CameraCharacteristics.LOGICAL_MULTI_CAMERA_SENSOR_SYNC_TYPE) == CameraMetadata.LOGICAL_MULTI_CAMERA_SENSOR_SYNC_TYPE_CALIBRATED
                    if (canSync)
                        Logd("This camera can SYNC its sensors with timestamp accurately.")
                }
                if (Build.VERSION.SDK_INT >= 28) {
                    lensDistortion = cameraChars.get(CameraCharacteristics.LENS_DISTORTION)
                    intrinsicCalibration = cameraChars.get(CameraCharacteristics.LENS_INTRINSIC_CALIBRATION)
                    poseRotation = cameraChars.get(CameraCharacteristics.LENS_POSE_ROTATION)
                    poseTranslation = cameraChars.get(CameraCharacteristics.LENS_POSE_TRANSLATION)
                }

                for (focalLength in focalLengths) {
                    MainActivity.Logd("In " + id + " found focalLength: " + focalLength)
                }
                MainActivity.Logd("Smallest smallestFocalLength: " + smallestFocalLength)
                MainActivity.Logd("minFocusDistance: " + minFocusDistance)

                for (aperture in apertures) {
                    MainActivity.Logd("In " + id + " found aperture: " + aperture)
                }
                MainActivity.Logd("Largest aperture: " + largestAperture)

                if (hasManualControl) {
                    MainActivity.Logd("Has Manual, minFocusDistance: " + minFocusDistance)
                }

                effects = cameraChars.get(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS)
                hasSepia = effects.contains(CameraMetadata.CONTROL_EFFECT_MODE_SEPIA)
                hasMono = effects.contains(CameraMetadata.CONTROL_EFFECT_MODE_MONO)
                hasAF = minFocusDistance != MainActivity.FIXED_FOCUS_DISTANCE //If camera is fixed focus, no AF


                if (hasSepia)
                    MainActivity.Logd("WE HAVE Sepia!")
                if (hasMono)
                    MainActivity.Logd("WE HAVE Mono!")
                if (hasAF)
                    MainActivity.Logd("Camera " + id + " has autofocus.")
                else
                    MainActivity.Logd("Camera " + id + " is fixed-focus.")

                if (hasDepth)
                    Logd("This camera has depth output!")

                //capturedPhoto = activity.imagePhoto

                cameraCallback = CameraStateCallback(this, activity)
                captureCallback = FocusCaptureSessionCallback(activity, this)
//                    textureListener = TextureListener(this,this@MainActivity)

                imageAvailableListener = ImageAvailableListener(activity, this)

                if (Build.VERSION.SDK_INT >= 28) {
                    physicalCameras = cameraChars.physicalCameraIds
                }


                //Get image capture sizes
                val map = characteristics?.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

                Logd("Output format map: \n" + map.toString())

                if (map != null) {
                    maxSize = Collections.max(Arrays.asList(*map.getOutputSizes(ImageFormat.JPEG)),
                        CompareSizesByArea())
                    minSize = Collections.min(Arrays.asList(*map.getOutputSizes(ImageFormat.JPEG)),
                        CompareSizesByArea())
                    setupImageReader(activity, this)
                }
            }
            MainActivity.cameraParams.put(cameraId, tempCameraParams)
        }

        //Default to using the first camera for everything
        if (!MainActivity.cameraParams.keys.isEmpty()) {
            MainActivity.logicalCamId = MainActivity.cameraParams.keys.first()
            MainActivity.wideAngleId = MainActivity.logicalCamId
            MainActivity.normalLensId = MainActivity.logicalCamId
        }

        //Next, if we have a front-facing camera, use the first one
//        for (tempCameraParams in MainActivity.cameraParams) {
//            if (tempCameraParams.value.isFront) {
//                MainActivity.wideAngleId = tempCameraParams.value.id ?: MainActivity.cameraParams.keys.first()
//                MainActivity.normalLensId = MainActivity.wideAngleId
//            }
//        }

        //Determine the first multi-camera logical camera (front or back)
        //Then choose the shortest focal for the wide-angle background camera
        //And closest to 50mm for the "normal lens"
        // TODO: Do the camera changings here.
        for (tempCameraParams in MainActivity.cameraParams) {
            if (tempCameraParams.value.hasMulti) {
//                MainActivity.logicalCamId = tempCameraParams.key
//                if(!tempCameraParams.value.physicalCameras.isEmpty()) {
//                    //Determine the widest angle lens
//                    MainActivity.wideAngleId = tempCameraParams.value.physicalCameras.first()
//                    for (physicalCamera in tempCameraParams.value.physicalCameras) {
//                        val tempLens: Float = MainActivity.cameraParams.get(physicalCamera)?.smallestFocalLength ?: MainActivity.INVALID_FOCAL_LENGTH
//                        val minLens: Float = MainActivity.cameraParams.get(MainActivity.wideAngleId)?.smallestFocalLength ?: MainActivity.INVALID_FOCAL_LENGTH
//                        if (tempLens < minLens)
//                            MainActivity.wideAngleId = physicalCamera
//                    }
//
//                    //Determine the closest to "normal" that is not the wide angle lens
//                    MainActivity.normalLensId = tempCameraParams.value.physicalCameras.first()
//                    for (physicalCamera in tempCameraParams.value.physicalCameras) {
//                        if (physicalCamera.equals(MainActivity.wideAngleId))
//                            continue
//
//                        //If these are still set to the same camera, change them so we have two different cameras
//                        if (normalLensId == wideAngleId)
//                            normalLensId = physicalCamera
//
//                        val tempLens: Float = MainActivity.cameraParams.get(physicalCamera)?.minDeltaFromNormal ?: MainActivity.INVALID_FOCAL_LENGTH
//                        val normalLens: Float = MainActivity.cameraParams.get(MainActivity.normalLensId)?.minDeltaFromNormal ?: MainActivity.INVALID_FOCAL_LENGTH
//                        if (tempLens < normalLens)
//                            MainActivity.normalLensId = physicalCamera
//                    }
//                }
                /*
                    Not all cameras support multicamera, so here are the following that support.
                    wideAngle => 2 (normal camera)
                    normalLens => 4 (zoomed in camera)
                    Draws wideAngle (normal cmaera).

                    wideAngle => 2 (normal camera)
                    normalLens => 3 (wide angle)
                    Draws wideAngle.
                 */
                MainActivity.wideAngleId = 2.toString()
                MainActivity.normalLensId = 3.toString()
                MainActivity.Logd("Found a multi: " + MainActivity.logicalCamId + " with wideAngle: " + MainActivity.wideAngleId + "(" + MainActivity.cameraParams.get(MainActivity.wideAngleId)?.smallestFocalLength
                        + ") and normal: " + MainActivity.normalLensId + " (" + MainActivity.cameraParams.get(MainActivity.normalLensId)?.minDeltaFromNormal + ")")

                //If we have a logical cam and two seprate physical cams
                if (!MainActivity.logicalCamId.equals("")
                    && MainActivity.logicalCamId != MainActivity.wideAngleId
                    && MainActivity.logicalCamId != MainActivity.normalLensId
                    && MainActivity.wideAngleId != MainActivity.normalLensId) {
                    MainActivity.cameraParams.get(MainActivity.logicalCamId)?.isLogicalBackedByPhysical = true
                    MainActivity.dualCamLogicalId = MainActivity.logicalCamId
                }

                break //Use the first multi-camera
            }
        }

        MainActivity.Logd("Setting logical: " + MainActivity.logicalCamId + " with wideAngle: " + MainActivity.wideAngleId + "(" + MainActivity.cameraParams.get(MainActivity.wideAngleId)?.smallestFocalLength
                + ") and normal: " + MainActivity.normalLensId + " (" + MainActivity.cameraParams.get(MainActivity.normalLensId)?.minDeltaFromNormal + ")")

        MainActivity.cameraParams.get(MainActivity.wideAngleId)?.previewTextureView  = activity.texture_background
        MainActivity.cameraParams.get(MainActivity.wideAngleId)?.previewTextureView?.surfaceTextureListener =
            TextureListener(MainActivity.cameraParams.get(MainActivity.wideAngleId)!!, activity, activity.texture_background)
        Logd(MainActivity.wideAngleId)
        // If multi-camera, ready both preview textures
//        if (MainActivity.wideAngleId != MainActivity.normalLensId) {
//            MainActivity.cameraParams.get(MainActivity.normalLensId)?.previewTextureView  = activity.texture_foreground
//            MainActivity.cameraParams.get(MainActivity.normalLensId)?.previewTextureView?.surfaceTextureListener =
//                    TextureListener(MainActivity.cameraParams.get(MainActivity.normalLensId)!!, activity, activity.texture_foreground)
//        }




//        //TODO: DYnamically blur preview, doing something like this: https://stackoverflow.com/questions/34972250/android-dynamically-blur-surface-with-video
    } catch (accessError: CameraAccessException) {
        accessError.printStackTrace()
    }
}


fun smallestFocalLength(focalLengths: FloatArray) : Float = focalLengths.min()
    ?: MainActivity.INVALID_FOCAL_LENGTH

fun largestAperture(apertures: FloatArray) : Float = apertures.max()
    ?: MainActivity.NO_APERTURE

fun focalLengthMinDeltaFromNormal(focalLengths: FloatArray) : Float
        = focalLengths.minBy { Math.abs(it - MainActivity.NORMAL_FOCAL_LENGTH) } ?: Float.MAX_VALUE

fun setAutoFlash(activity: Activity, camera: CameraDevice, requestBuilder: CaptureRequest.Builder?) {
    val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    try {
        val characteristics = manager.getCameraCharacteristics(camera.id)
        // Check if the flash is supported.
        val available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
        val isFlashSupported = available ?: false

        if (isFlashSupported) {
            requestBuilder?.set(CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
        }
    } catch (e: Exception) {
        //Do nothing
    }
}

fun getOrientation(params: CameraParams, rotation: Int): Int {
    Log.d("MCMVS", "Orientation: sensor: " + params.characteristics?.get(CameraCharacteristics.SENSOR_ORIENTATION)
            + " and current rotation: " + ORIENTATIONS.get(rotation))
    val sensorRotation: Int = params.characteristics?.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
    return (ORIENTATIONS.get(rotation) + sensorRotation + 270) % 360
}

/*fun getRequiredBitmapRotation(activity: MainActivity, depthMapCorrect: Boolean = false): Float {
    val rotation: Int = activity.windowManager.defaultDisplay.rotation

    var neededRotation = 0

    //Depth maps need a 90 CW turn so we undo that if needed
    if (depthMapCorrect) {
        neededRotation = when (rotation) {
            Surface.ROTATION_0 -> -180
            Surface.ROTATION_90 -> -270
            Surface.ROTATION_180 -> 0
            Surface.ROTATION_270 -> -90
            else -> 0
        }
    } else {
        neededRotation = when (rotation) {
            Surface.ROTATION_0 -> -90
            Surface.ROTATION_90 -> 0
            Surface.ROTATION_180 -> 90
            Surface.ROTATION_270 -> 180
            else -> 0
        }
    }

    Logd("So exciting, need rotation: " + neededRotation)

    return neededRotation.toFloat()
}*/

@SuppressLint("NewApi")
fun setupImageReader(activity: MainActivity, params: CameraParams) {
    with (params) {
        params.imageReader?.close()
        imageReader = ImageReader.newInstance(960, 720,
            ImageFormat.JPEG, /*maxImages*/30)
        imageReader?.setOnImageAvailableListener(
            imageAvailableListener, backgroundHandler)
    }


}

