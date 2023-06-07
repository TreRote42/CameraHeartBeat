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
    private final int HEARTBEAT_BUFFER = 400;
    private IHeartBeat iHeartBeat;
    private IPlotBeat iPlotBeat;

    private double[] redSet;
    private double redAVG;
    private double[] greenSet;
    private double greenAVG;
    private long[] timeStamp ;
    private ArrayList<Integer> heartBeatSet ;
    private int avgHeartBeat ;
    private double lastFreq;


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
        this.lastFreq = 0;
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
        double greenAvg = Arrays.stream(green).average().getAsDouble()/2; //divided by 2 to get more higher green values
        for (int i = 0; i < BUFFER; i++) {
            red[i] = red[i] - redAvg;
            green[i] = green[i] - greenAvg;
        }
        double[] redFFT = calculateFFT(red, BUFFER, time[end]-time[start]);
        double[] greenFFT = calculateFFT(green, BUFFER, time[end]-time[start]);
        int newHeartBeat = bestHeartBeat(redFFT, greenFFT);
        iPlotBeat.plotBeat(red, green, time, start);
        iHeartBeat.onHeartBeatChanged(newHeartBeat);
        addNewHeartBeat(newHeartBeat);
    }

    private double[] calculateFFT(double[] signal, int numberOfSample, float sampleRate)
    {
        sampleRate = numberOfSample/(sampleRate/1000);
        double magnitude = 0;
        double max2 = 0;
        double max3 = 0;
        double maxFreq = 0;
        double minFreq = (45.0/60*(2 * numberOfSample)/sampleRate); //45 bpm as minimum
        double[] samples = new double[2 * numberOfSample];
        double[] output = new double[4];

        for (int i = 0; i < samples.length; i++)
            samples[i] = 0;

        for (int x = 0; x < numberOfSample; x++) {
            samples[x] = signal[x];
        }

        DoubleFFT_1D fft = new DoubleFFT_1D(numberOfSample);
        fft.realForward(samples);

        for (int x = 0; x < 2 * numberOfSample; x++) {
            samples[x] = Math.abs(samples[x]);
        }

        for (int p =(int) Math.ceil(minFreq); p < numberOfSample; p++) {
            if (magnitude < samples[p]) {
                max3 = max2;
                max2 = maxFreq;
                magnitude = samples[p];
                maxFreq = p;

            }
        }

        Log.i(TAG, "maxFreq: " + maxFreq + " , mag:"+ samples[(int)maxFreq] + "max2: " + max2 + " , mag:"+ samples[(int)max2] + "max3: " + max3 + " , mag:"+ samples[(int)max3]);
        double frequency = maxFreq * sampleRate / (2 * numberOfSample);
        double frequency2 = max2 * sampleRate / (2 * numberOfSample);
        output[0] = (int)(frequency*60);
        output[1] = samples[(int)maxFreq];
        output[2] = (int)(frequency2*60);
        output[3] = samples[(int)max2];
        return output;
    }

    public int bestHeartBeat(double[] heartBeatR, double[] heartBeatG) {
        if (heartBeatR[0] == heartBeatG[0])
            return (int) heartBeatR[0];
        else if (heartBeatR[0] == heartBeatG[2])
            return (int) heartBeatR[0];
        else if (heartBeatR[2] == heartBeatG[0])
            return (int) heartBeatR[2];
        else if (heartBeatR[2] == heartBeatG[2] && (heartBeatR[1] < heartBeatR[3] * 1.2 || heartBeatG[1] < heartBeatR[3] * 1.2))
            return (int) heartBeatR[2];
        else if (Math.abs(heartBeatR[0] - heartBeatG[0]) <= 3)
            return (int) (heartBeatR[0] + heartBeatG[0] / 2);
        else if (Math.abs(heartBeatR[0] - heartBeatG[2]) <= 3)
            return (int) (heartBeatR[0] + heartBeatG[2] / 2);
        else if (Math.abs(heartBeatR[2] - heartBeatG[2]) <= 3)
            return (int) (heartBeatR[2] + heartBeatG[2] / 2);
        else if (Math.abs(heartBeatR[2] - heartBeatG[0]) <= 3)
            return (int) (heartBeatR[2] + heartBeatG[0] / 2);
        else if (heartBeatR[1] > heartBeatG[1] * 15)
            return (int) heartBeatR[0];
        else if (heartBeatG[1] > heartBeatR[1] * 15)
            return (int) heartBeatG[0];
        else
            return (int) (heartBeatR[0] + heartBeatG[0]) / 2;

    }

}
