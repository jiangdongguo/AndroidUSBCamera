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
package com.jiangdg.natives

/** YUV format transform
 *
 * @author Created by jiangdg on 2022/2/18
 */
object YUVUtils {
    init {
        System.loadLibrary("nativelib")
    }

    external fun yuv420spToNv21(data: ByteArray, width: Int, height: Int)
    external fun nv21ToYuv420sp(data: ByteArray, width: Int, height: Int)
    external fun nv21ToYuv420spWithMirror(data: ByteArray, width: Int, height: Int)
    external fun nv21ToYuv420p(data: ByteArray, width: Int, height: Int)
    external fun nv21ToYuv420pWithMirror(data: ByteArray, width: Int, height: Int)
    external fun nativeRotateNV21(data: ByteArray, width: Int, height: Int, degree: Int)
}