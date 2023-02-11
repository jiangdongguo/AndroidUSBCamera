![logo.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/906db02b1dbc49669c38f870b6df2e96~tplv-k3u1fbpfcp-watermark.image?)


# AUSBC&ensp;[![](https://jitpack.io/v/jiangdongguo/AndroidUSBCamera.svg)](https://jitpack.io/#jiangdongguo/AndroidUSBCamera) [![API](https://img.shields.io/badge/API-19%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=19)

&emsp;Flexible and useful UVC camera engine on Android platform, you can use it to simply  open your uvc camera (supporting multi-road cameras) without  any system permissions . The only thing you should do is that confirming your Android device must support OTG function. So, welcom to use **AUSBC3.0** and welcom to **star** & **fork** & **issues**!

![multi-road camera](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/75a8f700d97c4de4979c9d0927764af9~tplv-k3u1fbpfcp-watermark.image?)

&emsp;[@ Example showing](https://live.csdn.net/v/226993)



Feature
-------

- Support opening multi-road camera;
- Support opening uvc camera on Android 4.4+;
- Support previewing 480p、720p、1080p，etc;
- Support adding effects with OpenGL ES 2.0;
- Support capture photo(`.jpg`)、viedo(`.mp4`/`.h264`/`yuv`) and audio(`pcm`/`mp3`/`aac`)
- Support rotating camera view;
- Support offscreen rendering;
- Support recording media along with acquiring YUV/RGBA/PCM/H.264/AAC stream, you can push it to your media server;
- Support acquiring all resolutions and usb devices, etc;
- Support acquiring system or device mic(UAC) audio data;
- Support armeabi-v7a, arm64-v8a, x86 & x86_64;

Usages
-------

&emsp;`AUSBC 3.0`  was refactored by kotlin and It is simpler to use and more feature-rich. So, I highly recommend you to use the current version and let's build it together.

- **Get AUSBC**

&emsp;First,  add it in your root **build.gradle** or **settings.gradle** at the end of repositories: 

```groovy
allprojects {
    repositories {
        google()
        jcenter()
        maven { url "https://jitpack.io" }
    }
}
```

&emsp;Second, add the dependency in your **app.gradle** , latest tag is [![](https://jitpack.io/v/jiangdongguo/AndroidUSBCamera.svg)](https://jitpack.io/#jiangdongguo/AndroidUSBCamera):

```groovy
dependencies {
    implementation 'com.github.jiangdongguo.AndroidUSBCamera:libausbc:latest_tag'
}
```

- **Simply usage**

&emsp; As for how to use this module correctly,  just  making your Fragment or Activity implement **CameraFragment** or **CameraActivity**.

```kotlin
class DemoFragment: CameraFragment() {
    private var mViewBinding: FragmentUvcBinding? = null

    override fun getRootView(inflater: LayoutInflater, container: ViewGroup?): View? {
        if (mViewBinding == null) {
            mViewBinding = FragmentUvcBinding.inflate(inflater, container, false)
        }
        return mViewBinding?.root
    }

    // if you want offscreen render
    // please return null
    override fun getCameraView(): IAspectRatio? {
        return mViewBinding?.tvCameraRender
    }

    // if you want offscreen render
    // please return null, the same as getCameraView()
    override fun getCameraViewContainer(): ViewGroup? {
        return mViewBinding?.container
    }

    // camera open status callback
    override fun onCameraState(self: ICamera, 
                               code: ICameraStateCallBack.State,
                               msg: String?) {
        when (code) {
            ICameraStateCallBack.State.OPENED -> handleCameraOpened()
            ICameraStateCallBack.State.CLOSED -> handleCameraClosed()
            ICameraStateCallBack.State.ERROR -> handleCameraError()
        }
    }
    
    override fun getGravity(): Int = Gravity.TOP
}
```

&emsp;The most important is that you should override `getRootView()`/`getCameraViewContainer()`/
`getCameraView()` at least which means fragment's root view 、texture  or surface view and it's container。Of course, the same as **CameraActivity** and now you can see the uvc camera preview。

- **Advanced usage**

&emsp;If you want some custom configurations, you can do like this in your **DemoFragment**: 

```kotlin
// if you want to open a camera default
// please override getDefaultCamera()
override fun getDefaultCamera(): UsbDevice? {
    return super.getDefaultCamera()
}

// if you want to custom camera requst
// please override getCameraRequest()
override fun getCameraRequest(): CameraRequest {
    return CameraRequest.Builder()
    .setPreviewWidth(1280) // camera preview width
    .setPreviewHeight(720) // camera preview height
    .setRenderMode(CameraRequest.RenderMode.OPENGL) // camera render mode
    .setDefaultRotateType(RotateType.ANGLE_0) // rotate camera image when opengl mode
    .setAudioSource(CameraRequest.AudioSource.SOURCE_AUTO) // set audio source
    .setAspectRatioShow(true) // aspect render,default is true
    .setCaptureRawImage(false) // capture raw image picture when opengl mode
    .setRawPreviewData(false)  // preview raw image when opengl mode
    .create()
}


```

&emsp;Of course, you can also capture a **jpg** image or a **mp4** video or  a **mp3** audio file and update resolution or different uvc camera and acquring the stream of **H264/AAC/YUV/PCM**  by calling those method:

```kotlin
// capture jpg image
captureImage(callBack, savePath)
// capture mp4 video
captureVideoStart(callBack, path, durationInSec)
captureVideoStop()
// capture stream(H.264/AAC) only
captureStreamStart()
captureStreamStop()
// capture mp4 video
captureVideoStart(callBack, path, durationInSec)
captureVideoStop()
// capture mp3 audio
captureAudioStart(callBack, path)
captureAudioStop()
// play mic in real time
startPlayMic(callBack)
stopPlayMic()
// rotate camera
// base on opening opengl es
setRotateType(type)
// switch different camera
switchCamera(UsbDevice)
// update resolution
updateResolution(width, height)
// get all preview sizes
getAllPreviewSizes(aspectRatio)
// acquire encode data(H.264 or AAC)
addEncodeDataCallBack(callBack)
// acquire raw data(NV21 or RGBA)
setPreviewDataCallBack(callBack)
// get current camera
getCurrentCamera()
// get current camera status
isCameraOpened()
// get all camera device list
getDeviceList()
```

&emsp;Or, camera configuration:

```kotlin
setZoom(0)
setSharpness(0)
setHue(0)
setSaturation(0)
setContrast(0)
setGamma(0)
setGain(0)
setAutoWhiteBalance(true)
setAutoFocus(true)
// send custom command to camera
sendCameraCommand(command)
```

&emsp;For more advanced features, you can even add some **filters** to your camera.This library providers some default filters, sush as **EffectBlackWhite**、**EffectSoul** and **EffectZoom**, and more filters will be added in the future.Of coure, you can also relize your own filters by extending **AbstractEffect**. For example：

```kotlin
// First, extending AbstractEffect
class EffectBlackWhite(ctx: Context) : AbstractEffect(ctx) {

    override fun getId(): Int = ID

    override fun getClassifyId(): Int = CameraEffect.CLASSIFY_ID_FILTER

    override fun getVertexSourceId(): Int = R.raw.base_vertex

    override fun getFragmentSourceId(): Int = R.raw.effect_blackw_fragment

    companion object {
        const val ID = 100
    }
}

// Second, adding or updating or removing filter
addRenderEffect(effect)
removeRenderEffect(effect)
updateRenderEffect(classifyId, effect)
```

&emsp;At last, If you want to realize streaming, maybe **IPusher** and **AusbcPusher** can help you.

- open multi-road camera

&emsp;If you want to open multi-road camera, you can make your Fragment or Activity extend **MultiCameraFragment** or **MultiCameraActivity**.Just do like this and more details please look up **DemoMultiCameraFragment**：

```kotlin
class DemoMultiCameraFragment : MultiCameraFragment(), ICameraStateCallBack {

    // a camera be attached
    override fun onCameraAttached(camera: MultiCameraClient.Camera) {}
    
	// a camera be detached
    override fun onCameraDetached(camera: MultiCameraClient.Camera) {}

    // a camera be connected
    override fun onCameraConnected(camera: MultiCameraClient.Camera) {
  		camera.openCamera(textureView, getCameraRequest())
        camera.setCameraStateCallBack(this)
    }

    // a camera be disconnected
    override fun onCameraDisConnected(camera: MultiCameraClient.Camera) {
         camrea.closeCamera()
    }

    // a camera be opened or closed or error
    override fun onCameraState(
        self: MultiCameraClient.Camera,
        code: ICameraStateCallBack.State,
        msg: String?) {
		when (code) {
            ICameraStateCallBack.State.OPENED -> handleCameraOpened()
            ICameraStateCallBack.State.CLOSED -> handleCameraClosed()
            ICameraStateCallBack.State.ERROR -> handleCameraError()
        }
    }

    override fun getRootView(inflater: LayoutInflater, container: ViewGroup?): View {
        return rootView
    }
    
    // [optional]
    // If you want to open the specified camera,
    // you need to let isAutoRequestPermission() false.
    // And then you need to call requestPermission(device) in your own Fragment/Activity
    // when onAttachDev() called, default is true.
    protected fun isAutoRequestPermission() = true
}
```



- Debug online

&emsp;If you want to debug the project online or modify something, those steps you should do:

&emsp;**First**, modifying the **Settings.gradle** file and making those to module. 

```groovy
include ':app'

// For debug online
include ':libausbc'
include ':libuvc'
include ':libnative'
include ':libuvccommon'
```

&emsp;**Second**, let **app.gradle** dependenced on `':libausbc'` .

```groovy
dependencies {

    // For debug online
    implementation project(':libausbc')

    // demo
    //implementation 'com.github.jiangdongguo.AndroidUSBCamera:libausbc:latest_tag'
}
```



[@ 中文文档](https://juejin.cn/post/7115229806844706847/)



Demo
-------

![](https://www.pgyer.com/app/qrcode/fVo5)

&emsp;[AUSBC.apk](https://github.com/jiangdongguo/AndroidUSBCamera/blob/master/app/release/app-release.apk)


Version
-------

[Update details](https://github.com/jiangdongguo/AndroidUSBCamera/blob/master/VERSION.md)

Q & A
-------

[FAQ](https://github.com/jiangdongguo/AndroidUSBCamera/blob/master/FAQ.md)


Homepage & Help
-------

[1. JUEJIN](https://juejin.cn/user/1311062343296222)

[2. CSDN（Update stopped）](https://blog.csdn.net/andrexpert)



&emsp;If you have any question or fun ideas, please issues to me.  
&emsp;Of course, you can also send me a **微信**   "`laojiang299`"   or a **EMAIL**  "`765067602@qq.com`".   

&emsp;So, do not forget to send logs from location **Android/data/com.jiangdg.ausbc/files** and collect logcat information by executing command `adb shell logcat -v threadtime > usbcamera.log`



Thanks
-------

 [saki4510t/UVCCamera](https://github.com/saki4510t/UVCCamera)



License
-------

```c
Copyright 2017-2023 Jiangdongguo

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
