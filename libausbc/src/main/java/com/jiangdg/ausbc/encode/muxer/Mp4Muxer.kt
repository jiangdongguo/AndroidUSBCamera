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
package com.jiangdg.ausbc.encode.muxer

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Environment
import android.os.Handler
import android.os.Looper
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.callback.ICaptureCallBack
import com.jiangdg.ausbc.utils.Logger
import java.lang.Exception
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*

/**
 * MediaMuxer for Mp4
 *
 * @property path mp4 saving path
 * @property durationInSec mp4 file auto divided in seconds
 *
 * @constructor
 * @param onlyVideo save mp4 without audio
 * @param callBack mp4 capture status, see [ICaptureCallBack]
 *
 * @author Created by jiangdg on 2022/2/10
 */
class Mp4Muxer(
    callBack: ICaptureCallBack,
    private var path: String? = null,
    private val durationInSec: Long = 0,
    private val isVideoOnly: Boolean = false
) {
    private var mMediaMuxer: MediaMuxer? = null
    private var mFileSubIndex: Int = 0
    private var mVideoTrackerIndex = -1
    private var mAudioTrackerIndex = -1
    private var mVideoFormat: MediaFormat? = null
    private var mAudioFormat: MediaFormat? = null
    private var mBeginMillis: Long = 0
    private var mCaptureCallBack: ICaptureCallBack? = null
    private var mMainHandler: Handler = Handler(Looper.getMainLooper())
    private var mOriginalPath: String? = null
    private var mVideoPts: Long = 0L
    private var mAudioPts: Long = 0L
    private val mDateFormat by lazy {
        SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.getDefault())
    }
    private val mCameraDir by lazy {
        "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)}/Camera"
    }

    init {
        this.mCaptureCallBack = callBack
        try {
            if (path.isNullOrEmpty()) {
                val date = mDateFormat.format(System.currentTimeMillis())
                path = "$mCameraDir/VID_JJCamera_$date"
            }
            mOriginalPath = path
            path = "${path}.mp4"
            mMediaMuxer = MediaMuxer(path!!, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } catch (e: Exception) {
            mCaptureCallBack?.onError(e.localizedMessage)
            Logger.e(TAG, "init media muxer failed, err = ${e.localizedMessage}", e)
        }
    }

    /**
     * Add tracker
     *
     * @param mediaFormat media format, see [MediaFormat]
     * @param isVideo media type, audio or video
     */
    @Synchronized
    fun addTracker(mediaFormat: MediaFormat?, isVideo: Boolean) {
        if (isMuxerStarter() || mediaFormat == null) {
            return
        }
        try {
            mMediaMuxer?.apply {
                val tracker = addTrack(mediaFormat)
                if (isVideo) {
                    mVideoFormat = mediaFormat
                    mVideoTrackerIndex = tracker
                    if (mAudioTrackerIndex != -1 || isVideoOnly) {
                        start()
                        mMainHandler.post {
                            mCaptureCallBack?.onBegin()
                        }
                        mBeginMillis = System.currentTimeMillis()
                        Logger.i(TAG, "start media muxer")
                    }
                } else {
                    mAudioFormat = mediaFormat
                    mAudioTrackerIndex = tracker
                    if (mVideoTrackerIndex != -1) {
                        start()
                        mMainHandler.post {
                            mCaptureCallBack?.onBegin()
                        }
                        mBeginMillis = System.currentTimeMillis()
                        Logger.i(TAG, "start media muxer")
                    }
                }
                Logger.i(TAG, "addTracker index = $tracker isVideo = $isVideo")
            }
        } catch (e: Exception) {
            release()
            mMainHandler.post {
                mCaptureCallBack?.onError(e.localizedMessage)
            }
            Logger.e(TAG, "addTracker failed, err = ${e.localizedMessage}", e)
        }
    }

    /**
     * write audio(aac) or video(h264) data to media muxer
     *
     * @param outputBuffer encode output buffer, see [MediaCodec]
     * @param bufferInfo encode output buffer info, see [MediaCodec.BufferInfo]
     * @param isVideo media data type, audio or video
     */
    @Synchronized
    fun pumpStream(outputBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo, isVideo: Boolean) {
        try {
            if (!isMuxerStarter()) {
                return
            }
            if (bufferInfo.size <= 0) {
                return
            }
            val index = if (isVideo) {
                if (mVideoPts == 0L) {
                    mVideoPts = bufferInfo.presentationTimeUs
                }
                bufferInfo.presentationTimeUs = bufferInfo.presentationTimeUs - mVideoPts
                mVideoTrackerIndex
            } else {
                if (mAudioPts == 0L) {
                    mAudioPts = bufferInfo.presentationTimeUs
                }
                bufferInfo.presentationTimeUs = bufferInfo.presentationTimeUs - mAudioPts
                mAudioTrackerIndex
            }
            outputBuffer.position(bufferInfo.offset)
            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
            mMediaMuxer?.writeSampleData(index, outputBuffer, bufferInfo)
            saveNewFileIfNeed()
        } catch (e: Exception) {
            Logger.e(TAG, "pumpStream failed, err = ${e.localizedMessage}", e)
        }
    }

    private fun saveNewFileIfNeed() {
        try {
            val endMillis = System.currentTimeMillis()
            if (durationInSec == 0L) {
                return
            }
            if (endMillis - mBeginMillis <= durationInSec * 1000) {
                return
            }

            mMediaMuxer?.stop()
            mMediaMuxer?.release()
            mMediaMuxer = null
            mAudioTrackerIndex = -1
            mVideoTrackerIndex = -1
            mAudioPts = 0L
            mVideoPts = 0L

            path = "${mOriginalPath}_${++mFileSubIndex}.mp4"
            mMediaMuxer = MediaMuxer(path!!, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            addTracker(mVideoFormat, true)
            addTracker(mAudioFormat, false)
        } catch (e: Exception) {
            mMainHandler.post {
                mCaptureCallBack?.onError(e.localizedMessage)
            }
            Logger.e(TAG, "release media muxer failed, err = ${e.localizedMessage}", e)
        }
    }

    /**
     * Release mp4 muxer resource
     */
    @Synchronized
    fun release() {
        try {
            mMediaMuxer?.stop()
            mMediaMuxer?.release()
        } catch (e: Exception) {
            mMainHandler.post {
                mCaptureCallBack?.onError(e.localizedMessage)
            }
            Logger.e(TAG, "release media muxer failed, err = ${e.localizedMessage}", e)
        } finally {
            mMediaMuxer = null
            mAudioTrackerIndex = -1
            mVideoTrackerIndex = -1
            mAudioPts = 0L
            mVideoPts = 0L
        }
        mMainHandler.post {
            mCaptureCallBack?.onComplete(path)
        }
    }

    fun getOutputPath() = path

    fun isMuxerStarter() = mVideoTrackerIndex != -1 && (mAudioTrackerIndex != -1 || isVideoOnly)

    companion object {
        private const val TAG = "Mp4Muxer"
    }
}