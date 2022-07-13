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
package com.jiangdg.ausbc.render

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.location.Location
import android.location.LocationManager
import android.opengl.EGLContext
import android.os.*
import android.provider.MediaStore
import android.view.Surface
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.jiangdg.ausbc.callback.ICaptureCallBack
import com.jiangdg.ausbc.render.env.RotateType
import com.jiangdg.ausbc.render.effect.AbstractEffect
import com.jiangdg.ausbc.render.internal.*
import com.jiangdg.ausbc.utils.*
import com.jiangdg.ausbc.utils.bus.BusKey
import com.jiangdg.ausbc.utils.bus.EventBus
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Render manager
 *
 * @property previewWidth camera preview width
 * @property previewHeight camera preview height
 *
 * @param context context
 *
 * @author Created by jiangdg on 2021/12/28
 */
class RenderManager(context: Context, private val previewWidth: Int, private val previewHeight: Int) :
    SurfaceTexture.OnFrameAvailableListener, Handler.Callback {
    private var mTextureId: Int = 0
    private var mRenderThread: HandlerThread? = null
    private var mRenderHandler: Handler? = null
    private var mRenderCodecThread: HandlerThread? = null
    private var mRenderCodecHandler: Handler? = null
    private var mCameraRender: CameraRender? = null
    private var mScreenRender: ScreenRender? = null
    private var mEncodeRender: EncodeRender? = null
    private var mCaptureRender: CaptureRender? = null
    private var mCameraSurfaceTexture: SurfaceTexture? = null
    private var mTransformMatrix: FloatArray = FloatArray(16)
    private var mWidth: Int = 0
    private var mHeight: Int = 0
    private var mFBOId: Int = 0
    private var mContext: Context = context
    private var mEffectList = arrayListOf<AbstractEffect>()
    private var mCacheEffectList = arrayListOf<AbstractEffect>()
    private var mCaptureDataCb: ICaptureCallBack? = null
    private var mFrameRate = 0
    private var mEndTime: Long = 0L
    private var mStartTime = System.currentTimeMillis()
    private val mMainHandler: Handler by lazy {
        Handler(Looper.getMainLooper())
    }
    private val mCaptureState: AtomicBoolean by lazy {
        AtomicBoolean(false)
    }
    private val mDateFormat by lazy {
        SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.getDefault())
    }
    private val mCameraDir by lazy {
        "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)}/Camera"
    }

    init {
        this.mCameraRender = CameraRender(context)
        this.mScreenRender = ScreenRender(context)
        this.mCaptureRender = CaptureRender(context)
        addLifecycleObserver(context)

        Logger.i(TAG, "create RenderManager, Open ES version is ${Utils.getGLESVersion(context)}")
    }

    /**
     * Rendering processing logic
     *
     * Note: EGL must be initialized first, otherwise GL cannot run
     */
    override fun handleMessage(msg: Message): Boolean {
        Utils.getGLESVersion(mContext)?.let { version ->
            if (version.toFloat() < 2F) {
                Logger.e(TAG, "OpenGL ES version(${version}) is too lower")
                return true
            }
        }
        when (msg.what) {
            MSG_GL_INIT -> {
                (msg.obj as Triple<*, *, *>).apply {
                    val w = first as Int
                    val h = second as Int
                    val surface = third as? Surface
                    mScreenRender?.initEGLEvn()
                    mScreenRender?.setupSurface(surface, w, h)
                    mScreenRender?.eglMakeCurrent()
                    mCameraRender?.initGLES()
                    mScreenRender?.initGLES()
                    mCaptureRender?.initGLES()
                    EventBus.with<Boolean>(BusKey.KEY_RENDER_READY).postMessage(true)
                }
            }
            MSG_GL_CHANGED_SIZE -> {
                (msg.obj as Pair<*, *>).apply {
                    mWidth = first as Int
                    mHeight = second as Int
                    mCameraRender?.setSize(mWidth, mHeight)
                    mScreenRender?.setSize(mWidth, mHeight)
                    mCaptureRender?.setSize(mWidth, mHeight)
                    mCameraSurfaceTexture?.setDefaultBufferSize(mWidth, mHeight)
                }
            }
            MSG_GL_SAVE_IMAGE -> {
                saveImageInternal(msg.obj as? String)
            }
            MSG_GL_START_RENDER_CODEC -> {
                (msg.obj as Triple<*, *, *>).apply {
                    val surface = first as Surface
                    val width = second as Int
                    val height = third as Int
                    startRenderCodecInternal(surface, width, height)
                }
            }
            MSG_GL_STOP_RENDER_CODEC -> {
                stopRenderCodecInternal()
            }
            MSG_GL_ROUTE_ANGLE -> {
                (msg.obj as? RotateType)?.apply {
                    mCameraRender?.setRotateAngle(this)
                }
            }
            MSG_GL_DRAW -> {
                // 将摄像头数据渲染到SurfaceTexture
                // 同时设置图像的矫正矩阵
                mCameraSurfaceTexture?.updateTexImage()
                mCameraSurfaceTexture?.getTransformMatrix(mTransformMatrix)
                mCameraRender?.setTransformMatrix(mTransformMatrix)
                mCameraRender?.drawFrame(mTextureId)
                // 滤镜、渲染处理
                mCameraRender?.getFboTextureId()?.let { fboId ->
                    var effectId = fboId
                    mEffectList.forEach { effectRender ->
                        effectRender.drawFrame(effectId)
                        effectId = effectRender.getFboTextureId()
                    }
                    effectId
                }?.also { id ->
                    mScreenRender?.drawFrame(id)
                    drawFrame2Capture(id)
                    drawFrame2Codec(id, mCameraSurfaceTexture?.timestamp ?: 0)
                    mScreenRender?.swapBuffers(mCameraSurfaceTexture?.timestamp ?: 0)
                }
            }
            MSG_GL_ADD_EFFECT -> {
                (msg.obj as? AbstractEffect)?.let { effect->
                    if (mEffectList.contains(effect)) {
                        return@let
                    }
                    effect.initGLES()
                    effect.setSize(mWidth, mHeight)
                    mEffectList.add(effect)
                    mCacheEffectList.add(effect)
                    Logger.i(TAG, "add effect, name = ${effect.javaClass.simpleName}, size = ${mEffectList.size}")
                }
            }
            MSG_GL_REMOVE_EFFECT -> {
                (msg.obj as? AbstractEffect)?.let {
                    if (! mEffectList.contains(it)) {
                        return@let
                    }
                    it.releaseGLES()
                    mEffectList.remove(it)
                    mCacheEffectList.remove(it)
                    Logger.i(TAG, "remove effect, name = ${it.javaClass.simpleName}, size = ${mEffectList.size}")
                }
            }
            MSG_GL_RELEASE -> {
                EventBus.with<Boolean>(BusKey.KEY_RENDER_READY).postMessage(false)
                mEffectList.forEach { effect ->
                    effect.releaseGLES()
                }
                mEffectList.clear()
                mCameraRender?.releaseGLES()
                mScreenRender?.releaseGLES()
                mCaptureRender?.releaseGLES()
                mCameraSurfaceTexture?.setOnFrameAvailableListener(null)
                mCameraSurfaceTexture = null
            }
        }
        return true
    }

    private fun drawFrame2Capture(fboId: Int) {
        mCaptureRender?.drawFrame(fboId)
        mCaptureRender?.getFboTextureId()?.let { id->
            mFBOId = id
        }
    }

    /**
     * Start render screen
     *
     * @param w surface width
     * @param h surface height
     * @param outSurface render surface
     * @param listener acquire camera surface texture, see [CameraSurfaceTextureListener]
     */
    fun startRenderScreen(w: Int, h: Int, outSurface: Surface?, listener: CameraSurfaceTextureListener? = null) {
        if (mCameraSurfaceTexture == null) {
            mTextureId = mCameraRender?.createOESTexture()!!
            mCameraSurfaceTexture = SurfaceTexture(mTextureId)
            mCameraSurfaceTexture?.setOnFrameAvailableListener(this)
        }
        listener?.onSurfaceTextureAvailable(mCameraSurfaceTexture)
        mRenderThread = HandlerThread(RENDER_THREAD)
        mRenderThread?.start()
        mRenderHandler = Handler(mRenderThread!!.looper, this@RenderManager)
        Triple(w, h, outSurface).apply {
            mRenderHandler?.obtainMessage(MSG_GL_INIT, this)?.sendToTarget()
        }
        setRenderSize(w, h)
    }

    /**
     * Stop render screen
     */
    fun stopRenderScreen() {
        mRenderHandler?.obtainMessage(MSG_GL_RELEASE)?.sendToTarget()
        mRenderThread?.quitSafely()
        mRenderThread = null
        mRenderHandler = null
    }

    /**
     * Start render codec
     *
     * @param inputSurface mediacodec input surface, see [android.media.MediaCodec]
     * @param width camera preview width
     * @param height camera preview height
     */
    fun startRenderCodec(inputSurface: Surface, width: Int, height: Int) {
        Triple(inputSurface, width, height).apply {
            mRenderHandler?.obtainMessage(MSG_GL_START_RENDER_CODEC, this)?.sendToTarget()
        }
    }

    /**
     * Stop render codec
     */
    fun stopRenderCodec() {
        mRenderHandler?.obtainMessage(MSG_GL_STOP_RENDER_CODEC)?.sendToTarget()
    }

    /**
     * Set render size
     *
     * @param w surface width
     * @param h surface height
     */
    fun setRenderSize(w: Int, h: Int) {
        mRenderHandler?.obtainMessage(MSG_GL_CHANGED_SIZE, Pair(w, h))?.sendToTarget()
    }

    /**
     * Add render effect
     *
     * @param effect add a effect, see [AbstractEffect]
     */
    fun addRenderEffect(effect: AbstractEffect?) {
        mRenderHandler?.obtainMessage(MSG_GL_ADD_EFFECT, effect)?.sendToTarget()
    }

    /**
     * Remove render effect
     *
     * @param effect a effect removed, see [AbstractEffect]
     */
    fun removeRenderEffect(effect: AbstractEffect?) {
        mRenderHandler?.obtainMessage(MSG_GL_REMOVE_EFFECT, effect)?.sendToTarget()
    }

    /**
     * Rotate camera render angle
     *
     * @param type rotate angle, null means rotating nothing
     * see [RotateType.ANGLE_90], [RotateType.ANGLE_270],...etc.
     */
    fun setRotateType(type: RotateType?) {
        mRenderHandler?.obtainMessage(MSG_GL_ROUTE_ANGLE, type)?.sendToTarget()
    }

    /**
     * Get cache render effect list
     * @return current effects
     */
    fun getCacheEffectList() = mCacheEffectList

    /**
     * Save image
     *
     * @param callBack capture image status, see [ICaptureCallBack]
     * @param path custom image path
     */
    fun saveImage(callBack: ICaptureCallBack?, path: String?) {
        this.mCaptureDataCb = callBack
        mRenderHandler?.obtainMessage(MSG_GL_SAVE_IMAGE, path)?.sendToTarget()
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        emitFrameRate()
        mRenderHandler?.obtainMessage(MSG_GL_DRAW)?.sendToTarget()
    }

    private fun startRenderCodecInternal(surface: Surface, w: Int, h: Int) {
        stopRenderCodecInternal()
        mRenderCodecThread = HandlerThread(RENDER_CODEC_THREAD)
        mRenderCodecThread?.start()
        mRenderCodecHandler = Handler(mRenderCodecThread!!.looper) { message ->
            when (message.what) {
                MSG_GL_RENDER_CODEC_INIT -> {
                    (message.obj as Pair<*, *>).apply {
                        val shareContext = first as EGLContext
                        val inputSurface = second as Surface
                        mEncodeRender = EncodeRender(mContext)
                        mEncodeRender?.initEGLEvn(shareContext)
                        mEncodeRender?.setupSurface(inputSurface)
                        mEncodeRender?.initGLES()
                    }
                }
                MSG_GL_RENDER_CODEC_CHANGED_SIZE -> {
                    (message.obj as Pair<*, *>).apply {
                        val width = first as Int
                        val height = second as Int
                        mEncodeRender?.setSize(width, height)
                    }
                }
                MSG_GL_RENDER_CODEC_DRAW -> {
                    (message.obj as Pair<*, *>).apply {
                        val textureId = first as Int
                        val timeStamps = second as Long
                        mEncodeRender?.drawFrame(textureId)
                        mEncodeRender?.swapBuffers(timeStamps)
                    }
                }
                MSG_GL_RENDER_CODEC_RELEASE -> {
                    mEncodeRender?.releaseGLES()
                    mEncodeRender = null
                }
            }
            true
        }
        mScreenRender?.getCurrentContext().let {
            if (it == null) {
                throw NullPointerException("Current EGLContext can't be null.")
            }
            mRenderCodecHandler?.obtainMessage(MSG_GL_RENDER_CODEC_INIT, Pair(it, surface))?.sendToTarget()
        }
        mRenderCodecHandler?.obtainMessage(MSG_GL_RENDER_CODEC_CHANGED_SIZE, Pair(w, h))?.sendToTarget()
    }

    private fun drawFrame2Codec(textureId: Int, timeStamps: Long) {
        Pair(textureId, timeStamps).apply {
            mRenderCodecHandler?.obtainMessage(MSG_GL_RENDER_CODEC_DRAW, this)?.sendToTarget()
        }
    }

    private fun stopRenderCodecInternal() {
        mRenderCodecHandler?.obtainMessage(MSG_GL_RENDER_CODEC_RELEASE)?.sendToTarget()
        mRenderCodecThread?.quitSafely()
        mRenderCodecThread = null
        mRenderCodecHandler = null
    }

    private fun saveImageInternal(savePath: String?) {
        if (mCaptureState.get()) {
            return
        }
        mCaptureState.set(true)
        mMainHandler.post {
            mCaptureDataCb?.onBegin()
        }
        val date = mDateFormat.format(System.currentTimeMillis())
        val title = savePath ?: "IMG_JJCamera_$date"
        val displayName = savePath ?: "$title.jpg"
        val path = savePath ?: "$mCameraDir/$displayName"
        val width = mWidth
        val height = mHeight
        val location = getGpsLocation()
        // 写入文件
        // glReadPixels读取的是大端数据，但是我们保存的是小端
        // 故需要将图片上下颠倒为正
        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(path)
            GLBitmapUtils.transFrameBufferToBitmap(mFBOId, width, height).apply {
                compress(Bitmap.CompressFormat.JPEG, 100, fos)
                recycle()
            }
        } catch (e: IOException) {
            mMainHandler.post {
                mCaptureDataCb?.onError(e.localizedMessage)
            }
            Logger.e(TAG, "Failed to write file, err = ${e.localizedMessage}", e)
        } finally {
            try {
                fos?.close()
            } catch (e: IOException) {
                Logger.e(TAG, "Failed to write file, err = ${e.localizedMessage}", e)
            }
        }
        // 判断是否保存成功
        // 如果成功，则更新图库
        val file = File(path)
        if (file.length() == 0L) {
            Logger.e(TAG, "Failed to save file $path")
            file.delete()
            mCaptureState.set(false)
            return
        }
        val values = ContentValues()
        values.put(MediaStore.Images.ImageColumns.TITLE, title)
        values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, displayName)
        values.put(MediaStore.Images.ImageColumns.DATA, path)
        values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, date)
        values.put(MediaStore.Images.ImageColumns.WIDTH, width)
        values.put(MediaStore.Images.ImageColumns.HEIGHT, height)
        values.put(MediaStore.Images.ImageColumns.LONGITUDE, location?.longitude)
        values.put(MediaStore.Images.ImageColumns.LATITUDE, location?.latitude)
        mContext.contentResolver?.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        mMainHandler.post {
            mCaptureDataCb?.onComplete(path)
        }
        mCaptureState.set(false)
        if (Utils.debugCamera) {
            Logger.i(TAG, "captureImageInternal save path = $path")
        }
    }

    private fun addLifecycleObserver(context: Context) {
        if (context !is LifecycleOwner) return
        context.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_DESTROY -> {
                        stopRenderScreen()
                    }
                    else -> {}
                }
            }
        })
    }

    private fun getGpsLocation(): Location? {
        mContext.let { ctx->
            val locationManager = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val locPermission = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
            if (locPermission == PackageManager.PERMISSION_GRANTED) {
                return locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            }
        }
        return null
    }

    private fun emitFrameRate() {
        mFrameRate++
        mEndTime = System.currentTimeMillis()
        if (mEndTime - mStartTime >= 1000) {
            if (Utils.debugCamera) {
                Logger.i(TAG, "camera render frame rate is $mFrameRate fps")
            }
            EventBus.with<Int>(BusKey.KEY_FRAME_RATE).postMessage(mFrameRate)
            mStartTime = mEndTime
            mFrameRate = 0
        }
    }

    /**
     * Camera surface texture listener
     *
     * @constructor Create empty Camera surface texture listener
     */
    interface CameraSurfaceTextureListener {
        /**
         * On surface texture available
         *
         * @param surfaceTexture camera render surface texture
         */
        fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture?)
    }

    companion object {
        private const val TAG = "RenderManager"
        private const val RENDER_THREAD = "gl_render"
        private const val RENDER_CODEC_THREAD = "gl_render_codec"
        // render
        private const val MSG_GL_INIT = 0x00
        private const val MSG_GL_DRAW = 0x01
        private const val MSG_GL_RELEASE = 0x02
        private const val MSG_GL_START_RENDER_CODEC = 0x03
        private const val MSG_GL_STOP_RENDER_CODEC = 0x04
        private const val MSG_GL_CHANGED_SIZE = 0x05
        private const val MSG_GL_ADD_EFFECT = 0x06
        private const val MSG_GL_REMOVE_EFFECT = 0x07
        private const val MSG_GL_SAVE_IMAGE = 0x08
        private const val MSG_GL_ROUTE_ANGLE = 0x09

        // codec
        private const val MSG_GL_RENDER_CODEC_INIT = 0x11
        private const val MSG_GL_RENDER_CODEC_CHANGED_SIZE = 0x12
        private const val MSG_GL_RENDER_CODEC_DRAW = 0x13
        private const val MSG_GL_RENDER_CODEC_RELEASE = 0x14
    }
}