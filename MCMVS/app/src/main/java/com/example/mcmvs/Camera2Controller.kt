package com.example.mcmvs

import android.app.Activity
import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Build
import android.view.Surface
import android.view.View
import com.example.mcmvs.CameraStateCallback
import com.example.mcmvs.FocusCaptureSessionCallback
import com.example.mcmvs.FocusCaptureSessionCallback.Companion.STATE_PICTURE_TAKEN
import com.example.mcmvs.FocusCaptureSessionCallback.Companion.STATE_WAITING_PRECAPTURE
import com.example.mcmvs.PreviewSessionStateCallback
import com.example.mcmvs.StillCaptureSessionCallback
import com.example.mcmvs.MainActivity.Companion.Logd
import com.example.mcmvs.MainActivity.Companion.cameraParams
import com.example.mcmvs.MainActivity.Companion.dualCamLogicalId
import com.example.mcmvs.MainActivity.Companion.normalLensId
import com.example.mcmvs.MainActivity.Companion.twoLens
import com.example.mcmvs.MainActivity.Companion.wideAngleId
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

fun createCameraPreviewSession(activity: MainActivity, camera: CameraDevice, params: CameraParams) {
    Logd("In createCameraPreviewSession.")
    if (!params.isOpen) {
        return
    }

    try {
        if (Build.VERSION.SDK_INT >= 28 && params.id.equals(MainActivity.dualCamLogicalId)) {
            val normalParams: CameraParams? = MainActivity.cameraParams.get(normalLensId)
            val wideParams: CameraParams? = MainActivity.cameraParams.get(wideAngleId)

            Logd("In createCameraPreview. This is a Dual Cam stream. Starting up simultaneous streams.")

            if (null == normalParams || null == wideParams)
                return

//          val normalTexture = normalParams.previewTextureView?.surfaceTexture
            val wideTexture = wideParams.previewTextureView?.surfaceTexture

//          if (null == normalTexture || null == wideTexture)
//                return

//          normalTexture.setDefaultBufferSize(400,400)
            wideTexture!!.setDefaultBufferSize(400,400)

//          val normalSurface = Surface(normalTexture)
            val wideSurface = Surface(wideTexture)

//          if (null == normalSurface || null == wideSurface)
//                return


            val wideOutputConfigPreview = OutputConfiguration(wideSurface)
            val wideOutputConfigImageReader = OutputConfiguration(wideParams.imageReader?.surface!!)
            wideOutputConfigPreview.setPhysicalCameraId(wideAngleId)
            wideOutputConfigImageReader.setPhysicalCameraId(wideAngleId)

//          val normalOutputConfigPreview = OutputConfiguration(normalSurface)
            val normalOutputConfigImageReader = OutputConfiguration(normalParams.imageReader?.surface!!)
            normalOutputConfigImageReader.setPhysicalCameraId(normalLensId)

            val sessionConfig = SessionConfiguration(SessionConfiguration.SESSION_REGULAR,
                Arrays.asList( normalOutputConfigImageReader, wideOutputConfigPreview, wideOutputConfigImageReader),
                params.backgroundExecutor, PreviewSessionStateCallback(activity, params))

            params.previewBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
//          params.previewBuilder?.addTarget(normalSurface)
            params.previewBuilder?.addTarget(wideSurface)

            camera.createCaptureSession(sessionConfig)

            //Else we do not have a dual cam situation, just worry about the single camera
        } else {
            val texture = params.previewTextureView?.surfaceTexture

            if (null == texture)
                return

            val surface = Surface(texture)

            if (null == surface)
                return

            params.previewBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            params.previewBuilder?.addTarget(surface)

            val imageSurface = params.imageReader?.surface
            if (null == imageSurface)
                return

            // Here, we create a CameraCaptureSession for camera preview.
            if (Build.VERSION.SDK_INT >= 28) {
                val sessionConfig = SessionConfiguration(SessionConfiguration.SESSION_REGULAR,
                    Arrays.asList(OutputConfiguration(surface), OutputConfiguration(imageSurface)),
                    params.backgroundExecutor, PreviewSessionStateCallback(activity, params))

                camera.createCaptureSession(sessionConfig)

            } else {
                camera.createCaptureSession(Arrays.asList(surface, imageSurface),
                    PreviewSessionStateCallback(activity, params), params.backgroundHandler)
            }

        }

    } catch (e: CameraAccessException) {
        e.printStackTrace()
    } catch (e: IllegalStateException) {
        Logd("createCameraPreviewSession IllegalStateException, aborting: " + e)
    }
}

fun camera2OpenCamera(activity: MainActivity, params: CameraParams?) {
    if (null == params)
        return

    val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    try {
        params.cameraCallback = CameraStateCallback(params, activity)
        params.captureCallback = FocusCaptureSessionCallback(activity, params)

        rotatePreviewTexture(activity, params, activity.texture_background)
        //rotatePreviewTexture(activity, params, activity.texture_background)

        //We have a dual lens situation, only open logical cam
        if (!MainActivity.dualCamLogicalId.equals("")
            && MainActivity.dualCamLogicalId.equals(params.id)) {
            Logd("Open Logical Camera backed by 2+ physical streams: " + MainActivity.dualCamLogicalId)

            if (28 <= Build.VERSION.SDK_INT)
                manager.openCamera(params.id, params.backgroundExecutor, params.cameraCallback)
            else
                manager.openCamera(params.id, params.cameraCallback, params.backgroundHandler)

        } else {
            Logd("openCamera: " + params.id)
            if (28 <= Build.VERSION.SDK_INT)
                manager.openCamera(params.id, params.backgroundExecutor, params.cameraCallback)
            else
                manager.openCamera(params.id, params.cameraCallback, params.backgroundHandler)
        }

    } catch (e: CameraAccessException) {
        Logd("openCamera CameraAccessException: " + params.id)
        e.printStackTrace()
    } catch (e: SecurityException) {
        Logd("openCamera SecurityException: " + params.id)
        e.printStackTrace()
    }
}


//Close the first open camera we find
fun closeACamera(activity: MainActivity) {
    var closedACamera = false
    Logd("In closeACamera, looking for open camera.")
    for (tempCameraParams: CameraParams in cameraParams.values) {
        if (tempCameraParams.isOpen) {
            Logd("In closeACamera, found open camera, closing: " + tempCameraParams.id)
            closedACamera = true
            closeCamera(tempCameraParams, activity)
            break
        }
    }

    // We couldn't find an open camera, let's close everything
    if (!closedACamera) {
        closeAllCameras(activity)
    }
}

fun closeAllCameras(activity: MainActivity) {
    Logd("Closing all cameras.")
    for (tempCameraParams: CameraParams in cameraParams.values) {
        closeCamera(tempCameraParams, activity)
    }
}

fun closeCamera(params: CameraParams?, activity: MainActivity) {
    if (null == params)
        return

    Logd("closeCamera: " + params.id)
    params.isOpen = false
    params.captureSession?.close()
    params.device?.close()
}



fun takePicture(activity: MainActivity, params: CameraParams) {
    Logd("TakePicture: capture start.")

    if (!params.isOpen) {
        return
    }

    lockFocus(activity, params)
}

fun lockFocus(activity: MainActivity, params: CameraParams) {
    Logd("In lockFocus.")
    if (!params.isOpen) {
        return
    }

    try {
        val camera = params.captureSession?.getDevice()
        if (null != camera) {
            params.captureBuilder?.addTarget(params.imageReader?.surface)
            setAutoFlash(activity, camera, params.captureBuilder)

            //If this lens can focus, we need to start a focus search and wait for focus lock
            if (params.hasAF) {
                Logd("In lockFocus. About to request focus lock and call capture.")
//                params.captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
//                setAutoFlash(activity, camera, params.captureBuilder)
//                params.captureBuilder?.addTarget(params.imageReader?.getSurface())
//                params.captureBuilder?.set(CaptureRequest.CONTROL_AF_TRIGGER,
//                        CameraMetadata.CONTROL_AF_TRIGGER_START);
                //               params.captureBuilder?.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
//                params.captureBuilder?.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                params.state = STATE_PICTURE_TAKEN
                captureStillPicture(activity, params)
            } else {
                Logd("In lockFocus. Fixed focus lens about call captureStillPicture.")
                params.state = STATE_PICTURE_TAKEN
                captureStillPicture(activity, params)
            }
        }

    } catch (e: CameraAccessException) {
        e.printStackTrace()
    }
}

fun runPrecaptureSequence(activity: MainActivity, params: CameraParams) {
    if (!params.isOpen) {
        return
    }

    try {
        val camera = params.captureSession?.getDevice()

        if (null != camera) {
            setAutoFlash(activity, camera, params.captureBuilder)
            params.captureBuilder?.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START)

            params.state = STATE_WAITING_PRECAPTURE

            if (28 <= Build.VERSION.SDK_INT)
                params.captureSession?.captureSingleRequest(params.captureBuilder?.build(), params.backgroundExecutor, params.captureCallback)
            else
                params.captureSession?.capture(params.captureBuilder?.build(), params.captureCallback,
                    params.backgroundHandler)

        }
    } catch (e: CameraAccessException) {
        e.printStackTrace()
    }

}

fun captureStillPicture(activity: MainActivity, params: CameraParams) {
    if (!params.isOpen) {
        return
    }

    try {
        Logd("In captureStillPicture.")

        val camera = params.captureSession?.getDevice()

        if (null != camera) {
            params.captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
//            params.captureBuilder?.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            params.captureBuilder?.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)


//            setAutoFlash(activity, camera, params.captureBuilder)

            if (params.id.equals(dualCamLogicalId) && twoLens.isTwoLensShot) {

                val normalParams: CameraParams? = MainActivity.cameraParams.get(normalLensId)
                val wideParams: CameraParams? = MainActivity.cameraParams.get(wideAngleId)

                if (null == normalParams || null == wideParams)
                    return

                Logd("In captureStillPicture. This is a Dual Cam shot.")

                params.captureBuilder?.addTarget(normalParams.imageReader?.surface!!)
                params.captureBuilder?.addTarget(wideParams.imageReader?.surface!!)

            } else {
                //Default to wide
                val wideParams: CameraParams? = MainActivity.cameraParams.get(wideAngleId)
                if (null != wideParams)
                    params.captureBuilder?.addTarget(wideParams.imageReader?.surface!!)
                else
                    params.captureBuilder?.addTarget(params.imageReader?.getSurface())
            }

            params.captureBuilder?.set(CaptureRequest.JPEG_QUALITY, 90)
            // Orientation
            val rotation = activity.getWindowManager().getDefaultDisplay().getRotation()
            var capturedImageRotation = getOrientation(params, rotation)
            params.captureBuilder?.set(CaptureRequest.JPEG_ORIENTATION, capturedImageRotation)

            try {
                params.captureSession?.stopRepeating()
//                params.captureSession?.abortCaptures()
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
            //Do the capture
            if (28 <= Build.VERSION.SDK_INT )
                params.captureSession?.captureSingleRequest(params.captureBuilder?.build(), params.backgroundExecutor, StillCaptureSessionCallback(activity, params))
            else
                params.captureSession?.capture(params.captureBuilder?.build(), StillCaptureSessionCallback(activity, params),
                    params.backgroundHandler)

        }
    } catch (e: CameraAccessException) {
        e.printStackTrace()

    } catch (e: IllegalStateException) {
        Logd("captureStillPicture IllegalStateException, aborting: " + e)
    }
}

fun unlockFocus(activity: MainActivity, params: CameraParams) {
    Logd("In unlockFocus.")

    if (!params.isOpen) {
        return
    }

    try {
        if (null != params.device) {
        }
    } catch (e: CameraAccessException) {
        e.printStackTrace()

    } catch (e: IllegalStateException) {
        Logd("unlockFocus IllegalStateException, aborting: " + e)
    }

}

fun rotatePreviewTexture(activity: MainActivity, params: CameraParams, textureView: AutoFitTextureView) {
    val rotation: Int = activity.windowManager.defaultDisplay.rotation

    val matrix: Matrix = Matrix()
    val viewRect: RectF = RectF(0f, 0f, textureView.width.toFloat(), textureView.height.toFloat())
    val previewRect: RectF = RectF(0f, 0f, params.minSize.width.toFloat(), params.minSize.height.toFloat())
    val centerX: Float = viewRect.centerX()
    val centerY: Float = viewRect.centerY()

    if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
        previewRect.offset(centerX - previewRect.centerX(), centerY - previewRect.centerY());
        matrix.setRectToRect(viewRect, previewRect, Matrix.ScaleToFit.FILL);
        val scale: Float = Math.max(textureView.width.toFloat() / params.minSize.width.toFloat() ,
            textureView.height.toFloat() / params.minSize.height.toFloat());
        matrix.postScale(scale, scale, centerX, centerY);
        matrix.postRotate(90f * (rotation - 2), centerX, centerY);
    } else if (Surface.ROTATION_180 == rotation) {
        matrix.postRotate(180f, centerX, centerY);
    }
    textureView.setTransform(matrix);

}