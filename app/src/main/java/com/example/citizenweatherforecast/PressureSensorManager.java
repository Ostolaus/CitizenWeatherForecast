package com.example.citizenweatherforecast;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PressureSensorManager implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor pressureSensor;
    private float currentPressure;

    private float pressureSum;
    private int pressureReadings;

    public static final int BUFFER_SIZE = 128;
    List<Float> pressureValues;
    private int current_index = 0;

    public PressureSensorManager(SensorManager sensorManager) {
        this. sensorManager = sensorManager;
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
    }

    public void startListening() {
        sensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL);
        current_index = 0;
        pressureSum = 0;
        pressureValues = new ArrayList<>(1024);
    }

    public void stopListening() {
        sensorManager.unregisterListener(this);
        current_index = 0;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        currentPressure = event.values[0];
        pressureValues.add(current_index++, currentPressure);
        pressureSum+=currentPressure;
        if(current_index == 1023){
            pressureSum -= pressureValues.get(0);
        }

        current_index%=BUFFER_SIZE;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // handle accuracy changes
    }

    public float getCurrentPressure(){
        return currentPressure;
    }

    public float getAverage(){
        return pressureSum/current_index;
    }

    public float getMedian(){
       pressureValues.sort(Float::compare);
       return pressureValues.get(current_index/2);
    }


}
