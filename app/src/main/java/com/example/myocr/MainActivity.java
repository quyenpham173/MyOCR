package com.example.myocr;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.tesseract.R;

import java.util.Locale;


public class MainActivity extends AppCompatActivity {
    private TextToSpeech textToSpeech;
    final String contentWelcome = "Hello, Welcome to reading assitance application, please press screen to take photo";
    private RelativeLayout mh;
    TextView tvTest;
    /*    static {
            System.loadLibrary("Preprocess");
        }*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvTest = findViewById(R.id.txtView);
//        tvTest.setText(stringFromJNI());
        mh = (RelativeLayout) findViewById(R.id.activity_main);
        mh.setBackgroundResource(R.drawable.anh2);

        textToSpeech = new TextToSpeech(MainActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR){
                    textToSpeech.setLanguage(Locale.US);
                    textToSpeech.speak(contentWelcome, TextToSpeech.QUEUE_FLUSH,null);
                }
            }
        });
        RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.activity_main);
        relativeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, CaptureImage.class);
                startActivity(intent);

            }
        });
    }
    public native String stringFromJNI();
}
