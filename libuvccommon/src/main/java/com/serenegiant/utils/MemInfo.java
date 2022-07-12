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
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;
import android.text.TextUtils;

public class MemInfo {

	@SuppressLint("NewApi")
	public static JSONObject get(final Context contex) throws JSONException {
		final JSONObject result = new JSONObject();
		try {
			try {
				final ActivityManager.MemoryInfo mem_info = new ActivityManager.MemoryInfo();
				final ActivityManager am = ((ActivityManager)contex.getSystemService(Activity.ACTIVITY_SERVICE));
				am.getMemoryInfo(mem_info);
				final JSONObject am_info = new JSONObject();
				am_info.put("availMem", mem_info.availMem);
				am_info.put("totalMem", mem_info.totalMem);
				am_info.put("threshold", mem_info.threshold);
				am_info.put("lowMemory", mem_info.lowMemory);
				result.put("ACTIVITYMANAGER_MEMORYINFO",  am_info);
			} catch (final Exception e) {
				result.put("ACTIVITYMANAGER_MEMORYINFO",  e.getMessage());
			}

			try {
				final Debug.MemoryInfo dmeminfo = new Debug.MemoryInfo();
				Debug.getMemoryInfo(dmeminfo);
				final JSONObject dm_info = new JSONObject();
				dm_info.put("TotalPss", dmeminfo.getTotalPss());
				dm_info.put("TotalPrivateDirty", dmeminfo.getTotalPrivateDirty());
				dm_info.put("TotalSharedDirty", dmeminfo.getTotalSharedDirty());
				if (BuildCheck.isAndroid4_4()) {
					dm_info.put("TotalPrivateClean", dmeminfo.getTotalPrivateClean());
					dm_info.put("TotalSharedClean", dmeminfo.getTotalSharedClean());
					dm_info.put("TotalSwappablePss", dmeminfo.getTotalSwappablePss());
				}
				result.put("DEBUG_MEMORYINFO", dm_info);
			} catch (final Exception e) {
				result.put("DEBUG_MEMORYINFO", e.getMessage());
			}

			try {
				final JSONObject pm_info = new JSONObject();
				int i = 0;
				String proc_meminfo = null;
				final BufferedReader reader = new BufferedReader(new FileReader("/proc/meminfo"), 512);
				do {
					proc_meminfo = reader.readLine();
					if (proc_meminfo == null) break;
					if (!TextUtils.isEmpty(proc_meminfo))
						pm_info.put(Integer.toString(i++), proc_meminfo);
				} while (proc_meminfo != null);
				reader.close();
				result.put("PROC_MEMINFO", pm_info);
			} catch (final Exception e) {
				result.put("PROC_MEMINFO", e.getMessage());
			}
		} catch (final Exception e) {
			result.put("EXCEPTION", e.getMessage());
		}
		return result;
	}

}
