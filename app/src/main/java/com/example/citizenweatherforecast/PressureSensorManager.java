package com.example.citizenweatherforecast;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class PressureSensorManager implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor pressureSensor;
    private float currentPressure;
    private float pressureSum = 0;
    private int pressureReadings = 0;

    public PressureSensorManager(SensorManager sensorManager) {
        this. sensorManager = sensorManager;
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
    }

    public void startListening() {
        sensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void stopListening() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        currentPressure = event.values[0];

        pressureSum+=currentPressure;
        pressureReadings++;
            }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // handle accuracy changes
    }

    public float getCurrentPressure(){
        return currentPressure;
    }

    public float getAverage(){
        float avg =  pressureSum/pressureReadings;
        pressureSum = 0;
        pressureReadings = 0;
        return avg;
    }


}
