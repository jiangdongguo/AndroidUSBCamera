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

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

/**
 * テクスチャへOpenGL|ESで描画するためのオフスクリーン描画クラス
 * テクスチャをカラーバッファとしてFBOに割り当てる
 */
public class TextureOffscreen {
	private static final boolean DEBUG = false;
	private static final String TAG = "TextureOffscreen";

	private static final boolean DEFAULT_ADJUST_POWER2 = false;

	private final int TEX_TARGET;
	private final int TEX_UNIT;
	private final boolean mHasDepthBuffer, mAdjustPower2;
	/** 描画領域サイズ */
	private int mWidth, mHeight;
	/** テクスチャサイズ */
	private int mTexWidth, mTexHeight;
	/** オフスクリーンのカラーバッファに使うテクスチャ名 */
	private int mFBOTextureName = -1;
	/** // オフスクリーン用のバッファオブジェクト */
	private int mDepthBufferObj = -1, mFrameBufferObj = -1;
	/** テクスチャ座標変換行列 */
	private final float[] mTexMatrix = new float[16];

	/**
	 * コンストラクタ(GL_TEXTURE_2D), デプスバッファ無し
	 * テクスチャユニットはGL_TEXTURE0
	 * @param width
	 * @param height
	 */
	public TextureOffscreen(final int width, final int height) {
		this(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE0, -1,
			width, height, false, DEFAULT_ADJUST_POWER2);
	}

	/**
	 * コンストラクタ(GL_TEXTURE_2D), デプスバッファ無し
	 * テクスチャユニットはGL_TEXTURE0
	 * @param tex_unit
	 * @param width
	 * @param height
	 */
	public TextureOffscreen(final int tex_unit,
		final int width, final int height) {

		this(GLES20.GL_TEXTURE_2D, tex_unit, -1,
			width, height,
			false, DEFAULT_ADJUST_POWER2);
	}

	/**
	 * コンストラクタ(GL_TEXTURE_2D)
	 * テクスチャユニットはGL_TEXTURE0
	 * @param width dimension of offscreen(width)
	 * @param height dimension of offscreen(height)
	 * @param use_depth_buffer set true if you use depth buffer. the depth is fixed as 16bits
	 */
	public TextureOffscreen(final int width, final int height,
		final boolean use_depth_buffer) {

		this(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE0, -1,
			width, height, use_depth_buffer, DEFAULT_ADJUST_POWER2);
	}

	/**
	 * 既存のテクスチャ(GL_TEXTURE_2D)をwrapするためのコンストラクタ
	 * テクスチャユニットはGL_TEXTURE0
	 * @param tex_unit
	 * @param width
	 * @param height
	 * @param use_depth_buffer
	 */
	public TextureOffscreen(final int tex_unit,
		final int width, final int height, final boolean use_depth_buffer) {

		this(GLES20.GL_TEXTURE_2D, tex_unit, -1,
			width, height,
			use_depth_buffer, DEFAULT_ADJUST_POWER2);
	}
	
	/**
	 * コンストラクタ(GL_TEXTURE_2D)
	 * テクスチャユニットはGL_TEXTURE0
	 * @param width
	 * @param height
	 * @param use_depth_buffer
	 * @param adjust_power2
	 */
	public TextureOffscreen(final int width, final int height,
		final boolean use_depth_buffer, final boolean adjust_power2) {

		this(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE0, -1,
			width, height, use_depth_buffer, adjust_power2);
	}
	
	/**
	 * コンストラクタ(GL_TEXTURE_2D)
	 * @param tex_unit
	 * @param width
	 * @param height
	 * @param use_depth_buffer
	 * @param adjust_power2
	 */
	public TextureOffscreen(final int tex_unit,
		final int width, final int height,
		final boolean use_depth_buffer, final boolean adjust_power2) {

		this(GLES20.GL_TEXTURE_2D, tex_unit, -1,
			width, height, use_depth_buffer, adjust_power2);
	}

	/**
	 * 既存のテクスチャ(GL_TEXTURE_2D)をwrapするためのコンストラクタ, デプスバッファなし
	 * @param tex_id
	 * @param tex_unit
	 * @param width
	 * @param height
	 */
	public TextureOffscreen(final int tex_unit, final int tex_id,
		final int width, final int height) {

		this(GLES20.GL_TEXTURE_2D, tex_unit, tex_id,
			width, height,
			false, DEFAULT_ADJUST_POWER2);
	}

	/**
	 * 既存のテクスチャ(GL_TEXTURE_2D)をwrapするためのコンストラクタ
	 * @param tex_unit
	 * @param tex_id
	 * @param width
	 * @param height
	 * @param use_depth_buffer
	 */
	public TextureOffscreen(final int tex_unit, final int tex_id,
		final int width, final int height, final boolean use_depth_buffer) {

		this(GLES20.GL_TEXTURE_2D, tex_unit, tex_id,
			width, height,
			use_depth_buffer, DEFAULT_ADJUST_POWER2);
	}

	/**
	 * 既存のテクスチャをwrapするためのコンストラクタ
	 * @param tex_target GL_TEXTURE_2D
	 * @param tex_id
	 * @param width
	 * @param height
	 * @param use_depth_buffer
	 * @param adjust_power2
	 */
	public TextureOffscreen(final int tex_target, final int tex_unit, final int tex_id,
		final int width, final int height,
		final boolean use_depth_buffer, final boolean adjust_power2) {

		if (DEBUG) Log.v(TAG, "Constructor");
		TEX_TARGET = tex_target;
		TEX_UNIT = tex_unit;
		mWidth = width;
		mHeight = height;
		mHasDepthBuffer = use_depth_buffer;
		mAdjustPower2 = adjust_power2;

		createFrameBuffer(width, height);
		int tex = tex_id;
		if (tex < 0) {
			tex = genTexture(tex_target, tex_unit, mTexWidth, mTexHeight);
		}
		assignTexture(tex, width, height);
	}

	/** 破棄する */
	public void release() {
		if (DEBUG) Log.v(TAG, "release");
		releaseFrameBuffer();
	}

	/**
	 * オフスクリーン描画用のレンダリングバッファに切り替える
	 * Viewportも変更になるので必要であればunbind後にViewportの設定をすること
	 */
	public void bind() {
//		if (DEBUG) Log.v(TAG, "bind:");
		GLES20.glActiveTexture(TEX_UNIT);
		GLES20.glBindTexture(TEX_TARGET, mFBOTextureName);
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferObj);
		GLES20.glViewport(0, 0, mWidth, mHeight);
	}

	/**
	 * デフォルトのレンダリングバッファに戻す
	 */
	public void unbind() {
//		if (DEBUG) Log.v(TAG, "unbind:");
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		GLES20.glActiveTexture(TEX_UNIT);
		GLES20.glBindTexture(TEX_TARGET, 0);
	}

	private final float[] mResultMatrix = new float[16];
	/**
	 * get copy of texture matrix
	 * @return
	 */
	public float[] getTexMatrix() {
		System.arraycopy(mTexMatrix, 0, mResultMatrix, 0, 16);
		return mResultMatrix;
	}

	/**
	 * テクスチャ座標変換行列を取得(内部配列を直接返すので変更時は要注意)
	 * @return
	 */
	public float[] getRawTexMatrix() {
		return mTexMatrix;
	}

	/**
	 * テクスチャ変換行列のコピーを返す
	 * 領域チェックしていないのでoffset位置から16個以上確保しておくこと
	 * @param matrix
	 */
	public void getTexMatrix(final float[] matrix, final int offset) {
		System.arraycopy(mTexMatrix, 0, matrix, offset, mTexMatrix.length);
	}

	/**
	 * オフスクリーンテクスチャ名を取得
	 * このオフスクリーンへ書き込んだ画像をテクスチャとして使って他の描画を行う場合に使用できる
	 * @return
	 */
	public int getTexture() {
		return mFBOTextureName;
	}

	/** 指定したテクスチャをこのオフスクリーンに割り当てる */
	public void assignTexture(final int texture_name,
		final int width, final int height) {

		if ((width > mTexWidth) || (height > mTexHeight)) {
			mWidth = width;
			mHeight = height;
			releaseFrameBuffer();
			createFrameBuffer(width, height);
		}
		mFBOTextureName = texture_name;
		GLES20.glActiveTexture(TEX_UNIT);
		 // フレームバッファオブジェクトをbindする
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferObj);
		GLHelper.checkGlError("glBindFramebuffer " + mFrameBufferObj);
		// フレームバッファにカラーバッファ(テクスチャ)を接続する
		GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
			TEX_TARGET, mFBOTextureName, 0);
		GLHelper.checkGlError("glFramebufferTexture2D");

		if (mHasDepthBuffer) {
			// フレームバッファにデプスバッファを接続する
			GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER,
				GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, mDepthBufferObj);
			GLHelper.checkGlError("glFramebufferRenderbuffer");
		}

		// 正常に終了したかどうかを確認する
		final int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
		if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
			throw new RuntimeException("Framebuffer not complete, status=" + status);
		}

		 // デフォルトのフレームバッファに戻す
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

		// テクスチャ座標変換行列を初期化
		Matrix.setIdentityM(mTexMatrix, 0);
		mTexMatrix[0] = width / (float)mTexWidth;
		mTexMatrix[5] = height / (float)mTexHeight;
	}

	/** Bitmapからテクスチャを読み込む */
	public void loadBitmap(final Bitmap bitmap) {
		final int width = bitmap.getWidth();
		final int height = bitmap.getHeight();
		if ((width > mTexWidth) || (height > mTexHeight)) {
			mWidth = width;
			mHeight = height;
			releaseFrameBuffer();
			createFrameBuffer(width, height);
		}
		GLES20.glActiveTexture(TEX_UNIT);
		GLES20.glBindTexture(TEX_TARGET, mFBOTextureName);
		GLUtils.texImage2D(TEX_TARGET, 0, bitmap, 0);
		GLES20.glBindTexture(TEX_TARGET, 0);
		// initialize texture matrix
		Matrix.setIdentityM(mTexMatrix, 0);
		mTexMatrix[0] = width / (float)mTexWidth;
		mTexMatrix[5] = height / (float)mTexHeight;
	}
	
	/**
	 * カラーバッファのためにテクスチャを生成する
	 * @param tex_target
	 * @param tex_unit
	 * @param tex_width
	 * @param tex_height
	 * @return
	 */
	private static int genTexture(final int tex_target, final int tex_unit,
		final int tex_width, final int tex_height) {
		// カラーバッファのためにテクスチャを生成する
		final int tex_name = GLHelper.initTex(tex_target, tex_unit,
			GLES20.GL_LINEAR, GLES20.GL_LINEAR, GLES20.GL_CLAMP_TO_EDGE);
		// テクスチャのメモリ領域を確保する
		GLES20.glTexImage2D(tex_target, 0, GLES20.GL_RGBA, tex_width, tex_height, 0,
			GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
		GLHelper.checkGlError("glTexImage2D");
		return tex_name;
	}
	
	/** オフスクリーン描画用のフレームバッファオブジェクトを生成する */
	private final void createFrameBuffer(final int width, final int height) {
		final int[] ids = new int[1];

		if (mAdjustPower2) {
			// テクスチャのサイズは2の乗数にする
			int w = 1;
			for (; w < width; w <<= 1) ;
			int h = 1;
			for (; h < height; h <<= 1) ;
			if (mTexWidth != w || mTexHeight != h) {
				mTexWidth = w;
				mTexHeight = h;
			}
		} else {
			mTexWidth = width;
			mTexHeight = height;
		}

		if (mHasDepthBuffer) {
			// デプスバッファが必要な場合は、レンダーバッファオブジェクトを生成・初期化する
			GLES20.glGenRenderbuffers(1, ids, 0);
			mDepthBufferObj = ids[0];
			GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, mDepthBufferObj);
			// デプスバッファは16ビット
			GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER,
				GLES20.GL_DEPTH_COMPONENT16, mTexWidth, mTexHeight);
		}
		// フレームバッファオブジェクトを生成してbindする
		GLES20.glGenFramebuffers(1, ids, 0);
		GLHelper.checkGlError("glGenFramebuffers");
		mFrameBufferObj = ids[0];
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferObj);
		GLHelper.checkGlError("glBindFramebuffer " + mFrameBufferObj);

		// デフォルトのフレームバッファに戻す
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

	}

	/** オフスクリーンフレームバッファを破棄 */
    private final void releaseFrameBuffer() {
        final int[] names = new int[1];
		// デプスバッファがある時はデプスバッファを破棄
		if (mDepthBufferObj >= 0) {
			names[0] = mDepthBufferObj;
			GLES20.glDeleteRenderbuffers(1, names, 0);
			mDepthBufferObj = -1;
		}
		// オフスクリーンのカラーバッファ用のテクスチャを破棄
		if (mFBOTextureName >= 0) {
			names[0] = mFBOTextureName;
			GLES20.glDeleteTextures(1, names, 0);
			mFBOTextureName = -1;
		}
		// オフスクリーンのフレームバッファーオブジェクトを破棄
		if (mFrameBufferObj >= 0) {
			names[0] = mFrameBufferObj;
			GLES20.glDeleteFramebuffers(1, names, 0);
			mFrameBufferObj = -1;
		}
    }

	/**
	 * get dimension(width) of this offscreen
	 * @return
	 */
	public int getWidth() {
		return mWidth;
	}

	/**
	 * get dimension(height) of this offscreen
	 * @return
	 */
	public int getHeight() {
		return mHeight;
	}

	/**
	 * get backing texture dimension(width) of this offscreen
	 * @return
	 */
	public int getTexWidth() {
		return mTexWidth;
	}

	/**
	 * get backing texture dimension(height) of this offscreen
	 * @return
	 */
	public int getTexHeight() {
		return mTexHeight;
	}
	
	public int getTexTarget() {
		return TEX_TARGET;
	}
	
	public int getTexUnit() {
		return TEX_UNIT;
	}
}
