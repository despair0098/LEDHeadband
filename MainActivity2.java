package com.example.test;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.example.test.R.layout;
import kotlin.Metadata;
import org.jetbrains.annotations.Nullable;

public final class MainActivity2 extends AppCompatActivity {
    ImageView imgView;
    TextView colorValues;
    View colorViews;
    Bitmap bitmap;
    //ConnectedThread connectedThread;


    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(layout.content_main);
        imgView = findViewById(R.id.colorPicker);
        colorValues = findViewById(R.id.displayValues);
        colorViews = findViewById(R.id.displayColors);

        imgView.setDrawingCacheEnabled(true);
        imgView.buildDrawingCache(true);

        imgView.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event){
                if(event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE){
                    bitmap = imgView.getDrawingCache();
                    int pixels = bitmap.getPixel((int)event.getX(), (int)event.getY());

                    int r = Color.red(pixels);
                    int g = Color.green(pixels);
                    int b = Color.blue(pixels);

                    String hex = "#" + Integer.toHexString(pixels);
                    colorViews.setBackgroundColor(Color.rgb(r,g,b));
                    colorValues.setText("RGB: " + r + ", " + g + ", " + b + "\nHex: " + hex);
                }
                return true;
            }
        });

        //connectedThread = MyApplication.getApplication().getCurrentConnectedThread();
    }
}
