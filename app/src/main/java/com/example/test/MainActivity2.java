package com.example.test;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.example.test.R.layout;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.BitSet;

public final class MainActivity2 extends AppCompatActivity {
    ImageView imgView;
    TextView colorValues;
    View colorViews;
    Bitmap bitmap;
    Button sendButton;
    ConnectedThread connectedThread;
    int r;
    int g;
    int b;


    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(layout.content_main);
        imgView = findViewById(R.id.colorPicker);
        colorValues = findViewById(R.id.displayValues);
        colorViews = findViewById(R.id.displayColors);
        sendButton = findViewById(R.id.sendButton);

        connectedThread = MyApplication.getApplication().getCurrentConnectedThread();

        imgView.setDrawingCacheEnabled(true);
        imgView.buildDrawingCache(true);

        if(connectedThread.getSocket().isConnected()) {
            Toast.makeText(MainActivity2.this, "Bluetooth in color menu successfully connected", Toast.LENGTH_LONG).show();
        }

        imgView.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event){
                if(event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE){
                    bitmap = imgView.getDrawingCache();
                    if(event.getX() >= bitmap.getWidth() || event.getY() >= bitmap.getHeight()){
                        return true;
                    }
                    if(event.getX() < 0 || event.getY() < 0){
                        return true;
                    }
                    int pixels = bitmap.getPixel((int)event.getX(), (int)event.getY());

                    r = Color.red(pixels);
                    g = Color.green(pixels);
                    b = Color.blue(pixels);

                    String hex = "#" + Integer.toHexString(pixels);
                    colorViews.setBackgroundColor(Color.rgb(r,g,b));
                    colorValues.setText("RGB: " + r + ", " + g + ", " + b + "\nHex: " + hex);
                }
                return true;
            }
        });
        sendButton.setOnClickListener((View.OnClickListener) (new View.OnClickListener() {
            public final void onClick(View it) {

                BitSet temp = connectedThread.getBitSet();
                if(connectedThread.getBlinkMode()){
                    temp.set(0, false);
                    temp.set(1, true);
                    temp.set(2, false);
                    connectedThread.setBlinkMode(false);
                } else if(connectedThread.getMusicMode()){
                    temp.set(0, true);
                    temp.set(1, false);
                    temp.set(2, true);
                    connectedThread.setMusicMode(false);
                } else {
                    temp.set(1, 2, false);
                    temp.set(0, true);
                    connectedThread.setStaticMode(false);
                }
                temp.set(8, 440, true);
                byte[] RGB = temp.toByteArray();
                RGB[1] = (byte) r;
                RGB[2] = (byte) g;
                RGB[3] = (byte) b;

                for(int l = 4; l < RGB.length; l++){
                    if(RGB[l] == -1){
                        RGB[l] = 0;
                    }
                }

                try {
                    connectedThread.getMmOutStream().write(RGB);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                BitSet fix = BitSet.valueOf(RGB);
                connectedThread.setBitSet(fix);


                MyApplication.getApplication().setupConnectedThread(connectedThread);
                Intent intent = new Intent((Context) MainActivity2.this, MainActivity.class);
                MainActivity2.this.startActivity(intent);
            }
        }));
    }
}
