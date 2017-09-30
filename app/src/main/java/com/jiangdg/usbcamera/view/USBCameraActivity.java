package com.jiangdg.usbcamera.view;

import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.jiangdg.usbcamera.R;
import com.jiangdg.usbcamera.USBCameraManager;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.widget.CameraViewInterface;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * AndroidUSBCamera引擎使用Demo
 *
 * Created by jiangdongguo on 2017/9/30.
 */

public class USBCameraActivity extends AppCompatActivity implements CameraDialog.CameraDialogParent{
    @BindView(R.id.camera_view)
    public View mTextureView;
    @BindView(R.id.btn_capture_pic)
    public Button mBtnCapture;
    @BindView(R.id.btn_rec_video)
    public Button mBtnRecord;

    private USBCameraManager mUSBManager;
    private CameraViewInterface mUVCCameraView;

    // USB设备监听器
    private USBCameraManager.OnMyDevConnectListener listener = new USBCameraManager.OnMyDevConnectListener() {
        @Override
        public void onAttachDev(UsbDevice device) {
            showShortMsg("检测到设备："+device.getDeviceName());
        }

        @Override
        public void onDettachDev(UsbDevice device) {
            showShortMsg(device.getDeviceName()+"已拨出");
        }

        @Override
        public void onConnectDev(UsbDevice device) {
            // 处理连接到设备后的逻辑
        }

        @Override
        public void onDisConnectDev(UsbDevice device) {
            // 处理与设备断开后的逻辑
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usbcamera);
        ButterKnife.bind(this);
        // 初始化引擎
        mUSBManager = USBCameraManager.getInstance();
        mUVCCameraView = (CameraViewInterface) mTextureView;
        mUSBManager.init(this, mUVCCameraView, listener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // 注册USB事件广播监听器
        if(mUSBManager != null){
            mUSBManager.registerUSB();
        }
        // 恢复Camera预览
        if(mUVCCameraView != null){
            mUVCCameraView.onResume();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 注销USB事件广播监听器
        if(mUSBManager != null){
            mUSBManager.unregisterUSB();
        }
        // 暂停Camera预览
        if(mUVCCameraView != null){
            mUVCCameraView.onPause();
        }
    }

    @OnClick({R.id.camera_view, R.id.btn_capture_pic, R.id.btn_rec_video})
    public void onViewClick(View view) {
        int vId = view.getId();
        switch (vId) {
            // 开启或关闭Camera
            case R.id.camera_view:
                if(mUSBManager != null){
                    boolean isOpened = mUSBManager.isCameraOpened();
                    if(! isOpened){
                        CameraDialog.showDialog(USBCameraActivity.this);
                    }else {
                        mUSBManager.closeCamera();
                    }
                }

                break;
            case R.id.btn_capture_pic:
                if(mUSBManager == null || ! mUSBManager.isCameraOpened()){
                    showShortMsg("抓拍异常，摄像头未开启");
                    return;
                }
                String picPath = USBCameraManager.ROOT_PATH+System.currentTimeMillis()
                        +USBCameraManager.SUFFIX_PNG;
                mUSBManager.capturePicture(picPath);

                showShortMsg("保存路径："+picPath);
                break;
            case R.id.btn_rec_video:
                if(mUSBManager == null || ! mUSBManager.isCameraOpened()){
                    showShortMsg("录制异常，摄像头未开启");
                    return;
                }
                if(! mUSBManager.isRecording()){
                    String videoPath = USBCameraManager.ROOT_PATH+System.currentTimeMillis()
                            +USBCameraManager.SUFFIX_MP4;
                    mUSBManager.startRecording(videoPath);

                    mBtnRecord.setText("正在录制");
                } else {
                    mUSBManager.stopRecording();

                    mBtnRecord.setText("开始录制");
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 释放资源
        if(mUSBManager != null){
            mUSBManager.release();
        }
    }

    private void showShortMsg(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public USBMonitor getUSBMonitor() {
        return mUSBManager.getUSBMonitor();
    }

    @Override
    public void onDialogResult(boolean canceled) {
        if(canceled){
            showShortMsg("取消操作");
        }
    }
}
