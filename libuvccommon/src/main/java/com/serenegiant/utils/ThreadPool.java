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

import androidx.annotation.NonNull;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPool {

    // for thread pool
    private static final int CORE_POOL_SIZE = 4;		// initial/minimum threads
    private static final int MAX_POOL_SIZE = 32;		// maximum threads
    private static final int KEEP_ALIVE_TIME = 10;		// time periods while keep the idle thread
    private static final ThreadPoolExecutor EXECUTOR
		= new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME,
			TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

	static {
		EXECUTOR.allowCoreThreadTimeOut(true);	// this makes core threads can terminate
	}

	public static void preStartAllCoreThreads() {
		// in many case, calling createBitmapCache method means start the new query
		// and need to prepare to run asynchronous tasks
		EXECUTOR.prestartAllCoreThreads();
	}

	public static void queueEvent(@NonNull final Runnable command) {
		EXECUTOR.execute(command);
	}
	
	public static boolean removeEvent(@NonNull final Runnable command) {
		return EXECUTOR.remove(command);
	}
}
