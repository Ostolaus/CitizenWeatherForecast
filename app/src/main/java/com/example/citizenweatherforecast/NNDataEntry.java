package com.example.citizenweatherforecast;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public class NNDataEntry {
    public String rawTime;
    public float pressure;
    public float temperature;

    public float humidity;
    public double day_sin;
    public double day_cos;
    public double year_sin;
    public double year_cos;

    public NNDataEntry(String time,float pressure,float temperature,float humidity){
        this.rawTime = time;
        this.pressure = pressure;
        this.temperature = temperature;
        this.humidity = humidity;

        long seconds = LocalDateTime.parse(time).atZone(ZoneId.of(ZoneOffset.systemDefault().getId())).toEpochSecond();
        day_sin = Math.sin(seconds * 2 * Math.PI/(60*60*24));
        day_cos = Math.cos(seconds * 2 * Math.PI/(60*60*24));
        year_sin = Math.sin(seconds * 2 * Math.PI/(60*60*24*365.2425));
        year_cos = Math.cos(seconds * 2 * Math.PI/(60*60*24*365.2425));
    }

    public List<Float> getEntry(){
        List<Float> entry = new ArrayList<>();
        entry.add(temperature);
        entry.add(humidity);
        entry.add(pressure);
        entry.add(0.f); //rain not implemented
        entry.add((float)day_sin);
        entry.add((float)day_cos);
        entry.add((float)year_sin);
        entry.add((float)year_cos);
        return entry;
    }

    public void setPressure(float pressure){
        this.pressure = pressure;
    }

}
