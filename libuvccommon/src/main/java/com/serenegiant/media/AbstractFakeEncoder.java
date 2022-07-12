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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.utils.BufferHelper;
import com.serenegiant.utils.BuildCheck;
import com.serenegiant.utils.Time;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public abstract class AbstractFakeEncoder implements Encoder {

//	private static final boolean DEBUG = false;	// FIXME 実働時にはfalseにすること
	private static final String TAG = AbstractFakeEncoder.class.getSimpleName();

	@SuppressWarnings("deprecation")
	@SuppressLint("InlinedApi")
	public static final int BUFFER_FLAG_KEY_FRAME
		= BuildCheck.isLollipop()
			? MediaCodec.BUFFER_FLAG_KEY_FRAME : MediaCodec.BUFFER_FLAG_SYNC_FRAME;

	/**
	 * フレームプールの最大数
	 */
	private static final int DEFAULT_MAX_POOL_SZ = 8;
	/**
	 * フレームキューの最大数
	 */
	private static final int DEFAULT_MAX_QUEUE_SZ = 6;
	/**
	 * デフォルトのフレームサイズ
	 */
	private static final int DEFAULT_FRAME_SZ = 1024;
	/**
	 * フレームの待ち時間
	 */
	private static final long MAX_WAIT_FRAME_MS = 100;
	
	/**
	 * エンコード実行中フラグ
	 */
	private volatile boolean mIsCapturing;
	/**
	 * 終了要求フラグ(新規エンコード禁止フラグ)
	 */
	private volatile boolean mRequestStop;
	/**
	 * ファイルへの出力中フラグ
	 */
	private volatile boolean mRecorderStarted;
	/**
	 * キーフレーム待ち
	 */
	private volatile boolean mWaitingKeyFrame;
	/**
	 * 終了フラグ
	 */
	private boolean mIsEOS;
	/**
	 * トラックインデックス
	 */
	private int mTrackIndex;
	/**
	 * Recorderオブジェクトへの参照
	 */
	private IRecorder mRecorder;
	/**
	 * フラグの排他制御用
	 */
	private final Object mSync = new Object();
	/**
	 * エンコーダーイベントコールバックリスナー
	 */
	private final EncoderListener mListener;
	/**
	 * MIME
	 */
	private final String MIME_TYPE;
	/**
	 * フレームサイズ
	 */
	private final int FRAME_SZ;
	/**
	 * プールサイズ
	 */
	private final int MAX_POOL_SZ;
	/**
	 * フレームプール
	 */
	private final List<MediaData> mPool = new ArrayList<MediaData>();
	/**
	 * フレームキュー
	 */
	private final LinkedBlockingQueue<MediaData> mFrameQueue;
	/**
	 * フレーム情報(ワーク用)
	 */
	private final MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
	
	private Thread mDrainThread;
	
	/**
	 * コンストラクタ
	 * フレームデータのデフォルトサイズはDEFAULT_FRAME_SZ=1024バイト
	 * @param mimeType
	 * @param recorder
	 * @param listener
	 */
	public AbstractFakeEncoder(final String mimeType, @NonNull final IRecorder recorder,
		@NonNull final EncoderListener listener) {
		
		this(mimeType, recorder, listener, DEFAULT_FRAME_SZ,
			DEFAULT_MAX_POOL_SZ, DEFAULT_MAX_QUEUE_SZ);
	}
	
	/**
	 * コンストラクタ
	 * @param mimeType
	 * @param recorder
	 * @param listener
	 * @param frameSz
	 */
	public AbstractFakeEncoder(final String mimeType, @NonNull final IRecorder recorder,
		@NonNull final EncoderListener listener, final int frameSz) {
		
		this(mimeType, recorder, listener, frameSz,
			DEFAULT_MAX_POOL_SZ, DEFAULT_MAX_QUEUE_SZ);
	}
	
	/**
	 * コンストラクタ
 	 * @param mimeType
	 * @param recorder
	 * @param listener
	 * @param frameSz デフォルトで確保するフレームデータのサイズ
	 */
	public AbstractFakeEncoder(final String mimeType, @NonNull final IRecorder recorder,
		@NonNull final EncoderListener listener, final int frameSz,
		final int maxPoolSz, final int maxQueueSz) {
		
		MIME_TYPE = mimeType;
		FRAME_SZ = frameSz;
		MAX_POOL_SZ = maxPoolSz;
		mRecorder = recorder;
		mListener = listener;
		mFrameQueue = new LinkedBlockingQueue<MediaData>(maxQueueSz);
		
		recorder.addEncoder(this);
	}

	@Override
	protected void finalize() throws Throwable {
//		if (DEBUG) Log.v(TAG, "finalize:");
		release();
		super.finalize();
	}

	/**
	 * 出力用のIRecorderを返す
	 * @return
	 */
	public IRecorder getRecorder() {
		return mRecorder;
	}

	public boolean isRecorderStarted() {
		return mRecorderStarted;
	}
	
	/**
	 * 出力ファイルのパスを返す
	 * @return
	 */
	@Override
	public String getOutputPath() {
		return mRecorder != null ? mRecorder.getOutputPath() : null;
	}

	/**
	 * 子クラスでOverrideした時でもEncoder#releaseを呼び出すこと
	 */
	@Override
	public  synchronized void release() {
//		if (DEBUG) Log.v(TAG, "release:");
		try {
			if (mDrainThread != null) {
				mDrainThread.interrupt();
			}
		} catch (final Exception e) {
			Log.w(TAG, e);
		}
		mDrainThread = null;
		if (mRecorder != null) {
			internalRelease();
		}
//		if (DEBUG) Log.v(TAG, "release:finished");
	}

	/**
	 * ストリーミング終了指示を送る
	 */
	@Override
	public  void signalEndOfInputStream() {
//		if (DEBUG) Log.i(TAG, "signalEndOfInputStream:encoder=" + this);
        // MediaCodec#signalEndOfInputStreamはBUFFER_FLAG_END_OF_STREAMフラグを付けて
        // 空のバッファをセットするのと等価である
    	// ・・・らしいので空バッファを送る。
    	final MediaData frame = obtain(0);
    	frame.set(null, 0, 0, getInputPTSUs(), MediaCodec.BUFFER_FLAG_END_OF_STREAM);
    	offer(frame);
	}

	/**
	 * このクラスではサポートしていない。
	 * 代わりに#queueFrameへエンコード済みのフレームを渡すこと
	 * encodeはnative側からアクセスするので変更時は注意
	 * @param buffer
	 */
	@Override
	public void encode(final ByteBuffer buffer) {
		throw new UnsupportedOperationException("can not call encode");
	}
	
	/**
	 * このクラスではサポートしていない。
	 * 代わりに#queueFrameへエンコード済みのフレームを渡すこと
	 * @param buffer
	 * @param length
	 * @param presentationTimeUs
	 */
	@Override
	public void encode(final ByteBuffer buffer, final int length,
		final long presentationTimeUs) {
		throw new UnsupportedOperationException("can not call encode");
	}
	
	/**
	 * フレームデータをキューに追加する
	 * @param buffer
	 * @param offset
	 * @param size
	 * @param presentationTimeUs
	 * @param flags
	 * @return true: 正常にキューに追加できた
	 * @throws IllegalStateException
	 */
	public boolean queueFrame(@Nullable final ByteBuffer buffer,
		final int offset, final int size,
		final long presentationTimeUs, final int flags) throws IllegalStateException {
		
		if (!mIsCapturing) {
			throw new IllegalStateException();
		}
		if (mRequestStop) return false;
		final MediaData frame = obtain(size);
		frame.set(buffer, offset, size, presentationTimeUs, flags);
		return offer(frame);
	}
	
	@Override
	public boolean isCapturing() {
		return mIsCapturing;
	}
	
	/**
	 * エンコードの準備(IRecorderから呼び出される)
	 * @throws Exception
	 */
	@Override
	public void prepare() throws Exception {
//		if (DEBUG) Log.v(TAG, "prepare:");
		mTrackIndex = -1;
		mRecorderStarted = false;
		mIsCapturing = mWaitingKeyFrame = true;
		mRequestStop = mIsEOS = false;
		callOnStartEncode(null, -1, false);
	}

	/**
	 * エンコード開始要求(IRecorderから呼び出される)
	 */
	@Override
	public void start() {
//		if (DEBUG) Log.v(TAG, "start:");
		synchronized (mSync) {
			if (mIsCapturing && !mRequestStop) {
				initPool();
				// フレーム処理スレッドを生成＆起床
				mDrainThread = new Thread(mDrainTask, getClass().getSimpleName());
				mDrainThread.start();
				try {
					mSync.wait();	// エンコーダースレッド起床待ち
				} catch (final InterruptedException e) {
					// ignore
				}
			}
		}
//		if (DEBUG) Log.v(TAG, "start:finished");
	}
	
	/**
	 * エンコーダ終了要求(IRecorderから呼び出される)
	 */
	@Override
	public void stop() {
//		if (DEBUG) Log.v(TAG, "stop:mRequestStop=" + mRequestStop);
		synchronized (mSync) {
			if (/*!mIsCapturing ||*/ mRequestStop) {
				return;
			}
			// 終了要求
			mRequestStop = true;	// 新規のフレームを受けないようにする
			signalEndOfInputStream();
			mSync.notifyAll();
		}
		// 本当のところいつ終了するのかはわからないので、
		// 呼び出し元スレッドを遅延させないために終了待ちせずに直ぐに返る
//		if (DEBUG) Log.v(TAG, "stop:finished");
	}

	/**
	 * フレームデータの読込み準備要求
	 * native側からも呼び出されるので名前を変えちゃダメ
	 */
	@Override
	public void frameAvailableSoon() {
//		if (DEBUG) Log.v(TAG, "frameAvailableSoon:");
		synchronized (mSync) {
			if (!mIsCapturing || mRequestStop) {
				return;
			}
			mSync.notifyAll();
		}
	}

//================================================================================
	/**
	 * prepareの最後に呼び出すこと
	 * @param source
	 * @param captureFormat
	 */
	protected void callOnStartEncode(final Surface source,
		final int captureFormat, final boolean mayFail) {
		
//		if (DEBUG) Log.v(TAG, "callOnStartEncode:mListener=" + mListener);
		try {
			mListener.onStartEncode(this, source, captureFormat, mayFail);
		} catch (final Exception e) {
			Log.w(TAG, e);
		}
	}

	/**
	 * エラー発生時に呼び出す
	 * @param e
	 */
	protected void callOnError(final Exception e) {
		try {
			mListener.onError(e);
		} catch (final Exception e2) {
			Log.w(TAG, e2);
		}
	}

//================================================================================
	/**
	 * フレームプールを初期化する
	 */
	protected void initPool() {
//		if (DEBUG) Log.v(TAG, "initPool:");
		mFrameQueue.clear();
		synchronized (mPool) {
			mPool.clear();
			for (int i = 0; i < MAX_POOL_SZ; i++) {
				mPool.add(new MediaData(FRAME_SZ));
			}
		}
	}
	
	/**
	 * フレームプールとキューを空にする
	 */
	protected void clearFrames() {
//		if (DEBUG) Log.v(TAG, "clearFrames:");
		synchronized (mPool) {
			mPool.clear();
		}
		mFrameQueue.clear();
		cnt = 0;
	}
	
	/**
	 * 生成したフレームの数
	 */
	private int cnt = 0;
	
	/**
	 * フレームプールからフレームを取得する
	 * @param newSize
	 * @return
	 */
	protected MediaData obtain(final int newSize) {
		MediaData result;
		synchronized (mPool) {
			if (mPool.isEmpty()) {
				cnt++;
//				if (DEBUG) Log.v(TAG, "obtain:create new FrameData, total=" + cnt);
				result = new MediaData(FRAME_SZ);
			} else {
				result = mPool.remove(mPool.size() - 1);
				result.resize(newSize);
			}
		}
		return result;
	}
	
	/**
	 * フレームキューにフレームデータを追加する
	 * @param frame
	 */
	protected boolean offer(@NonNull final MediaData frame) {
		boolean result = mFrameQueue.offer(frame);
		if (!result) {
//			if (DEBUG) Log.w(TAG, "offer:先頭を破棄する");
		    final MediaData head = mFrameQueue.poll();
			result = mFrameQueue.offer(frame);
//			if (!result) {
//				if (DEBUG) Log.w(TAG, "offer:frame dropped");
//			}
			if (head != null) {
				recycle(head);
			}
		}
		return result;
	}
	
	/**
	 * フレームキューからフレームデータを取り出す
	 * フレームキューが空ならブロックする
	 * @param waitTimeMs 最大待ち時間[ミリ秒]
	 * @return
	 */
	protected MediaData waitFrame(final long waitTimeMs) {
//		if (DEBUG) Log.v(TAG, "waitFrame:");
		MediaData result = null;
		try {
			result = mFrameQueue.poll(waitTimeMs, TimeUnit.MILLISECONDS);
		} catch (final InterruptedException e) {
			// ignore
		}
//		if (DEBUG) Log.v(TAG, "waitFrame:result=" + result);
		return result;
	}
	
	/**
	 * フレームプールにフレームを戻す
	 * @param frame
	 */
	protected void recycle(@NonNull final MediaData frame) {
//		if (DEBUG) Log.v(TAG, "recycle:");
		synchronized (mPool) {
			if (mPool.size() < MAX_POOL_SZ) {
				mPool.add(frame);
			} else {
//				if (DEBUG) Log.v(TAG, "recycle:フレームプールがいっぱいで戻せなかった");
				cnt--;
			}
		}
	}
//================================================================================
	/**
	 * フレーム処理ループの実体
	 */
	private final Runnable mDrainTask = new Runnable() {
		@Override
		public void run() {
//			if (DEBUG) Log.v(TAG, "mDrainTask:");
			synchronized (mSync) {
				mRequestStop = false;
				mSync.notify();
			}
			for ( ; mIsCapturing ; ) {
				final MediaData frame = waitFrame(MAX_WAIT_FRAME_MS);
				if (frame != null) {
					try {
						if (mIsCapturing) {
							handleFrame(frame);
						}
					} finally {
						recycle(frame);
					}
				}
			} // end of while
			synchronized (mSync) {
				mRequestStop = true;
				mIsCapturing = false;
			}
			mDrainThread = null;
//			if (DEBUG) Log.v(TAG, "mDrainTask:finished");
		}
	};
	
	/**
	 * 1フレーム分の処理
	 * @param frame
	 */
	protected void handleFrame(final MediaData frame) {
//		if (DEBUG) Log.v(TAG, "handleFrame:");
		final IRecorder recorder = mRecorder;
		if (recorder == null) {
//			throw new NullPointerException("muxer is unexpectedly null");
			Log.w(TAG, "muxer is unexpectedly null");
			return;
		}
		mBufferInfo.set(0, frame.size, frame.presentationTimeUs, frame.flags);
		final boolean isKeyFrame
			= ((mBufferInfo.flags & BUFFER_FLAG_KEY_FRAME) == BUFFER_FLAG_KEY_FRAME);

		if (!mRecorderStarted
			&& (isKeyFrame
				|| ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0)) ) {
//			if (DEBUG) Log.d(TAG, "handleFrame:BUFFER_FLAG_KEY_FRAME");
			// csd-0とcsd-1が同時に来ているはずなので分離してセットする
			final byte[] tmp = new byte[mBufferInfo.size];
			final ByteBuffer b = frame.mBuffer.duplicate();
			b.clear();
			b.get(tmp, 0, mBufferInfo.size);
			final int ix0 = BufferHelper.findAnnexB(tmp, 0);
			final int ix1 = BufferHelper.findAnnexB(tmp, ix0 + 2);
			final int ix2 = BufferHelper.findAnnexB(tmp, ix1 + 2);
//			if (DEBUG) Log.i(TAG, String.format("ix0=%d,ix1=%d,ix2=%d", ix0, ix1, ix2));
			try {
				final MediaFormat outFormat = createOutputFormat(MIME_TYPE,
					tmp, mBufferInfo.size, ix0, ix1, ix2);
				if (!startRecorder(recorder, outFormat)) {
					Log.w(TAG, "handleFrame:failed to start recorder");
					return;
				}
			} catch (final Exception e) {
				return;
			}
		}
		if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
			mBufferInfo.size = 0;
		}
		
		// レコーダーの初期化済み＆フレームデータがある＆キーフレーム待機中でないかキーフレームが来たとき
		if (mRecorderStarted && (mBufferInfo.size != 0)
			&& (isKeyFrame || !mWaitingKeyFrame)) {

			mWaitingKeyFrame = false;
			try {
				mBufferInfo.presentationTimeUs = getNextOutputPTSUs(mBufferInfo.presentationTimeUs);
				recorder.writeSampleData(mTrackIndex, frame.mBuffer, mBufferInfo);
			 } catch (final TimeoutException e) {
//				if (DEBUG) Log.v(TAG, "最大録画時間を超えた", e);
				recorder.stopRecording();
			} catch (final Exception e) {
//				if (DEBUG) Log.w(TAG, e);
				recorder.stopRecording();
			}
		}
		if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
//			if (DEBUG) Log.v(TAG, "ストリーム終了指示が来た");
			stopRecorder(recorder);
		}
//		if (DEBUG) Log.v(TAG, "handleFrame:finished");
	}
	
	/**
	 * Muxer初期化用のMediaFormatを生成する
	 * @param csd
	 * @param size
	 * @param ix0
	 * @param ix1
	 * @param ix2
	 * @return
	 */
	protected abstract MediaFormat createOutputFormat(final String mime,
		final byte[] csd, final int size,
		final int ix0, final int ix1, final int ix2);

	/**
	 * コーデックからの出力フォーマットを取得してnative側へ引き渡してIRecorderをスタートさせる
	 */
	protected boolean startRecorder(final IRecorder recorder, final MediaFormat outFormat) {
//		if (DEBUG) Log.i(TAG, "startRecorder:outFormat=" + outFormat);
		mTrackIndex = recorder.addTrack(this, outFormat);
		if (mTrackIndex >= 0) {
			mRecorderStarted = true;
			if (!recorder.start(this)) {
				// startは全てのエンコーダーがstartを呼ぶまで返ってこない
				// falseを返した時は何らかの異常でmuxerをスタート出来てない。
				Log.e(TAG, "failed to start muxer mTrackIndex=" + mTrackIndex);
			}
    	} else {
			Log.e(TAG, "failed to addTrack: mTrackIndex=" + mTrackIndex);
			recorder.removeEncoder(this);
		}
		return recorder.isStarted();
	}
	
	/**
	 * BUFFER_FLAG_END_OF_STREAMを受け取った時の処理、IRecorderを終了させる。
	 * @param recorder
	 */
	protected void stopRecorder(final IRecorder recorder) {
//		if (DEBUG) Log.d(TAG, "stopRecorder:mRecorder=" + mRecorder);
		if (mRecorder != null) {
			internalRelease();
		}
	}

	private void internalRelease() {
//		if (DEBUG) Log.d(TAG, "internalRelease:");
		mIsEOS = true;
		if (mIsCapturing) {
			mIsCapturing = false;
			try {
				mListener.onStopEncode(this);
			} catch (final Exception e) {
				Log.e(TAG, "failed onStopped", e);
			}
		}
		if (mRecorderStarted) {
			mRecorderStarted = false;
			if (mRecorder != null) {
				try {
//					if (DEBUG) Log.v(TAG, "call IRecorder#stop");
					mRecorder.stop(this);
				} catch (final Exception e) {
					Log.e(TAG, "failed stopping muxer", e);
				}
			}
		}
		try {
			mListener.onDestroy(this);
		} catch (final Exception e) {
			Log.e(TAG, "destroy:", e);
		}
		mRecorder = null;
		clearFrames();
//		if (DEBUG) Log.d(TAG, "internalRelease:finished");
	}

//================================================================================
	/**
	 * 前回MediaCodecへのエンコード時に使ったpresentationTimeUs
	 */
	private long prevInputPTSUs = -1;

	/**
	 * 今回の書き込み用のpresentationTimeUs値を取得
	 * @return
	*/
	protected long getInputPTSUs() {
		long result = Time.nanoTime() / 1000L;
		// 以前の書き込みよりも値が小さくなるとエラーになるのでオフセットをかける
		if (result <= prevInputPTSUs) {
			result = prevInputPTSUs + 9643;
		}
		prevInputPTSUs = result;
		return result;
	}

	/**
	 * 前回Recorderに書き込んだ際のpresentationTimeUs
	 */
	private long prevOutputPTSUs = -1;

	/**
	 * Muxerの今回の書き込み用のpresentationTimeUs値を取得
	 * @return
	 */
	protected long getNextOutputPTSUs(long presentationTimeUs) {
		// 以前の書き込みよりも値が小さくなるとエラーになるのでオフセットをかける
		if (presentationTimeUs <= prevOutputPTSUs) {
			presentationTimeUs = prevOutputPTSUs + 9643;
		}
		prevOutputPTSUs = presentationTimeUs;
		return presentationTimeUs;
	}

}
