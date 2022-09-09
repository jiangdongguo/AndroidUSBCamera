package com.jiangdg.utils;
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
import android.os.HandlerThread;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class HandlerThreadHandler extends Handler {
	private static final String TAG = "HandlerThreadHandler";

	public static final HandlerThreadHandler createHandler() {
		return createHandler(TAG);
	}

	public static final HandlerThreadHandler createHandler(final String name) {
		final HandlerThread thread = new HandlerThread(name);
		thread.start();
		return new HandlerThreadHandler(thread.getLooper());
	}

	public static final HandlerThreadHandler createHandler(@Nullable final Callback callback) {
		return createHandler(TAG, callback);
	}

	public static final HandlerThreadHandler createHandler(final String name, @Nullable final Callback callback) {
		final HandlerThread thread = new HandlerThread(name);
		thread.start();
		return new HandlerThreadHandler(thread.getLooper(), callback);
	}

	private HandlerThreadHandler(@NonNull final Looper looper) {
		super(looper);
	}

	private HandlerThreadHandler(@NonNull final Looper looper, @Nullable final Callback callback) {
		super(looper, callback);
	}

}
