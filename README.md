![logo.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/906db02b1dbc49669c38f870b6df2e96~tplv-k3u1fbpfcp-watermark.image?)


# AUSBC&ensp;[![](https://jitpack.io/v/jiangdongguo/AndroidUSBCamera.svg)](https://jitpack.io/#jiangdongguo/AndroidUSBCamera) [![API](https://img.shields.io/badge/API-19%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=19)


&emsp;Flexible and useful UVC camera engine on Android platform, you can use it to simply  open your uvc camera without  any system permissions . The only thing you should do is that confirming your Android device must support OTG function. So, welcom to use **AUSBC3.0** and welcom to **star** & **fork** & **issues**!

&emsp;[@ Example showing](https://live.csdn.net/v/220915?spm=1001.2014.3001.5501)



Feature
-------

- Support opening camera1、camera2 and uvc camera on Android 4.4 ~ 11;
- Support previewing 480p、720p、1080p，etc;

- Support adding effects with OpenGL ES 2.0;
- Support capture photo(`.jpg`)、viedo(`.mp4`/`.h264`/`yuv`) and audio(`pcm`/`mp3`/`aac`)
- Support rotating camera view;
- Support showing camera offscreen;
- Support recording media along with acquring h264/aac stream, you can push it to your media server;
- Support acquring all resolutions and usb devices, etc 



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
class DemoFragment : CameraFragment() {
    private lateinit var mViewBinding: FragmentDemoBinding
    
    override fun initView() {
        
    }
    
    override fun getCameraView(): IAspectRatio {
        return AspectRatioTextureView(requireContext())
    }
    
    override fun initData() {
        
    }

    override fun getCameraViewContainer(): ViewGroup {
        return mViewBinding.cameraViewContainer
    }

    override fun getRootView(inflater: LayoutInflater, container: ViewGroup?): View {
        mViewBinding = FragmentDemoBinding.inflate(inflater, container, false)
        return mViewBinding.root
    }

    override fun getGravity(): Int = Gravity.TOP
}
```

&emsp;The most important is that you should override `getRootView()`/`getCameraViewContainer()`/
`getCameraView()` at least which means fragment's root view 、texture  or surface view and it's container。Of course, the same as **CameraActivity** and now you can see the uvc camera preview。

- **Advanced usage**

&emsp;If you want some custom configurations, you can do like this: 

```kotlin
class DemoFragment : CameraFragment() {
    ...
    override fun getCameraClient(): CameraClient? {
        return CameraClient.newBuilder(requireContext())
            .setEnableGLES(true)   // use opengl render 
            .setRawImage(true)     // capture raw or filter image
            .setDefaultEffect(EffectBlackWhite(requireContext())) // default effect
            .setCameraStrategy(CameraUvcStrategy(requireContext())) // camera type
            .setCameraRequest(getCameraRequest()) // camera configurations
            .setDefaultRotateType(RotateType.ANGLE_0) // default camera rotate angle
            .openDebug(true) // is debug mode
            .build()
    }
    
    private fun getCameraRequest(): CameraRequest {
        return CameraRequest.CameraRequestBuilder()
            .setFrontCamera(false) // only for camera1/camera2
            .setPreviewWidth(640)  // initial camera preview width
            .setPreviewHeight(480) // initial camera preview height
            .create()
    }
}
```

&emsp;There is no doubt that **CameraClient** is the core class in this library, you can use the default CameraClient object to preview your camera or custom it. By using **CameraClient**, you can capture a **jpg** image or a **mp4** video or  a **mp3** audio file and update resolution or different uvc camera. You can even acquring the stream of **H264/AAC/YUV**. For example:

```kotlin
// capture jpg image
mCameraClient?.captureImage(callBack, savePath)

// capture mp4 video
mCameraClient?.captureVideoStart(callBack, path, durationInSec)
mCameraClient?.captureVideoStop()

// capture mp3 audio
mCameraClient?.captureAudioStart(callBack, path)
mCameraClient?.captureAudioStop()

// play mic in real time
mCameraClient?.startPlayMic(callBack)
mCameraClient?.stopPlayMic()

// rotate camera
// base on opening opengl es
mCameraClient?.setRotateType(type)

// switch different camera
mCameraClient?.switchCamera(cameraId)

// update resolution
mCameraClient?.updateResolution(width, height)

// get all preview sizes
mCameraClient?.getAllPreviewSizes(aspectRatio)

// acquire encode data(h264 or aac)
mCameraClient?.addEncodeDataCallBack(callBack)

// acquire raw data(yuv)
mCameraClient?.addPreviewDataCallBack(callBack)
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
mCameraClient?.addRenderEffect(effect)
mCameraClient?.removeRenderEffect(effect)
mCameraClient?.updateRenderEffect(classifyId, effect)
```

- Debug online

&emsp;If you want to debug the project online or modify something, those steps you should do:

&emsp;**First**, modifying the **Settings.gradle** file and making those to module. 

```groovy
include ':app'

// For debug online
include ':libausbc'
include ':libuvc'
include ':libpush'
include ':libnative'
include ':libuvccommon'
```

&emsp;**Second**, let **app.gradle** dependenced on `':libausbc'` .

```groovy
dependencies {

    // For debug online
    implementation project(':libausbc')

    // demo
    //implementation 'com.github.jiangdongguo.AndroidUSBCamera:libausbc:3.1.7'
}
```



[@ 中文文档](https://juejin.cn/post/7115229806844706847/)



Demo
-------

- download by Scanning code


![apk.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/dc8984f3ae2844e48ca69aac5eca636d~tplv-k3u1fbpfcp-watermark.image?)

- download by opening url

&emsp;[AUSBC3.1.2.apk](https://github.com/jiangdongguo/AndroidUSBCamera/blob/master/AUSBC3.1.2.apk)



Version
-------

#### 2020.01.15  version 2.3.2  

1. Support adding time overlay(attention: overlay only support armeabi-v7a & arm64-v8a);
2. Support recording device mic;
3. Update to androidx and update commonLibVersion from 2.14.2 to 4.1.1;
4. Fix saving files failed.  

#### 2020.04.14  version 2.3.4

1. Fix pull version 2.3.2 failed.
2. Fix android 9.0 sometimes can not preview.
3. Fix pull out crash 1.
4. Update to all so files to new.

#### 2021.03.16  version 2.3.5

1. Fix stop preview crash.
2. Open UVCCamera NDK Library project [UVCCameraLib](https://github.com/jiangdongguo/UVCCameraLib)

#### 2021.04.08  version 2.3.6

1. Fix pull out crash 2.

#### 2021.11.17  version 2.3.7

1. Fix download common library failed, and use aar instead.

#### 2022.01.28  version 2.3.8

1. Detect multiple cameras.

#### 2022.07.01  version 3.0

1. Refactor the project with kotlin;
2. Support effects with OpenGL ES 2.0;
3. Support capture photo、viedo and audio;
4. Support rotate camera view;
5. Support show camera offscreen;
6. Support versionTarget>=29;
7. Fix call StopPreview() crash (may have others, please tell me);
8. Fix open some device failed, resolution or not uvc device as usual；
9. Fix acquire  common library failed, see `libuvc/libusbcommon_v4.1.1.aar`;
10. Merge NDK project into main project, see `libuvc` module.

#### 2022.07.12  version 3.1.2

1. Import usb common library instead of aar;
2. Fix “pthread_mutex_lock called on a destroyed mutex” when call stop preview
3. Fix jitpack.io build error.

#### 2022.07.13  version 3.1.7

1. Cancel supporting x86 & x86_64, If you need it, please downloading the project and build it;
2. Release version 3.1.7 and support depend on it directly.

#### 2022.07.15  version 3.1.8

1. fix a native memory leak when repeat open close camera

Homepage & Help
-------

[1. JUEJIN](https://juejin.cn/user/1311062343296222)

[2. CSDN（Update stopped）](https://blog.csdn.net/andrexpert)



&emsp;If you have any question or fun ideas, please issues to me.  
&emsp;Of course, you can also send me a email[ **765067602@qq.com** ].  

&emsp;So, do not forget to send logs from location **Android/data/com.jiangdg.ausbc/files** together!



Thanks
-------

 [saki4510t/UVCCamera](https://github.com/saki4510t/UVCCamera)



License
-------

    Copyright 2017-2022 Jiangdongguo
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
       http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
