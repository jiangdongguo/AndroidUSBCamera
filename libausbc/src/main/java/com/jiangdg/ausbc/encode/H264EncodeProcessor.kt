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
package com.jiangdg.ausbc.encode

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.view.Surface
import com.jiangdg.ausbc.utils.Logger
import com.jiangdg.ausbc.utils.Utils
import java.lang.Exception

/**
 * Encode h264 by MediaCodec
 *
 * @property width yuv width
 * @property height yuv height
 * @property gLESRender rendered by opengl flag
 * @author Created by jiangdg on 2022/2/10
 */
class H264EncodeProcessor(
    val width: Int,
    val height: Int,
    private val gLESRender: Boolean = true
) : AbstractProcessor() {

    private var mFrameRate: Int? = null
    private var mBitRate: Int? = null
    private var mReadyListener: OnEncodeReadyListener? = null

    override fun getThreadName(): String = TAG

    override fun handleStartEncode() {
        try {
            val mediaFormat = MediaFormat.createVideoFormat(MIME, width, height)
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mFrameRate ?: FRAME_RATE)
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate ?: getEncodeBitrate(width, height))
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, KEY_FRAME_INTERVAL)
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, getSupportColorFormat())
            mMediaCodec = MediaCodec.createEncoderByType(MIME)
            mMediaCodec?.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            if (gLESRender) {
                mReadyListener?.onReady(mMediaCodec?.createInputSurface())
            }
            mMediaCodec?.start()
            mEncodeState.set(true)
            if (Utils.debugCamera) {
                Logger.i(TAG, "init h264 media codec success.")
            }
            doEncodeData()
        } catch (e: Exception) {
            Logger.e(TAG, "start h264 media codec failed, err = ${e.localizedMessage}", e)
        }
    }

    override fun handleStopEncode() {
        try {
            mEncodeState.set(false)
            mMediaCodec?.stop()
            mMediaCodec?.release()
            if (Utils.debugCamera) {
                Logger.i(TAG, "release h264 media codec success.")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Stop mediaCodec failed, err = ${e.localizedMessage}", e)
        } finally {
            mRawDataQueue.clear()
            mMediaCodec = null
        }
    }

    override fun getPTSUs(bufferSize: Int): Long = System.nanoTime() / 1000L

    /**
     * Set on encode ready listener
     *
     * @param listener input surface ready listener
     */
    fun setOnEncodeReadyListener(listener: OnEncodeReadyListener) {
        this.mReadyListener = listener
    }

    private fun getSupportColorFormat(): Int {
        if (gLESRender) {
            return MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        }
        return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
    }

    private fun getEncodeBitrate(width: Int, height: Int): Int {
        var bitRate = width * height * 20 * 3 * 0.07F
        if (width >= 1920 || height >= 1920) {
            bitRate *= 0.75F
        } else if (width >= 1280 || height >= 1280) {
            bitRate *= 1.2F
        } else if (width >= 640 || height >= 640) {
            bitRate *= 1.4F
        }
        return bitRate.toInt()
    }

    /**
     * Set encode rate
     *
     * @param bitRate encode bit rate, kpb/s
     * @param frameRate encode frame rate, fp/s
     */
    fun setEncodeRate(bitRate: Int?, frameRate: Int?) {
        this.mBitRate = bitRate
        this.mFrameRate = frameRate
    }

    /**
     * On encode ready listener
     */
    interface OnEncodeReadyListener {
        /**
         * On ready
         *
         * @param surface mediacodec input surface for getting raw data
         */
        fun onReady(surface: Surface?)
    }

    companion object {
        private const val TAG = "H264EncodeProcessor"
        private const val MIME = "video/avc"
        private const val FRAME_RATE = 30
        private const val KEY_FRAME_INTERVAL = 1
    }
}