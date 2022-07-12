package com.serenegiant.io;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.ByteChannel;
import java.nio.charset.Charset;

/**
 * 全部ネットワークバイトオーダー = ビッグエンディアンやからな
 */
public class ChannelHelper {
	private static final Charset UTF8 = Charset.forName("UTF-8");
	
	/**
	 * ByteChannelからbooleanを読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static boolean readBoolean(@NonNull final ByteChannel channel)
		throws IOException {

		return readBoolean(channel, null);
	}

	/**
	 * ByteChannelからbooleanを読み込む
	 * @param channel
	 * @param work
	 * @return
	 * @throws IOException
	 */
	public static boolean readBoolean(
		@NonNull final ByteChannel channel,
		@Nullable final ByteBuffer work) throws IOException {
		
		final ByteBuffer buf = checkBuffer(work, 1);
		final int readBytes = channel.read(buf);
		if (readBytes != 1) throw new IOException();
		buf.clear();
		return buf.get() != 0;
	}
	
	/**
	 * ByteChannelからbyeを読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static byte readByte(@NonNull final ByteChannel channel)
		throws IOException {
		
		return readByte(channel, null);
	}
	
	/**
	 * ByteChannelからbyeを読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static byte readByte(@NonNull final ByteChannel channel,
		@Nullable final ByteBuffer work) throws IOException {
		
		final ByteBuffer buf = checkBuffer(work, 1);
		final int readBytes = channel.read(buf);
		if (readBytes != 1) throw new IOException();
		buf.clear();
		return buf.get();
	}

	/**
	 * ByteChannelからcharを読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static char readChar(@NonNull final ByteChannel channel)
		throws IOException {
		
		return readChar(channel, null);
	}
	
	/**
	 * ByteChannelからcharを読み込む
	 * @param channel
	 * @param work
	 * @return
	 * @throws IOException
	 */
	public static char readChar(@NonNull final ByteChannel channel,
		@Nullable final ByteBuffer work) throws IOException {
		
		final ByteBuffer buf = checkBuffer(work, 2);
		final int readBytes = channel.read(buf);
		if (readBytes != 2) throw new IOException();
		buf.clear();
		return buf.getChar();
	}

	/**
	 * ByteChannelからshortを読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static short readShort(@NonNull final ByteChannel channel)
		throws IOException {
		
		return readShort(channel, null);
	}
	
	/**
	 * ByteChannelからshortを読み込む
	 * @param channel
	 * @param work
	 * @return
	 * @throws IOException
	 */
	public static short readShort(@NonNull final ByteChannel channel,
		@Nullable final ByteBuffer work) throws IOException {
		
		final ByteBuffer buf = checkBuffer(work, 2);
		final int readBytes = channel.read(buf);
		if (readBytes != 2) throw new IOException();
		buf.clear();
		return buf.getShort();
	}

	/**
	 * ByteChannelからintを読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static int readInt(@NonNull final ByteChannel channel)
		throws IOException {
		
		return readInt(channel, null);
	}
	
	/**
	 * ByteChannelからintを読み込む
	 * @param channel
	 * @param work
	 * @return
	 * @throws IOException
	 */
	public static int readInt(@NonNull final ByteChannel channel,
		@Nullable final ByteBuffer work) throws IOException {
		
		final ByteBuffer buf = checkBuffer(work, 4);
		final int readBytes = channel.read(buf);
		if (readBytes != 4) throw new IOException();
		buf.clear();
		return buf.getInt();
	}

	/**
	 * ByteChannelからlongを読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static long readLong(@NonNull final ByteChannel channel)
		throws IOException {
		
		return readLong(channel, null);
	}
	
	/**
	 * ByteChannelからlongを読み込む
	 * @param channel
	 * @param work
	 * @return
	 * @throws IOException
	 */
	public static long readLong(@NonNull final ByteChannel channel,
		@Nullable final ByteBuffer work) throws IOException {
		
		final ByteBuffer buf = checkBuffer(work, 8);
		final int readBytes = channel.read(buf);
		if (readBytes != 8) throw new IOException();
		buf.clear();
		return buf.getLong();
	}

	/**
	 * ByteChannelからfloatを読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static float readFloat(@NonNull final ByteChannel channel)
		throws IOException {
		
		final ByteBuffer buf = ByteBuffer.allocate(4);
		final int readBytes = channel.read(buf);
		if (readBytes != 4) throw new IOException();
		buf.clear();
		return buf.getFloat();
	}
	
	/**
	 * ByteChannelからfloatを読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static float readFloat(@NonNull final ByteChannel channel,
		@Nullable final ByteBuffer work) throws IOException {
		
		final ByteBuffer buf = checkBuffer(work, 4);
		final int readBytes = channel.read(buf);
		if (readBytes != 4) throw new IOException();
		buf.clear();
		return buf.getFloat();
	}

	/**
	 * ByteChannelからdoubleを読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static double readDouble(@NonNull final ByteChannel channel)
		throws IOException {
		
		return readDouble(channel, null);
	}

	/**
	 * ByteChannelからdoubleを読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static double readDouble(@NonNull final ByteChannel channel,
		@Nullable final ByteBuffer work) throws IOException {

		final ByteBuffer buf = checkBuffer(work, 8);
		final int readBytes = channel.read(buf);
		if (readBytes != 8) throw new IOException();
		buf.clear();
		return buf.getDouble();
	}
	
	/**
	 * ByteChannelからStringを読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static String readString(@NonNull final ByteChannel channel)
		throws IOException {
		
		final int bytes = readInt(channel);
		final byte[] buf = new byte[bytes];
		final ByteBuffer b = ByteBuffer.wrap(buf);
		final int readBytes = channel.read(b);
		if (readBytes != bytes) throw new IOException();
		return new String(buf, UTF8);
	}
	
	/**
	 * ByteChannelからboolean配列を読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static boolean[] readBooleanArray(@NonNull final ByteChannel channel)
		throws IOException {
		
		final int n = readInt(channel);
		final ByteBuffer buf = ByteBuffer.allocate(n);
		final int readBytes = channel.read(buf);
		if (readBytes != n) throw new IOException();
		buf.clear();
		final boolean[] result = new boolean[n];
		for (int i = 0; i < n; i++) {
			result[i] = buf.get() != 0;
		}
		return result;
	}
	
	/**
	 * ByteChannelからbyte配列を読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static byte[] readByteArray(@NonNull final ByteChannel channel)
		throws IOException {
		
		final int n = readInt(channel);
		final byte[] result = new byte[n];
		final ByteBuffer buf = ByteBuffer.wrap(result);
		final int readBytes = channel.read(buf);
		if (readBytes != n) throw new IOException();
		return result;
	}
	
	/**
	 * ByteChannelからchar配列を読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static char[] readCharArray(@NonNull final ByteChannel channel)
		throws IOException {
		
		final int n = readInt(channel);
		final ByteBuffer buf = ByteBuffer.allocate(n * 2).order(ByteOrder.BIG_ENDIAN);
		final int readBytes = channel.read(buf);
		if (readBytes != n * 2) throw new IOException();
		buf.clear();
		final CharBuffer result = buf.asCharBuffer();
		if (result.hasArray()) {
			return result.array();
		}  else {
			final char[] b = new char[n];
			result.get(b);
			return b;
		}
	}
	
	/**
	 * ByteChannelからshort配列を読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static short[] readShortArray(@NonNull final ByteChannel channel)
		throws IOException {
		
		final int n = readInt(channel);
		final ByteBuffer buf = ByteBuffer.allocate(n * 2).order(ByteOrder.BIG_ENDIAN);
		final int readBytes = channel.read(buf);
		if (readBytes != n * 2) throw new IOException();
		buf.clear();
		final ShortBuffer result = buf.asShortBuffer();
		if (result.hasArray()) {
			return result.array();
		}  else {
			final short[] b = new short[n];
			result.get(b);
			return b;
		}
	}
	
	/**
	 * ByteChannelからint配列を読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static int[] readIntArray(@NonNull final ByteChannel channel)
		throws IOException {
		
		final int n = readInt(channel);
		final ByteBuffer buf = ByteBuffer.allocate(n * 4).order(ByteOrder.BIG_ENDIAN);
		final int readBytes = channel.read(buf);
		if (readBytes != n * 4) throw new IOException();
		buf.clear();
		final IntBuffer result = buf.asIntBuffer();
		if (result.hasArray()) {
			return result.array();
		}  else {
			final int[] b = new int[n];
			result.get(b);
			return b;
		}
	}
	
	/**
	 * ByteChannelからlong配列を読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static long[] readLongArray(@NonNull final ByteChannel channel)
		throws IOException {
		
		final int n = readInt(channel);
		final ByteBuffer buf = ByteBuffer.allocate(n * 8).order(ByteOrder.BIG_ENDIAN);
		final int readBytes = channel.read(buf);
		if (readBytes != n * 8) throw new IOException();
		buf.clear();
		final LongBuffer result = buf.asLongBuffer();
		if (result.hasArray()) {
			return result.array();
		}  else {
			final long[] b = new long[n];
			result.get(b);
			return b;
		}
	}
	
	/**
	 * ByteChannelからfloat配列を読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static float[] readFloatArray(@NonNull final ByteChannel channel)
		throws IOException {
		
		final int n = readInt(channel);
		final ByteBuffer buf = ByteBuffer.allocate(n * 4).order(ByteOrder.BIG_ENDIAN);
		final int readBytes = channel.read(buf);
		if (readBytes != n * 4) throw new IOException();
		buf.clear();
		final FloatBuffer result = buf.asFloatBuffer();
		if (result.hasArray()) {
			return result.array();
		}  else {
			final float[] b = new float[n];
			result.get(b);
			return b;
		}
	}
	
	/**
	 * ByteChannelからdouble配列を読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static double[] readDoubleArray(@NonNull final ByteChannel channel)
		throws IOException {
		
		final int n = readInt(channel);
		final ByteBuffer buf = ByteBuffer.allocate(n * 8).order(ByteOrder.BIG_ENDIAN);
		final int readBytes = channel.read(buf);
		if (readBytes != n * 8) throw new IOException();
		buf.clear();
		final DoubleBuffer result = buf.asDoubleBuffer();
		if (result.hasArray()) {
			return result.array();
		}  else {
			final double[] b = new double[n];
			result.get(b);
			return b;
		}
	}
	
	/**
	 * ByteChannelからByteBufferを読み込む
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static ByteBuffer readByteBuffer(@NonNull final ByteChannel channel)
		throws IOException {
		
		return readByteBuffer(channel, null, true);
	}
	
	/**
	 * ByteChannelからByteBufferを読み込む
	 * 指定したbufがnullまたは読み込むサイズよりも小さい場合は
	 * ダミーリード後IOExceptionを投げる
	 * @param channel
	 * @param buf
	 * @return
	 * @throws IOException
	 */
	public static ByteBuffer readByteBuffer(@NonNull final ByteChannel channel,
		@NonNull final ByteBuffer buf) throws IOException {
		
		return readByteBuffer(channel, buf, false);
	}

	/**
	 * ByteChannelからByteBufferを読み込む
	 * @param channel
	 * @param readBuf
	 * @param canReAllocate trueなら　指定したreadBufのremainingが読み込むサイズよりも小さい場合は
	 * 						IOExceptionを投げる、falseなら必要な分を確保し直す
	 * @return 読み込めた時はpositionは元のまま(読んだデータの先頭), limitをデータの最後に変更して返す
	 * canReAllocate=falseなら元のreadBufとは異なるByteBufferを返すかもしれない
	 * IOExceptionを投げた時は内容・position,limitは不定
	 * @throws IOException
	 */
	public static ByteBuffer readByteBuffer(@NonNull final ByteChannel channel,
		@Nullable final ByteBuffer readBuf, final boolean canReAllocate)
			throws IOException {
		
		final int n = readInt(channel);
		final int pos = readBuf != null ? readBuf.position() : 0;
		ByteBuffer buf = readBuf;
		if ((buf == null) || (buf.remaining() < n)) {
			if (canReAllocate) {
				if (buf == null) {
					buf = ByteBuffer.allocateDirect(n);
				} else {
					final ByteBuffer temp = ByteBuffer.allocateDirect(
						readBuf.limit() + n);
					buf.flip();
					temp.put(buf);
					buf = temp;
				}
			} else {
				throw new IOException();
			}
		}
		buf.limit(pos + n);
		final int readBytes = channel.read(buf);
		if (readBytes != n) throw new IOException();
		buf.position(pos);
		buf.limit(pos + n);
		return buf;
	}
	
	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		final boolean value) throws IOException {
		
		write(channel, value, null);
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @param work
	 * @throws IOException
	 */
	public static  void write(@NonNull final ByteChannel channel,
		final boolean value,
		@Nullable final ByteBuffer work) throws IOException {
		
		final ByteBuffer buf = checkBuffer(work, 1);
		buf.put((byte)(value ? 1 : 0));
		buf.flip();
		channel.write(buf);
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		final byte value) throws IOException {
		
		write(channel, value, null);
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @param work
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		final byte value,
		@Nullable final ByteBuffer work) throws IOException {
		
		final ByteBuffer buf = checkBuffer(work, 1);
		buf.put(value);
		buf.flip();
		channel.write(buf);
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		final char value) throws IOException {
		
		write(channel, value, null);
	}
	
	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @param work
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		final char value,
		@Nullable final ByteBuffer work) throws IOException {
		
		final ByteBuffer buf = checkBuffer(work, 2);
		buf.putChar(value);
		buf.flip();
		channel.write(buf);
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		final short value) throws IOException {
		
		write(channel, value, null);
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @param work
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		final short value,
		@Nullable final ByteBuffer work) throws IOException {
		
		final ByteBuffer buf = checkBuffer(work, 2);
		buf.putShort(value);
		buf.flip();
		channel.write(buf);
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		final int value) throws IOException {
		
		write(channel, value, null);
	}
	
	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @param work
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		final int value,
		@Nullable final ByteBuffer work) throws IOException {
		
		final ByteBuffer buf = checkBuffer(work, 4);
		buf.putInt(value);
		buf.flip();
		channel.write(buf);
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		final long value) throws IOException {
		
		write(channel, value, null);
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @param work
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		final long value,
		@Nullable final ByteBuffer work) throws IOException {
		
		final ByteBuffer buf = checkBuffer(work, 8);
		buf.putLong(value);
		buf.flip();
		channel.write(buf);
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		final float value) throws IOException {
		
		write(channel, value, null);
	}
	
	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @param work
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		final float value,
		@Nullable final ByteBuffer work) throws IOException {
		
		final ByteBuffer buf = checkBuffer(work, 4);
		buf.putFloat(value);
		buf.flip();
		channel.write(buf);
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		final double value) throws IOException {
		
		write(channel, value, null);
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @param work
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		final double value,
		@Nullable final ByteBuffer work) throws IOException {
		
		final ByteBuffer buf = checkBuffer(work, 8);
		buf.putDouble(value);
		buf.flip();
		channel.write(buf);
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		@NonNull final String value) throws IOException {
		
		final byte[] buf = value.getBytes(UTF8);
		write(channel, buf.length);
		channel.write(ByteBuffer.wrap(buf));
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		@NonNull final boolean[] value) throws IOException {
		
		write(channel, value, null);
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @param work
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		@NonNull final boolean[] value,
		@Nullable final ByteBuffer work) throws IOException {
		
		final int n = value.length;
		write(channel, n, work);
		final ByteBuffer buf = checkBuffer(work, n);
		for (int i = 0; i < n; i++) {
			buf.put((byte)(value[i] ? 1 : 0));
		}
		buf.flip();
		channel.write(buf);
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		@NonNull final byte[] value) throws IOException {
		
		write(channel, value.length);
		channel.write(ByteBuffer.wrap(value));
	}
	
	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		@NonNull final char[] value) throws IOException {
		
		write(channel, value, null);
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @param work
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		@NonNull final char[] value,
		@Nullable final ByteBuffer work) throws IOException {
		
		final int n = value.length;
		write(channel, n, work);
		final ByteBuffer buf = checkBuffer(work, n * 2);
		for (int i = 0; i < n; i++) {
			buf.putChar(value[i]);
		}
		buf.flip();
		channel.write(buf);
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		@NonNull final short[] value) throws IOException {
		
		write(channel, value, null);
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @param work
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		@NonNull final short[] value,
		@Nullable final ByteBuffer work) throws IOException {
		
		final int n = value.length;
		write(channel, n, work);
		final ByteBuffer buf = checkBuffer(work, n * 2);
		for (int i = 0; i < n; i++) {
			buf.putShort(value[i]);
		}
		buf.flip();
		channel.write(buf);
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		@NonNull final int[] value) throws IOException {
		
		write(channel, value, null);
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @param work
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		@NonNull final int[] value,
		@Nullable final ByteBuffer work) throws IOException {
		
		final int n = value.length;
		write(channel, n, work);
		final ByteBuffer buf = checkBuffer(work, n * 4);
		for (int i = 0; i < n; i++) {
			buf.putInt(value[i]);
		}
		buf.flip();
		channel.write(buf);
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		@NonNull final long[] value) throws IOException {
		
		write(channel, value, null);
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @param work
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		@NonNull final long[] value,
		@Nullable final ByteBuffer work) throws IOException {
		
		final int n = value.length;
		write(channel, n, work);
		final ByteBuffer buf = checkBuffer(work, n * 8);
		for (int i = 0; i < n; i++) {
			buf.putLong(value[i]);
		}
		buf.flip();
		channel.write(buf);
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		@NonNull final float[] value) throws IOException {
		
		write(channel, value, null);
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @param work
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		@NonNull final float[] value,
		@Nullable final ByteBuffer work) throws IOException {
		
		final int n = value.length;
		write(channel, n, work);
		final ByteBuffer buf = checkBuffer(work, n * 4);
		for (int i = 0; i < n; i++) {
			buf.putFloat(value[i]);
		}
		buf.flip();
		channel.write(buf);
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		@NonNull final double[] value) throws IOException {
		
		write(channel, value, null);
	}
	
	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @param work
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		@NonNull final double[] value,
		@Nullable final ByteBuffer work) throws IOException {
		
		final int n = value.length;
		write(channel, n, work);
		final ByteBuffer buf = checkBuffer(work, n * 8);
		for (int i = 0; i < n; i++) {
			buf.putDouble(value[i]);
		}
		buf.flip();
		channel.write(buf);
	}

	/**
	 * ByteChannelへ書き込む
	 * @param channel
	 * @param value
	 * @throws IOException
	 */
	public static void write(@NonNull final ByteChannel channel,
		@NonNull final ByteBuffer value) throws IOException {
		
		write(channel, value.remaining());
		channel.write(value);
	}
	
	/**
	 * 作業用のバッファーのサイズをチェックして足りなければ新規に確保して返す
	 * 作業用バッファーは必要サイズ分がremainingになるように
	 * clearしてlimitをセットして返す
	 * @param work
	 * @param sz
	 * @return
	 */
	private static ByteBuffer checkBuffer(
		@Nullable final ByteBuffer work, final int sz) {

		ByteBuffer buf = work;
		if ((buf == null) || (work.capacity() < sz)) {
			buf = ByteBuffer.allocateDirect(sz);
		}
		buf.order(ByteOrder.BIG_ENDIAN);
		buf.clear();
		buf.limit(sz);
		return buf;
	}
}
