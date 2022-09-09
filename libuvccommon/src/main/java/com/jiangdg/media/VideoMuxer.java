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
import java.util.Collection;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import androidx.annotation.NonNull;

/**
 * MediaMuxerがAPI>=18でしか使えないので、localに移植
 * 使い方はオリジナルのMediaMuxerとほぼ同じ
 * 普通はIMuxer経由で使う
 * libcommon内にはnative側が入ってないのでクラッシュするよ^^;
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public final class VideoMuxer implements IMuxer {

//	private static final boolean DEBUG = false;	// FIXME 実働時にはfalseにすること
//	private static final String TAG = "VideoMuxer";

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
	private volatile boolean mIsStarted;
	private boolean mReleased;

	public VideoMuxer(final String path) {
		mNativePtr = nativeCreate(path);
	}

	public VideoMuxer(final int fd) {
		mNativePtr = nativeCreateFromFD(fd);
	}

	@Override
	public void release() {
//    	if (DEBUG) Log.v(TAG, "release:");
		if (mNativePtr != 0) {
			mReleased = true;
			nativeDestroy(mNativePtr);
			mNativePtr = 0;
		}
	}

    @Override
	protected void finalize() throws Throwable {
    	release();
		super.finalize();
	}

	private int mLastTrackIndex = -1;
	@SuppressLint("InlinedApi")
	@Override
	public int addTrack(@NonNull final MediaFormat format) {
        if (format == null) {
            throw new IllegalArgumentException("format must not be null.");
        }
        // MediaMuxerと同じようにしてMediaFormat#getMapでデータを取りたかったんだけど
        // パッケージローカルでアクセス出来ない。リフレクションもだめなので、地道にキーを順にためして
        // 一旦HashMapにして、そこからキーと値を取り出す
        final HashMap<String, Object> map = new HashMap<String, Object>();
        if (format.containsKey(MediaFormat.KEY_MIME))
        	map.put(MediaFormat.KEY_MIME, format.getString(MediaFormat.KEY_MIME));
        if (format.containsKey(MediaFormat.KEY_WIDTH))
        	map.put(MediaFormat.KEY_WIDTH, format.getInteger(MediaFormat.KEY_WIDTH));
        if (format.containsKey(MediaFormat.KEY_HEIGHT))
        	map.put(MediaFormat.KEY_HEIGHT, format.getInteger(MediaFormat.KEY_HEIGHT));
        if (format.containsKey(MediaFormat.KEY_BIT_RATE))
        	map.put(MediaFormat.KEY_BIT_RATE, format.getInteger(MediaFormat.KEY_BIT_RATE));
        if (format.containsKey(MediaFormat.KEY_COLOR_FORMAT))
        	map.put(MediaFormat.KEY_COLOR_FORMAT, format.getInteger(MediaFormat.KEY_COLOR_FORMAT));
        if (format.containsKey(MediaFormat.KEY_FRAME_RATE))
        	map.put(MediaFormat.KEY_FRAME_RATE, format.getInteger(MediaFormat.KEY_FRAME_RATE));
        if (format.containsKey(MediaFormat.KEY_I_FRAME_INTERVAL))
        	map.put(MediaFormat.KEY_I_FRAME_INTERVAL, format.getInteger(MediaFormat.KEY_I_FRAME_INTERVAL));
        if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE))
        	map.put(MediaFormat.KEY_MAX_INPUT_SIZE, format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE));
        if (format.containsKey(MediaFormat.KEY_DURATION))
        	map.put(MediaFormat.KEY_DURATION, format.getInteger(MediaFormat.KEY_DURATION));
        if (format.containsKey("what"))
        	map.put("what", format.getInteger("what"));
        if (format.containsKey("csd-0"))
        	map.put("csd-0", format.getByteBuffer("csd-0"));
        if (format.containsKey("csd-1"))
        	map.put("csd-1", format.getByteBuffer("csd-1"));

        // HashMap<String, Object>#keySet().toArray()でKeyに対応するObject[]が取得できるけど
        // なぜか、そのままString[]にキャスト出来ないのでわざわざこんな展開をせざるを得ない
        // nativeAddTrackの第2引数をObject[]にしたら行けるかも
        final Object[] ks = map.keySet().toArray();
        final int n = ks.length;
        final String[] keys = new String[n];
        for (int i = 0; i < n; i++)
        	keys[i] = (String)ks[i];
        final Collection<Object> values = map.values();
        final int trackIndex = nativeAddTrack(mNativePtr, keys, values.toArray());

        // addTrackが成功すれば返り値のTrack Indexは順に増加するはず
        // フォーマットが正しくない時など失敗すれば負の値が返ってくる
		if (mLastTrackIndex >= trackIndex) {
            throw new IllegalArgumentException("Invalid format.");
        }
        mLastTrackIndex = trackIndex;
        return trackIndex;
    }

	@Override
	public void start() {
//		if (DEBUG) Log.v(TAG, "start:");
		int res = -1;
		if (mNativePtr != 0) {
			res = nativeStart(mNativePtr);
		}
		if (res != 0)
			throw new IllegalStateException("failed to start muxer");
		mIsStarted = true;
	}

	@Override
	public void stop() {
//		if (DEBUG) Log.v(TAG, "stop:");
		mIsStarted = false;
		if (mNativePtr != 0) {
			final int res = nativeStop(mNativePtr);
			if (res != 0)
				throw new RuntimeException("failed to stop muxer");
		}
//		if (DEBUG) Log.v(TAG, "stop:stopped");
 	}

	@Override
	public void writeSampleData(final int trackIndex,
		@NonNull final ByteBuffer buf, @NonNull final MediaCodec.BufferInfo bufferInfo) {

		int res = 1;
		if (!mReleased && (mNativePtr != 0)) {
			res = nativeWriteSampleData(mNativePtr, trackIndex, buf,
				bufferInfo.offset, bufferInfo.size, bufferInfo.presentationTimeUs, bufferInfo.flags);
		}
		if (res != 0) {
			switch (res) {
			case -2000:
				throw new TimeoutException();
			case 1:
				throw new IllegalStateException("muxer already released.");
			default:
				throw new IllegalArgumentException("writeSampleData:err=" + res);
			}
		}
	}

	// nativeメソッド(nativeCreate/nativeDestroyの２つはクラス内のフィールドにアクセスするためstaticじゃないよ)
	private final native long nativeCreate(final String path);
	private final native long nativeCreateFromFD(final int fd);
	private final native void nativeDestroy(final long id_encoder);

	private static final native int nativeAddTrack(final long id_muxer,
		final String[] keys, final Object[] values);
	private static final native int nativeStart(final long id_muxer);
	private static final native int nativeStop(final long id_muxer);
	private static final native int nativeWriteSampleData(final long id_muxer,
		final int trackIndex, final ByteBuffer buf, final int offset, final int size,
		final long presentationTimeUs, final int flags);

	@Override
	public boolean isStarted() {
		return mIsStarted && !mReleased;
	}

}
