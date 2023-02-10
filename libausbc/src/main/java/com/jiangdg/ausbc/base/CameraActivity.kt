/*
 * Copyright 2017-2023 Jiangdg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jiangdg.ausbc.base

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.usb.UsbDevice
import android.view.*
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.camera.bean.PreviewSize
import com.jiangdg.ausbc.callback.*
import com.jiangdg.ausbc.camera.CameraUVC
import com.jiangdg.ausbc.render.effect.AbstractEffect
import com.jiangdg.ausbc.render.effect.EffectBlackWhite
import com.jiangdg.ausbc.render.env.RotateType
import com.jiangdg.ausbc.utils.Logger
import com.jiangdg.ausbc.utils.SettableFuture
import com.jiangdg.ausbc.widget.IAspectRatio
import com.jiangdg.usb.USBMonitor
import java.lang.IllegalArgumentException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean


/**Extends from BaseActivity for one uvc camera
 *
 * @author Created by jiangdg on 2023/2/3
 */
abstract class CameraActivity: BaseActivity(), ICameraStateCallBack {
    private var mCameraView: IAspectRatio? = null
    private var mCameraClient: MultiCameraClient? = null
    private val mCameraMap = hashMapOf<Int, MultiCameraClient.ICamera>()
    private var mCurrentCamera: SettableFuture<MultiCameraClient.ICamera>? = null

    private val mRequestPermission: AtomicBoolean by lazy {
        AtomicBoolean(false)
    }

    override fun initData() {
        when (val cameraView = getCameraView()) {
            is TextureView -> {
                handleTextureView(cameraView)
                cameraView
            }
            is SurfaceView -> {
                handleSurfaceView(cameraView)
                cameraView
            }
            else -> {
                null
            }
        }.apply {
            mCameraView = this
            // offscreen render
            if (this == null) {
                registerMultiCamera()
                return
            }
        }.also { view->
            getCameraViewContainer()?.apply {
                removeAllViews()
                addView(view, getViewLayoutParams(this))
            }
        }
    }

    override fun clear() {
        unRegisterMultiCamera()
    }

    protected fun registerMultiCamera() {
        mCameraClient = MultiCameraClient(this, object : IDeviceConnectCallBack {
            override fun onAttachDev(device: UsbDevice?) {
                device ?: return
                if (mCameraMap.containsKey(device.deviceId)) {
                    return
                }
                generateCamera(this@CameraActivity, device).apply {
                    mCameraMap[device.deviceId] = this
                }
                // Initiate permission request when device insertion is detected
                // If you want to open the specified camera, you need to override getDefaultCamera()
                if (mRequestPermission.get()) {
                    return
                }
                getDefaultCamera()?.apply {
                    if (vendorId == device.vendorId && productId == device.productId) {
                        Logger.i(TAG, "default camera pid: $productId, vid: $vendorId")
                        requestPermission(device)
                    }
                    return
                }
                requestPermission(device)
            }

            override fun onDetachDec(device: UsbDevice?) {
                mCameraMap.remove(device?.deviceId)?.apply {
                    setUsbControlBlock(null)
                }
                mRequestPermission.set(false)
                mCurrentCamera = null
            }

            override fun onConnectDev(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
                device ?: return
                ctrlBlock ?: return
                mCameraMap[device.deviceId]?.apply {
                    setUsbControlBlock(ctrlBlock)
                }?.also { camera ->
                    mCurrentCamera = SettableFuture()
                    mCurrentCamera?.set(camera)
                    openCamera(mCameraView)
                    Logger.i(TAG, "camera connection. pid: ${device.productId}, vid: ${device.vendorId}")
                }
            }

            override fun onDisConnectDec(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
                closeCamera()
                mRequestPermission.set(false)
                mCurrentCamera = null
            }

            override fun onCancelDev(device: UsbDevice?) {
                mRequestPermission.set(false)
                mCurrentCamera = null
            }
        })
        mCameraClient?.register()
    }

    protected fun unRegisterMultiCamera() {
        mCameraMap.values.forEach {
            it.closeCamera()
        }
        mCameraMap.clear()
        mCameraClient?.unRegister()
        mCameraClient?.destroy()
        mCameraClient = null
    }

    protected fun getDeviceList() = mCameraClient?.getDeviceList()

    private fun handleTextureView(textureView: TextureView) {
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture?,
                width: Int,
                height: Int
            ) {
                registerMultiCamera()
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture?,
                width: Int,
                height: Int
            ) {
                surfaceSizeChanged(width, height)
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                unRegisterMultiCamera()
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
            }
        }
    }

    private fun handleSurfaceView(surfaceView: SurfaceView) {
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder?) {
                registerMultiCamera()
            }

            override fun surfaceChanged(
                holder: SurfaceHolder?,
                format: Int,
                width: Int,
                height: Int
            ) {
                surfaceSizeChanged(width, height)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder?) {
                unRegisterMultiCamera()
            }
        })
    }

    /**
     * Get current opened camera
     *
     * @return current camera, see [MultiCameraClient.ICamera]
     */
    protected fun getCurrentCamera(): MultiCameraClient.ICamera? {
        return try {
            mCurrentCamera?.get(2, TimeUnit.SECONDS)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Request permission
     *
     * @param device see [UsbDevice]
     */
    protected fun requestPermission(device: UsbDevice?) {
        mCameraClient?.requestPermission(device)
        mRequestPermission.set(true)
    }

    /**
     * Generate camera
     *
     * @param ctx context [Context]
     * @param device Usb device, see [UsbDevice]
     * @return Inheritor assignment camera api policy
     */
    protected open fun generateCamera(ctx: Context, device: UsbDevice): MultiCameraClient.ICamera {
        return CameraUVC(ctx, device)
    }

    /**
     * Get default camera
     *
     * @return Open camera by default, should be [UsbDevice]
     */
    protected open fun getDefaultCamera(): UsbDevice? = null

    /**
     * Capture image
     *
     * @param callBack capture status, see [ICaptureCallBack]
     * @param savePath custom image path
     */
    protected fun captureImage(callBack: ICaptureCallBack, savePath: String? = null) {
        getCurrentCamera()?.captureImage(callBack, savePath)
    }

    /**
     * Get default effect
     */
    protected fun getDefaultEffect() = getCurrentCamera()?.getDefaultEffect()

    /**
     * Switch camera
     *
     * @param usbDevice camera usb device
     */
    protected fun switchCamera(usbDevice: UsbDevice) {
        getCurrentCamera()?.closeCamera()
        try {
            Thread.sleep(500)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        requestPermission(usbDevice)
    }

    /**
     * Is camera opened
     *
     * @return camera open status
     */
    protected fun isCameraOpened() = getCurrentCamera()?.isCameraOpened()  ?: false

    /**
     * Update resolution
     *
     * @param width camera preview width
     * @param height camera preview height
     */
    protected fun updateResolution(width: Int, height: Int) {
        getCurrentCamera()?.updateResolution(width, height)
    }

    /**
     * Get all preview sizes
     *
     * @param aspectRatio preview size aspect ratio,
     *                      null means getting all preview sizes
     */
    protected fun getAllPreviewSizes(aspectRatio: Double? = null) = getCurrentCamera()?.getAllPreviewSizes(aspectRatio)

    /**
     * Add render effect
     *
     * @param effect a effect will be added, only enable opengl render worked, see [AbstractEffect]
     */
    protected fun addRenderEffect(effect: AbstractEffect) {
        getCurrentCamera()?.addRenderEffect(effect)
    }

    /**
     * Remove render effect
     *
     * @param effect a effect will be removed, only enable opengl render worked, see [AbstractEffect]
     */
    protected fun removeRenderEffect(effect: AbstractEffect) {
        getCurrentCamera()?.removeRenderEffect(effect)
    }

    /**
     * Update render effect
     *
     * @param classifyId effect classify id
     * @param effect new effect, null means set none
     */
    protected fun updateRenderEffect(classifyId: Int, effect: AbstractEffect?) {
        getCurrentCamera()?.updateRenderEffect(classifyId, effect)
    }

    /**
     * Start capture H264 & AAC only
     */
    protected fun captureStreamStart() {
        getCurrentCamera()?.captureStreamStart()
    }

    /**
     * Stop capture H264 & AAC only
     */
    protected fun captureStreamStop() {
        getCurrentCamera()?.captureStreamStop()
    }

    /**
     * Add encode data call back
     *
     * @param callBack encode data call back, see [IEncodeDataCallBack]
     */
    protected fun setEncodeDataCallBack(callBack: IEncodeDataCallBack) {
        getCurrentCamera()?.setEncodeDataCallBack(callBack)
    }

    /**
     * Add preview data call back
     *
     * @param callBack preview data call back, see [IPreviewDataCallBack]
     */
    protected fun addPreviewDataCallBack(callBack: IPreviewDataCallBack) {
        getCurrentCamera()?.addPreviewDataCallBack(callBack)
    }

    /**
     * Remove preview data call back
     *
     * @param callBack preview data call back, see [IPreviewDataCallBack]
     */
    fun removePreviewDataCallBack(callBack: IPreviewDataCallBack) {
        getCurrentCamera()?.removePreviewDataCallBack(callBack)
    }

    /**
     * Capture video start
     *
     * @param callBack capture status, see [ICaptureCallBack]
     * @param path custom save path
     * @param durationInSec divided record duration time in seconds
     */
    protected fun captureVideoStart(callBack: ICaptureCallBack, path: String ?= null, durationInSec: Long = 0L) {
        getCurrentCamera()?.captureVideoStart(callBack, path, durationInSec)
    }

    /**
     * Capture video stop
     */
    protected fun captureVideoStop() {
        getCurrentCamera()?.captureVideoStop()
    }

    /**
     * Capture audio start
     *
     * @param callBack capture status, see [ICaptureCallBack]
     * @param path custom save path
     */
    protected fun captureAudioStart(callBack: ICaptureCallBack, path: String ?= null) {
        getCurrentCamera()?.captureAudioStart(callBack, path)
    }

    /**
     * Capture audio stop
     */
    protected fun captureAudioStop() {
        getCurrentCamera()?.captureAudioStop()
    }

    /**
     * Start play mic
     *
     * @param callBack play mic in real-time, see [IPlayCallBack]
     */
    protected fun startPlayMic(callBack: IPlayCallBack? = null) {
        getCurrentCamera()?.startPlayMic(callBack)
    }

    /**
     * Stop play mic
     */
    protected fun stopPlayMic() {
        getCurrentCamera()?.stopPlayMic()
    }

    /**
     * Get current preview size
     *
     * @return camera preview size, see [PreviewSize]
     */
    protected fun getCurrentPreviewSize(): PreviewSize? {
        return getCurrentCamera()?.getCameraRequest()?.let {
            PreviewSize(it.previewWidth, it.previewHeight)
        }
    }

    /**
     * Rotate camera angle
     *
     * @param type rotate angle, null means rotating nothing
     * see [RotateType.ANGLE_90], [RotateType.ANGLE_270],...etc.
     */
    protected fun setRotateType(type: RotateType) {
        getCurrentCamera()?.setRotateType(type)
    }

    /***********************************************************************************************/
    /*********************************Camera parameter control *************************************/
    /**
     * Send camera command of uvc camera
     *
     * @param command hex value
     * @return control result
     */
    protected fun sendCameraCommand(command: Int) {
        getCurrentCamera()?.let { camera ->
            if (camera !is CameraUVC) {
                return
            }
            camera.sendCameraCommand(command)
        }
    }

    /**
     * Set auto focus
     *
     * @param focus
     */
    protected fun setAutoFocus(focus: Boolean) {
        getCurrentCamera()?.let { camera ->
            if (camera !is CameraUVC) {
                return
            }
            camera.setAutoFocus(focus)
        }
    }

    /**
     * Get auto focus
     *
     * @return is camera auto focus opened
     */
    protected fun getAutoFocus(): Boolean? {
        return getCurrentCamera()?.let { camera ->
            if (camera !is CameraUVC) {
                return@let false
            }
            camera.getAutoFocus()
        }
    }

    /**
     * Reset auto focus
     */
    protected fun resetAutoFocus() {
        getCurrentCamera()?.let { camera ->
            if (camera !is CameraUVC) {
                return
            }
            camera.resetAutoFocus()
        }
    }



    /**
     * Set brightness
     *
     * @param brightness camera brightness
     */
    protected fun setBrightness(brightness: Int) {
        getCurrentCamera()?.let { camera ->
            if (camera !is CameraUVC) {
                return
            }
            camera.setBrightness(brightness)
        }
    }

    /**
     * Get brightness
     *
     * @return current brightness value
     */
    protected fun getBrightness(): Int? {
        return getCurrentCamera()?.let { camera ->
            if (camera !is CameraUVC) {
                return@let null
            }
            camera.getBrightness()
        }
    }

    /**
     * Reset brightness
     */
    protected fun resetBrightness() {
        getCurrentCamera()?.let { camera ->
            if (camera !is CameraUVC) {
                return
            }
            camera.resetBrightness()
        }
    }

    /**
     * Set contrast
     *
     * @param contrast camera contrast
     */
    protected fun setContrast(contrast: Int) {
        getCurrentCamera()?.let { camera ->
            if (camera !is CameraUVC) {
                return
            }
            camera.setContrast(contrast)
        }
    }

    /**
     * Get contrast
     *
     * @return current contrast value
     */
    protected fun getContrast(): Int? {
        return getCurrentCamera()?.let { camera ->
            if (camera !is CameraUVC) {
                return@let null
            }
            camera.getContrast()
        }
    }

    /**
     * Reset contrast
     */
    protected fun resetContrast() {
        getCurrentCamera()?.let { camera ->
            if (camera !is CameraUVC) {
                return
            }
            camera.resetContrast()
        }
    }

    /**
     * Set gain
     *
     * @param gain camera gain
     */
    protected fun setGain(gain: Int) {
        getCurrentCamera()?.let { camera ->
            if (camera !is CameraUVC) {
                return
            }
            camera.setGain(gain)
        }
    }

    /**
     * Get gain
     *
     * @return current gain value
     */
    protected fun getGain(): Int? {
        return getCurrentCamera()?.let { camera ->
            if (camera !is CameraUVC) {
                return@let null
            }
            camera.getGain()
        }
    }

    /**
     * Reset gain
     */
    protected fun resetGain() {
        getCurrentCamera()?.let { camera ->
            if (camera !is CameraUVC) {
                return
            }
            camera.resetGain()
        }
    }

    /**
     * Set gamma
     *
     * @param gamma camera gamma
     */
    protected fun setGamma(gamma: Int) {
        getCurrentCamera()?.let { camera ->
            if (camera !is CameraUVC) {
                return
            }
            camera.setGamma(gamma)
        }
    }

    /**
     * Get gamma
     *
     * @return current gamma value
     */
    protected fun getGamma(): Int? {
        return getCurrentCamera()?.let { camera ->
            if (camera !is CameraUVC) {
                return@let null
            }
            camera.getGamma()
        }
    }

    /**
     * Reset gamma
     */
    protected fun resetGamma() {
        getCurrentCamera()?.let { camera ->
            if (camera !is CameraUVC) {
                return
            }
            camera.resetGamma()
        }
    }

    /**
     * Set hue
     *
     * @param hue camera hue
     */
    protected fun setHue(hue: Int) {
        getCurrentCamera()?.let { camera ->
            if (camera !is CameraUVC) {
                return
            }
            camera.setHue(hue)
        }
    }

    /**
     * Get hue
     *
     * @return current hue value
     */
    protected fun getHue(): Int? {
        return getCurrentCamera()?.let { camera ->
            if (camera !is CameraUVC) {
                return@let null
            }
            camera.getHue()
        }
    }

    /**
     * Reset hue
     */
    protected fun resetHue() {
        getCurrentCamera()?.let { camera ->
            if (camera !is CameraUVC) {
                return
            }
            camera.resetHue()
        }
    }

    /**
     * Set zoom
     *
     * @param zoom camera zoom
     */
    protected fun setZoom(zoom: Int) {
        getCurrentCamera()?.let { camera ->
            if (camera !is CameraUVC) {
                return
            }
            camera.setZoom(zoom)
        }
    }

    /**
     * Get hue
     *
     * @return current hue value
     */
    protected fun getZoom(): Int? {
        return getCurrentCamera()?.let { camera ->
            if (camera !is CameraUVC) {
                return@let null
            }
            camera.getZoom()
        }
    }

    /**
     * Reset hue
     */
    protected fun resetZoom() {
        getCurrentCamera()?.let { camera ->
            if (camera !is CameraUVC) {
                return
            }
            camera.resetZoom()
        }
    }

    /**
     * Set sharpness
     *
     * @param sharpness camera sharpness
     */
    protected fun setSharpness(sharpness: Int) {
        getCurrentCamera()?.let { camera ->
            if (camera !is CameraUVC) {
                return
            }
            camera.setSharpness(sharpness)
        }
    }

    /**
     * Get sharpness
     *
     * @return current sharpness value
     */
    protected fun getSharpness(): Int? {
        return getCurrentCamera()?.let { camera ->
            if (camera !is CameraUVC) {
                return@let null
            }
            camera.getSharpness()
        }
    }

    /**
     * Reset sharpness
     */
    protected fun resetSharpness() {
        getCurrentCamera()?.let { camera ->
            if (camera !is CameraUVC) {
                return
            }
            camera.resetSharpness()
        }
    }

    /**
     * Set saturation
     *
     * @param saturation camera saturation
     */
    protected fun setSaturation(saturation: Int) {
        getCurrentCamera()?.let { camera ->
            if (camera !is CameraUVC) {
                return
            }
            camera.setSaturation(saturation)
        }
    }

    /**
     * Get saturation
     *
     * @return current saturation value
     */
    protected fun getSaturation(): Int? {
        return getCurrentCamera()?.let { camera ->
            if (camera !is CameraUVC) {
                return@let null
            }
            camera.getSaturation()
        }
    }

    /**
     * Reset saturation
     */
    protected fun resetSaturation() {
        getCurrentCamera()?.let { camera ->
            if (camera !is CameraUVC) {
                return
            }
            camera.resetSaturation()
        }
    }

    protected fun openCamera(st: IAspectRatio? = null) {
        when (st) {
            is TextureView, is SurfaceView -> {
                st
            }
            else -> {
                null
            }
        }.apply {
            getCurrentCamera()?.openCamera(this, getCameraRequest())
            getCurrentCamera()?.setCameraStateCallBack(this@CameraActivity)
        }
    }

    protected fun closeCamera() {
        getCurrentCamera()?.closeCamera()
        getCurrentCamera()?.setCameraStateCallBack(null)
    }

    private fun surfaceSizeChanged(surfaceWidth: Int, surfaceHeight: Int) {
        getCurrentCamera()?.setRenderSize(surfaceWidth, surfaceHeight)
    }

    private fun getViewLayoutParams(viewGroup: ViewGroup): ViewGroup.LayoutParams {
        return when(viewGroup) {
            is FrameLayout -> {
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    getGravity()
                )
            }
            is LinearLayout -> {
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                ).apply {
                    gravity = getGravity()
                }
            }
            is RelativeLayout -> {
                RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT
                ).apply{
                    when(getGravity()) {
                        Gravity.TOP -> {
                            addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE)
                        }
                        Gravity.BOTTOM -> {
                            addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                        }
                        else -> {
                            addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE)
                            addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE)
                        }
                    }
                }
            }
            else -> throw IllegalArgumentException("Unsupported container view, " +
                    "you can use FrameLayout or LinearLayout or RelativeLayout")
        }
    }

    /**
     * Get camera view
     *
     * @return CameraView, such as AspectRatioTextureView etc.
     */
    protected abstract fun getCameraView(): IAspectRatio?

    /**
     * Get camera view container
     *
     * @return camera view container, such as FrameLayout ect
     */
    protected abstract fun getCameraViewContainer(): ViewGroup?

    /**
     * Camera render view show gravity
     */
    protected open fun getGravity() = Gravity.CENTER

    protected open fun getCameraRequest(): CameraRequest {
        return CameraRequest.Builder()
            .setPreviewWidth(1280)
            .setPreviewHeight(720)
            .setRenderMode(CameraRequest.RenderMode.OPENGL)
            .setDefaultRotateType(RotateType.ANGLE_0)
            .setAudioSource(CameraRequest.AudioSource.SOURCE_AUTO)
            .setAspectRatioShow(false)
            .setCaptureRawImage(false)
            .setRawPreviewData(false)
            .setDefaultEffect(EffectBlackWhite(this))
            .create();
    }

    companion object {
        private const val TAG = "CameraActivity"
    }
}