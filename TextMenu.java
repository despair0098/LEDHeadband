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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.BitSet;

public class TextMenu extends AppCompatActivity {
    ImageView imgView;
    TextView colorValues;
    View colorViews;
    Button sendButton;
    EditText inputText;
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
        inputText = findViewById(R.id.editText);

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
                String input = inputText.getText().toString();
                int inputSize = input.length();
                String temp2 = "";

                for(int i = 0; i < inputSize; i++){
                    char c = input.charAt(i);
                    int d = (int) c;
                    //Toast.makeText(TextMenu.this, input + "is " + Integer.toString(d), Toast.LENGTH_LONG).show();
                    if(d < 32 || d > 127){
                        c = ' ';
                    }
                    temp2 += c;
                }

                for(int j = temp2.length(); j < 50; j++){
                    temp2 += " ";
                }

                BitSet temp = connectedThread.getBitSet();
                if(connectedThread.getTextMode()){
                    temp.set(2, true);
                    temp.set(0, 1, false);
                    connectedThread.setTextMode(false);
                }
                temp.set(8, 440, true);
                byte[] RGB = temp.toByteArray();
                RGB[1] = (byte) r;
                RGB[2] = (byte) g;
                RGB[3] = (byte) b;
                RGB[4] = (byte) inputSize;

                BitSet fix = BitSet.valueOf(RGB);
                //connectedThread.write(fix);
                //connectedThread.setBitSet(fix);

                //Toast.makeText(TextMenu.this, input + "is " + inputDec, Toast.LENGTH_LONG).show();

                Toast.makeText(TextMenu.this, input + " is " + temp2, Toast.LENGTH_LONG).show();

                byte[] b = temp2.getBytes();
                byte[] b1 = fix.toByteArray();
                BitSet message = BitSet.valueOf(b);
                message.set((inputSize*8), 400, true);
                byte[] t = message.toByteArray();
                for(int k = 5; k < b1.length; k++){
                    b1[k] = t[k-5];
                }

                connectedThread.setBitSet(BitSet.valueOf(t));

                for(int l = 5; l < b1.length; l++){
                    if(b1[l] == -1){
                        b1[l] = 0;
                    }
                }
                try {
                    connectedThread.getMmOutStream().write(b1);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                MyApplication.getApplication().setupConnectedThread(connectedThread);
                Intent intent = new Intent((Context) TextMenu.this, MainActivity.class);
                TextMenu.this.startActivity(intent);
            }
        }));
    }
}