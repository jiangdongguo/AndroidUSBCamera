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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import android.util.Log;

import com.serenegiant.utils.BuildCheck;
import com.serenegiant.utils.Time;

public abstract class IAudioSampler {
//	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private final String TAG = getClass().getSimpleName();

	public static final int AUDIO_SOURCE_UAC = 100;
	@IntDef({
		MediaRecorder.AudioSource.DEFAULT,
		MediaRecorder.AudioSource.MIC,
		MediaRecorder.AudioSource.CAMCORDER,
		MediaRecorder.AudioSource.VOICE_RECOGNITION,
		MediaRecorder.AudioSource.VOICE_COMMUNICATION,
		AUDIO_SOURCE_UAC,
	})
	@Retention(RetentionPolicy.SOURCE)
	public @interface AudioSource {}

	@SuppressLint("NewApi")
	public static AudioRecord createAudioRecord(
		final int source, final int sampling_rate, final int channels, final int format, final int buffer_size) {

		@AudioSource
		final int[] AUDIO_SOURCES = new int[] {
			MediaRecorder.AudioSource.DEFAULT,		// ここ(1つ目)は引数で置き換えられる
			MediaRecorder.AudioSource.CAMCORDER,	// これにするとUSBオーディオルーティングが有効な場合でも内蔵マイクからの音になる
			MediaRecorder.AudioSource.MIC,
			MediaRecorder.AudioSource.DEFAULT,
			MediaRecorder.AudioSource.VOICE_COMMUNICATION,
			MediaRecorder.AudioSource.VOICE_RECOGNITION,
		};

		switch (source) {
		case 1:	AUDIO_SOURCES[0] = MediaRecorder.AudioSource.MIC; break;		// 自動
		case 2:	AUDIO_SOURCES[0] = MediaRecorder.AudioSource.CAMCORDER; break;	// 内蔵マイク
		case 3: AUDIO_SOURCES[0] = MediaRecorder.AudioSource.VOICE_COMMUNICATION; break;
		default:AUDIO_SOURCES[0] = MediaRecorder.AudioSource.MIC; break;		// 自動(UACのopenに失敗した時など)
		}
		AudioRecord audioRecord = null;
		for (final int src: AUDIO_SOURCES) {
            try {
            	if (BuildCheck.isAndroid6()) {
					audioRecord = new AudioRecord.Builder()
						.setAudioSource(src)
						.setAudioFormat(new AudioFormat.Builder()
							.setEncoding(format)
							.setSampleRate(sampling_rate)
							.setChannelMask((channels == 1
								? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO))
							.build())
						.setBufferSizeInBytes(buffer_size)
						.build();
				} else {
					audioRecord = new AudioRecord(src, sampling_rate,
						(channels == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO),
						format, buffer_size);
				}
				if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
					audioRecord.release();
					audioRecord = null;
				}
            } catch (final Exception e) {
            	audioRecord = null;
            }
            if (audioRecord != null)
            	break;
    	}
		return audioRecord;
	}

	/**
	 * 音声データ取得コールバックインターフェース
	 */
	public interface SoundSamplerCallback {
		/**
		 * 音声データが準備出来た時に呼び出される。
		 * presentationTimeUsは音声データを取得した時の時刻だが、他のコールバックでの処理によっては
		 * 遅延して呼び出される可能性がある。任意のスレッド上で実行される。
		 * 可能な限り早く処理を終えること。
		 * @param buffer
		 * @param size
		 * @param presentationTimeUs
		 */
		public void onData(ByteBuffer buffer, int size, long presentationTimeUs);
		/**
		 * エラーが起こった時の処理(今は未使用)
		 * @param e
		 */
		public void onError(Exception e);
	}

	/**
	 * バッファリング用に生成する音声データレコードの最大生成する
	 */
	private final int MAX_POOL_SIZE = 200;
	/**
	 * 音声データキューに存在できる最大音声データレコード数
	 * 25フレーム/秒のはずなので最大で約4秒分
	 */
	private final int MAX_QUEUE_SIZE = 200;

	// 音声データキュー用
	private final LinkedBlockingQueue<MediaData> mPool = new LinkedBlockingQueue<MediaData>(MAX_POOL_SIZE);
	private final LinkedBlockingQueue<MediaData> mAudioQueue = new LinkedBlockingQueue<MediaData>(MAX_QUEUE_SIZE);

	// コールバック用
	private CallbackThread mCallbackThread;
	private final Object mCallbackSync = new Object();
	private final Set<SoundSamplerCallback> mCallbacks = new CopyOnWriteArraySet<SoundSamplerCallback>();
	protected volatile boolean mIsCapturing;

	public IAudioSampler() {
	}

	/**
	 * 音声データのサンプリングを停止して全てのコールバックを削除する
	 */
	public void release() {
//		if (DEBUG) Log.v(TAG, "release:mIsCapturing=" + mIsCapturing);
		if (isStarted()) {
			stop();
		}
//		mIsCapturing = false;	// 念の為に
		mCallbacks.clear();
//		if (DEBUG) Log.v(TAG, "release:finished");
	}

	/**
	 * 音声サンプリング開始
	 */
	public synchronized void start() {
//		if (DEBUG) Log.v(TAG, "start:");
		// コールバック用スレッドを生成＆実行
		synchronized (mCallbackSync) {
			if (mCallbackThread == null) {
				mIsCapturing = true;
				mCallbackThread = new CallbackThread();
				mCallbackThread.start();
			}
		}
//		if (DEBUG) Log.v(TAG, "start:finished");
	}

	/**
	 * 音声サンプリング終了
	 */
	public synchronized void stop() {
//		if (DEBUG) Log.v(TAG, "stop:");
//		new Throwable().printStackTrace();
		synchronized (mCallbackSync) {
			final boolean capturing = mIsCapturing;
			mIsCapturing = false;
			mCallbackThread = null;
			if (capturing) {
				try {
					mCallbackSync.wait();
				} catch (InterruptedException e) {
				}
			}
		}
//		if (DEBUG) Log.v(TAG, "stop:finished");
	}

	/**
	 * コールバックを追加する
	 * @param callback
	 */
	public void addCallback(final SoundSamplerCallback callback) {
		if (callback != null) {
			mCallbacks.add(callback);
		}
	}

	/**
	 * コールバックを削除する
	 * @param callback
	 */
	public void removeCallback(final SoundSamplerCallback callback) {
		if (callback != null) {
			for (; mCallbacks.remove(callback); );
		}
	}

	/**
	 * 音声データのサンプリング中かどうかを返す
	 * @return
	 */
	public boolean isStarted() {
		return mIsCapturing;
	}

	/**
	 * 音声入力ソースを返す
	 * 100以上ならUAC
	 * @return
	 */
	public abstract int getAudioSource();
	/**
	 * チャネル数を返す
	 * @return
	 */
	public abstract int getChannels();
	/**
	 * サンプリング周波数を返す
	 * @return
	 */
	public abstract int getSamplingFrequency();
	/**
	 *PCMエンコードの解像度(ビット数)を返す。8か16
	 * @return
	 */
	public abstract int getBitResolution();
	/**
	 * 音声データ１つ当たりのバイト数を返す
	 * @return
	 */
	public int getBufferSize() {
		return mDefaultBufferSize;
	}

	/**
	 * 音声データ取得時のコールバックを呼び出す
	 * @param data
	 */
	private void callOnData(@NonNull final MediaData data) {
		final ByteBuffer buf = data.mBuffer;
		final int size = data.size;
		final long pts = data.presentationTimeUs;
		for (final SoundSamplerCallback callback: mCallbacks) {
			try {
				buf.clear();
				buf.position(size);
				buf.flip();
				callback.onData(buf, size, pts);
			} catch (final Exception e) {
				mCallbacks.remove(callback);
				Log.w(TAG, "callOnData:", e);
			}
		}
    }

	/**
	 * エラー発生時のコールバックを呼び出す
	 * @param e
	 */
    protected void callOnError(final Exception e) {
		for (final SoundSamplerCallback callback: mCallbacks) {
			try {
				callback.onError(e);
			} catch (final Exception e1) {
				mCallbacks.remove(callback);
				Log.w(TAG, "callOnError:", e1);
			}
		}
    }

	protected int mDefaultBufferSize = 1024;
	protected void init_pool(final int default_buffer_size) {
		mDefaultBufferSize = default_buffer_size;
		mAudioQueue.clear();
		mPool.clear();
		for (int i = 0; i < 8; i++) {
			mPool.add(new MediaData(default_buffer_size));
		}
	}

	/**
	 * このクラスで生成した音声データバッファの数
	 */
	private int mBufferNum = 0;
	/**
	 * 音声データバッファをプールから取得する。
	 * プールがからの場合には最大MAX_POOL_SIZE個までは新規生成する
	 * @return
	 */
	protected MediaData obtain() {
//		if (DEBUG) Log.v(TAG, "obtain:" + mPool.size() + ",mBufferNum=" + mBufferNum);
		MediaData result = null;
		if (!mPool.isEmpty()) {
			// プールに空バッファが有る時
			result = mPool.poll();
		} else if (mBufferNum < MAX_POOL_SIZE) {
//			if (DEBUG) Log.i(TAG, "create MediaData");
			result = new MediaData(mDefaultBufferSize);
			mBufferNum++;
		}
		if (result != null)
			result.size = 0;
//		if (DEBUG) Log.v(TAG, "obtain:result=" + result);
		return result;
	}

	/**
	 * 使用済みの音声データバッファを再利用するためにプールに戻す
	 * @param data
	 */
	protected void recycle(@NonNull final MediaData data) {
//		if (DEBUG) Log.v(TAG, "recycle:" + mPool.size());
		if (!mPool.offer(data)) {
			// ここには来ないはず
//			if (DEBUG) Log.i(TAG, "pool is full");
			mBufferNum--;
		}
	}

	protected boolean addMediaData(@NonNull final MediaData data) {
//		if (DEBUG) Log.v(TAG, "addMediaData:" + mAudioQueue.size());
		return mAudioQueue.offer(data);
	}

	protected MediaData pollMediaData(final long timeout_msec) throws InterruptedException {
		return mAudioQueue.poll(timeout_msec, TimeUnit.MILLISECONDS);
	}

	/**
	 * 前回MediaCodecへのエンコード時に使ったpresentationTimeUs
	 */
	private long prevInputPTSUs = -1;

	/**
	 * 今回の書き込み用のpresentationTimeUs値を取得
	 * @return
	 */
    @SuppressLint("NewApi")
	protected long getInputPTSUs() {
		long result = Time.nanoTime() / 1000L;
		if (result <= prevInputPTSUs) {
			result = prevInputPTSUs + 9643;
		}
		prevInputPTSUs = result;
		return result;
    }

    /**
     * キューから音声データを取り出してコールバックを呼び出すためのスレッド
     */
    private final class CallbackThread extends Thread {
    	public CallbackThread() {
    		super("AudioSampler");
    	}

    	@Override
    	public final void run() {
//    		if (DEBUG) Log.i(TAG, "CallbackThread:start");
    		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO); // THREAD_PRIORITY_URGENT_AUDIO
			MediaData data;
    		for (; mIsCapturing ;) {
    			try {
					data = pollMediaData(100);
				} catch (final InterruptedException e) {
					break;
				}
    			if (data != null) {
    				callOnData(data);
    				// 使用済みのバッファをプールに戻して再利用する
    				recycle(data);
    			}
    		} // for (; mIsCapturing ;)
    		synchronized (mCallbackSync) {
				mCallbackSync.notifyAll();
			}
//    		if (DEBUG) Log.i(TAG, "CallbackThread:finished");
    	}
    }

}
