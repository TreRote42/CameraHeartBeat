package com.example.cameraheartbeat.cameraUseCases;

import android.util.Log;

import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.example.cameraheartbeat.myInterface.IRedGreenAVG;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class RedGreenAnalyzer implements ImageAnalysis.Analyzer{

        private static final String TAG = "RedGreenAnalyzer";

        private IRedGreenAVG iRedGreenAVG;

        public RedGreenAnalyzer(IRedGreenAVG iRedGreenAVG){
            this.iRedGreenAVG = iRedGreenAVG;
        }

        @Override
        public void analyze(ImageProxy image) {
            // Image analysis code goes here
            // Process incoming image data
            //On CamreraX Once RGBA_8888 is selected, the output image format will be PixelFormat.RGBA_8888,
            // which has only one image plane (R, G, B, A pixel by pixel) with paddings.
            //Source: https://developer.android.com/reference/android/graphics/ImageFormat#RGBA_8888

            //Get the image buffer
            //byte[] data = image.getPlanes()[0].getBuffer().array();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            //Get red pixels
            byte[] red = new byte[data.length/4];
            for(int i=0; i < data.length; i+=4){
                red[i/4] = data[i];
            }
            //Get green pixels
            byte[] green = new byte[data.length/4];
            for(int i=1; i < data.length; i+=4){
                green[i/4] = data[i];
            }

            image.close();
            double redAvg = processAvg(red);
            double greenAvg = processAvg(green);

            if (iRedGreenAVG != null)
                iRedGreenAVG.onRedGreenAVGChanged(redAvg, greenAvg);
            else
                Log.i(TAG, "RedAvg: " + redAvg + " GreenAvg: " + greenAvg);
        }

        private double processAvg(byte[] data) {
            int[] pixels = new int[data.length];
            for(int i=0; i < data.length; i++){
                pixels[i] = (data[i] & 0xFF);
            }
            return Arrays.stream(pixels).average().orElse(0.0);
        }
}
