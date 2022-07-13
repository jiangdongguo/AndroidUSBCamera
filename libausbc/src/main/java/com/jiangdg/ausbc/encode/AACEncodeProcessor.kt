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

import android.media.*
import android.os.Process
import com.jiangdg.ausbc.callback.ICaptureCallBack
import com.jiangdg.ausbc.callback.IPlayCallBack
import com.jiangdg.ausbc.encode.bean.RawData
import com.jiangdg.ausbc.utils.Logger
import com.jiangdg.ausbc.utils.MediaUtils
import com.jiangdg.ausbc.utils.Utils
import com.jiangdg.natives.LameMp3
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.Exception

/** AAC encode by MediaCodec
 *
 * @author Created by jiangdg on 2022/2/10
 */
class AACEncodeProcessor : AbstractProcessor() {
    private var mAudioTrack: AudioTrack? = null
    private var mAudioRecord: AudioRecord? = null
    private var mPresentationTimeUs: Long = 0L
    private val mPlayQueue: ConcurrentLinkedQueue<RawData> by lazy {
        ConcurrentLinkedQueue()
    }
    private val mRecordMp3Queue: ConcurrentLinkedQueue<RawData> by lazy {
        ConcurrentLinkedQueue()
    }
    private val mAudioThreadPool: ExecutorService by lazy {
        Executors.newFixedThreadPool(3)
    }
    private val mAudioRecordState: AtomicBoolean by lazy {
        AtomicBoolean(false)
    }
    private val mAudioPlayState: AtomicBoolean by lazy {
        AtomicBoolean(false)
    }
    private val mRecordMp3State: AtomicBoolean by lazy {
        AtomicBoolean(false)
    }
    private val mBufferSize: Int by lazy {
        AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_IN_CONFIG, AUDIO_FORMAT_16BIT)
    }

    override fun getThreadName(): String = TAG

    override fun handleStartEncode() {
        initAudioRecord()
        try {
            MediaFormat().apply {
                setString(MediaFormat.KEY_MIME, MIME_TYPE)
                setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
                setInteger(MediaFormat.KEY_CHANNEL_COUNT, CHANNEL_COUNT)
                setInteger(MediaFormat.KEY_SAMPLE_RATE, SAMPLE_RATE)
                setInteger(MediaFormat.KEY_AAC_PROFILE, CODEC_AAC_PROFILE)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, MAX_INPUT_SIZE)
            }.also { format ->
                mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE)
                mMediaCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                mMediaCodec?.start()
                mEncodeState.set(true)
                if (Utils.debugCamera) {
                    Logger.i(TAG, "init aac media codec success.")
                }
            }
            doEncodeData()
        } catch (e: Exception) {
            Logger.e(TAG, "init aac media codec failed, err = ${e.localizedMessage}", e)
        }
    }

    override fun handleStopEncode() {
        try {
            mEncodeState.set(false)
            mMediaCodec?.stop()
            mMediaCodec?.release()
            if (Utils.debugCamera) {
                Logger.i(TAG, "release aac media codec success.")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "release aac media codec failed, err = ${e.localizedMessage}", e)
        } finally {
            releaseAudioRecord()
            mRawDataQueue.clear()
            mMediaCodec = null
        }
    }

    override fun getPTSUs(bufferSize: Int): Long {
        //A frame of audio frame size int size = sampling rate * bit width * sampling time * number of channels
        // 1s timestamp calculation formula presentationTimeUs = 1000000L * (totalBytes / sampleRate/ audioFormat / channelCount / 8 )
        //totalBytes : total size of incoming encoder
        //1000 000L : The unit is microseconds, after conversion = 1s,
        //Divided by 8: The original unit of pcm is bit, 1 byte = 8 bit, 1 short = 16 bit, and it needs to be converted if it is carried by Byte[] and Short[]
        mPresentationTimeUs += (1.0 * bufferSize / (SAMPLE_RATE * CHANNEL_COUNT * (AUDIO_FORMAT_BITS / 8)) * 1000000.0).toLong()
        return mPresentationTimeUs
    }

    /**
     * Play audio start
     *
     * @param callBack play status call back, see [IPlayCallBack]
     */
    fun playAudioStart(callBack: IPlayCallBack?) {
        mAudioThreadPool.submit {
            try {
                initAudioRecord()
                initAudioTrack()
                mMainHandler.post {
                    callBack?.onBegin()
                }
                if (Utils.debugCamera) {
                    Logger.i(TAG, "start play mic success.")
                }
                while (mAudioPlayState.get()) {
                    val state = mAudioTrack?.state
                    if (state != AudioTrack.STATE_INITIALIZED) {
                        break
                    }
                    mPlayQueue.poll()?.apply {
                        mAudioTrack?.play()
                        mAudioTrack?.write(data, 0, size)
                    }
                }
                releaseAudioTrack()
                releaseAudioRecord()
                mMainHandler.post {
                    callBack?.onComplete()
                }
                if (Utils.debugCamera) {
                    Logger.i(TAG, "stop play mic success.")
                }
            } catch (e: Exception) {
                mMainHandler.post {
                    callBack?.onError(e.localizedMessage?: "unknown exception")
                }
                Logger.e(TAG, "start/stop play mic failed, err = ${e.localizedMessage}", e)
            }
        }
    }

    /**
     * Play audio stop
     */
    fun playAudioStop() {
        mAudioPlayState.set(false)
    }

    /**
     * Record mp3start
     *
     * @param audioPath custom mp4 record saving path, default is [/data/data/packagename/files]
     * @param callBack record status, see [ICaptureCallBack]
     */
    fun recordMp3Start(audioPath: String?, callBack: ICaptureCallBack) {
        mAudioThreadPool.submit {
            var fos: FileOutputStream? = null
            try {
                if (audioPath.isNullOrEmpty()) {
                    mMainHandler.post {
                        callBack.onError("save path($audioPath) invalid")
                    }
                    return@submit
                }
                initAudioRecord()
                val file = File(audioPath)
                if (file.exists()) {
                    file.delete()
                }
                fos = FileOutputStream(file)
                val mp3Buf = ByteArray(1024)
                LameMp3.lameInit(SAMPLE_RATE, CHANNEL_COUNT, SAMPLE_RATE, BIT_RATE, DEGREE_RECORD_MP3)
                mMainHandler.post {
                    callBack.onBegin()
                }
                if (Utils.debugCamera) {
                    Logger.i(TAG, "start record mp3 success, path = $audioPath")
                }
                mRecordMp3State.set(true)
                while (mRecordMp3State.get()) {
                    mRecordMp3Queue.poll()?.apply {
                        val tmpData = MediaUtils.transferByte2Short(data, size)
                        val encodeSize = LameMp3.lameEncode(tmpData, null, tmpData.size, mp3Buf)
                        if (encodeSize > 0) {
                            fos?.write(mp3Buf, 0, encodeSize)
                        }
                    }
                }
                val flushSize = LameMp3.lameFlush(mp3Buf)
                if (flushSize > 0) {
                    fos.write(mp3Buf, 0, flushSize)
                }
            } catch (e: Exception) {
                mMainHandler.post {
                    callBack.onError(e.localizedMessage?: "unknown exception")
                }
                Logger.e(TAG, "start/stop record mp3 failed, err = ${e.localizedMessage}", e)
            } finally {
                try {
                    fos?.close()
                    fos = null
                    LameMp3.lameClose()
                    releaseAudioRecord()
                    mMainHandler.post {
                        callBack.onComplete(audioPath)
                    }
                    if (Utils.debugCamera) {
                        Logger.i(TAG, "stop record mp3 success.")
                    }
                } catch (e: Exception) {
                    mMainHandler.post {
                        callBack.onError(e.localizedMessage?: "unknown exception")
                    }
                    Logger.e(TAG, "stop record mp3 failed, err = ${e.localizedMessage}", e)
                }
            }
        }
    }

    /**
     * Record mp3stop
     */
    fun recordMp3Stop() {
        mRecordMp3State.set(false)
    }

    private fun initAudioRecord() {
        if (mAudioRecordState.get()) return
        mAudioThreadPool.submit {
            initAudioRecordInternal()
            while (mAudioRecordState.get()) {
                val recordingState = mAudioRecord?.recordingState
                if (recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                    Logger.e(TAG, "initAudioRecord failed, state = $recordingState")
                    break
                }
                val data = ByteArray(mBufferSize)
                val readBytes = mAudioRecord?.read(data, 0, mBufferSize) ?: 0
                if (readBytes <= 0) {
                    continue
                }
                // pcm encode queue
                if (mRawDataQueue.size >= MAX_QUEUE_SIZE) {
                    mRawDataQueue.poll()
                }
                mRawDataQueue.offer(RawData(data, readBytes))
                // pcm play queue
                if (mPlayQueue.size >= MAX_QUEUE_SIZE) {
                    mPlayQueue.poll()
                }
                mPlayQueue.offer(RawData(data, readBytes))
                // pcm to mp3 queue
                if (mRecordMp3Queue.size >= MAX_QUEUE_SIZE) {
                    mRecordMp3Queue.poll()
                }
                mRecordMp3Queue.offer(RawData(data, readBytes))
            }
            releaseAudioRecordInternal()
        }
    }

    private fun releaseAudioRecord() {
        if (mEncodeState.get() || mAudioPlayState.get() || mRecordMp3State.get()) {
            return
        }
        mAudioRecordState.set(false)
    }

    private fun initAudioRecordInternal() {
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            mAudioRecord = AudioRecord(
                AUDIO_RECORD_SOURCE, SAMPLE_RATE,
                CHANNEL_IN_CONFIG, AUDIO_FORMAT_16BIT, mBufferSize
            )
            mAudioRecord?.startRecording()
            mAudioRecordState.set(true)
            if (Utils.debugCamera) {
                Logger.i(TAG, "initAudioRecordInternal success")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "initAudioRecordInternal failed, err = ${e.localizedMessage}", e)
        }
    }

    private fun releaseAudioRecordInternal() {
        try {
            mAudioRecord?.stop()
            mAudioRecord?.release()
            mAudioRecord = null
            mRawDataQueue.clear()
            if (Utils.debugCamera) {
                Logger.i(TAG, "releaseAudioRecordInternal success.")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "releaseAudioRecordInternal failed, err = ${e.localizedMessage}", e)
        }
    }

    private fun initAudioTrack() {
        if (mAudioPlayState.get()) {
            Logger.w(TAG, "initAudioTracker has ready execute!")
            return
        }
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_OUT_CONFIG,
            AUDIO_FORMAT_16BIT
        )
        mAudioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            SAMPLE_RATE,
            CHANNEL_OUT_CONFIG,
            AUDIO_FORMAT_16BIT,
            minBufferSize,
            AUDIO_TRACK_MODE
        )
        mAudioPlayState.set(true)
    }

    private fun releaseAudioTrack() {
        try {
            mAudioTrack?.release()
            mAudioTrack = null
        } catch (e: Exception) {
            Logger.e(TAG, "releaseAudioTracker failed, err = ${e.localizedMessage}", e)
        }
    }

    companion object {
        private const val TAG = "AACEncodeProcessor"
        private const val MIME_TYPE = "audio/mp4a-latm"
        private const val SAMPLE_RATE = 8000
        private const val BIT_RATE = 16000
        private const val MAX_INPUT_SIZE = 8192
        private const val CHANNEL_COUNT = 1
        private const val CHANNEL_IN_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val CHANNEL_OUT_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        private const val AUDIO_FORMAT_16BIT = AudioFormat.ENCODING_PCM_16BIT
        private const val AUDIO_TRACK_MODE = AudioTrack.MODE_STREAM
        private const val AUDIO_RECORD_SOURCE = MediaRecorder.AudioSource.MIC
        private const val CODEC_AAC_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectLC
        private const val AUDIO_FORMAT_BITS = 16
        private const val DEGREE_RECORD_MP3 = 7
    }
}