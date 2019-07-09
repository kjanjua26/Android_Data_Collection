package com.example.mcmvs

import android.hardware.camera2.*
import androidx.annotation.NonNull
import com.example.mcmvs.*

class FocusCaptureSessionCallback(val activity: MainActivity, internal var params: CameraParams) : CameraCaptureSession.CaptureCallback() {
    override fun onCaptureSequenceCompleted(session: CameraCaptureSession?, sequenceId: Int, frameNumber: Long) {
        MainActivity.Logd("CaptureSessionCallback : Capture sequence COMPLETED")
        if (!params.isOpen) {
            return
        }
        super.onCaptureSequenceCompleted(session, sequenceId, frameNumber)
    }

    override fun onCaptureSequenceAborted(session: CameraCaptureSession?, sequenceId: Int) {
        MainActivity.Logd("CaptureSessionCallback : Capture sequence ABORTED")
        if (!params.isOpen) {
            return
        }

        super.onCaptureSequenceAborted(session, sequenceId)
    }

    override fun onCaptureFailed(session: CameraCaptureSession?, request: CaptureRequest?, failure: CaptureFailure?) {
        MainActivity.Logd("CaptureSessionCallback : Capture sequence FAILED - " + failure?.reason)
        if (!params.isOpen) {
            return
        }

        //There is a device failure this might help
        closeCamera(params, activity)
        camera2OpenCamera(activity, params)
    }

    private fun process(result: CaptureResult) {
        if (!params.isOpen) {
            return
        }

        when (params.state) {
            STATE_PREVIEW -> {
            }// We have nothing to do when the camera preview is working normally.

            STATE_WAITING_LOCK -> {
                val afState = result.get(CaptureResult.CONTROL_AF_STATE)
                MainActivity.Logd("CaptureSessionCallback : STATE_WAITING_LOCK, afstate == " + afState)

                if (afState == null) {
                    MainActivity.Logd("CaptureSessionCallback : STATE_WAITING_LOCK, Calling captureStillPicture!")
                    params.state = STATE_PICTURE_TAKEN
                    captureStillPicture(activity, params)

                } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState
                    || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {

                    // CONTROL_AE_STATE can be null on some devices
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                    if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                        MainActivity.Logd("CaptureSessionCallback : STATE_WAITING_LOCK, Calling captureStillPicture!")
                        params.state = STATE_PICTURE_TAKEN
                        captureStillPicture(activity, params)
                    } else {
                        runPrecaptureSequence(activity, params)
                    }
                }
            }

            STATE_WAITING_PRECAPTURE -> {
                val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
//                val afState = result.get(CaptureResult.CONTROL_AF_STATE)
                MainActivity.Logd("CaptureSessionCallback : STATE_WAITING_PRECAPTURE. afState: " + " aeState: " + aeState)

                // CONTROL_AE_STATE can be null on some devices
                if (aeState == null ||
                    aeState == 0 ||
                    aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                    aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                    params.state = STATE_WAITING_NON_PRECAPTURE
                } else if (aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED
                    || aeState == CaptureResult.CONTROL_AE_STATE_LOCKED) {
                    params.state = STATE_WAITING_LOCK
                }
            }

            STATE_WAITING_NON_PRECAPTURE -> {
                MainActivity.Logd("CaptureSessionCallback : STATE_NON_PRECAPTURE.")
                // CONTROL_AE_STATE can be null on some devices
                val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                if (aeState == null || aeState == 0 || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                    MainActivity.Logd("CaptureSessionCallback : STATE_WAITING_NON_PRECAPTURE, Calling captureStillPicture!")
                    params.state = STATE_PICTURE_TAKEN
                    captureStillPicture(activity, params)
                }
            }
        }
    }


    override fun onCaptureProgressed(@NonNull session: CameraCaptureSession,
                                     @NonNull request: CaptureRequest,
                                     @NonNull partialResult: CaptureResult) {
        process(partialResult)
    }

    override fun onCaptureCompleted(@NonNull session: CameraCaptureSession,
                                    @NonNull request: CaptureRequest,
                                    @NonNull result: TotalCaptureResult) {
        process(result)
    }

    companion object {
        const val STATE_UNINITIALIZED = -1
        const val STATE_PREVIEW = 0
        const val STATE_WAITING_LOCK = 1
        const val STATE_WAITING_PRECAPTURE = 2
        const val STATE_WAITING_NON_PRECAPTURE = 3
        const val STATE_PICTURE_TAKEN = 4
    }
}
