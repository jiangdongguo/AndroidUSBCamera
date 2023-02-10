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
package com.jiangdg.ausbc.encode.bean

import androidx.annotation.Keep

/**
 * PCM or YUV raw data
 *
 * @property data media data, pcm or yuv
 * @property size media data size
 * @constructor Create empty Raw data
 *
 * @author Created by jiangdg on 2022/2/10
 */
@Keep
data class RawData(val data: ByteArray, val size: Int) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RawData

        if (!data.contentEquals(other.data)) return false
        if (size != other.size) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + size
        return result
    }
}
