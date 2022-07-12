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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by saki on 2018/03/07.
 *
 */

public class SysFs {
	private static final boolean DEBUG = false;	// XXX set false on production
	private static final String TAG = SysFs.class.getSimpleName();
	
	private static final Map<String, WeakReference<ReentrantReadWriteLock>> sSysFs
		= new HashMap<String, WeakReference<ReentrantReadWriteLock>>();

	private final String mPath;
	private final String mName;
	protected final ReentrantReadWriteLock mLock;
	private final Lock mReadLock;
	private final Lock mWriteLock;

	public SysFs(@NonNull final String path) throws IOException {
		final File f = new File(path);
		if (!f.exists() || !f.canRead()) {
			throw new IOException(path + " does not exist or can't read.");
		}
		mPath = path;
		mName = f.getName();
		ReentrantReadWriteLock lock = null;
		synchronized (sSysFs) {
			if (sSysFs.containsKey(path)) {
				final WeakReference<ReentrantReadWriteLock> weakLock
					= sSysFs.get(path);
				lock = weakLock != null ? weakLock.get() : null;
			}
			if (lock == null) {
				lock = new ReentrantReadWriteLock();
				sSysFs.put(path, new WeakReference<ReentrantReadWriteLock>(lock));
			}
		}
		mLock = lock;
		mReadLock = lock.readLock();
		mWriteLock = lock.writeLock();
	}
	
	@Override
	protected void finalize() throws Throwable {
		try {
			release();
		} finally {
			super.finalize();
		}
	}
	
	public void release() {
	}
	
	public String getPath() {
		return mPath;
	}
	
	public String getName() {
		return mName;
	}

	public String readString(@Nullable final String name) throws IOException {
		String result;
		mReadLock.lock();
		try {
			final FileReader in = new FileReader(getPath(name));
			try {
				result = new BufferedReader(in).readLine();
			} finally {
				in.close();
			}
		} finally {
			mReadLock.unlock();
		}
		return result;
	}

	public String readString() throws IOException {
		return readString(null);
	}

	public byte[] readBytes(@Nullable final String name) throws IOException {
		byte[] result;
		mReadLock.lock();
		try {
			final byte[] buf = new byte[512];
			final MyByteArrayOutputStream out = new MyByteArrayOutputStream(1024);
			final InputStream in = new BufferedInputStream(new FileInputStream(getPath(name)));
			try {
				int available = in.available();
				for (; available > 0; ) {
					final int bytes = in.read(buf);
					if (bytes > 0) {
						out.write(buf, 0, bytes);
					}
					available = in.available();
				}
				result = out.toByteArray();
			} finally {
				in.close();
			}
		} finally {
			mReadLock.unlock();
		}
		return result;
	}

	public byte[] readBytes() throws IOException {
		return readBytes(null);
	}
	
	public byte readByte(@Nullable final String name) throws IOException {
		mReadLock.lock();
		try {
			final DataInputStream in = new DataInputStream(new FileInputStream(getPath(name)));
			try {
				return in.readByte();
			} finally {
				in.close();
			}
		} finally {
			mReadLock.unlock();
		}
	}

	public byte readByte() throws IOException {
		return readByte(null);
	}
	
	public short readShort(@Nullable final String name) throws IOException {
		mReadLock.lock();
		try {
			final DataInputStream in = new DataInputStream(new FileInputStream(getPath(name)));
			try {
				return in.readShort();
			} finally {
				in.close();
			}
		} finally {
			mReadLock.unlock();
		}
	}

	public short readShort() throws IOException {
		return readShort(null);
	}
	
	public int readInt(@Nullable final String name) throws IOException {
		mReadLock.lock();
		try {
			final DataInputStream in = new DataInputStream(new FileInputStream(getPath(name)));
			try {
				return in.readInt();
			} finally {
				in.close();
			}
		} finally {
			mReadLock.unlock();
		}
	}

	public int readInt() throws IOException {
		return readInt(null);
	}
	
	public long readLong(@Nullable final String name) throws IOException {
		mReadLock.lock();
		try {
			final DataInputStream in = new DataInputStream(new FileInputStream(getPath(name)));
			try {
				return in.readLong();
			} finally {
				in.close();
			}
		} finally {
			mReadLock.unlock();
		}
	}

	public long readLong() throws IOException {
		return readLong(null);
	}
	
	public float readFloat(@Nullable final String name) throws IOException {
		mReadLock.lock();
		try {
			final DataInputStream in = new DataInputStream(new FileInputStream(getPath(name)));
			try {
				return in.readFloat();
			} finally {
				in.close();
			}
		} finally {
			mReadLock.unlock();
		}
	}

	public float readFloat() throws IOException {
		return readFloat(null);
	}
	
	public double readDouble(@Nullable final String name) throws IOException {
		mReadLock.lock();
		try {
			final DataInputStream in = new DataInputStream(new FileInputStream(getPath(name)));
			try {
				return in.readDouble();
			} finally {
				in.close();
			}
		} finally {
			mReadLock.unlock();
		}
	}

	public double readDouble() throws IOException {
		return readDouble(null);
	}
	
	public void write(@Nullable final String name, @NonNull final byte[] value)
		throws IOException {

		write(name, value, 0, value.length);
	}

	public void write(@NonNull final byte[] value) throws IOException {
		write(null, value, 0, value.length);
	}
	
	public void write(@Nullable final String name,
		@NonNull final byte[] value, final int offset, final int length)
			throws IOException {

		mWriteLock.lock();
		try {
			final OutputStream out = new FileOutputStream(getPath(name));
			try {
				out.write(value, offset, length);
				out.flush();
			} finally {
				out.close();
			}
		} finally {
			mWriteLock.unlock();
		}
	}

	public void write(@NonNull final byte[] value, final int offset, final int length)
		throws IOException {
		
		write(null, value, offset, length);
	}
	
	public void write(@Nullable final String name, @NonNull final String value)
		throws IOException {

		mWriteLock.lock();
		try {
			final FileWriter out = new FileWriter(getPath(name));
			try {
				out.write(value);
				out.flush();
			} finally {
				out.close();
			}
		} finally {
			mWriteLock.unlock();
		}
	}
	
	public void write(@NonNull final String value) throws IOException {
		write(null, value);
	}
	
	public void write(@Nullable final String name, final boolean value) throws IOException {
		mWriteLock.lock();
		try {
			final DataOutputStream out = new DataOutputStream(new FileOutputStream(getPath(name)));
			try {
				out.writeBoolean(value);
				out.flush();
			} finally {
				out.close();
			}
		} finally {
			mWriteLock.unlock();
		}
	}

	public void write(final boolean value) throws IOException {
		write(null, value);
	}
	
	public void write(@Nullable final String name,  final byte value) throws IOException {
		mWriteLock.lock();
		try {
			final DataOutputStream out = new DataOutputStream(new FileOutputStream(getPath(name)));
			try {
				out.writeByte(value);
				out.flush();
			} finally {
				out.close();
			}
		} finally {
			mWriteLock.unlock();
		}
	}
	
	public void write(final byte value) throws IOException {
		write(null, value);
	}
	
	public void write(@Nullable final String name,  final short value) throws IOException {
		mWriteLock.lock();
		try {
			final DataOutputStream out = new DataOutputStream(new FileOutputStream(getPath(name)));
			try {
				out.writeShort(value);
				out.flush();
			} finally {
				out.close();
			}
		} finally {
			mWriteLock.unlock();
		}
	}

	public void write(final short value) throws IOException {
		write(null, value);
	}
	
	public void write(@Nullable final String name, final int value) throws IOException {
		mWriteLock.lock();
		try {
			final DataOutputStream out = new DataOutputStream(new FileOutputStream(getPath(name)));
			try {
				out.writeInt(value);
				out.flush();
			} finally {
				out.close();
			}
		} finally {
			mWriteLock.unlock();
		}
	}

	public void write(final int value) throws IOException {
		write(null, value);
	}
	
	public void write(@Nullable final String name,  final float value) throws IOException {
		mWriteLock.lock();
		try {
			final DataOutputStream out = new DataOutputStream(new FileOutputStream(getPath(name)));
			try {
				out.writeFloat(value);
				out.flush();
			} finally {
				out.close();
			}
		} finally {
			mWriteLock.unlock();
		}
	}

	public void write(final float value) throws IOException {
		write(null, value);
	}
	
	public void write(@Nullable final String name, final double value) throws IOException {
		mWriteLock.lock();
		try {
			final DataOutputStream out = new DataOutputStream(new FileOutputStream(getPath(name)));
			try {
				out.writeDouble(value);
				out.flush();
			} finally {
				out.close();
			}
		} finally {
			mWriteLock.unlock();
		}
	}

	public void write(final double value) throws IOException {
		write(null, value);
	}

	@Override
	public String toString() {
		try {
			return String.format(Locale.US, "%s=%s", mPath, readString());
		} catch (IOException e) {
			return String.format(Locale.US, "%s=null", mPath);
		}
	}

	private File getPath(@Nullable final String name) {
		if (TextUtils.isEmpty(name)) {
			return new File(mPath);
		} else {
			File f = new File(mPath);
			return new File(f, name);
		}
	}
	
	private static class MyByteArrayOutputStream extends ByteArrayOutputStream {
		public MyByteArrayOutputStream() {
			super();
		}
	 
		public MyByteArrayOutputStream(final int size) {
			super(size);
		}
	 
		/**
		 * return backed byte array as wrapped ByteBuffer
		 * @return
		 */
			public ByteBuffer getByteBuffer() {
				return ByteBuffer.wrap(buf, 0, size());
			}
	
		/**
		 * return backed byte array(without copy)
		 * @return
		 */
		public byte[] getBuffer() {
			return buf;
		}
	}
}
