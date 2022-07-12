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
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

/**
 * ByteChannelをIReadableとIWritableインターフェースでラップ
 * XXX FileChannel等の専用ラッパークラスを下位に作ったほうがいいかも
 */
public class ByteChannelWrapper implements IReadable, IWritable {

	@NonNull
	private final ByteChannel mChannel;
	public ByteChannelWrapper(@NonNull final ByteChannel channel) {
		mChannel = channel;
	}
	
	@Override
	public int read(final ByteBuffer dst) throws IOException {
		return mChannel.read(dst);
	}
	
	@Override
	public int read(final byte[] b, final int off, final int len) throws IOException {
		final ByteBuffer buf = ByteBuffer.wrap(b, off, len);
		return mChannel.read(buf);
	}
	
	@Override
	public int available() throws IOException {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public long skip(final long n) throws IOException {
		// FIXME 未実装, ダミー読み込みをする
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void write(final ByteBuffer src) throws IOException {
		mChannel.write(src);
	}
	
	@Override
	public void write(final byte[] b, final int off, final int len) throws IOException {
		final ByteBuffer buf = ByteBuffer.wrap(b, off, len);
		mChannel.write(buf);
	}
	
	@Override
	public void flush() throws IOException {
		// do nothing
	}
	
	@Override
	public void close() throws IOException {
		mChannel.close();
	}
}
