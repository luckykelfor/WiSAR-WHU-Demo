package com.whu.kelfor.wisar_whu_android;


import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.IntRange;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.TextureView.SurfaceTextureListener;
import android.view.ViewGroup;
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
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.Socket;

import dji.common.battery.BatteryState;
import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SystemState;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.product.Model;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.battery.Battery;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;


import dji.sdk.flightcontroller.FlightController;
import dji.sdk.flightcontroller.FlightController.OnboardSDKDeviceDataCallback;
import dji.sdk.gimbal.Gimbal;
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

    private DrawView drawView;
    private TextView textView;
    private TextView velInfo;
    private TextView heightText;
    private TextView batteryText;
    private ImageView preImageView;


    private  TextView targetGPS;
    private EditText targetGPS_long;
    private EditText targetGPS_latt;

    //按钮
    private Button  mStartMissionBtn,    mAbortMissionBtn ,    mReturnBtn ,    mInitBtn ,    mLanding;
    private ToggleButton mRecordBtn;

    //飞行控制相关
    private FlightController mFlightController = null;
    private Aircraft mAircraft  = null;

    private Battery mBattery = null;

   protected TextureView mVideoSurface = null;
//    private Button mCaptureBtn, mShootPhotoModeBtn, mRecordVideoModeBtn;
//    private ToggleButton mRecordBtn;
//    private TextView recordingTime;




     /*以下为WiSAR任务变量*/


    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
//                    historyAdapter.notifyDataSetChanged();
//                    Bitmap tag = videoSurface.getBitmap();
//                    preImageView.setImageBitmap(tag);
//                    detectedImagesList.add(tag);
                    break;
                case 1:
//                    int position = (int) msg.obj;
//                    historyAdapter.setSelectItem(position);
//                    historyAdapter.notifyDataSetChanged();
//                    preImageView.setImageBitmap(detectedImagesList.get(position));
                    break;
                case 0x88:
                    //TODO: 添加处理Qt 服务器端回传的数据
                    break;
                case 0x87:
                    //TODO: 显示目标GPS
                    String GPS = (String)msg.obj;

                    targetGPS_long.setText(GPS.substring(0,9));
                    targetGPS_latt.setText(GPS.substring(10));
                    break;
                case 0x89:
                    showToast((String)msg.obj);
                    break;
            }
        }
    };

   // private DJIFlightController_setReceiveExternalDeviceDataCallback;FlightControllerReceivedDataFromExternalDeviceCallback
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();


        mAircraft =  (Aircraft) DJISDKManager.getInstance().getProduct();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        ViewGroup.LayoutParams params = mVideoSurface.getLayoutParams();
//        params.width = displayMetrics.widthPixels ;
//        params.height = displayMetrics.heightPixels;
        scale = (double) params.width / 640;
        mVideoSurface.setLayoutParams(params);
        // The callback for receiving the raw H264 video data for camera live view

        mReceivedVideoDataCallBack = new VideoFeeder.VideoDataCallback() {

            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if (mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }
            }
        };

        handler.postDelayed(runnable, DELAY_TIME);
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
             try {
                 if(bytes.length>=46)//Get target ROI(Position) and its GPS info
                 {

                     String slux = new String(bytes, 0, 3, "ascii");
                     String sluy = new String(bytes, 3, 3, "ascii");
                     String srux = new String(bytes, 6, 3, "ascii");
                     String sruy = new String(bytes, 9, 3, "ascii");
                     String sldx = new String(bytes, 12, 3, "ascii");
                     String sldy = new String(bytes, 15, 3, "ascii");
                     String srdx = new String(bytes, 18, 3, "ascii");
                     String srdy = new String(bytes, 21, 3, "ascii");
                     String ssid = new String(bytes, 24, 3, "ascii");
                     String longitude = new String(bytes, 27, 10, "ascii");
                     String latitude = new String(bytes, 37, 9, "ascii");

                     int lux = (int) (Integer.valueOf(slux) * scale);
                     int luy = (int) (Integer.valueOf(sluy) * scale);
                     int rux = (int) (Integer.valueOf(srux) * scale);
                     int ruy = (int) (Integer.valueOf(sruy) * scale);
                     int ldx = (int) (Integer.valueOf(sldx) * scale);
                     int ldy = (int) (Integer.valueOf(sldy) * scale);
                     int rdx = (int) (Integer.valueOf(srdx) * scale);
                     int rdy = (int) (Integer.valueOf(srdy) * scale);
                     int id = Integer.valueOf(ssid);
                     String msg  = new String(bytes,0,46,"utf-8");//show.getText().toString();
//                   String msg = bytes.toString();
                     out_coord.print(msg);

                     out_coord.flush();//Very Important!
                     Message GPS_INFO = new Message();
                     GPS_INFO.what = 0x87;
                     GPS_INFO.obj = longitude+latitude;


                     handler.sendMessage(GPS_INFO);


//
//                        if (!map.containsKey(id)) {
//                            historyAdapter.add(new AprilTag(ssid, latitude, longitude));
//                            historyAdapter.setSelectItem(historyAdapter.getCount() - 1);
//                            listView.setSelection(historyAdapter.getCount() - 1);
//                            map.put(id, 1);
//
//                            new Thread() {
//                                @Override
//                                public void run() {
//                                    Message msg = new Message();
//                                    msg.what = 0;
//                                    handler.sendMessage(msg);
//                                }
//                            }.start();
//                        }

                     drawView.setXY(lux, luy, rux, ruy, ldx, ldy, rdx, rdy, id);
                     drawView.invalidate();
                 }
                 else if(bytes.length<=30)// Confirm the GPS (sent back from onboard)
                 {
                     Message msg = new Message();
                     msg.what = 0x89;
                     msg.obj = new String(bytes,"ascii");
//                            showToast(bytes.toString());
                     handler.sendMessage(msg);

                 }

             } catch (UnsupportedEncodingException e) {
                 Log.d(TAG, "Not ASCII string !");
             }
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

//        mShootPhotoModeBtn = (Button) findViewById(R.id.btn_shoot_photo_mode);
//        mRecordVideoModeBtn = (Button) findViewById(R.id.btn_record_video_mode);

        server = (EditText) findViewById(R.id.server_ip);
//        listView = (ListView) findViewById(R.id.historyList);
        drawView = (DrawView) findViewById(R.id.drawView);
        textView = (TextView) findViewById(R.id.stateView);
        velInfo = (TextView) findViewById(R.id.velInfo);
        heightText = (TextView) findViewById(R.id.heightView);
        batteryText = (TextView) findViewById(R.id.batteryView);
//        preImageView = (ImageView) findViewById(R.id.pre_tag);


        targetGPS = (TextView)findViewById(R.id.targetGPS);
        targetGPS_long = (EditText)findViewById(R.id.targetLong) ;
        targetGPS_latt = (EditText)findViewById(R.id.targetLat);
        server.setText("192.168.191.1");
        /*添加WiSAR任务按键*/
        mStartMissionBtn = (Button)findViewById(R.id.btn_startMission);
//        mAbortMissionBtn = (Button)findViewById(R.id.btn_abortMission);//TODO: 中止任务添加
        mReturnBtn = (Button)findViewById(R.id.btn_return);
        mInitBtn = (Button)findViewById(R.id.btn_init);
        mLanding = (Button)findViewById(R.id.btn_land) ;
        server = (EditText)findViewById(R.id.server_ip);
        mRecordBtn = (ToggleButton) findViewById(R.id.btn_record);

        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }




        mInitBtn.setOnClickListener(this);
        mStartMissionBtn.setOnClickListener(this);
        mAbortMissionBtn.setOnClickListener(this);
        mReturnBtn.setOnClickListener(this);
        mLanding.setOnClickListener(this);



//TODO:添加录像时间
//        recordingTime.setVisibility(View.INVISIBLE);

        mRecordBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    startRecord();
                }
                else {
                    stopRecord();
                }
            }
        });

    }

    private void initPreviewer() {

        BaseProduct product = WiSARApplication.getProductInstance();


        if (product == null || !product.isConnected()) {
            showToast(getString(R.string.disconnected));
            textView.setText(getString(R.string.disconnected));
        }
        else
        {
            if (null != mVideoSurface) {
                mVideoSurface.setSurfaceTextureListener(this);
            }
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                if (VideoFeeder.getInstance().getVideoFeeds() != null
                        && VideoFeeder.getInstance().getVideoFeeds().size() > 0) {
                    VideoFeeder.getInstance().getVideoFeeds().get(0).setCallback(mReceivedVideoDataCallBack);
                }
            }
            mBattery = product.getBattery();//TODO: 添加电池电量回调
            textView.setText(product.getModel() + " " + getString(R.string.connected));
//            mBattery.setStateCallback((BatteryState.Callback) batteryCallbck);
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
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.btn_init:{
                initController();
                break;

            }
            case R.id.btn_startMission:{
                startMission();
                break;
            }
//            case R.id.btn_abortMission:{
//                //TODO:添加中止任务
//                abortMission();
//                break;
//
//            }
            case R.id.btn_return:{
                endMission();
                break;
            }
            case R.id.btn_land:{
                land();
                break;
            }
            default:
                break;
        }

    }




    private Thread tcpThread = new Thread()
    {
        @Override
        public void run()
        {
//TODO:处理来自Qt服务器端的数据


            while (socket_cmd.isConnected()) {
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
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (mFlightController != null) {
                FlightControllerState state = mFlightController.getState();
                heightText.setText(String.valueOf(state.getAircraftLocation().getAltitude()));
                double x = state.getVelocityX();
                double y = state.getVelocityY();
                double velocity = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
                BigDecimal bigDecimal = new BigDecimal(velocity);
                velocity = bigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                velInfo.setText(String.valueOf(velocity));
            } else {
                heightText.setText(getString(R.string.not_available));
            }
            handler.postDelayed(this, DELAY_TIME);
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
        //        /* connect to Qt server */
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String ip = server.getText().toString().trim();

                    socket_coordTrans = new Socket(ip,9527);

                    out_coord = new PrintWriter( socket_coordTrans.getOutputStream());
                    socket_cmd = new Socket(ip,9527);
                    out_cmd = new PrintWriter(socket_cmd.getOutputStream());
                    //将Socket对应的输入流包装成BufferedReader
                    br = new BufferedReader(new InputStreamReader(socket_coordTrans.getInputStream()));
                    tcpThread.start();

                } catch (Exception e) {
                }
            }
        }).start();
        sendSearchingGPS();



    }

    private void sendSearchingGPS() {
        if (mFlightController != null) {
            if (mFlightController.isOnboardSDKDeviceAvailable()) {
                String LONG = new String("LONG");
                String LATT = new String("LATT");
                String GPS = LONG.concat(targetGPS_long.getText().toString())+LATT.concat(targetGPS_latt.getText().toString());


                mFlightController.sendDataToOnboardSDKDevice(GPS.getBytes(),new CommonCallbacks.CompletionCallback(){

                    @Override
                    public void onResult(DJIError djiError)
                    {
                        if(djiError == null) {
                            showToast("发送GPS成功");
                        }else {
                            showToast(djiError.getDescription());
                        }
                    }
                });


            } else {
                showToast(getString(R.string.onboard_device_unavailable));
            }
        } else {
            showToast("未获取权限");
        }
    }
    private void endMission() {

        if (mFlightController != null) {
            if (mFlightController.isOnboardSDKDeviceAvailable()) {
                mFlightController.sendDataToOnboardSDKDevice(END.getBytes(), new CommonCallbacks.CompletionCallback(){

                    @Override
                    public void onResult(DJIError djiError)
                    {
                        if(djiError == null) {
                            showToast("结束任务");
                        }else {
                            showToast(djiError.getDescription());
                        }
                    }
                });
                showToast(getString(R.string.stop_mission));
            } else {
                showToast(getString(R.string.onboard_device_unavailable));
            }
        } else {
            showToast(getString(R.string.fc_null));
        }
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
                mFlightController.sendDataToOnboardSDKDevice(START.getBytes(), new CommonCallbacks.CompletionCallback(){

                    @Override
                    public void onResult(DJIError djiError)
                    {
                        if(djiError == null) {
                            showToast("起飞命令发送至Onboard SDK成功");
                        }else {
                            showToast(djiError.getDescription());
                        }
                    }
                });

            } else {
                showToast(getString(R.string.onboard_device_unavailable));
            }
        } else {
            showToast("未获取控制权");
        }
    }
//
//    private void switchCameraMode(SettingsDefinitions.CameraMode cameraMode){
//
//        Camera camera = WiSARApplication.getCameraInstance();
//        if (camera != null) {
//            camera.setMode(cameraMode, new CommonCallbacks.CompletionCallback() {
//                @Override
//                public void onResult(DJIError error) {
//
//                    if (error == null) {
//                        showToast("Switch Camera Mode Succeeded");
//                    } else {
//                        showToast(error.getDescription());
//                    }
//                }
//            });
//        }
//    }

    private void abortMission()
    {
        if (mFlightController != null) {
            if (mFlightController.isOnboardSDKDeviceAvailable()) {
                mFlightController.sendDataToOnboardSDKDevice(ABORT.getBytes(),  new CommonCallbacks.CompletionCallback(){

                    @Override
                    public void onResult(DJIError djiError)
                    {
                        if(djiError == null) {
                            showToast("中止命令发送至Onboard SDK成功");
                        }else {
                            showToast(djiError.getDescription());
                        }
                    }
                });
                showToast("中止任务");
            } else {
                showToast(getString(R.string.onboard_device_unavailable));
            }
        } else {
            showToast(getString(R.string.fc_null));
        }

    }
    private void startRecord()
    {
        if (mFlightController != null) {
            if (mFlightController.isOnboardSDKDeviceAvailable()) {
                mFlightController.sendDataToOnboardSDKDevice(STARTRECORD.getBytes(),  new CommonCallbacks.CompletionCallback(){

                    @Override
                    public void onResult(DJIError djiError)
                    {
                        if(djiError == null) {
                            showToast("录像命令发送至Onboard SDK成功");
                        }else {
                            showToast(djiError.getDescription());
                        }
                    }
                });
                if(socket_cmd.isConnected()) {
                    out_cmd.print("STARTRECORD");
                    out_cmd.flush();
                }
                else
                {
                    showToast("地面站未连接");
                }
                showToast("开始录像");
            } else {
                showToast(getString(R.string.onboard_device_unavailable));
            }
        } else {
            showToast("未获取权限");
        }
    }
    private void stopRecord()
    {
        if (mFlightController != null) {
            if (mFlightController.isOnboardSDKDeviceAvailable()) {
                mFlightController.sendDataToOnboardSDKDevice(STOPRECORD.getBytes(),  new CommonCallbacks.CompletionCallback(){

                    @Override
                    public void onResult(DJIError djiError)
                    {
                        if(djiError == null) {
                            showToast("停止录像命令发送至Onboard SDK成功");
                        }else {
                            showToast(djiError.getDescription());
                        }
                    }
                });
                showToast("停止录像");
                if(socket_cmd.isConnected()) {
                    out_cmd.print("STOPRECORD");
                    out_cmd.flush();
                }
                else
                {
                    showToast("地面站未连接");
                }
            } else {
                showToast(getString(R.string.onboard_device_unavailable));
            }
        } else {
            showToast("未获取权限");
        }
    }
}
