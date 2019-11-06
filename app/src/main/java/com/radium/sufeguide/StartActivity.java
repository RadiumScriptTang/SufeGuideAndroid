package com.radium.sufeguide;

import android.Manifest;
import android.app.Application;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.radium.sufeguide.main.TestActivity;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;






public class StartActivity extends AppCompatActivity {
    private String response;
    private Map<String ,String > param;

    //读写权限
    private static String[] PERMISSIONS_STORAGE = {
            "Manifest.permission.READ_EXTERNAL_STORAGE",
            "Manifest.permission.WRITE_EXTERNAL_STORAGE"
    };
    //请求状态码
    private static int REQUEST_PERMISSION_CODE = 1;
    private int requestCode;
    private String[] permissions;
    private int[] grantResults;

    public static String getFileMD5(FileInputStream in, String algorithm) {

        MessageDigest digest = null;
        byte buffer[] = new byte[1024];
        int len;

        try {
            digest = MessageDigest.getInstance(algorithm);
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        BigInteger bigInt = new BigInteger(1, digest.digest());
        return bigInt.toString();
    }
    Thread enterMainThread = new Thread(new Runnable() {
        @Override
        public void run() {
            downloadThread.start();
            try {
                downloadThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Intent intent = new Intent(getApplicationContext(), TestActivity.class);
            startActivity(intent);
            finish();
        }
    });
    Thread downloadThread = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                URL url = new URL("http://59.110.174.149:8080/MVC5/app/download/上海财经大学国定路校区");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setReadTimeout(2000);
                connection.setConnectTimeout(2000);
                connection.setRequestProperty("Charset","UTF-8");
                connection.setRequestMethod("GET");
                if (connection.getResponseCode() != 200){
                    Log.e("radium","http error" + connection.getResponseCode());
                    return;
                }
                InputStream inputStream = connection.getInputStream();
                int length = connection.getContentLength();

                String fName = "上海财经大学国定路校区";
                FileOutputStream outputStream = openFileOutput(fName, MODE_PRIVATE);
                byte [] bs = new byte[1024];
                while ((length = inputStream.read(bs)) != -1){
                    outputStream.write(bs,0,length);
                    Log.e("radium",bs.toString());
                }
                outputStream.flush();
                outputStream.close();
                inputStream.close();

                Log.e("radium","download finished");
                //enterMainThread.start();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
    });

    Thread checkThread = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                FileInputStream fis = openFileInput("上海财经大学国定路校区");
                String sha1 = getFileMD5(fis, "SHA1");
                String university = "上海财经大学国定路校区";
                Log.e("radium",sha1);
                String url = "http://59.110.174.149:8080/MVC5/app/update";
                Map<String,String> map = new HashMap<>();
                map.put("SHA1",sha1);
                map.put("university",university);
                HttpHelper httpHelper = new HttpHelper(url,map,"UTF-8");
                String res = httpHelper.submitPostData();
                Log.e("radium",res);
                if(res.contains("1")){
                    downloadThread.start();
                } else {
                    Log.e("radium","校验无误！");
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
                downloadThread.start();
            }
            Log.e("radium","核对完成！");
        }
    });

    @Override
    protected void onStart(){
        checkPermission();
        super.onStart();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0){
            finish();
            return;
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);//隐藏状态栏
        setContentView(R.layout.activity_start);
//        try {
//            checkThread.start();
//            checkThread.join();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        enterMainThread.start();


    }

    public void checkPermission()
    {
        int targetSdkVersion = 0;
        String[] PermissionString={Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,Manifest.permission.ACCESS_FINE_LOCATION};
        try {
            final PackageInfo info = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
            targetSdkVersion = info.applicationInfo.targetSdkVersion;//获取应用的Target版本
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
//            Log.e("err", "检查权限_err0");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //Build.VERSION.SDK_INT是获取当前手机版本 Build.VERSION_CODES.M为6.0系统
            //如果系统>=6.0
            if (targetSdkVersion >= Build.VERSION_CODES.M) {
                //第 1 步: 检查是否有相应的权限
                boolean isAllGranted = checkPermissionAllGranted(PermissionString);
                if (isAllGranted) {
                    //Log.e("err","所有权限已经授权！");
                    return;
                }
                // 一次请求多个权限, 如果其他有权限是已经授予的将会自动忽略掉
                ActivityCompat.requestPermissions(this,
                        PermissionString, 1);
            }
        }
    }

    /**
     * 检查是否拥有指定的所有权限
     */
    private boolean checkPermissionAllGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                // 只要有一个权限没有被授予, 则直接返回 false
                //Log.e("err","权限"+permission+"没有授权");
                return false;
            }
        }
        return true;
    }

    //申请权限结果返回处理
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }

}
