package com.example.cameraheartbeat.cameraUseCases;

import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.example.cameraheartbeat.myInterface.ICameraData;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class RedGreenAnalyzer implements ImageAnalysis.Analyzer{

        private static final String TAG = "RedGreenAnalyzer";

        private ICameraData iCameraData;

        public RedGreenAnalyzer(ICameraData iCameraData){
            this.iCameraData = iCameraData;
        }

        @Override
        public void analyze(ImageProxy image) {
            // Image analysis code goes here
            // Process incoming image data
            //On CameraX Once RGBA_8888 is selected, the output image format will be PixelFormat.RGBA_8888,
            // which has only one image plane (R, G, B, A pixel by pixel) with paddings.
            //Source: https://developer.android.com/reference/android/graphics/ImageFormat#RGBA_8888

            //Get the image buffer
            //byte[] data = image.getPlanes()[0].getBuffer().array();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            image.close();

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

            if (iCameraData != null)
                iCameraData.onRedGreenAVGChanged(redAvg, greenAvg);
        }

        private double processAvg(byte[] data) {
            int[] pixels = new int[data.length];
            for(int i=0; i < data.length; i++){
                pixels[i] = (data[i] & 0xFF);
            }
            return Arrays.stream(pixels).average().orElse(0.0);
        }
}

