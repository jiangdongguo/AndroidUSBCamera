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

import android.app.Activity
import java.util.*

/** Activity stack manager
 *
 * @author Created by jiangdg on 2022/3/1
 */
object ActivityStackUtils {
    private const val TAG = "ActivityStackUtils"
    private val mStack: Stack<Activity> = Stack()

    fun pushActivity(activity: Activity) {
        mStack.push(activity)
        Logger.d(TAG, "push stack: ${activity.localClassName}")
    }

    fun popActivity() {
        if (!mStack.empty()) {
            val activity: Activity = mStack.pop()
            activity.finish()
            Logger.d(TAG, "pop stack: ${activity.localClassName}")
        }
    }

    fun removeActivity(activity: Activity) {
        if (!mStack.empty()) {
            mStack.remove(activity)
            Logger.d(TAG, "remove stack: ${activity.localClassName}")
        }
    }

    fun getStackTop(): Activity? {
        var activity: Activity? = null
        if (!mStack.empty()) {
            activity = mStack.peek()
            Logger.d(TAG, "stack top: ${activity.localClassName}")
        }
        return activity
    }

    fun popAllActivity() {
        if (!mStack.empty()) {
            val size: Int = mStack.size
            for (i in 0 until size) {
                popActivity()
            }
        }
    }

    fun hasActivity() = !mStack.isEmpty()
}