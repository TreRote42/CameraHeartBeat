package com.example.cameraheartbeat.myInterface;

public interface ICameraData {
    public void onRedGreenAVGChanged(double redAvg, double greenAvg);
    public void onLuxChanged(double lux);
}
