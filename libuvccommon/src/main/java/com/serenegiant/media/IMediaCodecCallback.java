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

/**
 * callback interface
 */
public interface IMediaCodecCallback {
	/**
	 * called when preparing finished
	 * @param codec
	 */
	public void onPrepared(IMediaCodec codec);

	/**
	 * called when start playing
	 * @param codec
	 */
	public void onStart(IMediaCodec codec);

	/**
	 * called every frame before time adjusting
	 * return true if you don't want to use internal time adjustment
	 * @param codec
	 * @param presentationTimeUs
	 * @return
	 */
	public boolean onFrameAvailable(IMediaCodec codec, long presentationTimeUs);

	/**
	 * called when playing stopped
	 * @param codec
	 */
	public void onStop(IMediaCodec codec);

	/**
	* called before releasing MediaCodec instance
	 * @param codec
	 */
	public void onRelease(IMediaCodec codec);

	/**
	 * called when error occurred
	 * @param codec
	 * @param e
	 * @return handled
	 */
	public boolean onError(IMediaCodec codec, Exception e);
}
