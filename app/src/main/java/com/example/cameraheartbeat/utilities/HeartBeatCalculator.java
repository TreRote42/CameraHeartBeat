package com.example.cameraheartbeat.utilities;

import android.content.Context;
import android.util.Log;

import com.example.cameraheartbeat.myInterface.IHeartBeat;
import com.example.cameraheartbeat.myInterface.IPlotBeat;

import org.jtransforms.fft.DoubleFFT_1D;

import java.util.Arrays;

public class HeartBeatCalculator {
    private final String TAG = "HearthBeatCalculator";

    private final int BUFFER = 200;
    private IHeartBeat iHeartBeat;
    private IPlotBeat iPlotBeat;

    private double[] redAvg = new double[BUFFER];
    private double[] greenAvg = new double[BUFFER];
    private long[] timeStamp = new long[BUFFER];
    private long startTime ;
    private int inserted ;
    private int lastInserted;


    public HeartBeatCalculator(Context context, long startTime) {
        this.iHeartBeat = (IHeartBeat) context;
        this.iPlotBeat = (IPlotBeat) context;
        this.startTime = startTime;
        this.inserted = BUFFER;
        this.lastInserted = -1;
    }


    public void calculateNewHeartBeat(double red, double green, long time) {
        lastInserted = (lastInserted +1)%BUFFER;
        redAvg[lastInserted] = red;
        greenAvg[lastInserted] = green;
        timeStamp[lastInserted] = time- this.startTime;
        if (inserted > 0)
            inserted--;
        else if (inserted == 0){
            inserted--;
            calculateHeartBeat();
        }
        else{
            calculateHeartBeat();
        }
    }

    private void calculateHeartBeat(){
        int end = lastInserted;
        int start = (lastInserted+1)%BUFFER;
        double[] red = redAvg.clone();
        double[] green = greenAvg.clone();
        long[] time = timeStamp.clone();
        double redAvg = Arrays.stream(red).average().orElse(Double.NaN);
        double greenAvg = Arrays.stream(green).average().orElse(Double.NaN);
        double toInsertR = red[start];
        long toInsertT = time[start];
        double tempR;
        long tempT;
        int last = start;
        for (int i = 0; i < red.length; i++) {
            /*last = (last+(BUFFER-start))%BUFFER;
            tempR = red[last];
            tempT = time[last];
            red[last] = toInsertR-redAvg;
            time[last] = toInsertT;
            toInsertR = tempR;
            toInsertT = tempT;*/
            red[i] = red[i]-redAvg;
            green[i] = green[i]-greenAvg;
        }
        iHeartBeat.onHeartBeatChanged((int)calculateFFT(red, BUFFER, time[end]-time[start]));
        iPlotBeat.plotBeat(red, green, time);
    }

    private double calculateFFT(double[] signal, int numberOfSample, float sampleRate)
    {
        sampleRate = numberOfSample/(sampleRate/1000);
        double magnitude = 0;
        double max2 = 0;
        double max3 = 0;
        double max4 = 0;
        double max5 = 0;
        double maxFreq = 0;
        double frequency;
        double minFreq = (45.0/60*(2 * numberOfSample)/sampleRate); //45 bpm as minimum
        double[] output = new double[2 * numberOfSample];

        for (int i = 0; i < output.length; i++)
            output[i] = 0;

        for (int x = 0; x < numberOfSample; x++) {
            output[x] = signal[x];
        }

        DoubleFFT_1D fft = new DoubleFFT_1D(numberOfSample);
        fft.realForward(output);

        for (int x = 0; x < 2 * numberOfSample; x++) {
            output[x] = Math.abs(output[x]);
        }

        for (int p =(int) Math.ceil(minFreq); p < numberOfSample; p++) {
            if (magnitude < output[p]) {
                max5 = max4;
                max4 = max3;
                max3 = max2;
                max2 = maxFreq;
                magnitude = output[p];
                maxFreq = p;

            }
        }
        
        Log.i(TAG, "maxFreq: " + maxFreq + " " + "max2: " + max2 + " " + "max3: " + max3+ " " + "max4: " + max4 + " " + "max5: " + max5);
        frequency = maxFreq * sampleRate / (2 * numberOfSample);
        return frequency*60;
        /*double[] magnitude = new double[numberOfSample/2];
        DoubleFFT_1D fft = new DoubleFFT_1D(numberOfSample);
        double[] fftData = new double[numberOfSample*2];
        double max_index=-1;
        double max_magnitude=-1;
        float freqResolution = numberOfSample/(sampleRate/1000);
        for (int i=0;i<numberOfSample;i++){
            fftData[2 * i] = signal[i];
            fftData[2 * i + 1] = 0;
        }
        fft.realForward(fftData);
        max_magnitude=fftData[1];
        max_index=1;
        for(int i = 0; i < numberOfSample/2; i++){
            //Log.i(TAG, "fftData: " + fftData[2*i] + " " + fftData[2*i + 1]);
            magnitude[i]=fftData[2*i] ;
            if (max_magnitude<magnitude[i]){
                max_magnitude=magnitude[i];
                max_index=i;
            }
            Log.d(TAG, "magnitude: " + magnitude[i] + " " + i);
        }
        Log.i(TAG, "max_index: " + max_index);
        return max_index;*/
    }



}
