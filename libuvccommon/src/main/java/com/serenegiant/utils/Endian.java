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
public class Endian {
	public static boolean be2boolean(final byte[] bytes, final int offset) {
		return bytes[offset] != 0;
	}

	public static boolean le2boolean(final byte[] bytes, final int offset) {
		return bytes[offset] != 0;
	}
	
	public static char be2char(final byte[] bytes, final int offset) {
		return (char) ((bytes[offset + 1] & 0xFF) +
			(bytes[offset] << 8));
	}

	public static char le2char(final byte[] bytes, final int offset) {
		return (char) ((bytes[offset] & 0xFF) +
			(bytes[offset + 1] << 8));
	}
	
	public static short be2short(final byte[] bytes, final int offset) {
		return (short) ((bytes[offset + 1] & 0xFF) +
			(bytes[offset] << 8));
	}

	public static short le2short(final byte[] bytes, final int offset) {
		return (short) ((bytes[offset] & 0xFF) +
			(bytes[offset + 1] << 8));
	}
	
	public static int be2int(final byte[] bytes, final int offset) {
		return ((bytes[offset + 3] & 0xFF)) +
			((bytes[offset + 2] & 0xFF) << 8) +
			((bytes[offset + 1] & 0xFF) << 16) +
			((bytes[offset]) << 24);
	}

	public static int le2int(final byte[] bytes, final int offset) {
		return ((bytes[offset] & 0xFF)) +
			((bytes[offset + 1] & 0xFF) << 8) +
			((bytes[offset + 2] & 0xFF) << 16) +
			((bytes[offset + 3]) << 24);
	}
	
	public static float be2float(final byte[] bytes, final int offset) {
		return Float.intBitsToFloat(be2int(bytes, offset));
	}

	public static float le2float(final byte[] bytes, final int offset) {
		return Float.intBitsToFloat(le2int(bytes, offset));
	}
	
	public static long be2long(final byte[] bytes, final int offset) {
		return ((bytes[offset + 7] & 0xFFL)) +
			((bytes[offset + 6] & 0xFFL) << 8) +
			((bytes[offset + 5] & 0xFFL) << 16) +
			((bytes[offset + 4] & 0xFFL) << 24) +
			((bytes[offset + 3] & 0xFFL) << 32) +
			((bytes[offset + 2] & 0xFFL) << 40) +
			((bytes[offset + 1] & 0xFFL) << 48) +
			(((long) bytes[offset]) << 56);
	}

	public static long le2long(final byte[] bytes, final int offset) {
		return ((bytes[offset] & 0xFFL)) +
			((bytes[offset + 1] & 0xFFL) << 8) +
			((bytes[offset + 2] & 0xFFL) << 16) +
			((bytes[offset + 3] & 0xFFL) << 24) +
			((bytes[offset + 4] & 0xFFL) << 32) +
			((bytes[offset + 5] & 0xFFL) << 40) +
			((bytes[offset + 6] & 0xFFL) << 48) +
			(((long) bytes[offset + 7]) << 56);
	}
	
	public static double be2double(final byte[] bytes, final int offset) {
		return Double.longBitsToDouble(be2long(bytes, offset));
	}

	public static double le2double(final byte[] bytes, final int offset) {
		return Double.longBitsToDouble(le2long(bytes, offset));
	}

	public static void bool2be(final byte[] bytes, final int offset, final boolean value) {
		bytes[offset] = (byte) (value ? 1 : 0);
	}

	public static void bool2le(final byte[] bytes, final int offset, final boolean value) {
		bytes[offset] = (byte) (value ? 1 : 0);
	}
	
	public static void char2be(final byte[] bytes, final int offset, final char value) {
		bytes[offset + 1] = (byte) (value);
		bytes[offset] = (byte) (value >>> 8);
	}

	public static void char2le(final byte[] bytes, final int offset, final char value) {
		bytes[offset] = (byte) (value);
		bytes[offset + 1] = (byte) (value >>> 8);
	}
	
	public static void short2be(final byte[] b, final int offset, final short value) {
		b[offset + 1] = (byte) (value);
		b[offset] = (byte) (value >>> 8);
	}

	public static void short2le(final byte[] b, final int offset, final short value) {
		b[offset] = (byte) (value);
		b[offset + 1] = (byte) (value >>> 8);
	}
	
	public static void int2be(final byte[] bytes, final int offset, final int value) {
		bytes[offset + 3] = (byte) (value);
		bytes[offset + 2] = (byte) (value >>> 8);
		bytes[offset + 1] = (byte) (value >>> 16);
		bytes[offset] = (byte) (value >>> 24);
	}

	public static void int2le(final byte[] bytes, final int offset, final int value) {
		bytes[offset] = (byte) (value);
		bytes[offset + 1] = (byte) (value >>> 8);
		bytes[offset + 2] = (byte) (value >>> 16);
		bytes[offset + 3] = (byte) (value >>> 24);
	}
	
	public static void float2be(final byte[] bytes, final int offset, final float value) {
		int2be(bytes, offset, Float.floatToIntBits(value));
	}

	public static void float2le(final byte[] bytes, final int offset, final float value) {
		int2le(bytes, offset, Float.floatToIntBits(value));
	}
	
	public static void long2be(final byte[] bytes, final int offset, long val) {
		bytes[offset + 7] = (byte) (val);
		bytes[offset + 6] = (byte) (val >>> 8);
		bytes[offset + 5] = (byte) (val >>> 16);
		bytes[offset + 4] = (byte) (val >>> 24);
		bytes[offset + 3] = (byte) (val >>> 32);
		bytes[offset + 2] = (byte) (val >>> 40);
		bytes[offset + 1] = (byte) (val >>> 48);
		bytes[offset] = (byte) (val >>> 56);
	}

	public static void long2le(final byte[] bytes, final int offset, long val) {
		bytes[offset] = (byte) (val);
		bytes[offset + 1] = (byte) (val >>> 8);
		bytes[offset + 2] = (byte) (val >>> 16);
		bytes[offset + 3] = (byte) (val >>> 24);
		bytes[offset + 4] = (byte) (val >>> 32);
		bytes[offset + 5] = (byte) (val >>> 40);
		bytes[offset + 6] = (byte) (val >>> 48);
		bytes[offset + 7] = (byte) (val >>> 56);
	}
	
	public static void double2be(final byte[] bytes, final int offset, final double value) {
		long2be(bytes, offset, Double.doubleToLongBits(value));
	}

	public static void double2le(final byte[] bytes, final int offset, final double value) {
		long2le(bytes, offset, Double.doubleToLongBits(value));
	}
}
