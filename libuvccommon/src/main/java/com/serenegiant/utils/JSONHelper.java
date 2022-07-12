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

import org.json.JSONException;
import org.json.JSONObject;

public class JSONHelper {
	private static final boolean DEBUG = false;	// FIXME set false on production
	private static final String TAG = JSONHelper.class.getSimpleName();

	public static long getLong(final JSONObject payload, final String key, final long defaultValue) throws JSONException {
		long result = defaultValue;
		if (payload.has(key)) {
			try {
				result = payload.getLong(key);
			} catch (final JSONException e) {
				try {
					result = Long.parseLong(payload.getString(key));
				} catch (final Exception e1) {
					result = payload.getBoolean(key) ? 1 :0;
				}
			}
		}
		return result;
	}

	public static long optLong(final JSONObject payload, final String key, final long defaultValue) {
		long result = defaultValue;
		if (payload.has(key)) {
			try {
				result = payload.getLong(key);
			} catch (final JSONException e) {
				try {
					result = Long.parseLong(payload.getString(key));
				} catch (final Exception e1) {
					try {
						result = payload.getBoolean(key) ? 1 :0;
					} catch (final Exception e2) {
						Log.w(TAG, e2);
					}
				}
			}
		}
		return result;
	}

	public static int getInt(final JSONObject payload, final String key, final int defaultValue) throws JSONException {
		int result = defaultValue;
		if (payload.has(key)) {
			try {
				result = payload.getInt(key);
			} catch (final JSONException e) {
				try {
					result = Integer.parseInt(payload.getString(key));
				} catch (final Exception e1) {
					result = payload.getBoolean(key) ? 1 :0;
				}
			}
		}
		return result;
	}

	public static int optInt(final JSONObject payload, final String key, final int defaultValue) {
		int result = defaultValue;
		if (payload.has(key)) {
			try {
				result = payload.getInt(key);
			} catch (final JSONException e) {
				try {
					result = Integer.parseInt(payload.getString(key));
				} catch (final Exception e1) {
					try {
						result = payload.getBoolean(key) ? 1 :0;
					} catch (final Exception e2) {
						Log.w(TAG, e2);
					}
				}
			}
		}
		return result;
	}

	public static boolean getBoolean(final JSONObject payload, final String key, final boolean defaultValue) throws JSONException {
		boolean result = defaultValue;
		if (payload.has(key)) {
			try {
				result = payload.getBoolean(key);
			} catch (final Exception e) {
				try {
					result = payload.getInt(key) != 0;
				} catch (JSONException e1) {
					result = payload.getDouble(key) != 0;
				}
			}
		}
		return result;
	}

	public static boolean optBoolean(final JSONObject payload, final String key, final boolean defaultValue) {
		boolean result = defaultValue;
		if (payload.has(key)) {
			try {
				result = payload.getBoolean(key);
			} catch (final Exception e) {
				try {
					result = payload.getInt(key) != 0;
				} catch (JSONException e1) {
					try {
						result = payload.getDouble(key) != 0;
					} catch (JSONException e2) {
						Log.w(TAG, e2);
					}
				}
			}
		}
		return result;
	}
}
