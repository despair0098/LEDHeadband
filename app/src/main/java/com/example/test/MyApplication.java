package com.example.test;

import android.app.Application;
import android.media.MediaRecorder;
import android.provider.MediaStore;

import java.util.BitSet;

public class MyApplication extends Application
{
    private static MyApplication sInstance;
    ConnectedThread connectedThread = null;
    Thread temp;



    public static MyApplication getApplication() {
        return sInstance;
    }

    public void onCreate() {
        super.onCreate();
        sInstance = this;
    }

    public void setupConnectedThread(ConnectedThread connectedThread)
    {
        this.connectedThread=connectedThread;
    }

    public ConnectedThread getCurrentConnectedThread()
    {
        return connectedThread;
    }

    public Thread getThread(){return temp;}
    public void setThread(Thread t){temp = t;}
    public boolean pressed = false;
    public Runnable updateRun;

}
