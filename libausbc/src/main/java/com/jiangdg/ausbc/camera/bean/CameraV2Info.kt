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
package com.jiangdg.ausbc.camera.bean

import android.hardware.camera2.CameraCharacteristics

/**
 *
 * @author Created by jiangdg on 2022/1/27
 */
@kotlin.Deprecated("Deprecated since version 3.3.0")
data class CameraV2Info(override val cameraId: String) : CameraInfo(cameraId) {
    var cameraType: Int = 0
    var cameraCharacteristics: CameraCharacteristics? = null

    override fun toString(): String {
        return "CameraV2Info(cameraId='$cameraId', " +
                "cameraType=$cameraType, " +
                "cameraCharacteristics=$cameraCharacteristics)"
    }
}
