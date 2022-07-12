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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.common.BuildConfig;
import com.serenegiant.utils.BuildCheck;
import com.serenegiant.utils.Time;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public abstract class MediaDecoder implements IMediaCodec {
	private static final boolean DEBUG = BuildConfig.DEBUG;
	private static final String TAG_STATIC = MediaDecoder.class.getSimpleName();
	protected final String TAG = getClass().getSimpleName();

	/*
	 * STATE_UNINITIALIZED => [setDataSource] => STATE_INITIALIZED
	 *  => [prepare] => STATE_PREPARED [start] => STATE_PLAYING
	 *  => [seek] => STATE_PLAYING
	 * 	=> [pause] => STATE_PAUSED => [start] => STATE_PLAYING
	 * 	=> [stop] => STATE_PREPARED => [start] => STATE_PLAYING
	 * 	=> [pause] => STATE_PAUSED => [stop] => STATE_PREPARED
	 * 	=> [release] => STATE_UNINITIALIZED
	 */
	protected static final int STATE_UNINITIALIZED = 0;
	protected static final int STATE_INITIALIZED = 1;
	protected static final int STATE_PREPARED = 2;
	protected static final int STATE_PLAYING = 3;
	protected static final int STATE_PAUSED = 4;
	protected static final int STATE_WAIT = 5;

	private static final int TIMEOUT_USEC = 10000;    // 10msec

	private final Object mSync = new Object();
	private IMediaCodecCallback mCallback;
	private volatile boolean mIsRunning;
	private volatile boolean mInputDone;
	private volatile boolean mOutputDone;

	private MediaMetadataRetriever mMediaMetadataRetriever;
	private MediaExtractor mMediaExtractor;
	private MediaCodec mMediaCodec;
	private int mTrackIndex;
	private long mDuration;
	private int mBitRate;
	private MediaCodec.BufferInfo mBufferInfo;
	private ByteBuffer[] mInputBuffers;
	private ByteBuffer[] mOutputBuffers;

	private long mStartTime;
	private long presentationTimeUs;
	private long mRequestTime = -1;

	protected int mState = STATE_UNINITIALIZED;

	public MediaDecoder() {
	}

	public void setCallback(IMediaCodecCallback callback) {
		mCallback = callback;
	}

	public IMediaCodecCallback getCallback() {
		return mCallback;
	}

	public long getDuration() {
		return mDuration;
	}

	public int getBitRate() {
		return mBitRate;
	}

	public void setDataSource(final String path) throws IOException {
		release();
		try {
			mMediaMetadataRetriever = new MediaMetadataRetriever();
			mMediaMetadataRetriever.setDataSource(path);
			updateMovieInfo(mMediaMetadataRetriever);
			mMediaExtractor = new MediaExtractor();
			mMediaExtractor.setDataSource(path);
			mState = STATE_INITIALIZED;
		} catch (IOException e) {
			internal_release();
			if (!callErrorHandler(e))
				throw e;
		}
	}

	public void setDataSource(final String path, final Map<String, String> headers)
		throws IOException {

		release();
		try {
			mMediaMetadataRetriever = new MediaMetadataRetriever();
			mMediaMetadataRetriever.setDataSource(path, headers);
			updateMovieInfo(mMediaMetadataRetriever);
			mMediaExtractor = new MediaExtractor();
			mMediaExtractor.setDataSource(path, headers);
			mState = STATE_INITIALIZED;
		} catch (IOException e) {
			internal_release();
			if (!callErrorHandler(e))
				throw e;
		}
	}

	public void setDataSource(final FileDescriptor fd) throws IOException {
		release();
		try {
			mMediaMetadataRetriever = new MediaMetadataRetriever();
			mMediaMetadataRetriever.setDataSource(fd);
			updateMovieInfo(mMediaMetadataRetriever);
			mMediaExtractor = new MediaExtractor();
			mMediaExtractor.setDataSource(fd);
			mState = STATE_INITIALIZED;
		} catch (IOException e) {
			internal_release();
			if (!callErrorHandler(e))
				throw e;
		}
	}

	public void setDataSource(final FileDescriptor fd, final long offset, final long length)
		throws IOException {

		release();
		try {
			mMediaMetadataRetriever = new MediaMetadataRetriever();
			mMediaMetadataRetriever.setDataSource(fd, offset, length);
			updateMovieInfo(mMediaMetadataRetriever);
			mMediaExtractor = new MediaExtractor();
			mMediaExtractor.setDataSource(fd, offset, length);
			mState = STATE_INITIALIZED;
		} catch (IOException e) {
			internal_release();
			if (!callErrorHandler(e))
				throw e;
		}
	}

	public void setDataSource(final Context context, final Uri uri,
		final Map<String, String> headers) throws IOException {

		release();
		try {
			mMediaMetadataRetriever = new MediaMetadataRetriever();
			mMediaMetadataRetriever.setDataSource(context, uri);
			updateMovieInfo(mMediaMetadataRetriever);
			mMediaExtractor = new MediaExtractor();
			mMediaExtractor.setDataSource(context, uri, headers);
			mState = STATE_INITIALIZED;
		} catch (IOException e) {
			internal_release();
			if (!callErrorHandler(e))
				throw e;
		}
	}

	public void setDataSource(Context context, Uri uri) throws IOException {
		release();
		mMediaMetadataRetriever = new MediaMetadataRetriever();
		try {
			mMediaMetadataRetriever.setDataSource(context, uri);
			updateMovieInfo(mMediaMetadataRetriever);
			mMediaExtractor = new MediaExtractor();
			mMediaExtractor.setDataSource(context, uri, null);
			mState = STATE_INITIALIZED;
		} catch (IOException e) {
			release();
			if (!callErrorHandler(e))
				throw e;
		}
	}

	@Override
	public void prepare() throws IOException {
		if (mMediaExtractor == null) {
			final IllegalStateException e = new IllegalStateException("DataSource not set yet");
			if (!callErrorHandler(e))
				throw e;
			return;
		}
		if (mState != STATE_INITIALIZED) {
			final IllegalStateException e = new IllegalStateException("already prepared");
			if (!callErrorHandler(e))
				throw e;
			return;
		}

		try {
			mTrackIndex = handlePrepare(mMediaExtractor);
			if (mTrackIndex >= 0) {
				mMediaExtractor.selectTrack(mTrackIndex);
				final MediaFormat format = mMediaExtractor.getTrackFormat(mTrackIndex);
				mDuration = format.getLong(MediaFormat.KEY_DURATION);
				mMediaCodec = createCodec(mMediaExtractor, mTrackIndex, format);
			} else {
				throw new IOException("track not found");
			}
		} catch (Exception e) {
			if (mMediaExtractor != null) {
				mMediaExtractor.release();
				mMediaExtractor = null;
			}
			if (!callErrorHandler(e))
				throw e;
		}
		if ((mTrackIndex >= 0) && (mMediaCodec != null)) {
			mState = STATE_PREPARED;
			callOnPrepared();
		}
	}

	@Override
	public boolean isPrepared() {
		return mState >= STATE_PREPARED;
	}

	@Override
	public boolean isRunning() {
		return mState == STATE_PLAYING;
	}

	protected abstract int handlePrepare(MediaExtractor media_extractor);
	protected abstract Surface getOutputSurface();

	protected MediaCodec createCodec(final MediaExtractor media_extractor,
		final int track_index, final MediaFormat format) throws IOException {

		if (DEBUG) Log.v(TAG, "createCodec:");
		MediaCodec codec = null;
		if (track_index >= 0) {
			final String mime = format.getString(MediaFormat.KEY_MIME);
			codec = MediaCodec.createDecoderByType(mime);
			codec.configure(format, getOutputSurface(), null, 0);
			codec.start();
			if (DEBUG) Log.v(TAG, "createCodec:codec started");
		}
		return codec;
	}

	public void restart() {
		if (DEBUG) Log.v(TAG, "restart:");
		if (mState == STATE_WAIT) {
			synchronized (mSync) {
				mMediaExtractor.unselectTrack(mTrackIndex);
				mMediaExtractor.selectTrack(mTrackIndex);
				mState = STATE_PLAYING;
				mSync.notifyAll();
			}
		}
	}

	@Override
	public void start() {
		if (DEBUG) Log.v(TAG, "start:");
		boolean needRestart = true;
		switch (mState) {
		case STATE_PLAYING:
			return;
		case STATE_PAUSED:
			needRestart = false;
		case STATE_PREPARED:
			mState = STATE_PLAYING;
			break;
		default:
			throw new IllegalStateException("invalid state:" + mState);
		}
		if (needRestart) {
			presentationTimeUs = -1;
			mBufferInfo = new MediaCodec.BufferInfo();
			mInputBuffers = mMediaCodec.getInputBuffers();
			mOutputBuffers = mMediaCodec.getOutputBuffers();
			new Thread(mPlaybackTask, TAG).start();
		}
	}

	@Override
	public void stop() {
		if (DEBUG) Log.v(TAG, "stop:");
		synchronized (mSync) {
			mIsRunning = false;
			if (mState >= STATE_PLAYING) {
				mSync.notifyAll();
				try {
					mSync.wait(50);
				} catch (final InterruptedException e) {
					// ignore
				}
			}
		}
	}

	private final void internal_stop() {
		switch (mState) {
			case STATE_PLAYING:
			case STATE_PAUSED:
			case STATE_WAIT:
				if (mMediaCodec != null) {
					mMediaCodec.stop();
				}
				mState = STATE_PREPARED;
				break;
		}
	}

	public void pause() {
		switch (mState) {
			case STATE_PLAYING:
			case STATE_PAUSED:
			case STATE_WAIT:
				mState = STATE_PAUSED;
				break;
			default:
				final IllegalStateException e = new IllegalStateException();
				if (!callErrorHandler(e))
					throw e;
		}
	}

	@Override
	public void release() {
		if (DEBUG) Log.v(TAG, "release:");
		if (mState != STATE_UNINITIALIZED) {
			stop();
			mState = STATE_UNINITIALIZED;
			callOnRelease();
		}
		internal_release();
	}

	private void internal_release() {
		if (mMediaCodec != null) {
			mMediaCodec.release();
			mMediaCodec = null;
		}
		if (mMediaExtractor != null) {
			mMediaExtractor.release();
			mMediaExtractor = null;
		}
		if (mMediaMetadataRetriever != null) {
			mMediaMetadataRetriever.release();
			mMediaMetadataRetriever = null;
		}
		mTrackIndex = -1;
		mDuration = 0;
		mBitRate = 0;
	}

	public void seek(final long newTime) {
		mRequestTime = newTime;
	}

	private final void handleSeek(final long newTime) {
		if (DEBUG) Log.d(TAG, "handleSeek:");
		if (newTime < 0) return;

		if (mMediaExtractor != null) {
			if (DEBUG) Log.d(TAG, "handleSeek:" + newTime);
			mMediaExtractor.seekTo(newTime,
				MediaExtractor.SEEK_TO_PREVIOUS_SYNC/*SEEK_TO_CLOSEST_SYNC*/);
			mMediaExtractor.advance();
		}
		mRequestTime = -1;
	}

	/**
	 * playback task
	 */
	private final Runnable mPlaybackTask = new Runnable() {
		@Override
		public final void run() {
			if (DEBUG) Log.v(TAG, "PlaybackTask:start");
			mInputDone = mOutputDone = false;
			mIsRunning = true;
			callOnStart();
			for (; !mInputDone || !mOutputDone ;) {
				try {
					if (mRequestTime >= 0) {
						handleSeek(mRequestTime);
					}
					if (!mInputDone) {
						internal_HandleInput();
					}
					if (!mOutputDone) {
						internal_handleOutput();
					}
					if (!mIsRunning || (mInputDone && mOutputDone)) {
						mState = STATE_WAIT;
						callOnStop();
						if (mIsRunning) {
							mMediaCodec.flush();
							synchronized (mSync) {
								if (mState == STATE_WAIT)
								try {
									mSync.wait();
								} catch (InterruptedException e) {
									// ignore
								}
							}
							if (mIsRunning) {
								callOnStart();
								mStartTime = presentationTimeUs = -1;
								mInputDone = mOutputDone = false;
								mState = STATE_PLAYING;
							} else
								break;
						} else
							break;
					}
				} catch (final Exception e) {
					Log.e(TAG, "PlaybackTask:", e);
					callErrorHandler(e);
					break;
				}
			} // end of for
			if (DEBUG) Log.v(TAG, "PlaybackTask:finished");
			internal_stop();
			synchronized (mSync) {
				mSync.notifyAll();
			}
		}
	};

	protected void internal_HandleInput() {
//		if (DEBUG) Log.v(TAG, "internal_HandleInput:");
		boolean b = false;
		if (!mInputDone) {
			if (presentationTimeUs < 0) {
				presentationTimeUs = mMediaExtractor.getSampleTime();
//				if (DEBUG) Log.v(TAG, "internal_HandleInput:getSampleTime=" + presentationTimeUs);
			}
			if (presentationTimeUs >= 0) {
				presentationTimeUs = handleInput(presentationTimeUs);
				b = true;
			}
		}
		if (!b) {
			if (DEBUG) Log.i(TAG, "input reached EOS");
			final int inputBufIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
			if (inputBufIndex >= 0) {
				mMediaCodec.queueInputBuffer(inputBufIndex, 0, 0, 0L,
						MediaCodec.BUFFER_FLAG_END_OF_STREAM);
				if (DEBUG) Log.v(TAG, "sent input EOS");
				mInputDone = true;
			}
		}
	}

	protected long handleInput(final long presentationTimeUs) {
		long result = presentationTimeUs;
		final int inputBufIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
		if (inputBufIndex >= 0) {
			final int size = mMediaExtractor.readSampleData(mInputBuffers[inputBufIndex], 0);
//			if (DEBUG) Log.v(TAG, "handleInput:readSampleData=" + size);
			if (size > 0) {
				mMediaCodec.queueInputBuffer(inputBufIndex, 0, size, presentationTimeUs, 0);
			}
			mMediaExtractor.advance();
			result = -1;
		}
		return result;
	}

	/**
	 */
	@SuppressWarnings("deprecation")
	protected void internal_handleOutput() {
		final int decoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
		if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
			return;
		} else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
			mOutputBuffers = mMediaCodec.getOutputBuffers();
			if (DEBUG) Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED:");
		} else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
			final MediaFormat newFormat = mMediaCodec.getOutputFormat();
			if (DEBUG) Log.d(TAG, "output format changed: " + newFormat);
		} else if (decoderStatus < 0) {
			final RuntimeException e = new RuntimeException(
					"unexpected result from dequeueOutputBuffer: " + decoderStatus);
			if (!callErrorHandler(e))
				throw e;
		} else { // decoderStatus >= 0
			boolean doRender = false;
			if (mBufferInfo.size > 0) {
				doRender = !handleOutput(mOutputBuffers[decoderStatus],
						0, mBufferInfo.size, mBufferInfo.presentationTimeUs);
				if (doRender) {
					if ((mCallback == null)
						|| !mCallback.onFrameAvailable(this, mBufferInfo.presentationTimeUs)) {
						
						mStartTime = adjustPresentationTime(mStartTime, mBufferInfo.presentationTimeUs);
					}
				}
			}
			mMediaCodec.releaseOutputBuffer(decoderStatus, doRender);
			if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
				if (DEBUG) Log.d(TAG, "received EOS");
				mOutputDone = true;
			}
		}
	}

	/**
	 * process decoded data
	 * @param buffer
	 * @param offset
	 * @param size
	 * @param presentationTimeUs
	 * @return if return false, presentation time adjustment is executed internally.
	 */
	protected abstract boolean handleOutput(ByteBuffer buffer,
		int offset, int size, long presentationTimeUs);

	protected boolean callErrorHandler(final Exception e) {
		if (mCallback != null) {
			return mCallback.onError(this, e);
		}
		return false;
	}

	protected void callOnPrepared() {
		if (mCallback != null) {
			try {
				mCallback.onPrepared(this);
			} catch (Exception e) {
				Log.w(TAG, "callOnPrepared", e);
			}
		}
	}

	protected void callOnStart() {
		if (mCallback != null) {
			try {
				mCallback.onStart(this);
			} catch (Exception e) {
				Log.w(TAG, "callOnStart", e);
			}
		}
	}

	protected void callOnStop() {
		if (mCallback != null) {
			try {
				mCallback.onStop(this);
			} catch (Exception e) {
				Log.w(TAG, "callOnStop", e);
			}
		}
	}

	protected void callOnRelease() {
		if (mCallback != null) {
			try {
				mCallback.onRelease(this);
			} catch (Exception e) {
				Log.w(TAG, "callOnRelease", e);
			}
		}
	}

	protected void updateMovieInfo(final MediaMetadataRetriever metadata) {
		mBitRate = 0;
		String value = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
		if (!TextUtils.isEmpty(value)) {
			mBitRate = Integer.parseInt(value);
		}
	}

	/**
	 * adjust presentation time
	 * @param startTime
	 * @param presentationTimeUs
	 * @return
	 */
	protected long adjustPresentationTime(final long startTime, final long presentationTimeUs) {
		if (BuildCheck.isJellyBeanMr1()) {
			return adjustPresentationTimeAPI17(startTime, presentationTimeUs);
		}
		if (startTime > 0) {
			for (long t = presentationTimeUs - (Time.nanoTime() / 1000 - startTime);
				 t > 0; t = presentationTimeUs - (Time.nanoTime() / 1000 - startTime)) {
				synchronized (mSync) {
					try {
						mSync.wait(t / 1000, (int)((t % 1000) * 1000));
					} catch (final InterruptedException e) {
						// ignore
					}
					if (!mIsRunning)
						break;
				}
			}
			return startTime;
		} else {
			return Time.nanoTime() / 1000;
		}
	}

	@SuppressLint("NewApi")
	protected long adjustPresentationTimeAPI17(final long startTime, final long presentationTimeUs) {
		if (startTime > 0) {
			for (long t = presentationTimeUs - (Time.nanoTime() / 1000 - startTime);
				 t > 0; t = presentationTimeUs - (Time.nanoTime() / 1000 - startTime)) {
				synchronized (mSync) {
					try {
						mSync.wait(t / 1000, (int)((t % 1000) * 1000));
					} catch (final InterruptedException e) {
						// ignore
					}
					if (!mIsRunning)
						break;
				}
			}
			return startTime;
		} else {
			return Time.nanoTime() / 1000;
		}
	}

	/**
	 * search first track index matched specific MIME
	 * @param extractor
	 * @param mimeType "video/" or "audio/"
	 * @return track index, -1 if not found
	 */
	protected static final int selectTrack(final MediaExtractor extractor, final String mimeType) {
		final int numTracks = extractor.getTrackCount();
		MediaFormat format;
		String mime;
		for (int i = 0; i < numTracks; i++) {
			format = extractor.getTrackFormat(i);
			mime = format.getString(MediaFormat.KEY_MIME);
			if (mime.startsWith(mimeType)) {
				if (DEBUG) Log.d(TAG_STATIC, "Extractor selected track " + i + " (" + mime + "): " + format);
				return i;
			}
		}
		return -1;
	}
}
