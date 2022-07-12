package com.serenegiant.mediaeffect;
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

import android.opengl.GLES20;
import android.opengl.Matrix;
import androidx.annotation.NonNull;

import com.serenegiant.glutils.GLHelper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Locale;

import static com.serenegiant.glutils.ShaderConst.*;

public class MediaEffectDrawer {

	protected boolean mEnabled = true;

	private static final float[] VERTICES = { 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, -1.0f };
	private static final float[] TEXCOORD = { 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f };

	private static final int FLOAT_SZ = Float.SIZE / 8;
	private static final int VERTEX_NUM = 4;
	private static final int VERTEX_SZ = VERTEX_NUM * 2;

	public static MediaEffectDrawer newInstance() {
		return new MediaEffectSingleDrawer(false, VERTEX_SHADER, FRAGMENT_SHADER_2D);
	}

	public static MediaEffectDrawer newInstance(final int numTex) {
		if (numTex <= 1) {
			return new MediaEffectSingleDrawer(false, VERTEX_SHADER, FRAGMENT_SHADER_2D);
		} else {
			return new MediaEffectDrawer(numTex, false, VERTEX_SHADER, FRAGMENT_SHADER_2D);
		}
	}

	public static MediaEffectDrawer newInstance(final String fss) {
		return new MediaEffectSingleDrawer(false, VERTEX_SHADER, fss);
	}

	public static MediaEffectDrawer newInstance(final int numTex, final String fss) {
		if (numTex <= 1) {
			return new MediaEffectSingleDrawer(false, VERTEX_SHADER, fss);
		} else {
			return new MediaEffectDrawer(numTex, false, VERTEX_SHADER, fss);
		}
	}

	public static MediaEffectDrawer newInstance(final boolean isOES, final String fss) {
		return new MediaEffectSingleDrawer(isOES, VERTEX_SHADER, fss);
	}

	public static MediaEffectDrawer newInstance(final int numTex,
		final boolean isOES, final String fss) {

		if (numTex <= 1) {
			return new MediaEffectSingleDrawer(isOES, VERTEX_SHADER, fss);
		} else {
			return new MediaEffectDrawer(numTex, isOES, VERTEX_SHADER, fss);
		}
	}

	public static MediaEffectDrawer newInstance(final boolean isOES,
		final String vss, final String fss) {

		return new MediaEffectSingleDrawer(isOES, VERTEX_SHADER, fss);
	}
	
	public static MediaEffectDrawer newInstance(final int numTex,
		final boolean isOES, final String vss, final String fss) {

		if (numTex <= 1) {
			return new MediaEffectSingleDrawer(isOES, vss, fss);
		} else {
			return new MediaEffectDrawer(numTex, isOES, vss, fss);
		}
	}
	
	/**
	 * テクスチャを1枚しか使わない場合はこちらを使うこと
	 */
	protected static class MediaEffectSingleDrawer extends MediaEffectDrawer {
		protected MediaEffectSingleDrawer(
			final boolean isOES, final String vss, final String fss) {
			super(1, isOES, vss, fss);
		}

		/**
		 * テクスチャのバインド処理
		 * mSyncはロックされて呼び出される
		 * @param tex_ids texture ID
		 */
		protected void bindTexture(@NonNull final int[] tex_ids) {
			GLES20.glActiveTexture(TEX_NUMBERS[0]);
			if (tex_ids[0] != NO_TEXTURE) {
				GLES20.glBindTexture(mTexTarget, tex_ids[0]);
				GLES20.glUniform1i(muTexLoc[0], 0);
			}
		}
	
		/**
		 * 描画後の後処理, テクスチャのunbind, プログラムをデフォルトに戻す
		 * mSyncはロックされて呼び出される
		 */
		protected void unbindTexture() {
			GLES20.glActiveTexture(TEX_NUMBERS[0]);
			GLES20.glBindTexture(mTexTarget, 0);
		}
	}

	protected final Object mSync = new Object();
	protected final int mTexTarget;
	protected final int muMVPMatrixLoc;
	protected final int muTexMatrixLoc;
	protected final int[] muTexLoc;
	protected final float[] mMvpMatrix = new float[16];
	protected int hProgram;

	protected MediaEffectDrawer() {
		this(1, false, VERTEX_SHADER, FRAGMENT_SHADER_2D);
	}

	protected MediaEffectDrawer(final int numTex) {
		this(numTex, false, VERTEX_SHADER, FRAGMENT_SHADER_2D);
	}

	protected MediaEffectDrawer(final String fss) {
		this(1, false, VERTEX_SHADER, fss);
	}

	protected MediaEffectDrawer(final int numTex, final String fss) {
		this(numTex, false, VERTEX_SHADER, fss);
	}

	protected MediaEffectDrawer(final boolean isOES, final String fss) {
		this(1, isOES, VERTEX_SHADER, fss);
	}

	protected MediaEffectDrawer(final int numTex,
		final boolean isOES, final String fss) {

		this(numTex, isOES, VERTEX_SHADER, fss);
	}

	protected MediaEffectDrawer(final boolean isOES,
		final String vss, final String fss) {

		this(1, isOES, VERTEX_SHADER, fss);
	}
	
	protected MediaEffectDrawer(final int numTex,
		final boolean isOES, final String vss, final String fss) {

		mTexTarget = isOES ? GL_TEXTURE_EXTERNAL_OES : GL_TEXTURE_2D;
		final FloatBuffer pVertex = ByteBuffer.allocateDirect(VERTEX_SZ * FLOAT_SZ)
			.order(ByteOrder.nativeOrder()).asFloatBuffer();
		pVertex.put(VERTICES);
		pVertex.flip();
		final FloatBuffer pTexCoord = ByteBuffer.allocateDirect(VERTEX_SZ * FLOAT_SZ)
			.order(ByteOrder.nativeOrder()).asFloatBuffer();
		pTexCoord.put(TEXCOORD);
		pTexCoord.flip();

		// テクスチャ用のロケーションは最低でも1つは確保する
		muTexLoc = new int[numTex > 0 ? numTex : 1];
		hProgram = GLHelper.loadShader(vss, fss);
		GLES20.glUseProgram(hProgram);
		final int maPositionLoc = GLES20.glGetAttribLocation(hProgram, "aPosition");
		final int maTextureCoordLoc = GLES20.glGetAttribLocation(hProgram, "aTextureCoord");
        muMVPMatrixLoc = GLES20.glGetUniformLocation(hProgram, "uMVPMatrix");
        muTexMatrixLoc = GLES20.glGetUniformLocation(hProgram, "uTexMatrix");
        muTexLoc[0] = GLES20.glGetUniformLocation(hProgram, "sTexture");
        for (int i = 1; i < numTex; i++) {
			muTexLoc[i] = GLES20.glGetUniformLocation(hProgram,
				String.format(Locale.US, "sTexture%d", i + 1));
		}
        // モデルビュー変換行列を初期化
		Matrix.setIdentityM(mMvpMatrix, 0);
		//
		if (muMVPMatrixLoc >= 0) {
        	GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mMvpMatrix, 0);
		}
		if (muTexMatrixLoc >= 0) {
			// ここは単位行列に初期化するだけなのでmMvpMatrixを流用
        	GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, mMvpMatrix, 0);
		}
		// 頂点座標配列を割り当てる
		GLES20.glVertexAttribPointer(maPositionLoc,
			2, GLES20.GL_FLOAT, false, VERTEX_SZ, pVertex);
		GLES20.glEnableVertexAttribArray(maPositionLoc);
		// テクスチャ座標配列を割り当てる
		GLES20.glVertexAttribPointer(maTextureCoordLoc,
			2, GLES20.GL_FLOAT, false, VERTEX_SZ, pTexCoord);
		GLES20.glEnableVertexAttribArray(maTextureCoordLoc);
	}

	public void release() {
		GLES20.glUseProgram(0);
		if (hProgram >= 0) {
			GLES20.glDeleteProgram(hProgram);
		}
		hProgram = -1;
	}

	protected int getProgram() {
		return hProgram;
	}

	public float[] getMvpMatrix() {
		return mMvpMatrix;
	}

	/**
	 * このクラスでは何もしない, 必要なら下位クラスでオーバーライドすること
	 * @param width
	 * @param height
	 */
	public void setTexSize(final int width, final int height) {
	}

	/**
	 * モデルビュー変換行列に行列を割り当てる
	 * @param matrix 領域チェックしていないのでoffsetから16個以上必須
	 * @param offset
	 * @return
	 */
	public void setMvpMatrix(final float[] matrix, final int offset) {
		synchronized (mSync) {
			System.arraycopy(matrix, offset, mMvpMatrix, 0, mMvpMatrix.length);
		}
	}

	/**
	 * モデルビュー変換行列のコピーを取得
	 * @param matrix 領域チェックしていないのでoffsetから16個以上必須
	 * @param offset
	 */
	public void getMvpMatrix(final float[] matrix, final int offset) {
		System.arraycopy(mMvpMatrix, 0, matrix, offset, mMvpMatrix.length);
	}

	/**
	 * preDraw => draw => postDrawを順に呼び出す
	 * @param tex_ids texture ID
	 * @param tex_matrix テクスチャ変換行列、nullならば以前に適用したものが再利用される.
	 * 			領域チェックしていないのでoffsetから16個以上確保しておくこと
	 * @param offset テクスチャ変換行列のオフセット
	 */
	public void apply(@NonNull final int[] tex_ids, final float[] tex_matrix, final int offset) {
		synchronized (mSync) {
			GLES20.glUseProgram(hProgram);
			preDraw(tex_ids, tex_matrix, offset);
			draw(tex_ids, tex_matrix, offset);
			postDraw();
		}
	}

	/**
	 * 描画の前処理
	 * テクスチャ変換行列/モデルビュー変換行列を代入, テクスチャをbindする
	 * mSyncはロックされて呼び出される
	 * @param tex_ids texture ID
	 * @param tex_matrix テクスチャ変換行列、nullならば以前に適用したものが再利用される.
	 * 			領域チェックしていないのでoffsetから16個以上確保しておくこと
	 * @param offset テクスチャ変換行列のオフセット
	 */
	protected void preDraw(@NonNull final int[] tex_ids, final float[] tex_matrix, final int offset) {
		if ((muTexMatrixLoc >= 0) && (tex_matrix != null)) {
			GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, tex_matrix, offset);
		}
		if (muMVPMatrixLoc >= 0) {
			GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mMvpMatrix, 0);
		}
		bindTexture(tex_ids);
	}

	protected void bindTexture(@NonNull final int[] tex_ids) {
		final int n = tex_ids.length < muTexLoc.length
			? tex_ids.length : muTexLoc.length;
		for (int i = 0; i < n; i++) {
			if (tex_ids[i] != NO_TEXTURE) {
				GLES20.glActiveTexture(TEX_NUMBERS[i]);
				GLES20.glBindTexture(mTexTarget, tex_ids[i]);
				GLES20.glUniform1i(muTexLoc[i], i);
			}
		}
	}
	
	/**
	 * 実際の描画実行, GLES20.glDrawArraysを呼び出すだけ
	 * mSyncはロックされて呼び出される
	 * @param tex_ids texture ID
	 * @param tex_matrix テクスチャ変換行列、nullならば以前に適用したものが再利用される.
	 * 			領域チェックしていないのでoffsetから16個以上確保しておくこと
	 * @param offset テクスチャ変換行列のオフセット
	 */
	protected void draw(@NonNull final int[] tex_ids, final float[] tex_matrix, final int offset) {
//		if (DEBUG) Log.v(TAG, "draw");
		// これが実際の描画
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTEX_NUM);
	}

	/**
	 * 描画後の後処理, テクスチャのunbind, プログラムをデフォルトに戻す
	 * mSyncはロックされて呼び出される
	 */
	protected void postDraw() {
		unbindTexture();
        GLES20.glUseProgram(0);
	}

	protected void unbindTexture() {
		for (int i = 0; i < muTexLoc.length; i++) {
			GLES20.glActiveTexture(TEX_NUMBERS[i]);
			GLES20.glBindTexture(mTexTarget, 0);
		}
	}
}
