package com.whu.kelfor.wisar_whu_android;


import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.TextureView.SurfaceTextureListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;

import android.widget.ImageView;
import android.widget.ListView;

import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStreamReader;

import java.io.PrintWriter;
import java.net.Socket;

import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SystemState;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.product.Model;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;


import dji.sdk.flightcontroller.FlightController;
import dji.sdk.flightcontroller.FlightController.OnboardSDKDeviceDataCallback;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.thirdparty.okio.ByteString;

public class MainActivity extends Activity implements SurfaceTextureListener,OnClickListener{

    private static final String TAG = MainActivity.class.getName();
    protected VideoFeeder.VideoDataCallback mReceivedVideoDataCallBack = null;

    // Codec for video live view
    protected DJICodecManager mCodecManager = null;


    //Add for WiSAR Demo
    private double scale;
    private final int DELAY_TIME = 200;
    private final String START = "q";
    private final String END = "d";
    private final String STARTRECORD = "r";
    private final String STOPRECORD = "t";
    private final String ABORT= "a";
    private final String TEAM_NAME = "WiSAR-WHU";

    private PrintWriter out_coord;
    private PrintWriter out_cmd;
    private BufferedReader br;
    private Socket socket_coordTrans=null;
    private Socket socket_cmd = null;


    /* widget */
    private EditText server;
    private ListView listView;
    private DrawView drawView;
    private TextView textView;
    private TextView velInfo;
    private TextView heightText;
    private TextView batteryText;
    private ImageView preImageView;
    private TextureView videoSurface;

    private  TextView targetGPS;
    private EditText targetGPS_long;
    private EditText targetGPS_latt;



   protected TextureView mVideoSurface = null;
//    private Button mCaptureBtn, mShootPhotoModeBtn, mRecordVideoModeBtn;
//    private ToggleButton mRecordBtn;
//    private TextView recordingTime;


    private Handler handler;


     /*以下为WiSAR任务变量*/
     private FlightController mFlightController = null;
     private Aircraft mAircraft  = null;
    private Socket socket = null;
    private PrintWriter out = null;
    private BufferedReader br= null;
    private Button mInitBtn, mStartMissionBtn, mAbortMissionBtn, mReturnBtn,mLanding;

    private EditText server = null;


   // private DJIFlightController_setReceiveExternalDeviceDataCallback;FlightControllerReceivedDataFromExternalDeviceCallback
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler()
        {
            @Override
            public void handleMessage(Message msg)
            {
                switch (msg.what) {
                    case 0:
//                        historyAdapter.notifyDataSetChanged();
//                        Bitmap tag = videoSurface.getBitmap();
//                        preImageView.setImageBitmap(tag);
//                        detectedImagesList.add(tag);
                        break;
                    case 1:
//                        int position = (int) msg.obj;
//                        historyAdapter.setSelectItem(position);
//                        historyAdapter.notifyDataSetChanged();
//                        preImageView.setImageBitmap(detectedImagesList.get(position));
                        break;
                    case 0x88:
                        //TODO: 添加处理Qt 服务器端回传的数据
                        String str = msg.obj.toString();
                        showToast(str);
                        break;
                }
            }


        };

        initUI();


        mAircraft =  (Aircraft) DJISDKManager.getInstance().getProduct();
        // The callback for receiving the raw H264 video data for camera live view
        mReceivedVideoDataCallBack = new VideoFeeder.VideoDataCallback() {

            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if (mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }
            }
        };
//
//        Camera camera = WiSARApplication.getCameraInstance();
//
//        if (camera != null) {
//
//            camera.setSystemStateCallback(new SystemState.Callback() {
//                @Override
//                public void onUpdate(SystemState cameraSystemState) {
//                    if (null != cameraSystemState) {
//
//                        int recordTime = cameraSystemState.getCurrentVideoRecordingTimeInSeconds();
//                        int minutes = (recordTime % 3600) / 60;
//                        int seconds = recordTime % 60;
//
//                        final String timeString = String.format("%02d:%02d", minutes, seconds);
//                        final boolean isVideoRecording = cameraSystemState.isRecording();
//
//                        MainActivity.this.runOnUiThread(new Runnable() {
//
//                            @Override
//                            public void run() {
//
//                                recordingTime.setText(timeString);
//
//                                /*
//                                 * Update recordingTime TextView visibility and mRecordBtn's check state
//                                 */
//                                if (isVideoRecording){
//                                    recordingTime.setVisibility(View.VISIBLE);
//                                }else
//                                {
//                                    recordingTime.setVisibility(View.INVISIBLE);
//                                }
//                            }
//                        });
//                    }
//                }
//            });
//
//        }

    }

//    private CommonCallbacks.CompletionCallback djiCompletionCallback = new CommonCallbacks.CompletionCallback()
//    {
//
//        @Override
//        public void onResult(DJIError djiError) {
//            if (djiError != null) {
//                showToast(djiError.getDescription());
//            }
//        }
//    };

     private OnboardSDKDeviceDataCallback recvCallback = new OnboardSDKDeviceDataCallback() {
         @Override
         public void onReceive(byte[] bytes) {
             //TODO: 添加数据透传接收;
         }
     };

     protected void onProductChange() {

        initPreviewer();
    }

    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
        initPreviewer();
        onProductChange();

        if(mVideoSurface == null) {
            Log.e(TAG, "mVideoSurface is null");
        }
    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        uninitPreviewer();
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.e(TAG, "onStop");
        super.onStop();
    }

    public void onReturn(View view){
        Log.e(TAG, "onReturn");
        this.finish();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        uninitPreviewer();
        super.onDestroy();
    }

    private void initUI() {
        // init mVideoSurface
        mVideoSurface = (TextureView)findViewById(R.id.video_previewer_surface);
//
//        recordingTime = (TextView) findViewById(R.id.timer);
//        mCaptureBtn = (Button) findViewById(R.id.btn_capture);
//        mRecordBtn = (ToggleButton) findViewById(R.id.btn_record);
//        mShootPhotoModeBtn = (Button) findViewById(R.id.btn_shoot_photo_mode);
//        mRecordVideoModeBtn = (Button) findViewById(R.id.btn_record_video_mode);

        /*添加WiSAR任务按键*/
        mStartMissionBtn = (Button)findViewById(R.id.btn_startMission);
        mAbortMissionBtn = (Button)findViewById(R.id.btn_abortMission);
        mReturnBtn = (Button)findViewById(R.id.btn_return);
        mInitBtn = (Button)findViewById(R.id.btn_init);
        mLanding = (Button)findViewById(R.id.btn_land) ;
        server = (EditText)findViewById(R.id.server_ip);


        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }

        mCaptureBtn.setOnClickListener(this);
        mRecordBtn.setOnClickListener(this);
        mShootPhotoModeBtn.setOnClickListener(this);
        mRecordVideoModeBtn.setOnClickListener(this);


        mInitBtn.setOnClickListener(this);
        mStartMissionBtn.setOnClickListener(this);
        mAbortMissionBtn.setOnClickListener(this);
        mReturnBtn.setOnClickListener(this);
        mLanding.setOnClickListener(this);




        recordingTime.setVisibility(View.INVISIBLE);

        mRecordBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //TODO:更改为数据透传发送指令录像
                if (isChecked) {
                    startRecord();
                } else {
                    stopRecord();
                }
            }
        });

    }

    private void initPreviewer() {

        BaseProduct product = WiSARApplication.getProductInstance();

        if (product == null || !product.isConnected()) {
            showToast(getString(R.string.disconnected));
        } else {
            if (null != mVideoSurface) {
                mVideoSurface.setSurfaceTextureListener(this);
            }
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                if (VideoFeeder.getInstance().getVideoFeeds() != null
                        && VideoFeeder.getInstance().getVideoFeeds().size() > 0) {
                    VideoFeeder.getInstance().getVideoFeeds().get(0).setCallback(mReceivedVideoDataCallBack);
                }
            }
        }
    }

    private void uninitPreviewer() {
        Camera camera = WiSARApplication.getCameraInstance();
        if (camera != null){
            // Reset the callback
            VideoFeeder.getInstance().getVideoFeeds().get(0).setCallback(null);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureAvailable");
        if (mCodecManager == null) {
            mCodecManager = new DJICodecManager(this, surface, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureSizeChanged");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.e(TAG,"onSurfaceTextureDestroyed");
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }

        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View v) {//TODO: 消息响应函数
/*
        switch (v.getId()) {
            case R.id.btn_capture:{
//                captureAction();//TODO:更改
                break;
            }
            case R.id.btn_shoot_photo_mode:{
//                switchCameraMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO);
                break;
            }
            case R.id.btn_record_video_mode:{
//                switchCameraMode(SettingsDefinitions.CameraMode.RECORD_VIDEO);
                break;
            }
            case R.id.btn_init:{
                //TODO: 完善初始化
                initController();

                break;

            }
            case R.id.btn_startMission:{
                //TODO:完善开始任务命令
                startMission();
                break;
            }
            case R.id.btn_abortMission:{
                //TODO:添加中止任务
                break;

            }
            case R.id.btn_return:{
                //TODO:添加返航
                break;
            }
            case R.id.btn_land:{
                //TODO:完善直接降落
                land();
                break;
            }
            default:
                break;
        }
        */
    }


    private Thread tcpThread = new Thread()
    {
        @Override
        public void run()
        {
//TODO:处理来自Qt服务器端的数据

            while (socket.isConnected()) {
                String str = null;
                try {
                    str = br.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(str!=null)
                {
                    Message msg = new Message();
                    msg.obj  = str;
                    msg.what = 0x88;
                    handler.sendMessage(msg);

                }
            }
        }
    };

    private void initController() {

        try {
            mFlightController = mAircraft.getFlightController();//.getAircraftInstance().getFlightController();
            mFlightController.setOnboardSDKDeviceDataCallback(recvCallback);
            showToast("获取控制权成功");
        } catch (NullPointerException e) {
            showToast("获取控制权失败");
        }

        /*建立TCP连接*/
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String ip = server.getText().toString().trim();

                    socket = new Socket(ip,9527);
                    out = new PrintWriter( socket.getOutputStream());
                    //将Socket对应的输入流包装成BufferedReader
                    br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    tcpThread.start();

                } catch (Exception e) {
                }
            }
        }).start();
    }

    private void land() {
            if (mFlightController != null) {
                FlightControllerState currentState = mFlightController.getState();
                if (currentState.isFlying()) {
                    mFlightController.startLanding(new CommonCallbacks.CompletionCallback(){

                        @Override
                        public void onResult(DJIError djiError)
                        {
                            if(djiError == null) {
                                showToast("开始降落");
                            }else {
                                showToast(djiError.getDescription());
                            }
                        }
                    });
                } else {
                    showToast("未起飞");
                }
            } else {
                showToast("未获取控制权");
            }
    }

    private void startMission() {
        if (mFlightController != null) {
            mFlightController.turnOnMotors(new CommonCallbacks.CompletionCallback(){

                @Override
                public void onResult(DJIError djiError)
                {
                    if(djiError == null) {
                        showToast("准备起飞");
                    }else {
                        showToast(djiError.getDescription());
                    }
                }
            });
            try {
                Thread.currentThread();//延时等待电机转稳定
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                showToast("sleep failed...");
            }
            if (mFlightController.isOnboardSDKDeviceAvailable()) {
                //TODO: 添加发送的指令
                String cmd = "起飞";
                mFlightController.sendDataToOnboardSDKDevice(cmd.getBytes(), new CommonCallbacks.CompletionCallback(){

                    @Override
                    public void onResult(DJIError djiError)
                    {
                        if(djiError == null) {
                            showToast("发送至Onboard SDK成功");
                        }else {
                            showToast(djiError.getDescription());
                        }
                    }
                });
                showToast("开始任务");
            } else {
                showToast("不支持 Onboardd SDK");
            }
        } else {
            showToast("未获取控制权");
        }
    }

    private void switchCameraMode(SettingsDefinitions.CameraMode cameraMode){

        Camera camera = WiSARApplication.getCameraInstance();
        if (camera != null) {
            camera.setMode(cameraMode, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {

                    if (error == null) {
                        showToast("Switch Camera Mode Succeeded");
                    } else {
                        showToast(error.getDescription());
                    }
                }
            });
        }
    }

    // Method for taking photo
//    private void captureAction(){
//
//        final Camera camera = WiSARApplication.getCameraInstance();
//        if (camera != null) {
//
//            SettingsDefinitions.ShootPhotoMode photoMode = SettingsDefinitions.ShootPhotoMode.SINGLE; // Set the camera capture mode as Single mode
//            camera.setShootPhotoMode(photoMode, new CommonCallbacks.CompletionCallback(){
//                @Override
//                public void onResult(DJIError djiError) {
//                    if (null == djiError) {
//                        handler.postDelayed(new Runnable() {
//                            @Override
//                            public void run() {
//                                camera.startShootPhoto(new CommonCallbacks.CompletionCallback() {
//                                    @Override
//                                    public void onResult(DJIError djiError) {
//                                        if (djiError == null) {
//                                            showToast("take photo: success");
//                                        } else {
//                                            showToast(djiError.getDescription());
//                                        }
//                                    }
//                                });
//                            }
//                        }, 2000);
//                    }
//                }
//            });
//        }
//    }

//    // Method for starting recording
//    private void startRecord(){
//
//        final Camera camera = WiSARApplication.getCameraInstance();
//        if (camera != null) {
//            camera.startRecordVideo(new CommonCallbacks.CompletionCallback(){
//                @Override
//                public void onResult(DJIError djiError)
//                {
//                    if (djiError == null) {
//                        showToast("Record video: success");
//                    }else {
//                        showToast(djiError.getDescription());
//                    }
//                }
//            }); // Execute the startRecordVideo API
//        }
//    }
//
//    // Method for stopping recording
//    private void stopRecord(){
//
//        Camera camera = WiSARApplication.getCameraInstance();
//        if (camera != null) {
//            camera.stopRecordVideo(new CommonCallbacks.CompletionCallback(){
//
//                @Override
//                public void onResult(DJIError djiError)
//                {
//                    if(djiError == null) {
//                        showToast("Stop recording: success");
//                    }else {
//                        showToast(djiError.getDescription());
//                    }
//                }
//            }); // Execute the stopRecordVideo API
//        }
//
//    }
}
