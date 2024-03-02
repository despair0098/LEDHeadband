package com.example.test;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
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

    BluetoothAdapter bluetoothAdapter;
    BluetoothManager bluetoothManager;

    BluetoothDevice bluetoothDevice;

    private BluetoothSocket btSocket;

    private OutputStream outStream;


    //ConnectedThread connectedThread;
    //Instances of the Android UI elements that will will use during the execution of the APP
    TextView btReadings;
    TextView btDevices;
    Button musicButton;
    Button blinkButton;
    Button textButton;
    Button rgbButton;
    Button staticButton;
    Button offButton;

    BitSet answer = new BitSet(40);

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

        //answer = MyApplication.getApplication().getBitSet();

        testing = MyApplication.getApplication().getCurrentConnectedThread();


        if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT > 31) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, 2);
                return;
            }
        }


        /*
        if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_DENIED){
            if(Build.VERSION.SDK_INT < 31){
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 100);
                return;
            }
        }
        */
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
                    if (Build.VERSION.SDK_INT < 31) {
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
                //btReadings.setText("RGB PRESSED");
            }
        }));

        musicButton.setOnClickListener((View.OnClickListener) (new View.OnClickListener() {
            public final void onClick(View it) {
                sendCommand("MUSIC");
            }
        }));
        offButton.setOnClickListener((View.OnClickListener) (new View.OnClickListener() {
            public final void onClick(View it) {
                sendCommand("OFF");
            }
        }));
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
        switch(str){
            case "RGB"://3
                answer.set(1, 2, true);
                answer.set(0, false);
                break;
            case "STATIC"://1
                answer.set(1, 2, false);
                answer.set(0, true);
                break;
            case "MUSIC"://5
                answer.set(0, true);
                answer.set(1, false);
                answer.set(2, true);
                break;
            case "BLINK"://2
                answer.set(0, false);
                answer.set(1, true);
                answer.set(2, false);
                break;
            case "TEXT"://4
                answer.set(2, true);
                answer.set(0, 1, false);
                break;
            case "OFF":// 0
                answer.set(0, 2, false);
                break;
        }
        try {
            //testing.setBitSet(answer);
            byte[] command = answer.toByteArray();
            String c = answer.toString();
            outStream.write(command);
            Log.d(TAG, c);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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
