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
package com.jiangdg.demo

import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.jiangdg.ausbc.base.BaseFragment
import com.jiangdg.ausbc.camera.ICameraStrategy
import com.jiangdg.ausbc.camera.Camera1Strategy
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.utils.Logger
import com.jiangdg.ausbc.utils.ToastUtils
import com.jiangdg.ausbc.widget.AspectRatioGLSurfaceView
import com.jiangdg.demo.databinding.FragmentGlsurfaceBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 *
 * @author Created by jiangdg on 2022/2/24
 */
class GlSurfaceFragment: BaseFragment() {

    private lateinit var mViewBinding: FragmentGlsurfaceBinding
    private val mCamera: ICameraStrategy by lazy {
        Camera1Strategy(requireContext())
    }

    override fun getRootView(inflater: LayoutInflater, container: ViewGroup?): View {
        mViewBinding = FragmentGlsurfaceBinding.inflate(inflater, container, false)
        return mViewBinding.root
    }

    override fun initView() {
        super.initView()
        mViewBinding.glSurfaceView.setDefaultBufferSize(CAMERA_PREVIEW_WIDTH, CAMERA_PREVIEW_HEIGHT)
        mViewBinding.glSurfaceView.setAspectRatio(CAMERA_PREVIEW_WIDTH, CAMERA_PREVIEW_HEIGHT)
        mViewBinding.glSurfaceView.setOnSurfaceLifecycleListener(object : AspectRatioGLSurfaceView.OnSurfaceLifecycleListener {
            override fun onSurfaceCreated(surface: SurfaceTexture?) {
                if (surface == null) {
                    ToastUtils.show("预览失败，SurfaceTexture=null")
                    return
                }
                lifecycleScope.launch(Dispatchers.IO) {
                    val id = createFBO(CAMERA_PREVIEW_WIDTH, CAMERA_PREVIEW_HEIGHT)
                    withContext(Dispatchers.Main) {
                        ToastUtils.show("--->$id}")
                    }
                }

                mCamera.startPreview(getCameraRequest(), surface)
            }

            override fun onSurfaceDestroyed() {
                mCamera.stopPreview()
            }
        })
    }

    private fun getCameraRequest(): CameraRequest {
        return CameraRequest.Builder()
            .setFrontCamera(false)
            .setPreviewWidth(CAMERA_PREVIEW_WIDTH)
            .setPreviewHeight(CAMERA_PREVIEW_HEIGHT)
            .create()
    }

    private fun createFBO(width: Int, height: Int): Int {
        val fboBuffers = IntArray(1)
        // 1. 创建、绑定FBO
        GLES20.glGenFramebuffers(1, fboBuffers, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboBuffers[0])

        // 2. 创建FBO（普通）纹理，将其绑定到FBO
        val fboId = createTexture()
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER,
            GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D,
            fboId,
            0
        )

        // 3. 设置FBO分配内存的大小
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D, 0,
            GLES20.GL_RGBA, width, height, 0,
            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null
        )
        val status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Logger.e(TAG, "glFramebufferTexture2D err = ${GLES20.glGetError()}")
        }

        lifecycleScope.launch(Dispatchers.Main) {
            ToastUtils.show("dddd-->${status}")
        }

        // 4. 解绑纹理和FBO
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        return fboBuffers[0]
    }

    private fun createTexture(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        Logger.i(TAG, "create texture, id = ${textures[0]}")
        return textures[0]
    }

    companion object {
        private const val TAG = "GlSurfaceFragment"
        private const val CAMERA_PREVIEW_WIDTH = 1280
        private const val CAMERA_PREVIEW_HEIGHT = 720
    }
}