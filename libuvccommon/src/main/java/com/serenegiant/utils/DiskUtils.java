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

import android.content.Context;
import android.os.Environment;
import androidx.annotation.NonNull;

import java.io.File;

/**
 * Imported by saki on 15/11/10.
 */
public class DiskUtils {
	/**
	 * キャッシュディレクトリのフルパスを取得する
	 * 外部ストレージが使える場合は外部ストレージのキャッシュディレクトリを、そうでない場合は内部のディレクトリを使う
	 * @param context
	 * @param uniqueName
	 * @return キャッシュディレクトリパス
	 */
	public static String getCacheDir(@NonNull final Context context, final String uniqueName) {
		// 外部ストレージが使える場合はそっちのディレクトリを、そうでない場合は内部のディレクトリを使う
		final String cachePath =
				(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
				 && !Environment.isExternalStorageRemovable()	// これが使えるのはAPI9以上
				) ? context.getExternalCacheDir().getPath() : context.getCacheDir().getPath();
		return cachePath + File.separator + uniqueName;
	}
}
