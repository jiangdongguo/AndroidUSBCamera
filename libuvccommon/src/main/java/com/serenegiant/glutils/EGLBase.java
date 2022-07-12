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

import android.os.Build;

/**
 * EGLレンダリングコンテキストを生成＆使用するためのヘルパークラス
 */
public abstract class EGLBase {
	public static final Object EGL_LOCK = new Object();

	public static final int EGL_RECORDABLE_ANDROID = 0x3142;
	public static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
	public static final int EGL_OPENGL_ES2_BIT = 4;
	public static final int EGL_OPENGL_ES3_BIT_KHR = 0x0040;
//	public static final int EGL_SWAP_BEHAVIOR_PRESERVED_BIT = 0x0400;

	/**
	 * EGL生成のヘルパーメソッド, 環境に応じてEGLBase10またはEGLBase14を生成する
	 * maxClientVersion=3, ステンシルバッファなし
	 * @param sharedContext
	 * @param withDepthBuffer
	 * @param isRecordable
	 * @return
	 */
	public static EGLBase createFrom(final IContext sharedContext,
		final boolean withDepthBuffer, final boolean isRecordable) {

		return createFrom(3, sharedContext, withDepthBuffer, 0, isRecordable);
	}

	/**
	 * EGL生成のヘルパーメソッド, 環境に応じてEGLBase10またはEGLBase14を生成する
	 * maxClientVersion=3
	 * @param sharedContext
	 * @param withDepthBuffer
	 * @param stencilBits
	 * @param isRecordable
	 * @return
	 */
	public static EGLBase createFrom(final IContext sharedContext,
		final boolean withDepthBuffer, final int stencilBits, final boolean isRecordable) {

		return createFrom(3, sharedContext,
			withDepthBuffer, stencilBits, isRecordable);
	}

	/**
	 * EGL生成のヘルパーメソッド, 環境に応じてEGLBase10またはEGLBase14を生成する
	 * @param maxClientVersion
	 * @param sharedContext
	 * @param withDepthBuffer trueなら16ビットのデプスバッファ有り, falseならデプスバッファなし
	 * @param stencilBits 0以下ならステンシルバッファなし
	 * @param isRecordable
	 * @return
	 */
	public static EGLBase createFrom(final int maxClientVersion,
		final IContext sharedContext, final boolean withDepthBuffer,
		final int stencilBits, final boolean isRecordable) {

		if (isEGL14Supported() && ((sharedContext == null)
			|| (sharedContext instanceof EGLBase14.Context))) {

			return new EGLBase14(maxClientVersion,
				(EGLBase14.Context)sharedContext,
				withDepthBuffer, stencilBits, isRecordable);
		} else {
			return new EGLBase10(maxClientVersion,
				(EGLBase10.Context)sharedContext,
				withDepthBuffer, stencilBits, isRecordable);
		}
	}

	/**
	 * EGLレンダリングコンテキストのホルダークラス
	 */
	public static abstract class IContext {
		public abstract long getNativeHandle();
		public abstract Object getEGLContext();
	}

	/**
	 * EGLコンフィグのホルダークラス
	 */
	public static abstract class IConfig {
	}

	/**
	 * EGLレンダリングコンテキストに紐付ける描画オブジェクト
	 */
	public interface IEglSurface {
		public void makeCurrent();
		public void swap();

		public IContext getContext();
		/**
		 * swap with presentation time[ns]
		 * only works well now when using EGLBase14
		 * @param presentationTimeNs
		 */
		public void swap(final long presentationTimeNs);
		public void release();
		public boolean isValid();
	}

	public static boolean isEGL14Supported() {
		return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2);
	}

	/**
	 * 関連するリソースを破棄する
	 */
	public abstract void release();
	/**
	 * GLESに文字列を問い合わせる
	 * @param what
	 * @return
	 */
	public abstract String queryString(final int what);
	/**
	 * GLESバージョンを取得する
	 * @return 1, 2または3
	 */
	public abstract int getGlVersion();
	/**
	 * EGLレンダリングコンテキストを取得する
	 * このEGLBaseインスタンスを使って生成したEglSurfaceをmakeCurrentした状態で
	 * eglGetCurrentContextを呼び出すのと一緒
	 * @return
	 */
	public abstract IContext getContext();

	/**
	 * EGLコンフィグを取得する
	 * @return
	 */
	public abstract IConfig getConfig();

	/**
	 * 指定したSurfaceからEglSurfaceを生成する
	 * 生成したEglSurfaceをmakeCurrentした状態で戻る
	 * @param nativeWindow Surface/SurfaceTexture/SurfaceHolder
	 * @return
	 */
	public abstract IEglSurface createFromSurface(final Object nativeWindow);
	/**
	 * 指定した大きさのオフスクリーンEglSurfaceを生成する
	 * 生成したEglSurfaceをmakeCurrentした状態で戻る
	 * @param width PBufferオフスクリーンのサイズ(0以下はだめ)
	 * @param height
	 * @return
	 */
	public abstract IEglSurface createOffscreen(final int width, final int height);
	/**
	 * EGLレンダリングコンテキストとスレッドの紐付けを解除する
	 */
	public abstract void makeDefault();

	/**
	 * eglWaitGLとeglWaitNativeを呼ぶ
	 *
	 * eglWaitGL: コマンドキュー内のコマンドをすべて転送する, GLES20.glFinish()と同様の効果
	 * eglWaitNative: GPU側の描画処理が終了するまで実行をブロックする
	 */
	public abstract void sync();
}
