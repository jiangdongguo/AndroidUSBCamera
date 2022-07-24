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

import androidx.annotation.Keep


/** Camera request parameters
 *
 * @author Created by jiangdg on 2021/12/20
 */
@Keep
class CameraRequest private constructor() {
    var previewWidth: Int = DEFAULT_WIDTH
    var previewHeight: Int = DEFAULT_HEIGHT
    var cameraId: String = ""
    var isFrontCamera: Boolean = false
    var isContinuousAFModel: Boolean = true
        private set
    var isContinuousAEModel: Boolean = true
        private set

    override fun toString(): String {
        return "CameraRequest(previewWidth=$previewWidth, previewHeight=$previewHeight, " +
                "cameraId='$cameraId', isFrontCamera=$isFrontCamera, " +
                "isContinuousAFModel=$isContinuousAFModel, isContinuousAEModel=$isContinuousAEModel)"
    }

    /**
     * Camera request builder
     *
     * @constructor Create empty Camera request builder
     */
    class Builder {
        private val mRequest by lazy {
            CameraRequest()
        }

        /**
         * Set preview width
         *
         * @param width camera preview width
         * @return see [Builder]
         */
        fun setPreviewWidth(width: Int): Builder {
            mRequest.previewWidth = width
            return this
        }

        /**
         * Set preview height
         *
         * @param height camera preview height
         * @return [Builder]
         */
        fun setPreviewHeight(height: Int): Builder {
            mRequest.previewHeight = height
            return this
        }

        /**
         * Set camera id
         *
         * @param cameraId camera id
         * @return [Builder]
         */
        fun setCameraId(cameraId: String): Builder {
            mRequest.cameraId = cameraId
            return this
        }

        /**
         * Set front camera
         *
         * @param isFrontCamera front camera flag
         * @return [Builder]
         */
        fun setFrontCamera(isFrontCamera: Boolean): Builder {
            mRequest.isFrontCamera = isFrontCamera
            return this
        }

        /**
         * Set continuous a f model
         *
         * @param isContinuousAF
         * @return [Builder]
         */
        fun setContinuousAFModel(isContinuousAF: Boolean): Builder {
            mRequest.isContinuousAFModel = isContinuousAF
            return this
        }

        /**
         * Set continuous auto model
         *
         * @param isContinuousAuto
         * @return [Builder]
         */
        fun setContinuousAutoModel(isContinuousAuto: Boolean): Builder {
            mRequest.isContinuousAEModel = isContinuousAuto
            return this
        }

        /**
         * Create a CameraRequest
         *
         * @return [CameraRequest]
         */
        fun create(): CameraRequest {
            return mRequest
        }
    }

    companion object {
        private const val DEFAULT_WIDTH = 640
        private const val DEFAULT_HEIGHT = 480
    }
}