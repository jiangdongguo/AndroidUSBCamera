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
package com.jiangdg.ausbc.base

import android.graphics.SurfaceTexture
import android.view.*
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import com.jiangdg.ausbc.CameraClient
import com.jiangdg.ausbc.callback.ICaptureCallBack
import com.jiangdg.ausbc.callback.IEncodeDataCallBack
import com.jiangdg.ausbc.callback.IPlayCallBack
import com.jiangdg.ausbc.callback.IPreviewDataCallBack
import com.jiangdg.ausbc.camera.CameraUvcStrategy
import com.jiangdg.ausbc.camera.ICameraStrategy
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.camera.bean.PreviewSize
import com.jiangdg.ausbc.render.env.RotateType
import com.jiangdg.ausbc.render.effect.AbstractEffect
import com.jiangdg.ausbc.widget.AspectRatioSurfaceView
import com.jiangdg.ausbc.widget.AspectRatioTextureView
import com.jiangdg.ausbc.widget.IAspectRatio
import java.lang.IllegalArgumentException

/** Extends from BaseFragment for CameraClient usage
 *
 * @author Created by jiangdg on 2022/1/21
 */
abstract class CameraFragment : BaseFragment() {
    private var mCameraClient: CameraClient? = null

    override fun initData() {
        mCameraClient = getCameraClient() ?: getDefault()
        when (val cameraView = getCameraView()) {
            is AspectRatioTextureView -> {
                handleTextureView(cameraView)
                cameraView
            }
            is AspectRatioSurfaceView -> {
                handleSurfaceView(cameraView)
                cameraView
            }
            else -> {
                null
            }
        }?.let { view->
            getCameraViewContainer()?.apply {
                removeAllViews()
                addView(view, getViewLayoutParams(this))
            }
        }
    }

    private fun handleTextureView(textureView: AspectRatioTextureView) {
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture?,
                width: Int,
                height: Int
            ) {
                openCamera(textureView)
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture?,
                width: Int,
                height: Int
            ) {
                surfaceSizeChanged(width, height)
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                closeCamera()
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
            }
        }
    }

    private fun handleSurfaceView(surfaceView: AspectRatioSurfaceView) {
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder?) {
                openCamera(surfaceView)
            }

            override fun surfaceChanged(
                holder: SurfaceHolder?,
                format: Int,
                width: Int,
                height: Int
            ) {
                surfaceSizeChanged(width, height)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder?) {
                closeCamera()
            }
        })
    }

    /**
     * Capture image
     *
     * @param callBack capture status, see [ICaptureCallBack]
     * @param savePath custom image path
     */
    protected fun captureImage(callBack: ICaptureCallBack, savePath: String? = null) {
        mCameraClient?.captureImage(callBack, savePath)
    }

    /**
     * Switch camera
     *
     * @param cameraId camera id
     */
    protected fun switchCamera(cameraId: String? = null) {
        mCameraClient?.switchCamera(cameraId)
    }

    /**
     * Is camera opened
     *
     * @return camera open status
     */
    protected fun isCameraOpened() = mCameraClient?.isCameraOpened()  ?: false

    /**
     * Update resolution
     *
     * @param width camera preview width, see [com.jiangdg.ausbc.camera.bean.PreviewSize]
     * @param height camera preview height, see [com.jiangdg.ausbc.camera.bean.PreviewSize]
     */
    protected fun updateResolution(width: Int, height: Int) {
        mCameraClient?.updateResolution(width, height)
    }

    /**
     * Get all preview sizes
     *
     * @param aspectRatio preview size aspect ratio,
     *                      null means getting all preview sizes
     */
    protected fun getAllPreviewSizes(aspectRatio: Double? = null) = mCameraClient?.getAllPreviewSizes(aspectRatio)

    /**
     * Add render effect
     *
     * @param effect a effect will be added, only enable opengl render worked, see [AbstractEffect]
     */
    protected fun addRenderEffect(effect: AbstractEffect) {
        mCameraClient?.addRenderEffect(effect)
    }

    /**
     * Remove render effect
     *
     * @param effect a effect will be removed, only enable opengl render worked, see [AbstractEffect]
     */
    protected fun removeRenderEffect(effect: AbstractEffect) {
        mCameraClient?.removeRenderEffect(effect)
    }

    /**
     * Update render effect
     *
     * @param classifyId effect classify id
     * @param effect new effect, null means set none
     */
    protected fun updateRenderEffect(classifyId: Int, effect: AbstractEffect?) {
        mCameraClient?.updateRenderEffect(classifyId, effect)
    }

    /**
     * Start push
     */
    protected fun startPush() {
        mCameraClient?.startPush()
    }

    /**
     * Stop push
     */
    protected fun stopPush() {
        mCameraClient?.stopPush()
    }

    /**
     * Add encode data call back
     *
     * @param callBack encode data call back, see [IEncodeDataCallBack]
     */
    protected fun addEncodeDataCallBack(callBack: IEncodeDataCallBack) {
        mCameraClient?.addEncodeDataCallBack(callBack)
    }

    /**
     * Add preview data call back
     *
     * @param callBack preview data call back, see [IPreviewDataCallBack]
     */
    protected fun addPreviewDataCallBack(callBack: IPreviewDataCallBack) {
        mCameraClient?.addPreviewDataCallBack(callBack)
    }

    /**
     * Capture video start
     *
     * @param callBack capture status, see [ICaptureCallBack]
     * @param path custom save path
     * @param durationInSec divided record duration time in seconds
     */
    protected fun captureVideoStart(callBack: ICaptureCallBack, path: String ?= null, durationInSec: Long = 0L) {
        mCameraClient?.captureVideoStart(callBack, path, durationInSec)
    }

    /**
     * Capture video stop
     */
    protected fun captureVideoStop() {
        mCameraClient?.captureVideoStop()
    }

    /**
     * Capture audio start
     *
     * @param callBack capture status, see [ICaptureCallBack]
     * @param path custom save path
     */
    protected fun captureAudioStart(callBack: ICaptureCallBack, path: String ?= null) {
        mCameraClient?.captureAudioStart(callBack, path)
    }

    /**
     * Capture audio stop
     */
    protected fun captureAudioStop() {
        mCameraClient?.captureAudioStop()
    }

    /**
     * Start play mic
     *
     * @param callBack play mic in real-time, see [IPlayCallBack]
     */
    protected fun startPlayMic(callBack: IPlayCallBack? = null) {
        mCameraClient?.startPlayMic(callBack)
    }

    /**
     * Stop play mic
     */
    protected fun stopPlayMic() {
        mCameraClient?.stopPlayMic()
    }

    /**
     * Get current preview size
     *
     * @return camera preview size, see [PreviewSize]
     */
    protected fun getCurrentPreviewSize(): PreviewSize? {
        return mCameraClient?.getCameraRequest()?.let {
            PreviewSize(it.previewWidth, it.previewHeight)
        }
    }

    /**
     * Get current camera strategy
     *
     * @return camera strategy, see [ICameraStrategy]
     */
    protected fun getCurrentCameraStrategy() = mCameraClient?.getCameraStrategy()

    /**
     * Get default effect
     *
     * @return default effect, see [AbstractEffect]
     */
    protected fun getDefaultEffect() = mCameraClient?.getDefaultEffect()

    /**
     * Rotate camera angle
     *
     * @param type rotate angle, null means rotating nothing
     * see [RotateType.ANGLE_90], [RotateType.ANGLE_270],...etc.
     */
    protected fun setRotateType(type: RotateType) {
        mCameraClient?.setRotateType(type)
    }

    /**
     * Send camera command of uvc camera
     *
     * @param command hex value
     * @return control result
     */
    protected fun sendCameraCommand(command: Int): Int? {
        return mCameraClient?.sendCameraCommand(command)
    }

    protected fun openCamera(st: IAspectRatio? = null) {
        mCameraClient?.openCamera(st)
    }

    protected fun closeCamera() {
        mCameraClient?.closeCamera()
    }

    private fun surfaceSizeChanged(surfaceWidth: Int, surfaceHeight: Int) {
        mCameraClient?.setRenderSize(surfaceWidth, surfaceHeight)
    }

    private fun getViewLayoutParams(viewGroup: ViewGroup): ViewGroup.LayoutParams {
        return when(viewGroup) {
            is FrameLayout -> {
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    getGravity()
                )
            }
            is LinearLayout -> {
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                ).apply {
                    gravity = getGravity()
                }
            }
            is RelativeLayout -> {
                RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT
                ).apply{
                    when(getGravity()) {
                        Gravity.TOP -> {
                            addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE)
                        }
                        Gravity.BOTTOM -> {
                            addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                        }
                        else -> {
                            addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE)
                            addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE)
                        }
                    }
                }
            }
            else -> throw IllegalArgumentException("Unsupported container view, " +
                    "you can use FrameLayout or LinearLayout or RelativeLayout")
        }
    }

    /**
     * Get camera view
     *
     * @return CameraView, such as AspectRatioTextureView etc.
     */
    protected abstract fun getCameraView(): IAspectRatio?

    /**
     * Get camera view container
     *
     * @return camera view container, such as FrameLayout ect
     */
    protected abstract fun getCameraViewContainer(): ViewGroup?

    /**
     * Camera render view show gravity
     */
    protected open fun getGravity() = Gravity.CENTER

    /**
     * Get camera client
     *
     * @return camera client, you can custom it, see [getDefault]
     */
    protected open fun getCameraClient(): CameraClient? {
        return null
    }

    private fun getDefault(): CameraClient {
        return CameraClient.newBuilder(requireContext())
            .setEnableGLES(true)
            .setRawImage(true)
            .setCameraStrategy(CameraUvcStrategy(requireContext()))
            .setCameraRequest(getCameraRequest())
            .setDefaultRotateType(RotateType.ANGLE_0)
            .openDebug(true)
            .build()
    }

    private fun getCameraRequest(): CameraRequest {
        return CameraRequest.Builder()
            .setFrontCamera(false)
            .setPreviewWidth(640)
            .setPreviewHeight(480)
            .create()
    }
}