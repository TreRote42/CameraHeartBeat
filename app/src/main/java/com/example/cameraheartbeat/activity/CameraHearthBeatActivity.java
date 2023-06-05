package com.example.cameraheartbeat.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraProvider;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.cameraheartbeat.R;
import com.example.cameraheartbeat.cameraUseCases.LuminosityAnalyzer;
import com.example.cameraheartbeat.cameraUseCases.RedGreenAnalyzer;
import com.example.cameraheartbeat.myInterface.IHeartBeat;
import com.example.cameraheartbeat.myInterface.IMyAccelerometer;
import com.example.cameraheartbeat.myInterface.IPlotBeat;
import com.example.cameraheartbeat.myInterface.IRedGreenAVG;
import com.example.cameraheartbeat.utilities.HeartBeatCalculator;
import com.example.cameraheartbeat.utilities.MyAccelerometer;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Arrays;

public class CameraHearthBeatActivity extends AppCompatActivity implements IRedGreenAVG, IHeartBeat, IPlotBeat, IMyAccelerometer {
    private final int INIT_BUFFER = 50;
    private static final String TAG = "MainActivity";

    Camera camera;
    CameraProvider cameraProvider;
    private Preview preview;
    private PreviewView viewFinder;

    //start time of image analysis
    long startTime;
    long endTime;

    //average red and green values
    double[] redAvg = new double[INIT_BUFFER];
    double[] greenAvg = new double[INIT_BUFFER];
    int iterator = 0;
    int fillBUFFER = INIT_BUFFER;
    boolean isCalculating = false;
    boolean userIsStill = true;
    private HeartBeatCalculator heartBeatCalculator;
    private MyAccelerometer myAccelerometer;
    private Handler handler;
    private Runnable resetUserIsStillRunnable = () -> userIsStill = true;
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"};

    TextView tvRedAvg, tvGreenAvg, tvMessage, tvIsStill;

    private LineChart chart;
    private SeekBar seekBarX, seekBarY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myAccelerometer = new MyAccelerometer(this, 1.0f);
        handler = new Handler();

        tvRedAvg = findViewById(R.id.tvRedAvg);
        tvGreenAvg = findViewById(R.id.tvGreenAvg);
        tvMessage = findViewById(R.id.tvMessage);
        tvIsStill = findViewById(R.id.tvIsStill);
        chart = findViewById(R.id.chart);

        chart.animateX(1200, Easing.EaseInSine);
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setGranularity(1f);
        //chart.getXAxis().setValueFormatter();


        instantiateCameraPreview();

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }


    private void instantiateCameraPreview() {
        preview = new Preview.Builder().build();
        viewFinder = findViewById(R.id.viewFinder);

        // The use case is bound to an Android Lifecycle with the following code
        //camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview);

// PreviewView creates a surface provider, using a Surface from a different
// kind of view will require you to implement your own surface provider.
        //preview.previewSurfaceProvider = viewFinder.getSurfaceProvider();
    }


    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            ProcessCameraProvider cameraProvider;
            Camera camera;


            Preview preview = new Preview.Builder().build();
            preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

            CameraSelector cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build();

            ImageAnalysis luminosityAnalyzer = new ImageAnalysis.Builder()
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build();
            luminosityAnalyzer.setAnalyzer(ContextCompat.getMainExecutor(this), new LuminosityAnalyzer());

            ImageAnalysis red_green_Analyzer = new ImageAnalysis.Builder()
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .setTargetResolution(new Size(1080, 1080))
                    .build();
            red_green_Analyzer.setAnalyzer(ContextCompat.getMainExecutor(this), new RedGreenAnalyzer(this));

            try {
                cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, red_green_Analyzer);
                //camera.getCameraControl().startFocusAndMetering(cameraSelector, null);
                camera.getCameraInfo();
            } catch (Exception e) {
                Log.e(TAG, "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }


    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(
                    this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
                startTime = System.currentTimeMillis();
            } else {
                Log.d(TAG, "Permissions not granted by the user.");
                finish();
            }
        }
    }

    @Override
    public void onRedGreenAVGChanged(double red, double green) {
        myAccelerometer.start();
        startTime = System.currentTimeMillis();
        tvRedAvg.setText("RedAvg: " + red);
        tvGreenAvg.setText("GreenAvg: " + green);
        if (fillBUFFER > 0) {
            redAvg[iterator] = red;
            greenAvg[iterator] = green;
            iterator = (iterator + 1) % INIT_BUFFER;
            fillBUFFER--;
        } else if (Arrays.stream(redAvg).average().orElse(0.0) < Arrays.stream(greenAvg).average().orElse(0.0) * 12 || red < green * 5) {
            redAvg[iterator] = red;
            greenAvg[iterator] = green;
            iterator = (iterator + 1) % INIT_BUFFER;
            isCalculating = false;
            tvMessage.setText("Metti il dito davanti alla fotocamera");
        } else {
            if (!isCalculating) {
                isCalculating = true;
                tvMessage.setText("Calcolo in corso...");
                heartBeatCalculator = new HeartBeatCalculator(this, startTime);
            }
            if (heartBeatCalculator != null) {
                heartBeatCalculator.calculateNewHeartBeat(red, green, startTime);
            } else {
                throw new NullPointerException("HeartBeatCalculator is null");
            }
        }
    }

    public void onHeartBeatChanged(int heartBeat) {
        tvMessage.setText("Battito: " + heartBeat);
    }

    public void plotBeat(double[] redAvg, double[] greenAvg, long[] time) {
        chart.clear();
        ArrayList<Entry> valuesRed = new ArrayList<>();
        ArrayList<Entry> valuesGreen = new ArrayList<>();
        for (int i = 0; i < redAvg.length; i++) {
            valuesRed.add(new Entry(time[i], (float) redAvg[i]));
            valuesGreen.add(new Entry(time[i], (float) greenAvg[i]));
        }
        LineDataSet setRed = new LineDataSet(valuesRed, "Red");
        setRed.setColor(Color.RED);
        setRed.setLineWidth(2.5f);
        LineDataSet setGreen = new LineDataSet(valuesGreen, "Green");
        setGreen.setColor(Color.GREEN);
        setGreen.setLineWidth(2.5f);
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(setRed);
        //dataSets.add(setGreen);
        LineData dataRed = new LineData(dataSets);
        chart.setData(dataRed);
        chart.invalidate();
    }


    public void onMovementDetected(boolean isStill) {
        userIsStill = false;
        if (!isStill) {
            tvIsStill.setText("");
            userIsStill = true;
        }
        else {
            tvIsStill.setText("STAI FERMO!");
        }
        handler.removeCallbacks(resetUserIsStillRunnable);
        handler.postDelayed(resetUserIsStillRunnable, 2000);
    }
}

