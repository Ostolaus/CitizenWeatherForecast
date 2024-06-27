package com.example.citizenweatherforecast;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;


import java.util.Calendar;

import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity{
    public static PressureSensorManager pressureSensorManager;
    private SensorManager sensorManager;
    private Context appContext;
    public CountDownTimer timer;
    public static MainActivity instance;
    public static DataHandler dataHandler;
    public static NNHandler nnHandler;
    public static Context context;
    public static SharedPreferences sharedPreferences;
    public static SharedPreferences.Editor sharedEditor;
    private float latestMeasurement;
    private boolean service_active;

    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        pressureSensorManager = new PressureSensorManager(sensorManager);
        instance=this;
        dataHandler = new DataHandler();
        nnHandler = new NNHandler(this);
        context = getApplicationContext();
        sharedPreferences = context.getSharedPreferences("sharedVars", MODE_PRIVATE);
        sharedEditor = sharedPreferences.edit();
        initMeasurement();
        initVariables();
        pressureSensorManager.startListening();
    }

    public void initVariables(){
        service_active = sharedPreferences.getBoolean("service_status", false);
    };

    public void stopService(View view) {
        service_active = false;
        sharedEditor.putBoolean("service_status", false);
        sharedEditor.apply();

        if(timer != null) {
            timer.cancel();
            timer = null;
        }

        TextView timerTextView = (TextView) findViewById(R.id.time_tv);
        timerTextView.setText("");

        TextView service_status_tv = (TextView) findViewById(R.id.serviceStatusTextView);
        service_status_tv.setText(R.string.service_is_not_running);
        pressureSensorManager.stopListening();

    }

    @SuppressLint("DefaultLocale")
    public void predictNow(View view){
        float[] prediction = nnHandler.runInferenceSingleOutput(dataHandler.generateNNData());

        TextView temp_tv = (TextView) findViewById(R.id.pred_temp_tv);
        TextView hum_tv = (TextView) findViewById(R.id.pred_hum_tv3);
        TextView press_tv = (TextView) findViewById(R.id.pred_press_tv);

        temp_tv.setText(String.format("%.2f", prediction[0]));
        hum_tv.setText(String.format("%.2f", prediction[1]));
        press_tv.setText(String.format("%.2f", prediction[2]));
    }

    public void predictAndUpdate(){
        float[] prediction = nnHandler.runInferenceSingleOutput(dataHandler.generateNNData());

        TextView temp_tv = (TextView) findViewById(R.id.pred_temp_tv);
        TextView hum_tv = (TextView) findViewById(R.id.pred_hum_tv3);
        TextView press_tv = (TextView) findViewById(R.id.pred_press_tv);

        temp_tv.setText(String.format("%.2f", prediction[0]));
        hum_tv.setText(String.format("%.2f", prediction[1]));
        press_tv.setText(String.format("%.2f", prediction[2]));
    }


    public void measureNow(View view){
        pressureSensorManager.startListening();
        float currentPressure = pressureSensorManager.getCurrentPressure();
        setLatestMeasurement(currentPressure);
    }

    @SuppressLint("DefaultLocale")
    public static void updatePressureReading(float val){
        TextView tv = (TextView) MainActivity.instance.findViewById(R.id.latest_measurement_tv);
        tv.setText(String.format("%.2f mBar", val));
    }

    public void startService(View view) {
        sharedEditor.putBoolean("service_status", true);
        sharedEditor.apply();

        Calendar calendar = Calendar.getInstance();
        int secondsUntilNextHour = (60 - calendar.get(Calendar.MINUTE)) *60 ;

        if (timer == null) {
            startTimer(secondsUntilNextHour);
        }


        Intent serviceIntent = new Intent(this, WorkService.class);
        startService(serviceIntent);

        TextView service_status_tv = (TextView) findViewById(R.id.serviceStatusTextView);
        service_status_tv.setText(R.string.service_is_running);
    }

    @SuppressLint("SetTextI18n")
    public void setLatestMeasurement(float measurement){
        latestMeasurement = measurement;
        TextView latest_measurement_tv = (TextView) findViewById(R.id.latest_measurement_tv);
        latest_measurement_tv.setText(String.format("%.2f mBar", latestMeasurement));
    }

    @SuppressLint("DefaultLocale")
    public void initMeasurement(){
        latestMeasurement = sharedPreferences.getFloat("latest_pressure", 0);
        TextView latest_measurement_tv = (TextView) findViewById(R.id.latest_measurement_tv);
        latest_measurement_tv.setText(String.format("%.2f mBar", latestMeasurement));
    }

    public void startTimer(long time_in_s){
        if (timer == null) {
            TextView timerTextView = (TextView) findViewById(R.id.time_tv);

            timer = new CountDownTimer(time_in_s * 1000, 1000) { // 60000 milliseconds = 60 seconds
                public void onTick(long millisUntilFinished) {
                    long seconds_total = millisUntilFinished / 1000;
                    long minutes_until_measurement = (int) seconds_total / 60;
                    long seconds_until_measurement = (int) seconds_total - minutes_until_measurement * 60;
                    timerTextView.setText(minutes_until_measurement + " min " + seconds_until_measurement + " s");
                }

                public void onFinish() {
                    startTimer(3600);
                    timer = null;
                }
            }.start();
        }
    }


    public static void setToast(String message){
        Toast.makeText(MainActivity.context, message, Toast.LENGTH_LONG).show();
    }
}
