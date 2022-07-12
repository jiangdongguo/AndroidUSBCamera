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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * InputStreamをIReadableインターフェースでラップ
 */
public class ReadableInputStreamWrapper extends InputStream implements IReadable {
	
	private final InputStream mParent;
	public ReadableInputStreamWrapper(final InputStream parent) {
		if (parent == null) {
			throw new NullPointerException();
		}
		mParent = parent;
	}
	
	@Override
	public int read(final ByteBuffer dst) throws IOException {
		final byte[] b = new byte[dst.remaining()];
		final int readBytes = mParent.read(b);
		dst.put(b, 0, readBytes);
		return readBytes;
	}
	
	@Override
	public int read() throws IOException {
		return mParent.read();
	}
	
	@Override
	public int read(@NonNull final byte[] b,
		final int off, final int len) throws IOException {

		return mParent.read(b, off, len);
	}

	@Override
	 public long skip(final long n) throws IOException {
		 return mParent.skip(n);
	 }

	@Override
	 public int available() throws IOException {
		return mParent.available();
	 }

	@Override
 	public void close() throws IOException {
		mParent.close();
 	}

	@Override
 	public synchronized void mark(final int readlimit) {
    	mParent.mark(readlimit);
 	}

	@Override
 	public synchronized void reset() throws IOException {
    	mParent.reset();
 	}

	@Override
 	public boolean markSupported() {
    	return mParent.markSupported();
 	}

}
