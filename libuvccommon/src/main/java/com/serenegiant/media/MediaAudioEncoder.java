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

import android.annotation.TargetApi;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

import com.serenegiant.common.BuildConfig;

import java.io.IOException;
import java.nio.ByteBuffer;

@SuppressWarnings("deprecation")
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class MediaAudioEncoder extends MediaEncoder implements IAudioEncoder {
	private static final boolean DEBUG = BuildConfig.DEBUG;
	private static final String TAG = "MediaAudioEncoder";

	private static final String MIME_TYPE = MediaCodecHelper.MIME_AUDIO_AAC;
	private static final int SAMPLE_RATE = 44100;		// 44.1[KHz] is only setting guaranteed to be available on all devices.
	private static final int BIT_RATE = 64000;			// 64[kbps]　5-320[kbps]
	public static final int SAMPLES_PER_FRAME = 1024;	// AAC, bytes/frame/channel
	public static final int FRAMES_PER_BUFFER = 25; 	// AAC, frame/buffer/sec

	private AudioThread mAudioThread = null;

	public MediaAudioEncoder(MediaMovieRecorder muxer, IMediaCodecCallback listener) {
		super(true, muxer, listener);
	}

	@Override
	public void prepare() throws IOException {
		if (DEBUG) Log.v(TAG, "prepare:");
		mTrackIndex = -1;
		mMuxerStarted = mIsEOS = false;
		// prepare MediaCodec for AAC encoding of audio data from internal mic.
		final MediaCodecInfo audioCodecInfo = MediaCodecHelper.selectAudioEncoder(MIME_TYPE);
		if (audioCodecInfo == null) {
			Log.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
			return;
		}
		if (DEBUG) Log.i(TAG, "selected codec: " + audioCodecInfo.getName());

		final MediaFormat audioFormat = MediaFormat.createAudioFormat(MIME_TYPE, SAMPLE_RATE, 1);
		audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
		audioFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_MONO);
		audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
		audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
//		audioFormat.setLong(MediaFormat.KEY_MAX_INPUT_SIZE, inputFile.length());
//      audioFormat.setLong(MediaFormat.KEY_DURATION, (long)durationInMs );
		if (DEBUG) Log.i(TAG, "format: " + audioFormat);
		mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
		mMediaCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
		mMediaCodec.start();
		mIsPrepared = true;
		if (DEBUG) Log.i(TAG, "prepare finishing");
		callOnPrepared();
	}

	@Override
	public void start() {
		super.start();
		// create and execute audio capturing thread using internal mic
		if (mAudioThread == null) {
			mAudioThread = new AudioThread();
			mAudioThread.start();
		}
	}

	@Override
	public void release() {
		mAudioThread = null;
		super.release();
	}

	private static final int[] AUDIO_SOURCES = new int[] {
			MediaRecorder.AudioSource.CAMCORDER,
			MediaRecorder.AudioSource.MIC,
			MediaRecorder.AudioSource.DEFAULT,
			MediaRecorder.AudioSource.VOICE_COMMUNICATION,
			MediaRecorder.AudioSource.VOICE_RECOGNITION,
	};

	/**
	 * Thread to capture audio data from internal mic as uncompressed 16bit PCM data
	 * and write them to the MediaCodec encoder
	 */
	private class AudioThread extends Thread {
		@Override
		public void run() {
			if (DEBUG) Log.v(TAG, "AudioThread:start");
    		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
			try {
				final int min_buffer_size = AudioRecord.getMinBufferSize(
						SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
						AudioFormat.ENCODING_PCM_16BIT);
				int buffer_size = SAMPLES_PER_FRAME * FRAMES_PER_BUFFER;
				if (buffer_size < min_buffer_size)
					buffer_size = ((min_buffer_size / SAMPLES_PER_FRAME) + 1) * SAMPLES_PER_FRAME * 2;

				AudioRecord audioRecord = null;
				for (final int source : AUDIO_SOURCES) {
					try {
						audioRecord = new AudioRecord(
								source, SAMPLE_RATE,
								AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, buffer_size);
						if (audioRecord != null) {
							if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
								audioRecord.release();
								audioRecord = null;
							}
						}
					} catch (final Exception e) {
						audioRecord = null;
					}
					if (audioRecord != null) break;
				}

				if (audioRecord != null) {
					try {
						if (mIsCapturing && (audioRecord.getState() == AudioRecord.STATE_INITIALIZED)) {
							final ByteBuffer buf = ByteBuffer.allocateDirect(SAMPLES_PER_FRAME);
							int readBytes;
							if (DEBUG) Log.v(TAG, "AudioThread:start audio recording");
							audioRecord.startRecording();
							try {
								while (mIsCapturing && !mRequestStop && !mIsEOS) {
									// read audio data from internal mic
									buf.clear();
									readBytes = audioRecord.read(buf, SAMPLES_PER_FRAME);
									if (readBytes > 0) {
										// set audio data to encoder
										buf.position(readBytes);
										buf.flip();
										encode(buf, readBytes, getPTSUs());
										frameAvailableSoon();
									} else {
										if (DEBUG)
											Log.w(TAG, "AudioThread:read return error:err=" + readBytes);
									}
								}
								frameAvailableSoon();
							} finally {
								if (DEBUG) Log.v(TAG, "AudioThread:stop audio recording");
								audioRecord.stop();
							}
						}
					} finally {
						if (DEBUG) Log.v(TAG, "AudioThread:releasing");
						audioRecord.release();
					}
				}
			} catch(Exception e){
				Log.e(TAG, "AudioThread#run", e);
			}
			if (DEBUG) Log.v(TAG, "AudioThread:finished");
		}
	}

}
