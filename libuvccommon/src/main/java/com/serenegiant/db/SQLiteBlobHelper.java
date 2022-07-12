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

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.serenegiant.graphics.BitmapHelper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class SQLiteBlobHelper {
//	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
//	private static final String TAG = SQLiteBlobHelper.class.getSimpleName();
	
	/**
	 * float[]をbyte[]に変換して返す
	 *
	 * @param array
	 * @param offset
	 * @param num
	 * @return
	 */
	public static byte[] floatArrayToByteArray(
		@NonNull final float[] array, final int offset, final int num) {
		
		final ByteBuffer buf = ByteBuffer.allocate(num * Float.SIZE / 8);
		buf.order(ByteOrder.nativeOrder());
		final int n8 = num % 8 + offset;
		final int n = offset + num;
		for (int i = offset; i < n8; i++) buf.putFloat(array[i]);
		for (int i = n8; i < n; i += 8) {
			buf.putFloat(array[i]);
			buf.putFloat(array[i + 1]);
			buf.putFloat(array[i + 2]);
			buf.putFloat(array[i + 3]);
			buf.putFloat(array[i + 4]);
			buf.putFloat(array[i + 5]);
			buf.putFloat(array[i + 6]);
			buf.putFloat(array[i + 7]);
		}
		buf.flip();
		return buf.array();
	}
	
	/**
	 * byte[]をfloat[]に変換して返す
	 *
	 * @param bytes
	 * @return
	 */
	@Nullable
	public static float[] byteArrayToFloatArray(
		@Nullable final byte[] bytes) {
		
		if ((bytes == null) || (bytes.length < Float.SIZE / 8)) return null;
		final ByteBuffer tmp = ByteBuffer.wrap(bytes);
		tmp.order(ByteOrder.nativeOrder());
		final int n = tmp.limit() / (Float.SIZE / 8);    // nはfloatの配列とみなした時の要素数
		final float[] array = new float[n];
		final int n8 = n % 8;
		for (int i = 0; i < n8; i++) array[i] = tmp.getFloat();
		for (int i = n8; i < n; i += 8) {
			array[i] = tmp.getFloat();
			array[i + 1] = tmp.getFloat();
			array[i + 2] = tmp.getFloat();
			array[i + 3] = tmp.getFloat();
			array[i + 4] = tmp.getFloat();
			array[i + 5] = tmp.getFloat();
			array[i + 6] = tmp.getFloat();
			array[i + 7] = tmp.getFloat();
		}
		return array;
	}
	
	/**
	 * double[]をbyte[]に変換して返す
	 *
	 * @param array
	 * @param offset
	 * @param num
	 * @return
	 */
	public static byte[] doubleArrayToByteArray(
		@NonNull final double[] array, final int offset, final int num) {
		
		final ByteBuffer buf = ByteBuffer.allocate(num * Double.SIZE / 8);
		buf.order(ByteOrder.nativeOrder());
		final int n8 = num % 8 + offset;
		final int n = offset + num;
		for (int i = offset; i < n8; i++) buf.putDouble(array[i]);
		for (int i = n8; i < n; i += 8) {
			buf.putDouble(array[i]);
			buf.putDouble(array[i + 1]);
			buf.putDouble(array[i + 2]);
			buf.putDouble(array[i + 3]);
			buf.putDouble(array[i + 4]);
			buf.putDouble(array[i + 5]);
			buf.putDouble(array[i + 6]);
			buf.putDouble(array[i + 7]);
		}
		buf.flip();
		return buf.array();
	}
	
	/**
	 * byte[]をdouble[]に変換して返す
	 *
	 * @param bytes
	 * @return
	 */
	@Nullable
	public static double[] byteArrayToDoubleArray(
		@Nullable final byte[] bytes) {
		
		if ((bytes == null) || (bytes.length < Double.SIZE / 8)) return null;
		final ByteBuffer tmp = ByteBuffer.wrap(bytes);
		tmp.order(ByteOrder.nativeOrder());
		final int n = tmp.limit() / (Double.SIZE / 8);    // nはdoubleの配列とみなした時の要素数
		final double[] array = new double[n];
		final int n8 = n % 8;
		for (int i = 0; i < n8; i++) array[i] = tmp.getDouble();
		for (int i = n8; i < n; i += 8) {
			array[i] = tmp.getDouble();
			array[i + 1] = tmp.getDouble();
			array[i + 2] = tmp.getDouble();
			array[i + 3] = tmp.getDouble();
			array[i + 4] = tmp.getDouble();
			array[i + 5] = tmp.getDouble();
			array[i + 6] = tmp.getDouble();
			array[i + 7] = tmp.getDouble();
		}
		return array;
	}
	
	/**
	 * double[]をbyte[]に変換して返す
	 *
	 * @param array
	 * @param offset
	 * @param num
	 * @return
	 */
	public static byte[] intArrayToByteArray(
		@NonNull final int[] array, final int offset, final int num) {
		
		final ByteBuffer buf = ByteBuffer.allocate(num * Integer.SIZE / 8);
		buf.order(ByteOrder.nativeOrder());
		final int n8 = num % 8 + offset;
		final int n = offset + num;
		for (int i = offset; i < n8; i++) buf.putInt(array[i]);
		for (int i = n8; i < n; i += 8) {
			buf.putInt(array[i]);
			buf.putInt(array[i + 1]);
			buf.putInt(array[i + 2]);
			buf.putInt(array[i + 3]);
			buf.putInt(array[i + 4]);
			buf.putInt(array[i + 5]);
			buf.putInt(array[i + 6]);
			buf.putInt(array[i + 7]);
		}
		buf.flip();
		return buf.array();
	}
	
	/**
	 * byte[]をint[]に変換して返す
	 *
	 * @param bytes
	 * @return
	 */
	@Nullable
	public static int[] byteArrayToIntArray(
		@Nullable final byte[] bytes) {
		
		if ((bytes == null) || (bytes.length < Integer.SIZE / 8)) return null;
		final ByteBuffer tmp = ByteBuffer.wrap(bytes);
		tmp.order(ByteOrder.nativeOrder());
		final int n = tmp.limit() / (Integer.SIZE / 8);    // nはintの配列とみなした時の要素数
		final int[] array = new int[n];
		final int n8 = n % 8;
		for (int i = 0; i < n8; i++) array[i] = tmp.getInt();
		for (int i = n8; i < n; i += 8) {
			array[i] = tmp.getInt();
			array[i + 1] = tmp.getInt();
			array[i + 2] = tmp.getInt();
			array[i + 3] = tmp.getInt();
			array[i + 4] = tmp.getInt();
			array[i + 5] = tmp.getInt();
			array[i + 6] = tmp.getInt();
			array[i + 7] = tmp.getInt();
		}
		return array;
	}
	
	/**
	 * short[]をbyte[]に変換して返す
	 *
	 * @param array
	 * @param offset
	 * @param num
	 * @return
	 */
	public static byte[] shortArrayToByteArray(
		@NonNull final short[] array, final int offset, final int num) {
		
		final ByteBuffer buf = ByteBuffer.allocate(num * Short.SIZE / 8);
		buf.order(ByteOrder.nativeOrder());
		final int n8 = num % 8 + offset;
		final int n = offset + num;
		for (int i = offset; i < n8; i++) buf.putShort(array[i]);
		for (int i = n8; i < n; i += 8) {
			buf.putShort(array[i]);
			buf.putShort(array[i + 1]);
			buf.putShort(array[i + 2]);
			buf.putShort(array[i + 3]);
			buf.putShort(array[i + 4]);
			buf.putShort(array[i + 5]);
			buf.putShort(array[i + 6]);
			buf.putShort(array[i + 7]);
		}
		buf.flip();
		return buf.array();
	}
	
	/**
	 * byte[]をshort[]に変換して返す
	 *
	 * @param bytes
	 * @return
	 */
	@Nullable
	public static short[] byteArrayToShortArray(
		@Nullable final byte[] bytes) {
		
		if ((bytes == null) || (bytes.length < Short.SIZE / 8)) return null;
		final ByteBuffer tmp = ByteBuffer.wrap(bytes);
		tmp.order(ByteOrder.nativeOrder());
		final int n = tmp.limit() / (Short.SIZE / 8);    // nはshortの配列とみなした時の要素数
		final short[] buf = new short[n];
		final int n8 = n % 8;
		for (int i = 0; i < n8; i++) buf[i] = tmp.getShort();
		for (int i = n8; i < n; i += 8) {
			buf[i] = tmp.getShort();
			buf[i + 1] = tmp.getShort();
			buf[i + 2] = tmp.getShort();
			buf[i + 3] = tmp.getShort();
			buf[i + 4] = tmp.getShort();
			buf[i + 5] = tmp.getShort();
			buf[i + 6] = tmp.getShort();
			buf[i + 7] = tmp.getShort();
		}
		return buf;
	}
	
	/**
	 * long[]をbyte[]に変換して返す
	 *
	 * @param array
	 * @param offset
	 * @param num
	 * @return
	 */
	public static byte[] longArrayToByteArray(
		@NonNull final long[] array, final int offset, final int num) {

		final ByteBuffer buf = ByteBuffer.allocate(num * Long.SIZE / 8);
		buf.order(ByteOrder.nativeOrder());
		final int n8 = num % 8 + offset;
		final int n = offset + num;
		for (int i = offset; i < n8; i++) buf.putLong(array[i]);
		for (int i = n8; i < n; i += 8) {
			buf.putLong(array[i]);
			buf.putLong(array[i + 1]);
			buf.putLong(array[i + 2]);
			buf.putLong(array[i + 3]);
			buf.putLong(array[i + 4]);
			buf.putLong(array[i + 5]);
			buf.putLong(array[i + 6]);
			buf.putLong(array[i + 7]);
		}
		buf.flip();
		return buf.array();
	}
	
	/**
	 * byte[]をlong[]に変換して返す
	 *
	 * @param bytes
	 * @return
	 */
	@Nullable
	public static long[] byteArrayToLongArray(
		@Nullable final byte[] bytes) {

		if ((bytes == null) || (bytes.length < Long.SIZE / 8)) return null;
		final ByteBuffer tmp = ByteBuffer.wrap(bytes);
		tmp.order(ByteOrder.nativeOrder());
		final int n = tmp.limit() / (Long.SIZE / 8);    // nはlongの配列とみなした時の要素数
		final long[] array = new long[n];
		final int n8 = n % 8;
		for (int i = 0; i < n8; i++) array[i] = tmp.getLong();
		for (int i = n8; i < n; i += 8) {
			array[i] = tmp.getLong();
			array[i + 1] = tmp.getLong();
			array[i + 2] = tmp.getLong();
			array[i + 3] = tmp.getLong();
			array[i + 4] = tmp.getLong();
			array[i + 5] = tmp.getLong();
			array[i + 6] = tmp.getLong();
			array[i + 7] = tmp.getLong();
		}
		return array;
	}
	
	/**
	 * SQLiteStatement#bindBlobのヘルパーメソッド。floatの配列をbyteの配列に変換して割り付ける
	 *
	 * @param stat
	 * @param index
	 * @param array
	 */
	public static void bindBlobFloatArray(@NonNull final SQLiteStatement stat,
		final int index, @NonNull final float[] array) {

		stat.bindBlob(index, floatArrayToByteArray(array, 0, array.length));
	}
	
	/**
	 * SQLiteStatement#bindBlobのヘルパーメソッド。floatの配列をbyteの配列に変換して割り付ける
	 *
	 * @param stat
	 * @param index
	 * @param array
	 * @param offset
	 * @param num
	 */
	public static void bindBlobFloatArray(@NonNull final SQLiteStatement stat,
		final int index, @NonNull final float[] array, final int offset, final int num) {

		stat.bindBlob(index, floatArrayToByteArray(array, offset, num));
	}
	
	/**
	 * SQLiteStatement#bindBlobのヘルパーメソッド。doubleの配列をbyteの配列に変換して割り付ける
	 *
	 * @param stat
	 * @param index
	 * @param array
	 */
	public static void bindBlobDoubleArray(@NonNull final SQLiteStatement stat,
		final int index, @NonNull final double[] array) {

		stat.bindBlob(index, doubleArrayToByteArray(array, 0, array.length));
	}
	
	/**
	 * SQLiteStatement#bindBlobのヘルパーメソッド。doubleの配列をbyteの配列に変換して割り付ける
	 *
	 * @param stat
	 * @param index
	 * @param array
	 * @param offset
	 * @param num
	 */
	public static void bindBlobDoubleArray(@NonNull final SQLiteStatement stat,
		final int index, @NonNull final double[] array, final int offset, final int num) {

		stat.bindBlob(index, doubleArrayToByteArray(array, offset, num));
	}
	
	/**
	 * SQLiteStatement#bindBlobのヘルパーメソッド。intの配列をbyteの配列に変換して割り付ける
	 *
	 * @param stat
	 * @param index
	 * @param array
	 */
	public static void bindBlobIntArray(@NonNull final SQLiteStatement stat,
		final int index, @NonNull final int[] array) {

		stat.bindBlob(index, intArrayToByteArray(array, 0, array.length));
	}
	
	/**
	 * SQLiteStatement#bindBlobのヘルパーメソッド。intの配列をbyteの配列に変換して割り付ける
	 *
	 * @param stat
	 * @param index
	 * @param array
	 */
	public static void bindBlobIntArray(@NonNull final SQLiteStatement stat,
		final int index, @NonNull final int[] array, final int offset, final int num) {

		stat.bindBlob(index, intArrayToByteArray(array, offset, num));
	}
	
	/**
	 * SQLiteStatement#bindBlobのヘルパーメソッド。shortの配列をbyteの配列に変換して割り付ける
	 *
	 * @param stat
	 * @param index
	 * @param array
	 */
	public static void bindBlobShortArray(@NonNull final SQLiteStatement stat,
		final int index, @NonNull final short[] array) {

		stat.bindBlob(index, shortArrayToByteArray(array, 0, array.length));
	}
	
	/**
	 * SQLiteStatement#bindBlobのヘルパーメソッド。shortの配列をbyteの配列に変換して割り付ける
	 *
	 * @param stat
	 * @param index
	 * @param array
	 * @param offset
	 * @param num
	 */
	public static void bindBlobShortArray(@NonNull final SQLiteStatement stat,
		final int index, @NonNull final short[] array, final int offset, final int num) {

		stat.bindBlob(index, shortArrayToByteArray(array, offset, num));
	}
	
	/**
	 * SQLiteStatement#bindBlobのヘルパーメソッド。longの配列をbyteの配列に変換して割り付ける
	 *
	 * @param stat
	 * @param index
	 * @param array
	 */
	public static void bindBlobLongArray(@NonNull final SQLiteStatement stat,
		final int index, @NonNull final long[] array) {

		stat.bindBlob(index, longArrayToByteArray(array, 0, array.length));
	}
	
	/**
	 * SQLiteStatement#bindBlobのヘルパーメソッド。longの配列をbyteの配列に変換して割り付ける
	 *
	 * @param stat
	 * @param index
	 * @param array
	 * @param offset
	 * @param num
	 */
	public static void bindBlobLongArray(@NonNull final SQLiteStatement stat,
		final int index, final long[] array, final int offset, final int num) {

		stat.bindBlob(index, longArrayToByteArray(array, offset, num));
	}
	
	/**
	 * SQLiteStatement#bindBlobのヘルパーメソッド。Bitmapをbyteの配列に変換して割り付ける
	 *
	 * @param stat
	 * @param index
	 * @param bitmap
	 */
	public static void bindBlobBitmap(@NonNull final SQLiteStatement stat,
		final int index, @NonNull final Bitmap bitmap) {

		stat.bindBlob(index, BitmapHelper.BitmapToByteArray(bitmap));
	}
	
	/**
	 * Cursor#getBlobのヘルパーメソッド。getBlobの返り値をfloatの配列として変換して返す
	 *
	 * @param cursor
	 * @param columnIndex
	 * @return float[]
	 */
	public static float[] getBlobFloatArray(@NonNull final Cursor cursor,
		final int columnIndex) {

		return byteArrayToFloatArray(cursor.getBlob(columnIndex));
	}
	
	/**
	 * Cursor#getBlobのヘルパーメソッド。getBlobの返り値をfloatの配列として変換して返す
	 *
	 * @param cursor
	 * @param columnName
	 * @param defaultValue
	 * @return float[]
	 */
	@Nullable
	public static float[] getBlobFloatArray(@NonNull final Cursor cursor,
		final String columnName, @Nullable final float[] defaultValue) {

		float[] result = byteArrayToFloatArray(getBlob(cursor, columnName, null));
		if (result == null) result = defaultValue;
		return result;
	}
	
	/**
	 * Cursor#getBlobのヘルパーメソッド。getBlobの返り値をdoubleの配列として変換して返す
	 *
	 * @param cursor
	 * @param columnIndex
	 * @return double[]
	 */
	public static double[] getBlobDoubleArray(@NonNull final Cursor cursor,
		final int columnIndex) {

		return byteArrayToDoubleArray(cursor.getBlob(columnIndex));
	}
	
	/**
	 * Cursor#getBlobのヘルパーメソッド。getBlobの返り値をdoubleの配列として変換して返す
	 *
	 * @param cursor
	 * @param columnName
	 * @param defaultValue
	 * @return double[]
	 */
	@Nullable
	public static double[] getBlobDoubleArray(@NonNull final Cursor cursor,
		final String columnName, @Nullable final double[] defaultValue) {

		double[] result = byteArrayToDoubleArray(getBlob(cursor, columnName, null));
		if (result == null) result = defaultValue;
		return result;
	}
	
	/**
	 * Cursor#getBlobのヘルパーメソッド。getBlobの返り値をbyteの配列として変換して返す
	 *
	 * @param cursor
	 * @param columnName
	 * @param defaultValue
	 * @return byte[]
	 */
	@Nullable
	public static byte[] getBlob(@NonNull final Cursor cursor,
		final String columnName, @Nullable final byte[] defaultValue) {

		byte[] result = defaultValue;
		try {
			result = cursor.getBlob(cursor.getColumnIndexOrThrow(columnName));
		} catch (final Exception e) {
//			if (DEBUG) Log.w(TAG, e);
		}
		return result;
	}
	
	/**
	 * Cursor#getBlobのヘルパーメソッド。getBlobの返り値をintの配列として変換して返す
	 *
	 * @param cursor
	 * @param columnIndex
	 * @return int[]
	 */
	public static int[] getBlobIntArray(@NonNull final Cursor cursor,
		final int columnIndex) {

		return byteArrayToIntArray(cursor.getBlob(columnIndex));
	}
	
	/**
	 * Cursor#getBlobのヘルパーメソッド。getBlobの返り値をintの配列として変換して返す
	 *
	 * @param cursor
	 * @param columnName
	 * @param defaultValue
	 * @return int[]
	 */
	@Nullable
	public static int[] getBlobIntArray(@NonNull final Cursor cursor,
		final String columnName, @Nullable final int[] defaultValue) {

		int[] result = byteArrayToIntArray(getBlob(cursor, columnName, null));
		if (result == null) result = defaultValue;
		return result;
	}
	
	/**
	 * Cursor#getBlobのヘルパーメソッド。getBlobの返り値をshortの配列として変換して返す
	 *
	 * @param cursor
	 * @param columnIndex
	 * @return short[]
	 */
	public static short[] getBlobShortArray(@NonNull final Cursor cursor,
		final int columnIndex) {

		return byteArrayToShortArray(cursor.getBlob(columnIndex));
	}
	
	/**
	 * Cursor#getBlobのヘルパーメソッド。getBlobの返り値をshortの配列として変換して返す
	 *
	 * @param cursor
	 * @param columnName
	 * @param defaultValue
	 * @return short[]
	 */
	@Nullable
	public static short[] getBlobShortArray(@NonNull final Cursor cursor,
		final String columnName, @Nullable final short[] defaultValue) {

		short[] result = byteArrayToShortArray(getBlob(cursor, columnName, null));
		if (result == null) result = defaultValue;
		return result;
	}
	
	/**
	 * Cursor#getBlobのヘルパーメソッド。getBlobの返り値をlongの配列として変換して返す
	 *
	 * @param cursor
	 * @param columnIndex
	 * @return long[]
	 */
	public static long[] getBlobLongArray(@NonNull final Cursor cursor,
		final int columnIndex) {

		return byteArrayToLongArray(cursor.getBlob(columnIndex));
	}
	
	/**
	 * Cursor#getBlobのヘルパーメソッド。getBlobの返り値をlongの配列として変換して返す
	 *
	 * @param cursor
	 * @param columnName
	 * @param defaultValue
	 * @return long[]
	 */
	@Nullable
	public static long[] getBlobLongArray(@NonNull final Cursor cursor,
		final String columnName, @Nullable final long[] defaultValue) {

		long[] result = byteArrayToLongArray(getBlob(cursor, columnName, null));
		if (result == null) result = defaultValue;
		return result;
	}
	
	/**
	 * Cursor#getBlobのヘルパーメソッド。getBlobの返り値をBitmapとして変換して返す
	 *
	 * @param cursor
	 * @param columnIndex
	 * @return Bitmap
	 */
	public static Bitmap getBlobBitmap(@NonNull final Cursor cursor,
		final int columnIndex) {

		return BitmapHelper.asBitmap(cursor.getBlob(columnIndex));
	}
	
	/**
	 * Cursor#getBlobのヘルパーメソッド。getBlobの返り値をBitmapとして変換して返す
	 *
	 * @param cursor
	 * @param columnName
	 * @return Bitmap
	 */
	public static Bitmap getBlobBitmap(@NonNull final Cursor cursor,
		final String columnName) {

		return BitmapHelper.asBitmap(getBlob(cursor, columnName, null));
	}
	
	/**
	 * Cursor#getBlobのヘルパーメソッド。getBlobの返り値を指定した大きさに最も近いBitmapに変換して返す
	 *
	 * @param cursor
	 * @param columnIndex
	 * @param requestWidth
	 * @param requestHeight
	 * @return Bitmap
	 */
	@Nullable
	public static Bitmap getBlobBitmap(@NonNull final Cursor cursor,
		final int columnIndex, final int requestWidth, final int requestHeight) {

		return BitmapHelper.asBitmap(cursor.getBlob(columnIndex), requestWidth, requestHeight);
	}
	
	/**
	 * Cursor#getBlobのヘルパーメソッド。getBlobの返り値を指定した大きさに最も近いBitmapに変換して返す
	 *
	 * @param cursor
	 * @param columnName
	 * @param requestWidth
	 * @param requestHeight
	 * @return Bitmap
	 */
	@Nullable
	public static Bitmap getBlobBitmap(@NonNull final Cursor cursor,
		final String columnName, final int requestWidth, final int requestHeight) {

		return BitmapHelper.asBitmap(getBlob(cursor, columnName, null), requestWidth, requestHeight);
	}
	
	/**
	 * Cursor#getBlobのヘルパーメソッド。getBlobの返り値を指定した大きさのBitmapに変換して返す
	 *
	 * @param cursor
	 * @param columnIndex
	 * @param requestWidth
	 * @param requestHeight
	 * @return Bitmap
	 */
	@Nullable
	public static Bitmap getBlobBitmapStrictSize(@NonNull final Cursor cursor,
		final int columnIndex, final int requestWidth, final int requestHeight) {

		return BitmapHelper.asBitmapStrictSize(
			cursor.getBlob(columnIndex), requestWidth, requestHeight);
	}
	
	/**
	 * Cursor#getBlobのヘルパーメソッド。getBlobの返り値を指定した大きさのBitmapに変換して返す
	 *
	 * @param cursor
	 * @param columnName
	 * @param requestWidth
	 * @param requestHeight
	 * @return Bitmap
	 */
	@Nullable
	public static Bitmap getBlobBitmapStrictSize(@NonNull final Cursor cursor,
		final String columnName, final int requestWidth, final int requestHeight) {

		return BitmapHelper.asBitmapStrictSize(
			getBlob(cursor, columnName, null), requestWidth, requestHeight);
	}
}
