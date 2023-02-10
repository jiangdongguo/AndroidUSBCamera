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
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

/** Handle Crash information
 *
 *  Crash files : /storage/emulated/0/Android/data/packagename/files
 *  or /data/data/packagename/files
 *
 * @author Created by jiangdg on 2022/3/1
 */
object CrashUtils : Thread.UncaughtExceptionHandler {

    private var mDefaultHandler: Thread.UncaughtExceptionHandler? = null
    private var mApplication: Application? = null

    fun init(application: Application) {
        this.mApplication = application
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        if (!handleException(e) && mDefaultHandler != null) {
            mDefaultHandler!!.uncaughtException(t, e)
        } else {
            try {
                Thread.sleep((2 * 1000).toLong())
            } catch (e1: InterruptedException) {
                e1.printStackTrace()
            }
            restartAppIfNeed()
        }
    }

    private fun restartAppIfNeed(reboot: Boolean = false) {
        AppUtils.apply {
            removeAllActivity()
            if (reboot) {
                restartApp(mApplication?.applicationContext)
            }
            releaseAppResource()
        }
    }

    private fun handleException(ex: Throwable?): Boolean {
        if (ex == null) {
            return false
        }
        saveErrorInfo(ex).apply {
            Thread {
                Looper.prepare()
                ToastUtils.show("App crash, saved path ${this?.path}")
                Looper.loop()
            }.start()
            uploadErrorInfo(this)
        }
        return true
    }

    private fun uploadErrorInfo(saveErrorInfo: File?) {
        // upload to server
    }

    private fun saveErrorInfo(ex: Throwable): File? {
        var logFile: File? = null
        var fw: FileWriter? = null
        var printWriter: PrintWriter? = null
        try {
            mApplication?.apply {
                val time = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS").format(Date())
                val crashPath = "${getExternalFilesDir(null)?.path}${File.separator}AUSBC-crash-${time}.log"
                logFile = File(crashPath)
                logFile ?: return null
                fw = FileWriter(logFile, true)
                fw ?: return null
                printWriter = PrintWriter(fw!!)
                val pm: PackageManager = packageManager
                val pi: PackageInfo = pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
                printWriter?.let { pw ->
                    pw.println()
                    pw.println("Time：${SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(Date())}")
                    pw.println("VersionInfo：versionCode=${pi.versionCode} versionName：${pi.versionName}")
                    // 通过反射获取手机参数
                    pw.println("PhoneInfo  ：manufacture=${Build.MANUFACTURER.toString()} model=${Build.MODEL}")
                    pw.println("SystemInfo ：version=${Build.VERSION.RELEASE}")
                    // 打印堆栈信息
                    ex.printStackTrace(pw)
                    pw.println("End=====================================")
                    pw.flush()
                    fw?.flush()
                }
            }
        } catch (e1: IOException) {
            e1.printStackTrace()
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        } finally {
            try {
                printWriter?.close()
                fw?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return logFile
    }
}