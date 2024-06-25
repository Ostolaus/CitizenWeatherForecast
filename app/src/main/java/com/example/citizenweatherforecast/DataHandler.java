package com.example.citizenweatherforecast;

import android.os.Looper;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.lang.Math;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import cz.msebera.android.httpclient.Header;


public class DataHandler {

    private static AsyncHttpClient asyncHttpClient = new AsyncHttpClient();

    public List<Float> temperatures = new ArrayList<>();
    public List<Float> humidities = new ArrayList<>();
    public List<Float> pressures = new ArrayList<>();

    public float[] ownPressures = new float[24];

    private List<List<Float>> NNData = new ArrayList<>();

    private final Object lock = new Object();

    public void getCurrentData(
    ) {
        String url = "https://api.open-meteo.com/v1/forecast?latitude=47.0667&longitude=15.4333&elevation=320&current=&hourly=temperature_2m,relative_humidity_2m,surface_pressure&timezone=Europe%2FBerlin&start_date=2024-06-18&end_date=2024-06-19";

        RequestParams rp = new RequestParams();

        JsonHttpResponseHandler responseHandler = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                synchronized (lock) {
                    try {
                        JSONObject serverResp = new JSONObject(response.toString());
                        saveData(serverResp);
                    } catch (JSONException | ParseException e) {
                        Toast.makeText(MainActivity.instance, "Fetching Data Failed", Toast.LENGTH_SHORT).show();
                    }
                    lock.notifyAll();
                }
            }

        };
        responseHandler.setUsePoolThread(true);
        get(url, rp, responseHandler);
    }

    public void saveData(JSONObject data) throws JSONException, ParseException {
        JSONObject hourly = (JSONObject) data.get("hourly");
        JSONArray times = (JSONArray) hourly.get("time");
        JSONArray temps = (JSONArray) hourly.get("temperature_2m");
        JSONArray hums = (JSONArray) hourly.get("relative_humidity_2m");
        JSONArray pres = (JSONArray) hourly.get("surface_pressure");
        temperatures = new Gson().fromJson(String.valueOf(temps), new TypeToken<List<Float>>() {}.getType());
        humidities = new Gson().fromJson(String.valueOf(hums), new TypeToken<List<Float>>() {}.getType());
        pressures = new Gson().fromJson(String.valueOf(pres), new TypeToken<List<Float>>() {}.getType());

        for (int i = 0; i < times.length(); i++) {
            long seconds = LocalDateTime.parse(times.get(i).toString()).atZone(ZoneId.of(ZoneOffset.systemDefault().getId())).toEpochSecond();
            double day_sin = Math.sin(seconds * 2 * Math.PI/(60*60*24));
            double day_cos = Math.cos(seconds * 2 * Math.PI/(60*60*24));
            double year_sin = Math.sin(seconds * 2 * Math.PI/(60*60*24*365.2425));
            double year_cos = Math.cos(seconds * 2 * Math.PI/(60*60*24*365.2425));

            List<Float> entry = new ArrayList<>();
            entry.add(temperatures.get(i));
            entry.add(humidities.get(i));
            entry.add(pressures.get(i));
            entry.add(0.f);
            entry.add((float)day_sin);
            entry.add((float)day_cos);
            entry.add((float)year_sin);
            entry.add((float)year_cos);
            NNData.add(entry);}
    }

    public List<List<Float>> getNNData() throws InterruptedException {
        if (NNData.isEmpty()){
            Thread t1 = new Thread(new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();
                    getCurrentData();
                    Looper.loop();
                }
            });
            t1.start();
        }

        synchronized (lock) {
            while (NNData.isEmpty()){
                try {
                    lock.wait(); // Wait until data is fetched
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        List<List<Float>> ret_vals = new ArrayList<>();
        ZonedDateTime currentTime = LocalDateTime.now().atZone(ZoneId.of(ZoneOffset.systemDefault().getId()));
        int hour = currentTime.getHour();

        for (int i = NNData.size()-24 + hour - 6 + 1; i <= NNData.size()- 24 + hour; i++) {
            ret_vals.add(NNData.get(i));
        }

        return ret_vals;
    }


    public void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        asyncHttpClient.get(url, params, responseHandler);
    }



}
