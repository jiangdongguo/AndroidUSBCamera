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
package com.jiangdg.ausbc.utils

import android.graphics.Bitmap
import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 *
 * @author Created by jiangdg on 2022/2/9
 */
object GLBitmapUtils {

    fun transFrameBufferToBitmap(frameBufferId: Int, width: Int, height: Int): Bitmap {
        val byteBuffer = ByteBuffer.allocateDirect(width * height * 4)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        return transFrameBufferToBitmap(frameBufferId, width, height, byteBuffer)
    }

    private fun transFrameBufferToBitmap(
        frameBufferId: Int, width: Int, height: Int,
        byteBuffer: ByteBuffer
    ): Bitmap {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBufferId)
        GLES20.glReadPixels(
            0,
            0,
            width,
            height,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            byteBuffer
        )
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap?.copyPixelsFromBuffer(byteBuffer)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        return bitmap
    }

    fun readPixelToByteBuffer(
        frameBufferId: Int, width: Int, height: Int,
        byteBuffer: ByteBuffer?
    ) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBufferId)
        GLES20.glReadPixels(
            0,
            0,
            width,
            height,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            byteBuffer
        )
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    fun readPixelToBitmap(width: Int, height: Int): Bitmap? {
        val byteBuffer = ByteBuffer.allocateDirect(width * height * 4)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        return readPixelToBitmapWithBuffer(width, height, byteBuffer)
    }

    /**
     * 直接readPixel保存到Bitmap, 复用byteBuffer
     *
     * @param width
     * @param height
     * @param byteBuffer
     * @return
     */
    private fun readPixelToBitmapWithBuffer(width: Int, height: Int, byteBuffer: ByteBuffer?): Bitmap? {
        if (byteBuffer == null) {
            return null
        }
        byteBuffer.clear()
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        GLES20.glReadPixels(
            0,
            0,
            width,
            height,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            byteBuffer
        )
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap?.copyPixelsFromBuffer(byteBuffer)
        return bitmap
    }
}