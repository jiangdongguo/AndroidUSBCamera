package com.jiangdg.ausbc

import android.content.ContentValues
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.usb.UsbDevice
import android.os.*
import android.provider.MediaStore
import android.view.Surface
import android.view.SurfaceView
import android.view.TextureView
import com.jiangdg.ausbc.callback.*
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.camera.bean.PreviewSize
import com.jiangdg.ausbc.encode.AACEncodeProcessor
import com.jiangdg.ausbc.encode.AbstractProcessor
import com.jiangdg.ausbc.encode.H264EncodeProcessor
import com.jiangdg.ausbc.encode.bean.RawData
import com.jiangdg.ausbc.encode.muxer.Mp4Muxer
import com.jiangdg.ausbc.utils.CameraUtils
import com.jiangdg.ausbc.utils.CameraUtils.isFilterDevice
import com.jiangdg.ausbc.utils.CameraUtils.isUsbCamera
import com.jiangdg.ausbc.utils.Logger
import com.jiangdg.ausbc.utils.MediaUtils
import com.jiangdg.ausbc.utils.Utils
import com.jiangdg.natives.YUVUtils
import com.serenegiant.usb.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/** Multi-road camera client
 *
 * @author Created by jiangdg on 2022/7/18
 */
class MultiCameraClient(ctx: Context, callback: IDeviceConnectCallBack?) {
    private var mUsbMonitor: USBMonitor? = null
    private val mMainHandler by lazy {
        Handler(Looper.getMainLooper())
    }

    init {
        mUsbMonitor = USBMonitor(ctx, object : USBMonitor.OnDeviceConnectListener {
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
                if (!isUsbCamera(device) && !isFilterDevice(ctx, device)) {
                    return
                }
                mMainHandler.post {
                    callback?.onAttachDev(device)
                }
            }

            /**
             * Called by receive usb device pulled out broadcast
             *
             * @param device usb device info,see [UsbDevice]
             */
            override fun onDetach(device: UsbDevice?) {
                if (Utils.debugCamera) {
                    Logger.i(TAG, "onDetach device = ${device?.toString()}")
                }
                device ?: return
                if (!isUsbCamera(device) && !isFilterDevice(ctx, device)) {
                    return
                }
                mMainHandler.post {
                    callback?.onDetachDec(device)
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
                    Logger.i(TAG, "onConnect device = ${device?.toString()}")
                }
                device ?: return
                if (!isUsbCamera(device) && !isFilterDevice(ctx, device)) {
                    return
                }
                mMainHandler.post {
                    callback?.onConnectDev(device, ctrlBlock)
                }
            }

            /**
             * Called by dis unauthorized permission
             *
             * @param device usb device info,see [UsbDevice]
             */
            override fun onDisconnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
                if (Utils.debugCamera) {
                    Logger.i(TAG, "onDisconnect device = ${device?.toString()}")
                }
                device ?: return
                if (!isUsbCamera(device) && !isFilterDevice(ctx, device)) {
                    return
                }
                mMainHandler.post {
                    callback?.onDisConnectDec(device, ctrlBlock)
                }
            }


            /**
             * Called by dis unauthorized permission or request permission exception
             *
             * @param device usb device info,see [UsbDevice]
             */
            override fun onCancel(device: UsbDevice?) {
                if (Utils.debugCamera) {
                    Logger.i(TAG, "onCancel device = ${device?.toString()}")
                }
                device ?: return
                if (!isUsbCamera(device) && !isFilterDevice(ctx, device)) {
                    return
                }
                mMainHandler.post {
                    callback?.onCancelDev(device)
                }
            }
        })
    }

    /**
     * Register usb insert broadcast
     */
    fun register() {
        if (isMonitorRegistered()) {
            return
        }
        mUsbMonitor?.register()
    }

    /**
     * UnRegister usb insert broadcast
     */
    fun unRegister() {
        if (!isMonitorRegistered()) {
            return
        }
        mUsbMonitor?.unregister()
    }

    /**
     * Request usb device permission
     *
     * @param device see [UsbDevice]
     * @return true ready to request permission
     */
    fun requestPermission(device: UsbDevice?): Boolean {
        if (!isMonitorRegistered()) {
            Logger.w(TAG, "Usb monitor haven't been registered.")
            return false
        }
        mUsbMonitor?.requestPermission(device)
        return true
    }

    /**
     * Uvc camera has permission
     *
     * @param device see [UsbDevice]
     * @return true permission granted
     */
    fun hasPermission(device: UsbDevice?) = mUsbMonitor?.hasPermission(device)

    /**
     * Get device list
     *
     * @param list filter regular
     * @return filter device list
     */
    fun getDeviceList(list: List<DeviceFilter>? = null): MutableList<UsbDevice>? {
        list?.let {
            addDeviceFilters(it)
        }
        return mUsbMonitor?.deviceList
    }

    /**
     * Add device filters
     *
     * @param list filter regular
     */
    fun addDeviceFilters(list: List<DeviceFilter>) {
        mUsbMonitor?.addDeviceFilter(list)
    }

    /**
     * Remove device filters
     *
     * @param list filter regular
     */
    fun removeDeviceFilters(list: List<DeviceFilter>) {
        mUsbMonitor?.removeDeviceFilter(list)
    }

    /**
     * Destroy usb monitor engine
     */
    fun destroy() {
        mUsbMonitor?.destroy()
    }

    fun openDebug(debug: Boolean) {
        Utils.debugCamera = debug
        USBMonitor.DEBUG = debug
        UVCCamera.DEBUG = debug
    }

    private fun isMonitorRegistered() = mUsbMonitor?.isRegistered == true


    /**
     * Create a uvc camera
     *
     * @property device see [UsbDevice]
     * @constructor Create camera
     */
    class Camera(private val ctx: Context, private val device: UsbDevice) : Handler.Callback {
        private var mCameraStateCallback: ICameraStateCallBack? = null
        private var mMediaMuxer: Mp4Muxer? = null
        private var mCameraView: Any? = null
        private var mCameraRequest: CameraRequest? = null
        private var mPreviewSize: PreviewSize? = null
        private var isPreviewed: Boolean = false
        private var mCameraThread: HandlerThread? = null
        private var mCameraHandler: Handler? = null
        private var mUvcCamera: UVCCamera? = null
        private var mPreviewCallback: IPreviewDataCallBack? = null
        private var mEncodeDataCallBack: IEncodeDataCallBack? = null
        private var mAudioProcess: AbstractProcessor? = null
        private var mVideoProcess: AbstractProcessor? = null
        private var mCtrlBlock: USBMonitor.UsbControlBlock? = null
        private val mMainHandler: Handler by lazy {
            Handler(Looper.getMainLooper())
        }
        private val mSaveImageExecutor: ExecutorService by lazy {
            Executors.newSingleThreadExecutor()
        }
        private val mNV21DataQueue: LinkedBlockingDeque<ByteArray> by lazy {
            LinkedBlockingDeque(MAX_NV21_DATA)
        }
        private val mDateFormat by lazy {
            SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.getDefault())
        }
        private val mCameraDir by lazy {
            "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)}/Camera"
        }

        private val frameCallBack = IFrameCallback { frame ->
            frame?.apply {
                val data = ByteArray(capacity())
                get(data)
                mPreviewCallback?.onPreviewData(data, IPreviewDataCallBack.DataFormat.NV21)
                // for image
                if (mNV21DataQueue.size >= MAX_NV21_DATA) {
                    mNV21DataQueue.removeLast()
                }
                mNV21DataQueue.offerFirst(data)
                // for video
                // avoid preview size changed
                mPreviewSize?.apply {
                    if (data.size != width * height * 3 /2) {
                        return@IFrameCallback
                    }
                    YUVUtils.nv21ToYuv420sp(data, width, height)
                    mVideoProcess?.putRawData(RawData(data, data.size))
                }
            }
        }

        override fun handleMessage(msg: Message): Boolean {
            when (msg.what) {
                MSG_START_PREVIEW -> {
                    (msg.obj as Pair<*, *>).apply {
                        openCameraInternal(first, second as? CameraRequest ?: getDefaultCameraRequest())
                    }
                }
                MSG_STOP_PREVIEW -> {
                    closeCameraInternal()
                }
                MSG_CAPTURE_IMAGE -> {
                    (msg.obj as Pair<*, *>).apply {
                        captureImageInternal(first as? String, second as ICaptureCallBack)
                    }
                }
                MSG_SEND_COMMAND -> {
                    sendCameraCommandInternal(msg.obj as Int)
                }
                MSG_CAPTURE_VIDEO_START -> {
                    (msg.obj as Triple<*, *, *>).apply {
                        captureVideoStartInternal(first as? String, second as Long, third as ICaptureCallBack)
                    }
                }
                MSG_CAPTURE_VIDEO_STOP -> {
                    captureVideoStopInternal()
                }
            }
            return true
        }

        private fun <T> openCameraInternal(cameraView: T, request: CameraRequest) {
            if (Utils.isTargetSdkOverP(ctx) && !CameraUtils.hasCameraPermission(ctx)) {
                closeCamera()
                mMainHandler.post {
                    mCameraStateCallback?.onCameraState(
                        this,
                        ICameraStateCallBack.State.ERROR,
                        "Has no CAMERA permission."
                    )
                }
                Logger.e(TAG ,"open camera failed, need Manifest.permission.CAMERA permission when targetSdk>=28")
                return
            }
            if (mCtrlBlock == null) {
                closeCamera()
                mMainHandler.post {
                    mCameraStateCallback?.onCameraState(
                        this,
                        ICameraStateCallBack.State.ERROR,
                        "Usb control block can not be null "
                    )
                }
                return
            }
            // 1. create a UVCCamera
            try {
                mUvcCamera = UVCCamera().apply {
                    open(mCtrlBlock)
                }
            } catch (e: Exception) {
                mMainHandler.post {
                    mCameraStateCallback?.onCameraState(
                        this,
                        ICameraStateCallBack.State.ERROR,
                        "open camera failed ${e.localizedMessage}"
                    )
                }
                Logger.e(TAG, "open camera failed.", e)
                closeCamera()
            }

            // 2. set preview size and register preview callback
            try {
                val previewSize = getSuitableSize(request.previewWidth, request.previewHeight)
                if (! isPreviewSizeSupported(previewSize)) {
                    mMainHandler.post {
                        mCameraStateCallback?.onCameraState(
                            this,
                            ICameraStateCallBack.State.ERROR,
                            "unsupported preview size"
                        )
                    }
                    closeCamera()
                    Logger.e(TAG, "open camera failed, preview size($previewSize) unsupported-> ${mUvcCamera?.supportedSizeList}")
                    return
                }
                initEncodeProcessor(previewSize.width, previewSize.height)
                mPreviewSize = previewSize
                mUvcCamera?.setPreviewSize(
                    previewSize.width,
                    previewSize.height,
                    MIN_FS,
                    MAX_FS,
                    UVCCamera.FRAME_FORMAT_MJPEG,
                    UVCCamera.DEFAULT_BANDWIDTH
                )
            } catch (e: Exception) {
                try {
                    val previewSize = getSuitableSize(request.previewWidth, request.previewHeight)
                    if (! isPreviewSizeSupported(previewSize)) {
                        mMainHandler.post {
                            mCameraStateCallback?.onCameraState(
                                this,
                                ICameraStateCallBack.State.ERROR,
                                "unsupported preview size"
                            )
                        }
                        closeCamera()
                        Logger.e(TAG, "open camera failed, preview size($previewSize) unsupported-> ${mUvcCamera?.supportedSizeList}")
                        return
                    }
                    Logger.e(TAG, " setPreviewSize failed, try to use yuv format...")
                    mUvcCamera?.setPreviewSize(
                        mPreviewSize!!.width,
                        mPreviewSize!!.height,
                        MIN_FS,
                        MAX_FS,
                        UVCCamera.FRAME_FORMAT_YUYV,
                        UVCCamera.DEFAULT_BANDWIDTH
                    )
                } catch (e: Exception) {
                    mMainHandler.post {
                        mCameraStateCallback?.onCameraState(
                            this,
                            ICameraStateCallBack.State.ERROR,
                            e.localizedMessage
                        )
                    }
                    closeCamera()
                    Logger.e(TAG, " setPreviewSize failed, even using yuv format", e)
                    return
                }
            }
            mUvcCamera?.setFrameCallback(frameCallBack, UVCCamera.PIXEL_FORMAT_YUV420SP)
            // 3. start preview
            when(cameraView) {
                is Surface -> {
                    mUvcCamera?.setPreviewDisplay(cameraView)
                }
                is SurfaceTexture -> {
                    mUvcCamera?.setPreviewTexture(cameraView)
                }
                is SurfaceView -> {
                    mUvcCamera?.setPreviewDisplay(cameraView.holder)
                }
                is TextureView -> {
                    mUvcCamera?.setPreviewTexture(cameraView.surfaceTexture)
                }
                else -> {
                    throw IllegalStateException("Only support Surface or SurfaceTexture or SurfaceView or TextureView or GLSurfaceView")
                }
            }
            mUvcCamera?.autoFocus = true
            mUvcCamera?.autoWhiteBlance = true
            mUvcCamera?.startPreview()
            mUvcCamera?.updateCameraParams()
            isPreviewed = true
            mMainHandler.post {
                mCameraStateCallback?.onCameraState(this, ICameraStateCallBack.State.OPENED)
            }
            if (Utils.debugCamera) {
                Logger.i(TAG, " start preview, name = ${device.deviceName}, preview=$mPreviewSize")
            }
        }

        private fun closeCameraInternal() {
            isPreviewed = false
            releaseEncodeProcessor()
            mUvcCamera?.destroy()
            mUvcCamera = null
            mMainHandler.post {
                mCameraStateCallback?.onCameraState(this, ICameraStateCallBack.State.CLOSED)
            }
            if (Utils.debugCamera) {
                Logger.i(TAG, " stop preview, name = ${device.deviceName}, preview=$mPreviewSize")
            }
        }

        private fun captureImageInternal(savePath: String?, callback: ICaptureCallBack) {
            mSaveImageExecutor.submit {
                if (! CameraUtils.hasStoragePermission(ctx)) {
                    mMainHandler.post { callback.onError("have no storage permission") }
                    Logger.e(TAG ,"open camera failed, have no storage permission")
                    return@submit
                }
                if (! isPreviewed || mPreviewSize == null) {
                    mMainHandler.post { callback.onError("camera not previewing") }
                    Logger.i(TAG, "captureImageInternal failed, camera not previewing")
                    return@submit
                }
                val data = mNV21DataQueue.pollFirst(CAPTURE_TIMES_OUT_SEC, TimeUnit.SECONDS)
                if (data == null) {
                    mMainHandler.post { callback.onError("Times out") }
                    Logger.i(TAG, "captureImageInternal failed, times out.")
                    return@submit
                }
                mMainHandler.post { callback.onBegin() }
                val date = mDateFormat.format(System.currentTimeMillis())
                val title = savePath ?: "IMG_AUSBC_$date"
                val displayName = savePath ?: "$title.jpg"
                val path = savePath ?: "$mCameraDir/$displayName"
                val location = Utils.getGpsLocation(ctx)
                val width = mPreviewSize!!.width
                val height = mPreviewSize!!.height
                YUVUtils.yuv420spToNv21(data, width, height)
                val ret = MediaUtils.saveYuv2Jpeg(path, data, width, height)
                if (! ret) {
                    val file = File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                    mMainHandler.post { callback.onError("save yuv to jpeg failed.") }
                    Logger.w(TAG, "save yuv to jpeg failed.")
                    return@submit
                }
                val values = ContentValues()
                values.put(MediaStore.Images.ImageColumns.TITLE, title)
                values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, displayName)
                values.put(MediaStore.Images.ImageColumns.DATA, path)
                values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, date)
                values.put(MediaStore.Images.ImageColumns.LONGITUDE, location?.longitude)
                values.put(MediaStore.Images.ImageColumns.LATITUDE, location?.latitude)
                ctx.contentResolver?.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                mMainHandler.post { callback.onComplete(path) }
                if (Utils.debugCamera) { Logger.i(TAG, "captureImageInternal save path = $path") }
            }
        }

        private fun sendCameraCommandInternal(command: Int) {
            mUvcCamera?.sendCommand(command)
        }

        private fun captureVideoStartInternal(path: String?, durationInSec: Long, callBack: ICaptureCallBack) {
            if (! CameraUtils.hasStoragePermission(ctx) || ! CameraUtils.hasAudioPermission(ctx)) {
                mMainHandler.post {
                    callBack.onError("have no storage or audio permission")
                }
                Logger.e(TAG ,"open camera failed, have no storage and audio permission")
                return
            }
            mMediaMuxer = Mp4Muxer(ctx, callBack, path, durationInSec)
            (mVideoProcess as? H264EncodeProcessor)?.apply {
                startEncode()
                setMp4Muxer(mMediaMuxer!!, true)
                addEncodeDataCallBack(mEncodeDataCallBack)
            }
            (mAudioProcess as? AACEncodeProcessor)?.apply {
                startEncode()
                setMp4Muxer(mMediaMuxer!!, false)
                addEncodeDataCallBack(mEncodeDataCallBack)
            }
        }

        private fun captureVideoStopInternal() {
            mMediaMuxer?.release()
            mVideoProcess?.stopEncode()
            mAudioProcess?.stopEncode()
            mMediaMuxer = null
        }

        /**
         * Set usb control block, when the uvc device was granted permission
         *
         * @param ctrlBlock see [USBMonitor.OnDeviceConnectListener]#onConnectedDev
         */
        fun setUsbControlBlock(ctrlBlock: USBMonitor.UsbControlBlock?) {
            this.mCtrlBlock = ctrlBlock
        }

        /**
         * Set camera state call back
         *
         * @param callback camera be opened or closed
         */
        fun setCameraStateCallBack(callback: ICameraStateCallBack) {
            mCameraStateCallback = callback
        }

        /**
         * Open camera
         *
         * @param cameraView render surface view，support Surface or SurfaceTexture
         *                      or SurfaceView or TextureView or GLSurfaceView
         * @param cameraRequest camera request
         */
        fun openCamera(cameraView: Any? = null, cameraRequest: CameraRequest? = null) {
            mCameraView = cameraView ?: mCameraView
            mCameraRequest = cameraRequest ?: mCameraRequest ?: getDefaultCameraRequest()
            val thread = HandlerThread("${device.deviceName}-${device.deviceId}").apply {
                start()
            }.also {
                mCameraHandler = Handler(it.looper, this)
                mCameraHandler?.obtainMessage(MSG_START_PREVIEW, Pair(cameraView, cameraRequest))?.sendToTarget()
            }
            this.mCameraThread = thread
        }

        /**
         * Close camera
         */
        fun closeCamera() {
            mCameraHandler?.obtainMessage(MSG_STOP_PREVIEW)?.sendToTarget()
            mCameraThread?.quitSafely()
            mCameraThread = null
            mCameraHandler = null
        }

        /**
         * check if camera opened
         *
         * @return camera open status, true or false
         */
        fun isCameraOpened() = isPreviewed

        /**
         * Get current camera request
         *
         * @return see [CameraRequest], can be null
         */
        fun getCameraRequest() = mCameraRequest

        /**
         * Capture image
         *
         * @param callBack capture a image status, see [ICaptureCallBack]
         * @param path image save path, default is DICM/Camera
         */
        fun captureImage(callBack: ICaptureCallBack, path: String? = null) {
            Pair(path, callBack).apply {
                mCameraHandler?.obtainMessage(MSG_CAPTURE_IMAGE, this)?.sendToTarget()
            }
        }

        /**
         * Capture video start
         *
         * @param callBack capture result callback, see [ICaptureCallBack]
         * @param path video save path, default is DICM/Camera
         * @param durationInSec video file auto divide duration is seconds
         */
        fun captureVideoStart(callBack: ICaptureCallBack, path: String? = null, durationInSec: Long = 0L) {
            Triple(path, durationInSec, callBack).apply {
                mCameraHandler?.obtainMessage(MSG_CAPTURE_VIDEO_START, this)?.sendToTarget()
            }
        }

        /**
         * Capture video stop
         */
        fun captureVideoStop() {
            mCameraHandler?.obtainMessage(MSG_CAPTURE_VIDEO_STOP)?.sendToTarget()
        }

        /**
         * Send camera command
         *
         * This method cannot be verified, please use it with caution
         */
        fun sendCameraCommand(command: Int) {
            mCameraHandler?.obtainMessage(MSG_SEND_COMMAND, command)?.sendToTarget()
        }

        /**
         * Update resolution
         *
         * @param width camera preview width, see [PreviewSize]
         * @param height camera preview height, [PreviewSize]
         * @return result of operation
         */
        fun updateResolution(width: Int, height: Int) {
            if (mCameraRequest == null) {
                Logger.w(TAG, "updateResolution failed, please open camera first.")
                return
            }
            if (mVideoProcess?.isEncoding() == true) {
                Logger.e(TAG, "updateResolution failed, video recording...")
                return
            }
            closeCamera()
            mMainHandler.postDelayed({
                mCameraRequest!!.previewWidth = width
                mCameraRequest!!.previewHeight = height
                openCamera()
            }, 100)
        }

        /**
         * Is record video
         */
        fun isRecordVideo() = mVideoProcess?.isEncoding() == true

        /**
         * Add encode data call back
         *
         * @param callBack camera encoded data call back, see [IEncodeDataCallBack]
         */
        fun addEncodeDataCallBack(callBack: IEncodeDataCallBack) {
            this.mEncodeDataCallBack = callBack
        }

        /**
         * Add preview raw data call back
         *
         * @param callBack camera preview data call back, see [IPreviewDataCallBack]
         */
        fun addPreviewDataCallBack(callBack: IPreviewDataCallBack) {
            this.mPreviewCallback = callBack
        }

        /**
         * Get usb device information
         *
         * @return see [UsbDevice]
         */
        fun getUsbDevice() = device

        /**
         * Get all preview sizes
         *
         * @param aspectRatio aspect ratio
         * @return [PreviewSize] list of camera
         */
        fun getAllPreviewSizes(aspectRatio: Double? = null): MutableList<PreviewSize> {
            val previewSizeList = arrayListOf<PreviewSize>()
            if (mUvcCamera?.supportedSizeList?.isNotEmpty() == true) {
                mUvcCamera?.supportedSizeList
            }  else {
                mUvcCamera?.getSupportedSizeList(UVCCamera.FRAME_FORMAT_YUYV)
            }.also { sizeList ->
                sizeList?.forEach { size ->
                    val width = size.width
                    val height = size.height
                    val ratio = width.toDouble() / height
                    if (aspectRatio == null || aspectRatio == ratio) {
                        previewSizeList.add(PreviewSize(width, height))
                    }
                }
            }
            if (Utils.debugCamera)
                Logger.i(TAG, "aspect ratio = $aspectRatio, getAllPreviewSizes = $previewSizeList, ")
            return previewSizeList
        }

        private fun initEncodeProcessor(previewWidth: Int, previewHeight: Int) {
            releaseEncodeProcessor()
            mAudioProcess = AACEncodeProcessor()
            mVideoProcess = H264EncodeProcessor(previewWidth, previewHeight, false)
        }

        private fun releaseEncodeProcessor() {
            mVideoProcess?.stopEncode()
            mAudioProcess?.stopEncode()
            mVideoProcess = null
            mAudioProcess = null
        }

        private fun getSuitableSize(maxWidth: Int, maxHeight: Int): PreviewSize {
            val sizeList = getAllPreviewSizes()
            if (sizeList.isNullOrEmpty()) {
                return PreviewSize(DEFAULT_PREVIEW_WIDTH, DEFAULT_PREVIEW_HEIGHT)
            }
            // find it
            sizeList.find {
                it.width == maxWidth && it.height == maxHeight
            }.also { size ->
                size ?: return@also
                return size
            }

            // find the same aspectRatio
            val aspectRatio = maxWidth.toFloat() / maxHeight
            sizeList.find {
                val w = it.width
                val h = it.height
                val ratio = w.toFloat() / h
                ratio == aspectRatio && w <= maxWidth && h <= maxHeight
            }.also { size ->
                size ?: return@also
                return size
            }
            // find the closest aspectRatio
            var minDistance: Int = maxWidth
            var closetSize = sizeList[0]
            sizeList.forEach { size ->
                if (minDistance >= abs((maxWidth - size.width))) {
                    minDistance = abs(maxWidth - size.width)
                    closetSize = size
                }
            }
            return closetSize
        }

        private fun isPreviewSizeSupported(previewSize: PreviewSize): Boolean {
            return getAllPreviewSizes().find {
                it.width == previewSize.width && it.height == previewSize.height
            } != null
        }


        private fun getDefaultCameraRequest(): CameraRequest {
            return CameraRequest.Builder()
                .setPreviewWidth(640)
                .setPreviewHeight(480)
                .create()
        }
    }

    companion object {
        private const val TAG = "MultiCameraClient"
        private const val MIN_FS = 10
        private const val MAX_FS = 60
        private const val MSG_START_PREVIEW = 0x01
        private const val MSG_STOP_PREVIEW = 0x02
        private const val MSG_CAPTURE_IMAGE = 0x03
        private const val MSG_CAPTURE_VIDEO_START = 0x04
        private const val MSG_CAPTURE_VIDEO_STOP = 0x05
        private const val MSG_SEND_COMMAND = 0x06
        private const val DEFAULT_PREVIEW_WIDTH = 640
        private const val DEFAULT_PREVIEW_HEIGHT = 480
        private const val MAX_NV21_DATA = 5
        private const val CAPTURE_TIMES_OUT_SEC = 3L
    }
}