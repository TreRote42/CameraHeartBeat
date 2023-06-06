package com.example.cameraheartbeat.utilities;

import android.content.Context;
import android.util.Log;

import com.example.cameraheartbeat.myInterface.IHeartBeat;
import com.example.cameraheartbeat.myInterface.IPlotBeat;

import org.jtransforms.fft.DoubleFFT_1D;

import java.util.ArrayList;
import java.util.Arrays;

public class HeartBeatCalculator {
    private final String TAG = "HearthBeatCalculator";

    private final int BUFFER = 200;
    private final int AVG_BUFFER = 40;
    private final int HEARTBEAT_BUFFER = 350;
    private IHeartBeat iHeartBeat;
    private IPlotBeat iPlotBeat;

    private double[] redSet;
    private double redAVG;
    private double[] greenSet;
    private double greenAVG;
    private long[] timeStamp ;
    private ArrayList<Integer> heartBeatSet ;
    private int avgHeartBeat ;


    private long startTime ;
    private int inserted ;
    private int lastInserted;
    private boolean isCalculating = false;


    public HeartBeatCalculator(Context context, long startTime) {
        this.iHeartBeat = (IHeartBeat) context;
        this.iPlotBeat = (IPlotBeat) context;
        this.startTime = startTime;
        this.inserted = BUFFER;
        this.lastInserted = -1;
        this.redSet = new double[BUFFER];
        this.greenSet = new double[BUFFER];
        this.timeStamp = new long[BUFFER];
        this.isCalculating = true;
    }


    public void calculateNewHeartBeat(double red, double green, long time) {
        if (!isCalculating)
            return;
        lastInserted = (lastInserted +1)%BUFFER;
        redSet[lastInserted] = red;
        greenSet[lastInserted] = green;
        timeStamp[lastInserted] = time- this.startTime;
        if (inserted > 0)
            inserted--;
        else if (inserted == 0){
            inserted--;
            this.heartBeatSet = new ArrayList<>();
            calculateHeartBeat();
        }
        else{
            calculateHeartBeat();
        }
    }

    private void addNewHeartBeat(int heartBeat){
        if ( heartBeatSet.size() == 0){
            heartBeatSet.add(heartBeat);
            avgHeartBeat = heartBeat;
            return;
        }
        else if (heartBeatSet.size() < HEARTBEAT_BUFFER){
            heartBeatSet.add(heartBeat);
            avgHeartBeat = (avgHeartBeat+heartBeat)/2; //non facciamo la media pesata, ma la media aritmetica ogni volta in modo da dare più importanza a valori uguali ripetuti, e meno a singloi valori fuori scala
        }
        else{
            avgHeartBeat = (avgHeartBeat+heartBeat)/2;
            iHeartBeat.finalHeartBeat(avgHeartBeat);
            isCalculating = false;
            }
        }

    private void calculateHeartBeat(){
        int start = (lastInserted+1)%BUFFER;
        int end = lastInserted;
        double[] red = redSet.clone();
        double[] green = greenSet.clone();
        long[] time = timeStamp.clone();
        double redAvg = Arrays.stream(red).average().getAsDouble();
        double greenAvg = Arrays.stream(green).average().getAsDouble();
        for (int i = 0; i < BUFFER; i++) {
            red[i] = red[i] - redAvg;
            green[i] = green[i] - greenAvg;
        }
        int newHeartBeat = (int)calculateFFT(red, BUFFER, time[end]-time[start]);
        Log.i(TAG, "newGreenHeartBeat: " + calculateFFT(green, BUFFER, time[end]-time[start]));
        iHeartBeat.onHeartBeatChanged(newHeartBeat);
        addNewHeartBeat(newHeartBeat);
        iPlotBeat.plotBeat(red, green, time, start);
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
    }


}
