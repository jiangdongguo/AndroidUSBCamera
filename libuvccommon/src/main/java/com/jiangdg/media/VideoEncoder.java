package com.jiangdg.media;
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

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public final class VideoEncoder extends AbstractVideoEncoder {
//	private static final boolean DEBUG = false;	// FIXME 実働時にはfalseにすること
	private static final String TAG = VideoEncoder.class.getSimpleName();

	private static boolean isLoaded = false;
	static {
		if (!isLoaded) {
			System.loadLibrary("c++_shared");
			System.loadLibrary("jpeg-turbo2000");
			System.loadLibrary("png16");
			System.loadLibrary("libuvccommon");
			System.loadLibrary("mediaencoder");
			isLoaded = true;
		}
	}

	// native側からアクセスするフィールド
	// nativeコードからアクセするので名前を変えたり削除したりしたらダメ
    protected long mNativePtr;
    //
//	private final WeakReference<AbstractUVCCamera>mCamera;
    private final boolean mAlign16;
    private int mColorFormat;

	public VideoEncoder(final Recorder recorder, final EncoderListener listener, final boolean align16) {
		super(MediaCodecHelper.MIME_VIDEO_AVC, recorder, listener);
//		if (DEBUG) Log.i(TAG, "コンストラクタ:");
		mAlign16 = align16;
//		mCamera = new WeakReference<AbstractUVCCamera>(camera);
//		if (DEBUG) Log.i(TAG, "VideoEncoder:" + camera + ",nativePtr=" + (camera != null ? camera.getNativePtr() : ""));
		mNativePtr = nativeCreate();
    }

	@Override
	protected boolean internalPrepare() throws Exception {
//		if (DEBUG) Log.i(TAG, "internalPrepare:");
        mRecorderStarted = false;
        mIsCapturing = true;
        mIsEOS = false;

        final MediaCodecInfo codecInfo = MediaCodecHelper.selectVideoEncoder(MediaCodecHelper.MIME_VIDEO_AVC);
        if (codecInfo == null) {
			Log.e(TAG, "Unable to find an appropriate codec for " + MediaCodecHelper.MIME_VIDEO_AVC);
            return true;
        }
//		if (DEBUG) Log.i(TAG, "selected codec: " + codecInfo.getName());
//		if (DEBUG) Log.i(TAG, "selected colorFormat: " + mColorFormat);

//		if (DEBUG) dumpProfileLevel(VIDEO_MIME_TYPE, codecInfo);
        final boolean mayFail
//			= ((VideoConfig.currentConfig == VideoConfig.HD)
//			|| (VideoConfig.currentConfig == VideoConfig.FullHD))
        	= ((mWidth >= 1000) || (mHeight >= 1000));
//        	&& checkProfileLevel(VIDEO_MIME_TYPE, codecInfo);	// SC-06DでCodecInfo#getCapabilitiesForTypeが返ってこない/凄い時間がかかるのでコメントアウト

        final MediaFormat format = MediaFormat.createVideoFormat(MediaCodecHelper.MIME_VIDEO_AVC, mWidth, mHeight);

		mColorFormat = MediaCodecHelper.selectColorFormat(codecInfo, MediaCodecHelper.MIME_VIDEO_AVC);
        // MediaCodecに適用するパラメータを設定する。
        // 誤った設定をするとMediaCodec#configureが復帰不可能な例外を生成する
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, mColorFormat);	// API >= 16
		format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate > 0
			? mBitRate : VideoConfig.getBitrate(mWidth, mHeight));
		format.setInteger(MediaFormat.KEY_FRAME_RATE, mFramerate > 0
			? mFramerate : VideoConfig.getCaptureFps());
		format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mIFrameIntervals > 0
			? mIFrameIntervals : VideoConfig.getIFrame());
//		format.setInteger(MediaFormat.KEY_WIDTH, currentConfig.width);
//		format.setInteger(MediaFormat.KEY_HEIGHT, currentConfig.height);
		Log.d(TAG, "format: " + format);

        // 設定したフォーマットに従ってMediaCodecのエンコーダーを生成する
        mMediaCodec = MediaCodec.createEncoderByType(MediaCodecHelper.MIME_VIDEO_AVC);
        mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec.start();
//		if (DEBUG) Log.v(TAG, "MediaCodec start");

        if (mAlign16) {
        	if ((mWidth / 16) * 16 != mWidth) mWidth = ((mWidth / 16) + 1) * 16;
        	if ((mHeight / 16) * 16 != mHeight) mHeight = ((mHeight / 16) + 1) * 16;
        }
		nativePrepare(mNativePtr, mWidth, mHeight,
			MediaCodecHelper.selectColorFormat(codecInfo, MediaCodecHelper.MIME_VIDEO_AVC));
//		if (DEBUG) Log.i(TAG, String.format("request(%d,%d),align(%d,%d)", EncoderConfig.currentConfig.width, EncoderConfig.currentConfig.height, width, height));
		// native側でMediaCodecへ書き込むための設定
		// 先にコーデックへの入力を開始しないとdrainが回らない
/*		final AbstractUVCCamera camera = mCamera.get();
		if (camera == null)
			throw new RuntimeException("unexpectedly camera instance is null");
        camera.setEncoder(this); */
//		if (DEBUG) Log.i(TAG, "internalPrepare:finished");

		return mayFail;
	}

	@Override
	public void stop() {
/*		final AbstractUVCCamera camera = mCamera.get();
		if (camera != null)
			camera.setEncoder(null); */
		if (mNativePtr != 0) {
			nativeStop(mNativePtr);
		}
		super.stop();
	}

	/**
	 * 停止させてから破棄する。再利用は出来ない
	 */
	@Override
	public void release() {
//		if (DEBUG) Log.i(TAG, "release:");
		stop();
		if (mNativePtr != 0) {
			nativeDestroy(mNativePtr);
			mNativePtr = 0;
		}
		super.release();
	}

	/**
	 * コーデックからの出力フォーマットを取得してnative側へ引き渡してMuxerをスタートさせる
	 */
	@Override
	protected synchronized boolean startRecorder(final IRecorder recorder,
		final MediaFormat outFormat) {
	
//		if (DEBUG) Log.i(TAG, "startRecorder:outFormat=" + outFormat);
        // MediaCodecがセットした実際の高さ・幅に調整し直す
		int w, h;
		try {
			w = outFormat.getInteger(MediaFormat.KEY_WIDTH);
		} catch (final Exception e) {
			w = mWidth;
		}
		try {
			h = outFormat.getInteger(MediaFormat.KEY_HEIGHT);
		} catch (final Exception e) {
			h = mHeight;
		}
        nativeResize(mNativePtr, w, h, mColorFormat);
        final int sz = w * h * 2 * 3 / 4;
        if (sz != BUF_SIZE) {
        	BUF_SIZE = sz;
        	mEncodeBytes = new byte[BUF_SIZE];
//        	if (DEBUG) Log.i(TAG, "mBuffer size changed:");
        }
        return super.startRecorder(recorder, outFormat);
	}

	@Override
	protected void stopRecorder(final IRecorder recorder) {
		if (mRecorderStarted) {
/*			final AbstractUVCCamera camera = mCamera.get();
			if (camera != null) {
				camera.setEncoder(null);
			} */
		    nativeStop(mNativePtr);
		}
		super.stopRecorder(recorder);
	}

	private int BUF_SIZE = mWidth * ((int)Math.ceil(mHeight / 16.f)) * 16 * 2 * 3 / 4;
    private byte[] mEncodeBytes = new byte[BUF_SIZE];
	/**
	 * nativeから呼び出すので名前を変えちゃダメ
	 */
    @Override
	public void encode(final ByteBuffer buffer) {
//    	if (DEBUG) Log.v(TAG, "encode:");
		synchronized (mSync) {
			if (!mIsCapturing || mRequestStop) return;
		}
/*    	final int n = mBuffer.limit();
    	mBuffer.position(0);
    	final int sz = n < BUF_SIZE ? n : BUF_SIZE;
   		mBuffer.get(mEncodeBytes, 0, sz);
   		encode(mEncodeBytes, sz, getInputPTSUs()); */
		try {
			buffer.rewind();
	    	encode(buffer, buffer.limit(), getInputPTSUs());
		} catch (final Exception e) {
			callOnError(e);
		}
    }

	@Override
	public int getCaptureFormat() {
		return -1;
	}

	// nativeメソッド(nativeCreate/nativeDestroyの２つはクラス内のフィールドにアクセスするためstaticじゃないよ)
	private final native long nativeCreate();
	private final native void nativeDestroy(long id_encoder);

	private static final native int nativePrepare(long id_encoder, int width, int height, int colorFormat);
	private static final native int nativeResize(long id_encoder, int width, int height, int colorFormat);
	private static final native int nativeStop(long id_encoder);

}
