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
package com.jiangdg.ausbc.render.internal

import android.content.Context
import android.opengl.GLES20
import com.jiangdg.ausbc.utils.Logger

/** A AbstractRender subclass, also abstract
 *       create a fbo,and draw to it instead of screen.
 *
 * Attention: Your should set your context as the current context before creating fbo,
 *      Otherwise GLES20.glCheckFramebufferStatus=0 on some other devices!
 *
 * @author Created by jiangdg on 2021/12/27
 */
abstract class AbstractFboRender(context: Context) : AbstractRender(context) {
    private var mFBOTextureId: Int = -1

    fun getFboTextureId() = mFBOTextureId

    override fun drawFrame(textureId: Int) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFBOTextureId)
        super.drawFrame(textureId)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    override fun setSize(width: Int, height: Int) {
        super.setSize(width, height)
        mFBOTextureId = createFBO(width, height)
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
        // 4. 解绑纹理和FBO
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        return fboBuffers[0]
    }

    companion object {
        private const val TAG = "AbstractFboRender"
    }
}