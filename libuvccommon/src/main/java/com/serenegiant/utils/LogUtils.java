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

import android.util.Log;

public class LogUtils {

	public static final int DEBUG_LEVEL_OFF = 0;
	public static final int DEBUG_LEVEL_ERROR = 1;
	public static final int DEBUG_LEVEL_WARNING = 2;
	public static final int DEBUG_LEVEL_INFO = 3;
	public static final int DEBUG_LEVEL_DEBUG = 4;
	public static final int DEBUG_LEVEL_VERBOSE = 5;

	private static String TAG = LogUtils.class.getSimpleName();
	private static int LOG_LEVEL = DEBUG_LEVEL_OFF;

	public void tag(final String tag) {
		if (tag != null) {
			TAG = tag;
		} else {
			TAG = LogUtils.class.getSimpleName();
		}
	}

	public static void logLevel(final int level) {
		LOG_LEVEL = level;
	}

	public static int logLevel() {
		return LOG_LEVEL;
	}

	public static void v() {
		if (LOG_LEVEL >= DEBUG_LEVEL_VERBOSE) Log.v(TAG, getMetaInfo());
	}

	public static void v(final String message) {
		if (LOG_LEVEL >= DEBUG_LEVEL_VERBOSE) Log.v(TAG, getMetaInfo() + null2str(message));
	}

	public static void d() {
		if (LOG_LEVEL >= DEBUG_LEVEL_DEBUG) Log.d(TAG, getMetaInfo());
	}

	public static void d(final String message) {
		if (LOG_LEVEL >= DEBUG_LEVEL_DEBUG) Log.d(TAG, getMetaInfo() + null2str(message));
	}

	public static void i() {
		if (LOG_LEVEL >= DEBUG_LEVEL_INFO) Log.i(TAG, getMetaInfo());
	}

	public static void i(final String message) {
		if (LOG_LEVEL >= DEBUG_LEVEL_INFO) Log.i(TAG, getMetaInfo() + null2str(message));
	}

	public static void w(final String message) {
		if (LOG_LEVEL >= DEBUG_LEVEL_WARNING) Log.w(TAG, getMetaInfo() + null2str(message));
	}

	public static void w(final String message, final Throwable e) {
		if (LOG_LEVEL >= DEBUG_LEVEL_WARNING) {
			Log.w(TAG, getMetaInfo() + null2str(message), e);
			printThrowable(e);
			if (e.getCause() != null) {
				printThrowable(e.getCause());
			}
		}
	}

	public static void e(final String message) {
		if (LOG_LEVEL >= DEBUG_LEVEL_ERROR) Log.e(TAG, getMetaInfo() + null2str(message));
	}

	public static void e(final String message, final Throwable e) {
		if (LOG_LEVEL >= DEBUG_LEVEL_ERROR) {
			Log.e(TAG, getMetaInfo() + null2str(message), e);
			printThrowable(e);
			if (e.getCause() != null) {
				printThrowable(e.getCause());
			}
		}
	}

	public static void e(final Throwable e) {
		if (LOG_LEVEL >= DEBUG_LEVEL_ERROR) {
			printThrowable(e);
			if (e.getCause() != null) {
				printThrowable(e.getCause());
			}
		}
	}

	private static String null2str(final String string) {
		if (string == null) {
			return "(null)";
		}
		return string;
	}

	/**
	 * 例外のスタックトレースをログに出力する
	 *
	 * @param e
	 */
	private static void printThrowable(final Throwable e) {
		Log.e(TAG, e.getClass().getName() + ": " + e.getMessage());
		for (final StackTraceElement element : e.getStackTrace()) {
			Log.e(TAG, "  at " + LogUtils.getMetaInfo(element));
		}
	}

	/**
	 * ログ呼び出し元のメタ情報を取得する
	 *
	 * @return [className#methodName:line]
	 */
	private static String getMetaInfo() {
		// スタックトレースから情報を取得
		// 0: VM, 1: Thread, 2: LogUtil#getMetaInfo, 3:LogUtil#d など, 4: 呼び出し元
		final StackTraceElement element = Thread.currentThread().getStackTrace()[4];
		return LogUtils.getMetaInfo(element);
	}

	/**
	 * スタックトレースからクラス名、メソッド名、行数を取得する
	 *
	 * @return [className#methodName:line]
	 */
	public static String getMetaInfo(final StackTraceElement element) {
		// クラス名、メソッド名、行数を取得
		final String fullClassName = element.getClassName();
		final String simpleClassName = fullClassName.substring(fullClassName
				.lastIndexOf(".") + 1);
		final String methodName = element.getMethodName();
		final int lineNumber = element.getLineNumber();
		// メタ情報
		final String metaInfo = "[" + simpleClassName + "#" + methodName + ":"
				+ lineNumber + "]";
		return metaInfo;
	}

}
