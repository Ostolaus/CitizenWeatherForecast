package com.example.citizenweatherforecast;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

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
        Intent serviceIntent = new Intent(getApplicationContext(), PressureSensorManager.class);
        PendingIntent pendingIntent = PendingIntent.getService(
                getApplicationContext(),
                0,
                serviceIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        Calendar calendar = Calendar.getInstance();
        int minutesUntilNextHour = 60 - calendar.get(Calendar.MINUTE);
        long initialDelayMillis = minutesUntilNextHour * 60 * 1000;

        long intervalMillis = AlarmManager.INTERVAL_HOUR;
        alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + initialDelayMillis,
                intervalMillis,
                pendingIntent
        );



        return START_STICKY;
    }
}
