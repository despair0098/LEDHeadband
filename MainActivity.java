package com.example.test;

import static java.lang.Math.log10;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
    private static String address = "98:D3:02:96:6A:C4";
    //private static String address = "98:D3:02:96:C2:23";
    //98D3:02:96C223
    //98:D3:02:96:C2:23

    BluetoothAdapter bluetoothAdapter;
    BluetoothManager bluetoothManager;

    BluetoothDevice bluetoothDevice;

    private BluetoothSocket btSocket;

    private OutputStream outStream;

    //float volume = 10000;
    //private SoundDiscView soundDiscView;
    //private MyMediaRecorder mRecorder;
    //private static final int msgWhat = 0x1001;
    //private static final int refreshTime = 100;
    //SoundDiscView discView;
    /*private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (this.hasMessages(msgWhat)) {
                return;
            }
            volume = mRecorder.getMaxAmplitude();  //获取声压值
            if(volume > 0 && volume < 1000000) {
                World.setDbCount(20 * (float)(Math.log10(volume)));  //将声压值转为分贝值
                float test = 20 * (float)(Math.log10(volume));
                Log.d("Testing", String.valueOf(test));
                soundDiscView.refresh();
            }
            handler.sendEmptyMessageDelayed(msgWhat, refreshTime);
        }
    };

     */


    //MediaRecorder mRecorder;
    //Thread runner;
    //private static double mEMA = 0.0;
    //static final private double EMA_FILTER = 0.6;

    //Runnable updater;
    //Handler mHandler;


    //ConnectedThread connectedThread;
    //Instances of the Android UI elements that will will use during the execution of the APP
    TextView btReadings;
    TextView decibelReadings;
    Button musicButton;
    Button blinkButton;
    Button textButton;
    Button rgbButton;
    Button staticButton;
    Button offButton;

    BitSet answer = new BitSet(440);

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

        btReadings = findViewById(id.searchResult);

        //discView = findViewById(id.soundDiscView);
        //mRecorder = new MyMediaRecorder();

        //answer = MyApplication.getApplication().getBitSet();

        testing = MyApplication.getApplication().getCurrentConnectedThread();


        if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT > 31) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, 2);
                return;
            }
        }
        bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);

        /*
        if(testing != null) {
            btSocket = testing.getSocket();
            //btSocket.connect();
            if (btSocket.isConnected()) {
                outStream = testing.getMmOutStream();
            }
        }
         */

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
                //testing = new ConnectedThread(btSocket, answer);
                MyApplication.getApplication().setupConnectedThread(testing);
                sendCommand("STATIC");
                Intent intent = new Intent((Context) MainActivity.this, MainActivity2.class);
                MainActivity.this.startActivity(intent);
            }
        }));

        blinkButton.setOnClickListener((View.OnClickListener) (new View.OnClickListener() {
            public final void onClick(View it) {
                //testing = new ConnectedThread(btSocket, answer);
                MyApplication.getApplication().setupConnectedThread(testing);
                sendCommand("BLINK");
                Intent intent = new Intent((Context) MainActivity.this, MainActivity2.class);
                MainActivity.this.startActivity(intent);
            }
        }));

        textButton.setOnClickListener((View.OnClickListener) (new View.OnClickListener() {
            public final void onClick(View it) {
                //testing = new ConnectedThread(btSocket, answer);
                MyApplication.getApplication().setupConnectedThread(testing);
                sendCommand("TEXT");
                Intent intent = new Intent((Context) MainActivity.this, TextMenu.class);
                MainActivity.this.startActivity(intent);
            }
        }));
        rgbButton.setOnClickListener((View.OnClickListener) (new View.OnClickListener() {
            public final void onClick(View it) {
                sendCommand("RGB");
            }
        }));

        musicButton.setOnClickListener((View.OnClickListener) (new View.OnClickListener() {
            public final void onClick(View it) {
                MyApplication.getApplication().setupConnectedThread(testing);
                sendCommand("MUSIC");
                Intent intent = new Intent((Context) MainActivity.this, MainActivity2.class);
                MainActivity.this.startActivity(intent);
            }
        }));
        offButton.setOnClickListener((View.OnClickListener) (new View.OnClickListener() {
            public final void onClick(View it) {
                sendCommand("OFF");
            }
        }));

        /*
         updater = new Runnable(){

            public void run(){
                updateTv();
            };
        };

        mHandler = new Handler();

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
        }

         */

    }


    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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
        }
        if(str == "RGB" || str == "OFF") {
            try {
                testing.setBitSet(answer);
                byte[] command = answer.toByteArray();
                for (int i = 1; i < 55; i++) {
                    if (command[i] == -1) {
                        command[i] = 0;
                    }
                }
                //testing.setBitSet(BitSet.valueOf(command));
                String c = answer.toString();
                outStream.write(command);
                Log.d(TAG, c);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    /*
    private void startListenAudio() {
        handler.sendEmptyMessageDelayed(msgWhat, refreshTime);
    }

    /**
     * 开始记录
     * @param fFile
     */
    /*
    public void startRecord(File fFile, Context c){
        if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT > 31) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, 3);
                return;
            }
        }

        if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT > 31) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 4);
                return;
            }
        }
        if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT > 31) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 5);
                return;
            }
        }
        try{
            mRecorder.setMyRecAudioFile(fFile);
            if (mRecorder.startRecorder(c)) {
                startListenAudio();
            }else{
                Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show();
            }
        }catch(Exception e){
            Toast.makeText(this, "Recorder is occupied or recording privileges are disabled", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT > 31) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, 3);
                return;
            }
        }

        if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT > 31) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 4);
                return;
            }
        }
        if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT > 31) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 5);
                return;
            }
        }
        soundDiscView = (SoundDiscView) findViewById(R.id.soundDiscView);
        File file = FileUtil.createFile("temp.amr");
        if (file != null) {
            Log.v("file", "file =" + file.getAbsolutePath());
            startRecord(file, MainActivity.this);
        } else {
            Toast.makeText(getApplicationContext(), "Failed to create a file", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 停止记录
     */
    /*
    @Override
    protected void onPause() {
        super.onPause();
        mRecorder.delete(); //停止记录并删除录音文件
        handler.removeMessages(msgWhat);
    }
    */

    /*
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
            mRecorder = new MediaRecorder(MainActivity.this);
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mRecorder.setOutputFile("/dev/null");
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

            //mEMA = 0.0;
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
        btReadings.setText(Double.toString((getAmplitudeEMA())) + " dB");
    }
    public double soundDb(double ampl){
        return  20 * Math.log10(getAmplitudeEMA() / ampl);
    }
    public double getAmplitude() {
        if (mRecorder != null)
            return  (mRecorder.getMaxAmplitude());
        else
            return 0;

    }
    public double getAmplitudeEMA() {
        double amp = getAmplitude();
        mEMA = EMA_FILTER * amp + (1.0 - EMA_FILTER) * mEMA;
        return mEMA;
    }

     */
    // used when the app is closed
    @Override
    protected void onDestroy() {
        //handler.removeMessages(msgWhat);
        //mRecorder.delete();
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
