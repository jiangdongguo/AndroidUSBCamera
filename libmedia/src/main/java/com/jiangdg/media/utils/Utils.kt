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

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.PowerManager
import androidx.core.content.ContextCompat

/** Common Utils
 *
 * @author Created by jiangdg on 2021/12/27
 */
object Utils  {

    internal var debugCamera = false

    fun getGpsLocation(context: Context?): Location? {
        context?.let { ctx->
            val locationManager = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val locPermission = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
            if (locPermission == PackageManager.PERMISSION_GRANTED) {
                return locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            }
        }
        return null
    }

    fun dp2px(context: Context, dpValue: Float): Int {
        val scale: Float = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    fun wakeLock(context: Context): PowerManager.WakeLock {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val mWakeLock: PowerManager.WakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "jj:camera")
        mWakeLock.setReferenceCounted(false)
        mWakeLock.acquire(10*60*1000L /*10 minutes*/)
        return mWakeLock
    }

    fun wakeUnLock(wakeLock: PowerManager.WakeLock?) {
        wakeLock?.release()
    }

    fun getGLESVersion(context: Context): String? {
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).apply {
            return deviceConfigurationInfo.glEsVersion
        }
    }
}