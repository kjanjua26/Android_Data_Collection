package com.example.mcmvs

import android.graphics.Bitmap
import android.hardware.camera2.*
import android.os.Build
import android.view.Surface
import androidx.annotation.NonNull
import com.example.mcmvs.*
import com.example.mcmvs.MainActivity.Companion.dualCamLogicalId
import com.example.mcmvs.MainActivity.Companion.normalLensId
import com.example.mcmvs.MainActivity.Companion.twoLens
import com.example.mcmvs.MainActivity.Companion.wideAngleId

class StillCaptureSessionCallback(val activity: MainActivity, val params: CameraParams) : CameraCaptureSession.CaptureCallback() {


    override fun onCaptureSequenceAborted(session: CameraCaptureSession?, sequenceId: Int) {
        if (!params.isOpen) {
            return
        }

        MainActivity.Logd("captureStillPicture captureCallback: Sequence aborted.")
        super.onCaptureSequenceAborted(session, sequenceId)
    }

    override fun onCaptureFailed(session: CameraCaptureSession?, request: CaptureRequest?, failure: CaptureFailure?) {
        if (!params.isOpen) {
            return
        }

        MainActivity.Logd("captureStillPicture captureCallback: Capture Failed. Failure: " + failure?.reason)

        //The session failed. Let's just try again (yay infinite loops)
        closeCamera(params, activity)
        camera2OpenCamera(activity, params)
        super.onCaptureFailed(session, request, failure)
    }

    override fun onCaptureStarted(session: CameraCaptureSession?, request: CaptureRequest?, timestamp: Long, frameNumber: Long) {
        if (!params.isOpen) {
            return
        }

        MainActivity.Logd("captureStillPicture captureCallback: Capture Started.")
        super.onCaptureStarted(session, request, timestamp, frameNumber)
    }

    override fun onCaptureProgressed(session: CameraCaptureSession?, request: CaptureRequest?, partialResult: CaptureResult?) {
        if (!params.isOpen) {
            return
        }

        MainActivity.Logd("captureStillPicture captureCallback: Capture progressed.")
        super.onCaptureProgressed(session, request, partialResult)
    }

    override fun onCaptureBufferLost(session: CameraCaptureSession?, request: CaptureRequest?, target: Surface?, frameNumber: Long) {
        if (!params.isOpen) {
            return
        }

        MainActivity.Logd("captureStillPicture captureCallback: Buffer lost.")
        super.onCaptureBufferLost(session, request, target, frameNumber)
    }

    override fun onCaptureCompleted(@NonNull session: CameraCaptureSession,
                                    @NonNull request: CaptureRequest,
                                    @NonNull result: TotalCaptureResult) {
        if (!params.isOpen) {
            return
        }

        MainActivity.Logd("captureStillPicture onCaptureCompleted. Hooray!.")

//        val mode = result.get(CaptureResult.STATISTICS_FACE_DETECT_MODE)
//        val faces = result.get(CaptureResult.STATISTICS_FACES)
//
//        MainActivity.Logd("FACE-DETECT DEBUG: in onCaptureCompleted. CaptureResult.STATISTICS_FACE_DETECT_MODE is: " + mode)
//
//        if (faces != null && mode != null) {
//            MainActivity.Logd("faces : " + faces.size + " , mode : " + mode)
//            for (face in faces) {
//                val rect = face.bounds
//                MainActivity.Logd("Bounds: bottom: " + rect.bottom + " left: " + rect.left + " right: " + rect.right + " top: " + rect.top)
//            }
//            MainActivity.Logd("Image size: " + params.maxSize.width + " x " + params.maxSize.height)
//            if (faces.isNotEmpty()) {
//                params.hasFace = true
//                params.faceBounds = faces.first().bounds
//
//                //params.faceBounds.top -= 20
//                //params.faceBounds.bottom += 20
//                //params.faceBounds.right += 20
//                //params.faceBounds.left -= 20
//
//                //TODO    This assumes we are taking max size stills. Need a Prefs setting.
//                params.expandedFaceBounds.top = params.faceBounds.top - (params.maxSize.height / 8)
//                params.expandedFaceBounds.bottom = params.faceBounds.bottom + (params.maxSize.height / 8)
//                params.expandedFaceBounds.right = params.faceBounds.right + (params.maxSize.width / 8)
//                params.expandedFaceBounds.left = params.faceBounds.left - (params.maxSize.width / 8)
//
//                //Make sure we don't overshoot
//                if (params.expandedFaceBounds.left < 0) params.expandedFaceBounds.left = 0
//                if (params.expandedFaceBounds.top < 0) params.expandedFaceBounds.top = 0
//                if (params.expandedFaceBounds.right > params.maxSize.width)
//                    params.expandedFaceBounds.right = params.maxSize.width
//                if (params.expandedFaceBounds.bottom > params.maxSize.height)
//                    params.expandedFaceBounds.bottom = params.maxSize.height
//
//                MainActivity.Logd("Adjusted Face Bounds: bottom: " + params.expandedFaceBounds.bottom + " left: " + params.expandedFaceBounds.left + " right: " + params.expandedFaceBounds.right + " top: " + params.expandedFaceBounds.top)
//
//                if (PrefHelper.getGrabCut(activity)) {
//                    //Expand facebox to include an extra "head" to left and right, and all the way to bottom of photo
//                    params.grabCutBounds = faceBoundsToGrabCutBounds(activity, params.expandedFaceBounds, params.maxSize.width, params.maxSize.height)
//                    MainActivity.Logd("Grabcut Bounds: bottom: " + params.grabCutBounds.bottom + " left: " + params.grabCutBounds.left + " right: " + params.grabCutBounds.right + " top: " + params.grabCutBounds.top)
//                }
//            }
//        }

        //It might be that we received this callback first and we're waiting for the image
        if (twoLens.isTwoLensShot) {
            when (params.id) {
                dualCamLogicalId-> {
                    twoLens.wideShotDone = true
                    twoLens.normalShotDone = true
                    twoLens.normalParams = MainActivity.cameraParams.get(normalLensId) ?: params
                    twoLens.wideParams = MainActivity.cameraParams.get(wideAngleId) ?: params
                    twoLens.normalParams.hasFace = params.hasFace
                    twoLens.normalParams.faceBounds = params.faceBounds
                    twoLens.normalParams.expandedFaceBounds = params.expandedFaceBounds
                    twoLens.wideParams.hasFace = params.hasFace
                    twoLens.wideParams.faceBounds = params.faceBounds
                    twoLens.wideParams.expandedFaceBounds = params.expandedFaceBounds
                }
                wideAngleId-> {
                    twoLens.wideShotDone = true
                    twoLens.wideParams = params
                }
                normalLensId-> {
                    twoLens.normalShotDone = true
                    twoLens.normalParams = params
                }
            }

            if (twoLens.wideShotDone && twoLens.normalShotDone
                && null != twoLens.wideImage
                && null != twoLens.normalImage) {

                //val finalBitmap: Bitmap =
                DoBokeh(activity, twoLens)
                //setCapturedPhoto(activity, params.capturedPhoto, finalBitmap)

                twoLens.normalImage?.close()
                twoLens.wideImage?.close()

                activity.runOnUiThread {
                    activity.captureFinished()
                }
            }
        }
        MainActivity.Logd("captureStillPicture onCaptureCompleted. CaptureEnd.")
        createCameraPreviewSession(activity, params.device!!, params)
    }
}
