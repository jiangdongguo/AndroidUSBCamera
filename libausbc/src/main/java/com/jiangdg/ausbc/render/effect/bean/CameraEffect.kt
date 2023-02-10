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
package com.jiangdg.ausbc.render.effect.bean

import androidx.annotation.Keep
import com.jiangdg.ausbc.R
import com.jiangdg.ausbc.render.effect.AbstractEffect

/** camera filter info
 *
 * @author Created by jiangdg on 2022/3/16
 */
@Keep
data class CameraEffect(
    val id: Int,
    val name: String,
    val classifyId: Int,
    val effect: AbstractEffect? = null,
    val coverResId: Int? = null,
    val coverUrl: String? = null
) {
    companion object {
        val NONE_FILTER by lazy {
            CameraEffect(
                ID_NONE_FILTER,
                NAME_NONE,
                CLASSIFY_ID_FILTER,
                coverResId = R.drawable.effect_none
            )
        }

        val NONE_ANIMATION by lazy {
            CameraEffect(
                ID_NONE_ANIMATION,
                NAME_NONE,
                CLASSIFY_ID_ANIMATION,
                coverResId = R.drawable.effect_none
            )
        }

        const val CLASSIFY_ID_FILTER = 1
        const val CLASSIFY_ID_ANIMATION = 2
        const val ID_NONE_FILTER = -1
        const val ID_NONE_ANIMATION = -2
        private const val NAME_NONE = "None"
    }
}