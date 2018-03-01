AndroidUSBCamera 2.0
============   
  AndroidUSBCamera is developed based on the saki4510t/UVCCamera, the project of USB Camera (UVC equipment) and the use of video data acquisition are highly packaged, and it can help developers using USB Camera devices easily by a few simple APIs. By using AndroidUSBCamera,you can detect and connect to a USB Camera simply.And you also can use it to realize taking picture,recording mp4,switching resolutions and setting  camera's contrast or brightness,etc.Here is some gifs of this demo:  
![效果图](http://img.blog.csdn.net/20171025213631816)  

[中文文档： OkCamera，Android 相机应用开发通用库](http://blog.csdn.net/andrexpert/article/details/79302141)  

Usage
-------
### 1.Add to your Android Studio project  

Step 1. Add the JitPack repository to your build file.Add it in your root build.gradle at the end of repositories:  
```java
allprojects {
		repositories {
			...
			maven { url 'http://raw.github.com/saki4510t/libcommon/master/repository/' }
			maven { url 'https://jitpack.io' }
		}
	}
```  
Step 2. Add the dependency  
```java
dependencies { 
	    compile 'com.github.jiangdongguo:AndroidUSBCamera:2.0'
}  
```
### 2. APIs Introduction  
(1) In order to using it correctly,the following four steps must be achieved：  
```java
mUVCCameraView = (CameraViewInterface) mTextureView;
mUVCCameraView.setCallback(mCallback);
mCameraHelper = UVCCameraHelper.getInstance();
mCameraHelper.initUSBMonitor(this, mUVCCameraView, mDevConnectListener); 
```
   To be attention,mCallback is a object of interface CameraViewInterface.Callback,and it's used to be listenering surfaceView
created or detoryed.mDevConnectListener is a object of interface UVCCameraHelper.OnMyDevConnectListener,and it's used to be listenering to detect and conntect USB device.Here is the coding order:  
```java
private CameraViewInterface.Callback mCallback = new CameraViewInterface.Callback mCallback(){
    @Override
    public void onSurfaceCreated(CameraViewInterface view, Surface surface) {
        // must have
        if (!isPreview && mCameraHelper.isCameraOpened()) {
            mCameraHelper.startPreview(mUVCCameraView);
            isPreview = true;
        }
    }

    @Override
    public void onSurfaceChanged(CameraViewInterface view, Surface surface, int width, int height) {

    }

    @Override
    public void onSurfaceDestroy(CameraViewInterface view, Surface surface) {
        // must have
        if (isPreview && mCameraHelper.isCameraOpened()) {
            mCameraHelper.stopPreview();
            isPreview = false;
        }
    }
}
private UVCCameraHelper.OnMyDevConnectListener listener = new UVCCameraHelper.OnMyDevConnectListener() {

        @Override
        public void onAttachDev(UsbDevice device) {
            // request open permission(must have)
            if (!isRequest) {
                isRequest = true;
                if (mCameraHelper != null) {
                    mCameraHelper.requestPermission(0);
                }
            }
        }

        @Override
        public void onDettachDev(UsbDevice device) {
            // close camera(must have)
            if (isRequest) {
                isRequest = false;
                mCameraHelper.closeCamera();
            }
        }

        @Override
        public void onConnectDev(UsbDevice device, boolean isConnected) {
       	
        }

        @Override
        public void onDisConnectDev(UsbDevice device) {
          
        }
    };
```

License
-------

    Copyright 2018 Jiangdongguo

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

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
