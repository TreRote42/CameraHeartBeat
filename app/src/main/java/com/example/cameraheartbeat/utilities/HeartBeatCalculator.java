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
        int start = lastInserted;
        double[] red = redAvg.clone();
        double[] green = greenAvg.clone();
        long[] time = timeStamp.clone();
        double redAvg = Arrays.stream(red).average().orElse(Double.NaN);
        double greenAvg = Arrays.stream(green).average().orElse(Double.NaN);
        double toInsertR = red[start];
        long toInsertT = time[start];
        double end = red[Math.abs(start-1)%BUFFER];
        double tempR;
        long tempT;
        int last = start;
        for (int i = 0; i < red.length; i++) {
            last = (last+(BUFFER-start))%BUFFER;
            tempR = red[last];
            tempT = time[last];
            red[last] = toInsertR-redAvg;
            time[last] = toInsertT;
            toInsertR = tempR;
            toInsertT = tempT;
        }
        Log.i(TAG, "lastR " + red[BUFFER-1]+" end red:" + end);
        iHeartBeat.onHeartBeatChanged((int)calculateFFT(red, BUFFER));
        iPlotBeat.plotBeat(red, green, time);
    }

    private double calculateFFT(double[] signal, int mNumberOfFFTPoints)
    {
        double[] magnitude = new double[mNumberOfFFTPoints/2];
        DoubleFFT_1D fft = new DoubleFFT_1D(mNumberOfFFTPoints);
        double[] fftData = new double[mNumberOfFFTPoints*2];
        double max_index=-1;
        double max_magnitude=-1;
        final float sampleRate=60;
        double frequency;
        for (int i=0;i<mNumberOfFFTPoints;i++){
            //fftData[2 * i] = buffer[i+firstSample];
            fftData[2 * i] = signal[i];  //da controllare
            fftData[2 * i + 1] = 0;
            fft.complexForward(fftData);
        }
        for(int i = 0; i < mNumberOfFFTPoints/2; i++){
            magnitude[i]=Math.sqrt((fftData[2*i] * fftData[2*i]) + (fftData[2*i + 1] * fftData[2*i + 1]));
            if (max_magnitude<magnitude[i]){
                max_magnitude=magnitude[i];
                max_index=i;
            }
        }
        return frequency=sampleRate*(double)max_index/(double)mNumberOfFFTPoints;
    }



}
