/*
 * Copyright (C) 2017 Baidu, Inc. All Rights Reserved.
 */
package com.radium.sufeguide.main;

import com.baidu.mapapi.walknavi.WalkNavigateHelper;
import com.baidu.mapapi.walknavi.adapter.IWNaviStatusListener;
import com.baidu.mapapi.walknavi.adapter.IWRouteGuidanceListener;
import com.baidu.mapapi.walknavi.adapter.IWTTSPlayer;
import com.baidu.mapapi.walknavi.model.RouteGuideKind;
import com.baidu.platform.comapi.walknavi.WalkNaviModeSwitchListener;
import com.baidu.platform.comapi.walknavi.widget.ArCameraView;
import com.radium.sufeguide.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import javax.security.auth.login.LoginException;


public class WNaviGuideActivity extends FragmentActivity {

    private final static String TAG = WNaviGuideActivity.class.getSimpleName();

    private ArFragment arFragment;
    private WalkNavigateHelper mNaviHelper;
    private FragmentTransaction fragmentTransaction;
    private SensorManager sensorManager;

    private Sensor orientationSensor = null;
    private SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            float xAngel =  Math.abs((float) (Math.round(event.values[1] * 100)) / 100);
            if (xAngel > 60){
                arFragment.resuemCam();
                ft.show(arFragment).commitAllowingStateLoss();
            } else {
                arFragment.pauseCam();
                ft.hide(arFragment).commitAllowingStateLoss();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mNaviHelper.quit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNaviHelper.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(sensorEventListener);
        mNaviHelper.pause();
    }


    private int counter = 0;
    private void findLayout(View v) throws InterruptedException {
        if (counter == 15){
            return;
        }
        if (v instanceof ImageView){
            if (counter == 14){
                v.setVisibility(View.GONE);
            }
            counter ++;
        }
        if (v == null){
            return;
        }
        if (v instanceof ViewGroup){
            ViewGroup layout = (ViewGroup) v;
            for (int i = 0; i < layout.getChildCount(); i++){
                findLayout(layout.getChildAt(i));
            }

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_walk_navi);
        FrameLayout frameLayout = findViewById(R.id.fragment_area_for_navi);
        arFragment = new ArFragment();

        fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.fragment_area_for_navi, arFragment).commit();


        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        sensorManager.registerListener(sensorEventListener,orientationSensor,SensorManager.SENSOR_DELAY_GAME);

        mNaviHelper = WalkNavigateHelper.getInstance();
        try {
            View view = mNaviHelper.onCreate(WNaviGuideActivity.this);
//            FrameLayout f1 = (FrameLayout) view;
//            RelativeLayout r1 = (RelativeLayout) f1.getChildAt(1);
//            Log.e("radium",String.valueOf(r1.getChildCount()));
//            RelativeLayout r2 = (RelativeLayout) r1.getChildAt(4);
//            ImageView imageView = (ImageView) r2.getChildAt(1);

//            if (imageView != null){
//                Log.e("radium","image found!");
//                Log.e("radium", imageView.toString());
//                imageView.setVisibility(View.GONE);
//            }

            if (view != null) {
                frameLayout.addView(view);
                findLayout(view);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        mNaviHelper.setWalkNaviStatusListener(new IWNaviStatusListener() {
            @Override
            public void onWalkNaviModeChange(int mode, WalkNaviModeSwitchListener listener) {
                mNaviHelper.switchWalkNaviMode(WNaviGuideActivity.this, mode, listener);
            }

            @Override
            public void onNaviExit() {
                Log.d(TAG, "onNaviExit");
            }
        });

        mNaviHelper.setTTsPlayer(new IWTTSPlayer() {
            @Override
            public int playTTSText(final String s, boolean b) {
                Log.d(TAG, "tts: " + s);
                return 0;
            }
        });

        boolean startResult = mNaviHelper.startWalkNavi(WNaviGuideActivity.this);
        Log.e(TAG, "startWalkNavi result : " + startResult);

        mNaviHelper.setRouteGuidanceListener(this, new IWRouteGuidanceListener() {
            @Override
            public void onRouteGuideIconUpdate(Drawable icon) {

            }

            @Override
            public void onRouteGuideKind(RouteGuideKind routeGuideKind) {
                Log.d(TAG, "onRouteGuideKind: " + routeGuideKind);
            }

            @Override
            public void onRoadGuideTextUpdate(CharSequence charSequence, CharSequence charSequence1) {
                Log.d(TAG, "onRoadGuideTextUpdate   charSequence=: " + charSequence + "   charSequence1 = : " +
                        charSequence1);

            }

            @Override
            public void onRemainDistanceUpdate(CharSequence charSequence) {
                Log.d(TAG, "onRemainDistanceUpdate: charSequence = :" + charSequence);

            }

            @Override
            public void onRemainTimeUpdate(CharSequence charSequence) {
                Log.d(TAG, "onRemainTimeUpdate: charSequence = :" + charSequence);

            }

            @Override
            public void onGpsStatusChange(CharSequence charSequence, Drawable drawable) {
                Log.d(TAG, "onGpsStatusChange: charSequence = :" + charSequence);

            }

            @Override
            public void onRouteFarAway(CharSequence charSequence, Drawable drawable) {
                Log.d(TAG, "onRouteFarAway: charSequence = :" + charSequence);

            }

            @Override
            public void onRoutePlanYawing(CharSequence charSequence, Drawable drawable) {
                Log.d(TAG, "onRoutePlanYawing: charSequence = :" + charSequence);

            }

            @Override
            public void onReRouteComplete() {

            }

            @Override
            public void onArriveDest() {

            }

            @Override
            public void onIndoorEnd(Message msg) {

            }

            @Override
            public void onFinalEnd(Message msg) {

            }

            @Override
            public void onVibrate() {

            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ArCameraView.WALK_AR_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(WNaviGuideActivity.this, "没有相机权限,请打开后重试", Toast.LENGTH_SHORT).show();
            } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mNaviHelper.startCameraAndSetMapView(WNaviGuideActivity.this);
            }
        }
    }

}
