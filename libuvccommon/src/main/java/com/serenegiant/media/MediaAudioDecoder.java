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
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.common.BuildConfig;

import java.io.IOException;
import java.nio.ByteBuffer;

@SuppressWarnings("deprecation")
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class MediaAudioDecoder extends MediaDecoder {
	private static final boolean DEBUG = BuildConfig.DEBUG;

	private int mAudioChannels;
	private int mAudioSampleRate;
	private int mAudioInputBufSize;
	private byte[] mAudioOutTempBuf;
	private AudioTrack mAudioTrack;

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@Override
	protected int handlePrepare(MediaExtractor media_extractor) {
		int track_index = selectTrack(media_extractor, "audio/");
		if (track_index >= 0) {
			final MediaFormat format = media_extractor.getTrackFormat(track_index);
			mAudioChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
			mAudioSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
			final int min_buf_size = AudioTrack.getMinBufferSize(mAudioSampleRate,
					(mAudioChannels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO),
					AudioFormat.ENCODING_PCM_16BIT);
			final int max_input_size = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
			mAudioInputBufSize =  min_buf_size > 0 ? min_buf_size * mAudioChannels * 2 : max_input_size;
			if (mAudioInputBufSize > max_input_size) mAudioInputBufSize = max_input_size;
			final int frameSizeInBytes = mAudioChannels * 2;
			mAudioInputBufSize = (mAudioInputBufSize / frameSizeInBytes) * frameSizeInBytes;
			if (DEBUG) Log.v(TAG, String.format("getMinBufferSize=%d,max_input_size=%d,mAudioInputBufSize=%d",min_buf_size, max_input_size, mAudioInputBufSize));
		}
		return track_index;
	}

	@Override
	protected MediaCodec createCodec(final MediaExtractor media_extractor, final int track_index, final MediaFormat format)
		throws IOException, IllegalArgumentException {

		final MediaCodec codec = super.createCodec(media_extractor, track_index, format);
		if (codec != null) {
			final ByteBuffer[] buffers = codec.getOutputBuffers();
			int sz = buffers[0].capacity();
			if (sz <= 0)
				sz = mAudioInputBufSize;
			if (DEBUG) Log.v(TAG, "AudioOutputBufSize:" + sz);
			mAudioOutTempBuf = new byte[sz];
			try {
				mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
					mAudioSampleRate,
					(mAudioChannels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO),
					AudioFormat.ENCODING_PCM_16BIT,
					mAudioInputBufSize,
					AudioTrack.MODE_STREAM);
				mAudioTrack.play();
			} catch (final Exception e) {
				Log.e(TAG, "failed to start audio track playing", e);
				if (mAudioTrack != null) {
					mAudioTrack.release();
					mAudioTrack = null;
				}
				throw e;
			}
		}
		return codec;
	}

	@Override
	protected Surface getOutputSurface() {
		return null;
	}

	@Override
	protected boolean handleOutput(ByteBuffer buffer,
		int offset, int size, long presentationTimeUs) {

		if (mAudioOutTempBuf.length < size) {
			mAudioOutTempBuf = new byte[size];
		}
		buffer.position(offset);
		buffer.get(mAudioOutTempBuf, 0, size);
		buffer.clear();
		if (mAudioTrack != null)
			mAudioTrack.write(mAudioOutTempBuf, 0, size);
		return true;
	}

}
