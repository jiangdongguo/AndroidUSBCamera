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

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.common.BuildConfig;

import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class AbstractRecorder {
	private static final boolean DEBUG = BuildConfig.DEBUG;
	private static final String TAG = "AbstractRecorder";

	protected final String mOutputPath;
	protected int mEncoderCount, mStartedCount;
	protected volatile boolean mIsStarted;

	protected MediaEncoder mVideoEncoder, mAudioEncoder;

	public AbstractRecorder(String output_path) {
		mOutputPath = output_path;
		mEncoderCount = mStartedCount = 0;
		mIsStarted = false;
	}

	public void prepare() throws IOException {
		if (DEBUG) Log.v(TAG, "prepare:");
		if (mVideoEncoder != null)
			mVideoEncoder.prepare();
		if (mAudioEncoder != null)
			mAudioEncoder.prepare();
	}

	public void startRecording() {
		if (DEBUG) Log.v(TAG, "startRecording:");
		if (mVideoEncoder != null)
			mVideoEncoder.start();
		if (mAudioEncoder != null)
			mAudioEncoder.start();
	}

	public void stopRecording() {
		if (DEBUG) Log.v(TAG, "stopRecording:");
		if (mVideoEncoder != null) {
			mVideoEncoder.stop();
		}
		mVideoEncoder = null;
		if (mAudioEncoder != null) {
			mAudioEncoder.stop();
		}
		mAudioEncoder = null;
	}

	public void release() {
		if (mVideoEncoder != null) {
			mVideoEncoder.release();
			mVideoEncoder = null;
		}
		if (mAudioEncoder != null) {
			mAudioEncoder.release();
			mAudioEncoder = null;
		}
	}

	public boolean isStarted() {
		return mIsStarted;
	}

	/**
	 * assign encoder to this class. this is called from encoder.
	 * @param encoder
	 */
	/*package*/ void addEncoder(MediaEncoder encoder) {
		if (encoder.isAudio()) {
			if (mAudioEncoder != null)
				throw new IllegalArgumentException("Video encoder already added.");
			mAudioEncoder = encoder;
		} else {
			if (mVideoEncoder != null)
				throw new IllegalArgumentException("Video encoder already added.");
			mVideoEncoder = encoder;
		}
		mEncoderCount = (mVideoEncoder != null ? 1 : 0) + (mAudioEncoder != null ? 1 : 0);
	}

	protected abstract void internal_start();
	/**
	 * request start recording from encoder
	 * @return true when muxer is ready to write
	 */
	/*package*/ synchronized boolean start() {
		if (DEBUG) Log.v(TAG, "start:");
		mStartedCount++;
		if ((mEncoderCount > 0) && (mStartedCount == mEncoderCount)) {
			internal_start();
			mIsStarted = true;
			notifyAll();
			if (DEBUG) Log.v(TAG,  "MediaMuxer started:");
		}
		return mIsStarted;
	}

	protected abstract void internal_stop();
	/**
	 * request stop recording from encoder when encoder received EOS
	 */
	/*package*/ synchronized void stop() {
		if (DEBUG) Log.v(TAG,  "stop:mStartedCount=" + mStartedCount);
		mStartedCount--;
		if ((mEncoderCount > 0) && (mStartedCount <= 0)) {
			mIsStarted = false;
			internal_stop();
			if (DEBUG) Log.v(TAG,  "stopped:");
		}
	}

	/**
	 * assign encoder to muxer
	 * @param format
	 * @return minus value indicate error
	 */
	/*package*/ abstract int addTrack(MediaFormat format);

	/**
	 * write encoded data to muxer
	 * @param trackIndex
	 * @param byteBuf
	 * @param bufferInfo
	 */
	/*package*/ abstract void writeSampleData(int trackIndex, ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo);

	public abstract int getWidth();

	public abstract int getHeight();

	public void frameAvailableSoon() {
		if (mVideoEncoder != null)
			mVideoEncoder.frameAvailableSoon();
	}

	public abstract Surface getInputSurface() throws IllegalStateException;

	public String getOutputPath() {
		return mOutputPath;
	}
}
