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
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.callback.ICaptureCallBack
import com.jiangdg.ausbc.callback.IPreviewDataCallBack
import com.jiangdg.ausbc.camera.bean.CameraInfo
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.camera.bean.CameraStatus
import com.jiangdg.ausbc.camera.bean.PreviewSize
import com.jiangdg.ausbc.utils.bus.BusKey
import com.jiangdg.ausbc.utils.bus.EventBus
import java.lang.Deprecated
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Camera Manager abstract class
 *
 * @author Created by jiangdg on 2021/12/20
 *
 * Deprecated since version 3.3.0, and it will be deleted in the future.
 * I recommend using the [MultiCameraClient.ICamera] API for your application.
 */
@kotlin.Deprecated("Deprecated since version 3.3.0")
abstract class ICameraStrategy(context: Context) : Handler.Callback {
    private var mThread: HandlerThread? = null
    private var mCameraHandler: Handler? = null
    private var mSurfaceTexture: SurfaceTexture? = null
    private var mSurfaceHolder: SurfaceHolder? = null
    private var mCameraRequest: CameraRequest? = null
    private var mContext: Context? = null
    protected var mPreviewDataCbList = CopyOnWriteArrayList<IPreviewDataCallBack>()
    protected var mCaptureDataCb: ICaptureCallBack? = null
    protected val mMainHandler: Handler = Handler(Looper.getMainLooper())
    protected val mSaveImageExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    protected val mCameraInfoMap = hashMapOf<Int, CameraInfo>()
    protected var mIsCapturing: AtomicBoolean = AtomicBoolean(false)
    protected var mIsPreviewing: AtomicBoolean = AtomicBoolean(false)
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
                    }
                    mDeviceOrientation.enable()
                    mCameraRequest = this
                    startPreviewInternal()
                }
            }
            MSG_STOP_PREVIEW -> {
                mCameraInfoMap.clear()
                mDeviceOrientation.disable()
                stopPreviewInternal()
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

    /**
     * Start preview
     *
     * @param request camera quest, see [CameraRequest]
     * @param renderSurface [SurfaceHolder] or [SurfaceTexture]
     */
    @Synchronized
    fun <T> startPreview(request: CameraRequest?, renderSurface: T?) {
        if (mIsPreviewing.get() || mThread?.isAlive == true) {
            stopPreview()
        }
        if (mCameraRequest == null && request == null) {
            throw IllegalStateException("camera request can't be null")
        }
        if (mSurfaceHolder == null && mSurfaceTexture == null && renderSurface == null) {
            throw IllegalStateException("render surface can't be null")
        }
        when (renderSurface) {
            is SurfaceTexture -> {
                setSurfaceTexture(renderSurface)
            }
            is SurfaceHolder -> {
                setSurfaceHolder(renderSurface)
            }
            else -> {
            }
        }.also {
            val thread = HandlerThread(THREAD_NAME).apply {
                start()
            }.also {
                mCameraHandler = Handler(it.looper, this)
                mCameraHandler?.obtainMessage(MSG_INIT)?.sendToTarget()
                mCameraHandler?.obtainMessage(MSG_START_PREVIEW, request ?: mCameraRequest)?.sendToTarget()
            }
            this.mThread = thread
        }
    }

    /**
     * Stop preview
     */
    @Synchronized
    fun stopPreview() {
        mThread ?: return
        mCameraHandler ?: return
        mCameraHandler?.obtainMessage(MSG_STOP_PREVIEW)?.sendToTarget()
        mThread?.quitSafely()
        mThread = null
        mCameraHandler = null
    }

    /**
     * Capture image
     *
     * @param callBack capture status, see [ICaptureCallBack]
     * @param savePath image save path
     */
    @Synchronized
    fun captureImage(callBack: ICaptureCallBack, savePath: String?) {
        this.mCaptureDataCb = callBack
        mCameraHandler?.obtainMessage(MSG_CAPTURE_IMAGE, savePath)?.sendToTarget()
    }

    /**
     * Switch camera
     *
     * @param cameraId camera id, camera1/camera2/camerax is null
     */
    @Synchronized
    fun switchCamera(cameraId: String? = null) {
        mCameraHandler?.obtainMessage(MSG_SWITCH_CAMERA, cameraId)?.sendToTarget()
    }

    private fun setSurfaceTexture(surfaceTexture: SurfaceTexture) {
        this.mSurfaceTexture = surfaceTexture
    }

    private fun setSurfaceHolder(holder: SurfaceHolder) {
        this.mSurfaceHolder = holder
    }

    /**
     * Get all preview sizes
     *
     * @param aspectRatio preview size aspect ratio
     * @return preview size list
     */
    abstract fun getAllPreviewSizes(aspectRatio: Double? = null): MutableList<PreviewSize>?

    /**
     * Get surface texture
     *
     * @return camera render [SurfaceTexture]
     */
    fun getSurfaceTexture(): SurfaceTexture? = mSurfaceTexture

    /**
     * Get surface holder
     *
     * @return camera render [SurfaceHolder]
     */
    fun getSurfaceHolder(): SurfaceHolder? = mSurfaceHolder

    /**
     * Get context
     *
     * @return context
     */
    protected fun getContext(): Context? = mContext

    /**
     * Get request
     *
     * @return camera request, see [CameraRequest]
     */
    protected fun getRequest(): CameraRequest? = mCameraRequest

    /**
     * Get camera handler
     *
     * @return camera thread handler, see [HandlerThread]
     */
    protected fun getCameraHandler(): Handler? = mCameraHandler

    /**
     * Get device orientation
     *
     * @return device orientation angle
     */
    protected fun getDeviceOrientation(): Int = mDeviceOrientation.orientation

    /**
     * Post camera status
     *
     * @param status see [CameraStatus]
     */
    protected fun postCameraStatus(status: CameraStatus) {
        EventBus.with<CameraStatus>(BusKey.KEY_CAMERA_STATUS).postMessage(status)
    }

    /**
     * Register uvc camera monitor, see [CameraUvcStrategy]
     */
    open fun register() {}

    /**
     * Un register uvc camera monitor, see [CameraUvcStrategy]
     */
    open fun unRegister() {}

    /**
     * Load camera info,
     *  see [Camera1Strategy] or [Camera2Strategy] or [CameraUvcStrategy]
     */
    protected abstract fun loadCameraInfo()

    /**
     * Start preview internal,
     * see [Camera1Strategy] or [Camera2Strategy] or [CameraUvcStrategy]
     */
    protected abstract fun startPreviewInternal()

    /**
     * Stop preview internal,
     * see [Camera1Strategy] or [Camera2Strategy] or [CameraUvcStrategy]
     */
    protected abstract fun stopPreviewInternal()

    /**
     * Capture image internal,
     * see [Camera1Strategy] or [Camera2Strategy] or [CameraUvcStrategy]
     * @param savePath
     */
    protected abstract fun captureImageInternal(savePath: String?)

    /**
     * Switch camera internal,
     * see [Camera1Strategy] or [Camera2Strategy] or [CameraUvcStrategy]
     * @param cameraId camera id. only uvc camera used
     */
    protected abstract fun switchCameraInternal(cameraId: String?)

    /**
     * Update resolution internal
     * see [Camera1Strategy] or [Camera2Strategy] or [CameraUvcStrategy]
     * @param width
     * @param height
     */
    protected abstract fun updateResolutionInternal(width: Int, height: Int)

    /**
     * Has camera permission
     * see [Camera1Strategy] or [Camera2Strategy] or [CameraUvcStrategy]
     * @return true was granted
     */
    protected fun hasCameraPermission(): Boolean{
        getContext() ?: return false
        val locPermission = ContextCompat.checkSelfPermission(getContext()!!, Manifest.permission.CAMERA)
        return locPermission == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Has storage permission
     * see [Camera1Strategy] or [Camera2Strategy] or [CameraUvcStrategy]
     * @return true was granted
     */
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
                    Lifecycle.Event.ON_CREATE -> {
                        register()
                    }
                    Lifecycle.Event.ON_DESTROY -> {
                        stopPreview()
                        unRegister()
                    }
                    else -> {}
                }
            }
        })
    }

    /**
     * Add preview data call back
     * see [Camera1Strategy] or [Camera2Strategy] or [CameraUvcStrategy]
     * @param callBack preview data call back
     */
    fun addPreviewDataCallBack(callBack: IPreviewDataCallBack) {
        if (mPreviewDataCbList.contains(callBack)) {
            return
        }
        mPreviewDataCbList.add(callBack)
    }

    /**
     * Remove preview data call back
     *
     * @param callBack preview data call back
     */
    fun removePreviewDataCallBack(callBack: IPreviewDataCallBack) {
        if (! mPreviewDataCbList.contains(callBack)) {
            return
        }
        mPreviewDataCbList.remove(callBack)
    }

    /**
     * check camera opened
     *
     * @return camera open status, true or false
     */
    fun isCameraOpened() = mIsPreviewing.get()

    companion object {
        private const val TAG = "ICameraStrategy"
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