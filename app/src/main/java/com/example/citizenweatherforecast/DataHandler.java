package com.example.citizenweatherforecast;

import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;


public class DataHandler {

    private static final AsyncHttpClient asyncHttpClient = new AsyncHttpClient();

    public float[] ownPressures = new float[24];

    private final List<NNDataEntry> NNData = new ArrayList<>(24);

    private final Object lock = new Object();

    public void getCurrentData(
    ) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String url = String.format("https://api.open-meteo.com/v1/forecast?latitude=47.0667&longitude=15.4333&elevation=320&current=&hourly=temperature_2m,relative_humidity_2m,surface_pressure&timezone=Europe%%2FBerlin&start_date=%s&end_date=%s", date, date);

        RequestParams rp = new RequestParams();

        JsonHttpResponseHandler responseHandler = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                synchronized (lock) {
                    try {
                        JSONObject serverResp = new JSONObject(response.toString());
                        saveData(serverResp);
                        insertOwnPressures();
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
        JSONArray tms = (JSONArray) hourly.get("time");
        JSONArray temps = (JSONArray) hourly.get("temperature_2m");
        JSONArray hums = (JSONArray) hourly.get("relative_humidity_2m");
        JSONArray pres = (JSONArray) hourly.get("surface_pressure");
        List<Float> temperatures = new Gson().fromJson(String.valueOf(temps), new TypeToken<List<Float>>() {}.getType());
        List<Float> humidities = new Gson().fromJson(String.valueOf(hums), new TypeToken<List<Float>>() {}.getType());
        List<Float> pressures = new Gson().fromJson(String.valueOf(pres), new TypeToken<List<Float>>() {}.getType());
        List<String> times = new Gson().fromJson(String.valueOf(tms), new TypeToken<List<String>>() {}.getType());

        Log.d("DATA", "saveData: " + times.size());
        for (int i = 0; i < pressures.size(); i++) {
            NNDataEntry nnDataEntry = new NNDataEntry(times.get(i), pressures.get(i), temperatures.get(i), humidities.get(i));
            if (NNData.size() <= 24)
                NNData.add(i, nnDataEntry);
            else{
                NNData.set(i, nnDataEntry);
            }
       }
    }

    public List<List<Float>> generateNNData() {
        if (NNData.isEmpty()){
            Thread t1 = new Thread(() -> {
                Looper.prepare();
                getCurrentData();
                Looper.loop();
            });
            t1.start();
        }

        synchronized (lock) {
            while (NNData.isEmpty()){
                try {
                    lock.wait(); // Wait until data is fetched
                } catch (InterruptedException ignored) {
                }
            }
        }
        List<List<Float>> ret_vals = new ArrayList<>();
        ZonedDateTime currentTime = LocalDateTime.now().atZone(ZoneId.of(ZoneOffset.systemDefault().getId()));
        int hour = currentTime.getHour();

        for (int i = NNData.size()-24 + hour - 6 + 1; i <= NNData.size()- 24 + hour; i++) {
            ret_vals.add(NNData.get(i).getEntry());
        }

        return ret_vals;
    }

    public void updatePressure(float pressure){
        MainActivity.sharedEditor.putFloat("latest_pressure", pressure);
        MainActivity.sharedEditor.putFloat(Integer.toString(LocalTime.now().getHour()), pressure);
        MainActivity.sharedEditor.putFloat(Integer.toString((LocalTime.now().getHour()-5 + 24) % 24), 0);
        MainActivity.sharedEditor.apply();

        ownPressures[LocalTime.now().getHour()] = pressure;
        ownPressures[(LocalTime.now().getHour()-5 + 24)%24] = 0;
        MainActivity.updatePressureReading(pressure);

        getCurrentData();
    }


    private void insertOwnPressures(){
        for (int i = 0; i < 24; i++) {
            if (ownPressures[i] != 0){

                NNData.get(i).setPressure(ownPressures[i]);
            }
        }
    }

    private static void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        asyncHttpClient.get(url, params, responseHandler);
    }


}
