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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.accessibility.AccessibilityEventCompat;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

public class TalkBackHelper {
	/**
	 * Accessibilityが有効になっているかどうかを取得
	 * @param context
	 * @return
	 */
	public static boolean isEnabled(@NonNull final Context context) {
		final AccessibilityManager manager = (AccessibilityManager) context
			.getSystemService(Context.ACCESSIBILITY_SERVICE);
		return manager.isEnabled();
	}

	/**
	 * 指定したテキストをTalkBackで読み上げる(TalkBackが有効な場合)
	 * @param context
	 * @param text
	 * @throws IllegalStateException
	 */
	public static void announceText(@NonNull final Context context,
		@Nullable final CharSequence text) throws IllegalStateException {

		if (TextUtils.isEmpty(text) || (context == null)) return;
		final AccessibilityManager manager = (AccessibilityManager) context
			.getSystemService(Context.ACCESSIBILITY_SERVICE);
		if ((manager != null) && manager.isEnabled()) {
			final AccessibilityEvent event = AccessibilityEvent.obtain();
			if (event != null) {
				event.setEventType(AccessibilityEventCompat.TYPE_ANNOUNCEMENT);
				event.setClassName(TalkBackHelper.class.getName());
				event.setPackageName(context.getPackageName());
				event.getText().add(text);
				manager.sendAccessibilityEvent(event);
			} else {
				throw new IllegalStateException("failed to obtain AccessibilityEvent");
			}
		} else {
			throw new IllegalStateException("AccessibilityManager is not available/or disabled");
		}
	}

	/**
	 * 指定したテキストをTalkBackで読み上げる(TalkBackが有効な場合)
	 * @param context
	 * @param text
	 * @throws IllegalStateException
	 */
	public static void announceText(@NonNull final Context context,
		@Nullable final CharSequence[] text) throws IllegalStateException {

		if ((text == null) || (text.length == 0) || (context == null)) return;
		final AccessibilityManager manager = (AccessibilityManager) context
			.getSystemService(Context.ACCESSIBILITY_SERVICE);
		if ((manager != null) && manager.isEnabled()) {
			final AccessibilityEvent event = AccessibilityEvent.obtain();
			if (event != null) {
				event.setEventType(AccessibilityEventCompat.TYPE_ANNOUNCEMENT);
				event.setClassName(TalkBackHelper.class.getName());
				event.setPackageName(context.getPackageName());
				for (final CharSequence t: text) {
					event.getText().add(t);
				}
				manager.sendAccessibilityEvent(event);
			} else {
				throw new IllegalStateException("failed to obtain AccessibilityEvent");
			}
		} else {
			throw new IllegalStateException("AccessibilityManager is not available/or disabled");
		}
	}
}
