package com.example.test;

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

import androidx.appcompat.app.AppCompatActivity;
import com.example.test.R.layout;

import org.jetbrains.annotations.Nullable;

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
                    int pixels = bitmap.getPixel((int)event.getX(), (int)event.getY());

                    r = Color.red(pixels);
                    g = Color.green(pixels);
                    b = Color.blue(pixels);

                    String hex = "#" + Integer.toHexString(pixels);
                    colorViews.setBackgroundColor(Color.rgb(r,g,b));
                    colorValues.setText("RGB: " + r + ", " + g + ", " + b + "\nHex: " + hex);

                    //connectedThread.write(hex);
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
                Intent intent = new Intent((Context) MainActivity2.this, MainActivity.class);
                MainActivity2.this.startActivity(intent);
            }
        }));
    }
}
