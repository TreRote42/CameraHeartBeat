package com.example.cameraheartbeat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cameraheartbeat.activity.CameraHeartBeatActivity;

public class MainActivity extends AppCompatActivity {

    private Button cameraButton;
    private EditText etAge;

    private static final String TAG = "MainActivity";
    private int savedAge;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraButton = findViewById(R.id.bttCamera);
        etAge = findViewById(R.id.etAge);
        preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        editor = preferences.edit();
        savedAge = preferences.getInt("age", 0);
        etAge.setText(String.valueOf(savedAge));
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CameraHeartBeatActivity.class);
                String ageString = etAge.getText().toString();
                try {
                    int age = Integer.parseInt(ageString);
                    if(age <= 0 || age > 120) {
                        throw new Exception();
                    }
                    editor.putInt("age", age);
                    editor.apply();
                    intent.putExtra("age", age);
                }
                catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Please enter a valid age", Toast.LENGTH_SHORT).show();
                    return;
                }
                startActivity(intent);
            }
        });
    }
}