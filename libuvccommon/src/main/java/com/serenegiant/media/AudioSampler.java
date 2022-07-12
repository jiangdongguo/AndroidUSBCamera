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

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.util.Log;

/**
 * AudioRecordを使って音声データを取得し、登録したコールバックへ分配するためのクラス
 * 同じ音声入力ソースに対して複数のAudioRecordを生成するとエラーになるのでシングルトン的にアクセス出来るようにするため
 */
public class AudioSampler extends IAudioSampler {
//	private static final boolean DEBUG = false;
	private static final String TAG = AudioSampler.class.getSimpleName();

	private AudioThread mAudioThread;
    private final int AUDIO_SOURCE;
    private final int SAMPLING_RATE, CHANNEL_COUNT;
	private final int SAMPLES_PER_FRAME;
	private final int BUFFER_SIZE;
	private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

	public AudioSampler(final int audio_source, final int channel_num,
		final int sampling_rate, final int samples_per_frame, final int frames_per_buffer) {

		super();
//		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		// パラメータを保存
		AUDIO_SOURCE = audio_source;
		CHANNEL_COUNT = channel_num;
		SAMPLING_RATE = sampling_rate;
		SAMPLES_PER_FRAME = samples_per_frame * channel_num;
		BUFFER_SIZE = getAudioBufferSize(channel_num, sampling_rate, samples_per_frame, frames_per_buffer);
	}

	/**
	 * 音声データ１つ当たりのバイト数を返す
	 * @return
	 */
	@Override
	public int getBufferSize() {
		return SAMPLES_PER_FRAME;
	}

	/**
	 * 音声データサンプリング開始
	 * 実際の処理は別スレッド上で実行される
	 */
	@Override
	public synchronized void start() {
//		if (DEBUG) Log.v(TAG, "start:mIsCapturing=" + mIsCapturing);
		super.start();
		if (mAudioThread == null) {
			init_pool(SAMPLES_PER_FRAME);
			// 内蔵マイクからの音声取り込みスレッド生成＆実行
	        mAudioThread = new AudioThread();
			mAudioThread.start();
		}
	}

	/**
	 * 音声データのサンプリングを停止させる
	 */
	@Override
	public synchronized void stop() {
//		if (DEBUG) Log.v(TAG, "stop:mIsCapturing=" + mIsCapturing);
		mIsCapturing = false;
		mAudioThread = null;
		super.stop();
	}

	@Override
	public int getAudioSource() {
		return AUDIO_SOURCE;
	}

	protected static final class AudioRecordRec {
		AudioRecord audioRecord;
		int bufferSize;
	}

	/**
	 * AudioRecorder初期化時に使用するバッファサイズを計算
	 * @param channel_num
	 * @param sampling_rate
	 * @param samples_per_frame
	 * @param frames_per_buffer
	 * @return
	 */
	public static int getAudioBufferSize(final int channel_num,
			final int sampling_rate, final int samples_per_frame, final int frames_per_buffer) {
		final int min_buffer_size = AudioRecord.getMinBufferSize(sampling_rate,
			(channel_num == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO),
			AUDIO_FORMAT);
		int buffer_size = samples_per_frame * frames_per_buffer;
		if (buffer_size < min_buffer_size)
			buffer_size = ((min_buffer_size / samples_per_frame) + 1) * samples_per_frame * 2 * channel_num;
		return buffer_size;
	}

	/**
	 * AudioRecordから無圧縮PCM16bitで内蔵マイクからの音声データを取得してキューへ書き込むためのスレッド
	 */
    private final class AudioThread extends Thread {

    	public AudioThread() {
    		super("AudioThread");
    	}

    	@Override
    	public final void run() {
//    		if (DEBUG) Log.v(TAG, "AudioThread:start");
    		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO); // THREAD_PRIORITY_URGENT_AUDIO
//    		if (DEBUG) Log.v(TAG, getName() + " started");
/*			final Class audioSystemClass = Class.forName("android.media.AudioSystem");
			// will disable the headphone
			setDeviceConnectionState.Invoke(audioSystemClass, (Integer)DEVICE_OUT_WIRED_HEADPHONE, (Integer)DEVICE_STATE_UNAVAILABLE, new String(""));
			// will enable the headphone
			setDeviceConnectionState.Invoke(audioSystemClass, (Integer)DEVICE_OUT_WIRED_HEADPHONE, (Integer)DEVICE_STATE_AVAILABLE, new Lang.String(""));
*/
			int retry = 3;
RETRY_LOOP:	for ( ; mIsCapturing && (retry > 0) ; ) {
				final AudioRecord audioRecord = createAudioRecord(
					AUDIO_SOURCE, SAMPLING_RATE, CHANNEL_COUNT, AUDIO_FORMAT, BUFFER_SIZE);
				int err_count = 0;
				if (audioRecord != null) {
					try {
						if (mIsCapturing) {
//		        			if (DEBUG) Log.v(TAG, "AudioThread:start audio recording");
							int readBytes;
							ByteBuffer buffer;
							audioRecord.startRecording();
							try {
								MediaData data;
LOOP:							for ( ; mIsCapturing ;) {
									data = obtain();
									if (data != null) {
										// check recording state
										final int recordingState = audioRecord.getRecordingState();
										if (recordingState
											!= AudioRecord.RECORDSTATE_RECORDING) {

											if (err_count == 0) {
												Log.e(TAG, "not a recording state," + recordingState);
											}
											err_count++;
											recycle(data);
											if (err_count > 20) {
												retry--;
												break LOOP;
											} else {
												Thread.sleep(100);
												continue;
											}
										}
										// try to read audio data
										buffer = data.mBuffer;
										buffer.clear();
										// 1回に読み込むのはSAMPLES_PER_FRAMEバイト
										try {
											readBytes = audioRecord.read(buffer, SAMPLES_PER_FRAME);
										} catch (final Exception e) {
											Log.e(TAG, "AudioRecord#read failed:" + e);
											err_count++;
											retry--;
											recycle(data);
											callOnError(e);
											break LOOP;
										}
										if (readBytes > 0) {
											// 正常に読み込めた時
											err_count = 0;
											data.presentationTimeUs = getInputPTSUs();
											data.size = readBytes;
											buffer.position(readBytes);
											buffer.flip();
											// 音声データキューに追加する
											addMediaData(data);
											continue;
										} else if (readBytes == AudioRecord.SUCCESS) {	// == 0
											err_count = 0;
											recycle(data);
											continue;
										} else if (readBytes == AudioRecord.ERROR) {
											if (err_count == 0) {
												Log.e(TAG, "Read error ERROR");
											}
										} else if (readBytes == AudioRecord.ERROR_BAD_VALUE) {
											if (err_count == 0) {
												Log.e(TAG, "Read error ERROR_BAD_VALUE");
											}
										} else if (readBytes == AudioRecord.ERROR_INVALID_OPERATION) {
											if (err_count == 0) {
												Log.e(TAG, "Read error ERROR_INVALID_OPERATION");
											}
										} else if (readBytes == AudioRecord.ERROR_DEAD_OBJECT) {
											Log.e(TAG, "Read error ERROR_DEAD_OBJECT");
											err_count++;
											retry--;
											recycle(data);
											break LOOP;
										} else if (readBytes < 0) {
											if (err_count == 0) {
												Log.e(TAG, "Read returned unknown err " + readBytes);
											}
										}
										err_count++;
										recycle(data);
									} // end of if (data != null)
									if (err_count > 10) {
										retry--;
										break LOOP;
									}
								} // end of for ( ; mIsCapturing ;)
							} finally {
								audioRecord.stop();
							}
						}
					} catch (final Exception e) {
//	        			Log.w(TAG, "exception on AudioRecord:", e);
						retry--;
						callOnError(e);
					} finally {
						audioRecord.release();
					}
					if (mIsCapturing && (err_count > 0) && (retry > 0)) {
						// キャプチャリング中でエラーからのリカバリー処理が必要なときは0.5秒待機
						for (int i = 0; mIsCapturing && (i < 5); i++) {
							try {
								Thread.sleep(100);
							} catch (final InterruptedException e) {
								break RETRY_LOOP;
							}
						}
					}
				} else {
//        			Log.w(TAG, "AudioRecord failed to initialize");
					callOnError(new RuntimeException("AudioRecord failed to initialize"));
					retry = 0;	// 初期化できんかったときはリトライしない
				}
			}	// end of for
			AudioSampler.this.stop();
//    		if (DEBUG) Log.v(TAG, "AudioThread:finished");
    	} // #run
    }

	@Override
	public int getChannels() {
		return CHANNEL_COUNT;
	}

	@Override
	public int getSamplingFrequency() {
		return SAMPLING_RATE;
	}

	@Override
	public int getBitResolution() {
		return 16;	// AudioFormat.ENCODING_PCM_16BIT
	}

}
