package com.example.cameraheartbeat.cameraUseCases;

import android.util.Log;
import android.widget.Toast;

import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.example.cameraheartbeat.myInterface.ICameraData;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class LuminosityAnalyzer implements ImageAnalysis.Analyzer {

    private static final String TAG = "LuminosityAnalyzer";
    private static ICameraData iCameraData;

    public LuminosityAnalyzer(ICameraData CameraData) {
        iCameraData = CameraData;
    }

    @Override
    public void analyze(ImageProxy image) {
        // Image analysis code goes here
        // Process incoming image data
        //CameraX produces images with YUV420_888 with Luma (Y), Chroma (U, V) and Paddings (P) with 8 bits for each channel.
        //Source: https://developer.android.com/reference/android/graphics/ImageFormat#YUV_420_888
        ByteBuffer bufferY = image.getPlanes()[0].getBuffer();
        byte[] dataY = new byte[bufferY.remaining()];
        bufferY.get(dataY);
        double lux = processAvgLuminosity(dataY);
        Log.d(TAG, "analyze: lux = " + lux);
        image.close();
        if (lux > 75)
            iCameraData.onLuxHigher();
        else if (lux < 55)
            iCameraData.onLuxLower();
        else
            iCameraData.onLuxNormal();
    }
    private double processAvgLuminosity(byte[] data) {
        int[] pixels = new int[data.length];
        for(int i=0; i < data.length; i++){
            pixels[i] = (data[i] & 0xFF);
        }
        return Arrays.stream(pixels).average().orElse(0.0);
    }


}
