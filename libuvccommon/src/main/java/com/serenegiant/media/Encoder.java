package com.serenegiant.media;
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

import java.nio.ByteBuffer;

public interface Encoder {
	public abstract void prepare()  throws Exception;
	public abstract void start();;
	public abstract void stop();
	public abstract void release();
	public abstract void signalEndOfInputStream();
	public abstract void encode(final ByteBuffer buffer);
	public abstract void encode(final ByteBuffer buffer, final int length, final long presentationTimeUs);
	public abstract void frameAvailableSoon();
	public abstract boolean isCapturing();
	public abstract String getOutputPath();
	@Deprecated
	public abstract boolean isAudio();
}
