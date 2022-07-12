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

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;

public class ComponentUtils {
	public static void disable(@NonNull final Context context, final Class<?> clazz) {
		setComponentState(context, clazz, false);
	}

	public static void enable(@NonNull final Context context, final Class<?> clazz) {
		setComponentState(context, clazz, true);
	}

	public static void setComponentState(@NonNull final Context context,
		final Class<?> clazz, final boolean enabled) {

		final int newState = enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
		final ComponentName componentName = new ComponentName(context, clazz);
		final PackageManager pm = context.getPackageManager();
		pm.setComponentEnabledSetting(componentName, newState, PackageManager.DONT_KILL_APP);
	}
}
