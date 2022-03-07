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
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Camera Manager abstract class
 *
 * @author Created by jiangdg on 2021/12/20
 */
abstract class ICameraStrategy(context: Context) : Handler.Callback {
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
    protected var mIsCapturing: AtomicBoolean = AtomicBoolean(false)
    private var mIsPreviewing: AtomicBoolean = AtomicBoolean(false)

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
                msg.obj ?: return true
                (msg.obj as CameraRequest).apply {
                    if (mIsPreviewing.get()) {
                        mDeviceOrientation.disable()
                        stopPreviewInternal()
                        mIsPreviewing.set(false)
                    }
                    mDeviceOrientation.enable()
                    mCameraRequest = this
                    startPreviewInternal()
                    mIsPreviewing.set(true)
                }
            }
            MSG_STOP_PREVIEW -> {
                (msg.obj as Boolean).let { stopped ->
                    mDeviceOrientation.disable()
                    stopPreviewInternal()
                    mIsPreviewing.set(false)
                    if (stopped) {
                        mCameraRequest = null
                        mPreviewDataCbList.clear()
                        mThread?.quitSafely()
                        mThread = null
                        mCameraHandler = null
                    }
                }
            }
            MSG_CAPTURE_IMAGE -> {
                captureImageInternal(msg.obj as? String)
            }
            MSG_SWITCH_CAMERA -> {
                switchCameraInternal(msg.obj as? String)
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

    fun stopPreview(needThreadStop: Boolean = true) {
        mCameraHandler?.obtainMessage(MSG_STOP_PREVIEW, needThreadStop)?.sendToTarget()
    }

    fun captureImage(callBack: ICaptureCallBack, savePath: String?) {
        this.mCaptureDataCb = callBack
        mCameraHandler?.obtainMessage(MSG_CAPTURE_IMAGE, savePath)?.sendToTarget()
    }

    fun switchCamera(cameraId: String? = null) {
        mCameraHandler?.obtainMessage(MSG_SWITCH_CAMERA, cameraId)?.sendToTarget()
    }

    private fun setSurfaceTexture(surfaceTexture: SurfaceTexture) {
        this.mSurfaceTexture = surfaceTexture
    }

    private fun setSurfaceHolder(holder: SurfaceHolder) {
        this.mSurfaceHolder = holder
    }

    abstract fun getAllPreviewSizes(aspectRatio: Double? = null): MutableList<PreviewSize>?

    fun getSurfaceTexture(): SurfaceTexture? = mSurfaceTexture
    fun getSurfaceHolder(): SurfaceHolder? = mSurfaceHolder
    protected fun getContext(): Context? = mContext
    protected fun getRequest(): CameraRequest? = mCameraRequest
    protected fun getCameraHandler(): Handler? = mCameraHandler
    protected fun getDeviceOrientation(): Int = mDeviceOrientation.orientation

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
        if (mPreviewDataCbList.contains(callBack)) {
            return
        }
        mPreviewDataCbList.add(callBack)
    }

    companion object {
        private const val THREAD_NAME = "camera_manager"
        private const val MSG_INIT = 0x00
        private const val MSG_START_PREVIEW = 0x01
        private const val MSG_STOP_PREVIEW = 0x02
        private const val MSG_CAPTURE_IMAGE = 0x03
        private const val MSG_SWITCH_CAMERA = 0x04

        internal const val TYPE_FRONT = 0
        internal const val TYPE_BACK = 1
        internal const val TYPE_OTHER = 2
    }
}