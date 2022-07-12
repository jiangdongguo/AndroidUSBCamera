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

import android.os.Build;

/**
 * 従来はプレビュー解像度=動画の解像度の設定用に使用していたが
 * 今後は動画解像度用のみに使用してプレビュー解像度はDeviceSettingから取得する
 */
public class VideoConfig {
//	private static final String TAG = "VideoConfig";

	/**
	 * BPP(Bits Per Pixel)
	 * (0.050/0.075/0.100/0.125/0.150/0.175/0.200/0.225/0.25)
	 */
	private static float BPP = 0.25f;
	public static final float BPP_MIN = 0.01f;
	public static float BPP_MAX = 0.30f;

	public static final int FPS_MIN = 2;
	public static final int FPS_MAX = 121;
	/**
	 * I-frame(単独で圧縮された単独再生可能な一番劣化の少ないキーフレーム)間の秒数@30fps
	 */
    private static float IFRAME_INTERVAL = 10;
	private static final int IFRAME_MIN = 1;
	private static final int IFRAME_MAX = 30;
	/**
	 * I-Frameの間隔 300 = 30fpsの時に10秒間隔 = 300フレームに1回
	 */
	private static float IFI = IFRAME_INTERVAL * 30.0f;

	/**
	 * エンコード時のFPS
	 */
	private static int captureFps = 15;
	/**
	 * 最大録画時間[ミリ秒], 負数=制限なし
	 */
	public static long maxDuration = 30 * 1000L;
	/**
	 * 繰り返し休止時間[ミリ秒](とりあえず60秒)
	 */
	public static  long repeatInterval = 60 * 1000L;
	/**
	 * 負は回数制限なし, 1以上なら指定回数繰り返し
	 */
	public static  int maxRepeats = 1;

	/**
	 * trueならMediaMuxerを使う、 falseならVideoMuxerを使う
	 * ・・・VideoMuxerを使ってnative側での最大録画時間チェックを有効にするため常にfalse
	 */
	public static boolean sUseMediaMuxer = (Build.VERSION.SDK_INT >= 18) && false;

	/**
	 * SurfaceEncoderを使って動画キャプチャをするかどうか
	 */
	public static boolean sIsSurfaceCapture = (Build.VERSION.SDK_INT >= 18) && false;

	public static void setCaptureFps(final int fps) {
		captureFps = fps > FPS_MAX ? FPS_MAX : (fps < FPS_MIN ? FPS_MIN : fps);
	}
	
	public static int getCaptureFps() {
		return captureFps > FPS_MAX ? FPS_MAX : (captureFps < FPS_MIN ? FPS_MIN : captureFps);
	}

	public static void setIFrame(final float iframe_interval) {
		IFRAME_INTERVAL = iframe_interval;
		IFI = IFRAME_INTERVAL * 30.f;
	}

	public static final int getIFrame() {
		final int fps = getCaptureFps();
		float iframe;
		try {
			if (fps < 2)
				iframe = IFRAME_MIN;
			else
				iframe = (float)Math.ceil(IFI / fps);
		} catch (final Exception e) {
			iframe = IFRAME_INTERVAL;
		}
		if ((int)iframe < IFRAME_MIN) iframe = IFRAME_MIN;
		else if ((int)iframe > IFRAME_MAX) iframe = IFRAME_MAX;
//		Log.d(TAG, "iframe_intervals=" + iframe);
		return (int)iframe;
	}

	/**
	 * ビットレートを計算[bps]
	 * @param width
	 * @param height
	 * @param frameRate
	 * @param bpp
	 * @return
	 */
	public static int calcBitrate(final int width, final int height,
		final int frameRate, final float bpp) {

		int r = (int)(Math.floor(bpp * frameRate * width * height / 1000 / 100) * 100) * 1000;
		if (r < 200000) r = 200000;
		else if (r > 20000000) r = 20000000;
//		Log.d(TAG, String.format("bitrate=%d[kbps]", r / 1024));
		return r;
	}


	/**
	 * ビットレートを計算[bps]
	 * @param width
	 * @param height
	 * @return
	 */
	public static int getBitrate(final int width, final int height) {
		return calcBitrate(width, height, getCaptureFps(), BPP);
	}

	/**
	 * ビットレートを計算[bps]
	 * @param width
	 * @param height
	 * @param frameRate
	 * @return
	 */
	public static int getBitrate(final int width, final int height, final int frameRate) {
		return calcBitrate(width, height, frameRate, BPP);
	}

	/**
	 * ビットレートを計算[bps]
	 * @param width
	 * @param height
	 * @param frameRate
	 * @return
	 */
	public static int getBitrate(final int width, final int height,
		final int frameRate, final float bpp) {

		return calcBitrate(width, height, frameRate, bpp);
	}

	/**
	 * BPPを計算
	 * @param width
	 * @param height
	 * @param captureFps
	 * @param bitrate
	 * @return
	 */
	public static float calcBPP(final int width, final int height,
		final int captureFps, final int bitrate) {

		return bitrate / (float)(captureFps * width * height);
	}

	/**
	 * BPPを計算
	 * captureFpsは#getCaptureFpsを使用
	 * @param width
	 * @param height
	 * @param bitrate
	 * @return
	 */
	public static float calcBPP(final int width, final int height,
		final int bitrate) {

		return calcBPP(width, height, getCaptureFps(), bitrate);
	}

	/**
	 * BPPをセット
	 * @param width
	 * @param height
	 * @param bitrate
	 * @throws IllegalArgumentException
	 */
	public static void setBPP(final int width, final int height, final int bitrate)
		throws IllegalArgumentException {

		setBPP(calcBPP(width, height, bitrate));
	}

	/**
	 * BPPをセット
	 * @param bpp [BPP_MIN==0.01f, BPP_MAX]
	 * @throws IllegalArgumentException
	 */
	public static void setBPP(final float bpp) throws IllegalArgumentException {
		if ((bpp < BPP_MIN) || (bpp > BPP_MAX)) {
			throw new IllegalArgumentException("bpp should be within [BPP_MIN, BPP_MAX]");
		}
		BPP = bpp;
	}

	/**
	 * 現在のBPP設定を取得
	 * @return
	 */
	public static float bpp() {
		return BPP;
	}

	/**
	 * 現在の設定で生成される概略ファイルサイズを計算[バイト/分]
	 * 音声データ分は含まない
	 * @param width
	 * @param height
	 * @return
	 */
	public static int getSizeRate(final int width, final int height) {
		final int bitrate = getBitrate(width, height);
		return bitrate * 60 / 8;	// bits/sec -> bytes/min
	}

}
