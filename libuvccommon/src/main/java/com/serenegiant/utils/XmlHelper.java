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
import android.text.TextUtils;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.List;

/**
 * XmlPullParserのヘルパークラス
 */
public class XmlHelper {
	/**
	 * read as integer values with default value from xml(w/o exception throws)
	 * resource integer id is also resolved into integer
	 * @param parser
	 * @param namespace
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public static final int getAttribute(@NonNull final Context context,
		@NonNull final XmlPullParser parser,
		final String namespace, final String name, final int defaultValue) {

		try {
			return ResourceHelper.get(context, parser.getAttributeValue(namespace, name), defaultValue);
		} catch (final Exception e) {
			return defaultValue;
		}
	}

	/**
	 * read as boolean values with default value from xml(w/o exception throws)
	 * resource boolean id is also resolved into boolean
	 * if the value is zero, return false, if the value is non-zero integer, return true
	 * @param context
	 * @param parser
	 * @param namespace
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public static final boolean getAttribute(@NonNull final Context context,
		@NonNull final XmlPullParser parser,
		final String namespace, final String name, final boolean defaultValue) {

		try {
			return ResourceHelper.get(context, parser.getAttributeValue(namespace, name), defaultValue);
		} catch (final Exception e) {
			return defaultValue;
		}
	}

	/**
	 * read as String attribute with default value from xml(w/o exception throws)
	 * resource string id is also resolved into string
	 * @param parser
	 * @param namespace
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public static final String getAttribute(@NonNull final Context context,
		@NonNull final XmlPullParser parser,
		final String namespace, final String name, final String defaultValue) {

		try {
			return ResourceHelper.get(context, parser.getAttributeValue(namespace, name), defaultValue);
		} catch (final Exception e) {
			return defaultValue;
		}
	}

	/**
	 * read as String attribute with default value from xml(w/o exception throws)
	 * resource string id is also resolved into string
	 * @param parser
	 * @param namespace
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public static final CharSequence getAttribute(@NonNull final Context context,
		@NonNull final XmlPullParser parser,
		final String namespace, final String name, final CharSequence defaultValue) {

		try {
			return ResourceHelper.get(context, parser.getAttributeValue(namespace, name), defaultValue);
		} catch (final Exception e) {
			return defaultValue;
		}
	}
	
	public static final int[] getAttribute(@NonNull final Context context,
		@NonNull final XmlPullParser parser,
		final String namespace, final String name, final int[] defaultValue) {
		
		int[] result = defaultValue;
		
		final String valueString = getAttribute(context, parser, namespace, name, "");
		if (!TextUtils.isEmpty(valueString)) {
			final String[] values = valueString.split(",");
			final List<Integer> list = new ArrayList<>();
			final int n = values.length;
			for (final String value: values) {
				try {
					list.add(ResourceHelper.get(context, parser.getAttributeValue(namespace, name), 0));
				} catch (final Exception e) {
					// ignore
				}
			}
			if (list.size() > 0) {
				result = new int[list.size()];
				int i = 0;
				for (final Integer value: list) {
					result[i++] = value;
				}
			}
		}
		return result;
	}

	public static final boolean[] getAttribute(@NonNull final Context context,
		@NonNull final XmlPullParser parser,
		final String namespace, final String name, final boolean[] defaultValue) {
		
		boolean[] result = defaultValue;
		
		final String valueString = getAttribute(context, parser, namespace, name, "");
		if (!TextUtils.isEmpty(valueString)) {
			final String[] values = valueString.split(",");
			final List<Boolean> list = new ArrayList<>();
			final int n = values.length;
			for (final String value: values) {
				try {
					list.add(ResourceHelper.get(context, parser.getAttributeValue(namespace, name), false));
				} catch (final Exception e) {
					// ignore
				}
			}
			if (list.size() > 0) {
				result = new boolean[list.size()];
				int i = 0;
				for (final Boolean value: list) {
					result[i++] = value;
				}
			}
		}
		return result;
	}
}
