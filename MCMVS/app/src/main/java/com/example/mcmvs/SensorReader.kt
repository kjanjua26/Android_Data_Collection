package com.example.mcmvs

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import com.example.mcmvs.MainActivity.Companion.Logd

class SensorReader(val activity: MainActivity) : SensorEventListener {
    override fun onSensorChanged(sensorEvent: SensorEvent?) {
        if(sensorEvent!!.sensor.type == Sensor.TYPE_GYROSCOPE) {
            activity.gyroData = activity.gyroData +
                    (sensorEvent.timestamp/1e9)  + "," + "4" + ',' + sensorEvent.values[0].toString() + ',' + sensorEvent.values[1].toString() + ',' + sensorEvent.values[2].toString() + "\n"
            //Logd("Sensor: " + activity.gyroData)
        }
        else if(sensorEvent.sensor.type == Sensor.TYPE_ACCELEROMETER){
            activity.accData = activity.accData +
                    (sensorEvent.timestamp/1e9) +  "," + "3" + ',' + sensorEvent.values[0].toString() + ',' + sensorEvent.values[1].toString() + ',' + sensorEvent.values[2].toString() + "\n"
            //Logd("Sensor: " + activity.accData)
        }
    }
    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}
}