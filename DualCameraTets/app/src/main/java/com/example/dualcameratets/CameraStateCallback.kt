package com.example.dualcameratets


import android.hardware.camera2.CameraDevice
import androidx.annotation.NonNull
import com.example.dualcameratets.*

class CameraStateCallback(internal var params: CameraParams, internal var activity: MainActivity) : CameraDevice.StateCallback() {
    override fun onClosed(camera: CameraDevice?) {
        MainActivity.Logd("In CameraStateCallback onClosed. Camera: " + params.id + " is closed.")
        super.onClosed(camera)
    }

    override fun onOpened(@NonNull cameraDevice: CameraDevice) {
        MainActivity.Logd("In CameraStateCallback onOpened: " + cameraDevice.id)
        params.isOpen = true
        params.device = cameraDevice

        createCameraPreviewSession(activity, cameraDevice, params)
    }

    override fun onDisconnected(@NonNull cameraDevice: CameraDevice) {
        MainActivity.Logd("In CameraStateCallback onDisconnected: " + params.id)
        if (!params.isOpen) {
            return
        }

//        closeCamera(params, activity)
    }

    override fun onError(@NonNull cameraDevice: CameraDevice, error: Int) {
        MainActivity.Logd("In CameraStateCallback onError: " + cameraDevice.id + " and error: " + error)
        if (!params.isOpen) {
            return
        }


        if (CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE == error) {
            //Let's try to close an open camera and re-open this one
            MainActivity.Logd("In CameraStateCallback too many cameras open, closing one...")
            closeACamera(activity)
            camera2OpenCamera(activity, params)
        } else if (CameraDevice.StateCallback.ERROR_CAMERA_DEVICE == error){
            MainActivity.Logd("Fatal camera error, close device.")
            closeCamera(params, activity)
//            camera2OpenCamera(activity, params)
        } else if (CameraDevice.StateCallback.ERROR_CAMERA_IN_USE == error){
            MainActivity.Logd("This camera is already open... doing nothing")
        } else {
            closeCamera(params, activity)
        }
    }
}
