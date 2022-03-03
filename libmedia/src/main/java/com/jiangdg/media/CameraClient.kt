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
import android.os.Build
import android.view.Surface
import android.view.SurfaceHolder
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
import com.jiangdg.media.encode.AACEncodeProcessor
import com.jiangdg.media.encode.AbstractProcessor
import com.jiangdg.media.encode.H264EncodeProcessor
import com.jiangdg.media.encode.bean.RawData
import com.jiangdg.media.encode.muxer.Mp4Muxer
import com.jiangdg.media.render.RenderManager
import com.jiangdg.media.render.filter.AbstractFilter
import com.jiangdg.media.utils.Logger
import com.jiangdg.media.utils.Utils
import com.jiangdg.natives.YUVUtils
import java.lang.IllegalArgumentException
import java.lang.NullPointerException

/**
 * Camera client
 *
 * @author Created by jiangdg on 2022/2/20
 */
class CameraClient internal constructor(builder: Builder) {

    private val mCtx: Context? = builder.context
    private val isEnableGLEs: Boolean = builder.enableGLEs
    private val mCameraType: CameraType? = builder.cameraType
    private var mRequest: CameraRequest? = builder.cameraRequest
    private var mDefaultFilter: AbstractFilter? = builder.defaultFilter
    private val mEncodeBitRate: Int? = builder.videoEncodeBitRate
    private val mEncodeFrameRate: Int? = builder.videoEncodeFrameRate

    private var mCamera: AbstractCamera? = null
    private var mAudioProcess: AbstractProcessor? = null
    private var mVideoProcess: AbstractProcessor? = null
    private var mMediaMuxer: Mp4Muxer? = null

    private val mRenderManager: RenderManager? by lazy {
        RenderManager(mCtx!!, mRequest!!.previewWidth, mRequest!!.previewHeight)
    }

    init {
       mCamera = when(mCameraType) {
            CameraType.V1 -> CameraV1(mCtx!!)
            CameraType.V2 -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    CameraV2(mCtx!!)
                } else {
                    CameraV1(mCtx!!)
                }
            }
            CameraType.VX -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    CameraVx(mCtx!!)
                } else {
                    CameraV1(mCtx!!)
                }
            }
            else -> CameraUvc(mCtx!!)
        }
        mRequest = mRequest ?: CameraRequest.CameraRequestBuilder().create()
        addLifecycleObserver(mCtx)
        if (Utils.debugCamera) Logger.i(TAG, "init camera client, camera = ${mCameraType?.info}")
    }

    /**
     * Open camera
     *
     * @param surfaceW surface width
     * @param surfaceH surface height
     * @param holder surface holder, null means offscreen render
     */
    fun openCamera(surfaceW: Int, surfaceH: Int, holder: SurfaceHolder?) {
        if (Utils.debugCamera) Logger.i(TAG, "openCamera request = $mRequest, gl = $isEnableGLEs")
        initEncodeProcessor()
        if (! isEnableGLEs) {
            if (holder == null) {
                throw NullPointerException("SurfaceHolder can't be null when gles is not enabled")
            }
            mCamera?.startPreview(mRequest!!, holder)
            mCamera?.addPreviewDataCallBack(object : IPreviewDataCallBack {
                override fun onPreviewData(
                    data: ByteArray?,
                    format: IPreviewDataCallBack.DataFormat) {
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
            })
            return
        }
        // using opengl es
        val listener = object : RenderManager.CameraSurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture?) {
                surfaceTexture?.let {
                    mCamera?.startPreview(mRequest!!, it)
                }
            }
        }
        mRenderManager?.startRenderScreen(surfaceW, surfaceH, holder?.surface, listener)
        mRenderManager?.addRenderFilter(mDefaultFilter)
    }

    /**
     * Open camera
     *
     * @param surfaceW surface width
     * @param surfaceH surface height
     * @param surfaceTexture surface texture, null means offscreen render
     */
    fun openCamera(surfaceW: Int, surfaceH: Int, surfaceTexture: SurfaceTexture?) {
        if (Utils.debugCamera) Logger.i(TAG, "openCamera request = $mRequest, gl = $isEnableGLEs")
        initEncodeProcessor()
        if (! isEnableGLEs) {
            if (surfaceTexture == null) {
                throw NullPointerException("SurfaceTexture can't be null when OpenGL is not enabled")
            }
            mCamera?.startPreview(mRequest!!, surfaceTexture)
            mCamera?.addPreviewDataCallBack(object : IPreviewDataCallBack {
                override fun onPreviewData(
                    data: ByteArray?,
                    format: IPreviewDataCallBack.DataFormat) {
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
            })
            return
        }
        // use opengl es
        val listener = object : RenderManager.CameraSurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture?) {
                surfaceTexture?.let {
                    mCamera?.startPreview(mRequest!!, it)
                }
            }
        }
        val surface = if (surfaceTexture == null) {
            null
        } else {
            Surface(surfaceTexture)
        }
        mRenderManager?.startRenderScreen(surfaceW, surfaceH, surface, listener)
        mRenderManager?.addRenderFilter(mDefaultFilter)
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
     * Set render size
     *
     * @param width surface width
     * @param height surface height
     */
    fun setRenderSize(width: Int, height: Int) {
        mRenderManager?.setRenderSize(width, height)
    }

    /**
     * Add render filter
     * <p>
     * The default filters:
     * @see [com.jiangdg.media.render.filter.FilterBlackWhite]
     * @see [com.jiangdg.media.render.filter.FilterZoom]
     * @see [com.jiangdg.media.render.filter.FilterSoul]
     * <p>
     * Of course, you can also realize a custom filter by extending from [AbstractFilter]
     *
     * @param filter a filter
     */
    fun addRenderFilter(filter: AbstractFilter) {
        mRenderManager?.addRenderFilter(filter)
    }

    /**
     * Remove render filter
     *
     * @param filter a filter, extending from [AbstractFilter]
     */
    fun removeRenderFilter(filter: AbstractFilter) {
        mRenderManager?.removeRenderFilter(filter)
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
            Logger.i(TAG, "takePicture")
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
     */
    fun updateResolution(width: Int, height: Int) {
        if (Utils.debugCamera) {
            Logger.i(TAG, "updateResolution size = ${width}x${height}")
        }
        mCamera?.updateResolution(width, height)
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

    private fun initEncodeProcessor() {
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
        internal var cameraType: CameraType? = null
        internal var defaultFilter: AbstractFilter? = null
        internal var videoEncodeBitRate: Int? = null
        internal var videoEncodeFrameRate: Int? = null

        constructor(context: Context) : this() {
            this.context = context
        }

        /**
         * Set camera type
         * <p>
         * @param camera camera type, see [CameraType]
         * @return [CameraClient.Builder]
         */
        fun setCameraType(camera: CameraType?): Builder {
            this.cameraType = camera
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
         * Set default filter
         * <p>
         * @param filter default filter,
         *  see [com.jiangdg.media.render.filter.FilterBlackWhite], [com.jiangdg.media.render.filter.FilterZoom] etc.
         * @return [CameraClient.Builder]
         */
        fun setDefaultFilter(filter: AbstractFilter): Builder {
            this.defaultFilter = filter
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

        override fun toString(): String {
            return "Builder(context=$context, cameraType=$cameraType, " +
                    "cameraRequest=$cameraRequest, glEsVersion=$enableGLEs)"
        }

        /**
         * Build for [CameraClient]
         *
         * @return [CameraClient.Builder]
         */
        fun build() = CameraClient(this)
    }

    /**
     * Camera type
     *
     * @property info description of camera
     */
    enum class CameraType(val info: String? = null) {
        /**
         * Using camera v1, see [CameraV1]
         */
        V1("camera1"),

        /**
         * Using camera v2, see [CameraV2]
         */
        V2("camera2"),

        /**
         * Using camera x, see [CameraVx]
         */
        VX("camerax"),

        /**
         * Using camera uvc, see [CameraUvc]
         */
        UVC("uvc camera")
    }
}

