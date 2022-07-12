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
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import com.serenegiant.common.BuildConfig;
import com.serenegiant.utils.Time;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

@SuppressWarnings("deprecation")
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public abstract class MediaEncoder implements IMediaCodec {
	private static final boolean DEBUG = BuildConfig.DEBUG;
	private final String TAG = getClass().getSimpleName();

	protected final boolean mIsAudio;

	protected final Object mSync = new Object();
	protected boolean mIsPrepared;
	/**
	 * Flag that indicate this encoder is capturing now.
	 */
	protected volatile boolean mIsCapturing;
	/**
	 * Flag that indicate the frame data will be available soon.
	 */
	private int mRequestDrain;
	/**
	 * Flag to request stop capturing
	 */
	protected volatile boolean mRequestStop;
	/**
	 * Flag that indicate encoder received EOS(End Of Stream)
	 */
	protected boolean mIsEOS;
	/**
	 * Flag the indicate the muxer is running
	 */
	protected boolean mMuxerStarted;
	/**
	 * Track Number
	 */
	protected int mTrackIndex;
	/**
	 * MediaCodec instance for encoding
	 */
	protected MediaCodec mMediaCodec;				// API >= 16(Android4.1.2)
	/**
	 * Weak reference of MediaMuxerWrapper instance
	 */
	protected final WeakReference<AbstractRecorder> mWeakMuxer;
	/**
	 * BufferInfo instance for dequeuing
	 */
	private MediaCodec.BufferInfo mBufferInfo;		// API >= 16(Android4.1.2)

	private final IMediaCodecCallback mCallback;

	public MediaEncoder(final boolean is_audio, AbstractRecorder muxer, IMediaCodecCallback listener) {
		if (listener == null) throw new NullPointerException("MediaEncoderListener is null");
		if (muxer == null) throw new NullPointerException("MediaMuxerWrapper is null");
		mIsAudio = is_audio;
		mWeakMuxer = new WeakReference<AbstractRecorder>(muxer);
		muxer.addEncoder(this);
		mCallback = listener;
		synchronized (mSync) {
			// create BufferInfo here for effectiveness(to reduce GC)
			mBufferInfo = new MediaCodec.BufferInfo();
			// wait for starting thread
			new Thread(mEncodeTask, getClass().getSimpleName()).start();
			try {
				mSync.wait();
			} catch (InterruptedException e) {
			}
		}
	}

	@Override
	public void start() {
		if (DEBUG) Log.v(TAG, "start:");
		synchronized (mSync) {
			mIsCapturing = true;
			mRequestStop = false;
			mSync.notifyAll();
		}
	}

	/**
	 * the method to request stop encoding
	 */
	@Override
	public void stop() {
		if (DEBUG) Log.v(TAG, "stop:mIsCapturing=" + mIsCapturing + ", mRequestStop=" + mRequestStop);
		synchronized (mSync) {
			if (!mIsCapturing || mRequestStop) {
				return;
			}
			mRequestStop = true;	// for rejecting newer frame
			mSync.notifyAll();
			// We can not know when the encoding and writing finish.
			// so we return immediately after request to avoid delay of caller thread
		}
	}

	/**
	 * 出力ファイルのパスを返す
	 * @return
	 */
	public String getOutputPath() {
		final AbstractRecorder recorder = mWeakMuxer.get();
		return recorder != null ? recorder.getOutputPath() : null;
	}

	/**
	 * the method to indicate frame data is soon available or already available
	 * @return return true if encoder is ready to encode.
	 */
	public void frameAvailableSoon() {
//    	if (DEBUG) Log.v(TAG, "frameAvailableSoon");
		synchronized (mSync) {
			if (!mIsCapturing || mRequestStop) {
				return;
			}
			mRequestDrain++;
			mSync.notifyAll();
		}
	}

	@Override
	public boolean isPrepared() {
		return mIsPrepared;
	}

	@Override
	public boolean isRunning() {
		return mIsCapturing;
	}

	public boolean isCapturing() {
		return mIsCapturing;
	}
	
	public boolean isAudio() {
		return mIsAudio;
	}

	protected boolean callErrorHandler(final Exception e) {
		return mCallback != null && mCallback.onError(this, e);
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

	private final Runnable mEncodeTask = new Runnable() {
		/**
		 * encoding loop on private thread
		 */
		@Override
		public void run() {
			synchronized (mSync) {
				mRequestStop = false;
				mRequestDrain = 0;
				mSync.notify();
				try {
					mSync.wait();
					callOnStart();
				} catch (InterruptedException e) {
				}
			}
			boolean localRequestStop;
			boolean localRequestDrain;
			for (;;) {
				synchronized (mSync) {
					localRequestStop = mRequestStop;
					localRequestDrain = (mRequestDrain > 0);
					if (localRequestDrain)
						mRequestDrain--;
					if (!localRequestDrain && !localRequestStop) {
						try {
							mSync.wait();
						} catch (InterruptedException e) {
							break;
						}
						continue;
					}
				}
				if (localRequestStop) {
					if (DEBUG) Log.v(TAG, "stopping");
					callOnStop();
					drain();
					// request stop recording
					signalEndOfInputStream();
					// process output data again for EOS signal
					drain();
					break;
				}
				if (localRequestDrain) {
					drain();
				}
			} // end of while
			// release all related objects
			release();
			if (DEBUG) Log.d(TAG, "MediaEncoder thread exiting");
			synchronized (mSync) {
				mRequestStop = true;
				mIsCapturing = false;
			}

		}
	};

//********************************************************************************
//********************************************************************************
	/**
	 * Release all related objects
	 */
	public void release() {
		if (DEBUG) Log.d(TAG, "release:");
		mIsCapturing = false;
		if (mMediaCodec != null) {
			try {
				mMediaCodec.stop();
				mMediaCodec.release();
				mMediaCodec = null;
			} catch (Exception e) {
				final boolean handled = callErrorHandler(e);
				if (!handled)
					Log.e(TAG, "failed releasing MediaCodec", e);
			}
		}
		if (mMuxerStarted) {
			final AbstractRecorder muxer = mWeakMuxer.get();
			if (muxer != null) {
				try {
					muxer.stop();
				} catch (Exception e) {
					final boolean handled = callErrorHandler(e);
					if (!handled)
						Log.e(TAG, "failed stopping muxer", e);
				}
			}
		}
		mBufferInfo = null;
		callOnRelease();
	}

	public void signalEndOfInputStream() {
		if (DEBUG) Log.d(TAG, "sending EOS to encoder");
		// signalEndOfInputStream is only available for video encoding with surface
		// and equivalent sending a empty buffer with BUFFER_FLAG_END_OF_STREAM flag.
//		mMediaCodec.signalEndOfInputStream();	// API >= 18
		encode(null, 0, getPTSUs());
	}

	public void encode(final ByteBuffer buffer) {
		encode(buffer, buffer.capacity(), getPTSUs());
	}
	
	/**
	 * Method to set byte array to the MediaCodec encoder
	 * @param buffer
	 * @param length　length of byte array, zero means EOS.
	 * @param presentationTimeUs
	 */
	public void encode(final ByteBuffer buffer, int length, long presentationTimeUs) {
		if (!mIsCapturing || mRequestStop || (mMediaCodec == null)) return;
		final ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
		while (mIsCapturing) {
			final int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
			if (inputBufferIndex >= 0) {
				final ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
				inputBuffer.clear();
				if (buffer != null) {
					inputBuffer.put(buffer);
				}
				if (length > 0) {
					mMediaCodec.queueInputBuffer(inputBufferIndex, 0, length,
							presentationTimeUs, 0);
				} else {
					// send EOS
					mIsEOS = true;
					if (DEBUG) Log.i(TAG, "send BUFFER_FLAG_END_OF_STREAM");
					mMediaCodec.queueInputBuffer(inputBufferIndex, 0, 0,
							presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
				}
				break;
			} else if (inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
				// wait for MediaCodec encoder is ready to encode
				// nothing to do here because MediaCodec#dequeueInputBuffer(TIMEOUT_USEC)
				// will wait for maximum TIMEOUT_USEC(10msec) on each call
			}
		}
	}

	/**
	 * drain encoded data and write them to muxer
	 */
	protected void drain() {
		if (mMediaCodec == null) return;
		ByteBuffer[] encoderOutputBuffers = mMediaCodec.getOutputBuffers();
		int encoderStatus, count = 0;
		final AbstractRecorder muxer = mWeakMuxer.get();
		if (muxer == null) {
//        	throw new NullPointerException("muxer is unexpectedly null");
			Log.w(TAG, "muxer is unexpectedly null");
			return;
		}
LOOP:	while (mIsCapturing)
		try {
			// get encoded data with maximum timeout duration of TIMEOUT_USEC(=10[msec])
			encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
			if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
				// wait 5 counts(=TIMEOUT_USEC x 5 = 50msec) until data/EOS come
				if (!mIsEOS) {
					if (++count > 5)
						break LOOP;        // out of while
				}
			} else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
				if (DEBUG) Log.v(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
				// this should not come when encoding
				encoderOutputBuffers = mMediaCodec.getOutputBuffers();
			} else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
				if (DEBUG) Log.v(TAG, "INFO_OUTPUT_FORMAT_CHANGED");
				// this status indicate the output format of codec is changed
				// this should come only once before actual encoded data
				// but this status never come on Android4.3 or less
				// and in that case, you should treat when MediaCodec.BUFFER_FLAG_CODEC_CONFIG come.
				if (mMuxerStarted) {    // second time request is error
					final RuntimeException e = new RuntimeException("format changed twice");
					final boolean handled = callErrorHandler(e);
					if (!handled)
						throw e;
				}
				// get output format from codec and pass them to muxer
				// getOutputFormat should be called after INFO_OUTPUT_FORMAT_CHANGED otherwise crash.
				final MediaFormat format = mMediaCodec.getOutputFormat(); // API >= 16
				mTrackIndex = muxer.addTrack(format);
				mMuxerStarted = true;
				if (!muxer.start()) {
					// we should wait until muxer is ready
					synchronized (muxer) {
						while (!muxer.isStarted() && mIsCapturing)
							try {
								muxer.wait(100);
							} catch (final InterruptedException e) {
								break LOOP;
							}
					}
				}
			} else if (encoderStatus < 0) {
				// unexpected status
				if (DEBUG)
					Log.w(TAG, "drain:unexpected result from encoder#dequeueOutputBuffer: " + encoderStatus);
			} else {
				final ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
				if (encodedData == null) {
					// this never should come...may be a MediaCodec internal error
					final RuntimeException e = new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
					final boolean handled = callErrorHandler(e);
					if (!handled)
						throw e;
				}
				if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
					// You should set output format to muxer here when you target Android4.3 or less
					// but MediaCodec#getOutputFormat can not call here(because INFO_OUTPUT_FORMAT_CHANGED don't come yet)
					// therefor we should expand and prepare output format from buffer data.
					// This sample is for API>=18(>=Android 4.3), just ignore this flag here
					if (DEBUG) Log.d(TAG, "drain:BUFFER_FLAG_CODEC_CONFIG");
					mBufferInfo.size = 0;
				}

				if (mBufferInfo.size != 0) {
					// encoded data is ready, clear waiting counter
					count = 0;
					if (!mMuxerStarted) {
						// muxer is not ready...this will programing failure.
						throw new RuntimeException("drain:muxer hasn't started");
					}
					// write encoded data to muxer(need to adjust presentationTimeUs.
					mBufferInfo.presentationTimeUs = getPTSUs();
					muxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
					prevOutputPTSUs = mBufferInfo.presentationTimeUs;
				}
				// return buffer to encoder
				mMediaCodec.releaseOutputBuffer(encoderStatus, false);
				if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
					// when EOS come.
					mIsCapturing = false;
					break;      // out of while
				}
			}
		} catch (Exception e) {
			callErrorHandler(e);
			break;
		}
	}

	/**
	 * previous presentationTimeUs for writing
	 */
	private long prevOutputPTSUs = 0;
	/**
	 * get next encoding presentationTimeUs
	 * @return
	 */
    @SuppressLint("NewApi")
	protected long getPTSUs() {
		long result = Time.nanoTime() / 1000L;
		// presentationTimeUs should be monotonic
		// otherwise muxer fail to write
		if (result < prevOutputPTSUs)
			result = (prevOutputPTSUs - result) + result;
		return result;
	}
}
