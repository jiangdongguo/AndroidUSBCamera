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
package com.jiangdg.ausbc.utils

import android.content.Context
import android.widget.Toast
import androidx.annotation.MainThread

/** Simple Toast
 *
 * @author Created by jiangdongguo on 2022/1/21
 */
object ToastUtils {

    private var applicationCtx: Context ?= null

    @MainThread
    fun init(ctx: Context) {
        if (applicationCtx != null) {
            return
        }
        this.applicationCtx = ctx.applicationContext
    }

    @JvmStatic
    fun show(msg: String) {
        applicationCtx?.let { ctx ->
            Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
        }
    }

    @JvmStatic
    fun show(resId: Int) {
        applicationCtx?.let { ctx ->
            Toast.makeText(ctx, ctx.getString(resId), Toast.LENGTH_LONG).show()
        }
    }

}