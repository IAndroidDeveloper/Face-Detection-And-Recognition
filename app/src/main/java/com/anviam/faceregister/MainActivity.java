package com.anviam.faceregister;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.anviam.faceregister.face_recognition.FaceClassifier;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    Button registerButton;
    Button RecognizeButton;
    public static HashMap<String, FaceClassifier.Recognition> registered = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        registerButton = findViewById(R.id.registerButton);
        RecognizeButton = findViewById(R.id.recognizeButton);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, RegisterActivity.class));
            }
        });
        RecognizeButton.setOnClickListener(view -> startActivity(new Intent(MainActivity.this, RecognizeActivity.class)));
    }


    //This method Performs the Functionality to select the image from our Gallery Files

}
