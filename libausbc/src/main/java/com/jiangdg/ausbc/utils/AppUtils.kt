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

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Process

/** App operator utils
 *
 * @author Created by jiangdg on 2022/3/1
 */
object AppUtils {

    @SuppressLint("UnspecifiedImmutableFlag")
    fun restartApp(ctx: Context?) {
        ctx ?: return
        val pckgManager: PackageManager = ctx.applicationContext.packageManager
        val intent: Intent? = pckgManager.getLaunchIntentForPackage(ctx.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            ctx.applicationContext, 0, intent, PendingIntent.FLAG_ONE_SHOT
        )
        val manager: AlarmManager = ctx.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        manager.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, pendingIntent)
    }

    fun releaseAppResource() {
        Process.killProcess(Process.myPid())
        System.exit(0)
    }

    fun removeAllActivity() {
        ActivityStackUtils.popAllActivity()
    }

    fun getAppName(ctx: Context): String? {
        val packageManager: PackageManager = ctx.packageManager
        try {
            val packageInfo: PackageInfo = packageManager.getPackageInfo(ctx.packageName, 0)
            val labelRes: Int = packageInfo.applicationInfo.labelRes
            return ctx.getString(labelRes)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return null
    }
}