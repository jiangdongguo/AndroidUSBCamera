package com.serenegiant.db;
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

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.util.Log;


public final class CursorHelper {
	//	private static final boolean DEBUG = false;	// FIXME 実働時にはfalseにすること
	private static final String TAG = CursorHelper.class.getSimpleName();
	
	public static String getString(final Cursor cursor, final String columnName, final String defaultValue) {
		String result = defaultValue;
		try {
			result = cursor.getString(cursor.getColumnIndexOrThrow(columnName));
		} catch (final Exception e) {
//			if (DEBUG) Log.w(TAG, e);
		}
		return result;
	}
	
	public static CharSequence getCharSequence(final Cursor cursor, final String columnName, final CharSequence defaultValue) {
		CharSequence result = defaultValue;
		try {
			result = cursor.getString(cursor.getColumnIndexOrThrow(columnName));
		} catch (final Exception e) {
//			if (DEBUG) Log.w(TAG, e);
		}
		return result;
	}
	
	public static String getString(final Cursor cursor, final String columnName, final CharSequence defaultValue) {
		final CharSequence result = getCharSequence(cursor, columnName, defaultValue);
		return result != null ? result.toString() : null;
	}
	
	public static int getInt(final Cursor cursor, final String columnName, final int defaultValue) {
		int result = defaultValue;
		try {
			result = cursor.getInt(cursor.getColumnIndexOrThrow(columnName));
		} catch (final Exception e) {
//			if (DEBUG) Log.w(TAG, e);
		}
		return result;
	}
	
	public static short getShort(final Cursor cursor, final String columnName, final short defaultValue) {
		short result = defaultValue;
		try {
			result = cursor.getShort(cursor.getColumnIndexOrThrow(columnName));
		} catch (final Exception e) {
//			if (DEBUG) Log.w(TAG, e);
		}
		return result;
	}
	
	public static long getLong(final Cursor cursor, final String columnName, final long defaultValue) {
		long result = defaultValue;
		try {
			result = cursor.getLong(cursor.getColumnIndexOrThrow(columnName));
		} catch (final Exception e) {
//			if (DEBUG) Log.w(TAG, e);
		}
		return result;
	}
	
	public static float getFloat(final Cursor cursor, final String columnName, final float defaultValue) {
		float result = defaultValue;
		try {
			result = cursor.getFloat(cursor.getColumnIndexOrThrow(columnName));
		} catch (final Exception e) {
//			if (DEBUG) Log.w(TAG, e);
		}
		return result;
	}
	
	public static double getDouble(final Cursor cursor, final String columnName, final double defaultValue) {
		double result = defaultValue;
		try {
			result = cursor.getDouble(cursor.getColumnIndexOrThrow(columnName));
		} catch (final Exception e) {
//			if (DEBUG) Log.w(TAG, e);
		}
		return result;
	}
	
	/**
	 * カラム名"_id"から値を読み取り指定したidと一致するpositionを探す。見つからなければ-1を返す
	 *
	 * @param cursor
	 * @param requestID
	 * @return
	 */
	public static int findPositionFromId(final Cursor cursor, final long requestID) {
		int savedPosition, position = -1;
		if (cursor != null) {
			savedPosition = cursor.getPosition();
			try {
				if (cursor.moveToFirst()) {
					long rowId;
					do {
						rowId = CursorHelper.getLong(cursor, "_id", 0);
						if (rowId == requestID) {
							position = cursor.getPosition();
							break;
						}
					} while (cursor.moveToNext());
				}
			} finally {
				cursor.moveToPosition(savedPosition);
			}
		}
		return position;
	}
	
	@SuppressLint("NewApi")
	public static void dumpCursor(final Cursor cursor) {
		if (cursor.moveToFirst()) {
			int n, row = 0;
			final StringBuilder sb = new StringBuilder();
			String[] columnNames;
			n = cursor.getColumnCount();
			columnNames = cursor.getColumnNames();
			do {
				sb.setLength(0);
				sb.append("row=").append(row).append(", ");
				for (int i = 0; i < n; i++) {
					switch (cursor.getType(i)) {
					case Cursor.FIELD_TYPE_FLOAT:
						sb.append(columnNames[i]).append("=").append(cursor.getDouble(i));
						break;
					case Cursor.FIELD_TYPE_INTEGER:
						sb.append(columnNames[i]).append("=").append(cursor.getLong(i));
						break;
					case Cursor.FIELD_TYPE_STRING:
						sb.append(columnNames[i]).append("=").append(cursor.getString(i));
						break;
					case Cursor.FIELD_TYPE_BLOB:
						sb.append(columnNames[i]).append("=").append("BLOB");
						break;
					case Cursor.FIELD_TYPE_NULL:
						sb.append(columnNames[i]).append("=").append("NULL");
						continue;
					default:
						sb.append(columnNames[i]).append("=").append("UNKNOWN");
						continue;
					}
					sb.append(", ");
				}
				Log.v("CursorHelper#dumpCursor:", sb.toString());
				row++;
			} while (cursor.moveToNext());
		}
	}
	
}
