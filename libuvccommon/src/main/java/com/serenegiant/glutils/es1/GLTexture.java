package com.serenegiant.glutils.es1;
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
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.opengl.GLES10;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.text.TextUtils;

import com.serenegiant.glutils.ITexture;

import java.io.IOException;

/**
 * OpenGL|ESのテクスチャ操作用のヘルパークラス
 */
public class GLTexture implements ITexture {
//	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
//	private static final String TAG = "GLTexture";

	/* package */final int mTextureTarget;
	/* package */final int mTextureUnit;
	/* package */int mTextureId;
	/* package */final float[] mTexMatrix = new float[16];	// テクスチャ変換行列
	/* package */int mTexWidth, mTexHeight;
	/* package */int mImageWidth, mImageHeight;
	
	/**
	 * コンストラクタ
	 * テクスチャユニットが常時GL_TEXTURE0なので複数のテクスチャを同時に使えない
	 * @param width
	 * @param height
	 * @param filter_param
	 */
	public GLTexture(final int width, final int height, final int filter_param) {
		this(GLES10.GL_TEXTURE_2D, GLES10.GL_TEXTURE0, width, height, filter_param);
	}
	
	/**
	 * コンストラクタ
	 * @param texTarget GL_TEXTURE_EXTERNAL_OESはだめ
	 * @param texUnit
	 * @param width テクスチャサイズ
	 * @param height テクスチャサイズ
	 * @param filter_param	テクスチャの補間方法を指定 GL_LINEARとかGL_NEAREST
	 */
	public GLTexture(final int texTarget, final int texUnit,
		final int width, final int height, final int filter_param) {

//		if (DEBUG) Log.v(TAG, String.format("コンストラクタ(%d,%d)", width, height));
		mTextureTarget = texTarget;
		mTextureUnit = texUnit;
		// テクスチャに使うビットマップは縦横サイズが2の乗数でないとダメ。
		// 更に、ミップマップするなら正方形でないとダメ
		// 指定したwidth/heightと同じか大きい2の乗数にする
		int w = 32;
		for (; w < width; w <<= 1);
		int h = 32;
		for (; h < height; h <<= 1);
		if (mTexWidth != w || mTexHeight != h) {
			mTexWidth = w;
			mTexHeight = h;
		}
//		if (DEBUG) Log.v(TAG, String.format("texSize(%d,%d)", mTexWidth, mTexHeight));
		mTextureId = GLHelper.initTex(mTextureTarget, filter_param);
		// テクスチャのメモリ領域を確保する
		GLES10.glTexImage2D(mTextureTarget,
			0,							// ミップマップレベル0(ミップマップしない)
			GLES10.GL_RGBA,				// 内部フォーマット
			mTexWidth, mTexHeight,		// サイズ
			0,							// 境界幅
			GLES10.GL_RGBA,				// 引き渡すデータのフォーマット
			GLES10.GL_UNSIGNED_BYTE,		// データの型
			null);						// ピクセルデータ無し
		// テクスチャ変換行列を初期化
		Matrix.setIdentityM(mTexMatrix, 0);
		mTexMatrix[0] = width / (float)mTexWidth;
		mTexMatrix[5] = height / (float)mTexHeight;
//		if (DEBUG) Log.v(TAG, "GLTexture:id=" + mTextureId);
	}

	@Override
	protected void finalize() throws Throwable {
		release();	// GLコンテキスト内じゃない可能性があるのであまり良くないけど
		super.finalize();
	}

	/**
	 * テクスチャを破棄
	 * GLコンテキスト/EGLレンダリングコンテキスト内で呼び出すこと
	 */
	@Override
	public void release() {
//		if (DEBUG) Log.v(TAG, "release:");
		if (mTextureId > 0) {
			GLHelper.deleteTex(mTextureId);
			mTextureId = 0;
		}
	}

	/**
	 * このインスタンスで管理しているテクスチャを有効にする(バインドする)
	 */
	@Override
	public void bind() {
//		if (DEBUG) Log.v(TAG, "bind:");
		GLES10.glActiveTexture(mTextureUnit);	// テクスチャユニットを選択
		GLES10.glBindTexture(mTextureTarget, mTextureId);
	}

	/**
	 * このインスタンスで管理しているテクスチャを無効にする(アンバインドする)
	 */
	@Override
	public void unbind() {
//		if (DEBUG) Log.v(TAG, "unbind:");
		GLES10.glActiveTexture(mTextureUnit);	// テクスチャユニットを選択
		GLES10.glBindTexture(mTextureTarget, 0);
	}

	/**
	 * テクスチャターゲットを取得(GL_TEXTURE_2D)
	 * @return
	 */
	@Override
	public int getTexTarget() { return mTextureTarget; }
	/**
	 * テクスチャ名を取得
	 * @return
	 */
	@Override
	public int getTexture() { return mTextureId; }
	/**
	 * テクスチャ座標変換行列を取得(内部配列をそのまま返すので変更時は要注意)
	 * @return
	 */
	@Override
	public float[] getTexMatrix() { return mTexMatrix; }
	/**
	 * テクスチャ座標変換行列のコピーを取得
	 * @param matrix 領域チェックしていないのでoffset位置から16個以上確保しておくこと
	 * @param offset
	 */
	@Override
	public void getTexMatrix(final float[] matrix, final int offset) {
		System.arraycopy(mTexMatrix, 0, matrix, offset, mTexMatrix.length);
	}
	/**
	 * テクスチャ幅を取得
	 * @return
	 */
	@Override
	public int getTexWidth() { return mTexWidth; }
	/**
	 * テクスチャ高さを取得
	 * @return
	 */
	@Override
	public int getTexHeight() { return mTexHeight; }

	/**
	 * 指定したファイルから画像をテクスチャに読み込む
	 * ファイルが存在しないか読み込めなければIOException/NullPointerExceptionを生成
	 * @param filePath
	 */
	@Override
	public void loadTexture(final String filePath) throws NullPointerException, IOException {
//		if (DEBUG) Log.v(TAG, "loadTexture:path=" + filePath);
		if (TextUtils.isEmpty(filePath))
			throw new NullPointerException("image file path should not be a null");
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;	// Bitmapを生成せずにサイズ等の情報だけを取得する
		BitmapFactory.decodeFile(filePath, options);
		// テキスチャサイズ内に指定したイメージが収まるためのサブサンプリングを値を求める
		final int imageWidth = options.outWidth;
		final int imageHeight = options.outHeight;
		int inSampleSize = 1;	// サブサンプリングサイズ
		if ((imageHeight > mTexHeight) || (imageWidth > mTexWidth)) {
			if (imageWidth > imageHeight) {
				inSampleSize = (int)Math.ceil(imageHeight / (float)mTexHeight);
			} else {
				inSampleSize = (int)Math.ceil(imageWidth / (float)mTexWidth);
			}
		}
//		if (DEBUG) Log.v(TAG, String.format("image(%d,%d),tex(%d,%d),inSampleSize=%d", imageWidth, imageHeight, mTexWidth, mTexHeight, inSampleSize));
		// 実際の読み込み処理
		options.inSampleSize = inSampleSize;
		options.inJustDecodeBounds = false;
		loadTexture(BitmapFactory.decodeFile(filePath, options));
	}
	
	/**
	 * ビットマップからテクスチャを読み込む
	 * @param bitmap
	 * @throws NullPointerException
	 */
	@Override
	public void loadTexture(final Bitmap bitmap) throws NullPointerException {
		mImageWidth = bitmap.getWidth();	// 読み込んだイメージのサイズを取得
		mImageHeight = bitmap.getHeight();
		Bitmap texture = Bitmap.createBitmap(mTexWidth, mTexHeight, Bitmap.Config.ARGB_8888);
		final Canvas canvas = new Canvas(texture);
		canvas.drawBitmap(bitmap, 0, 0, null);
		bitmap.recycle();
		// テクスチャ座標変換行列を設定(読み込んだイメージサイズがテクスチャサイズにフィットするようにスケール変換)
		Matrix.setIdentityM(mTexMatrix, 0);
		mTexMatrix[0] = mImageWidth / (float)mTexWidth;
		mTexMatrix[5] = mImageHeight / (float)mTexHeight;
//		if (DEBUG) Log.v(TAG, String.format("image(%d,%d),scale(%f,%f)", mImageWidth, mImageHeight, mMvpMatrix[0], mMvpMatrix[5]));
		bind();
		GLUtils.texImage2D(mTextureTarget, 0, texture, 0);
		unbind();
		texture.recycle();
	}
}
