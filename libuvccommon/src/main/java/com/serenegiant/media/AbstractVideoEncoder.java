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

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;

import com.serenegiant.utils.BuildCheck;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public abstract class AbstractVideoEncoder extends AbstractEncoder
	implements IVideoEncoder {

//	private static final boolean DEBUG = false;	// FIXME 実働時にはfalseにすること
//	private static final String TAG = "AbstractVideoEncoder";

   protected int mWidth, mHeight;
    protected int mBitRate = -1;
	protected int mFramerate = -1;
    protected int mIFrameIntervals = -1;

    public AbstractVideoEncoder(final String mime, final IRecorder recorder, final EncoderListener listener) {
		super(mime, recorder, listener);
    }

	/**
	 * 動画サイズをセット
	 * ビットレートもサイズとVideoConfigの設定値に合わせて変更される
	 * @param width
	 * @param height
	 */
	@Override
	public void setVideoSize(final int width, final int height)
		throws IllegalArgumentException, IllegalStateException {
//    	Log.d(TAG, String.format("setVideoSize(%d,%d)", width, height));
    	mWidth = width;
    	mHeight = height;
		mBitRate = VideoConfig.getBitrate(width, height);
    }

	public void setVideoConfig(final int bitRate, final int frameRate, final int iFrameIntervals) {
		mBitRate = bitRate;
		mFramerate = frameRate;
		mIFrameIntervals = iFrameIntervals;
	}

	@Override
    public int getWidth() {
    	return mWidth;
    }

	@Override
    public int getHeight() {
    	return mHeight;
    }

	@Override
	public final boolean isAudio() {
		return false;
	}

    public static boolean supportsAdaptiveStreaming = BuildCheck.isKitKat();

    @TargetApi(Build.VERSION_CODES.KITKAT)
	public void adjustBitrate(final int targetBitrate) {
        if (supportsAdaptiveStreaming && mMediaCodec != null) {
            final Bundle bitrate = new Bundle();
            bitrate.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, targetBitrate);
            mMediaCodec.setParameters(bitrate);
        } else if (!supportsAdaptiveStreaming) {
//			Log.w(TAG, "Ignoring adjustVideoBitrate call. This functionality is only available on Android API 19+");
        }
    }

	@Override
	protected MediaFormat createOutputFormat(final byte[] csd, final int size,
		final int ix0, final int ix1, final int ix2) {
		
		final MediaFormat outFormat;
        if (ix0 >= 0) {
            outFormat = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
        	final ByteBuffer csd0 = ByteBuffer.allocateDirect(ix1 - ix0).order(ByteOrder.nativeOrder());
        	csd0.put(csd, ix0, ix1 - ix0);
        	csd0.flip();
            outFormat.setByteBuffer("csd-0", csd0);
            if (ix1 > ix0) {
				final int sz = (ix2 > ix1) ? (ix2 - ix1) : (size - ix1);
            	final ByteBuffer csd1 = ByteBuffer.allocateDirect(size - ix1 + ix0).order(ByteOrder.nativeOrder());
            	csd1.put(csd, ix1, size - ix1 + ix0);
            	csd1.flip();
                outFormat.setByteBuffer("csd-1", csd1);
            }
        } else {
        	throw new RuntimeException("unexpected csd data came.");
        }
        return outFormat;
	}

}
