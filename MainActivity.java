package com.example.test;

import static java.lang.Math.log10;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.test.R.id;
import com.example.test.R.layout;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.BitSet;
import java.util.UUID;

public final class MainActivity extends ComponentActivity {
    // Global variables we will use in the code
    private static final String TAG = "BTTest";
    UUID arduinoUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //We declare a default UUID to create the global variable

    // MAC-address of Adafruit Bluefruit EZ-Link module (you must edit this line)
    //private static String address = "98:D3:02:96:6A:C4";
    private static String address = "98:D3:02:96:C2:23";

    BluetoothAdapter bluetoothAdapter;
    BluetoothManager bluetoothManager;

    BluetoothDevice bluetoothDevice;

    private BluetoothSocket btSocket;

    private OutputStream outStream;
    TextView btReadings;
    TextView decibelReadings;
    Button musicButton;
    Button blinkButton;
    Button textButton;
    Button rgbButton;
    Button staticButton;
    Button offButton;
    Button colorButton;
    //Button decibelButton;
    BitSet answer = new BitSet(440);
    MediaRecorder mRecorder;
    Thread runner;
    private static double mEMA = 0.0;
    static final private double EMA_FILTER = 0.6;
    Runnable updater = new Runnable(){
        public void run(){
            updateTv();
        };
    };

    Handler mHandler = new Handler(Looper.getMainLooper());

    ConnectedThread testing;

    @RequiresApi(api = Build.VERSION_CODES.M)
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(layout.activity_main);

        musicButton = (Button) findViewById(R.id.musicButton);
        blinkButton = (Button) findViewById(R.id.blinkButton);
        rgbButton = (Button) findViewById(R.id.rgbButton);
        textButton = (Button) findViewById(R.id.textButton);
        staticButton = (Button) findViewById(R.id.staticButton);
        offButton = (Button) findViewById(R.id.offButton);
        colorButton = (Button) findViewById(id.colorButton);
        //decibelButton = (Button) findViewById(id.decibelButton);

        btReadings = findViewById(id.searchResult);
        decibelReadings = findViewById(id.decibelView);

        testing = MyApplication.getApplication().getCurrentConnectedThread();


        if(testing != null) {
            boolean[] t = testing.getButtonsPressed();
            if (t[0]) {//OFF
                offButton.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(9, 235, 43)));
            } else if (t[1]) {//STATIC
                staticButton.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(9, 235, 43)));
            } else if (t[2]) {//BLINK
                blinkButton.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(9, 235, 43)));
            } else if (t[3]) {//RGB
                rgbButton.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(9, 235, 43)));
            } else if (t[4]) {//TEXT
                textButton.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(9, 235, 43)));
            } else if (t[5]) {//MUSIC
                musicButton.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(9, 235, 43)));
            } else if (t[6]) {//COLOR
                colorButton.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(9, 235, 43)));
            } else {
                offButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                staticButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                blinkButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                rgbButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                textButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                musicButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                colorButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
            }
        }


        if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT > 31) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, 2);
                return;
            }
        }

        if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT > 31) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 10);
                return;
            }
        }

        if (runner == null)
        {
            runner = new Thread(){
                public void run()
                {
                    while (runner != null)
                    {
                        try
                        {
                            Thread.sleep(1000);
                            Log.i("Noise", "Tock");
                        } catch (InterruptedException e) { };
                        mHandler.post(updater);
                    }
                }
            };
            runner.start();
            Log.d("Noise", "start runner()");
        } else {
            runner = MyApplication.getApplication().getThread();
            updater = MyApplication.getApplication().updateRun;
        }

        bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);

        if(testing == null){
            new Thread(() -> {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
                    if (Build.VERSION.SDK_INT > 31) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 100);
                        return;
                    }
                }
                try {
                        btSocket = bluetoothDevice.createRfcommSocketToServiceRecord(arduinoUUID);
                        bluetoothAdapter.cancelDiscovery();
                        btSocket.connect();
                        if (btSocket.isConnected()) {
                            Log.d(TAG, "Calling ConnectedThread class");
                            testing = new ConnectedThread(btSocket, answer);
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Bluetooth successfully connected", Toast.LENGTH_LONG).show());
                        } else {
                            Toast.makeText(MainActivity.this, "Bluetooth successfully not connected", Toast.LENGTH_LONG).show();
                        }

                    outStream = btSocket.getOutputStream();
                    Log.d(TAG, "Connected to HC-06");
                    //runOnUiThread(() -> Toast.makeText(MainActivity.this, "Bluetooth successfully connected", Toast.LENGTH_LONG).show());

                } catch (IOException e) {
                    Log.d(TAG, "Turn on bluetooth and restart the app");
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Turn on bluetooth and restart the app", Toast.LENGTH_LONG).show());
                    throw new RuntimeException(e);
                }
            }).start();
        } else {
            btSocket = testing.getSocket();
            outStream = testing.getMmOutStream();
        }

        if (bluetoothAdapter == null) {
            btReadings.setText("Bluetooth is not available");
        } else {
            btReadings.setText("Bluetooth is available");
        }


        staticButton.setOnClickListener((View.OnClickListener) (new View.OnClickListener() {
            public final void onClick(View it) {
                MyApplication.getApplication().setupConnectedThread(testing);
                buttonPress("STATIC");
                sendCommand("STATIC");
                Intent intent = new Intent((Context) MainActivity.this, MainActivity2.class);
                MainActivity.this.startActivity(intent);
            }
        }));

        blinkButton.setOnClickListener((View.OnClickListener) (new View.OnClickListener() {
            public final void onClick(View it) {
                MyApplication.getApplication().setupConnectedThread(testing);
                buttonPress("BLINK");
                sendCommand("BLINK");
                Intent intent = new Intent((Context) MainActivity.this, MainActivity2.class);
                MainActivity.this.startActivity(intent);
            }
        }));

        textButton.setOnClickListener((View.OnClickListener) (new View.OnClickListener() {
            public final void onClick(View it) {
                MyApplication.getApplication().setupConnectedThread(testing);
                buttonPress("TEXT");
                sendCommand("TEXT");
                Intent intent = new Intent((Context) MainActivity.this, TextMenu.class);
                MainActivity.this.startActivity(intent);
            }
        }));
        rgbButton.setOnClickListener((View.OnClickListener) (new View.OnClickListener() {
            public final void onClick(View it) {
                buttonPress("RGB");
                sendCommand("RGB");
            }
        }));

        musicButton.setOnClickListener((View.OnClickListener) (new View.OnClickListener() {
            public final void onClick(View it) {
                MyApplication.getApplication().setupConnectedThread(testing);
                buttonPress("MUSIC");
                sendCommand("MUSIC");
                Intent intent = new Intent((Context) MainActivity.this, MainActivity2.class);
                MainActivity.this.startActivity(intent);
            }
        }));
        offButton.setOnClickListener((View.OnClickListener) (new View.OnClickListener() {
            public final void onClick(View it) {
                buttonPress("OFF");
                sendCommand("OFF");
            }
        }));

        colorButton.setOnClickListener((View.OnClickListener) (new View.OnClickListener() {
            public final void onClick(View it) {
                buttonPress("COLOR");
                sendCommand("COLOR");
            }
        }));

        //decibelButton.setOnClickListener((View.OnClickListener) (new View.OnClickListener() {
        //    public final void onClick(View it) {
                //MyApplication.getApplication().setupConnectedThread(testing);
                //Intent intent = new Intent((Context) MainActivity.this, DecibelMenu.class);
                //MainActivity.this.startActivity(intent);
        //    }
        //}));
    }

    private void sendCommand(String str) {
        if (outStream == null) {
            Log.d(TAG, "Output stream error");
            return;
        }
        answer = testing.getBitSet();
        answer.set(0, 7, false);
        answer.set(40, 440, true);
        switch(str){
            case "RGB"://3
                answer.set(0, true);
                answer.set(1, true);
                answer.set(2, false);

                break;
            case "STATIC"://1
                testing.setStaticMode(true);
                break;
            case "MUSIC"://5
                testing.setMusicMode(true);
                break;
            case "BLINK"://2
                testing.setBlinkMode(true);
                break;
            case "TEXT"://4
                testing.setTextMode(true);
                break;
            case "OFF":// 0
                answer.set(0, 2, false);
                break;
            case "COLOR"://6
                answer.set(0, false);
                answer.set(1, true);
                answer.set(2, true);
                break;
        }
        if(str == "RGB" || str == "COLOR") {
            try {
                testing.setBitSet(answer);
                byte[] command = answer.toByteArray();
                command[1] = (byte)255;
                command[2] = (byte)255;
                command[3] = (byte)255;
                for (int i = 4; i < 55; i++) {
                    if (command[i] == -1) {
                        command[i] = 0;
                    }
                }
                String c = answer.toString();
                outStream.write(command);
                Log.d(TAG, c);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if( str == "OFF" ){
            try {
                testing.setBitSet(answer);
                byte[] command = answer.toByteArray();
                for (int i = 1; i < 55; i++) {
                    if (command[i] == -1) {
                        command[i] = 0;
                    }
                }
                String c = answer.toString();
                outStream.write(command);
                Log.d(TAG, c);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void buttonPress(String str) {
        int index = 0;
        switch (str) {
            case "OFF":// 0
                index = 0;
                offButton.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(9, 235, 43)));
                staticButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                blinkButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                rgbButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                textButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                musicButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                colorButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                break;
            case "STATIC"://1
                index = 1;
                staticButton.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(9, 235, 43)));
                offButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                blinkButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                rgbButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                textButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                musicButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                colorButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                break;
            case "BLINK"://2
                index = 2;
                blinkButton.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(9, 235, 43)));
                offButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                staticButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                rgbButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                textButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                musicButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                colorButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                break;
            case "RGB"://3
                index = 3;
                rgbButton.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(9, 235, 43)));
                blinkButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                offButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                staticButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                textButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                musicButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                colorButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                break;
            case "TEXT"://4
                index = 4;
                textButton.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(9, 235, 43)));
                rgbButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                blinkButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                offButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                staticButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                musicButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                colorButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                break;
            case "MUSIC"://5
                index = 5;
                musicButton.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(9, 235, 43)));
                textButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                rgbButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                blinkButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                offButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                staticButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                colorButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                break;
            case "COLOR"://6
                index = 6;
                colorButton.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(9, 235, 43)));
                musicButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                textButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                rgbButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                blinkButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                offButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                staticButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                break;
        }
        boolean[] temp = testing.buttonsPressed;
        for(int i = 0; i < temp.length; i++){
            if(i == index){
                temp[i] = true;
            } else {
                temp[i] = false;
            }
        }
        testing.buttonsPressed = temp;
    }
    public void onResume()
    {
        super.onResume();
        startRecorder();
    }

    public void onPause()
    {
        super.onPause();
        stopRecorder();
    }

    public void startRecorder(){
        if (mRecorder == null)
        {
            if(ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
                if (Build.VERSION.SDK_INT > 31) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, 3);
                    return;
                }
            }

            mRecorder = new MediaRecorder(MainActivity.this);
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mRecorder.setOutputFile(MainActivity.this.getFilesDir().getAbsolutePath() + "/test.3gp");
            try
            {
                mRecorder.prepare();
            }catch (java.io.IOException ioe) {
                android.util.Log.e("[Monkey]", "IOException: " + android.util.Log.getStackTraceString(ioe));

            }catch (java.lang.SecurityException e) {
                android.util.Log.e("[Monkey]", "SecurityException: " + android.util.Log.getStackTraceString(e));
            }
            try
            {
                mRecorder.start();
            }catch (java.lang.SecurityException e) {
                android.util.Log.e("[Monkey]", "SecurityException: " + android.util.Log.getStackTraceString(e));
            }
        }

    }
    public void stopRecorder() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
    }

    public void updateTv(){
        if(mRecorder != null) {
            double temp = soundDb(1.1);
            decibelReadings.setText("Decibel: " + Double.toString(temp) + " dB");
            //Log.i("Noise", "Giving notif rn");
            if(temp >= 88.0){
                //makeNotification(soundDb(Math.pow(10, -12)));
                Log.i("Noise", "Giving notif rn");
                makeNotification(temp);
                //Toast.makeText(DecibelMenu.this, "You are receiving too high decibel! It's around " + Double.toString(temp) + " dB now!", Toast.LENGTH_LONG).show();
            }
        }
    }
    public double soundDb(double ampl){
        return  20 * Math.log10(getAmplitude() / ampl);
    }
    public double getAmplitude() {
        if (mRecorder != null) {
            int temp = mRecorder.getMaxAmplitude();
            //Log.i("Noise", "Recorder is not null now");
            //Log.i("Noise", "Max Amplitude double: " + Double.toString(Double.valueOf(temp)));
            return Double.valueOf(temp);
        }else {
            //Log.i("Noise", "Recorder is null now");
            return 0;
        }
    }
    public double getAmplitudeEMA() {
        double amp = getAmplitude();
        Log.i("Noise", "Amp: " + Double.toString(amp));
        mEMA = EMA_FILTER * amp + (1.0 - EMA_FILTER) * mEMA;
        //return amp;
        return mEMA;
    }
    public void makeNotification(double decibel){
        String channelID = "CHANNEL_NOTIFICATION";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channelID);
        builder.setSmallIcon(R.drawable.decibel_notifications);
        builder.setContentTitle("HIGH DECIBEL WARNING");
        builder.setContentText("You are receiving too high decibel! It's around " + Double.toString(decibel) + " dB now!" );
        builder.setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel notificationChannel = notificationManager.getNotificationChannel(channelID);
            if(notificationChannel == null){
                int importance = NotificationManager.IMPORTANCE_HIGH;
                notificationChannel = new NotificationChannel(channelID, "DECIBEL WARNING", importance);
                notificationChannel.setLightColor(Color.GREEN);
                notificationChannel.enableVibration(true);
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }
        notificationManager.notify(0, builder.build());

    }
    // used when the app is closed
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (btSocket != null) {
            try {
                btSocket.close();
                Log.d(TAG, "Connection closed");
            } catch (IOException e) {
                Log.d(TAG, "Error while closing the connection");
            }
        }
    }

}
