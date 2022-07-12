package com.serenegiant.utils;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2018 saki t_saki@serenegiant.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import android.util.Log;

public final class UIThreadHelper {
	private static final String TAG = UIThreadHelper.class.getSimpleName();

	/** UI操作用のHandler */
	private static final Handler sUIHandler = new Handler(Looper.getMainLooper());
	/** UIスレッドの参照 */
	private static final Thread sUiThread = sUIHandler.getLooper().getThread();

	/**
	 * UIスレッドでRunnableを実行するためのヘルパーメソッド
	 * @param task
	 */
	public static final void runOnUiThread(@NonNull final Runnable task) {
		if (Thread.currentThread() != sUiThread) {
			sUIHandler.post(task);
		} else {
			try {
				task.run();
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}
	}

	public static final void runOnUiThread(@NonNull final Runnable task, final long duration) {
		if ((duration > 0) || Thread.currentThread() != sUiThread) {
			sUIHandler.postDelayed(task, duration);
		} else {
			try {
				task.run();
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}
	}

	public static final void removeFromUiThread(@NonNull final Runnable task) {
		sUIHandler.removeCallbacks(task);
	}
}
