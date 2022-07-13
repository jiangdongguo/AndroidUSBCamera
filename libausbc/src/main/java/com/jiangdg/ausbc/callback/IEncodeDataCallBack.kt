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
package com.jiangdg.ausbc.callback

/** Encode data callback
 *
 *  type = 0  video encode data -> h264
 *  type = 1  audio encode data, aac
 *
 * @author Created by jiangdg on 2022/1/29
 */
interface IEncodeDataCallBack {
    fun onEncodeData(data: ByteArray?, size: Int, type: DataType)

    enum class DataType {
        AAC, H264
    }
}