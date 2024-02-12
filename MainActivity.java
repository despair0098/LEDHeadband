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
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.example.test.R.id;
import com.example.test.R.layout;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;
import kotlin.Metadata;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

public final class MainActivity extends ComponentActivity {
    // Global variables we will use in the code
    /*
    private static String[] PERMISSIONS_STORAGE = {
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT,
    };
    private static String[] PERMISSIONS_LOCATION = {
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED
    };

     */
    private static final String TAG = "BTTest";
    //private static final int REQUEST_ENABLE_BT = 1;
    //We will use a Handler to get the BT Connection statys
    //public static Handler handler;
    //private final static int ERROR_READ = 0; // used in bluetooth handler to identify message update
    //BluetoothDevice arduinoBTModule = null;
    UUID arduinoUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //We declare a default UUID to create the global variable

    // MAC-address of Adafruit Bluefruit EZ-Link module (you must edit this line)
    private static String address = "98:D3:02:96:6A:C4";

    BluetoothAdapter bluetoothAdapter;
    BluetoothManager bluetoothManager;

    BluetoothDevice bluetoothDevice;

    private BluetoothSocket btSocket;

    private OutputStream outStream;

    private InputStream inStream;

    //ConnectedThread connectedThread;
    //Instances of the Android UI elements that will will use during the execution of the APP
    TextView btReadings;
    TextView btDevices;
    Button connectToDevice;
    Button searchDevices;
    Button rgbButton;

    /*
    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "PERMISSION GRANTED");
                } else {
                    Log.d(TAG, "PERMISSION DENIED");
                }
            });
     */

    @RequiresApi(api = Build.VERSION_CODES.M)
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(layout.activity_main);
        Button button = (Button) this.findViewById(id.button5);
        button.setOnClickListener((View.OnClickListener) (new View.OnClickListener() {
            public final void onClick(View it) {
                //MyApplication.getApplication().setupConnectedThread(connectedThread);
                Intent intent = new Intent((Context) MainActivity.this, MainActivity2.class);
                MainActivity.this.startActivity(intent);
            }
        }));

        //Instances of BT Manager and BT Adapter needed to work with BT in Android.

        //bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //Instances of the Android UI elements that will will use during the execution of the APP
        btReadings = findViewById(id.searchResult);
        btDevices = findViewById(id.connectResult);
        connectToDevice = (Button) findViewById(id.connectButton);
        searchDevices = (Button) findViewById(id.searchButton);
        rgbButton = (Button) findViewById(id.button3);


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

        new Thread(() -> {
            if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED){
                if(Build.VERSION.SDK_INT < 31){
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 100);
                    return;
                }
            }
            try {
                btSocket = bluetoothDevice.createRfcommSocketToServiceRecord(arduinoUUID);
                bluetoothAdapter.cancelDiscovery();
                btSocket.connect();
                outStream = btSocket.getOutputStream();
                Log.d(TAG, "Connected to HC-06");
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Bluetooth successfully connected", Toast.LENGTH_LONG).show());

            } catch (IOException e) {
                Log.d(TAG, "Turn on bluetooth and restart the app");
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Turn on bluetooth and restart the app", Toast.LENGTH_LONG).show());
                throw new RuntimeException(e);
            }
        }).start();



        //checkBTState();


        /*
        //Using a handler to update the interface in case of an error connecting to the BT device
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {

                    case ERROR_READ:
                        String arduinoMsg = msg.obj.toString(); // Read message from Arduino
                        btReadings.setText(arduinoMsg);
                        break;
                }
            }
        };

        final Observable<ConnectedClass> connectedToBTObservable = Observable.create(emitter -> {
            Log.d(TAG, "Calling connectThread class");
            //Call the constructor of the ConnectThread class
            //Passing the Arguments: an Object that represents the BT device,
            // the UUID and then the handler to update the UI
            ConnectThread connectThread = new ConnectThread(arduinoBTModule, arduinoUUID, handler);
            connectThread.run();
            //Check if Socket connected
            if (connectThread.getMmSocket().isConnected()) {
                Log.d(TAG, "Calling ConnectedThread class");
                //The pass the Open socket as arguments to call the constructor of ConnectedThread
                connectedThread = new ConnectedThread(connectThread.getMmSocket());
                if (connectedThread.getMmInStream() != null && connectedThread != null) {
                    ConnectedClass connected = new ConnectedClass();
                    connected.setConnected(true);
                    emitter.onNext(connected);
                }
                //We just want to stream 1 value, so we close the BT stream
                //connectedThread.cancel();
            }
            //Then we close the socket connection
            //connectThread.cancel();
            //We could Override the onComplete function
            emitter.onComplete();
        });

        connectToDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (arduinoBTModule != null) {
                    //We subscribe to the observable until the onComplete() is called
                    //We also define control the thread management with
                    // subscribeOn:  the thread in which you want to execute the action
                    // observeOn: the thread in which you want to get the response
                    connectedToBTObservable.
                            observeOn(AndroidSchedulers.mainThread()).
                            subscribeOn(Schedulers.io()).
                            subscribe(connectedToBTDevice -> {
                                btReadings.setText("It's connected!");
                            });
                }
            }
        });

        searchDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bluetoothAdapter == null) {
                    Log.d(TAG, "Device doesn't support Bluetooth");
                } else {
                    Log.d(TAG, "Device support Bluetooth");
                    //Check BT enabled. If disabled, we ask the user to enable BT

                    if (!bluetoothAdapter.isEnabled()) {
                        Log.d(TAG, "Bluetooth is disabled");
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);


                        if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            Log.d(TAG, "We don't BT Permissions");
                            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                            Log.d(TAG, "Bluetooth is enabled now");
                        } else {
                            Log.d(TAG, "We have BT Permissions");
                            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                            Log.d(TAG, "Bluetooth is enabled now");
                        }
                    } else {
                        Log.d(TAG, "Bluetooth is enabled");
                    }

                  /*
                    int permission1 = ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    int permission2 = ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_SCAN);
                    if (permission1 != PackageManager.PERMISSION_GRANTED) {
                        // We don't have permission so prompt the user
                        ActivityCompat.requestPermissions(
                                MainActivity.this,
                                PERMISSIONS_STORAGE,
                                1
                        );
                    } else if (permission2 != PackageManager.PERMISSION_GRANTED){
                        ActivityCompat.requestPermissions(
                                MainActivity.this,
                                PERMISSIONS_LOCATION,
                                1
                        );
                    }

                    String result = "";
                    Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

                    if(pairedDevices.size() > 0){
                        for(BluetoothDevice device: pairedDevices){
                            String name = device.getName();
                            String hardwareAddress = device.getAddress();
                            Log.d(TAG, "deviceName:" + name);
                            Log.d(TAG, "deviceHardwareAddress:" + hardwareAddress);
                            result = result + name + " || " + hardwareAddress+ "\n";
                            if(name.equals("Testing")){
                                Log.d(TAG, "HC-05 found");
                                arduinoUUID = device.getUuids()[0].getUuid();
                                arduinoBTModule = device;
                                connectToDevice.setEnabled(true);
                            }
                            btDevices.setText(result);
                        }
                    }
                }
                Log.d(TAG, "Button Pressed");
            }
        });
        */
        /*
        ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == MainActivity.REQUEST_ENABLE_BT) {
                            // There are no request codes
                            Intent data = result.getData();
                            btDevices.setText("Bluetooth is on");
                        } else {
                            btDevices.setText("Can not turn on Bluetooth");
                        }
                    }
                }
        );
         */

        if (bluetoothAdapter == null) {
            btReadings.setText("Bluetooth is not available");
        } else {
            btReadings.setText("Bluetooth is available");
        }

        rgbButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendCommand("ON");
                showToast("Turn LED ON");
            }
        });

        searchDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendCommand("OFF");
                showToast("Turn LED OFF");
            }
        });
    }
    //this is for enabling the BT`
    /*
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        switch(resultCode){
            case REQUEST_ENABLE_BT:
                if(resultCode == RESULT_OK){
                    btDevices.setText("Bluetooth is on");
                } else {
                    btDevices.setText("Can not turn on Bluetooth");
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

     */


    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void sendCommand(String str) {
        if (outStream == null) {
            Log.d(TAG, "Output stream error");
            return;
        }
        try {
            String command = str;
            command = command + '\n';
            outStream.write(command.getBytes());
            Log.d(TAG, command);
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

    /*
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        if (Build.VERSION.SDK_INT >= 31) {
            try {
                final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[]{UUID.class});
                return (BluetoothSocket) m.invoke(device, arduinoUUID);
            } catch (Exception e) {
                Log.e(TAG, "Could not create Insecure RFComm Connection", e);
            }
        }
        if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            //
            Log.d(TAG, "missing Manifest.permission.BLUETOOTH_CONNECT permission");
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 100);
        }
        return device.createRfcommSocketToServiceRecord(arduinoUUID);
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume(): Creating bluetooth socket ...");

        // Set up a pointer to the remote node using it's address.
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);

        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.

        try {
            btSocket = createBluetoothSocket(device);
            Log.d(TAG, "onResume(): Bluetooth socket created ...");
        } catch (IOException e1) {
            errorExit("Fatal Error", "onResume(): Create bluetooth socket FAILED: " + e1.getMessage() + ".");
        }

        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.d(TAG, "missing Manifest.permission.BLUETOOTH_CONNECT permission");
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 100);
        }
        bluetoothAdapter.cancelDiscovery();

        // Establish the connection.  This will block until it connects.
        Log.d(TAG, "Connecting to Bluetooth Device ...");
        try {
            btSocket.connect();
            Log.d(TAG, "Bluetooth Device Connected ...");
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                errorExit("Fatal Error", "onResume(): Unable to close socket when closing connection: " + e2.getMessage() + ".");
            }
        }

        // Create a data stream so we can talk to server.
        Log.d(TAG, "onResume(): Creating data output stream ...");

        try {
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
            errorExit("Fatal Error", "onResume(): Creating data output stream FAILED: " + e.getMessage() + ".");
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "Inside onPause()...");

        if (outStream != null) {
            try {
                outStream.flush();
            } catch (IOException e) {
                errorExit("Fatal Error", "onPause(): FAILED to flush data output stream: " + e.getMessage() + ".");
            }
        }

        try {
            btSocket.close();
        } catch (IOException e2) {
            errorExit("Fatal Error", "onPause(): FAILED to close socket." + e2.getMessage() + ".");
        }
    }

    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if (bluetoothAdapter == null) {
            errorExit("Fatal Error", "Bluetooth is not supported");
        } else {
            if (bluetoothAdapter.isEnabled()) {
                Log.d(TAG, "Bluetooth is ON...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    Log.d(TAG, "missing Manifest.permission.BLUETOOTH_CONNECT permission");
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 100);
                    return;
                }
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    private void errorExit(String title, String message){
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }

    private void sendData(String message) {
        byte[] msgBuffer = message.getBytes();

        Log.d(TAG, "Sending data: " + message + "...");

        try {
            outStream.write(msgBuffer);
        } catch (IOException e) {
            String msg = "onResume(): Exception occurred during write: " + e.getMessage();
            if (address.equals("00:00:00:00:00:00"))
                msg = msg + ".\n\nChange your server address from 00:00:00:00:00:00 to the correct address on line 35 in the java code";
            msg = msg +  ".\n\nCheck that the SPP UUID: " + arduinoUUID.toString() + " exists on server.\n\n";

            errorExit("Fatal Error", msg);
        }
    }

     */

}
