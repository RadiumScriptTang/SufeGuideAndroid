package com.radium.sufeguide.main;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.radium.sufeguide.R;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import map.baidu.ar.model.ArLatLng;
import map.baidu.ar.model.ArPoiInfo;
import map.baidu.ar.model.PoiInfoImpl;

public class TestActivity extends AppCompatActivity {
    public static JSONArray locationsArray;
    private float [] accValues = new float[3];
    private float [] magValue = new float[3];
    private FragmentTransaction fragmentTransaction;
    private MapFragment mapFragment = new MapFragment();
    private ArFragment arFragment = new ArFragment();

    private Sensor orientationSensor = null;
    private SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            float xAngel =  Math.abs((float) (Math.round(event.values[1] * 100)) / 100);
            if (xAngel > 60){
                arFragment.resuemCam();
                ft.hide(mapFragment).show(arFragment).commit();
            } else {
                arFragment.pauseCam();
                ft.hide(arFragment).show(mapFragment).commit();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    private void loadLocationsArray() {
        FileInputStream fis = null;
        try {
            fis = openFileInput("上海财经大学国定路校区");
            byte[] temp = new byte[fis.available()];
            fis.read(temp);
            String jsonString = new String(temp);
            locationsArray = JSONArray.parseArray(jsonString);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        loadLocationsArray();
        fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.fragment_area, mapFragment);
        fragmentTransaction.add(R.id.fragment_area, arFragment);
        fragmentTransaction.hide(arFragment);
        fragmentTransaction.commit();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.hide(mapFragment).show(arFragment).commit();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.hide(arFragment).show(mapFragment).commit();

        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        sensorManager.registerListener(sensorEventListener,orientationSensor,SensorManager.SENSOR_DELAY_GAME);


//        startService(new Intent(this, MyService.class));
    }





}
