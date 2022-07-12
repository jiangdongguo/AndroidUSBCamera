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

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;

import android.annotation.TargetApi;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import android.util.Log;

/**
 * MediaMuxerをIMuxerインターフェースでラップ
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class MediaMuxerWrapper implements IMuxer {
	private static final String TAG = MediaMuxerWrapper.class.getSimpleName();

	private final MediaMuxer mMuxer;
	private volatile boolean mIsStarted;
	private boolean mReleased;

	public MediaMuxerWrapper(final String output_path, final int format)
		throws IOException {

		mMuxer = new MediaMuxer(output_path, format);
	}

	@RequiresApi(api = Build.VERSION_CODES.O)
	public MediaMuxerWrapper(final FileDescriptor fd, final int format)
		throws IOException {

		mMuxer = new MediaMuxer(fd, format);
	}
	
	@Override
	public int addTrack(@NonNull final MediaFormat format) {
		return mMuxer.addTrack(format);
	}

	@Override
	public void writeSampleData(final int trackIndex,
		@NonNull final ByteBuffer byteBuf, @NonNull final BufferInfo bufferInfo) {

		if (!mReleased) {
			mMuxer.writeSampleData(trackIndex, byteBuf, bufferInfo);
		}
	}

	@Override
	public void start() {
		mMuxer.start();
		mIsStarted = true;
	}

	@Override
	public void stop() {
		if (mIsStarted) {
			mIsStarted = false;
			mMuxer.stop();
		}
	}

	@Override
	public void release() {
		mIsStarted = false;
		if (!mReleased) {
			mReleased = true;
			try {
				mMuxer.release();
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}
	}

	@Override
	public boolean isStarted() {
		return mIsStarted && !mReleased;
	}

}
