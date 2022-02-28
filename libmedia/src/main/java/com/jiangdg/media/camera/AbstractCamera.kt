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

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.os.*
import android.view.OrientationEventListener
import android.view.SurfaceHolder
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.jiangdg.media.callback.ICaptureCallBack
import com.jiangdg.media.callback.IPreviewDataCallBack
import com.jiangdg.media.camera.bean.CameraInfo
import com.jiangdg.media.camera.bean.CameraRequest
import com.jiangdg.media.camera.bean.PreviewSize
import com.jiangdg.media.utils.MediaUtils
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Camera Manager abstract class
 *
 * @author Created by jiangdg on 2021/12/20
 */
abstract class AbstractCamera(context: Context) : Handler.Callback {
    private var mThread: HandlerThread? = null
    private var mCameraHandler: Handler? = null
    private var mSurfaceTexture: SurfaceTexture? = null
    private var mSurfaceHolder: SurfaceHolder? = null
    private var mCameraRequest: CameraRequest? = null
    private var mContext: Context? = null
    protected var mPreviewDataCbList = mutableListOf<IPreviewDataCallBack>()
    protected var mCaptureDataCb: ICaptureCallBack? = null
    protected val mMainHandler: Handler = Handler(Looper.getMainLooper())
    protected val mSaveImageExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    protected val mCameraInfoMap = hashMapOf<Int, CameraInfo>()
    protected var isCapturing: Boolean = false

    protected val mDateFormat by lazy {
        SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.getDefault())
    }
    protected val mCameraDir by lazy {
        "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)}/Camera"
    }

    private val mDeviceOrientation = object : OrientationEventListener(context) {
        var orientation = 0
            private set

        override fun onOrientationChanged(orientation: Int) {
            this.orientation = orientation
        }
    }

    init {
        val thread = HandlerThread(THREAD_NAME).apply {
            start()
        }.also {
            mCameraHandler = Handler(it.looper, this)
            mCameraHandler?.obtainMessage(MSG_INIT)?.sendToTarget()
        }
        this.mThread = thread
        this.mContext = context.applicationContext
        addLifecycleObserver(context)
    }

    override fun handleMessage(msg: Message): Boolean {
        when(msg.what) {
            MSG_INIT -> {
                loadCameraInfo()
            }
            MSG_START_PREVIEW -> {
                (msg.obj as CameraRequest).apply {
                    mDeviceOrientation.enable()
                    mCameraRequest = this
                    startPreviewInternal()
                }
            }
            MSG_STOP_PREVIEW -> {
                mDeviceOrientation.disable()
                mCameraRequest = null
                mPreviewDataCbList.clear()
                stopPreviewInternal()
                mThread?.quitSafely()
                mThread = null
                mCameraHandler = null
            }
            MSG_CAPTURE_IMAGE -> {
                captureImageInternal(msg.obj as? String)
            }
            MSG_SWITCH_CAMERA -> {
                switchCameraInternal(msg.obj as? String)
            }
            MSG_UPDATE_RESOLUTION -> {
                (msg.obj as PreviewSize).let {
                    updateResolutionInternal(it.width, it.height)
                }
            }
        }
        return true
    }

    fun startPreview(request: CameraRequest, surfaceTexture: SurfaceTexture) {
        setSurfaceTexture(surfaceTexture)
        mCameraHandler?.obtainMessage(MSG_START_PREVIEW, request)?.sendToTarget()
    }

    fun startPreview(request: CameraRequest, holder: SurfaceHolder) {
        setSurfaceHolder(holder)
        mCameraHandler?.obtainMessage(MSG_START_PREVIEW, request)?.sendToTarget()
    }

    fun stopPreview() {
        mCameraHandler?.obtainMessage(MSG_STOP_PREVIEW)?.sendToTarget()
    }

    fun captureImage(callBack: ICaptureCallBack, savePath: String?) {
        this.mCaptureDataCb = callBack
        mCameraHandler?.obtainMessage(MSG_CAPTURE_IMAGE, savePath)?.sendToTarget()
    }

    fun switchCamera(cameraId: String? = null) {
        mCameraHandler?.obtainMessage(MSG_SWITCH_CAMERA, cameraId)?.sendToTarget()
    }

    fun updateResolution(width: Int, height: Int) {
        PreviewSize(width, height).apply {
            mCameraHandler?.obtainMessage(MSG_UPDATE_RESOLUTION, this)
        }
    }

    private fun setSurfaceTexture(surfaceTexture: SurfaceTexture) {
        this.mSurfaceTexture = surfaceTexture
    }

    private fun setSurfaceHolder(holder: SurfaceHolder) {
        this.mSurfaceHolder = holder
    }

    abstract fun getAllPreviewSizes(aspectRatio: Double? = null): MutableList<PreviewSize>

    fun getSurfaceTexture() = mSurfaceTexture
    fun getSurfaceHolder() = mSurfaceHolder
    protected fun getContext() = mContext
    protected fun getRequest() = mCameraRequest!!
    protected fun getCameraHandler() = mCameraHandler!!
    protected fun getDeviceOrientation() = mDeviceOrientation.orientation

    protected open fun release() {}
    protected open fun register() {}
    protected open fun unRegister() {}

    protected abstract fun loadCameraInfo()
    protected abstract fun startPreviewInternal()
    protected abstract fun stopPreviewInternal()
    protected abstract fun captureImageInternal(savePath: String?)
    protected abstract fun switchCameraInternal(cameraId: String?)
    protected abstract fun updateResolutionInternal(width: Int, height: Int)

    protected fun hasCameraPermission(): Boolean{
        getContext() ?: return false
        val locPermission = ContextCompat.checkSelfPermission(getContext()!!, Manifest.permission.CAMERA)
        return locPermission == PackageManager.PERMISSION_GRANTED
    }

    protected fun hasStoragePermission(): Boolean {
        getContext() ?: return false
        val locPermission = ContextCompat.checkSelfPermission(getContext()!!, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return locPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun addLifecycleObserver(context: Context) {
        if (context !is LifecycleOwner) return
        context.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        register()
                    }
                    Lifecycle.Event.ON_STOP -> {
                        unRegister()
                    }
                    Lifecycle.Event.ON_DESTROY -> {
                        stopPreview()
                        release()
                    }
                    else -> {}
                }
            }
        })
    }

    fun addPreviewDataCallBack(callBack: IPreviewDataCallBack) {
        this.mPreviewDataCbList.add(callBack)
    }

    companion object {
        private const val THREAD_NAME = "camera_manager"
        private const val MSG_INIT = 0x00
        private const val MSG_START_PREVIEW = 0x01
        private const val MSG_STOP_PREVIEW = 0x02
        private const val MSG_CAPTURE_IMAGE = 0x03
        private const val MSG_SWITCH_CAMERA = 0x04
        private const val MSG_UPDATE_RESOLUTION = 0x05

        internal const val TYPE_FRONT = 0
        internal const val TYPE_BACK = 1
        internal const val TYPE_OTHER = 2
    }
}