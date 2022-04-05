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
package com.jiangdg.media.camera

import android.content.ContentValues
import android.content.Context
import android.hardware.usb.UsbDevice
import android.os.Build
import android.provider.MediaStore
import com.jiangdg.media.utils.SettableFuture
import com.jiangdg.media.R
import com.jiangdg.media.callback.IPreviewDataCallBack
import com.jiangdg.media.camera.bean.CameraStatus
import com.jiangdg.media.camera.bean.CameraUvcInfo
import com.jiangdg.media.camera.bean.PreviewSize
import com.jiangdg.media.utils.Logger
import com.jiangdg.media.utils.MediaUtils
import com.jiangdg.media.utils.Utils
import com.jiangdg.natives.YUVUtils
import com.serenegiant.usb.DeviceFilter
import com.serenegiant.usb.IFrameCallback
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.UVCCamera
import java.io.File
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit

/** UVC Camera usage
 *
 * @author Created by jiangdg on 2021/12/20
 */
class CameraUvcStrategy(ctx: Context) : ICameraStrategy(ctx) {
    private val mDevSettableFuture: SettableFuture<UsbDevice?> by lazy {
        SettableFuture()
    }
    private val mCtrlBlockSettableFuture: SettableFuture<USBMonitor.UsbControlBlock?> by lazy {
        SettableFuture()
    }
    private val mNV21DataQueue: LinkedBlockingDeque<ByteArray> by lazy {
        LinkedBlockingDeque(MAX_NV21_DATA)
    }
    private var mUsbMonitor: USBMonitor? = null
    private var mUVCCamera: UVCCamera? = null
    private var mDevConnectCallBack: IDeviceConnectCallBack? = null
    private var mCacheDeviceList: MutableList<UsbDevice> = arrayListOf()

    init {
        mUsbMonitor = USBMonitor(getContext(), object : USBMonitor.OnDeviceConnectListener {
            override fun onAttach(device: UsbDevice?) {
                if (!mCacheDeviceList.contains(device)) {
                    device?.let {
                        mCacheDeviceList.add(it)
                    }
                }
                requestCameraPermission(device)
                mDevConnectCallBack?.onAttachDev(device)
                if (Utils.debugCamera) {
                    Logger.i(TAG, "attach device = ${device?.toString()}")
                }
            }

            override fun onDetach(device: UsbDevice?) {
                mDevConnectCallBack?.onDetachDec(device)
                if (mCacheDeviceList.contains(device)) {
                    mCacheDeviceList.remove(device)
                }
                mDevSettableFuture.set(null)
                mCtrlBlockSettableFuture.set(null)
                if (Utils.debugCamera) {
                    Logger.i(TAG, "onDetach device = ${device?.toString()}")
                }
            }

            override fun onConnect(
                device: UsbDevice?,
                ctrlBlock: USBMonitor.UsbControlBlock?,
                createNew: Boolean
            ) {
                startPreview(null, null)
                mDevSettableFuture.set(device)
                mCtrlBlockSettableFuture.set(ctrlBlock)
                if (Utils.debugCamera) {
                    Logger.i(TAG, "onConnect device = ${device?.toString()}")
                }
            }

            override fun onDisconnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
                val curDevice = mDevSettableFuture.get()
                if (curDevice?.deviceId != device?.deviceId) {
                    return
                }
                stopPreview()
                mDevConnectCallBack?.onDisConnectDec(device)
                if (Utils.debugCamera) {
                    Logger.i(TAG, "onDisconnect device = ${device?.toString()}")
                }
            }

            override fun onCancel(device: UsbDevice?) {
                val curDevice = mDevSettableFuture.get()
                if (curDevice?.deviceId != device?.deviceId) {
                    return
                }
                stopPreview()
                mDevConnectCallBack?.onDisConnectDec(device)
                if (Utils.debugCamera) {
                    Logger.i(TAG, "onCancel device = ${device?.toString()}")
                }
            }
        })
        register()
    }

    override fun loadCameraInfo() {
        try {
            val devList = getUsbDeviceListInternal()
            if (devList.isNullOrEmpty()) {
                Logger.e(TAG, "Find no uvc devices.")
                return
            }
            loadCameraInfoInternal(devList)
        } catch (e: Exception) {
            Logger.e(TAG, "Find no uvc devices, err = ${e.localizedMessage}", e)
        }
    }

    private fun loadCameraInfoInternal(devList: MutableList<UsbDevice>) {
        mCameraInfoMap.clear()
        devList.forEach { dev ->
            val cameraInfo = CameraUvcInfo(dev.deviceId.toString()).apply {
                cameraVid = dev.vendorId
                cameraPid = dev.productId
                cameraName = dev.deviceName
                cameraProtocol = dev.deviceProtocol
                cameraClass = dev.deviceClass
                cameraSubClass = dev.deviceSubclass
                cameraPreviewSizes
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    cameraProductName = dev.productName
                    cameraManufacturerName = dev.manufacturerName
                }
            }
            mCameraInfoMap[dev.deviceId] = cameraInfo
        }
        if (Utils.debugCamera) {
            Logger.i(TAG, "loadCameraInfo success, camera = $mCameraInfoMap")
        }
    }

    override fun startPreviewInternal() {
        postCameraStatus(CameraStatus(CameraStatus.STOP))
        createCamera()
        realStartPreview()
        mDevSettableFuture.get().apply {
            mDevConnectCallBack?.onConnectDev(this)
        }?.also {
            mCameraInfoMap[it.deviceId]?.let { cameraInfo ->
                cameraInfo.cameraPreviewSizes?.clear()
                getAllPreviewSizes()?.apply {
                    cameraInfo.cameraPreviewSizes?.addAll(this)
                }
            }
            if (Utils.debugCamera) {
                Logger.i(
                    TAG,
                    "start preview success!!!, id = ${it.deviceId}, name = ${it.deviceName}"
                )
            }
        }
    }

    private fun createCamera() {
        val ctrlBlock = mCtrlBlockSettableFuture.get()
        val device = mDevSettableFuture.get()
        getRequest()?.let { request ->
            val previewSize = getSuitableSize(request.previewWidth, request.previewHeight)
            try {
                val camera = UVCCamera().apply {
                    open(ctrlBlock)
                }
                mUVCCamera = camera
                request.cameraId = device?.deviceId.toString()
                mUVCCamera?.setPreviewSize(
                    previewSize.width,
                    previewSize.height,
                    MIN_FS,
                    MAX_FS,
                    UVCCamera.FRAME_FORMAT_YUYV,
                    UVCCamera.DEFAULT_BANDWIDTH
                )
                mUVCCamera?.setFrameCallback(frameCallBack, UVCCamera.PIXEL_FORMAT_YUV420SP)
                Logger.i(TAG, "createCamera request = $request")
            } catch (e: Exception) {
                try {
                    // fallback to YUV mode
                    mUVCCamera?.setPreviewSize(
                        previewSize.width,
                        previewSize.height,
                        MIN_FS,
                        MAX_FS,
                        UVCCamera.DEFAULT_PREVIEW_MODE,
                        UVCCamera.DEFAULT_BANDWIDTH
                    )
                } catch (e1: IllegalArgumentException) {
                    Logger.e(TAG, "createCamera failed, err = ${e.localizedMessage}", e)
                }
                postCameraStatus(CameraStatus(CameraStatus.ERROR, e.localizedMessage))
            }
        }
    }

    private fun realStartPreview() {
        try {
            val st = getSurfaceTexture()
            val holder = getSurfaceHolder()
            if (st == null && holder == null) {
                postCameraStatus(CameraStatus(CameraStatus.ERROR, "surface is null"))
                Logger.e(
                    TAG,
                    "realStartPreview failed, SurfaceTexture or SurfaceHolder cannot be null."
                )
                return
            }
            if (st != null) {
                mUVCCamera?.setPreviewTexture(st)
            } else {
                mUVCCamera?.setPreviewDisplay(holder)
            }
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
            if (Utils.debugCamera) {
                Logger.i(TAG, "realStartPreview...")
            }
        } catch (e: Exception) {
            postCameraStatus(CameraStatus(CameraStatus.ERROR, e.localizedMessage))
            Logger.e(TAG, "realStartPreview, err = ${e.localizedMessage}", e)
        }
    }

    override fun stopPreviewInternal() {
        if (Utils.debugCamera && mIsPreviewing.get()) {
            Logger.i(TAG, "stopPreviewInternal")
        }
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
            YUVUtils.yuv420spToNv21(data, width, height)
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
        getRequest()?.let { request ->
            if (cameraId.isNullOrEmpty()) {
                Logger.e(TAG, "camera id invalid.")
                return@let
            }
            val curDevice = mDevSettableFuture.get()
            if (curDevice?.deviceId?.toString() == cameraId) {
                Logger.e(TAG, "camera was already opened.")
                return@let
            }
            mCacheDeviceList.find {
                cameraId == it.deviceId.toString()
            }.also { dev ->
                if (dev == null) {
                    Logger.e(TAG, "switch camera failed, not found.")
                    return@also
                }
                mDevSettableFuture.set(dev)
                stopPreviewInternal()
                startPreviewInternal()
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
            val list = mutableListOf<PreviewSize>()
            val cameraInfo = mCameraInfoMap.values.find {
                request.cameraId == it.cameraId
            }
            val previewSizeList = cameraInfo?.cameraPreviewSizes ?: mutableListOf()
            if (previewSizeList.isEmpty()) {
                mUVCCamera?.supportedSizeList?.forEach { size ->
                    list.add(PreviewSize(size.width, size.height))
                }
                previewSizeList.addAll(list)
            }
            list.clear()
            previewSizeList.forEach { size ->
                val width = size.width
                val height = size.height
                val ratio = width.toDouble() / height
                if (aspectRatio == null || ratio == aspectRatio) {
                    list.add(size)
                }
            }
            Logger.i(TAG, "getAllPreviewSizes aspect ratio = $aspectRatio, list= $list")
            return list
        }
        return null
    }

    override fun release() {
        mUsbMonitor?.destroy()
        mUsbMonitor = null
    }

    override fun register() {
        if (mUsbMonitor?.isRegistered == true) {
            return
        }
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
        if (Utils.debugCamera) {
            Logger.i(TAG, "unRegister uvc device monitor")
        }
    }

    /**
     * Add device connect status call back
     *
     * @param cb see [IDeviceConnectCallBack]]
     */
    fun addDeviceConnectCallBack(cb: IDeviceConnectCallBack) {
        this.mDevConnectCallBack = cb
    }

    /**
     * Get usb device list
     *
     * @param resId device filter regular, like [R.xml.default_device_filter]
     * @return uvc device list
     */
    fun getUsbDeviceList(resId: Int? = null): MutableList<UsbDevice> {
        val deviceList = getUsbDeviceListInternal() ?: arrayListOf()
        if (resId != null) {
            getUsbDeviceListInternal(resId)?.let {
                deviceList.addAll(it)
            }
        }
        mCacheDeviceList.clear()
        mCacheDeviceList.addAll(deviceList)
        loadCameraInfoInternal(deviceList)
        return deviceList
    }

    private fun getUsbDeviceListInternal(resId: Int = R.xml.default_device_filter): MutableList<UsbDevice>? {
        val deviceFilters = DeviceFilter.getDeviceFilters(getContext(), resId)
        if (mUsbMonitor == null || deviceFilters == null) {
            Logger.e(TAG, "getUsbDeviceList failed, usbMonitor or deviceFilters is null.")
            return null
        }
        return mUsbMonitor?.getDeviceList(deviceFilters)?.apply {
            mCacheDeviceList.clear()
            mCacheDeviceList.addAll(this)
        }
    }

    private fun requestCameraPermission(device: UsbDevice?) {
        if (device == null) {
            Logger.e(TAG, "open camera failed, device is null.")
            return
        }
        mCacheDeviceList.find {
            device.deviceId == it.deviceId
        }.also { dev ->
            if (dev == null) {
                Logger.e(TAG, "open camera failed, not found.")
                return@also
            }
            mUsbMonitor?.requestPermission(dev)
        }
    }

    private fun getSuitableSize(
        maxWidth: Int,
        maxHeight: Int
    ): PreviewSize {
        val aspectRatio = maxWidth.toFloat() / maxHeight
        val sizeList = getAllPreviewSizes(aspectRatio.toDouble())
        sizeList?.forEach { size ->
            val w = size.width
            val h = size.height
            val ratio = w.toFloat() / h
            if (ratio == aspectRatio && w <= maxWidth && h <= maxHeight) {
                return PreviewSize(w, h)
            }
        }
        return if (sizeList.isNullOrEmpty()) {
            PreviewSize(maxWidth, maxHeight)
        } else {
            PreviewSize(sizeList[0].width, sizeList[0].height)
        }
    }

    private val frameCallBack = IFrameCallback { frame ->
        mPreviewDataCbList.forEach { cb ->
            frame?.apply {
                val data = ByteArray(capacity())
                get(data)
                cb.onPreviewData(data, IPreviewDataCallBack.DataFormat.NV21)
                if (mNV21DataQueue.size >= MAX_NV21_DATA) {
                    mNV21DataQueue.removeLast()
                }
                mNV21DataQueue.offerFirst(data)
            }
        }
    }

    /**
     * I device connect call back
     *
     * @constructor Create empty I device connect call back
     */
    interface IDeviceConnectCallBack {
        /**
         * On attach dev
         *
         * @param device uvc device
         */
        fun onAttachDev(device: UsbDevice?)

        /**
         * On detach dev
         *
         * @param device uvc device
         */
        fun onDetachDec(device: UsbDevice?)

        /**
         * On connect dev
         *
         * @param device uvc device
         */
        fun onConnectDev(device: UsbDevice?)

        /**
         * On dis connect dev
         *
         * @param device uvc device
         */
        fun onDisConnectDec(device: UsbDevice?)
    }

    companion object {
        private const val TAG = "CameraUvc"
        private const val MIN_FS = 10
        private const val MAX_FS = 60
        private const val MAX_NV21_DATA = 5
        private const val CAPTURE_TIMES_OUT_SEC = 1L
    }
}