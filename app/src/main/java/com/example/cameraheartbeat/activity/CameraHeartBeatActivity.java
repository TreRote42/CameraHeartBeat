package com.example.cameraheartbeat.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.widget.ImageButton;
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
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Arrays;

public class CameraHeartBeatActivity extends AppCompatActivity implements IRedGreenAVG, IHeartBeat, IPlotBeat, IMyAccelerometer {
    private final int INIT_BUFFER = 40;
    private static final String TAG = "CameraHeartBeatActivity";

    Camera camera;
    ProcessCameraProvider cameraProvider;
    private Preview preview;
    private PreviewView viewFinder;


    //start time of image analysis
    long startTime;
    long endTime;

    //average red and green values
    double[] redSet = new double[INIT_BUFFER];
    double[] greenSet = new double[INIT_BUFFER];
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
    private int age;
    TextView tvRedAvg, tvGreenAvg, tvMessage, tvIsStill;
    ImageButton toggleFlashlight;
    private boolean isFlashOn = false;

    private LineChart chart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_heart_beat);
        myAccelerometer = new MyAccelerometer(this, 1.0f);
        handler = new Handler();
        age = getIntent().getIntExtra("age", 20);

        toggleFlashlight = findViewById(R.id.toggleFlashlight);
        tvRedAvg = findViewById(R.id.tvRedAvg);
        tvGreenAvg = findViewById(R.id.tvGreenAvg);
        tvMessage = findViewById(R.id.tvMessage);
        tvIsStill = findViewById(R.id.tvIsStill);
        chart = findViewById(R.id.chart);

        chart.animateX(1200, Easing.EaseInSine);
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setNoDataText("");
        chart.setKeepScreenOn(true);
        chart.getLegend().setEnabled(false);
        chart.getXAxis().setEnabled(false);
        chart.getAxisLeft().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
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
                toggleFlashlight.setOnClickListener(v -> {
                    if (camera.getCameraInfo().hasFlashUnit()) {
                        if (isFlashOn) {
                            camera.getCameraControl().enableTorch(false);
                            isFlashOn = false;
                            toggleFlashlight.setImageResource(R.drawable.baseline_flashlight_off_24);
                        } else {
                            camera.getCameraControl().enableTorch(true);
                            isFlashOn = true;
                            toggleFlashlight.setImageResource(R.drawable.baseline_flashlight_on_24);
                        }
                    }
                });
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
            redSet[iterator] = red;
            greenSet[iterator] = green;
            iterator = (iterator + 1) % INIT_BUFFER;
            fillBUFFER--;
        } else if (Arrays.stream(redSet).average().orElse(0.0) < Arrays.stream(greenSet).average().orElse(0.0) * 13 || red < green * 8) {
            redSet[iterator] = red;
            greenSet[iterator] = green;
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

    public void finalHeartBeat(int heartBeat) {
        myAccelerometer.stop();
        cameraProvider.unbindAll();
        Intent intent = new Intent(this, DataAnalysisActivity.class);
        int age = getIntent().getIntExtra("age", 0);
        intent.putExtra("age", age);
        intent.putExtra("heartBeat", heartBeat);
        startActivity(intent);
    }

    public void plotBeat(double[] redAvg, double[] greenAvg, long[] time, int start) {
        int toInsert = 0;
        int lenght = redAvg.length;
        chart.clear();
        ArrayList<Entry> valuesRed = new ArrayList<>();
        ArrayList<Entry> valuesGreen = new ArrayList<>();
        for (int i = 0; i < lenght; i++) {
            toInsert = (start + i) % lenght;
            valuesRed.add(new Entry(time[toInsert], (float) redAvg[toInsert]));
            valuesGreen.add(new Entry(time[toInsert], (float) greenAvg[toInsert]));
        }
        LineDataSet setRed = new LineDataSet(valuesRed, "BATTITO");
        setRed.setColor(Color.RED);
        setRed.setDrawCircles(false);
        setRed.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        setRed.setLineWidth(2.5f);
        LineDataSet setGreen = new LineDataSet(valuesGreen, "Green");
        setGreen.setColor(Color.GREEN);
        setGreen.setLineWidth(2.5f);
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(setRed);
        //dataSets.add(setGreen);
        LineData dataRed = new LineData(dataSets);
        dataRed.setDrawValues(false);
        chart.setData(dataRed);
        chart.setDrawGridBackground(false);
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

