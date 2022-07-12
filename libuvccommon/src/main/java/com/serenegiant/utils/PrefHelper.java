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

import android.content.SharedPreferences;
import androidx.annotation.Nullable;

import java.util.Map;

/**
 * Created by saki on 2016/11/05.
 *
 */
public class PrefHelper {
	public static short get(@Nullable final SharedPreferences pref,
		final String key, final short defaultValue) {

		short result = defaultValue;
		if ((pref != null) && pref.contains(key)) {
			try {
				result = (short)pref.getInt(key, defaultValue);
			} catch (final Exception e) {
				result = ObjectHelper.asShort(
					getObject(pref, key, defaultValue), defaultValue);
			}
		}
		return result;
	}

	public static int get(@Nullable final SharedPreferences pref,
		final String key, final int defaultValue) {

		int result = defaultValue;
		if ((pref != null) && pref.contains(key)) {
			try {
				result = pref.getInt(key, defaultValue);
			} catch (final Exception e) {
				result = ObjectHelper.asInt(
					getObject(pref, key, defaultValue), defaultValue);
			}
		}
		return result;
	}

	public static long get(@Nullable final SharedPreferences pref,
		final String key, final long defaultValue) {

		long result = defaultValue;
		if ((pref != null) && pref.contains(key)) {
			try {
				result = pref.getLong(key, defaultValue);
			} catch (final Exception e) {
				result = ObjectHelper.asLong(
					getObject(pref, key, defaultValue), defaultValue);
			}
		}
		return result;
	}

	public static float get(@Nullable final SharedPreferences pref,
		final String key, final float defaultValue) {

		float result = defaultValue;
		if ((pref != null) && pref.contains(key)) {
			try {
				result = pref.getFloat(key, defaultValue);
			} catch (final Exception e) {
				result = ObjectHelper.asFloat(
					getObject(pref, key, defaultValue), defaultValue);
			}
		}
		return result;
	}

	public static double get(@Nullable final SharedPreferences pref,
		final String key, final double defaultValue) {

		double result = defaultValue;
		if ((pref != null) && pref.contains(key)) {
			try {
				result = Double.parseDouble(pref.getString(key, Double.toString(defaultValue)));
			} catch (final Exception e) {
				result = ObjectHelper.asDouble(
					getObject(pref, key, defaultValue), defaultValue);
			}
		}
		return result;
	}

	public static boolean get(@Nullable final SharedPreferences pref,
		final String key, final boolean defaultValue) {

		boolean result = defaultValue;
		if ((pref != null) && pref.contains(key)) {
			try {
				result = pref.getBoolean(key, defaultValue);
			} catch (final Exception e) {
				result = ObjectHelper.asBoolean(
					get(pref, key, defaultValue), defaultValue);
			}
		}
		return result;
	}

	public static final Object getObject(@Nullable final SharedPreferences pref,
		final String key) {

		return getObject(pref, key, null);
	}

	public static final Object getObject(@Nullable final SharedPreferences pref,
		final String key, final Object defaultValue) {

		Object result = defaultValue;
		if ((pref != null) && pref.contains(key)) {
			final Map<String, ?> all = pref.getAll();
			result = all.get(key);
		}
		return result;
	}

}
