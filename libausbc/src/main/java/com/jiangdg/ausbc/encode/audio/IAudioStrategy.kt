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

import com.jiangdg.ausbc.encode.bean.RawData

/** Audio(pcm) collection context
 *
 * @author Created by jiangdg on 2022/9/14
 */
interface IAudioStrategy {
    fun initAudioRecord()
    fun startRecording()
    fun stopRecording()
    fun releaseAudioRecord()
    fun read(): RawData?
    fun isRecording(): Boolean
    fun getSampleRate(): Int
    fun getAudioFormat(): Int
    fun getChannelCount(): Int
    fun getChannelConfig(): Int
}