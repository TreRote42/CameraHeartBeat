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
            image.close();
            Log.i(TAG, "Image Height: " + image.getHeight() + " Width: " + image.getWidth() + " Data length: " + data.length);

            //Get the red and green channel
            // to have more accurate result, we will use a central cropped square of the image
            int cropSize = Math.min(image.getHeight(), image.getWidth())/2;
            int cropStartX = (image.getWidth() - cropSize)/2;
            int cropStartY = (image.getHeight() - cropSize)/2;
            data = Arrays.copyOfRange(data, cropStartX*4 + cropStartY*image.getWidth()*4, cropStartX*4 + cropStartY*image.getWidth()*4 + cropSize*4*cropSize*4);
            byte[] red = new byte[data.length/4];
            byte[] green = new byte[data.length/4];
            for(int i=0; i < data.length; i+=4){
                red[i/4] = data[i];
                green[i/4] = data[i+1];
            }

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
