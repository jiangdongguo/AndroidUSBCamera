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
package com.jiangdg.media.encode

import android.media.MediaCodec
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import com.jiangdg.media.callback.IEncodeDataCallBack
import com.jiangdg.media.encode.bean.RawData
import com.jiangdg.media.encode.muxer.Mp4Muxer
import com.jiangdg.media.utils.Logger
import com.jiangdg.media.utils.Utils
import java.lang.Exception
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/** Abstract processor
 *
 * @author Created by jiangdg on 2022/2/10
 */
abstract class AbstractProcessor {
    private var mEncodeThread: HandlerThread? = null
    private var mEncodeHandler: Handler? = null
    protected var mMediaCodec: MediaCodec? = null
    private var mMp4Muxer: Mp4Muxer? = null
    private var isVideo: Boolean = false
    private var mEncodeDataCb: IEncodeDataCallBack? = null
    protected val mRawDataQueue: ConcurrentLinkedQueue<RawData> = ConcurrentLinkedQueue()

    protected val mEncodeState: AtomicBoolean by lazy {
        AtomicBoolean(false)
    }

    fun startEncode() {
        mEncodeThread = HandlerThread(this.getThreadName())
        mEncodeThread?.start()
        mEncodeHandler = Handler(mEncodeThread!!.looper) { msg ->
            when (msg.what) {
                MSG_START -> {
                    handleStartEncode()
                }
                MSG_STOP -> {
                    handleStopEncode()
                }
            }
            true
        }
        mEncodeHandler?.obtainMessage(MSG_START)?.sendToTarget()
    }

    fun stopEncode() {
        mEncodeState.set(false)
        mEncodeHandler?.obtainMessage(MSG_STOP)?.sendToTarget()
        mEncodeThread?.quitSafely()
        mEncodeThread = null
        mEncodeHandler = null
        mEncodeDataCb = null
    }

    fun addEncodeDataCallBack(callBack: IEncodeDataCallBack) {
        this.mEncodeDataCb = callBack
    }

    @Synchronized
    fun setMp4Muxer(muxer: Mp4Muxer, isVideo: Boolean) {
        this.mMp4Muxer = muxer
        this.isVideo = isVideo
        mMediaCodec?.outputFormat?.let { format->
            mMp4Muxer?.addTracker(format, isVideo)
        }
    }

    fun putRawData(data: RawData) {
        if (! mEncodeState.get()) {
            return
        }
        if (mRawDataQueue.size >= MAX_QUEUE_SIZE) {
            mRawDataQueue.poll()
        }
        mRawDataQueue.offer(data)
    }

    protected abstract fun getThreadName(): String
    protected abstract fun handleStartEncode()
    protected abstract fun handleStopEncode()
    protected abstract fun getPTSUs(bufferSize: Int): Long

    protected fun isLowerLollipop() = Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP

    protected fun doEncodeData() {
        while (mEncodeState.get()) {
            try {
                queueFrameIfNeed()
                val bufferInfo = MediaCodec.BufferInfo()
                var outputIndex = 0
                do {
                    mMediaCodec?.let { codec ->
                        outputIndex = codec.dequeueOutputBuffer(bufferInfo, TIMES_OUT_US)
                        when (outputIndex) {
                            MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                                if (Utils.debugCamera) {
                                    Logger.i(TAG, "addTracker is video = $isVideo")
                                }
                                mMp4Muxer?.addTracker(mMediaCodec?.outputFormat, isVideo)
                            }
                            else -> {
                                if (outputIndex < 0) {
                                    return@let
                                }
                                val outputBuffer = if (isLowerLollipop()) {
                                    codec.outputBuffers[outputIndex]
                                } else {
                                    codec.getOutputBuffer(outputIndex)
                                }
                                if (outputBuffer != null) {
                                    val encodeData = ByteArray(bufferInfo.size)
                                    outputBuffer.get(encodeData)
                                    val type = if (isVideo) {
                                        IEncodeDataCallBack.DataType.H264
                                    } else {
                                        IEncodeDataCallBack.DataType.AAC
                                    }
                                    mEncodeDataCb?.onEncodeData(encodeData, encodeData.size, type)
                                    mMp4Muxer?.pumpStream(outputBuffer, bufferInfo, isVideo)
                                    logSpecialFrame(bufferInfo, encodeData.size)
                                }
                                codec.releaseOutputBuffer(outputIndex, false)
                            }
                        }
                    }
                } while (outputIndex >= 0)
            } catch (e: Exception) {
                Logger.e(TAG, "doEncodeData failed, video = ${isVideo}， err = ${e.localizedMessage}", e)
            }
        }
    }

    private fun queueFrameIfNeed() {
        mMediaCodec?.let { codec ->
            if (mRawDataQueue.isEmpty()) {
                return@let
            }
            val rawData = mRawDataQueue.poll() ?: return@let
            val inputIndex = codec.dequeueInputBuffer(TIMES_OUT_US)
            if (inputIndex < 0) {
                return@let
            }
            val inputBuffer = if (isLowerLollipop()) {
                codec.inputBuffers[inputIndex]
            } else {
                codec.getInputBuffer(inputIndex)
            }
            inputBuffer?.clear()
            inputBuffer?.put(rawData.data)
            codec.queueInputBuffer(inputIndex, 0, rawData.data.size, getPTSUs(rawData.data.size), 0)
        }
    }

    private fun logSpecialFrame(bufferInfo: MediaCodec.BufferInfo, length: Int) {
        if (length == 0 || !isVideo) {
            return
        }
        if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
            Logger.i(TAG, "Key frame, len = $length")
        } else if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
            Logger.i(TAG, "Pps/sps frame, len = $length")
        }
    }

    companion object {
        private const val TAG = "AbstractProcessor"
        private const val MSG_START = 1
        private const val MSG_STOP = 2
        private const val TIMES_OUT_US = 10000L

        const val MAX_QUEUE_SIZE = 10
    }

}