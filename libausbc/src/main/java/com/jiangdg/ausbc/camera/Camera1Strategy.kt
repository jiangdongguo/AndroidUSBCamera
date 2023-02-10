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

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.Camera
import android.provider.MediaStore
import android.view.Surface
import com.jiangdg.ausbc.callback.IPreviewDataCallBack
import com.jiangdg.ausbc.camera.bean.CameraStatus
import com.jiangdg.ausbc.camera.bean.CameraV1Info
import com.jiangdg.ausbc.camera.bean.PreviewSize
import com.jiangdg.ausbc.utils.Logger
import com.jiangdg.ausbc.utils.Utils
import java.io.File
import kotlin.Exception

/** Camera1 usage
 *
 * @author Created by jiangdg on 2021/12/20
 *
 * Deprecated since version 3.3.0, and it will be deleted in the future.
 * I recommend using the [CameraUVC] API for your application.
 */
@kotlin.Deprecated("Deprecated since version 3.3.0")
@Suppress("DEPRECATION")
class Camera1Strategy(ctx: Context) : ICameraStrategy(ctx), Camera.PreviewCallback {
    private var mCamera: Camera? = null

    override fun loadCameraInfo() {
        val cameraInfo = Camera.CameraInfo()
        for (cameraId in 0 until Camera.getNumberOfCameras()) {
            Camera.getCameraInfo(cameraId, cameraInfo)
            when(cameraInfo.facing) {
                Camera.CameraInfo.CAMERA_FACING_FRONT -> {
                    TYPE_FRONT
                }
                Camera.CameraInfo.CAMERA_FACING_BACK -> {
                    TYPE_BACK
                }
                else -> {
                    TYPE_OTHER
                }
            }.also { type->
                val info = CameraV1Info(cameraId.toString()).apply {
                    cameraType = type
                    cameraVid = cameraId + 1
                    cameraPid = cameraId + 1
                }
                mCameraInfoMap[type] = info
            }
        }
        if (Utils.debugCamera) {
            Logger.i(TAG, "loadCameraInfo = $mCameraInfoMap")
        }
    }

    override fun startPreviewInternal() {
        createCamera()
        setParameters()
        realStartPreview()
    }

    override fun stopPreviewInternal() {
        destroyCamera()
    }

    override fun captureImageInternal(savePath: String?) {
        val jpegDataCb = Camera.PictureCallback { data, camera ->
            mSaveImageExecutor.submit {
                mMainHandler.post {
                    mCaptureDataCb?.onBegin()
                }
                val date = mDateFormat.format(System.currentTimeMillis())
                val title = savePath ?: "IMG_JJCamera_$date"
                val displayName = savePath ?: "$title.jpg"
                val path = savePath ?: "$mCameraDir/$displayName"
                val width = getRequest()?.previewWidth
                val height = getRequest()?.previewHeight
                val orientation = 0
                val location = Utils.getGpsLocation(getContext())
                // 写入文件
                File(path).writeBytes(data)
                // 更新
                val values = ContentValues()
                values.put(MediaStore.Images.ImageColumns.TITLE, title)
                values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, displayName)
                values.put(MediaStore.Images.ImageColumns.DATA, path)
                values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, date)
                values.put(MediaStore.Images.ImageColumns.WIDTH, width)
                values.put(MediaStore.Images.ImageColumns.HEIGHT, height)
                values.put(MediaStore.Images.ImageColumns.ORIENTATION, orientation)
                values.put(MediaStore.Images.ImageColumns.LONGITUDE, location?.longitude)
                values.put(MediaStore.Images.ImageColumns.LATITUDE, location?.latitude)
                getContext()?.contentResolver?.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                mMainHandler.post {
                    mCaptureDataCb?.onComplete(path)
                }
                stopPreviewInternal()
                startPreviewInternal()
                realStartPreview()
                mIsCapturing.set(false)
                if (Utils.debugCamera) {
                    Logger.i(TAG, "takePictureInternal save path = $path")
                }
            }
        }
        if (! hasCameraPermission() || !hasStoragePermission()) {
            mMainHandler.post {
                mCaptureDataCb?.onError("Have no storage or camera permission.")
            }
            Logger.i(TAG, "takePictureInternal failed, has no storage/camera permission.")
            return
        }
        if (mIsCapturing.get()) {
            return
        }
        mIsCapturing.set(true)
        mCamera?.takePicture(null, null, null, jpegDataCb)
    }

    override fun switchCameraInternal(cameraId: String?) {
        getRequest()?.let { request ->
            request.isFrontCamera = !request.isFrontCamera
            stopPreviewInternal()
            startPreviewInternal()

            if (Utils.debugCamera) {
                Logger.i(TAG, "switchCameraInternal")
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
                mCamera?.parameters?.supportedPreviewSizes?.forEach { size->
                    list.add(PreviewSize(size.width, size.height))
                }
                previewSizeList.addAll(list)
            }
            previewSizeList.forEach { size->
                val width = size.width
                val height = size.height
                val ratio = width.toDouble() / height
                if (aspectRatio==null || ratio == aspectRatio) {
                    list.add(size)
                }
            }
            Logger.i(TAG, "getAllPreviewSizes aspect ratio = $aspectRatio, list= $list")
            return list
        }
        return null
    }

    private fun createCamera() {
        getRequest()?.let { request->
            if (! hasCameraPermission()) {
                Logger.i(TAG, "openCamera failed, has no camera permission.")
                postCameraStatus(CameraStatus(CameraStatus.ERROR, "no permission"))
                return
            }
            stopPreviewInternal()
            mCamera = try {
                if (request.isFrontCamera) {
                    val cameraId = mCameraInfoMap[TYPE_FRONT]!!.cameraId
                    Camera.open(cameraId.toInt())
                } else {
                    Camera.open()
                }
            } catch (e: Exception) {
                Logger.e(TAG, "open camera failed, err = ${e.localizedMessage}", e)
                postCameraStatus(CameraStatus(CameraStatus.ERROR, e.localizedMessage))
                null
            } ?: return
            getAllPreviewSizes()
            if (Utils.debugCamera) {
                Logger.i(TAG, "createCamera id = ${request.cameraId}, front camera = ${request.isFrontCamera}")
            }
        }
    }

    private fun setParameters() {
        try {
            getRequest()?.let { request ->
                mCamera?.parameters?.apply {
                    val suitablePreviewSize = getSuitableSize(
                        supportedPreviewSizes,
                        request.previewWidth,
                        request.previewHeight
                    )
                    val width = suitablePreviewSize.width
                    val height = suitablePreviewSize.height
                    previewFormat = ImageFormat.NV21
                    pictureFormat = ImageFormat.JPEG
                    if (supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                        focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                    }
                    setPreviewSize(width, height)
                    set("orientation", "portrait")
                    set("rotation", 90)
                    request.previewWidth = width
                    request.previewHeight = height
                }.also {
                    mCamera?.parameters = it
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG, "open camera failed, err = ${e.localizedMessage}", e)
            mIsPreviewing.set(false)
            mCamera?.setPreviewCallbackWithBuffer(null)
            mCamera?.addCallbackBuffer(null)
            mCamera?.release()
            mCamera = null
            postCameraStatus(CameraStatus(CameraStatus.ERROR, e.localizedMessage))
            return
        }
    }

    private fun realStartPreview() {
        val st = getSurfaceTexture()
        val holder = getSurfaceHolder()
        if (st == null && holder == null) {
            postCameraStatus(CameraStatus(CameraStatus.ERROR, "surface is null"))
            Logger.e(TAG, "realStartPreview failed, SurfaceTexture or SurfaceHolder cannot be null.")
            return
        }
        try {
            getRequest()?.let { request->
                val width = request.previewWidth
                val height = request.previewHeight
                mCamera?.setDisplayOrientation(getPreviewDegree(getContext(), getRequest()?.isFrontCamera ?: false))
                mCamera?.setPreviewCallbackWithBuffer(this)
                mCamera?.addCallbackBuffer(ByteArray(width * height * 3 / 2))
                if (st != null) {
                    mCamera?.setPreviewTexture(st)
                } else {
                    mCamera?.setPreviewDisplay(holder)
                }
                mCamera?.startPreview()
                mIsPreviewing.set(true)
                postCameraStatus(CameraStatus(CameraStatus.START, Pair(width, height).toString()))
                if (Utils.debugCamera) {
                    Logger.i(TAG, "realStartPreview width =$width, height=$height")
                }
            }
        } catch (e: Exception) {
            postCameraStatus(CameraStatus(CameraStatus.ERROR, e.localizedMessage))
        }
    }

    private fun destroyCamera() {
        if (! mIsPreviewing.get()) return
        mIsPreviewing.set(false)
        mCamera?.setPreviewCallbackWithBuffer(null)
        mCamera?.addCallbackBuffer(null)
        mCamera?.stopPreview()
        mCamera?.release()
        mCamera = null
        postCameraStatus(CameraStatus(CameraStatus.STOP))
        if (Utils.debugCamera) {
            Logger.i(TAG, "destroyCamera")
        }
    }

    private fun getSuitableSize(
        sizeList: MutableList<Camera.Size>,
        maxWidth: Int,
        maxHeight: Int
    ): PreviewSize {
        val aspectRatio = maxWidth.toFloat() / maxHeight
        sizeList.forEach { size ->
            val w = size.width
            val h = size.height
            val ratio = w.toFloat() / h
            if (ratio == aspectRatio && w <= maxWidth && h <= maxHeight) {
                return PreviewSize(w, h)
            }
        }
        return if (sizeList.isEmpty()) {
            PreviewSize(maxWidth, maxHeight)
        } else {
            PreviewSize(sizeList[0].width, sizeList[0].height)
        }
    }

    private fun getPreviewDegree(context: Context?, isFrontCamera: Boolean): Int {
        if (context !is Activity) {
            return 90
        }
        val degree = when (context.windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
        val cameraInfo = Camera.CameraInfo()
        return if (isFrontCamera) {
            Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_FRONT, cameraInfo)
            (360 - (cameraInfo.orientation - +degree) % 360) % 360
        } else {
            Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, cameraInfo)
            (cameraInfo.orientation - degree + 360) % 360
        }
    }

    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
        data ?: return
        getRequest() ?: return
        try {
            val frameSize = getRequest()!!.previewWidth * getRequest()!!.previewHeight * 3 /2
            if (data.size != frameSize) {
                return
            }
            mPreviewDataCbList.forEach { cb ->
                cb.onPreviewData(data, getRequest()!!.previewWidth , getRequest()!!.previewHeight, IPreviewDataCallBack.DataFormat.NV21)
            }
            mCamera?.addCallbackBuffer(data)
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val TAG = "CameraV1"
    }
}