package com.example.test;

import android.bluetooth.BluetoothSocket;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.BitSet;

//Class that given an open BT Socket will
//Open, manage and close the data Stream from the Arduino BT device
public class ConnectedThread extends Thread{
    private static final String TAG = "Thread";
    private final BluetoothSocket mmSocket;
    private final OutputStream mmOutStream;
    BitSet answer;
    private String valueRead;
    private boolean staticMode = false;
    private boolean blinkMode = false;
    private boolean textMode = false;
    private boolean musicMode = false;
    boolean[] buttonsPressed = {false, false, false, false, false, false, false};

    public OutputStream getMmOutStream() {
        return mmOutStream;
    }

    public BluetoothSocket getSocket() {
        return mmSocket;
    }

    public BitSet getBitSet(){return answer;}

    public void setBitSet(BitSet b){answer = b;}
    public void setTextMode(boolean b){textMode = b;}
    public void setBlinkMode(boolean b){blinkMode = b;}
    public void setStaticMode(boolean b){staticMode = b;}
    public void setMusicMode(boolean b){musicMode = b;}

    public boolean getTextMode(){return textMode;}
    public boolean getBlinkMode(){return blinkMode;}
    public boolean getStaticMode(){return staticMode;}
    public boolean getMusicMode(){return musicMode;}

    public boolean[] getButtonsPressed() {
        return buttonsPressed;
    }

    public void setButtonsPressed(boolean[] b){buttonsPressed = b;}


    public ConnectedThread(BluetoothSocket socket, BitSet b) {
        mmSocket = socket;
        OutputStream tmpOut = null;
        answer = b;

        // Get the input and output streams; using temp objects because
        // member streams are final.
        try {
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating output stream", e);
        }
        //Input and Output streams members of the class
        //We wont use the Output stream of this project
        mmOutStream = tmpOut;
    }
}
