package com.radium.sufeguide;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.radium.sufeguide.test.TestActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;




public class StartActivity extends AppCompatActivity {
    private String response;
    private Map<String ,String > param;
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
        }
    });
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_start);

        checkThread.start();
        while (checkThread.isAlive()){

        }
        enterMainThread.start();


    }
}
