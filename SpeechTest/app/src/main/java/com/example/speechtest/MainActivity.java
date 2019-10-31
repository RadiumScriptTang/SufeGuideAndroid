package com.example.speechtest;

import android.app.Activity;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.baidu.aip.speech.AipSpeech;

import org.json.JSONObject;

import static com.example.speechtest.GlobalConfig.AUDIO_FORMAT;
import static com.example.speechtest.GlobalConfig.CHANNEL_CONFIG;
import static com.example.speechtest.GlobalConfig.SAMPLE_RATE_INHZ;

public class MainActivity extends Activity {

    //声音相关
    private AudioRecord mAudioRecord = null;
    private int audioBufferSize;
    private byte[] speechBuffer;
    private int speechBufferOffset;
    private boolean isRecording;
    private JSONObject speechResult;

    //语音识别参数
    public static final String APP_ID = "16977327";
    public static final String API_KEY = "hXzoqrXASEwjpr5HShRtz4d9";
    public static final String SECRET_KEY = "LVfh2HNpMGXphqLYNx3x5p64rSwhVqGX";


    final private String TAG = "RadiumScript";

    //图形界面
    Button startRecordButton, stopRecordButton;

    //初始化
    private void init(){
        // 注册录音器
        audioBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE_INHZ,CHANNEL_CONFIG,AUDIO_FORMAT);
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,SAMPLE_RATE_INHZ,CHANNEL_CONFIG,AUDIO_FORMAT,audioBufferSize);
        //获取按钮
        startRecordButton = findViewById(R.id.startRecord);
        stopRecordButton = findViewById(R.id.stopRecord);
        //注册监听器
        startRecordButton.setOnClickListener(startRecordListener);
        stopRecordButton.setOnClickListener(stopRecordListener);
    }

    //监听器
    View.OnClickListener startRecordListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            mAudioRecord.startRecording();
            isRecording = true;
            speechBuffer = new byte[1024 * 1024 * 5];
            speechBufferOffset = 0;
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    byte [] tempBuffer = new byte[audioBufferSize];
                    while (isRecording){
                        int len = mAudioRecord.read(tempBuffer,0,audioBufferSize);
                        System.arraycopy(tempBuffer,0,speechBuffer,speechBufferOffset,len);
                        speechBufferOffset += len;
                    }
                }
            });
            t.start();
            Toast.makeText(MainActivity.this,"开始录音",Toast.LENGTH_LONG).show();
        }
    };
    View.OnClickListener stopRecordListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            isRecording = false;
            mAudioRecord.stop();
            Toast.makeText(MainActivity.this,"停止录音",Toast.LENGTH_LONG).show();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    AipSpeech client = new AipSpeech(APP_ID,API_KEY,SECRET_KEY);
                    // 可选：设置网络连接参数
                    client.setConnectionTimeoutInMillis(10000);
                    client.setSocketTimeoutInMillis(60000);
                    byte [] temp = new byte[speechBufferOffset];
                    System.arraycopy(speechBuffer,0,temp,0,speechBufferOffset);
                    speechResult = client.asr(temp,"pcm",16000,null);
                    System.out.println(temp);
                    Log.i(TAG + ":识别结果",speechResult.toString());
                }
            }).start();
            Log.i(TAG, String.valueOf(speechBufferOffset));
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAudioRecord.release();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }
}

