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

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.utils.BuildCheck;
import com.serenegiant.utils.UriHelper;

public abstract class Recorder implements IRecorder {
//	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = Recorder.class.getSimpleName();

	public static final long CHECK_INTERVAL = 45 * 1000L;	// 空き容量,EOSのチェクする間隔[ミリ秒](=45秒)

	private final RecorderCallback mCallback;
	protected IMuxer mMuxer;
	private volatile int mEncoderCount, mStartedCount;
	@RecorderState
	private int mState;
	protected Encoder mVideoEncoder;
	protected Encoder mAudioEncoder;
	private volatile boolean mVideoStarted, mAudioStarted;
    // 別スレッドで、一定時間毎に空き容量チェック＆一定時間後に終了指示を送るためのハンドラー
    // エンコーダー自体のスレッドで遅延メッセージを送ると30秒のつもりが46秒とか掛かっちゃうから
    private EosHandler mEosHandler;
    protected long mStartTime;
    private volatile boolean mReleased;

	/**
	 * コンストラクタ
	 */
	public Recorder(final RecorderCallback callback) {
		mCallback = callback;
		synchronized(this) {
			mState = STATE_UNINITIALIZED;
		}
	}

	/* (non-Javadoc)
	 * @see com.serenegiant.media.IRecorder#setMuxer(com.serenegiant.media.IMuxer)
	 */
	@Override
	public void setMuxer(final IMuxer muxer) {
		if (!mReleased) {
			mMuxer = muxer;
			mEncoderCount = mStartedCount = 0;
			synchronized(this) {
				mState = STATE_INITIALIZED;
			}
		}
	}

	/**
	 * デバッグ用にいつ破棄されるかを確認したいだけ
	 */
/*	@Override
	protected void finalize() throws Throwable {
		if (DEBUG) Log.v(TAG, "finalize:");
		super.finalize();
	} */

	@Override
	public void prepare() {
//		if (DEBUG) Log.v(TAG, "prepare:");
		synchronized(this) {
			if (mReleased) {
				throw new IllegalStateException("already released");
			}
			if (mState != STATE_INITIALIZED) {
				throw new IllegalStateException("prepare:state=" + mState);
			}
		}
		try {
			if (mVideoEncoder != null)
				mVideoEncoder.prepare();
			if (mAudioEncoder != null)
				mAudioEncoder.prepare();
		} catch (final Exception e) {
			callOnError(e);
			return;
		}
		synchronized(this) {
			mState = STATE_PREPARED;
		}
		callOnPrepared();
	}

	@Override
	public void startRecording() throws IllegalStateException {
//		if (DEBUG) Log.v(TAG, "start:state=" + mState);
		synchronized(this) {
			if (mReleased) {
				throw new IllegalStateException("already released");
			}
			if (mState != STATE_PREPARED) {
				throw new IllegalStateException("start:not prepared");
			}
			mState = STATE_STARTING;
		}
//		if (DEBUG) Log.v(TAG, "call encoder#start");
		mStartTime = System.currentTimeMillis();
		if (mVideoEncoder != null)
			mVideoEncoder.start();
		if (mAudioEncoder != null)
			mAudioEncoder.start();
    	if (mEosHandler == null)
    		mEosHandler = EosHandler.createHandler(this);
    	mEosHandler.startCheckFreeSpace();	// 空き容量のチェック開始
	}

	@Override
	public void stopRecording() {
//		if (DEBUG) Log.v(TAG, "stop:");
        if (mEosHandler != null) {
        	mEosHandler.terminate();
        	mEosHandler = null;
        }
		synchronized(this) {
			if ((mState == STATE_UNINITIALIZED)
				|| (mState == STATE_INITIALIZED)
				|| (mState == STATE_STOPPING))
//				|| (mState == STATE_STOPPED))
				return;
			mState = STATE_STOPPING;
		}
		if (mAudioEncoder != null) {
			mAudioEncoder.stop();
		}
		if (mVideoEncoder != null) {
			mVideoEncoder.stop();
		}
		callOnStopped();
//		if (DEBUG) Log.v(TAG, "stop:fin");
	}

	@Override
	public Surface getInputSurface() {
		return (mVideoEncoder instanceof ISurfaceEncoder)
			? ((ISurfaceEncoder)mVideoEncoder).getInputSurface() : null;
	}

	@Override
	public Encoder getVideoEncoder() {
		return mVideoEncoder;
	}

	@Override
	public Encoder getAudioEncoder() {
		return mAudioEncoder;
	}

	@Override
	public synchronized boolean isStarted() {
		return !mReleased && (mState == STATE_STARTED);
	}

	@Override
	public synchronized boolean isReady() {
		return !mReleased
			&& (mState == STATE_STARTED || mState == STATE_PREPARED);
	}

	@Override
	public synchronized boolean isStopping() {
		return mState == STATE_STOPPING;
	}

	@Override
	public synchronized boolean isStopped() {
		return mState <= STATE_INITIALIZED;
	}

	public boolean isReleased() {
		return mReleased;
	}

	@Override
	public synchronized int getState() {
		return mState;
	}

	@Override
	public IMuxer getMuxer() {
		return mMuxer;
	}

	@Override
	public void frameAvailableSoon() {
		if (mVideoEncoder != null) {
			mVideoEncoder.frameAvailableSoon();
		}
	}

	@Override
	public void release() {
		if (!mReleased) {
			mReleased = true;
			if (mAudioEncoder != null) {
				mAudioEncoder.release();
			}
			if (mVideoEncoder != null) {
				mVideoEncoder.release();
			}
			if (mMuxer != null) {
				mMuxer.release();
			}
		}
		mAudioEncoder = null;
		mVideoEncoder = null;
		mMuxer = null;
	}
//================================================================================
	/**
	 * Encoderを登録する
	 * Encoderの下位クラスのコンストラクタから呼び出される
	 * 同じRecorderに対して映像用・音声用をそれぞれ最大１つしか設定できない
	 * @param encoder
	 */
	@Override
	public synchronized void addEncoder(final Encoder encoder) {
//		if (DEBUG) Log.v(TAG, "addEncoder:encoder=" + encoder);
		// ここの例外に引っかかるのはプログラムミス
		synchronized (this) {
			if (mReleased) {
				throw new IllegalStateException("already released");
			}
			if (mState > STATE_INITIALIZED) {
				throw new IllegalStateException("addEncoder already prepared/started");
			}
		}
		if (encoder instanceof IAudioEncoder) {
			if (mAudioEncoder != null)
				throw new IllegalArgumentException("Audio encoder already added.");
			mAudioEncoder = encoder;
		}
		if (encoder instanceof IVideoEncoder) {
			if (mVideoEncoder != null)
				throw new IllegalArgumentException("Video encoder already added.");
			mVideoEncoder = encoder;
		}
		mEncoderCount = (mVideoEncoder != null ? 1 : 0) + (mAudioEncoder != null ? 1 : 0);
	}

	/**
	 * Encoderの登録を解除する
	 * Encoderの下位クラスから呼び出される
	 * @param encoder
	 */
	@Override
	public synchronized void removeEncoder(final Encoder encoder) {
//		if (DEBUG) Log.v(TAG, "removeEncoder:encoder=" + encoder);
		if (encoder instanceof IVideoEncoder) {
			mVideoEncoder = null;
			mVideoStarted = false;
		}
		if (encoder instanceof AudioEncoder) {
			mAudioEncoder = null;
			mAudioStarted = false;
		}
		mEncoderCount = (mVideoEncoder != null ? 1 : 0) + (mAudioEncoder != null ? 1 : 0);
	}

	/**
	 * Muxerを開始する。Encoder#drainから呼び出される。
	 * 実際に開始しているかどうかはこのメソッドの戻り値または#isStartedでチェックする
	 * 複数のエンコーダー使用時には開始するまでブロックする
	 * @return true Muxerがstartしている
	 */
	@Override
	public synchronized boolean start(final Encoder encoder) {
//		if (DEBUG) Log.v(TAG,  "start:mEncoderCount=" + mEncoderCount + ",mStartedCount=" + mStartedCount);
		if (mReleased) {
			throw new IllegalStateException("already released");
		}
		if (mState != STATE_STARTING) {
			throw new IllegalStateException("muxer has not prepared:state=");
		}
		if (encoder.equals(mVideoEncoder)) {
			mVideoStarted = true;
		} else if (encoder.equals(mAudioEncoder)) {
			mAudioStarted = true;
		}
		mStartedCount++;
		for (; (mState == STATE_STARTING) && (mEncoderCount > 0) ;) {
			final boolean canStart
				= ((mVideoEncoder == null) || mVideoStarted)
				&& ((mAudioEncoder == null) || mAudioStarted);
			if (canStart) {
				mMuxer.start();
				mState = STATE_STARTED;
				notifyAll();
				callOnStarted();
				// 最大録画時間をセット
				if (mEosHandler != null)
					mEosHandler.setDuration(VideoConfig.maxDuration);
				break;
			} else {
				try {
					wait(100);
				} catch (final InterruptedException e) {
					break;
				}
			}
		}
		return mState == STATE_STARTED;
	}

	/**
	 * Muxerを停止させる。複数のエンコーダーが動作している時は最後の１個から呼び出された時に実際にstopさせる
	 */
	@Override
	public synchronized void stop(final Encoder encoder) {
//		if (DEBUG) Log.v(TAG,  "stop:mState=" + mState + ", mEncoderCount=" + mEncoderCount + ",mStartedCount=" + mStartedCount);
		if (encoder.equals(mVideoEncoder)) {
			if (mVideoStarted) {
				mVideoStarted = false;
				mStartedCount--;
			}
		} else if (encoder.equals(mAudioEncoder)) {
			if (mAudioStarted) {
				mAudioStarted = false;
				mStartedCount--;
			}
		}
		if ((mEncoderCount > 0) && (mStartedCount <= 0)) {
			if (mState == STATE_STOPPING) {
//				if (DEBUG) Log.v(TAG, "call muxer#stop");
				mMuxer.stop();
			}
			mState = STATE_INITIALIZED/*STATE_STOPPED*/;
			mVideoEncoder = null;
			mAudioEncoder = null;
//			if (DEBUG) Log.v(TAG,  "MediaAVRecorder stopped:");
		}
	}

	/**
	 * 音声または動画トラックの登録処理
	 * MediaEncoderのdrain内から呼び出される
	 * @param format
	 * @return
	 * @throws Exception
	 */
	@Override
	public synchronized int addTrack(final Encoder encoder, final MediaFormat format) {
//		if (DEBUG) Log.i(TAG, "addTrack:");
		int trackIx;
		try {
			if (mReleased) {
				throw new IllegalStateException("already released");
			}
			if (mState != STATE_STARTING) {
				throw new IllegalStateException("muxer not ready:state=" + mState);
			}
			trackIx = mMuxer.addTrack(format);
		} catch (final Exception e) {
			Log.w(TAG, "addTrack:", e);
			trackIx = -1;
			removeEncoder(encoder);
		}
//		if (DEBUG) Log.i(TAG, "addTrack:trackNum=" + mEncoderCount + ",trackIx=" + trackIx);
		return trackIx;
	}

	/**
	 * エンコード済みのバッファからmuxerへデータを書き込む
	 * @param trackIndex
	 * @param byteBuf
	 * @param bufferInfo
	 */
	@Override
	public void writeSampleData(final int trackIndex,
		final ByteBuffer byteBuf, final MediaCodec.BufferInfo bufferInfo) {

		try {
			if (!mReleased && (mStartedCount > 0)) {
				mMuxer.writeSampleData(trackIndex, byteBuf, bufferInfo);
			}
		} catch (final Exception e) {
//			if (DEBUG) Log.w(TAG, "writeSampleData:", e);
			callOnError(e);
		}
	}

//================================================================================
	protected void callOnPrepared() {
//		if (DEBUG) Log.v(TAG, "callOnPrepared:");
		if (mCallback != null)
		try {
			mCallback.onPrepared(this);
		} catch (final Exception e) {
			Log.e(TAG, "onPrepared:", e);
		}
	}

	protected void callOnStarted() {
//		if (DEBUG) Log.v(TAG, "callOnStarted:");
		if (mCallback != null)
		try {
			mCallback.onStarted(this);
		} catch (final Exception e) {
			Log.e(TAG, "onStarted:", e);
		}
	}

	protected void callOnStopped() {
//		if (DEBUG) Log.v(TAG, "callOnStopped:");
		if (mCallback != null)
		try {
			mCallback.onStopped(this);
		} catch (final Exception e) {
			Log.e(TAG, "onStopped:", e);
		}
	}

	protected void callOnError(final Exception e) {
//		if (DEBUG) Log.v(TAG, "callOnError:");
		if (!mReleased) {
			if (mCallback != null)
			try {
				mCallback.onError(e);
			} catch (final Exception e1) {
				Log.e(TAG, "onError:", e);
			}
		}
	}

//================================================================================
	/**
	 * 空き容量等のチェック処理の実体
	 * @return trueを返すとrecordingを終了する
	 */
	protected abstract boolean check();

//================================================================================
	/**
	 * 一定時間毎の空き容量チェック＆一定時間後に録画を停止させるためのハンドラー
	 */
	private static final class EosHandler extends Handler {
	    private static final int MSG_CHECK_FREESPACE = 5;
	    private static final int MSG_SEND_EOS = 8;
	    private static final int MSG_SEND_QUIT = 9;
	    private final EosThread mThread;

	    private EosHandler(final EosThread thread) {
	    	mThread = thread;
	    }

	    /**
	     * 別スレッドで処理するためのスレッドを生成しメッセージを送るためのハンドラーを返す
	     * ハンドラーの準備ができるまでブロックされる
	     * @param recorder
	     * @return
	     */
		public static final EosHandler createHandler(final Recorder recorder) {
			final EosThread thread = new EosThread(recorder);
			thread.start();
			return thread.getHandler();
		}

		/**
		 * 停止させるまでの時間[ミリ秒]をセット
		 * @param duration
		 */
    	public final void setDuration(final long duration) {
    		removeMessages(MSG_SEND_EOS);
    		if (duration > 0) {
	    		sendEmptyMessageDelayed(MSG_SEND_EOS, duration);
    		}
    	}

    	/**
    	 * 空き容量のチェック開始要求
    	 * 一度開始要求を送るとこのスレッドが停止されるまで自分で繰返し定期的に呼び出す
    	 */
    	public final void startCheckFreeSpace() {
    		// 未処理の空き容量チェックメッセージがあれば破棄
    		removeMessages(MSG_CHECK_FREESPACE);
    		// 一定時間後に空き容量チェックメッセージを送る
    		sendEmptyMessageDelayed(MSG_CHECK_FREESPACE, CHECK_INTERVAL);
    	}

    	/**
    	 * このスレッドの終了要求
    	 */
    	public final void terminate() {
    		// 未処理のメッセージがあれば破棄
       		removeMessages(MSG_SEND_EOS);
       		removeMessages(MSG_CHECK_FREESPACE);
       		// 終了要求を送信
   			sendEmptyMessage(MSG_SEND_QUIT);
    	}

		@SuppressWarnings("ConstantConditions")
		@Override
		public final void handleMessage(final Message msg) {
			final Recorder recorder = mThread.mWeakRecorder.get();
			if (recorder == null) {
//				Log.w(TAG, "unexpectedly recorder is null");
				try {
					Looper.myLooper().quit();
				} catch (final Exception e) {
					// ignore
				}
				return;
			}
			switch (msg.what) {
			case MSG_SEND_EOS:			// キャプチャ終了要求メッセージ
				recorder.stopRecording();
				break;
            case MSG_CHECK_FREESPACE:	// 空き容量チェックメッセージ
            	if (!mThread.check(recorder)) {
            		sendEmptyMessageDelayed(MSG_CHECK_FREESPACE, CHECK_INTERVAL);
            	} else {
            		recorder.stopRecording();
            	}
				break;
			case MSG_SEND_QUIT:
				try {
					Looper.myLooper().quit();
				} catch (final Exception e) {
					// ignore
				}
				break;
			default:
				super.handleMessage(msg);
			}
		}

		/**
		 * 一定時間毎の空き容量チェック・一定時間後に録画を停止させるためのスレッドの実体
		 */
	    private static final class EosThread extends Thread {
	        private final Object mSync = new Object();
	        // 親Recorderオブジェクトへの弱参照。親から終了要求されるので弱参照で無くてもいいかも
		    private final WeakReference<Recorder> mWeakRecorder;
	        private EosHandler mHandler;
	        private boolean mIsReady = false;

	        public EosThread(final Recorder recorder) {
	        	super("EosThread");
	        	mWeakRecorder = new WeakReference<Recorder>(recorder);
	        }

	        private final EosHandler getHandler() {
	            synchronized (mSync) {
	            	while (!mIsReady) {
	            		try {
							mSync.wait(300);
						} catch (final InterruptedException e) {
							break;
						}
	            	}
	            }
	            return mHandler;
	        }

	    	@Override
	    	public final void run() {
	            Looper.prepare();
	            synchronized (mSync) {
	            	mHandler = new EosHandler(this);
	            	mIsReady = true;
	            	mSync.notify();
	            }
	            Looper.loop();
	            synchronized (mSync) {
					mIsReady = false;
	            	mHandler = null;
	            }
	    	}

	    	private boolean check(final Recorder recorder) {
				return recorder.check();
	    	}
	    }
	}

	protected static IMuxer createMuxer(final String output_oath) throws IOException {
		IMuxer result;
		if (VideoConfig.sUseMediaMuxer) {
			result = new MediaMuxerWrapper(output_oath,
				MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
		} else {
			result = new VideoMuxer(output_oath);
		}
		return result;
	}

	@SuppressLint("NewApi")
	protected static IMuxer createMuxer(final int fd) throws IOException {
		IMuxer result;
		if (VideoConfig.sUseMediaMuxer) {
			if (BuildCheck.isOreo()) {
				final ParcelFileDescriptor pfd = ParcelFileDescriptor.fromFd(fd);
				result = new MediaMuxerWrapper(pfd.getFileDescriptor(),
					MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
			} else {
				throw new RuntimeException("createMuxer from fd does not support now");
			}
		} else {
			result = new VideoMuxer(fd);
		}
		return result;
	}

	@SuppressLint("NewApi")
	protected static IMuxer createMuxer(@NonNull final Context context,
		@NonNull final DocumentFile file) throws IOException {

		IMuxer result = null;
		if (VideoConfig.sUseMediaMuxer) {
			if (BuildCheck.isOreo()) {
				result = new MediaMuxerWrapper(context.getContentResolver()
					.openFileDescriptor(file.getUri(), "rw").getFileDescriptor(),
					MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
			} else {
				final String path = UriHelper.getPath(context, file.getUri());
				final File f = new File(UriHelper.getPath(context, file.getUri()));
				if (/*!f.exists() &&*/ f.canWrite()) {
					// 書き込めるファイルパスを取得できればそれを使う
					result = new MediaMuxerWrapper(path,
						MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
				} else {
					Log.w(TAG, "cant't write to the file, try to use VideoMuxer instead");
				}
			}
		}
		if (result == null) {
			result = new VideoMuxer(context.getContentResolver()
				.openFileDescriptor(file.getUri(), "rw").getFd());
		}
		return result;
	}
}
