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

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

public class IEnum {

	public interface EnumInterface {
		public int id();
		public String label();
		public String name();
		public int ordinal();
	}

	public interface EnumInterfaceEx extends EnumInterface {
		public void put(final JSONObject json) throws JSONException;
		public void put(final String key, final JSONObject json) throws JSONException;
	}

	/**
	 * find enum specified by Class and id
	 * @param enumClazz
	 * @param id
	 * @param <E>
	 * @return
	 */
	public static <E extends EnumInterface> E as(final Class<E> enumClazz, final int id)
		throws IllegalArgumentException {

		E result;
		for (final E e: enumClazz.getEnumConstants()) {
			if (e.id() == id) {
				return e;
			}
		}
		throw new IllegalArgumentException();
	}

	/**
	 * find enum specified by Class and label
	 * @param enumClazz
	 * @param label
	 * @param <E>
	 * @return
	 */
	public static <E extends EnumInterface> E as(final Class<E> enumClazz, final String label)
		throws IllegalArgumentException {

		E result;
		if (!TextUtils.isEmpty(label)) {
			for (final E e: enumClazz.getEnumConstants()) {
				if (label.equalsIgnoreCase(e.label())) {
					return e;
				}
			}
			final String _label = label.toUpperCase();
			for (final E e: enumClazz.getEnumConstants()) {
				if (label.startsWith(e.name().toUpperCase())) {
					return e;
				}
			}
		}
		throw new IllegalArgumentException();
	}
	
	/**
	 * find enum specified by Class and id/label
	 * if enum not found by id, try to find with label
	 * @param enumClazz
	 * @param id
	 * @param label
	 * @param <E>
	 * @return
	 */
	public static <E extends EnumInterface> E as(final Class<E> enumClazz,
		final int id, final String label) throws IllegalArgumentException {
		
		try {
			return as(enumClazz, id);
		} catch (final IllegalArgumentException e) {
			// ignore
		}
		return as(enumClazz, label);
	}

	/**
	 * find enum specified by Class and id/label
	 * if enum not found by label, try to find with id
	 * @param enumClazz
	 * @param label
	 * @param id
	 * @param <E>
	 * @return
	 */
	public static <E extends EnumInterface> E as(final Class<E> enumClazz,
		final String label, final int id) throws IllegalArgumentException {
		
		try {
			return as(enumClazz, label);
		} catch (final IllegalArgumentException e) {
			// ignore
		}
		return as(enumClazz, id);
	}

	/**
	 * split identity from value string
	 * "UVC123456" => return "123456",
	 * "MIC1" => "1"
	 * @param enumClazz
	 * @param value
	 * @param <E>
	 * @return
	 */
	public static <E extends EnumInterface> String identity(final Class<E> enumClazz, final String value) {
		String result = null;
		if (!TextUtils.isEmpty(value)) {
			final String _value = value.toUpperCase();
			for (final E e: enumClazz.getEnumConstants()) {
				if (_value.startsWith(e.name().toUpperCase())) {
					result = value.substring(e.name().length() + 1);
				}
			}
		}
		return result;
	}

	public static JSONObject put(final JSONObject payload, final String key, final EnumInterface v)
		throws JSONException {

		payload.put(key, v.label());
		return payload;
	}

}
