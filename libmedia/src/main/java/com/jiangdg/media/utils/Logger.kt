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
package com.jiangdg.media.utils

import android.app.Application
import android.util.Log
import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.LogLevel
import com.elvishew.xlog.XLog
import com.elvishew.xlog.flattener.PatternFlattener
import com.elvishew.xlog.printer.AndroidPrinter
import com.elvishew.xlog.printer.ConsolePrinter
import com.elvishew.xlog.printer.file.FilePrinter
import com.elvishew.xlog.printer.file.naming.FileNameGenerator
import java.text.SimpleDateFormat
import java.util.*

/** Logger utils
 *
 *  Default log files dir:  /storage/emulated/0/Android/data/packagename/files
 *  or /data/data/packagename/files
 *
 * @author Created by jiangdg on 2022/1/24
 */
object Logger {
    private var mHasInit: Boolean = false
    private const val TAG = "JJCamera"

    fun init(application: Application, folderPath: String? = null) {
        val androidPrinter = AndroidPrinter(true)
        val config = LogConfiguration.Builder().apply {
            logLevel(LogLevel.ALL)
            tag(TAG)
            enableThreadInfo()
            disableStackTrace()
        }.build()
        val filePrinter = folderPath.let {
            FilePrinter.Builder(
                folderPath ?: application.getExternalFilesDir(null)?.path
                ?: application.filesDir.path
            ).apply {
                fileNameGenerator(MyFileNameGenerator())
                flattener(MyFlatterer())
            }.build()
        }
        XLog.init(config,androidPrinter , filePrinter)
        mHasInit = true
    }

    fun i(flag: String, msg: String) {
        if (mHasInit) {
            XLog.i("++++++++Info->$flag###$msg")
            return
        }
        Log.i(TAG,"++++++++Info->$flag###$msg")
    }

    fun d(flag: String, msg: String) {
        if (mHasInit) {
            XLog.d("++++++++Info->$flag###$msg")
            return
        }
        Log.d(TAG,"++++++++Info->$flag###$msg")
    }

    fun w(flag: String, msg: String) {
        if (mHasInit) {
            XLog.w("++++++++Info->$flag###$msg")
            return
        }
        Log.w(TAG,"++++++++Info->$flag###$msg")
    }

    fun e(flag: String, msg: String, throwable: Throwable? = null) {
        if (mHasInit) {
            XLog.e("++++++++Info->$flag###$msg")
            return
        }
        Log.e(TAG,"++++++++Info->$flag###$msg")
    }

    class MyFileNameGenerator : FileNameGenerator {
        private val mLocalDateFormat: ThreadLocal<SimpleDateFormat?> =
            object : ThreadLocal<SimpleDateFormat?>() {
                override fun initialValue(): SimpleDateFormat {
                    return SimpleDateFormat("yyyy-MM-dd", Locale.US)
                }
            }

        override fun isFileNameChangeable(): Boolean = true

        override fun generateFileName(logLevel: Int, timestamp: Long): String {
            val dateStr = mLocalDateFormat.get()!!.let { sdf ->
                sdf.timeZone = TimeZone.getDefault()
                sdf.format(Date(timestamp))
            }
            return "JJCamera-$dateStr.log"
        }
    }

    class MyFlatterer : PatternFlattener(FLATTERER) {
        companion object {
            private const val FLATTERER = "{d yyyy-MM-dd HH:mm:ss.SSS} {l}/{t}: {m}"
        }
    }
}