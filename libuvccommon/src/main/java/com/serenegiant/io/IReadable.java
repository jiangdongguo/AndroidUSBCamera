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

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 読み込み可能なクラスを示すインターフェース
 */
public interface IReadable extends Closeable {
	public int read(final ByteBuffer dst) throws IOException;
	public int read(final byte[] b, final int off, final int len) throws IOException;
	public int available() throws IOException;
	public long skip(final long n) throws IOException;
}
