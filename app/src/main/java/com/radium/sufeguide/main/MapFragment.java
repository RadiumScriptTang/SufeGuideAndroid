package com.radium.sufeguide.main;

import android.content.Context;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.overlayutil.WalkingRouteOverlay;
import com.baidu.mapapi.search.route.BikingRouteResult;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.IndoorRouteResult;
import com.baidu.mapapi.search.route.MassTransitRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.PlanNode;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRoutePlanOption;
import com.baidu.mapapi.search.route.WalkingRouteResult;
import com.google.gson.JsonObject;
import com.radium.sufeguide.R;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import map.baidu.ar.model.ArLatLng;
import map.baidu.ar.model.ArPoiInfo;
import map.baidu.ar.model.PoiInfoImpl;
import map.baidu.ar.utils.DistanceByMcUtils;

public class MapFragment extends Fragment {

    //视图对象
    private MapView mapView = null;
    private ImageButton centerLocationBtn = null;
    private SearchView searchView = null;
    private ListView searchResultListView = null;
    private RelativeLayout selectedLocationInfo = null;
    private TextView selectedLocationName = null;
    private TextView selectedLocationDistanceAndInclude = null;
    private ImageButton selectedLocationFindRouteBtn = null;
    //地图对象
    private BaiduMap mapObj = null;
    private boolean isFirstLoc = true;
    private LocationClient mLocationClient = null;
    private double myLat;
    private double myLng;
    private double selectedLocationLat;
    private double selectedLocationLng;

    private MyAdapter myAdapter;


    private class MyAdapter extends BaseAdapter implements Filterable {

        private Context context;
        private MyFilter myFilter;
        private JSONArray jsonArray;
        private JSONArray originalJsonArray;

        private class MyFilter extends Filter {
            private String prefix;

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                JSONArray j = new JSONArray();

                prefix = (String) constraint;
                if (prefix == null || prefix.length() == 0){
                    filterResults.count = originalJsonArray.size();
                    filterResults.values = originalJsonArray;
                    return filterResults;
                }

                for(int i = 0; i < originalJsonArray.size(); i++){
                    JSONObject o = originalJsonArray.getJSONObject(i);
                    if (filterKernel(o.getString("locationName")) || filterKernel(o.getString("locationDetail"))){
                        j.add(o);
                    }
                }
                filterResults.values = j;
                filterResults.count = j.size();
                Log.e("radium:jsonAfterFilter",j.toJSONString());
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                JSONArray j = (JSONArray) results.values;
                if (j == null){
                    Log.e("radium","null pointer gotten");
                } else {
                    Log.e("radium",j.toJSONString());
                    jsonArray = j;
                }
                if (results.count > 0){
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }

            private boolean filterKernel(String string){
                int i = 0;
                int j = 0;
                if (string.length() < 1){
                    return false;
                }
                string += string.charAt(string.length() - 1);
                while (j < prefix.length()){
                    while (i < string.length() && string.charAt(i++) != prefix.charAt(j)){

                    }
                    j++;
                }
                return i < string.length();
            }
        }

        public MyAdapter(Context context, JSONArray jsonArray){
            this.context = context;
            this.jsonArray = (JSONArray) jsonArray.clone();
            this.originalJsonArray = (JSONArray) jsonArray.clone();
            this.myFilter = new MyFilter();
        }

        @Override
        public int getCount() {
            return jsonArray.size();
        }

        @Override
        public Object getItem(int position) {
            return jsonArray.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public JSONObject getItemAtPosition(int p){
            return (JSONObject) jsonArray.get(p);
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            RelativeLayout item;
            item = (RelativeLayout) LayoutInflater.from(context).inflate(R.layout.search_list_view_item,parent,false);
            TextView t1 = item.findViewById(R.id.search_list_view_name);
            TextView t2 = item.findViewById(R.id.search_list_view_include);
            JSONObject jsonObject = (JSONObject) jsonArray.get(position);
            t1.setText(jsonObject.getString("locationName"));
            t2.setText(jsonObject.getString("locationDetail"));

            return item;
        }

        @Override
        public Filter getFilter() {
            return this.myFilter;
        }
    }

    private class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            //mapView 销毁后不在处理新接收的位置
            if (location == null || mapObj == null){
                return;
            }
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(location.getDirection()).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            myLat = location.getLatitude();
            myLng = location.getLongitude();
            mapObj.setMyLocationData(locData);
            if (isFirstLoc){
                isFirstLoc = false;
                LatLng p = new LatLng(myLat, myLng);
                MapStatus mapStatus = new MapStatus.Builder().target(p).zoom(18).build();
                mapObj.setMapStatus(MapStatusUpdateFactory.newMapStatus(mapStatus));
            }
            initArPoints(myLat,myLng);
        }
        private void initArPoints(double lat, double lng){
            ArFragment.poiInfos = new ArrayList<>();
            for (int i = 0; i < TestActivity.locationsArray.size(); i++){
                JSONObject jsonObject = (JSONObject) TestActivity.locationsArray.get(i);
                ArPoiInfo info = new ArPoiInfo();
                info.name = jsonObject.getString("locationName");
                double local_lat = jsonObject.getDouble("locationLat");
                double local_lng = jsonObject.getDouble("locationLng");

                double distance = DistanceByMcUtils.getDistanceByLL(local_lat,local_lng,lat,lng);
                if (distance < ArFragment.visibleDistance){
                    info.location = new ArLatLng(local_lat,local_lng);
                    info.city = jsonObject.getString("locationDetail");
                    PoiInfoImpl poiInfo = new PoiInfoImpl();
                    poiInfo.setPoiInfo(info);
                    ArFragment.poiInfos.add(poiInfo);
                }

            }
        }
    }


    //路线规划对象
    private RoutePlanSearch mSearch = null;
    //搜索结果适配器对象
    private SimpleAdapter searchResultListViewAdpater;

    //生成搜索部分的适配器对象
    private SimpleAdapter getSimpleAdapter() {
        FileInputStream fis = null;
        List<HashMap<String, Object>> list = new ArrayList<>();

        JSONArray jsonArray = TestActivity.locationsArray;
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject location = (JSONObject) jsonArray.get(i);
            HashMap<String, Object> map = new HashMap<>();
            map.put("name", location.get("locationName"));
            map.put("include", location.getString("locationDetail"));
            map.put("lat", location.getDouble("locationLat"));
            map.put("lng", location.getDouble("locationLng"));
            list.add(map);
        }


//        String [] names = {"SIME","21 Building"};
//        String [] include = {"向老师办公室","雷帅实验室"};
//        Double [] lat = {31.308140527855457,31.311495102593028};
//        Double [] lng = {121.50482983858917,121.50251949141196};
//        List<HashMap<String,Object>> list = new ArrayList<>();
//        for (int i = 0; i < names.length; i++){
//            HashMap<String,Object> map = new HashMap<>();
//            map.put("name",names[i]);
//            map.put("include", include[i]);
//            map.put("lat",lat[i]);
//            map.put("lng",lng[i]);
//            list.add(map);
//        }

        return new SimpleAdapter(getActivity().getApplicationContext(), list, R.layout.search_list_view_item, new String[]{"name", "include"}, new int[]{R.id.search_list_view_name, R.id.search_list_view_include});
    }

    //获取视图对象
    private void findViews(View view) {
        mapView = view.findViewById(R.id.bmap);
        centerLocationBtn = view.findViewById(R.id.bmap_location);

        searchView = view.findViewById(R.id.search_input);
        searchResultListView = view.findViewById(R.id.search_result_listview);

        selectedLocationInfo = view.findViewById(R.id.selected_location_info);
        selectedLocationName = view.findViewById(R.id.selected_location_name);
        selectedLocationDistanceAndInclude = view.findViewById(R.id.selected_location_distance_and_include);
        selectedLocationFindRouteBtn = view.findViewById(R.id.selected_location_find_route_btn);

    }

    //初始化视图：设置地图对象、定位
    private void initMap() {
        mapObj = mapView.getMap();
        mapObj.setMyLocationEnabled(true);

        mLocationClient = new LocationClient(getActivity());

//通过LocationClientOption设置LocationClient相关参数
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(1000);

//设置locationClientOption
        mLocationClient.setLocOption(option);

//注册LocationListener监听器
        MyLocationListener myLocationListener = new MyLocationListener();
        mLocationClient.registerLocationListener(myLocationListener);
//开启地图定位图层
        mLocationClient.start();



    }

    //初始化视图设置
    private void initViews() {
        //设置搜索过滤器及初始不可见
        searchResultListView.setTextFilterEnabled(true);
        searchResultListView.setVisibility(View.INVISIBLE);
        //设置导航按钮不可见
        selectedLocationInfo.setVisibility(View.GONE);
        //  初始化适配器
        searchResultListViewAdpater = getSimpleAdapter();

        //测试新适配器
        myAdapter = new MyAdapter(getActivity(),TestActivity.locationsArray);
        searchResultListView.setAdapter(myAdapter);
    }

    //测试跳转


    private void setTextForSelectedLocationInfo(String name, String include, String distance) {
        selectedLocationName.setText(name);
        selectedLocationDistanceAndInclude.setText(distance + "|" + include);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_main, null);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        findViews(getView());
        initViews();
        initMap();



        mSearch = RoutePlanSearch.newInstance();
        mSearch.setOnGetRoutePlanResultListener(new OnGetRoutePlanResultListener() {
            @Override
            public void onGetWalkingRouteResult(WalkingRouteResult walkingRouteResult) {
                if (walkingRouteResult.getRouteLines() == null){
                    return;
                }
                Log.i("radium",walkingRouteResult.getRouteLines().toString());

                if (walkingRouteResult != null && walkingRouteResult.getRouteLines().size() > 0 ){
                    WalkingRouteOverlay overlay = new WalkingRouteOverlay(mapObj);
                    overlay.setData(walkingRouteResult.getRouteLines().get(0));
                    overlay.addToMap();
                }
            }

            @Override
            public void onGetTransitRouteResult(TransitRouteResult transitRouteResult) {

            }

            @Override
            public void onGetMassTransitRouteResult(MassTransitRouteResult massTransitRouteResult) {

            }

            @Override
            public void onGetDrivingRouteResult(DrivingRouteResult drivingRouteResult) {

            }

            @Override
            public void onGetIndoorRouteResult(IndoorRouteResult indoorRouteResult) {

            }

            @Override
            public void onGetBikingRouteResult(BikingRouteResult bikingRouteResult) {

            }
        });





        selectedLocationFindRouteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PlanNode stNode = PlanNode.withLocation(new LatLng(myLat,myLng));
                PlanNode edNode = PlanNode.withLocation(new LatLng(selectedLocationLat,selectedLocationLng));
                mSearch.walkingSearch((new WalkingRoutePlanOption()).from(stNode).to(edNode));
            }
        });




        searchResultListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                JSONObject object = (JSONObject) myAdapter.getItem(i);
                double lat = object.getDouble("locationLat");
                double lng = object.getDouble("locationLng");
                LatLng point = new LatLng(lat,lng);
                BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.mipmap.location);
                OverlayOptions options = new MarkerOptions().position(point).icon(bitmapDescriptor);
                mapObj.clear();
                mapObj.addOverlay(options);
                mapObj.setMapStatus(MapStatusUpdateFactory.newLatLng(point));
                searchView.clearFocus();
                searchResultListView.setVisibility(View.INVISIBLE);
                setTextForSelectedLocationInfo(object.getString("locationName"),object.getString("locationDetail"),String.valueOf(Math.floor(DistanceByMcUtils.getDistanceByLL(myLat,myLng,lat,lng))) + "米");
                selectedLocationInfo.setVisibility(View.VISIBLE);
                searchView.setQueryHint(object.getString("locationName").toString());

                selectedLocationLat = lat;
                selectedLocationLng = lng;
            }
        });


        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                searchResultListView.setVisibility(View.INVISIBLE);
                selectedLocationInfo.setVisibility(View.GONE);
                mapObj.clear();
                return false;
            }
        });

        searchView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (!b){
                    searchResultListView.setVisibility(View.VISIBLE);
                } else {
                    searchResultListView.setVisibility(View.INVISIBLE);
                }
            }
        });
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchResultListView.setVisibility(View.VISIBLE);
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                if(!TextUtils.isEmpty(s)){
                    myAdapter.getFilter().filter(s);
                } else {
                    myAdapter.getFilter().filter(null);
                }
                return false;
            }
        });



        centerLocationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mapObj.animateMapStatus(MapStatusUpdateFactory.newLatLng(new LatLng(myLat, myLng)));
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }
}
