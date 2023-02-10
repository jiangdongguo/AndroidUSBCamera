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
package com.jiangdg.ausbc.encode.audio

import android.media.AudioFormat
import com.jiangdg.ausbc.encode.bean.RawData
import com.jiangdg.ausbc.utils.Logger
import com.jiangdg.ausbc.utils.Utils
import com.jiangdg.uac.UACAudioCallBack
import com.jiangdg.uac.UACAudioHandler
import com.jiangdg.usb.USBMonitor
import java.util.concurrent.ConcurrentLinkedQueue

/** UAC audio record
 *
 * @author Created by jiangdg on 2022/9/14
 */
class AudioStrategyUAC(private val ctrlBlock: USBMonitor.UsbControlBlock): IAudioStrategy {
    private var mUacHandler: UACAudioHandler? = null
    private val mPcmDataQueue: ConcurrentLinkedQueue<ByteArray> = ConcurrentLinkedQueue()

    private val mCallback = UACAudioCallBack { data ->
        if (mPcmDataQueue.size >= MAX_QUEUE_SIZE) {
            mPcmDataQueue.poll()
        }
        mPcmDataQueue.offer(data)
    }

    override fun initAudioRecord() {
        mUacHandler = UACAudioHandler.createHandler(ctrlBlock)
        mUacHandler?.initAudioRecord()
        if (Utils.debugCamera) {
            Logger.i(TAG, "initAudioRecord")
        }
    }

    override fun startRecording() {
        mUacHandler?.addDataCallBack(mCallback)
        mUacHandler?.startRecording()
        if (Utils.debugCamera) {
            Logger.i(TAG, "startRecording:")
        }
    }

    override fun stopRecording() {
        mUacHandler?.stopRecording()
        mUacHandler?.removeDataCallBack(mCallback)
        if (Utils.debugCamera) {
            Logger.i(TAG, "stopRecording:")
        }
    }

    override fun releaseAudioRecord() {
        mUacHandler?.releaseAudioRecord()
        if (Utils.debugCamera) {
            Logger.i(TAG, "releaseAudioRecord:")
        }
    }

    override fun read(): RawData? {
        return mPcmDataQueue.poll()?.let {
            RawData(it, it.size)
        }
    }

    override fun isRecording(): Boolean = mUacHandler?.isRecording == true

    override fun getSampleRate(): Int {
        return mUacHandler?.sampleRate ?: SAMPLE_RATE
    }

    override fun getAudioFormat(): Int {
        return if (mUacHandler?.bitResolution == BIT_RESOLUTION) {
            AudioFormat.ENCODING_PCM_8BIT
        } else {
            AudioFormat.ENCODING_PCM_16BIT
        }
    }

    override fun getChannelCount(): Int {
        return mUacHandler?.channelCount ?: CHANNEL_COUNT
    }

    override fun getChannelConfig(): Int = if (getChannelCount() == CHANNEL_COUNT) {
        AudioFormat.CHANNEL_IN_MONO
    } else {
        AudioFormat.CHANNEL_IN_STEREO
    }

    companion object {
        private const val TAG = "AudioUac"
        private const val MAX_QUEUE_SIZE = 10
        private const val SAMPLE_RATE = 8000
        private const val BIT_RESOLUTION = 8
        private const val CHANNEL_COUNT = 1
    }
}