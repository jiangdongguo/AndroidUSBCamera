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

import java.io.BufferedReader;
import java.io.FileReader;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.os.Build;
import android.text.TextUtils;

public final class DeviceInfo {
	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	public static JSONObject get() throws JSONException {
		final JSONObject result = new JSONObject();
		try {
			result.put("BUILD", Build.ID);
		} catch (final Exception e) {
			result.put("BUILD", e.getMessage());
		}
		try {
			result.put("DISPLAY", Build.DISPLAY);
		} catch (final Exception e) {
			result.put("DISPLAY", e.getMessage());
		}
		try {
			result.put("PRODUCT", Build.PRODUCT);
		} catch (final Exception e) {
			result.put("PRODUCT", e.getMessage());
		}
		try {
			result.put("DEVICE", Build.DEVICE);
		} catch (final Exception e) {
			result.put("DEVICE", e.getMessage());
		}
		try {
			result.put("BOARD", Build.BOARD);
		} catch (final Exception e) {
			result.put("BOARD", e.getMessage());
		}
		try {
			result.put("MANUFACTURER", Build.MANUFACTURER);
		} catch (final Exception e) {
			result.put("MANUFACTURER", e.getMessage());
		}
		try {
			result.put("BRAND", Build.BRAND);
		} catch (final Exception e) {
			result.put("BRAND", e.getMessage());
		}
		try {
			result.put("MODEL", Build.MODEL);
		} catch (final Exception e) {
			result.put("MODEL", e.getMessage());
		}
		try {
			result.put("BOOTLOADER", Build.BOOTLOADER);
		} catch (final Exception e) {
			result.put("BOOTLOADER", e.getMessage());
		}
		try {
			result.put("HARDWARE", Build.HARDWARE);
		} catch (final Exception e) {
			result.put("HARDWARE", e.getMessage());
		}
		if (BuildCheck.isAndroid5()) {
			try {
				final String[] supported_abis = Build.SUPPORTED_ABIS;
				if ((supported_abis != null) && (supported_abis.length > 0)) {
					final JSONObject temp = new JSONObject();
					final int n = supported_abis.length;
					for (int i = 0; i < n; i++)
						temp.put(Integer.toString(i), supported_abis[i]);
					result.put("SUPPORTED_ABIS", temp);
				}
			} catch (final Exception e) {
				result.put("SUPPORTED_ABIS", e.getMessage());
			}
			try {
				final String[] supported_abis32 = Build.SUPPORTED_32_BIT_ABIS;
				if ((supported_abis32 != null) && (supported_abis32.length > 0)) {
					final JSONObject temp = new JSONObject();
					final int n = supported_abis32.length;
					for (int i = 0; i < n; i++)
						temp.put(Integer.toString(i), supported_abis32[i]);
					result.put("SUPPORTED_32_BIT_ABIS", temp);
				}
			} catch (final Exception e) {
				result.put("SUPPORTED_32_BIT_ABIS", e.getMessage());
			}
			try {
				final String[] supported_abis64 = Build.SUPPORTED_64_BIT_ABIS;
				if ((supported_abis64 != null) && (supported_abis64.length > 0)) {
					final JSONObject temp = new JSONObject();
					final int n = supported_abis64.length;
					for (int i = 0; i < n; i++)
						temp.put(Integer.toString(i), supported_abis64[i]);
					result.put("SUPPORTED_64_BIT_ABIS", temp);
				}
			} catch (final Exception e) {
				result.put("SUPPORTED_64_BIT_ABIS", e.getMessage());
			}
		} else {
			try {
				final JSONObject temp = new JSONObject();
				temp.put("0", Build.CPU_ABI);
				temp.put("1", Build.CPU_ABI2);
				result.put("SUPPORTED_ABIS", temp);
			} catch (final Exception e) {
				result.put("SUPPORTED_ABIS", e.getMessage());
			}
		}
		try {
			result.put("RELEASE", Build.VERSION.RELEASE);
		} catch (final Exception e) {
			result.put("RELEASE", e.getMessage());
		}
		try {
			result.put("API_LEVEL", Build.VERSION.SDK_INT);
		} catch (final Exception e) {
			result.put("API_LEVEL", e.getMessage());
		}
		try {
			String proc_version = null;
			final BufferedReader reader = new BufferedReader(new FileReader("/proc/version"), 512);
			proc_version = reader.readLine();
			reader.close();
			result.put("PROC_VERSION", proc_version);
		} catch (final Exception e) {
			result.put("PROC_VERSION", e.getMessage());
		}

		final JSONObject cpu_info = new JSONObject();
		int i = 0;
		String proc_cpuinfo = null;
		try {
			final BufferedReader reader = new BufferedReader(new FileReader("/proc/cpuinfo"), 512);
			do {
				proc_cpuinfo = reader.readLine();
				if (proc_cpuinfo == null) break;
				if (!TextUtils.isEmpty(proc_cpuinfo))
					cpu_info.put(Integer.toString(i++), proc_cpuinfo);
			} while (proc_cpuinfo != null);
			reader.close();
			result.put("PROC_CPUINFO",  cpu_info);
		} catch (final Exception e) {
			result.put("PROC_CPUINFO",  e.getMessage());
		}
		return result;
	}
}
