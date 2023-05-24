package com.example.cameraheartbeat.utilities;

import android.content.Context;

import com.example.cameraheartbeat.myInterface.IHearthBeat;
import com.example.cameraheartbeat.myInterface.IPlotBeat;

import java.util.Arrays;

public class HearthBeatCalculator {
    private final String TAG = "HearthBeatCalculator";

    private final int BUFFER = 200;
    private IHearthBeat iHearthBeat;
    private IPlotBeat iPlotBeat;

    private double[] redAvg = new double[BUFFER];
    private double[] greenAvg = new double[BUFFER];
    private long[] timeStamp = new long[BUFFER];
    private long startTime ;
    private int inserted ;
    private int lastInserted;


    public HearthBeatCalculator(Context context, long startTime) {
        this.iHearthBeat = (IHearthBeat) context;
        this.iPlotBeat = (IPlotBeat) context;
        this.startTime = startTime;
        this.inserted = BUFFER;
        this.lastInserted = -1;
    }


    public void calculateNewHearthBeat(double red, double green, long time) {
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
        for (int i = 0; i < red.length; i++) {
            red[i] = red[i] - redAvg;
            green[i] = green[i] - greenAvg;
        }

        iPlotBeat.plotBeat(red, green, time);
    }


}
