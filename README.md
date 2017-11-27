# AndroidUSBCamera开源项目
### AndroidUSBCamera基于[saki4510t/UVCCamera](https://github.com/saki4510t/UVCCamera)开发，该项目对USB Camera(UVC设备)的使用和视频数据采集进行了高度封装，能够帮助开发者通过几个简单的API实现USB Camera设备的检测、连接、预览和音视频数据采集，最重要的是手机无需root，只需支持otg功能即可驱动。主要功能包括：  
(1)支持USB Camera设备检测，画面实时预览;  
(2)支持本地录制mp4格式视频,支持实时获取音视频数据流;  
(3)支持jpg格式图片抓拍;  
(4)支持获取camera支持的分辨率，和分辨率切换;  
(5)支持屏蔽声音,重启Camera; 
(6)支持相机自动对焦；  
(7)支持调整对比度和亮度

> AndroidUSBCamera is developed based on the saki4510t/UVCCamera, the project of USB Camera (UVC equipment) and the use of video data acquisition are highly packaged, and it can help developers using USB Camera devices to connect, preview and video data collection by a few simple API. The main functions include:   
   (1)supports detecting USB Camera equipment, and previewing;  
   (2)supports recording MP4 format video, and acquiring real-time audio and video data;  
   (3)supports capturing JPG format image;  
   (4)supports getting supported preview sizes，and switching resolution;  
   (5)supports shielding sound;  
   (6)supports camera auto foucs;  
   (7)supports change camera's contrast and brightness

## 如何使用AndroidUSBCamera项目 
![效果图](http://img.blog.csdn.net/20171025213631816)
### 1.添加依赖到本地工程
  To get a Git project into your build:  
  
第一步 添加JitPack仓库到工程gradle  
Step 1. Add the JitPack repository to your build file
Add it in your root build.gradle at the end of repositories:
```	
allprojects {
		repositories {
			...
			maven { url 'http://raw.github.com/saki4510t/libcommon/master/repository/' }
			maven { url 'https://jitpack.io' }
		}
	}
```  

第二步 添加依赖到app Module的gradle   
Step 2. Add the dependency  

```
dependencies {
	 compile 'com.github.jiangdongguo:AndroidUSBCamera:1.3.8'
} 
```  

### 2.初始化引擎，注册USB设备事件监听器  
  Init AndroidUSBCamera engine，register the USB device event listener  
  
```
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
                isPreview = false;
            }else{
                isPreview = true;
            }
        }

        // 与USB设备断开连接
        @Override
        public void onDisConnectDev(UsbDevice device) {
            showShortMsg("连接失败");
        }
    };
    
    mUVCCameraView = (CameraViewInterface) mTextureView;
    mUVCCameraView.setCallback(new CameraViewInterface.Callback() {
        @Override
        public void onSurfaceCreated(CameraViewInterface view, Surface surface) {
            if(!isPreview && mUSBManager.isCameraOpened()) {
                 mUSBManager.startPreview(mUVCCameraView, new AbstractUVCCameraHandler.OnPreViewResultListener() {
                 @Override
                 public void onPreviewResult(boolean result) {

                 }
              });
              isPreview = true;
              }
         }
            @Override
            public void onSurfaceChanged(CameraViewInterface view, Surface surface, int width, int height) {

            }

            @Override
            public void onSurfaceDestroy(CameraViewInterface view, Surface surface) {
                if(isPreview && mUSBManager.isCameraOpened()) {
                    mUSBManager.stopPreview();
                    isPreview = false;
                }
            }
        });
        // 初始化引擎
        mUSBManager = USBCameraManager.getInstance();
        mUSBManager.initUSBMonitor(this,listener);
        mUSBManager.createUVCCamera(mUVCCameraView);
    
```  

### 3. 注册USB设备广播事件监听器，开始Camera预览  
  Register the USB device broadcast event listener and start the Camera Preview
```
// 注册USB事件广播监听器
if(mUSBManager != null){
      mUSBManager.registerUSB();
}
// 恢复Camera预览
 if(mUVCCameraView != null){
      mUVCCameraView.onResume();
 }
```  

### 4. 注销USB设备广播事件监听器，停止Camera预览  
  Unregister the USB device broadcast event listener and stop the Camera Preview
```
// 注销USB事件广播监听器
 if(mUSBManager != null){
         mUSBManager.unregisterUSB();
  }
 // 暂停Camera预览
 if(mUVCCameraView != null){
         mUVCCameraView.onPause();
 }
```  

### 5. 图片抓拍
  Picture capturing
```
if(mUSBManager == null || ! mUSBManager.isCameraOpened()){
          showShortMsg("抓拍异常，摄像头未开启");
          return;
 }
 mUSBManager.capturePicture(picPath, new AbstractUVCCameraHandler.OnCaptureListener() {
          @Override
          public void onCaptureResult(String path) {
               showShortMsg("保存路径："+path);
          }
 });
		
```  

### 6. 本地录制(可实时获取音视频数据流)
   recoring mp4，and get media real-stream  
     
```
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

                        @Override
                        public void onRecordResult(String videoPath) {
                            showShortMsg(videoPath);
                        }
                    });
// 停止录制
mUSBManager.stopRecording();
```  

### 7. 切换分辨率
  update Resulotion  
    
```
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
 // 获取Camera支持得分辨率  
 List<Size> list = mUSBManager.getSupportedPreviewSizes();
 // Camera自动对焦  
 mUSBManager.startCameraFoucs();
```  

### 8. 释放引擎资源
  release resource  
    
```
// 释放资源
if(mUSBManager != null){
       mUSBManager.release();
 }
```  
### 9. 添加权限
  add permissions  
    
```
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
``` 
### USBCameraManager  API (Other)
```
(1) void requestPermission(int index)：请求授予开启USB摄像头权限；
(2) int getUsbDeviceCount()：返回查询到的可用USB Camera数目；
(3) boolean isRecording()：判断是否正在录制视频；
(4) boolean isCameraOpened()：判断USB摄像头是否正常打开；
(5) void release()：释放资源
(6) USBMonitor getUSBMonitor()：返回USBMonitor实例；
(7) mUSBManager.setModelValue(USBCameraManager.MODE_CONTRAST,contrast++); 调整对比度
(8) mUSBManager.setModelValue(USBCameraManager.MODE_BRIGHTNESS,brightness++);调整亮度
```
