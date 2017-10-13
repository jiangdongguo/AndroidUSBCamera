package com.jiangdg.usbcamera;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Environment;

import com.jiangdg.libusbcamera.R;
import com.serenegiant.usb.DeviceFilter;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.common.AbstractUVCCameraHandler;
import com.serenegiant.usb.common.UVCCameraHandler;
import com.serenegiant.usb.widget.CameraViewInterface;

import java.io.File;
import java.util.List;

import static android.R.attr.filter;

/**USB摄像头工具类
 *
 * Created by jiangdongguo on 2017/9/30.
 */

public class USBCameraManager{
    public static final String ROOT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
            + File.separator;
    public static final String SUFFIX_PNG = ".png";
    public static final String SUFFIX_MP4 = ".mp4";
    private static final String TAG = "USBCameraManager";
    private static final int PREVIEW_WIDTH = 640;
    private static final int PREVIEW_HEIGHT = 480;
    private static final int ENCODER_TYPE = 1;
    //0为YUYV，1为MJPEG
    private static final int PREVIEW_FORMAT = 1;

    private static USBCameraManager mUsbCamManager;
    // USB设备管理类
    private USBMonitor mUSBMonitor;
    // Camera业务逻辑处理
    private UVCCameraHandler mCameraHandler;

    private Context mContext;

    private USBCameraManager(){}

    public static USBCameraManager getInstance(){
        if(mUsbCamManager == null){
            mUsbCamManager = new USBCameraManager();
        }
        return mUsbCamManager;
    }

    public interface OnMyDevConnectListener{
        void onAttachDev(UsbDevice device);
        void onDettachDev(UsbDevice device);
        void onConnectDev(UsbDevice device);
        void onDisConnectDev(UsbDevice device);
    }

    /** 初始化
     *
     *  context  上下文
     *  cameraView Camera要渲染的Surface
     *  listener USB设备检测与连接状态事件监听器
     * */
    public void init(Activity activity, final CameraViewInterface cameraView, final OnMyDevConnectListener listener){
        if(cameraView == null)
            throw new NullPointerException("CameraViewInterface cannot be null!");
        mContext = activity.getApplicationContext();

        mUSBMonitor = new USBMonitor(activity.getApplicationContext(), new USBMonitor.OnDeviceConnectListener() {
            // 当检测到USB设备，被回调
            @Override
            public void onAttach(UsbDevice device) {
                if(listener != null){
                    listener.onAttachDev(device);
                }
            }

            // 当拨出或未检测到USB设备，被回调
            @Override
            public void onDettach(UsbDevice device) {
                if(listener != null){
                    listener.onDettachDev(device);
                }
            }

            // 当连接到USB Camera时，被回调
            @Override
            public void onConnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
                if(listener != null){
                    listener.onConnectDev(device);
                }
                // 打开摄像头
                openCamera(ctrlBlock);
                // 开启预览
                startPreview(cameraView);
            }

            // 当与USB Camera断开连接时，被回调
            @Override
            public void onDisconnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
                if(listener != null){
                    listener.onDisConnectDev(device);
                }
                // 关闭摄像头
                closeCamera();
            }

            @Override
            public void onCancel(UsbDevice device) {
            }
        });

        // 设置长宽比
        cameraView.setAspectRatio(PREVIEW_WIDTH / (float)PREVIEW_HEIGHT);
        mCameraHandler = UVCCameraHandler.createHandler(activity,cameraView,ENCODER_TYPE,
                PREVIEW_WIDTH,PREVIEW_HEIGHT,PREVIEW_FORMAT);
    }

    /**
     * 注册检测USB设备广播接收器
     * */
    public void registerUSB(){
        if(mUSBMonitor != null){
            mUSBMonitor.register();
        }
    }

    /**
     *  注销检测USB设备广播接收器
     */
    public void unregisterUSB(){
        if(mUSBMonitor != null){
            mUSBMonitor.unregister();
        }
    }

    /**
     *  请求开启第index USB摄像头
     */
    public void requestPermission(int index){
        List<UsbDevice> devList = getUsbDeviceList();
        if(devList==null || devList.size() ==0){
            return;
        }
        int count = devList.size();
        if(index >= count)
            new IllegalArgumentException("index illegal,should be < devList.size()");
        if(mUSBMonitor != null) {
            mUSBMonitor.requestPermission(getUsbDeviceList().get(index));
        }
    }

    /**
     * 返回
     * */
    public int getUsbDeviceCount(){
        List<UsbDevice> devList = getUsbDeviceList();
        if(devList==null || devList.size() ==0){
            return 0;
        }
        return devList.size();
    }

    private List<UsbDevice> getUsbDeviceList(){
        List<DeviceFilter> deviceFilters = DeviceFilter.getDeviceFilters(mContext, R.xml.device_filter);
        if(mUSBMonitor == null || deviceFilters == null)
            return null;
        return mUSBMonitor.getDeviceList(deviceFilters.get(0));
    }

    /**
     * 抓拍照片
     * */
    public void capturePicture(String savePath){
        if(mCameraHandler != null && mCameraHandler.isOpened()){
            mCameraHandler.captureStill(savePath);
        }
    }

    public void startRecording(String videoPath, AbstractUVCCameraHandler.OnEncodeResultListener listener){
        if(mCameraHandler != null && ! isRecording()){
            mCameraHandler.startRecording(videoPath,listener);
        }
    }

    public void stopRecording(){
        if(mCameraHandler != null && isRecording()){
            mCameraHandler.stopRecording();
        }
    }

    public boolean isRecording(){
        if(mCameraHandler != null){
            return mCameraHandler.isRecording();
        }
        return false;
    }

    public boolean isCameraOpened(){
        if(mCameraHandler != null){
            return mCameraHandler.isOpened();
        }
        return false;
    }



    /**
     * 释放资源
     * */
    public void release(){
        // 关闭摄像头
        closeCamera();
        //释放CameraHandler占用的相关资源
        if(mCameraHandler != null){
            mCameraHandler.release();
            mCameraHandler = null;
        }
        // 释放USBMonitor占用的相关资源
        if(mUSBMonitor != null){
            mUSBMonitor.destroy();
            mUSBMonitor = null;
        }
    }

    public USBMonitor getUSBMonitor() {
        return mUSBMonitor;
    }


    public void closeCamera() {
        if(mCameraHandler != null){
            mCameraHandler.close();
        }
    }

    private void openCamera(USBMonitor.UsbControlBlock ctrlBlock) {
        if(mCameraHandler != null){
            mCameraHandler.open(ctrlBlock);
        }
    }

    public void startPreview(CameraViewInterface cameraView) {
        SurfaceTexture st = cameraView.getSurfaceTexture();
        if(mCameraHandler != null){
            mCameraHandler.startPreview(st);
        }
    }
}
