package com.jiangdg.usbcamera;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Environment;

import com.jiangdg.libusbcamera.R;
import com.serenegiant.usb.DeviceFilter;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usb.common.AbstractUVCCameraHandler;
import com.serenegiant.usb.common.UVCCameraHandler;
import com.serenegiant.usb.encoder.RecordParams;
import com.serenegiant.usb.widget.CameraViewInterface;

import java.io.File;
import java.util.List;

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
    private int previewWidth = 640;
    private int previewHeight = 480;
    public static int MODE_BRIGHTNESS = UVCCamera.PU_BRIGHTNESS;
    public static int MODE_CONTRAST = UVCCamera.PU_CONTRAST;
    // 使用MediaVideoBufferEncoder
    private static final int ENCODER_TYPE = 2;
    //0为YUYV，1为MJPEG
    private static final int PREVIEW_FORMAT = 0;

    private static USBCameraManager mUsbCamManager;
    // USB设备管理类
    private USBMonitor mUSBMonitor;
    // Camera业务逻辑处理
    private UVCCameraHandler mCameraHandler;
    // 上下文
    private Activity mActivity;

    private USBMonitor.UsbControlBlock mCtrlBlock;
    private CameraViewInterface cameraView;

    private USBCameraManager(){}

    public static USBCameraManager getInstance(){
        if(mUsbCamManager == null){
            mUsbCamManager = new USBCameraManager();
        }
        return mUsbCamManager;
    }

    public void closeCamera() {
        if(mCameraHandler != null){
            mCameraHandler.close();
        }
    }

    public interface OnMyDevConnectListener{
        void onAttachDev(UsbDevice device);
        void onDettachDev(UsbDevice device);

        void onConnectDev(UsbDevice device,boolean isConnected);
        void onDisConnectDev(UsbDevice device);
    }

    public interface OnPreviewListener{
        void onPreviewResult(boolean isSuccess);
    }

    /** 初始化
     *
     *  context  上下文
     *  cameraView Camera要渲染的Surface
     *  listener USB设备检测与连接状态事件监听器
     * */
    public void initUSBMonitor(Activity activity,final OnMyDevConnectListener listener){
        this.mActivity = activity;

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
            public void onConnect(final UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
                mCtrlBlock = ctrlBlock;
                // 打开摄像头
                openCamera(ctrlBlock);
                // 开启预览
                startPreview(cameraView, new AbstractUVCCameraHandler.OnPreViewResultListener() {
                    @Override
                    public void onPreviewResult(boolean isConnected) {
                        if(listener != null){
                            listener.onConnectDev(device,isConnected);
                        }
                    }
                });
            }

            // 当与USB Camera断开连接时，被回调
            @Override
            public void onDisconnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
                if(listener != null){
                    listener.onDisConnectDev(device);
                }
            }

            @Override
            public void onCancel(UsbDevice device) {
            }
        });
    }

    public void createUVCCamera(CameraViewInterface cameraView) {
        if(cameraView == null)
            throw new NullPointerException("CameraViewInterface cannot be null!");

        // 释放CameraHandler占用的相关资源
        if(mCameraHandler != null){
            mCameraHandler.release();
            mCameraHandler = null;
        }
        // 重新初始化mCameraHandler
        this.cameraView = cameraView;
        cameraView.setAspectRatio(previewWidth / (float)previewHeight);
        mCameraHandler = UVCCameraHandler.createHandler(mActivity,cameraView,ENCODER_TYPE,
                previewWidth,previewHeight,PREVIEW_FORMAT);
    }

    // 切换分辨率
    public void updateResolution(int width, int height, final OnPreviewListener mPreviewListener){
        // 如果分辨率无变化，则无需重启Camera
        if(previewWidth == width && previewHeight == height){
            return;
        }
        this.previewWidth = width;
        this.previewHeight = height;
        // 释放CameraHandler占用的相关资源
        if(mCameraHandler != null){
            mCameraHandler.release();
            mCameraHandler = null;
        }
        // 重新初始化mCameraHandler
        cameraView.setAspectRatio(previewWidth / (float)previewHeight);
        mCameraHandler = UVCCameraHandler.createHandler(mActivity,cameraView,ENCODER_TYPE,
                previewWidth,previewHeight,PREVIEW_FORMAT);
        openCamera(mCtrlBlock);
        // 开始预览
        startPreview(cameraView, new AbstractUVCCameraHandler.OnPreViewResultListener() {
            @Override
            public void onPreviewResult(boolean result) {
                if(mPreviewListener != null){
                    mPreviewListener.onPreviewResult(result);
                }
            }
        });
    }

    public void restartUSBCamera(CameraViewInterface cameraView,final OnPreviewListener mPreviewListener){
        if(mCtrlBlock == null || cameraView == null)
           throw new NullPointerException("mCtrlBlock or cameraView is null,please connected to camera");

        // 创建Camera管理线程
        createUVCCamera(cameraView);
        // 创建Camera
        openCamera(mCtrlBlock);
        // 开始预览
        startPreview(cameraView, new AbstractUVCCameraHandler.OnPreViewResultListener() {
            @Override
            public void onPreviewResult(boolean result) {
                if(mPreviewListener != null){
                    mPreviewListener.onPreviewResult(result);
                }
            }
        });
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

    public boolean checkSupportFlag(final int flag) {
        return mCameraHandler != null && mCameraHandler.checkSupportFlag(flag);
    }

    public int getModelValue(final int flag) {
        return mCameraHandler != null ? mCameraHandler.getValue(flag) : 0;
    }

    public int setModelValue(final int flag, final int value) {
        return mCameraHandler != null ? mCameraHandler.setValue(flag, value) : 0;
    }

    public int resetModelValue(final int flag) {
        return mCameraHandler != null ? mCameraHandler.resetValue(flag) : 0;
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

    // 返回USB设备列表数量
    public int getUsbDeviceCount(){
        List<UsbDevice> devList = getUsbDeviceList();
        if(devList==null || devList.size() ==0){
            return 0;
        }
        return devList.size();
    }

    // 返回USB设备列表
    public List<UsbDevice> getUsbDeviceList(){
        List<DeviceFilter> deviceFilters = DeviceFilter.getDeviceFilters(mActivity.getApplicationContext(), R.xml.device_filter);
        if(mUSBMonitor == null || deviceFilters == null)
            return null;
        return mUSBMonitor.getDeviceList(deviceFilters.get(0));
    }

    // 拍照
    public void capturePicture(String savePath,AbstractUVCCameraHandler.OnCaptureListener listener){
        if(mCameraHandler != null && mCameraHandler.isOpened()){
            mCameraHandler.captureStill(savePath,listener);
        }
    }

    // 开始录像
    public void startRecording(RecordParams params, AbstractUVCCameraHandler.OnEncodeResultListener listener){
        if(mCameraHandler != null && ! isRecording()){
            mCameraHandler.startRecording(params,listener);
        }
    }

    // 停止录像
    public void stopRecording(){
        if(mCameraHandler != null && isRecording()){
            mCameraHandler.stopRecording();
        }
    }

    // 是否正在录像
    public boolean isRecording(){
        if(mCameraHandler != null){
            return mCameraHandler.isRecording();
        }
        return false;
    }

    // 是否打开Camera
    public boolean isCameraOpened(){
        if(mCameraHandler != null){
            return mCameraHandler.isOpened();
        }
        return false;
    }

    // 释放资源
    public void release(){
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


    // 打开Camera
    private void openCamera(USBMonitor.UsbControlBlock ctrlBlock) {
        if(mCameraHandler != null){
            mCameraHandler.open(ctrlBlock);
        }
    }

    // 开始预览
    public void startPreview(CameraViewInterface cameraView,AbstractUVCCameraHandler.OnPreViewResultListener mPreviewListener) {
        SurfaceTexture st = cameraView.getSurfaceTexture();
        if(mCameraHandler != null){
            mCameraHandler.startPreview(st,mPreviewListener);
        }
    }

    // Camera对焦
    public void startCameraFoucs(){
        if(mCameraHandler != null){
            mCameraHandler.startCameraFoucs();
        }
    }

    // 获取Camera支持的所有分辨率参数
    public List<Size> getSupportedPreviewSizes(){
        if(mCameraHandler == null)
            return null;
        return mCameraHandler.getSupportedPreviewSizes();
    }
}
