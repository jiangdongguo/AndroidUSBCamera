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
package com.jiangdg.media

import android.content.Context
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.Looper
import android.view.Surface
import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.jiangdg.media.callback.ICaptureCallBack
import com.jiangdg.media.callback.IEncodeDataCallBack
import com.jiangdg.media.callback.IPlayCallBack
import com.jiangdg.media.callback.IPreviewDataCallBack
import com.jiangdg.media.camera.*
import com.jiangdg.media.camera.bean.CameraRequest
import com.jiangdg.media.camera.bean.PreviewSize
import com.jiangdg.media.camera.callback.ICameraCallBack
import com.jiangdg.media.encode.AACEncodeProcessor
import com.jiangdg.media.encode.AbstractProcessor
import com.jiangdg.media.encode.H264EncodeProcessor
import com.jiangdg.media.encode.bean.RawData
import com.jiangdg.media.encode.muxer.Mp4Muxer
import com.jiangdg.media.render.RenderManager
import com.jiangdg.media.render.env.RotateType
import com.jiangdg.media.render.effect.AbstractEffect
import com.jiangdg.media.utils.Logger
import com.jiangdg.media.utils.Utils
import com.jiangdg.media.widget.AspectRatioSurfaceView
import com.jiangdg.media.widget.AspectRatioTextureView
import com.jiangdg.media.widget.IAspectRatio
import com.jiangdg.natives.YUVUtils
import java.lang.IllegalArgumentException

/**
 * Camera client
 *
 * @author Created by jiangdg on 2022/2/20
 */
class CameraClient internal constructor(builder: Builder) : IPreviewDataCallBack {
    private val mCtx: Context? = builder.context
    private val isEnableGLEs: Boolean = builder.enableGLEs
    private val mCamera: ICameraStrategy? = builder.camera
    private var mCameraView: IAspectRatio? = null
    private var mRequest: CameraRequest? = builder.cameraRequest
    private var mDefaultEffect: AbstractEffect? = builder.defaultEffect
    private val mEncodeBitRate: Int? = builder.videoEncodeBitRate
    private val mEncodeFrameRate: Int? = builder.videoEncodeFrameRate
    private val mDefaultRotateType: RotateType? = builder.defaultRotateType
    private var mAudioProcess: AbstractProcessor? = null
    private var mVideoProcess: AbstractProcessor? = null
    private var mMediaMuxer: Mp4Muxer? = null
    private val mMainHandler: Handler = Handler(Looper.getMainLooper())

    private val mRenderManager: RenderManager? by lazy {
        RenderManager(mCtx!!, mRequest!!.previewWidth, mRequest!!.previewHeight)
    }

    init {
        mRequest = mRequest ?: CameraRequest.CameraRequestBuilder().create()
        mCtx?.let { context ->
            addLifecycleObserver(context)
        }
        if (Utils.debugCamera) {
            Logger.i(TAG, "init camera client, camera = $mCamera")
        }
    }

    override fun onPreviewData(data: ByteArray?, format: IPreviewDataCallBack.DataFormat) {
        data?.let {
            val width = mRequest!!.previewWidth
            val height = mRequest!!.previewHeight
            when(format) {
                IPreviewDataCallBack.DataFormat.NV21 -> {
                    YUVUtils.nv21ToYuv420sp(data, width, height)
                }
                else -> {
                    throw IllegalArgumentException("Unsupported format")
                }
            }
            mVideoProcess?.putRawData(RawData(it, it.size))
        }
    }

    /**
     * Open camera
     *
     * @param cameraView camera render view, null means offscreen render
     */
    fun openCamera(cameraView: IAspectRatio?, isReboot: Boolean = false, callback: ICameraCallBack? = null) {
        Logger.i(TAG, "start open camera request = $mRequest, gl = $isEnableGLEs")
        initEncodeProcessor()
        val previewWidth = mRequest!!.previewWidth
        val previewHeight = mRequest!!.previewHeight
        when (cameraView) {
            is AspectRatioSurfaceView -> {
                cameraView.setAspectRatio(previewWidth, previewHeight)
                if (! isEnableGLEs) {
                    cameraView.postUITask {
                        mCamera?.startPreview(mRequest!!, cameraView.holder, callback)
                        mCamera?.addPreviewDataCallBack(this)
                    }
                }
                cameraView
            }
            is AspectRatioTextureView -> {
                cameraView.setAspectRatio(previewWidth, previewHeight)
                if (! isEnableGLEs) {
                    cameraView.postUITask {
                        mCamera?.startPreview(mRequest!!, cameraView.surfaceTexture, callback)
                        mCamera?.addPreviewDataCallBack(this)
                    }
                }
                cameraView
            }
            else -> {
                cameraView
            }
        }.also { view->
            // If view is null, should cache the last set
            // otherwise it can't recover the last status
            mCameraView = view ?: mCameraView

            // using opengl es
            // cameraView is null, means offscreen render
            if (! isEnableGLEs) return
            view.apply {
                val listener = object : RenderManager.CameraSurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture?) {
                        surfaceTexture?.let {
                            mCamera?.startPreview(mRequest!!, it, callback)
                        }
                    }
                }
                if (this == null) {
                    Logger.i(TAG, "Offscreen render, width=$previewWidth, height=$previewHeight")
                    mRenderManager?.startRenderScreen(previewWidth, previewHeight, null, listener)
                    if (isReboot) {
                        mRenderManager?.getCacheEffectList()?.forEach { effect ->
                            mRenderManager?.addRenderEffect(effect)
                        }
                        return@apply
                    }
                    mRenderManager?.addRenderEffect(mDefaultEffect)
                    return@apply
                }
                postUITask {
                    val surfaceWidth = getSurfaceWidth()
                    val surfaceHeight = getSurfaceHeight()
                    val surface = getSurface()
                    mRenderManager?.startRenderScreen(surfaceWidth, surfaceHeight, surface, listener)
                    mRenderManager?.setRotateType(mDefaultRotateType)
                    if (isReboot) {
                        mRenderManager?.getCacheEffectList()?.forEach { effect ->
                            mRenderManager?.addRenderEffect(effect)
                        }
                        return@postUITask
                    }
                    mRenderManager?.addRenderEffect(mDefaultEffect)
                    Logger.i(TAG, "Display render, width=$surfaceWidth, height=$surfaceHeight")
                }
            }
        }
    }

    /**
     * Close camera
     */
    fun closeCamera() {
        if (Utils.debugCamera) {
            Logger.i(TAG, "closeCamera")
        }
        releaseEncodeProcessor()
        if (isEnableGLEs) {
            mRenderManager?.stopRenderScreen()
        }
        mCamera?.stopPreview()
    }

    /**
     * Rotate camera render angle
     *
     * @param type rotate angle, null means rotating nothing
     * see [RotateType.ANGLE_90], [RotateType.ANGLE_270],...etc.
     */
    fun setRotateType(type: RotateType?) {
        mRenderManager?.setRotateType(type)
    }

    /**
     * Set render size
     *
     * @param width surface width
     * @param height surface height
     */
    fun setRenderSize(width: Int, height: Int) {
        mRenderManager?.setRenderSize(width, height)
    }

    /**
     * Add render effect.There is only one setting in the same category
     * <p>
     * The default effects:
     * @see [com.jiangdg.media.render.effect.EffectBlackWhite]
     * @see [com.jiangdg.media.render.effect.EffectZoom]
     * @see [com.jiangdg.media.render.effect.EffectSoul]
     * <p>
     * Of course, you can also realize a custom effect by extending from [AbstractEffect]
     *
     * @param effect a effect
     */
    fun addRenderEffect(effect: AbstractEffect) {
        mRenderManager?.addRenderEffect(effect)
    }

    /**
     * Remove render effect
     *
     * @param effect a effect, extending from [AbstractEffect]
     */
    fun removeRenderEffect(effect: AbstractEffect) {
        mRenderManager?.removeRenderEffect(effect)
    }

    /**
     * Update render effect
     *
     * @param classifyId effect classify id
     * @param effect new effect, null means set none
     */
    fun updateRenderEffect(classifyId: Int, effect: AbstractEffect?) {
        mRenderManager?.getCacheEffectList()?.find {
            it.getClassifyId() == classifyId
        }?.also {
            removeRenderEffect(it)
        }
        effect ?: return
        addRenderEffect(effect)
    }

    /**
     * Switch camera
     *
     * @param cameraId camera id
     */
    fun switchCamera(cameraId: String? = null) {
        if (Utils.debugCamera) {
            Logger.i(TAG, "switchCamera, id = $cameraId")
        }
        mCamera?.switchCamera(cameraId)
    }

    /**
     * Capture image
     *
     * @param callBack capture a image status, see [ICaptureCallBack]
     * @param path image save path, default is DICM/Camera
     */
    fun captureImage(callBack: ICaptureCallBack, path: String? = null) {
        if (Utils.debugCamera) {
            Logger.i(TAG, "captureImage...")
        }
        if (isEnableGLEs) {
            mRenderManager?.saveImage(callBack, path)
            return
        }
        mCamera?.captureImage(callBack, path)
    }

    /**
     * Start play mic
     *
     * @param callBack play mic status in real-time, see [IPlayCallBack]
     */
    fun startPlayMic(callBack: IPlayCallBack?) {
        (mAudioProcess as? AACEncodeProcessor)?.playAudioStart(callBack)
    }

    /**
     * Stop play mic
     */
    fun stopPlayMic() {
        (mAudioProcess as? AACEncodeProcessor)?.playAudioStop()
    }

    /**
     * Start push
     */
    fun startPush() {
        mVideoProcess?.startEncode()
        mAudioProcess?.startEncode()
    }

    /**
     * Start rec mp3
     *
     * @param mp3Path  mp3 save path
     * @param callBack record status, see [ICaptureCallBack]
     */
    fun captureAudioStart(callBack: ICaptureCallBack, mp3Path: String?=null) {
        val path = if (mp3Path.isNullOrEmpty()) {
            "${mCtx?.getExternalFilesDir(null)?.path}/${System.currentTimeMillis()}.mp3"
        } else {
            mp3Path
        }
        (mAudioProcess as? AACEncodeProcessor)?.recordMp3Start(path, callBack)
    }

    /**
     * Stop rec mp3
     */
    fun captureAudioStop() {
        (mAudioProcess as? AACEncodeProcessor)?.recordMp3Stop()
    }

    /**
     * Stop push
     */
    fun stopPush() {
        mVideoProcess?.stopEncode()
        mAudioProcess?.stopEncode()
    }

    /**
     * Add encode data call back
     *
     * @param callBack camera encoded data call back, see [IEncodeDataCallBack]
     */
    fun addEncodeDataCallBack(callBack: IEncodeDataCallBack) {
        mVideoProcess?.addEncodeDataCallBack(callBack)
        mAudioProcess?.addEncodeDataCallBack(callBack)
    }

    /**
     * Add preview data call back
     *
     * @param callBack camera preview data call back, see [IPreviewDataCallBack]
     */
    fun addPreviewDataCallBack(callBack: IPreviewDataCallBack) {
        if (isEnableGLEs) {
            mRenderManager?.addPreviewDataCallBack(callBack)
            return
        }
        mCamera?.addPreviewDataCallBack(callBack)
    }

    /**
     * Capture video start
     *
     * @param callBack capture result callback, see [ICaptureCallBack]
     * @param path video save path, default is DICM/Camera
     * @param durationInSec video file auto divide duration is seconds
     */
    fun captureVideoStart(callBack: ICaptureCallBack, path: String ?= null, durationInSec: Long = 0L) {
        mMediaMuxer = Mp4Muxer(mCtx, callBack, path, durationInSec)
        (mVideoProcess as? H264EncodeProcessor)?.apply {
            setEncodeRate(mEncodeBitRate, mEncodeFrameRate)
            startEncode()
            setMp4Muxer(mMediaMuxer!!, true)
            setOnEncodeReadyListener(object : H264EncodeProcessor.OnEncodeReadyListener {
                override fun onReady(surface: Surface?) {
                    if (! isEnableGLEs) {
                        return
                    }
                    if (surface == null) {
                        Logger.e(TAG, "Input surface can't be null.")
                        return
                    }
                    mRenderManager?.startRenderCodec(surface, width, height)
                }
            })
        }
        (mAudioProcess as? AACEncodeProcessor)?.apply {
            startEncode()
            setMp4Muxer(mMediaMuxer!!, false)
        }
    }

    /**
     * Capture video stop
     */
    fun captureVideoStop() {
        mRenderManager?.stopRenderCodec()
        mMediaMuxer?.release()
        mVideoProcess?.stopEncode()
        mAudioProcess?.stopEncode()
        mMediaMuxer = null
    }

    /**
     * Update resolution
     *
     * @param width camera preview width, see [PreviewSize]
     * @param height camera preview height, [PreviewSize]
     * @return result of operation
     */
    @MainThread
    fun updateResolution(width: Int, height: Int): Boolean {
        if (Utils.debugCamera) {
            Logger.i(TAG, "updateResolution size = ${width}x${height}")
        }
        getCameraRequest().apply {
            if (this == null) {
                Logger.e(TAG, "updateResolution failed, camera request is null.")
                return false
            }
            if (mVideoProcess?.isEncoding() == true) {
                Logger.e(TAG, "updateResolution failed, video recording...")
                return false
            }
            previewWidth = width
            previewHeight = height
            closeCamera()
            mMainHandler.postDelayed({
                openCamera(mCameraView, true)
            }, 500)
        }
        return true
    }


    /**
     * Get all preview sizes
     *
     * @param aspectRatio
     * @return [PreviewSize] list of camera
     */
    fun getAllPreviewSizes(aspectRatio: Double? = null): MutableList<PreviewSize>? {
        return mCamera?.getAllPreviewSizes(aspectRatio).apply {
            if (Utils.debugCamera) {
                Logger.i(TAG, "getAllPreviewSizes size = $this")
            }
        }
    }

    /**
     * Get camera request
     *
     * @return a camera request, see [CameraRequest]
     */
    fun getCameraRequest() = mRequest

    /**
     * Get camera strategy
     *
     * @return camera strategy, see [ICameraStrategy]
     */
    fun getCameraStrategy() = mCamera

    /**
     * Get default effect
     *
     * @return default effect, see [AbstractEffect]
     */
    fun getDefaultEffect() = mDefaultEffect

    private fun initEncodeProcessor() {
        releaseEncodeProcessor()
        val  encodeWidth = if (isEnableGLEs) {
            mRequest!!.previewHeight
        } else {
            mRequest!!.previewWidth
        }
        val encodeHeight = if (isEnableGLEs) {
            mRequest!!.previewWidth
        } else {
            mRequest!!.previewHeight
        }
        mAudioProcess = AACEncodeProcessor()
        mVideoProcess = H264EncodeProcessor(encodeWidth, encodeHeight, isEnableGLEs)
    }

    private fun releaseEncodeProcessor() {
        (mAudioProcess as? AACEncodeProcessor)?.playAudioStop()
        mVideoProcess?.stopEncode()
        mAudioProcess?.stopEncode()
        mVideoProcess = null
        mAudioProcess = null
    }

    private fun addLifecycleObserver(context: Context) {
        if (context !is LifecycleOwner) return
        context.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_DESTROY -> {
                        captureVideoStop()
                    }
                    else -> {}
                }
            }
        })
    }

    companion object {
        private const val TAG = "CameraClient"

        @JvmStatic
        fun newBuilder(ctx: Context) = Builder(ctx)
    }

    class Builder constructor() {
        internal var context: Context? = null
        internal var cameraRequest: CameraRequest? = null
        internal var enableGLEs: Boolean = true
        internal var camera: ICameraStrategy? = null
        internal var defaultEffect: AbstractEffect? = null
        internal var videoEncodeBitRate: Int? = null
        internal var videoEncodeFrameRate: Int? = null
        internal var defaultRotateType: RotateType? = null

        constructor(context: Context) : this() {
            this.context = context
        }

        /**
         * Set camera strategy
         * <p>
         * @param camera camera strategy, see [ICameraStrategy]
         * @return [CameraClient.Builder]
         */
        fun setCameraStrategy(camera: ICameraStrategy?): Builder {
            this.camera = camera
            return this
        }

        /**
         * Set camera request
         * <p>
         * @param request camera request, see [CameraRequest]
         * @return [CameraClient.Builder]
         */
        fun setCameraRequest(request: CameraRequest): Builder {
            this.cameraRequest = request
            return this
        }

        /**
         * Set enable opengl es
         * <p>
         * @param enable should render by opengl es,
         *      If you want to offscreen rendering, you must set it to true!
         * @return [CameraClient.Builder]
         */
        fun setEnableGLES(enable: Boolean): Builder {
            this.enableGLEs = enable
            return this
        }

        /**
         * Set default effect
         * <p>
         * @param effect default effect,
         *  see [com.jiangdg.media.render.effect.EffectBlackWhite], [com.jiangdg.media.render.effect.EffectZoom] etc.
         * @return [CameraClient.Builder]
         */
        fun setDefaultEffect(effect: AbstractEffect): Builder {
            this.defaultEffect = effect
            return this
        }

        /**
         * Set video encode bit rate
         *
         * @param bitRate bit rate for h264 encoding
         * @return [CameraClient.Builder]
         */
        fun setVideoEncodeBitRate(bitRate: Int): Builder {
            this.videoEncodeBitRate = bitRate
            return this
        }

        /**
         * Set video encode frame rate
         *
         * @param frameRate frame rate for h264 encoding
         * @return [CameraClient.Builder]
         */
        fun setVideoEncodeFrameRate(frameRate: Int): Builder {
            this.videoEncodeFrameRate = frameRate
            return this
        }

        /**
         * Open debug
         *
         * @param debug debug switch
         * @return [CameraClient.Builder]
         */
        fun openDebug(debug: Boolean): Builder {
            Utils.debugCamera = debug
            return this
        }

        /**
         * Set default camera render angle
         *
         * @param type rotate angle, null means rotating nothing
         * see [RotateType.ANGLE_90], [RotateType.ANGLE_270],...etc.
         */
        fun setDefaultRotateType(type: RotateType?): Builder {
            this.defaultRotateType = type
            return this
        }

        override fun toString(): String {
            return "Builder(context=$context, cameraType=$camera, " +
                    "cameraRequest=$cameraRequest, glEsVersion=$enableGLEs)"
        }

        /**
         * Build for [CameraClient]
         *
         * @return [CameraClient.Builder]
         */
        fun build() = CameraClient(this)
    }
}

