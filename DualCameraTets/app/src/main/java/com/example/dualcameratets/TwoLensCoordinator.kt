package com.example.dualcameratets

import android.media.Image

class TwoLensCoordinator (var isTwoLensShot: Boolean = false, var wideShotDone: Boolean = false, var normalShotDone: Boolean = false,
                          var wideImage: Image? = null, var normalImage: Image? = null, var wideParams: CameraParams = CameraParams(),
                          var normalParams: CameraParams = CameraParams()) {
    fun reset() {
        isTwoLensShot = false
        wideShotDone = false
        normalShotDone = false
        wideImage = null
        normalImage = null
    }
}