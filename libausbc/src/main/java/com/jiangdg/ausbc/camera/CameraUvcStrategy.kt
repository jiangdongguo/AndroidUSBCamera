/*
 * Copyright 2017-2022 Jiangdg
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
package com.jiangdg.ausbc.camera

import android.content.ContentValues
import android.content.Context
import android.hardware.usb.UsbDevice
import android.os.Build
import android.provider.MediaStore
import com.jiangdg.ausbc.R
import com.jiangdg.ausbc.callback.IDeviceConnectCallBack
import com.jiangdg.ausbc.callback.IPreviewDataCallBack
import com.jiangdg.ausbc.camera.bean.CameraStatus
import com.jiangdg.ausbc.camera.bean.CameraUvcInfo
import com.jiangdg.ausbc.camera.bean.PreviewSize
import com.jiangdg.ausbc.utils.*
import com.jiangdg.ausbc.utils.CameraUtils.isFilterDevice
import com.jiangdg.ausbc.utils.CameraUtils.isUsbCamera
import com.jiangdg.usb.DeviceFilter
import com.jiangdg.usb.USBMonitor
import com.jiangdg.uvc.IFrameCallback
import com.jiangdg.uvc.UVCCamera
import java.io.File
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.Exception

/** UVC Camera usage
 *
 * @author Created by jiangdg on 2021/12/20
 *
 * Deprecated since version 3.3.0, and it will be deleted in the future.
 * I recommend using the [CameraUVC] API for your application.
 */
@kotlin.Deprecated("Deprecated since version 3.3.0")
class CameraUvcStrategy(ctx: Context) : ICameraStrategy(ctx) {
    private var mDevSettableFuture: SettableFuture<UsbDevice?>? = null
    private var mCtrlBlockSettableFuture: SettableFuture<USBMonitor.UsbControlBlock?>? = null
    private val mConnectSettableFuture: SettableFuture<Boolean> = SettableFuture()
    private val mNV21DataQueue: LinkedBlockingDeque<ByteArray> by lazy {
        LinkedBlockingDeque(MAX_NV21_DATA)
    }
    private val mRequestPermission: AtomicBoolean by lazy {
        AtomicBoolean(false)
    }
    private var mUsbMonitor: USBMonitor? = null
    private var mUVCCamera: UVCCamera? = null
    private var mDevConnectCallBack: IDeviceConnectCallBack? = null
    private var mCacheDeviceList: MutableList<UsbDevice> = arrayListOf()

    init {
        register()
    }

    override fun loadCameraInfo() {
        try {
            val devList = getUsbDeviceListInternal()
            if (devList.isNullOrEmpty()) {
                val emptyTip = "Find no uvc devices, " +
                        "if you want some special device please use getUsbDeviceList() " +
                        "or add device info into default_device_filter.xml"
                postCameraStatus(
                    CameraStatus(
                        CameraStatus.ERROR,
                        emptyTip
                    )
                )
                Logger.e(TAG, emptyTip)
                return
            }
            devList.forEach { dev ->
                loadCameraInfoInternal(dev)
            }
        } catch (e: Exception) {
            Logger.e(TAG, " Find no uvc devices, err = ${e.localizedMessage}", e)
        }
    }

    private fun loadCameraInfoInternal(dev: UsbDevice) {
        if (mCameraInfoMap.containsKey(dev.deviceId)) {
            return
        }
        val cameraInfo = CameraUvcInfo(dev.deviceId.toString()).apply {
            cameraVid = dev.vendorId
            cameraPid = dev.productId
            cameraName = dev.deviceName
            cameraProtocol = dev.deviceProtocol
            cameraClass = dev.deviceClass
            cameraSubClass = dev.deviceSubclass
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cameraProductName = dev.productName
                cameraManufacturerName = dev.manufacturerName
            }
        }
        mCameraInfoMap[dev.deviceId] = cameraInfo
    }

    override fun startPreviewInternal() {
        try {
            createCamera()
            realStartPreview()
        } catch (e: Exception) {
            stopPreview()
            Logger.e(TAG, " preview failed, err = ${e.localizedMessage}", e)
            postCameraStatus(CameraStatus(CameraStatus.ERROR, e.localizedMessage))
        }
    }

    private fun createCamera(): Boolean? {
        val ctrlBlock = mCtrlBlockSettableFuture?.get()
        val device = mDevSettableFuture?.get()
        device ?: return null
        ctrlBlock ?: return null
        getRequest()?.let { request ->
            val previewWidth = request.previewWidth
            val previewHeight = request.previewHeight
            request.cameraId = device.deviceId.toString()
            mUVCCamera = UVCCamera().apply {
                open(ctrlBlock)
            }
            if (! isPreviewSizeSupported(previewWidth, previewHeight)) {
                postCameraStatus(CameraStatus(CameraStatus.ERROR_PREVIEW_SIZE, "unsupported preview size(${request.previewWidth}, ${request.previewHeight})"))
                Logger.e(TAG, " unsupported preview size(${request.previewWidth}, ${request.previewHeight})")
                return null
            }
            try {
                mUVCCamera?.setPreviewSize(
                    request.previewWidth,
                    request.previewHeight,
                    MIN_FS,
                    MAX_FS,
                    UVCCamera.FRAME_FORMAT_MJPEG,
                    UVCCamera.DEFAULT_BANDWIDTH
                )
            } catch (e: Exception) {
                try {
                    Logger.w(TAG, " setPreviewSize failed ${e.localizedMessage}, try yuv format...")
                    if (! isPreviewSizeSupported(previewWidth, previewHeight)) {
                        postCameraStatus(CameraStatus(CameraStatus.ERROR_PREVIEW_SIZE, "unsupported preview size(${request.previewWidth}, ${request.previewHeight})"))
                        Logger.e(TAG, " unsupported preview size(${request.previewWidth}, ${request.previewHeight})")
                        return null
                    }
                    mUVCCamera?.setPreviewSize(
                        request.previewWidth,
                        request.previewHeight,
                        MIN_FS,
                        MAX_FS,
                        UVCCamera.FRAME_FORMAT_YUYV,
                        UVCCamera.DEFAULT_BANDWIDTH
                    )
                } catch (e: Exception) {
                    postCameraStatus(CameraStatus(CameraStatus.ERROR, "setPreviewSize failed, err = ${e.localizedMessage}"))
                    Logger.e(TAG, " setPreviewSize failed", e)
                    return null
                }
            }
            mUVCCamera?.setFrameCallback(frameCallBack, UVCCamera.PIXEL_FORMAT_YUV420SP)
            Logger.i(TAG, " createCamera success! request = $request")
        }
        return true
    }

    private fun isPreviewSizeSupported(previewWidth: Int, previewHeight: Int): Boolean {
        return getAllPreviewSizes()?.find {
            it.width == previewWidth && it.height == previewHeight
        } != null
    }

    private fun realStartPreview(): Boolean? {
        try {
            val st = getSurfaceTexture()
            val holder = getSurfaceHolder()
            if (st == null && holder == null) {
                postCameraStatus(CameraStatus(CameraStatus.ERROR, "surface is null"))
                Logger.e(TAG, " SurfaceTexture or SurfaceHolder cannot be null.")
                return null
            }
            if (st != null) {
                mUVCCamera?.setPreviewTexture(st)
            } else {
                mUVCCamera?.setPreviewDisplay(holder)
            }
            mUVCCamera?.autoFocus = true
            mUVCCamera?.autoWhiteBlance = true
            mUVCCamera?.startPreview()
            mUVCCamera?.updateCameraParams()
            mIsPreviewing.set(true)
            getRequest()?.apply {
                postCameraStatus(
                    CameraStatus(
                        CameraStatus.START,
                        Pair(previewWidth, previewHeight).toString()
                    )
                )
            }
            val dev = mDevSettableFuture?.get().apply {
                mDevConnectCallBack?.onConnectDev(this)
            }
            if (Utils.debugCamera) {
                Logger.i(TAG, " start preview success!!!, id(${dev?.deviceName}")
            }
        } catch (e: Exception) {
            postCameraStatus(CameraStatus(CameraStatus.ERROR, e.localizedMessage))
            Logger.e(TAG, " startPreview failed. err = ${e.localizedMessage}", e)
            return null
        }
        return true
    }

    override fun stopPreviewInternal() {
        if (Utils.debugCamera && mIsPreviewing.get()) {
            Logger.i(TAG, "stopPreviewInternal")
        }
        mRequestPermission.set(false)
        mIsPreviewing.set(false)
        mUVCCamera?.stopPreview()
        mUVCCamera?.destroy()
        mUVCCamera = null
        postCameraStatus(CameraStatus(CameraStatus.STOP))
    }

    override fun captureImageInternal(savePath: String?) {
        if (!hasCameraPermission() || !hasStoragePermission()) {
            mMainHandler.post {
                mCaptureDataCb?.onError("Have no storage or camera permission.")
            }
            Logger.i(TAG, "captureImageInternal failed, has no storage/camera permission.")
            return
        }
        if (mIsCapturing.get()) {
            return
        }
        mSaveImageExecutor.submit {
            val data = mNV21DataQueue.pollFirst(CAPTURE_TIMES_OUT_SEC, TimeUnit.SECONDS)
            if (data == null || getRequest() == null) {
                mMainHandler.post {
                    mCaptureDataCb?.onError("Times out or camera request is null")
                }
                Logger.i(TAG, "captureImageInternal failed, times out.")
                return@submit
            }
            mIsCapturing.set(true)
            mMainHandler.post {
                mCaptureDataCb?.onBegin()
            }
            val date = mDateFormat.format(System.currentTimeMillis())
            val title = savePath ?: "IMG_JJCamera_$date"
            val displayName = savePath ?: "$title.jpg"
            val path = savePath ?: "$mCameraDir/$displayName"
            val orientation = 0
            val location = Utils.getGpsLocation(getContext())
            val width = getRequest()!!.previewWidth
            val height = getRequest()!!.previewHeight
            val ret = MediaUtils.saveYuv2Jpeg(path, data, width, height)
            if (!ret) {
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
                mMainHandler.post {
                    mCaptureDataCb?.onError("save yuv to jpeg failed.")
                }
                Logger.w(TAG, "save yuv to jpeg failed.")
                return@submit
            }
            val values = ContentValues()
            values.put(MediaStore.Images.ImageColumns.TITLE, title)
            values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, displayName)
            values.put(MediaStore.Images.ImageColumns.DATA, path)
            values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, date)
            values.put(MediaStore.Images.ImageColumns.ORIENTATION, orientation)
            values.put(MediaStore.Images.ImageColumns.LONGITUDE, location?.longitude)
            values.put(MediaStore.Images.ImageColumns.LATITUDE, location?.latitude)
            getContext()?.contentResolver?.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )
            mMainHandler.post {
                mCaptureDataCb?.onComplete(path)
            }
            mIsCapturing.set(false)
            if (Utils.debugCamera) {
                Logger.i(TAG, "captureImageInternal save path = $path")
            }
        }
    }

    override fun switchCameraInternal(cameraId: String?) {
        getRequest()?.let {
            if (Utils.debugCamera) {
                Logger.i(TAG, "switchCameraInternal, camera id = $cameraId")
            }
            if (cameraId.isNullOrEmpty()) {
                Logger.e(TAG, "camera id invalid.")
                return@let
            }
            if (getCurrentDevice()?.deviceId?.toString() == cameraId) {
                Logger.e(TAG, "camera was already opened.")
                return@let
            }
            getUsbDeviceList()?.find {
                cameraId == it.deviceId.toString()
            }.also { dev ->
                if (dev == null) {
                    Logger.e(TAG, "switch camera(: $cameraId) failed, not found.")
                    return@also
                }
                if (!mCacheDeviceList.contains(dev)) {
                    mCacheDeviceList.add(dev)
                }
                stopPreviewInternal()
                requestCameraPermission(dev)
            }
        }
    }

    override fun updateResolutionInternal(width: Int, height: Int) {
        getRequest()?.let { request ->
            request.previewWidth = width
            request.previewHeight = height
            stopPreviewInternal()
            startPreviewInternal()
        }
    }

    override fun getAllPreviewSizes(aspectRatio: Double?): MutableList<PreviewSize>? {
        getRequest()?.let { request ->
            val cameraInfo = mCameraInfoMap.values.find {
                request.cameraId == it.cameraId
            }
            val previewSizeList = cameraInfo?.cameraPreviewSizes ?: mutableListOf()
            if (previewSizeList.isEmpty()) {
                Logger.i(TAG, "getAllPreviewSizes = ${mUVCCamera?.supportedSizeList}")
                if (mUVCCamera?.supportedSizeList?.isNotEmpty() == true) {
                    mUVCCamera?.supportedSizeList
                } else {
                    mUVCCamera?.getSupportedSizeList(UVCCamera.FRAME_FORMAT_YUYV)
                }.also { sizeList ->
                    sizeList?.forEach { size ->
                        previewSizeList.find {
                            it.width == size.width && it.height == size.height
                        }.also {
                            if (it == null) {
                                previewSizeList.add(PreviewSize(size.width, size.height))
                            }
                        }
                    }
                    cameraInfo?.cameraPreviewSizes = previewSizeList
                }
            }
            aspectRatio ?: return previewSizeList
            // aspect ratio list or all
            val aspectList = mutableListOf<PreviewSize>()
            aspectList.clear()
            cameraInfo?.cameraPreviewSizes?.forEach { size ->
                val width = size.width
                val height = size.height
                val ratio = width.toDouble() / height
                if (ratio == aspectRatio) {
                    aspectList.add(size)
                }
            }
            Logger.i(TAG, "getAllPreviewSizes aspectRatio = $aspectRatio, size = $aspectList")
            return aspectList
        }
        return null
    }

    override fun register() {
        if (mUsbMonitor?.isRegistered == true) {
            return
        }
        mUsbMonitor = USBMonitor(getContext(), object : USBMonitor.OnDeviceConnectListener {
            /**
             * Called by receive usb device inserted broadcast
             *
             * @param device usb device info,see [UsbDevice]
             */
            override fun onAttach(device: UsbDevice?) {
                if (Utils.debugCamera) {
                    Logger.i(TAG, "attach device = ${device?.toString()}")
                }
                device ?: return
                if (!isUsbCamera(device) && !isFilterDevice(getContext(), device)) {
                    return
                }
                if (!mCacheDeviceList.contains(device)) {
                    device.let {
                        mCacheDeviceList.add(it)
                    }
                    mDevConnectCallBack?.onAttachDev(device)
                }
                loadCameraInfoInternal(device)
                requestCameraPermission(device)
            }

            /**
             * Called by receive usb device pulled out broadcast
             *
             * @param device usb device info,see [UsbDevice]
             */
            override fun onDetach(device: UsbDevice?) {
                if (Utils.debugCamera) {
                    Logger.i(TAG, "onDetach device = ${device?.deviceName}")
                }
                if (!isUsbCamera(device) && !isFilterDevice(getContext(), device) && !mCacheDeviceList.contains(device)) {
                    return
                }
                mCameraInfoMap.remove(device?.deviceId)
                mDevConnectCallBack?.onDetachDec(device)
                if (mCacheDeviceList.contains(device)) {
                    mCacheDeviceList.remove(device)
                }
                // 重置正在打开的设备
                val dev = mDevSettableFuture?.get()
                if (dev?.deviceId == device?.deviceId) {
                    mRequestPermission.set(false)
                }
            }

            /**
             * Called by granted permission
             *
             * @param device usb device info,see [UsbDevice]
             */
            override fun onConnect(
                device: UsbDevice?,
                ctrlBlock: USBMonitor.UsbControlBlock?,
                createNew: Boolean
            ) {
                if (Utils.debugCamera) {
                    Logger.i(TAG, "onConnect device = ${device?.deviceName}")
                }
                if (!isUsbCamera(device) && !isFilterDevice(getContext(), device) && !mCacheDeviceList.contains(device)) {
                    return
                }
                mDevSettableFuture = SettableFuture()
                mCtrlBlockSettableFuture = SettableFuture()
                getRequest()?.apply {
                    if (getSurfaceTexture() != null) {
                        startPreview(this, getSurfaceTexture())
                    } else {
                        startPreview(this, getSurfaceHolder())
                    }
                }
                mDevSettableFuture?.set(device)
                mCtrlBlockSettableFuture?.set(ctrlBlock)
                mConnectSettableFuture.set(true)
            }

            /**
             * Called by dis unauthorized permission
             *
             * @param device usb device info,see [UsbDevice]
             */
            override fun onDisconnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
                if (Utils.debugCamera) {
                    Logger.i(TAG, "onDisconnect device = ${device?.deviceName}")
                }
                if (!isUsbCamera(device) && !isFilterDevice(getContext(), device) && !mCacheDeviceList.contains(device)) {
                    return
                }
                val curDevice = mDevSettableFuture?.get()
                if (curDevice?.deviceId != device?.deviceId) {
                    return
                }
                stopPreview()
                mDevConnectCallBack?.onDisConnectDec(device, ctrlBlock)
                mConnectSettableFuture.set(false)
            }

            /**
             * Called by dis unauthorized permission  or request permission exception
             *
             * @param device usb device info,see [UsbDevice]
             */
            override fun onCancel(device: UsbDevice?) {
                if (Utils.debugCamera) {
                    Logger.i(TAG, "onCancel device = ${device?.deviceName}")
                }
                if (!isUsbCamera(device) && !isFilterDevice(getContext(), device) && !mCacheDeviceList.contains(device)) {
                    return
                }
                val curDevice = mDevSettableFuture?.get()
                if (curDevice?.deviceId != device?.deviceId) {
                    return
                }
                stopPreview()
                mDevConnectCallBack?.onDisConnectDec(device)
            }
        })
        mUsbMonitor?.register()
        if (Utils.debugCamera) {
            Logger.i(TAG, "register uvc device monitor")
        }
    }

    override fun unRegister() {
        if (mUsbMonitor?.isRegistered == false) {
            return
        }
        mUsbMonitor?.unregister()
        mUsbMonitor?.destroy()
        mUsbMonitor = null
        if (Utils.debugCamera) {
            Logger.i(TAG, "unRegister uvc device monitor")
        }
    }

    /**
     * set device connect status call back
     *
     * @param cb see [IDeviceConnectCallBack]]
     */
    fun setDeviceConnectStatusListener(cb: IDeviceConnectCallBack) {
        this.mDevConnectCallBack = cb
    }

    /**
     * Get usb device list
     *
     * @param resId device filter regular, like [R.xml.default_device_filter]
     *         null means all usb devices, more than uvc devices
     * @return device list
     */
    fun getUsbDeviceList(resId: Int? = null): MutableList<UsbDevice>? {
        return mUsbMonitor?.deviceList?.let { usbDevList ->
            val list = arrayListOf<UsbDevice>()
            if (resId == null) {
                null
            } else {
                DeviceFilter.getDeviceFilters(getContext(), resId)
            }.also { filterList ->
                if (filterList == null) {
                    list.addAll(usbDevList)
                    return@also
                }
                usbDevList.forEach { dev ->
                    val filterDev = filterList.find {
                        it.mProductId == dev?.productId && it.mVendorId == dev.vendorId
                    }
                    if (filterDev != null) {
                        list.add(dev)
                    }
                }
            }
            list
        }
    }

    /**
     * Get current device in 1 seconds
     *
     * @return current opened [UsbDevice]
     */
    fun getCurrentDevice(): UsbDevice? {
        return try {
            val isConnected = mConnectSettableFuture.get(3, TimeUnit.SECONDS)
            if (isConnected != true) {
                return null
            }
            mDevSettableFuture?.get(1, TimeUnit.SECONDS)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get current usb control block
     *
     * @return USBMonitor.UsbControlBlock
     */
    fun getUsbControlBlock(): USBMonitor.UsbControlBlock? {
        return try {
            val isConnected = mConnectSettableFuture.get(3, TimeUnit.SECONDS)
            if (isConnected != true) {
                return null
            }
            mCtrlBlockSettableFuture?.get(1, TimeUnit.SECONDS)
        } catch (e: Exception) {
            Logger.w(TAG, "get current usb control block times out")
            null
        }
    }

    /**
     * Is mic supported
     *
     * @return true camera support mic
     */
    fun isMicSupported() = CameraUtils.isCameraContainsMic(getCurrentDevice())

    /**
     * Send camera command
     *
     * @param command hex value
     * @return control result
     */
    fun sendCameraCommand(command: Int): Int? {
        return mUVCCamera?.sendCommand(command).apply {
            Logger.i(TAG, "send command ret = $this")
        }
    }

    /**
     * Set auto focus
     *
     * @param enable true enable auto focus
     */
    fun setAutoFocus(enable: Boolean) {
        mUVCCamera?.autoFocus = enable
    }

    /**
     * Set auto white balance
     *
     * @param autoWhiteBalance true enable auto white balance
     */
    fun setAutoWhiteBalance(autoWhiteBalance: Boolean) {
        mUVCCamera?.autoWhiteBlance = autoWhiteBalance
    }

    /**
     * Set zoom
     *
     * @param zoom zoom value, 0 means reset
     */
    fun setZoom(zoom: Int) {
        mUVCCamera?.zoom = zoom
    }

    /**
     * Get zoom
     */
    fun getZoom() = mUVCCamera?.zoom

    /**
     * Set gain
     *
     * @param gain gain value, 0 means reset
     */
    fun setGain(gain: Int) {
        mUVCCamera?.gain = gain
    }

    /**
     * Get gain
     */
    fun getGain() = mUVCCamera?.gain

    /**
     * Set gamma
     *
     * @param gamma gamma value, 0 means reset
     */
    fun setGamma(gamma: Int) {
        mUVCCamera?.gamma = gamma
    }

    /**
     * Get gamma
     */
    fun getGamma() = mUVCCamera?.gamma

    /**
     * Set brightness
     *
     * @param brightness brightness value, 0 means reset
     */
    fun setBrightness(brightness: Int) {
        mUVCCamera?.brightness = brightness
    }

    /**
     * Get brightness
     */
    fun getBrightness() = mUVCCamera?.brightness

    /**
     * Set contrast
     *
     * @param contrast contrast value, 0 means reset
     */
    fun setContrast(contrast: Int) {
        mUVCCamera?.contrast = contrast
    }

    /**
     * Get contrast
     */
    fun getContrast() = mUVCCamera?.contrast

    /**
     * Set sharpness
     *
     * @param sharpness sharpness value, 0 means reset
     */
    fun setSharpness(sharpness: Int) {
        mUVCCamera?.sharpness = sharpness
    }

    /**
     * Get sharpness
     */
    fun getSharpness() = mUVCCamera?.sharpness

    /**
     * Set saturation
     *
     * @param saturation saturation value, 0 means reset
     */
    fun setSaturation(saturation: Int) {
        mUVCCamera?.saturation = saturation
    }

    /**
     * Get saturation
     */
    fun getSaturation() = mUVCCamera?.saturation

    /**
     * Set hue
     *
     * @param hue hue value, 0 means reset
     */
    fun setHue(hue: Int) {
        mUVCCamera?.hue = hue
    }

    /**
     * Get hue
     */
    fun getHue() = mUVCCamera?.hue

    private fun getUsbDeviceListInternal(): MutableList<UsbDevice>? {
        return mUsbMonitor?.getDeviceList(arrayListOf<DeviceFilter>())?.let { devList ->
            mCacheDeviceList.clear()
            val devInfoList = ArrayList<String>();
            devList.forEach {
                devInfoList.add(it.deviceName)
                // check is camera or need device
                if (isUsbCamera(it) || isFilterDevice(getContext(), it)) {
                    mCacheDeviceList.add(it)
                }
            }
            Logger.i(TAG, " find some device list, = $devInfoList")
            mCacheDeviceList
        }
    }

    private fun requestCameraPermission(device: UsbDevice?) {
        if (mRequestPermission.get()) {
            return
        }
        mCacheDeviceList.find {
            device?.deviceId == it.deviceId
        }.also { dev ->
            if (dev == null) {
                Logger.e(TAG, "open camera failed, not found.")
                return@also
            }
            mRequestPermission.set(true)
            mUsbMonitor?.requestPermission(dev)
        }
    }

    private val frameCallBack = IFrameCallback { frame ->
        mPreviewDataCbList.forEach { cb ->
            frame?.apply {
                frame.position(0)
                val data = ByteArray(capacity())
                get(data)
                cb.onPreviewData(data, getRequest()!!.previewWidth, getRequest()!!.previewHeight,IPreviewDataCallBack.DataFormat.NV21)
                if (mNV21DataQueue.size >= MAX_NV21_DATA) {
                    mNV21DataQueue.removeLast()
                }
                mNV21DataQueue.offerFirst(data)
            }
        }
    }

    companion object {
        private const val TAG = "CameraUvc"
        private const val MIN_FS = 10
        private const val MAX_FS = 60
        private const val MAX_NV21_DATA = 5
        private const val CAPTURE_TIMES_OUT_SEC = 1L
    }
}