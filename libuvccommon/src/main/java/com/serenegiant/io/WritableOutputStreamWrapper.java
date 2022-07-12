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
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * OutputStreamをIWritableでラップ
 */
public class WritableOutputStreamWrapper extends OutputStream implements IWritable {
	private final OutputStream mParent;
	
	public WritableOutputStreamWrapper(final OutputStream parent) {
		if (parent == null) {
			throw new NullPointerException();
		}
		mParent = parent;
	}
	
	@Override
	public void write(final ByteBuffer src) throws IOException {
		final int len = src.remaining();
		if (len > 0) {
			final byte[] buf = new byte[len];
			src.get(buf);
			mParent.write(buf, 0, len);
		}
	}

	@Override
	public void write(final int val) throws IOException {
		mParent.write(val);
	}

	@Override
	public void write(@NonNull final byte[] b) throws IOException {
		mParent.write(b);
	}

	@Override
	public void write(@NonNull final byte[] b, final int off, final int len)
		throws IOException {

		mParent.write(b, off, len);
	}

	@Override
 	public void flush() throws IOException {
 		mParent.flush();
	}

	@Override
	public void close() throws IOException {
		mParent.close();
	}
}
