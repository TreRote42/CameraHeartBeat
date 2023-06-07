package com.example.cameraheartbeat.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cameraheartbeat.MainActivity;
import com.example.cameraheartbeat.R;

public class DataAnalysisActivity extends AppCompatActivity {

    private TextView tvHeartRate;
    private Button bttBack;
    private Button bttRestingAnalysis;
    private Button bttExerciseAnalysis;
    private TextView tvExplanation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_analysis);

        tvHeartRate = findViewById(R.id.tvHeartRate);
        tvExplanation = findViewById(R.id.tvExplanation);
        bttRestingAnalysis = findViewById(R.id.bttRestingAnalysis);
        bttExerciseAnalysis = findViewById(R.id.bttExerciseAnalysis);
        bttBack = findViewById(R.id.bttBack);

        // Get heart rate value from intent
        int heartRate = getIntent().getIntExtra("heartBeat", 0);
        int age = getIntent().getIntExtra("age", 0);
        tvHeartRate.setText("Heart Rate: " + heartRate);

        bttBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DataAnalysisActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        bttRestingAnalysis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (heartRate >= 60 && heartRate <= 100) {
                    tvExplanation.setText("Il tuo battito cardiaco a riposo è nella norma.");
                    showExplanation();
                } else if (heartRate < 60 && heartRate > 20) {
                    tvExplanation.setText("Il tuo battito cardiaco a riposo è troppo basso\n A meno che tu non sia un atleta potresti soffrire di bradicardia.");
                    showExplanation();
                } else if (heartRate > 100 && heartRate < 220) {
                    tvExplanation.setText("Il tuo battito cardiaco a riposo è troppo alto!\n Potresti soffrire di tachicardia.");
                    showExplanation();
                } else {
                    tvExplanation.setText("C'è stato un errore nella misurazione del battito cardiaco.");
                    showExplanation();
                }
            }
        });

        bttExerciseAnalysis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int maxHeartRate = 220 - age;
                int lowTarget = (int) (maxHeartRate * 0.6);
                int highTarget = (int) (maxHeartRate * 0.8);
                if (heartRate >= lowTarget && heartRate <= highTarget) {
                    tvExplanation.setText("L'intensità del tuo esercizio è perfetta!\n DAJE ROMA DAJE YAYAUUUUUUUUUUU!.");
                    showExplanation();
                } else if (heartRate < lowTarget) {
                    tvExplanation.setText("L'intensità del tuo esercizio è troppo bassa.\n SPINGI UOMO!");
                    showExplanation();
                } else if (heartRate > highTarget) {
                    tvExplanation.setText("L'intensità del tuo esercizio è troppo alta.\n MA CHE SEI MATTO AO?");
                    showExplanation();
                } else {
                    tvExplanation.setText("C'è stato un errore nella misurazione del battito cardiaco.");
                    showExplanation();
                }
            }
        });
    }

    public void showExplanation() {
        bttExerciseAnalysis.setVisibility(View.GONE);
        bttRestingAnalysis.setVisibility(View.GONE);
        tvExplanation.setVisibility(View.VISIBLE);
        bttBack.setVisibility(View.VISIBLE);
    }
}
