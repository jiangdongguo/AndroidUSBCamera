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
package com.jiangdg.ausbc.encode

import android.media.MediaCodec
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.jiangdg.ausbc.callback.IEncodeDataCallBack
import com.jiangdg.ausbc.encode.bean.RawData
import com.jiangdg.ausbc.encode.muxer.Mp4Muxer
import com.jiangdg.ausbc.utils.Logger
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.Exception


/** Abstract processor
 *
 * @author Created by jiangdg on 2022/2/10
 */
abstract class AbstractProcessor(private val isVideo: Boolean) {
    private var mEncodeThread: HandlerThread? = null
    private var mEncodeHandler: Handler? = null
    private var mStartTimeStamps: Long = 0L
    protected var mMediaCodec: MediaCodec? = null
    private var mMp4Muxer: Mp4Muxer? = null
    private var mEncodeDataCb: IEncodeDataCallBack? = null
    protected val mRawDataQueue: ConcurrentLinkedQueue<RawData> = ConcurrentLinkedQueue()
    protected var mBitRate: Int? = null
    private var isExit = true
    protected val mMainHandler: Handler by lazy {
        Handler(Looper.getMainLooper())
    }

    protected val mEncodeState: AtomicBoolean by lazy {
        AtomicBoolean(false)
    }

    private val mBufferInfo by lazy {
        MediaCodec.BufferInfo()
    }

    /**
     * Start encode
     *
     * [getThreadName] diff audio thread or  video thread
     */
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
        isExit = false
    }

    /**
     * Stop encode
     */
    fun stopEncode() {
        isExit = true
        mEncodeHandler?.obtainMessage(MSG_STOP)?.sendToTarget()
        mEncodeThread?.quitSafely()
        mEncodeThread = null
        mEncodeHandler = null
    }

    /**
     * Update bit rate for encode audio or video
     *
     * @param bitRate bps
     */
    fun updateBitRate(bitRate: Int) {
        this.mBitRate = bitRate
        mEncodeHandler?.obtainMessage(MSG_STOP)?.sendToTarget()
        mEncodeHandler?.obtainMessage(MSG_START)?.sendToTarget()
    }

    /**
     * se encode data call back
     *
     * @param callBack aac or h264 data call back, see [IEncodeDataCallBack]
     */
    fun setEncodeDataCallBack(callBack: IEncodeDataCallBack?) {
        this.mEncodeDataCb = callBack
    }

    /**
     * Set mp4muxer
     *
     * @param muxer mp4 media muxer
     */
    @Synchronized
    fun setMp4Muxer(muxer: Mp4Muxer) {
        this.mMp4Muxer = muxer
        // Set muxer if record midway when encoding
        // if not encoding, cancel it
        if (! isEncoding()) {
            return
        }
        try {
            mMp4Muxer?.addTracker(mMediaCodec?.outputFormat, isVideo)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Put raw data
     *
     * @param data media data, pcm or yuv
     */
    fun putRawData(data: RawData) {
        if (! mEncodeState.get()) {
            return
        }
        if (mRawDataQueue.size >= MAX_QUEUE_SIZE) {
            mRawDataQueue.poll()
        }
        mRawDataQueue.offer(data)
    }

    /**
     * Is encoding
     */
    fun isEncoding() = mEncodeState.get() && !isExit

    /**
     * Get thread name
     *
     * @return Get encode thread name
     */
    protected abstract fun getThreadName(): String

    /**
     * Handle start encode
     */
    protected abstract fun handleStartEncode()

    /**
     * Handle stop encode
     */
    protected abstract fun handleStopEncode()

    /**
     * Get presentation time
     *
     * @param bufferSize buffer size
     * @return presentation time in us
     */
    protected abstract fun getPTSUs(bufferSize: Int): Long

    /**
     * Is lower lollipop
     */
    private fun isLowerLollipop() = Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP

    /**
     * Do encode data
     */
    protected fun doEncodeData() {
        while (isEncoding()) {
            try {
                queueFrameIfNeed()
                var outputIndex = 0
                do {
                    mMediaCodec?.let { codec ->
                        outputIndex = codec.dequeueOutputBuffer(mBufferInfo, TIMES_OUT_US)
                        when (outputIndex) {
                            MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                                Logger.i(TAG, "addTracker is video = $isVideo")
                                mMp4Muxer?.addTracker(mMediaCodec?.outputFormat, isVideo)
                            }
                            else -> {
                                if (outputIndex < 0) {
                                    return@let
                                }
                                if (mStartTimeStamps == 0L) {
                                    mStartTimeStamps = mBufferInfo.presentationTimeUs / 1000L
                                }
                                try {
                                    val outputBuffer = if (isLowerLollipop()) {
                                        codec.outputBuffers[outputIndex]
                                    } else {
                                        codec.getOutputBuffer(outputIndex)
                                    }
                                    outputBuffer ?: return@let
                                    processOutputData(outputBuffer, mBufferInfo)?.apply {
                                        mEncodeDataCb?.onEncodeData(
                                            first,
                                            outputBuffer,
                                            mBufferInfo.offset,
                                            mBufferInfo.size,
                                            mBufferInfo.presentationTimeUs / 1000L - mStartTimeStamps
                                        )
                                    }
                                    // muxer data
                                    mMp4Muxer?.pumpStream(outputBuffer, mBufferInfo, isVideo)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    codec.releaseOutputBuffer(outputIndex, false)
                                }
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
            val data: ByteArray = rawData.data
            if (processInputData(data) == null) {
                return@let
            }
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
            inputBuffer?.put(data)
            codec.queueInputBuffer(inputIndex, 0, data.size, getPTSUs(data.size), 0)
        }
    }

    protected abstract fun processOutputData(
        encodeData: ByteBuffer,
        bufferInfo: MediaCodec.BufferInfo
    ): Pair<IEncodeDataCallBack.DataType, ByteBuffer>?

    protected abstract fun processInputData(data: ByteArray): ByteArray?

    companion object {
        private const val TAG = "AbstractProcessor"
        private const val MSG_START = 1
        private const val MSG_STOP = 2
        private const val TIMES_OUT_US = 10000L

        const val MAX_QUEUE_SIZE = 5
    }

}