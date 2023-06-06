package com.example.cameraheartbeat.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cameraheartbeat.R;

public class DataAnalysisActivity extends AppCompatActivity {

    private TextView tvHeartRate;
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

        // Get heart rate value from intent
        int heartRate = getIntent().getIntExtra("heartBeat", 0);
        tvHeartRate.setText("Heart Rate: " + heartRate);

        // Set button click listeners
            bttRestingAnalysis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(heartRate >= 60 && heartRate <= 100) {
                   tvExplanation.setText("Il tuo battito cardiaco a riposo è nella norma.");
                   showExplanation();
                } else if(heartRate < 60) {
                  tvExplanation.setText("Il tuo battito cardiaco a riposo è troppo basso\n A meno che tu non sia un atleta potresti soffrire di bradicardia.");
                  showExplanation();
                } else if(heartRate > 100) {
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
                //Borg scale
            }
        });
    }
    public void showExplanation() {
        bttExerciseAnalysis.setVisibility(View.GONE);
        bttRestingAnalysis.setVisibility(View.GONE);
        tvExplanation.setVisibility(View.VISIBLE);
    }
}
