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
package com.jiangdg.media.base

import android.graphics.SurfaceTexture
import android.view.*
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import com.jiangdg.media.CameraClient
import com.jiangdg.media.callback.ICaptureCallBack
import com.jiangdg.media.callback.IEncodeDataCallBack
import com.jiangdg.media.callback.IPlayCallBack
import com.jiangdg.media.callback.IPreviewDataCallBack
import com.jiangdg.media.camera.bean.CameraRequest
import com.jiangdg.media.render.filter.AbstractFilter
import com.jiangdg.media.render.filter.FilterBlackWhite
import com.jiangdg.media.render.filter.FilterZoom
import com.jiangdg.media.render.internal.CaptureRender
import com.jiangdg.media.widget.AspectRatioSurfaceView
import com.jiangdg.media.widget.AspectRatioTextureView
import com.jiangdg.media.widget.IAspectRatio
import java.lang.IllegalArgumentException

/** Extends from BaseFragment for CameraClient usage
 *
 * @author Created by jiangdg on 2022/1/21
 */
abstract class CameraFragment : BaseFragment() {
    private var mCameraClient: CameraClient? = null
    private var mCameraView: IAspectRatio? = null

    override fun initData() {
        val client = getCameraClient() ?: getDefault()
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
            client.getCameraRequest()?.apply {
                view.setAspectRatio(previewWidth, previewHeight)
            }
            mCameraView = view
        }
        mCameraClient = client
    }

    private fun handleTextureView(textureView: AspectRatioTextureView) {
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture?,
                width: Int,
                height: Int
            ) {
                openCamera(width, height, surface!!)
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
                val width = surfaceView.width
                val height = surfaceView.height
                openCamera(width, height, holder!!)
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

    protected fun captureImage(callBack: ICaptureCallBack, savePath: String? = null) {
        mCameraClient?.captureImage(callBack, savePath)
    }

    protected fun switchCamera(cameraId: String? = null) {
        mCameraClient?.switchCamera(cameraId)
    }

    protected fun updateResolution(width: Int, height: Int) {
        mCameraClient?.updateResolution(width, height)
    }

    protected fun getAllPreviewSizes(aspectRatio: Double? = null) = mCameraClient?.getAllPreviewSizes()

    protected fun addRenderFilter(filter: AbstractFilter) {
        mCameraClient?.addRenderFilter(filter)
    }

    protected fun removeRenderFilter(filter: AbstractFilter) {
        mCameraClient?.removeRenderFilter(filter)
    }

    protected fun startPush() {
        mCameraClient?.startPush()
    }

    protected fun stopPush() {
        mCameraClient?.stopPush()
    }

    protected fun addEncodeDataCallBack(callBack: IEncodeDataCallBack) {
        mCameraClient?.addEncodeDataCallBack(callBack)
    }

    protected fun addPreviewDataCallBack(callBack: IPreviewDataCallBack) {
        mCameraClient?.addPreviewDataCallBack(callBack)
    }

    protected fun captureVideoStart(callBack: ICaptureCallBack, path: String ?= null, durationInSec: Long = 0L) {
        mCameraClient?.captureVideoStart(callBack, path, durationInSec)
    }

    protected fun captureVideoStop() {
        mCameraClient?.captureVideoStop()
    }

    protected fun startPlayMic(callBack: IPlayCallBack? = null) {
        mCameraClient?.startPlayMic(callBack)
    }

    protected fun stopPlayMic() {
        mCameraClient?.stopPlayMic()
    }

    private fun openCamera(surfaceWidth: Int, surfaceHeight: Int, st: SurfaceTexture? = null) {
        mCameraClient?.openCamera(surfaceWidth, surfaceHeight, st)
    }

    private fun openCamera(surfaceWidth: Int, surfaceHeight: Int, holder: SurfaceHolder? = null) {
        mCameraClient?.openCamera(surfaceWidth, surfaceHeight, holder)
    }

    private fun closeCamera() {
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
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                ).apply {
                    gravity = getGravity()
                }
            }
            is RelativeLayout -> {
                RelativeLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
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

    protected abstract fun getCameraView(): IAspectRatio?
    protected abstract fun getCameraViewContainer(): ViewGroup?
    protected open fun getGravity() = Gravity.CENTER

    protected open fun getCameraClient(): CameraClient? {
        return null
    }

    private fun getDefault(): CameraClient {
        return CameraClient.newBuilder(requireContext())
            .setEnableGLES(true)
            .setDefaultFilter(FilterBlackWhite(requireContext()))
            .setCameraType(CameraClient.CameraType.UVC)
            .setCameraRequest(getCameraRequest())
            .openDebug(true)
            .build()
    }

    private fun getCameraRequest(): CameraRequest {
        return CameraRequest.CameraRequestBuilder()
            .setFrontCamera(false)
            .setContinuousAFModel(true)
            .setContinuousAutoModel(true)
            .setPreviewWidth(640)
            .setPreviewHeight(480)
            .create()
    }
}