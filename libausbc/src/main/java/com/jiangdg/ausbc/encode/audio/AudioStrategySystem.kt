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

import android.media.*
import android.os.Process
import com.jiangdg.ausbc.encode.bean.RawData
import com.jiangdg.ausbc.utils.Logger
import com.jiangdg.ausbc.utils.Utils

/** System audio record
 *
 * @author Created by jiangdg on 2022/9/14
 */
class AudioStrategySystem : IAudioStrategy {
    private val mBufferSize: Int by lazy {
        AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_IN_CONFIG,
            AUDIO_FORMAT_16BIT
        )
    }
    private var mAudioRecord: AudioRecord? = null

    override fun initAudioRecord() {
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            mAudioRecord = AudioRecord(AUDIO_RECORD_SOURCE, SAMPLE_RATE,
                CHANNEL_IN_CONFIG, AUDIO_FORMAT_16BIT, mBufferSize
            )
            if (Utils.debugCamera) {
                Logger.i(TAG, "initAudioRecord success")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "initAudioRecord failed, err = ${e.localizedMessage}", e)
        }
    }

    override fun startRecording() {
        try {
            mAudioRecord?.startRecording()
            if (Utils.debugCamera) {
                Logger.i(TAG, "startRecording success-->${mAudioRecord?.recordingState}")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "startRecording failed, err = ${e.localizedMessage}", e)
        }
    }

    override fun stopRecording() {
        try {
            mAudioRecord?.stop()
            if (Utils.debugCamera) {
                Logger.i(TAG, "stopRecording success")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "startRecording failed, err = ${e.localizedMessage}", e)
        }
    }

    override fun releaseAudioRecord() {
        try {
            mAudioRecord?.release()
            mAudioRecord = null
            if (Utils.debugCamera) {
                Logger.i(TAG, "releaseAudioRecord success.")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "releaseAudioRecord failed, err = ${e.localizedMessage}", e)
        }
    }

    override fun read(): RawData? {
        return if (! isRecording()) {
            null
        } else {
            val data = ByteArray(mBufferSize)
            val readBytes = mAudioRecord?.read(data, 0, mBufferSize) ?: 0
            RawData(data, readBytes)
        }
    }

    override fun isRecording(): Boolean = mAudioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING

    override fun getSampleRate(): Int = SAMPLE_RATE

    override fun getAudioFormat(): Int = AUDIO_FORMAT_16BIT

    override fun getChannelCount(): Int = CHANNEL_COUNT

    override fun getChannelConfig(): Int = CHANNEL_IN_CONFIG

    companion object {
        private const val TAG = "AudioSystem"
        private const val SAMPLE_RATE = 8000
        private const val CHANNEL_COUNT = 1
        private const val CHANNEL_IN_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT_16BIT = AudioFormat.ENCODING_PCM_16BIT
        private const val AUDIO_RECORD_SOURCE = MediaRecorder.AudioSource.MIC
    }
}