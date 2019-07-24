package com.example.mcmvs

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener

class SensorReader(val activity: MainActivity) : SensorEventListener {
    var sensorID: Int = 0
    override fun onSensorChanged(sensorEvent: SensorEvent?) {
        if(sensorEvent!!.sensor.type == Sensor.TYPE_GYROSCOPE) {
            var sensorID = 4
            activity.gyroData =
                sensorID.toString() + ',' + sensorEvent.values[0].toString() + ',' + sensorEvent.values[1].toString() + ',' + sensorEvent.values[2].toString()
        }
        else if(sensorEvent.sensor.type == Sensor.TYPE_ACCELEROMETER){
            var sensorID = 3
            activity.accData =
                sensorID.toString() + ',' + sensorEvent.values[0].toString() + ',' + sensorEvent.values[1].toString() + ',' + sensorEvent.values[2].toString()
        }
    }
    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}
}