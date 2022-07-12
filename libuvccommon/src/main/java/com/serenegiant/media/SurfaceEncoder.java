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
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.view.Surface;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class SurfaceEncoder extends AbstractVideoEncoder implements ISurfaceEncoder {
//	private static final boolean DEBUG = false;	// FIXME 実働時にはfalseにすること
//	private static final String TAG = "SurfaceEncoder";

	protected Surface mInputSurface;

    public SurfaceEncoder(final IRecorder recorder, final EncoderListener listener) {
		super(MediaCodecHelper.MIME_VIDEO_AVC, recorder, listener);
    }

    /**
     * Returns the encoder's input surface.
     */
    @Override
	public Surface getInputSurface() {
        return mInputSurface;
    }

	@Override
	public int getCaptureFormat() {
		return 0; // AbstractUVCCamera.CAPTURE_RGB565;
	}

	@Override
	protected boolean internalPrepare() throws Exception {
        mTrackIndex = -1;
        mRecorderStarted = false;
        mIsCapturing = true;
        mIsEOS = false;

        final MediaCodecInfo codecInfo = MediaCodecHelper.selectVideoEncoder(MediaCodecHelper.MIME_VIDEO_AVC);
        if (codecInfo == null) {
//			Log.e(TAG, "Unable to find an appropriate codec for " + MIME_AVC);
            return true;
        }
//		if (DEBUG) Log.i(TAG, "selected codec: " + codecInfo.getName());
//		/*if (DEBUG) */dumpProfileLevel(VIDEO_MIME_TYPE, codecInfo);
        final boolean mayFail
//        	= ((VideoConfig.currentConfig == VideoConfig.HD)
//        	|| (VideoConfig.currentConfig == VideoConfig.FullHD))
        	= ((mWidth >= 1000) || (mHeight >= 1000));
//			&& checkProfileLevel(VIDEO_MIME_TYPE, codecInfo);	// SC-06DでCodecInfo#getCapabilitiesForTypeが返ってこない/凄い時間がかかるのでコメントアウト

        final MediaFormat format = MediaFormat.createVideoFormat(MediaCodecHelper.MIME_VIDEO_AVC, mWidth, mHeight);

        // MediaCodecに適用するパラメータを設定する。誤った設定をするとMediaCodec#configureが
        // 復帰不可能な例外を生成する
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);	// API >= 18
        format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate > 0
        	? mBitRate : VideoConfig.getBitrate(mWidth, mHeight));
        format.setInteger(MediaFormat.KEY_FRAME_RATE, mFramerate > 0
        	? mFramerate : VideoConfig.getCaptureFps());
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mIFrameIntervals > 0
        	? mIFrameIntervals : VideoConfig.getIFrame());
//		if (DEBUG) Log.d(TAG, "format: " + format);

        // 設定したフォーマットに従ってMediaCodecのエンコーダーを生成する
        // エンコーダーへの入力に使うSurfaceを取得する
        mMediaCodec = MediaCodec.createEncoderByType(MediaCodecHelper.MIME_VIDEO_AVC);
        mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mInputSurface = mMediaCodec.createInputSurface();	// API >= 18
        mMediaCodec.start();

		return mayFail;
	}

    /**
     * Releases encoder resources.
     */
	@Override
	public void release() {
//		if (DEBUG) Log.d(TAG, "release:");
        super.release();
        mInputSurface = null;
    }

    @Override
	public void signalEndOfInputStream() {
//		if (DEBUG) Log.i(TAG, "signalEndOfInputStream:encoder=" + this);
        mIsEOS = true;
        if (mMediaCodec != null)
        	mMediaCodec.signalEndOfInputStream();	// API >= 18
	}

}
