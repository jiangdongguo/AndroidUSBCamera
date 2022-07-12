package com.serenegiant.media;
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

import android.os.Environment;
import android.util.Log;

import com.serenegiant.common.BuildConfig;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;

public class MediaFileUtils {
	private static final boolean DEBUG = BuildConfig.DEBUG;
	private static final String TAG = "MediaFileUtils";

	private static final SimpleDateFormat sDateTimeFormat
		= new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);

	/**
	 * generate output file
	 * @param type Environment.DIRECTORY_MOVIES / Environment.DIRECTORY_DCIM etc.
	 * @param ext .mp4(.m4a for audio) or .png
	 * @return return null when this app has no writing permission to external storage.
	 */
	public static final File getCaptureFile(final String dir_name, final String type, final String ext) {
		final File dir = new File(Environment.getExternalStoragePublicDirectory(type), dir_name);
		if (DEBUG) Log.d(TAG, "path=" + dir.toString());
		dir.mkdirs();
		if (dir.canWrite()) {
			return new File(dir, getDateTimeString() + ext);
		}
		return null;
	}

	/**
	 * get current date and time as String
	 * @return
	 */
	private static final String getDateTimeString() {
		final GregorianCalendar now = new GregorianCalendar();
		return sDateTimeFormat.format(now.getTime());
	}
}
