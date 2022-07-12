package com.serenegiant.utils;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;

import androidx.annotation.NonNull;

public class ResourceHelper {

	public static int get(@NonNull final Context context,
		final String value, final int defaultValue)
		throws Resources.NotFoundException, NumberFormatException, NullPointerException {

		int result = defaultValue;
		String v = value;
		if (!TextUtils.isEmpty(v) && v.startsWith("@")) {
			final String r = v.substring(1);
			final int resId = context.getResources().getIdentifier(r, null, context.getPackageName());
			if (resId > 0) {
				result = context.getResources().getInteger(resId);
			}
		} else {
			int radix = 10;
			if (v != null && v.length() > 2 && v.charAt(0) == '0' &&
				(v.charAt(1) == 'x' || v.charAt(1) == 'X')) {
				// allow hex values starting with 0x or 0X
				radix = 16;
				v = v.substring(2);
			}
			result = Integer.parseInt(v, radix);
		}

		return result;
	}

	public static boolean get(@NonNull final Context context,
		final String value, final boolean defaultValue)
			throws Resources.NotFoundException, NumberFormatException, NullPointerException {

		boolean result = defaultValue;
		String v = value;
		if ("TRUE".equalsIgnoreCase(v)) {
			result = true;
		} else if ("FALSE".equalsIgnoreCase(v)) {
			result = false;
		} else if (!TextUtils.isEmpty(v) && v.startsWith("@")) {
			final String r = v.substring(1);
			final int resId = context.getResources().getIdentifier(r, null, context.getPackageName());
			if (resId > 0) {
				result = context.getResources().getBoolean(resId);
			}
		} else {
			int radix = 10;
			if (v != null && v.length() > 2 && v.charAt(0) == '0' &&
				(v.charAt(1) == 'x' || v.charAt(1) == 'X')) {
				// allow hex values starting with 0x or 0X
				radix = 16;
				v = v.substring(2);
			}
			final int val = Integer.parseInt(v, radix);
			result = val != 0;
		}

		return result;
	}

	public static String get(@NonNull final Context context,
		final String value, final String defaultValue)
			throws Resources.NotFoundException, NullPointerException {

		String result = value;
		if (result == null) {
			result = defaultValue;
		}
		if (!TextUtils.isEmpty(result) && result.startsWith("@")) {
			final String r = result.substring(1);
			final int resId = context.getResources().getIdentifier(r, null, context.getPackageName());
			if (resId > 0) {
				result = context.getResources().getString(resId);
			}
		}

		return result;
	}

	public static CharSequence get(@NonNull final Context context,
		final CharSequence value, final CharSequence defaultValue)
			throws Resources.NotFoundException, NullPointerException {

		CharSequence result = value;
		if (result == null) {
			result = defaultValue;
		}
		if (!TextUtils.isEmpty(result)) {
			final String s = result.toString();
			if (s.startsWith("@")) {
				final String r = s.substring(1);
				final int resId = context.getResources().getIdentifier(r, null, context.getPackageName());
				if (resId > 0) {
					result = context.getResources().getText(resId);
				}
			}
		}

		return result;
	}
}
