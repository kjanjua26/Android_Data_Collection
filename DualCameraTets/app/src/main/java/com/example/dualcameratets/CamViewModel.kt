package com.example.dualcameratets

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CamViewModel : ViewModel() {
    private var cameraParams: HashMap<String, CameraParams> = HashMap<String, CameraParams>()
    private val doDualCamShot: MutableLiveData<Boolean> = MutableLiveData()
    private val showIntermediate: MutableLiveData<Boolean> = MutableLiveData()
    private val shouldOutputLog: MutableLiveData<Boolean> = MutableLiveData()

    fun getCameraParams(): HashMap<String, CameraParams> {
        return cameraParams
    }

    fun getShouldOutputLog(): MutableLiveData<Boolean> {
        if (shouldOutputLog.value == null)
            shouldOutputLog.value = true
        return shouldOutputLog
    }

    fun getDoDualCamShot(): MutableLiveData<Boolean> {
        if (doDualCamShot.value == null)
            doDualCamShot.value = false
        return doDualCamShot
    }

    fun getShowIntermediate(): MutableLiveData<Boolean> {
        if (showIntermediate.value == null)
            showIntermediate.value = false
        return showIntermediate
    }
}