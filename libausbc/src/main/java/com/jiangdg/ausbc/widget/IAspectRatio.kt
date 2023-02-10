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
package com.jiangdg.ausbc.widget

import android.view.Surface

/** aspect ratio setting func interface
 *
 * @author Created by jiangdg on 2022/1/26
 */
interface IAspectRatio {
    fun setAspectRatio(width: Int, height: Int)
    fun getSurfaceWidth(): Int
    fun getSurfaceHeight(): Int
    fun getSurface(): Surface?
    fun postUITask(task: ()->Unit)
}