package com.example.citizenweatherforecast;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.time.LocalTime;
import java.util.Calendar;
import java.util.concurrent.CompletableFuture;

public class WorkService extends Service {

    @Override
    public void onCreate() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms();
        }
        Intent serviceIntent = new Intent(getApplicationContext(), WorkService.class);
        PendingIntent pendingIntent = PendingIntent.getService(
                getApplicationContext(),
                0,
                serviceIntent,
                PendingIntent.FLAG_UPDATE_CURRENT + PendingIntent.FLAG_MUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        //int millisUntilNextHour = (60 - calendar.get(Calendar.MINUTE)) *60 *1000;
        int millisUntilNextHour = 3000;
        alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + millisUntilNextHour,
                pendingIntent
        );

        MainActivity.pressureSensorManager.startListening();
        float avg = MainActivity.pressureSensorManager.getCurrentPressure();
        Log.d("PRESSURE", "" + avg);
        MainActivity.pressureSensorManager.stopListening();

        return START_STICKY;
    }
}
