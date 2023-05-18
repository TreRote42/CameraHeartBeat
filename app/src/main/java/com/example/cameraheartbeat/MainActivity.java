package com.example.cameraheartbeat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraProvider;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.core.ResolutionInfo;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.widget.TextView;

import com.example.cameraheartbeat.cameraUseCases.LuminosityAnalyzer;
import com.example.cameraheartbeat.cameraUseCases.RedGreenAnalyzer;
import com.example.cameraheartbeat.myInterface.IRedGreenAVG;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Arrays;


public class MainActivity extends AppCompatActivity implements IRedGreenAVG {
    private final int INIT_BUFFER = 100;
    private static final String TAG = "MainActivity";

    Camera camera;
    CameraProvider cameraProvider;
    private Preview  preview;
    private PreviewView viewFinder;

    //start time of image analysis
    long startTime;
    long endTime;

    //average red and green values
    double[] redAvg = new double[INIT_BUFFER];
    double[] greenAvg = new double[INIT_BUFFER];
    int iterator = 0;
    int fillBUFFER = INIT_BUFFER;
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[] {"android.permission.CAMERA", "android.permission.RECORD_AUDIO"};

    TextView tvRedAvg, tvGreenAvg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvRedAvg = findViewById(R.id.tvRedAvg);
        tvGreenAvg = findViewById(R.id.tvGreenAvg);

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

        cameraProviderFuture .addListener(() -> {
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
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, luminosityAnalyzer, red_green_Analyzer);
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
        long endTime = System.currentTimeMillis();
        tvRedAvg.setText("RedAvg: "+ red);
        tvGreenAvg.setText("GreenAvg: "+ green);
        Log.d(TAG, "onRedGreenAVGChanged: " + (endTime - startTime));
        startTime = endTime;
        if (fillBUFFER > 0) {
            redAvg[iterator] = red;
            greenAvg[iterator] = green;
            iterator= (iterator+1) % INIT_BUFFER;
            fillBUFFER--;
        }
        else if (Arrays.stream(redAvg).average().orElse(0.0) < Arrays.stream(greenAvg).average().orElse(0.0) * 15 || red < green * 5) {
            redAvg[iterator] = red;
            greenAvg[iterator] = green;
            iterator= (iterator+1) % INIT_BUFFER;
        }
        else
            Log.i (TAG, "Dito rilevamento battito cardiaco");
    }

}