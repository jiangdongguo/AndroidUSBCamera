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

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Point;
import androidx.annotation.NonNull;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

public class ScreenInfo {

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	public static JSONObject get(@NonNull final Activity activity) throws JSONException {
		final JSONObject result = new JSONObject();
		try {
			final WindowManager wm = activity.getWindowManager();
			final Display display = wm.getDefaultDisplay();
			final DisplayMetrics metrics = new DisplayMetrics();
			display.getMetrics(metrics);
			try {
				result.put("widthPixels", metrics.widthPixels);
			} catch (final Exception e) {
				result.put("widthPixels", e.getMessage());
			}
			try {
				result.put("heightPixels", metrics.heightPixels);
			} catch (final Exception e) {
				result.put("heightPixels", e.getMessage());
			}
			try {
				result.put("density", metrics.density);
			} catch (final Exception e) {
				result.put("density", e.getMessage());
			}
			try {
				result.put("densityDpi", metrics.densityDpi);
			} catch (final Exception e) {
				result.put("densityDpi", e.getMessage());
			}
			try {
				result.put("scaledDensity", metrics.scaledDensity);
			} catch (final Exception e) {
				result.put("scaledDensity", e.getMessage());
			}
			try {
				result.put("xdpi", metrics.xdpi);
			} catch (final Exception e) {
				result.put("xdpi", e.getMessage());
			}
			try {
				result.put("ydpi", metrics.ydpi);
			} catch (final Exception e) {
				result.put("ydpi", e.getMessage());
			}
			try {
				final Point size = new Point();
				if (BuildCheck.isAndroid4_2()) {
					display.getRealSize(size);
					result.put("width", size.x);
					result.put("height", size.y);
				} else {
					result.put("width", display.getWidth());
					result.put("height", display.getHeight());
				}
			} catch (final Exception e) {
				result.put("size", e.getMessage());
			}
		} catch (final Exception e) {
			result.put("EXCEPTION", e.getMessage());
		}
		return result;
	}


}
