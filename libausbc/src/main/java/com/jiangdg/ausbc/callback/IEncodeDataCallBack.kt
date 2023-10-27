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
package com.jiangdg.ausbc.callback

import java.nio.ByteBuffer

/** Encode data callback
 *
 * @author Created by jiangdg on 2022/1/29
 */
interface IEncodeDataCallBack {
    fun onEncodeData(type: DataType, buffer:ByteBuffer, offset: Int, size: Int, timestamp: Long)
    enum class DataType {
        AAC,       // aac without ADTS header,
                   // if want adding adts, should call MediaUtils.addADTStoPacket() method
        H264_KEY,  // H.264, key frame
        H264_SPS,  // H.264, sps & pps
        H264       // H.264 not key frame
    }
}