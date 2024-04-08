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

import android.app.Application
import timber.log.Timber

/** Logger utils
 *
 *  Default log files dir:  /storage/emulated/0/Android/data/packagename/files
 *  or /data/data/packagename/files
 *
 * @author Created by jiangdg on 2022/1/24
 */
object Logger {
    fun i(flag: String, msg: String) {
        Timber.tag(flag).i(msg)
    }

    fun d(flag: String, msg: String) {
        Timber.tag(flag).d(msg)
    }

    fun w(flag: String, msg: String) {
        Timber.tag(flag).w(msg)
    }

    fun w(flag: String, throwable: Throwable?) {
        Timber.tag(flag).w(throwable)
    }

    fun w(flag: String, msg: String, throwable: Throwable?) {
        Timber.tag(flag).w(throwable, msg)
    }

    fun e(flag: String, msg: String) {
        Timber.tag(flag).e(msg)
    }

    fun e(flag: String, msg: String, throwable: Throwable?) {
        Timber.tag(flag).e(throwable, msg)
    }
}