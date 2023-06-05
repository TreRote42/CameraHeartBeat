package com.example.cameraheartbeat.utilities;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.example.cameraheartbeat.myInterface.IMyAccelerometer;

public class MyAccelerometer implements SensorEventListener {

    private final String TAG = "MyAccelerometer";

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private IMyAccelerometer iMyAccelerometer;

    private float prevX, prevY, prevZ;
    static public float threshold;

    public MyAccelerometer(Context context, float Threshold) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        this.iMyAccelerometer = (IMyAccelerometer) context;
        threshold = Threshold;
    }

    public void start() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        prevX = 0;
        prevY = 0;
        prevZ = 0;
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float x = sensorEvent.values[0];
        float y = sensorEvent.values[1];
        float z = sensorEvent.values[2];
        iMyAccelerometer.onMovementDetected(hasMovement(x, y, z));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        Log.i(TAG, "onAccuracyChanged");
    }

    public boolean hasMovement(float currentX, float currentY, float currentZ) {
        float deltaX = Math.abs(currentX - prevX);
        float deltaY = Math.abs(currentY - prevY);
        float deltaZ = Math.abs(currentZ - prevZ);
        prevX = currentX;
        prevY = currentY;
        prevZ = currentZ;
        return (deltaX > threshold) || (deltaY > threshold) || (deltaZ > threshold);
    }


}