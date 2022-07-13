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
package com.jiangdg.ausbc.camera

import android.content.Context
import com.jiangdg.ausbc.camera.bean.PreviewSize

/** CameraX API
 *
 * @author Created by jiangdg on 2021/12/20
 */
class CameraxStrategy(ctx: Context): ICameraStrategy(ctx) {
    override fun getAllPreviewSizes(aspectRatio: Double?): MutableList<PreviewSize> {
        TODO("Not yet implemented")
    }

    override fun loadCameraInfo() {
        TODO("Not yet implemented")
    }

    override fun startPreviewInternal() {
        TODO("Not yet implemented")
    }

    override fun stopPreviewInternal() {
        TODO("Not yet implemented")
    }

    override fun captureImageInternal(savePath: String?) {
        TODO("Not yet implemented")
    }

    override fun switchCameraInternal(cameraId: String?) {
        TODO("Not yet implemented")
    }

    override fun updateResolutionInternal(width: Int, height: Int) {
        TODO("Not yet implemented")
    }

}