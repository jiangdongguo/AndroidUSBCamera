# AndroidUSBCamera开源项目
### AndroidUSBCamera基于[saki4510t/UVCCamera](https://github.com/saki4510t/UVCCamera)开发，该项目对USB Camera(UVC设备)的使用和视频数据采集进行了高度封装，能够帮助开发者通过几个简单的API实现USB Camera设备的检测、连接、预览和视频数据采集。主要功能包括：USB Camera实时预览；本地录制mp4格式视频；png格式图片抓拍；实时获取编码后的音视频数据流。  
> AndroidUSBCamera is developed based on the saki4510t/UVCCamera, the project of USB Camera (UVC equipment) and the use of video data acquisition are highly packaged, and it can help developers using USB Camera devices to connect, preview and video data collection by a few simple API. The main functions include: USB Camera real-time preview; local recording MP4 format video; PNG format photo capture; real-time access to encoded audio and video data stream.  

## 如何使用AndroidUSBCamera项目  
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
	 compile 'com.github.jiangdongguo:AndroidUSBCamera:v1.1.0'
} 
```  

### 2.初始化引擎，注册USB设备事件监听器  
  Init AndroidUSBCamera engine，register the USB device event listener  
  
```
USBCameraManager mUSBManager = USBCameraManager.getInstance();
// mTextureView为UVCCameraTextureView实例，继承于TextureView
// 用于渲染图像，需要在xml文件中定义
CameraViewInterface mUVCCameraView = (CameraViewInterface) mTextureView;
// 初始化引擎，注册事件监听器
mUSBManager.init(this, mUVCCameraView, new USBCameraManager.OnMyDevConnectListener() {
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
        public void onConnectDev(UsbDevice device) {
        
        }

        // 与USB设备断开连接
        @Override
        public void onDisConnectDev(UsbDevice device) {
          
        }
    });
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
mUSBManager.capturePicture(picPath);
```  

### 6. 本地录制(可实时获取音视频数据流)
   recoring mp4，and get media real-stream  
     
```
 if(mUSBManager == null || ! mUSBManager.isCameraOpened()){
           showShortMsg("录制异常，摄像头未开启");
           return;
 }
// 开始录制
if( !mUSBManager.isRecording()){
mUSBManager.startRecording(videoPath, new AbstractUVCCameraHandler.OnEncodeResultListener() {
        @Override
        public void onEncodeResult(byte[] data, int offset, int length, long timestamp, int type) {
              // type=0为音频流，type=1为视频流
        });
}
// 停止录制
mUSBManager.stopRecording();
```  

### 7. 释放引擎资源
  release resource  
    
```
// 释放资源
if(mUSBManager != null){
       mUSBManager.release();
 }
```  

### USBCameraManager  API (Other)
```
(1) void requestPermission(int index)：请求授予开启USB摄像头权限；
(2) int getUsbDeviceCount()：返回查询到的可用USB Camera数目；
(3) boolean isRecording()：判断是否正在录制视频；
(4) boolean isCameraOpened()：判断USB摄像头是否正常打开；
(5) void release()：释放资源
(6) USBMonitor getUSBMonitor()：返回USBMonitor实例；
```
