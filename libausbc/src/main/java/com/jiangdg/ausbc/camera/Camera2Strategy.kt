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

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import android.view.OrientationEventListener
import android.view.Surface
import androidx.annotation.RequiresApi
import com.jiangdg.ausbc.callback.IPreviewDataCallBack
import com.jiangdg.ausbc.camera.bean.CameraStatus
import com.jiangdg.ausbc.camera.bean.PreviewSize
import com.jiangdg.ausbc.utils.SettableFuture
import com.jiangdg.ausbc.camera.bean.CameraV2Info
import com.jiangdg.ausbc.utils.Logger
import com.jiangdg.ausbc.utils.Utils
import java.io.File
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit
import kotlin.Exception

/** Camera2 usage
 *
 * @author Created by jiangdg on 2021/12/20
 * Deprecated since version 3.3.0, and it will be deleted in the future.
 * I recommend using the [CameraUVC] API for your application.
 */
@kotlin.Deprecated("Deprecated since version 3.3.0")
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class Camera2Strategy(ctx: Context) : ICameraStrategy(ctx) {
    private val mCaptureResults: BlockingQueue<CaptureResult> = LinkedBlockingDeque()
    private var mImageCaptureBuilder: CaptureRequest.Builder? = null
    private var mPreviewCaptureBuilder: CaptureRequest.Builder? = null
    private var mCameraDeviceFuture: SettableFuture<CameraDevice>? = null
    private var mCameraCharacteristicsFuture: SettableFuture<CameraCharacteristics>? = null
    private var mCameraSessionFuture: SettableFuture<CameraCaptureSession>? = null
    private var mImageSavePath: SettableFuture<String> = SettableFuture()
    private var mPreviewDataImageReader: ImageReader? = null
    private var mJpegImageReader: ImageReader? = null
    // 输出到屏幕的Surface
    private var mPreviewSurface: Surface? = null
    // 输出到预览ImageReader的Surface
    // 便于从中获取预览数据
    private var mPreviewDataSurface: Surface? = null
    // 输出到拍照ImageReader的Surface
    // 便于从中获取拍照数据
    private var mJpegDataSurface: Surface? = null
    private var mCameraManager: CameraManager? = null
    private var mYUVData: ByteArray? = null

    override fun loadCameraInfo() {
        mCameraManager = getContext()?.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        mCameraManager?.apply {
            cameraIdList.forEach { cameraId ->
                val characteristics = getCameraCharacteristics(cameraId)
                when (characteristics[CameraCharacteristics.LENS_FACING]) {
                    CameraCharacteristics.LENS_FACING_FRONT -> {
                        TYPE_FRONT
                    }
                    CameraCharacteristics.LENS_FACING_BACK -> {
                        TYPE_BACK
                    }
                    else -> {
                        TYPE_OTHER
                    }
                }.let { type ->
                    val list = mutableListOf<PreviewSize>()
                    val streamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    val sizeList = streamConfigurationMap?.getOutputSizes(SurfaceTexture::class.java)
                    sizeList?.forEach {
                        list.add(PreviewSize(it.width, it.height))
                    }
                    if (mCameraInfoMap[type] == null) {
                        val cameraInfo = CameraV2Info(cameraId).apply {
                            cameraType = type
                            cameraPreviewSizes = list
                            cameraCharacteristics = characteristics
                            cameraVid = cameraId.toInt() + 1
                            cameraPid = cameraId.toInt() + 1
                        }
                        mCameraInfoMap[type] = cameraInfo
                    }
                }
            }
            if (Utils.debugCamera) {
                Logger.i(TAG, "loadCameraInfo success, camera = $mCameraInfoMap")
            }
        }
    }

    override fun startPreviewInternal() {
        openCamera()
        createCaptureRequestBuilders()
        setPreviewSize()
        setImageSize()
        createSession()
        realStartPreview()
    }

    override fun stopPreviewInternal() {
        closeSession()
        closeCamera()
    }

    override fun captureImageInternal(savePath: String?) {
        if (! hasCameraPermission() || !hasStoragePermission()) {
            mMainHandler.post {
                mCaptureDataCb?.onError("Have no storage or camera permission.")
            }
            Logger.i(TAG, "takePictureInternal failed, has no storage/camera permission.")
            return
        }
        val cameraSession = mCameraSessionFuture?.get(3, TimeUnit.SECONDS)
        val characteristics = mCameraCharacteristicsFuture?.get(3, TimeUnit.SECONDS)
        val captureBuilder = mImageCaptureBuilder
        val jpegSurface = mJpegDataSurface
        if (cameraSession == null || characteristics==null || captureBuilder==null || jpegSurface == null) {
            mMainHandler.post {
                mCaptureDataCb?.onError("camera2 init failed.")
            }
            Logger.e(TAG, "takePictureInternal failed, camera init error.")
            return
        }
        try {
            val captureRequest = captureBuilder.let {
                val deviceOrientation = getDeviceOrientation()
                val jpegOrientation = getJpegOrientation(characteristics, deviceOrientation)
                val location = Utils.getGpsLocation(getContext())
                captureBuilder[CaptureRequest.JPEG_ORIENTATION] = jpegOrientation
                captureBuilder[CaptureRequest.JPEG_GPS_LOCATION] = location
                captureBuilder[CaptureRequest.JPEG_QUALITY] = 100
                captureBuilder.addTarget(jpegSurface)
                captureBuilder.build()
            }
            mImageSavePath.set(savePath)
            cameraSession.capture(captureRequest, mImageCaptureStateCallBack, mMainHandler)
        } catch (e: Exception) {
            mMainHandler.post {
                mCaptureDataCb?.onError(e.localizedMessage)
            }
            Logger.e(TAG, "takePictureInternal failed, camera access error.", e)
        }

    }

    override fun switchCameraInternal(cameraId: String?) {
        getRequest()?.let { request ->
            request.isFrontCamera = !request.isFrontCamera
            stopPreviewInternal()
            startPreviewInternal()
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

    override fun getAllPreviewSizes(aspectRatio: Double?): MutableList<PreviewSize> {
        val list = mutableListOf<PreviewSize>()
        getRequest()?.let { request ->
            val cameraInfo = mCameraInfoMap.values.find {
                request.cameraId == it.cameraId
            }
            cameraInfo?.cameraPreviewSizes?.forEach { size->
                val width = size.width
                val height = size.height
                val ratio = width.toDouble() / height
                if (aspectRatio==null || ratio == aspectRatio) {
                    list.add(size)
                }
            }
        }
        Logger.i(TAG, "getAllPreviewSizes aspect ratio = $aspectRatio, list= $list")
        return list
    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        getRequest()?.let { request->
            mCameraDeviceFuture = SettableFuture()
            mCameraCharacteristicsFuture = SettableFuture()
            try {
                if (! hasCameraPermission()) {
                    Logger.e(TAG, "openCamera failed, has no camera permission.")
                    return@let
                }
                if (mCameraManager == null) {
                    Logger.e(TAG, "init camera manager failed, is null!")
                    return@let
                }
                val cameraId = when {
                    request.isFrontCamera -> {
                        mCameraInfoMap[TYPE_FRONT]!!.cameraId
                    }
                    else -> {
                        mCameraInfoMap[TYPE_BACK]!!.cameraId
                    }
                }
                request.cameraId = cameraId
                mCameraManager!!.openCamera(cameraId, mCameraStateCallBack, mMainHandler)
                Logger.i(TAG, "openCamera success, id = $cameraId.")
            }catch (e: CameraAccessException) {
                closeCamera()
                Logger.e(TAG, "openCamera failed, err = ${e.reason}.", e)
            }
        }
    }

    private fun createCaptureRequestBuilders() {
        try {
            val cameraDevice = mCameraDeviceFuture?.get(3, TimeUnit.SECONDS)
            if (cameraDevice == null) {
                Logger.e(TAG, "createCaptureRequestBuilders failed, camera device is null.")
                return
            }
            getRequest()?.let {
                mPreviewCaptureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                mPreviewCaptureBuilder?.set(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
                )
                mPreviewCaptureBuilder?.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
                mImageCaptureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                Logger.i(TAG, "createCaptureRequestBuilders success.")
            }

        } catch (e: CameraAccessException) {
            Logger.e(TAG, "createCaptureRequestBuilders failed, err = ${e.reason}", e)
        }
    }

    private fun setPreviewSize() {
        val characteristics = mCameraCharacteristicsFuture?.get(3, TimeUnit.SECONDS)
        val previewSurface = if (getSurfaceTexture() != null) {
            Surface(getSurfaceTexture())
        } else {
            getSurfaceHolder()?.surface
        }
        if (characteristics == null || previewSurface == null) {
            Logger.e(TAG, "setPreviewSize failed. Camera characteristics is null.")
            return
        }
        // 创建预览Preview Surface
        // 缓存匹配的预览尺寸
        getRequest()?.let { request->
            val maxWidth = request.previewWidth
            val maxHeight = request.previewHeight
            val previewSize = getSuitableSize(characteristics, SurfaceTexture::class.java, maxWidth, maxHeight)
            mPreviewSurface = previewSurface
            request.previewWidth = previewSize.width
            request.previewHeight = previewSize.height
            mYUVData = ByteArray(request.previewWidth * request.previewHeight * 3 / 2)
            // 创建预览ImageReader & Preview Data Surface
            val imageFormat = ImageFormat.YUV_420_888
            val streamConfigurationMap = characteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]
            if (streamConfigurationMap?.isOutputSupportedFor(imageFormat) == true) {
                mPreviewDataImageReader = ImageReader.newInstance(previewSize.width, previewSize.height, imageFormat, 3)
                mPreviewDataImageReader?.setOnImageAvailableListener(mPreviewAvailableListener, getCameraHandler())
                mPreviewDataSurface = mPreviewDataImageReader?.surface
            }
            Logger.i(TAG, "setPreviewSize success, size = ${previewSize}.")
        }
    }

    private fun setImageSize() {
        val characteristics = mCameraCharacteristicsFuture?.get(3, TimeUnit.SECONDS)
        val captureBuilder = mImageCaptureBuilder
        if (characteristics == null) {
            Logger.e(TAG, "setImageSize failed. Camera characteristics is null.")
            return
        }
        getRequest()?.let { request->
            // 创建Jpeg Surface
            // 缓存匹配得到的尺寸
            val maxWidth = request.previewWidth
            val maxHeight = request.previewHeight
            val imageSize = getSuitableSize(characteristics, ImageReader::class.java, maxWidth, maxHeight)
            mJpegImageReader = ImageReader.newInstance(imageSize.width, imageSize.height, ImageFormat.JPEG, 5)
            mJpegImageReader?.setOnImageAvailableListener(mJpegAvailableListener, getCameraHandler())
            mJpegDataSurface = mJpegImageReader?.surface
            request.previewWidth = imageSize.width
            request.previewHeight = imageSize.height

            // 设定缩略图尺寸
            captureBuilder?.let {
                val availableThumbnailSizes = characteristics[CameraCharacteristics.JPEG_AVAILABLE_THUMBNAIL_SIZES]
                val thumbnailSize = getSuitableSize(availableThumbnailSizes, maxWidth, maxHeight)
                captureBuilder[CaptureRequest.JPEG_THUMBNAIL_SIZE] = thumbnailSize
            }
            Logger.i(TAG, "setImageSize success, size = ${imageSize}.")
        }
    }

    @Suppress("DEPRECATION")
    private fun createSession() {
        try {
            val cameraDevice = mCameraDeviceFuture?.get(3, TimeUnit.SECONDS)
            if (cameraDevice==null) {
                Logger.e(TAG, "realStartPreview failed, camera init failed.")
                stopPreviewInternal()
                return
            }
            mCameraSessionFuture = SettableFuture()
            val outputs = mutableListOf<Surface>().apply {
                mPreviewSurface?.let { add(it) }
                mPreviewDataSurface?.let { add(it) }
                mJpegDataSurface?.let { add(it) }
            }
            // 注意：这里要求回调在主线程
            cameraDevice.createCaptureSession(outputs, mCreateSessionStateCallBack, mMainHandler)
            Logger.i(TAG, "createSession, outputs = ${outputs.size}")
        } catch (e: Exception) {
            Logger.e(TAG, "createCaptureSession failed, err = ${e.localizedMessage}", e)
        }
    }

    private fun realStartPreview() {
        val cameraDevice = mCameraDeviceFuture?.get(3, TimeUnit.SECONDS)
        val cameraSession = mCameraSessionFuture?.get(3, TimeUnit.SECONDS)
        if (cameraDevice==null || cameraSession == null) {
            Logger.e(TAG, "realStartPreview failed, camera init failed.")
            stopPreviewInternal()
            postCameraStatus(CameraStatus(CameraStatus.ERROR, "camera init failed"))
            return
        }
        val previewSurface = mPreviewSurface!!
        val previewDataSurface = mPreviewDataSurface
        // 防止拍照时预览丢帧
        mImageCaptureBuilder?.let { builder ->
            builder.addTarget(previewSurface)
            previewDataSurface?.apply {
                builder.addTarget(this)
            }
        }

        // 开启预览
        mPreviewCaptureBuilder?.let { builder ->
            previewDataSurface?.apply {
                builder.addTarget(this)
            }
            builder.addTarget(previewSurface)
            builder.build()
        }.also { captureRequest->
            if (captureRequest == null) {
                Logger.e(TAG, "realStartPreview failed, captureRequest is null.")
                postCameraStatus(CameraStatus(CameraStatus.ERROR, "capture request is null"))
                return
            }
            cameraSession.setRepeatingRequest(captureRequest, null, getCameraHandler())
            mIsPreviewing.set(true)
            getRequest()?.apply {
                postCameraStatus(CameraStatus(CameraStatus.START, Pair(previewWidth, previewHeight).toString()))
            }
        }
        Logger.i(TAG, "realStartPreview success!")
    }

    private fun closeSession() {
        if (Utils.debugCamera && mIsPreviewing.get())
            Logger.i(TAG, "closeSession success.")
        mIsPreviewing.set(false)
        mCameraSessionFuture?.get(10, TimeUnit.MILLISECONDS)?.close()
        mCameraDeviceFuture?.get(10, TimeUnit.MILLISECONDS)?.close()
        mCameraSessionFuture = null
        mCameraDeviceFuture = null
    }

    private fun closeCamera() {
        if (Utils.debugCamera && mIsPreviewing.get())
            Logger.i(TAG, "closeCamera success.")
        mPreviewDataImageReader?.close()
        mPreviewDataImageReader = null
        mJpegImageReader?.close()
        mJpegImageReader = null
        mCameraCharacteristicsFuture = null
        postCameraStatus(CameraStatus(CameraStatus.STOP))
    }

    private fun getCameraCharacteristics(id: String): CameraCharacteristics? {
        mCameraInfoMap.values.forEach {
            val cameraInfo = it as CameraV2Info
            if (cameraInfo.cameraId == id) {
                return cameraInfo.cameraCharacteristics
            }
        }
        return null
    }

    private fun getSuitableSize(
        cameraCharacteristics: CameraCharacteristics,
        clazz: Class<*>,
        maxWidth: Int,
        maxHeight: Int
    ): Size {
        val streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val supportedSizes = streamConfigurationMap?.getOutputSizes(clazz)
        return getSuitableSize(supportedSizes, maxWidth, maxHeight)
    }

    private fun getSuitableSize(
        sizeList: Array<Size>?,
        maxWidth: Int,
        maxHeight: Int
    ): Size {
        val aspectRatio = maxWidth.toFloat() / maxHeight
        sizeList?.forEach { size ->
            val w = size.width
            val h = size.height
            val ratio = w.toFloat() / h
            if (ratio == aspectRatio && w <= maxWidth && h <= maxHeight) {
                return Size(w, h)
            }
        }
        return if (sizeList.isNullOrEmpty()) {
            Size(maxWidth, maxHeight)
        } else {
            Size(sizeList[0].width, sizeList[0].height)
        }
    }

    private fun getJpegOrientation(
        characteristics: CameraCharacteristics,
        deviceOrientation: Int
    ): Int {
        var myDeviceOrientation = deviceOrientation
        if (myDeviceOrientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
            return 0
        }
        val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
        val cameraFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
        val facingFront =  cameraFacing == CameraCharacteristics.LENS_FACING_FRONT
        myDeviceOrientation = (myDeviceOrientation + 45) / 90 * 90
        if (facingFront) {
            myDeviceOrientation = -myDeviceOrientation
        }
        return (sensorOrientation + myDeviceOrientation + 360) % 360
    }

    /**
     *  连接相机状态监听
     */
    private val mCameraStateCallBack = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            mCameraDeviceFuture?.set(camera)
            mCameraCharacteristicsFuture?.set(getCameraCharacteristics(camera.id))
            Logger.i(TAG, "connect camera success in callback.")
        }

        override fun onDisconnected(camera: CameraDevice) {
            mCameraDeviceFuture?.set(camera)
            stopPreviewInternal()
            Logger.i(TAG, "disconnect camera success in callback.")
        }

        override fun onError(camera: CameraDevice, error: Int) {
            mCameraDeviceFuture?.set(camera)
            stopPreviewInternal()
            Logger.i(TAG, "connect camera err = ($error) in callback.")
        }
    }

    /**
     * session创建状态监听
     */
    private val mCreateSessionStateCallBack = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            Logger.i(TAG, "configure session success in callback!")
            mCameraSessionFuture?.set(session)
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            Logger.i(TAG, "configure session failed in callback!")
            mCameraSessionFuture?.set(session)
        }
    }

    /**
     * 拍照状态回调
     */
    private val mImageCaptureStateCallBack  = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureStarted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            timestamp: Long,
            frameNumber: Long
        ) {
            super.onCaptureStarted(session, request, timestamp, frameNumber)
            mMainHandler.post {
                mCaptureDataCb?.onBegin()
            }
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            super.onCaptureCompleted(session, request, result)
            mCaptureResults.put(result)
        }
    }

    /**
     * 预览数据(YUV)回调
     * YUV_420_888[] -> NV21[YYYYYYYY VUVU]
     *
     */
    private val mPreviewAvailableListener = ImageReader.OnImageAvailableListener { imageReader ->
        val image = imageReader?.acquireNextImage()
        image?.use {
            val request = getRequest()
            request ?: return@OnImageAvailableListener
            mYUVData ?: return@OnImageAvailableListener
            try {
                val planes = it.planes
                // Y通道
                val yBuffer = planes[0].buffer
                val yuv420pYLen = request.previewWidth * request.previewHeight
                yBuffer.get(mYUVData!!, 0, yuv420pYLen)
                // V通道
                val vBuffer = planes[2].buffer
                val vPixelStride = planes[2].pixelStride
                for ((index, i) in (0 until vBuffer.remaining() step vPixelStride).withIndex()) {
                    mYUVData!![yuv420pYLen + 2 * index] = vBuffer.get(i)
                }

                // U通道
                val uBuffer = planes[1].buffer
                val uPixelStride = planes[1].pixelStride
                for ((index, i) in (0 until uBuffer.remaining() step uPixelStride).withIndex()) {
                    mYUVData!![yuv420pYLen + (2 * index + 1)] = uBuffer.get(i)
                }

                mPreviewDataCbList.forEach { cb ->
                    cb.onPreviewData(mYUVData, request.previewWidth, request.previewHeight, IPreviewDataCallBack.DataFormat.NV21)
                }
                it.close()
            } catch (e: IndexOutOfBoundsException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 拍照数据(JPEG)回调
     *      JPEG数据存放在plane[0]
     */
    @Suppress("DEPRECATION")
    private val mJpegAvailableListener = ImageReader.OnImageAvailableListener { imageReader ->
        val image = imageReader?.acquireNextImage()
        image?.use {
            val captureResult = mCaptureResults.take()
            val jpegBuffer = it.planes[0].buffer
            val jpegBufferArray = ByteArray(jpegBuffer.remaining())
            jpegBuffer.get(jpegBufferArray)
            mSaveImageExecutor.submit {
                var savePath: String? = null
                try {
                    savePath = mImageSavePath.get(3, TimeUnit.SECONDS)
                } catch (e: Exception) {
                    Logger.e(TAG, "times out.", e)
                    mMainHandler.post {
                        mCaptureDataCb?.onError("set path failed, save auto ")
                    }
                }
                val date = mDateFormat.format(System.currentTimeMillis())
                val title = savePath ?: "IMG_JJCamera_$date"
                val displayName = savePath ?: "$title.jpg"
                val path = savePath ?: "$mCameraDir/$displayName"
//                val orientation = captureResult[CaptureResult.JPEG_ORIENTATION]
//                val location = captureResult[CaptureResult.JPEG_GPS_LOCATION]
                // 写入文件
                File(path).writeBytes(jpegBufferArray)
                // 更新
                val values = ContentValues()
                values.put(MediaStore.Images.ImageColumns.TITLE, title)
                values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, displayName)
                values.put(MediaStore.Images.ImageColumns.DATA, path)
                values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, date)
//                values.put(MediaStore.Images.ImageColumns.ORIENTATION, orientation)
//                values.put(MediaStore.Images.ImageColumns.LONGITUDE, location?.longitude)
//                values.put(MediaStore.Images.ImageColumns.LATITUDE, location?.latitude)
                getContext()?.contentResolver?.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                mMainHandler.post {
                    mCaptureDataCb?.onComplete(path)
                }
            }
        }
    }

    companion object {
        private const val TAG = "CameraV2"
    }
}