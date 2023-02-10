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
package com.jiangdg.ausbc.render.effect

import android.content.Context
import com.jiangdg.ausbc.R
import com.jiangdg.ausbc.render.effect.bean.CameraEffect

/** Black White effect
 *
 * @author Created by jiangdg on 2022/1/26
 */
class EffectBlackWhite(ctx: Context) : AbstractEffect(ctx) {

    override fun getId(): Int = ID

    override fun getClassifyId(): Int = CameraEffect.CLASSIFY_ID_FILTER

    override fun getVertexSourceId(): Int = R.raw.base_vertex

    override fun getFragmentSourceId(): Int = R.raw.effect_blackw_fragment

    companion object {
        const val ID = 100
    }
}