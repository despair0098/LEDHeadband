package com.example.test;

import android.app.Application;

import java.util.BitSet;

public class MyApplication extends Application
{
    private static MyApplication sInstance;
    ConnectedThread connectedThread = null;

    BitSet ans;


    public static MyApplication getApplication() {
        return sInstance;
    }

    //public  void setupConnectedThread() {
    //}

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

    public void setBitSet(BitSet b){
        this.ans = b;
    }

    public BitSet getBitSet(){return ans;}

}
