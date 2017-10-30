package com.jiangdg.usbcamera.view;

import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.jiangdg.usbcamera.FileUtils;
import com.jiangdg.usbcamera.R;
import com.jiangdg.usbcamera.USBCameraManager;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.common.AbstractUVCCameraHandler;
import com.serenegiant.usb.encoder.RecordParams;
import com.serenegiant.usb.widget.CameraViewInterface;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

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
    @BindView(R.id.btn_update_resolution)
    public Button mBtnUpdateResultion;

    private USBCameraManager mUSBManager;
    private CameraViewInterface mUVCCameraView;

    private boolean isRequest;

    /**
     * USB设备事件监听器
     * */
    private USBCameraManager.OnMyDevConnectListener listener = new USBCameraManager.OnMyDevConnectListener() {
        // 插入USB设备
        @Override
        public void onAttachDev(UsbDevice device) {
            if(mUSBManager == null || mUSBManager.getUsbDeviceCount() == 0){
                showShortMsg("未检测到USB摄像头设备");
                return;
            }
            // 请求打开摄像头
            if(! isRequest){
                isRequest = true;
                if(mUSBManager != null){
                    mUSBManager.requestPermission(0);
                }
            }
        }

        // 拔出USB设备
        @Override
        public void onDettachDev(UsbDevice device) {
            if(isRequest){
                // 关闭摄像头
                isRequest = false;
                mUSBManager.closeCamera();
                showShortMsg(device.getDeviceName()+"已拨出");
            }
        }

        // 连接USB设备成功
        @Override
        public void onConnectDev(UsbDevice device,boolean isConnected) {
            if(! isConnected) {
                showShortMsg("连接失败，请检查分辨率参数是否正确");
            }
            showShortMsg("连接成功");
        }

        // 与USB设备断开连接
        @Override
        public void onDisConnectDev(UsbDevice device) {
            showShortMsg("连接失败");
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
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 注销USB事件广播监听器
        if(mUSBManager != null){
            mUSBManager.unregisterUSB();
        }
    }

    @OnClick({R.id.camera_view, R.id.btn_capture_pic, R.id.btn_rec_video,R.id.btn_update_resolution})
    public void onViewClick(View view) {
        int vId = view.getId();
        switch (vId) {
            // 切换分辨率
            case R.id.btn_update_resolution:
                if(mUSBManager == null)
                    return;
                mUSBManager.updateResolution(this, mUVCCameraView, 320, 240, new USBCameraManager.OnPreviewListener() {
                    @Override
                    public void onPreviewResult(boolean isSuccess) {
                        if(! isSuccess) {
                            showShortMsg("预览失败，不支持该分辨率");
                        }else {
                            showShortMsg("以切换到分辨率为320x240");
                        }
                    }
                });
                break;
            // 点击后自动对焦
            case R.id.camera_view:
                if(mUSBManager == null)
                    return;
//                mUSBManager.startCameraFoucs();
//                showShortMsg("对焦相机");
                List<Size> list = mUSBManager.getSupportedPreviewSizes();
                StringBuilder sb = new StringBuilder();
                for(Size size:list){
                    sb.append(size.width+"x"+size.height);
                    sb.append("\n");
                }
                showShortMsg(sb.toString());
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
                    String videoPath = USBCameraManager.ROOT_PATH+System.currentTimeMillis();
                    FileUtils.createfile(FileUtils.ROOT_PATH+"test666.h264");
                    RecordParams params = new RecordParams();
                    params.setRecordPath(videoPath);
                    params.setRecordDuration(0);    // 设置为0，不分割保存
                    params.setVoiceClose(false);    // 不屏蔽声音
                    mUSBManager.startRecording(params, new AbstractUVCCameraHandler.OnEncodeResultListener() {
                        @Override
                        public void onEncodeResult(byte[] data, int offset, int length, long timestamp, int type) {
                            // type = 0,aac格式音频流
                            // type = 1,h264格式视频流
                            if(type == 1){
                                FileUtils.putFileStream(data,offset,length);
                            }
                        }
                    });

                    mBtnRecord.setText("正在录制");
                } else {
                    FileUtils.releaseFile();
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
