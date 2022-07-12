package com.serenegiant.glutils;
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

import android.graphics.SurfaceTexture;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.Surface;

import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 分配描画インターフェース
 */
public interface IRendererHolder extends IRendererCommon {
	public static final int DEFAULT_CAPTURE_COMPRESSION = 80;

	public static final int OUTPUT_FORMAT_JPEG = 0;	// Bitmap.CompressFormat.JPEG
	public static final int OUTPUT_FORMAT_PNG = 1;	// Bitmap.CompressFormat.PNG
	public static final int OUTPUT_FORMAT_WEBP = 2;	// Bitmap.CompressFormat.WEBP

	@IntDef({OUTPUT_FORMAT_JPEG, OUTPUT_FORMAT_PNG, OUTPUT_FORMAT_WEBP})
	@Retention(RetentionPolicy.SOURCE)
	public @interface StillCaptureFormat {}

	/**
	 * 実行中かどうか
	 * @return
	 */
	public boolean isRunning();
	/**
	 * 関係するすべてのリソースを開放する。再利用できない
	 */
	public void release();

	@Nullable
	public EGLBase.IContext getContext();

	/**
	 * マスター用の映像を受け取るためのSurfaceを取得
	 * @return
	 */
	public Surface getSurface();

	/**
	 * マスター用の映像を受け取るためのSurfaceTextureを取得
	 * @return
	 */
	public SurfaceTexture getSurfaceTexture();

	/**
	 * マスター用の映像を受け取るためのマスターをチェックして無効なら再生成要求する
	 */
	public void reset();

	/**
	 * マスター映像サイズをサイズ変更要求
	 * @param width
	 * @param height
	 */
	public void resize(final int width, final int height)
		throws IllegalStateException;

	/**
	 * 分配描画用のSurfaceを追加
	 * このメソッドは指定したSurfaceが追加されるか
	 * interruptされるまでカレントスレッドをブロックする。
	 * @param id 普通は#hashCodeを使う
	 * @param surface, should be one of Surface, SurfaceTexture or SurfaceHolder
	 * @param isRecordable
	 */
	public void addSurface(final int id, final Object surface,
		final boolean isRecordable)
			throws IllegalStateException, IllegalArgumentException;

	/**
	 * 分配描画用のSurfaceを追加
	 * このメソッドは指定したSurfaceが追加されるか
	 * interruptされるまでカレントスレッドをブロックする。
	 * @param id 普通は#hashCodeを使う
	 * @param surface, should be one of Surface, SurfaceTexture or SurfaceHolder
	 * @param isRecordable
	 * @param maxFps 0以下なら制限しない
	 */
	public void addSurface(final int id, final Object surface,
		final boolean isRecordable, final int maxFps)
			throws IllegalStateException, IllegalArgumentException;

	/**
	 * 分配描画用のSurfaceを削除
	 * このメソッドは指定したSurfaceが削除されるか
	 * interruptされるまでカレントスレッドをブロックする。
	 * @param id
	 */
	public void removeSurface(final int id);
	
	/**
	 * 分配描画用のSurfaceを全て削除
	 * このメソッドはSurfaceが削除されるか
	 * interruptされるまでカレントスレッドをブロックする。
	 */
	public void removeSurfaceAll();
	
	/**
	 * 分配描画用のSurfaceを指定した色で塗りつぶす
	 * @param id
	 * @param color
	 */
	public void clearSurface(final int id, final int color);
	
	/**
	 * 分配描画用のSurfaceを指定した色で塗りつぶす
	 * @param color
	 */
	public void clearSurfaceAll(final int color);
	
	/**
	 * モデルビュー変換行列をセット
	 * @param id
	 * @param offset
	 * @param matrix offset以降に16要素以上
	 */
	public void setMvpMatrix(final int id,
		final int offset, @NonNull final float[] matrix);

	/**
	 * 分配描画用のSurfaceへの描画が有効かどうかを取得
	 * @param id
	 * @return
	 */
	public boolean isEnabled(final int id);
	
	/**
	 * 分配描画用のSurfaceへの描画の有効・無効を切替
	 * @param id
	 * @param enable
	 */
	public void setEnabled(final int id, final boolean enable);

	/**
	 * 強制的に現在の最新のフレームを描画要求する
	 * 分配描画用Surface全てが更新されるので注意
	 */
	public void requestFrame();

	/**
	 * 追加されている分配描画用のSurfaceの数を取得
	 * @return
	 */
	public int getCount();

	/**
	 * 静止画を撮影する
	 * 撮影完了を待機しない
	 * @param path
	 */
	@Deprecated
	public void captureStillAsync(@NonNull final String path)
		throws FileNotFoundException, IllegalStateException;
	
	/**
	 * 静止画を撮影する
	 * 撮影完了を待機しない
	 * @param path
	 * @param captureCompression JPEGの圧縮率, pngの時は無視
	 */
	@Deprecated
	public void captureStillAsync(@NonNull final String path,
		@IntRange(from = 1L,to = 99L) final int captureCompression)
			throws FileNotFoundException, IllegalStateException;

	/**
	 * 静止画を撮影する
	 * 撮影完了を待機する
	 * @param path
	 */
	public void captureStill(@NonNull final String path)
		throws FileNotFoundException, IllegalStateException;

	/**
	 * 静止画を撮影する
	 * 撮影完了を待機する
	 * @param path
	 * @param captureCompression JPEGの圧縮率, pngの時は無視
	 */
	public void captureStill(@NonNull final String path,
		@IntRange(from = 1L,to = 99L) final int captureCompression)
			throws FileNotFoundException, IllegalStateException;

	/**
	 * 静止画を撮影する
	 * 撮影完了を待機する
	 * @param out
	 * @param stillCaptureFormat
	 * @param captureCompression
	 */
	public void captureStill(@NonNull final OutputStream out,
		@StillCaptureFormat final int stillCaptureFormat,
		@IntRange(from = 1L,to = 99L) final int captureCompression)
			throws IllegalStateException;
}
