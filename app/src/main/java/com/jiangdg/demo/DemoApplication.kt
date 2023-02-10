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
package com.jiangdg.demo

import android.content.Context
import androidx.multidex.MultiDex
import com.jiangdg.ausbc.base.BaseApplication
import com.jiangdg.utils.MMKVUtils
import com.tencent.bugly.crashreport.CrashReport

/**
 *
 * @author Created by jiangdg on 2022/2/28
 */
class DemoApplication: BaseApplication() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        // init bugly library
        CrashReport.initCrashReport(this, "9baa0e3fac", true)
        MMKVUtils.init(this)
    }
}