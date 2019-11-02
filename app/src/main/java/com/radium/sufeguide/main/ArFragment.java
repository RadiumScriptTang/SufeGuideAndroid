package com.radium.sufeguide.main;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;
import com.radium.sufeguide.R;
import com.warkiz.widget.IndicatorSeekBar;

import java.util.ArrayList;

import map.baidu.ar.ArPageListener;
import map.baidu.ar.camera.SimpleSensor;
import map.baidu.ar.camera.find.FindArCamGLView;
import map.baidu.ar.model.ArLatLng;
import map.baidu.ar.model.ArPoiInfo;
import map.baidu.ar.model.PoiInfoImpl;
import map.baidu.ar.utils.DistanceByMcUtils;
import map.baidu.ar.utils.TypeUtils;

public class ArFragment extends Fragment {

    public static ArrayList<PoiInfoImpl> poiInfos;
    public static double visibleDistance = 1000d;
    public static double [] distanceOptions = {200.0, 500.0, 1000.0};

    private IndicatorSeekBar indicatorSeekBar;
    private RelativeLayout detailRelativeLayout;
    private TextView closeDetail;
    private LinearLayout distanceOptionLayout;
    private TextView detailName;
    private TextView detailContent;

    private RelativeLayout camRl;
    private FindArCamGLView mCamGLView;
    private RelativeLayout mArPoiItemRl;
    private SimpleSensor mSensor;
    private ArPageListener onSelectedNodeListener = new ArPageListener() {
        @Override
        public void noPoiInScreen(boolean b) {

        }

        @Override
        public void selectItem(Object o) {
            if (o instanceof  PoiInfoImpl){
                PoiInfoImpl poiInfo = (PoiInfoImpl) o;
                ArPoiInfo arPoiInfo = poiInfo.getPoiInfo();
//                Toast.makeText(getActivity(),arPoiInfo.city,Toast.LENGTH_LONG).show();
                detailName.setText(arPoiInfo.name);
                detailContent.setText(arPoiInfo.city);
                detailRelativeLayout.animate().translationY(0).start();
                distanceOptionLayout.animate().translationY(0).start();

            }
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
        indicatorSeekBar = getView().findViewById(R.id.seek_bar);
        indicatorSeekBar.setOnSeekChangeListener(new IndicatorSeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(IndicatorSeekBar seekBar, int progress, float progressFloat, boolean fromUserTouch) {
                ArFragment.visibleDistance = ArFragment.distanceOptions[progress - 1];
            }

            @Override
            public void onSectionChanged(IndicatorSeekBar seekBar, int thumbPosOnTick, String tickBelowText, boolean fromUserTouch) {

            }

            @Override
            public void onStartTrackingTouch(IndicatorSeekBar seekBar, int thumbPosOnTick) {

            }

            @Override
            public void onStopTrackingTouch(IndicatorSeekBar seekBar) {

            }
        });

        detailRelativeLayout = getView().findViewById(R.id.detail_relative_layout);
        closeDetail = getView().findViewById(R.id.close_detail);
        distanceOptionLayout = getView().findViewById(R.id.distanc_option_layout);
        detailName = getView().findViewById(R.id.detail_name);
        detailContent = getView().findViewById(R.id.detail_content);

        closeDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                detailRelativeLayout.animate().translationY(detailRelativeLayout.getHeight()).start();
                distanceOptionLayout.animate().translationY(detailRelativeLayout.getHeight()).start();

            }
        });
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
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_ar,null);
    }

    @Override
    public void onStart() {
        super.onStart();
        ArFragment.poiInfos = new ArrayList<>();
        mArPoiItemRl = (RelativeLayout) getView().findViewById(R.id.ar_poi_item_rl);
        mArPoiItemRl.setVisibility(View.VISIBLE);
        initView();
    }
}
