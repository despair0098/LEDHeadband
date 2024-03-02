package com.example.test;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.BitSet;

public class TextMenu extends AppCompatActivity {
    ImageView imgView;
    TextView colorValues;
    View colorViews;
    Button sendButton;
    Bitmap bitmap;
    ConnectedThread connectedThread;
    int r;
    int g;
    int b;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_menu);
        imgView = findViewById(R.id.colorPicker_text);
        colorValues = findViewById(R.id.displayValues_text);
        colorViews = findViewById(R.id.displayColors_text);
        sendButton = findViewById(R.id.sendButton_text);

        connectedThread = MyApplication.getApplication().getCurrentConnectedThread();

        if(connectedThread.getSocket().isConnected()) {
            Toast.makeText(TextMenu.this, "Bluetooth in Text successfully connected", Toast.LENGTH_LONG).show();
        }

        imgView.setDrawingCacheEnabled(true);
        imgView.buildDrawingCache(true);

        imgView.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event){
                if(event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE){
                    bitmap = imgView.getDrawingCache();
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
                temp.set(8, 31, true);
                byte[] RGB = temp.toByteArray();
                RGB[1] = (byte) r;
                RGB[2] = (byte) g;
                RGB[3] = (byte) b;
                BitSet fix = BitSet.valueOf(RGB);
                connectedThread.write(fix);
                connectedThread.setBitSet(fix);

                MyApplication.getApplication().setupConnectedThread(connectedThread);
                Intent intent = new Intent((Context) TextMenu.this, MainActivity.class);
                TextMenu.this.startActivity(intent);
            }
        }));
    }
}