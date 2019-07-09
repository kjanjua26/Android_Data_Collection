package com.example.mcmvs

import android.graphics.SurfaceTexture
import android.view.SurfaceHolder
import android.view.TextureView


class TextureListener(internal var params: CameraParams, internal val activity: MainActivity, internal val textureView: AutoFitTextureView): TextureView.SurfaceTextureListener {
    override fun onSurfaceTextureUpdated(p0: SurfaceTexture?) {
//        Logd( "In surfaceTextureUpdated. Id: " + params.id)
//        openCamera(params, activity)
    }

    override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
        MainActivity.Logd("In surfaceTextureAvailable. Id: " + params.id)
        MainActivity.Logd( MainActivity.wideAngleId)
        MainActivity.Logd(MainActivity.normalLensId)
        //If we are a dual cam case, and both preview surfaces are ready, open them both
        if (!MainActivity.dualCamLogicalId.equals("")
            && MainActivity.cameraParams.get(MainActivity.wideAngleId)?.previewTextureView?.isAvailable == true
        //&& MainActivity.cameraParams.get(MainActivity.normalLensId)?.previewTextureView?.isAvailable == true
        ) {
            val dualParams: CameraParams? = MainActivity.cameraParams.get(MainActivity.dualCamLogicalId)
            if (null != dualParams) {
                camera2OpenCamera(activity, dualParams)
            }
        } else {
            //Else, no dual cam and our surface is ready
//            camera2OpenCamera(activity, params)
        }
    }

    override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
        MainActivity.Logd("In surfaceTextureSizeChanged. Id: " + params.id)
        rotatePreviewTexture(activity, params, textureView)
    }

    override fun onSurfaceTextureDestroyed(texture: SurfaceTexture) : Boolean {
        MainActivity.Logd("In surfaceTextureDestroyed. Id: " + params.id)

        //If this is a dual camera, we only close the logical camera
        if (!MainActivity.dualCamLogicalId.equals("")) {
            val dualParams: CameraParams? = MainActivity.cameraParams.get(MainActivity.dualCamLogicalId)
            val wideParams: CameraParams? = MainActivity.cameraParams.get(MainActivity.wideAngleId)
            val normalParams: CameraParams? = MainActivity.cameraParams.get(MainActivity.normalLensId)

            try {
                wideParams?.captureSession?.stopRepeating()
                normalParams?.captureSession?.stopRepeating()
                dualParams?.captureSession?.stopRepeating()
            } catch (ex: Exception) {
                //
            }
            closeCamera(dualParams, activity)
        } else {
            closeCamera(params, activity)
        }

        return true
    }
}


class SurfaceCallback(val activity: MainActivity, val params: CameraParams): SurfaceHolder.Callback {
    override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {
    }

    override fun surfaceDestroyed(p0: SurfaceHolder?) {
    }

    override fun surfaceCreated(p0: SurfaceHolder?) {
    }

}
