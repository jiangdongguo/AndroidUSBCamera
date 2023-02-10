/*
 * Copyright 2017-2023 Jiangdg
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
package com.jiangdg.ausbc.render.internal

import android.content.Context
import android.view.Surface
import com.jiangdg.ausbc.R
import com.jiangdg.ausbc.render.env.EGLEvn
/** Inherit from AbstractFboRender
 *      render data to screen from fbo with base_vertex.glsl and base_fragment.glsl
 *
 * @author Created by jiangdg on 2021/12/27
 */
class ScreenRender(context: Context) : AbstractRender(context) {
    private var mEgl: EGLEvn? = null

    fun initEGLEvn() {
        mEgl = EGLEvn()
        mEgl?.initEgl()
    }

    fun setupSurface(surface: Surface?, surfaceWidth: Int = 0, surfaceHeight: Int = 0) {
        mEgl?.setupSurface(surface, surfaceWidth, surfaceHeight)
        mEgl?.eglMakeCurrent()
    }

    fun swapBuffers(timeStamp: Long) {
        mEgl?.setPresentationTime(timeStamp)
        mEgl?.swapBuffers()
    }

    fun getCurrentContext() = mEgl?.getEGLContext()

    override fun clear() {
        mEgl?.releaseElg()
        mEgl = null
    }

    override fun getVertexSourceId(): Int = R.raw.base_vertex

    override fun getFragmentSourceId(): Int = R.raw.base_fragment
}