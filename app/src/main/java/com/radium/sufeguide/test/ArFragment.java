package com.radium.sufeguide.test;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;
import com.radium.sufeguide.R;

import java.util.ArrayList;

import map.baidu.ar.ArPageListener;
import map.baidu.ar.camera.SimpleSensor;
import map.baidu.ar.camera.find.FindArCamGLView;
import map.baidu.ar.model.ArLatLng;
import map.baidu.ar.model.ArPoiInfo;
import map.baidu.ar.model.PoiInfoImpl;
import map.baidu.ar.utils.TypeUtils;

public class ArFragment extends Fragment {

    private RelativeLayout camRl;
    private FindArCamGLView mCamGLView;
    public static ArrayList<PoiInfoImpl> poiInfos;
    private RelativeLayout mArPoiItemRl;
    private SimpleSensor mSensor;
    private ArPageListener onSelectedNodeListener = new ArPageListener() {
        @Override
        public void noPoiInScreen(boolean b) {

        }

        @Override
        public void selectItem(Object o) {
            Toast.makeText(getActivity(),"点击事件",Toast.LENGTH_LONG).show();
        }
    };

    /**
     * 初始化视图
     */
    private void initView() {
        camRl = (RelativeLayout) getView().findViewById(R.id.cam_rl);
        mCamGLView = (FindArCamGLView) LayoutInflater.from(getActivity()).inflate(R.layout.layout_find_cam_view, null);
        mCamGLView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {

            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (bottom == 0 || oldBottom != 0 || mCamGLView == null) {
                    return;
                }
                RelativeLayout.LayoutParams params = TypeUtils.safeCast(
                        mCamGLView.getLayoutParams(), RelativeLayout.LayoutParams.class);
                if (params == null) {
                    return;
                }
                params.height = bottom - top;
                mCamGLView.requestLayout();
            }
        });
        camRl.addView(mCamGLView);
        initSensor();
        // 保持屏幕不锁屏
    }

    private void initSensor() {
        if (mSensor == null) {
            mSensor = new SimpleSensor(getActivity(), new HoldPositionListenerImp());
        }
        mSensor.startSensor();
    }

    private class HoldPositionListenerImp implements SimpleSensor.OnHoldPositionListener {
        @Override
        public void onOrientationWithRemap(float[] remapValue) {
            if (mCamGLView != null && mArPoiItemRl != null) {
                if (poiInfos.size() <= 0) {
                    mArPoiItemRl.setVisibility(View.GONE);
                    Toast.makeText(getActivity(), "附近没有可识别的类别", Toast.LENGTH_LONG).show();
                } else {
                    mCamGLView.setFindArSensorState(remapValue, getLayoutInflater(),
                            mArPoiItemRl, onSelectedNodeListener, poiInfos, getActivity());
                    mArPoiItemRl.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    public void pauseCam(){
        mCamGLView.setVisibility(View.INVISIBLE);
    }
    public void resuemCam(){
        mCamGLView.setVisibility(View.VISIBLE);
    }
    private void finishCamInternal() {
        if (mCamGLView != null) {
            mCamGLView.stopCam();
            camRl.removeAllViews();
            mCamGLView = null;

        }
        if (mArPoiItemRl != null) {
            mArPoiItemRl.removeAllViews();
        }
        if (mSensor != null) {
            mSensor.stopSensor();
        }
        // 恢复屏幕自动锁屏
    }




    @Override
    public void onDestroy() {
        super.onDestroy();
        finishCamInternal();
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_ar,null);
    }

    @Override
    public void onStart() {
        super.onStart();
        poiInfos = new ArrayList<>();

        for (int i = 0; i < TestActivity.locationsArray.size(); i++){
            JSONObject jsonObject = (JSONObject) TestActivity.locationsArray.get(i);
            ArPoiInfo info = new ArPoiInfo();
            info.name = jsonObject.getString("locationName");
            double local_lat = jsonObject.getDouble("locationLat");
            double local_lng = jsonObject.getDouble("locationLng");
            info.location = new ArLatLng(local_lat,local_lng);
            PoiInfoImpl poiInfo = new PoiInfoImpl();
            poiInfo.setPoiInfo(info);
            poiInfos.add(poiInfo);
        }
        mArPoiItemRl = (RelativeLayout) getView().findViewById(R.id.ar_poi_item_rl);
        mArPoiItemRl.setVisibility(View.VISIBLE);
        initView();
    }
}
