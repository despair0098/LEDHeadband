package com.example.test;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;

public class DecibelMenu extends AppCompatActivity {
    MediaRecorder mRecorder;
    Thread runner;
    private static double mEMA = 0.0;
    static final private double EMA_FILTER = 0.6;
    Runnable updater = new Runnable(){
        public void run(){
            updateTv();
        };
    };
    ConnectedThread connectedThread;
    TextView decibelreading;
    Button backButton;
    Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decibel_menu);

        if(ActivityCompat.checkSelfPermission(DecibelMenu.this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT > 31) {
                ActivityCompat.requestPermissions(DecibelMenu.this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 10);
                return;
            }
        }

        decibelreading = findViewById(R.id.textView);
        backButton = findViewById(R.id.backButton);

        connectedThread = MyApplication.getApplication().getCurrentConnectedThread();

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
        if(connectedThread.getSocket().isConnected()) {
            Toast.makeText(DecibelMenu.this, "Bluetooth in decibel menu successfully connected", Toast.LENGTH_LONG).show();
        }

        backButton.setOnClickListener((View.OnClickListener) (new View.OnClickListener() {
            public final void onClick(View it) {
                MyApplication.getApplication().setupConnectedThread(connectedThread);
                Intent intent = new Intent((Context) DecibelMenu.this, MainActivity.class);
                DecibelMenu.this.startActivity(intent);
            }
        }));
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
            if(ActivityCompat.checkSelfPermission(DecibelMenu.this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
                if (Build.VERSION.SDK_INT > 31) {
                    ActivityCompat.requestPermissions(DecibelMenu.this, new String[]{Manifest.permission.RECORD_AUDIO}, 3);
                    return;
                }
            }

            mRecorder = new MediaRecorder(DecibelMenu.this);
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mRecorder.setOutputFile(DecibelMenu.this.getFilesDir().getAbsolutePath() + "/test.3gp");
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
            decibelreading.setText("Decibel: " + Double.toString(temp) + " dB");
            //Log.i("Noise", "Giving notif rn");
            if(temp >= 84.0){
                //makeNotification(soundDb(Math.pow(10, -12)));
                Log.i("Noise", "Giving notif rn");
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
}