package com.example.mcmvs

import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.os.Build
import androidx.annotation.NonNull
import com.example.mcmvs.FocusCaptureSessionCallback.Companion.STATE_PREVIEW
import com.example.mcmvs.CameraParams
import com.example.mcmvs.MainActivity
import com.example.mcmvs.createCameraPreviewSession

class PreviewSessionStateCallback(val activity: MainActivity, val params: CameraParams) : CameraCaptureSession.StateCallback() {
    override fun onActive(session: CameraCaptureSession?) {
        if (!params.isOpen) {
            return
        }

        super.onActive(session)
    }

    override fun onReady(session: CameraCaptureSession?) {
        if (!params.isOpen) {
            return
        }

        //This may be the initial start or we may have cleared an in-progress capture and the pipeline is now clear
        MainActivity.Logd("In onReady.")

        super.onReady(session)
    }

    override fun onConfigured(@NonNull cameraCaptureSession: CameraCaptureSession) {
        if (!params.isOpen) {
            return
        }

        MainActivity.Logd("In onConfigured: CaptureSession configured!")
        // When the session is ready, we start displaying the preview.
        try {

            params.previewBuilder?.set(CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            //params.previewBuilder?.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, [15, 15])
            params.captureSession = cameraCaptureSession
            params.state = STATE_PREVIEW

            // Finally, we start displaying the camera preview.
            if (28 <= Build.VERSION.SDK_INT)
                params.captureSession?.setSingleRepeatingRequest(params.previewBuilder?.build(), params.backgroundExecutor,
                    params.captureCallback)
            else
                params.captureSession?.setRepeatingRequest(params.previewBuilder?.build(),
                    params.captureCallback, params.backgroundHandler)

        } catch (e: CameraAccessException) {
            MainActivity.Logd("Create Preview Session error: " + params.id)
            e.printStackTrace()

        } catch (e: IllegalStateException) {
            MainActivity.Logd("createCameraPreviewSession onConfigured IllegalStateException, aborting: " + e)
            // camera2Abort(activity, params, testConfig)
        }
    }

    override fun onConfigureFailed(
        @NonNull cameraCaptureSession: CameraCaptureSession) {
        if (!params.isOpen) {
            return
        }

        MainActivity.Logd("Camera preview initialization failed.")
        MainActivity.Logd("Trying again")
        createCameraPreviewSession(activity, cameraCaptureSession.device, params)
        //TODO: fix endless loop potential.
    }
}
