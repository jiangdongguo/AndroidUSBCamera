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
import java.nio.ByteOrder;

import android.media.MediaRecorder;

/**
 * AudioSampleから音声データを受け取ってMediaCodecでエンコードするためのクラス
 */
public class AudioSamplerEncoder extends AbstractAudioEncoder {
//	private static final boolean DEBUG = false;	// FIXME 実働時にはfalseにすること
//	private static final String TAG = "AudioSamplerEncoder";

	private final boolean mSamplerCreated;
	private final IAudioSampler mSampler;
	private int frame_count = 0;

	public AudioSamplerEncoder(final IRecorder recorder, final EncoderListener listener,
							   final int audio_source, IAudioSampler sampler) {
		super(recorder, listener, audio_source,
			sampler != null ? sampler.getChannels() : 1,
			sampler != null ? sampler.getSamplingFrequency() : DEFAULT_SAMPLE_RATE,
			DEFAULT_BIT_RATE);
//		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		// もしAudioSamplerが指定されていない場合には内部で生成する
		if (sampler == null) {
			if (audio_source < MediaRecorder.AudioSource.DEFAULT
				|| audio_source > MediaRecorder.AudioSource.VOICE_COMMUNICATION)
				throw new IllegalArgumentException("invalid audio source:" + audio_source);
			sampler = new AudioSampler(audio_source, 1,
				DEFAULT_SAMPLE_RATE, SAMPLES_PER_FRAME, FRAMES_PER_BUFFER);
			mSamplerCreated = true;
		} else {
			mSamplerCreated = false;
		}
		mSampler = sampler;
	}

	@Override
	public void start() {
		super.start();
		mSampler.addCallback(mSoundSamplerCallback);
		if (mSamplerCreated)
			mSampler.start();
		new Thread(mAudioTask, "AudioTask").start();
	}

	@Override
	public void stop() {
		mSampler.removeCallback(mSoundSamplerCallback);
		if (mSamplerCreated)
			mSampler.stop();
		super.stop();
	}

	@Override
	public void release() {
		if (mSamplerCreated)
			mSampler.release();
		super.release();
	}

	/**
	 * AudioSampleからのコールバックリスナー
	 */
	private final AudioSampler.SoundSamplerCallback mSoundSamplerCallback
		= new AudioSampler.SoundSamplerCallback() {

		@Override
		public void onData(final ByteBuffer buffer, final int size, final long presentationTimeUs) {
    		synchronized (mSync) {
    			// 既に終了しているか終了指示が出てれば何もしない
        		if (!mIsCapturing || mRequestStop || mIsEOS) return;
    		}
			if (size > 0) {
				// 音声データを受け取った時はエンコーダーへ書き込む
				frameAvailableSoon();
				encode(buffer, size, presentationTimeUs);
				frame_count++;
			}
		}

		@Override
		public void onError(final Exception e) {
		}
	};

	/**
	 * エンコード処理が終了するまで待機するためのRunnable
	 * MediaMuxerを使ってエンコード済データの出力を行う場合に、
	 * １フレームも書き込めなかった場合にはMediaMuxerがクラッシュするので
	 * その場合にダミーのデータを書き込むために別スレッドを生成して待機する
	 */
	private final Runnable mAudioTask = new Runnable() {
		@Override
		public void run() {
			for (; ;) {
        		synchronized (mSync) {
            		if (!mIsCapturing || mRequestStop || mIsEOS) break;
            		try {
						mSync.wait();
					} catch (final InterruptedException e) {
						break;
					}
            	}
			}
			if (frame_count == 0) {
		    	// 1フレームも書き込めなかった時は動画出力時にMediaMuxerがクラッシュしないように
		    	// ダミーデータを書き込む
		    	final ByteBuffer buf = ByteBuffer.allocateDirect(SAMPLES_PER_FRAME).order(ByteOrder.nativeOrder());
		    	for (int i = 0; mIsCapturing && (i < 5); i++) {
		    		buf.clear();
					buf.position(SAMPLES_PER_FRAME);
					buf.flip();
					encode(buf, SAMPLES_PER_FRAME, getInputPTSUs());
					frameAvailableSoon();
					synchronized (this) {
						try {
							wait(50);
						} catch (final InterruptedException e) {
						}
					}
		    	}
			}
		}
	};
}
