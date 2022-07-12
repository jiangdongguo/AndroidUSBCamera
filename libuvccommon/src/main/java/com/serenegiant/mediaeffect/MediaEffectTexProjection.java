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

import android.graphics.Matrix;
import android.opengl.GLES20;
import androidx.annotation.NonNull;
import android.util.Log;

import com.serenegiant.glutils.GLHelper;

import static com.serenegiant.glutils.ShaderConst.*;

/**
 * うまく動かない, Matrixの計算がダメなのかも
 */
public class MediaEffectTexProjection extends MediaEffectGLESBase {
	private static final boolean DEBUG = false;
	private static final String TAG = "MediaEffectTexProjection";

	private static class MediaEffectTexProjectionDrawer
		extends MediaEffectDrawer.MediaEffectSingleDrawer {

		private float[] texMatrix2 = new float[9];
		private final int muTexMatrixLoc2;

		public MediaEffectTexProjectionDrawer(final String vss, final String fss) {
			super(false, vss, fss);
			muTexMatrixLoc2 = GLES20.glGetUniformLocation(getProgram(), "uTexMatrix2");
			GLHelper.checkLocation(muTexMatrixLoc2, "uTexMatrix2");
			reset();
		}

		@Override
		protected void preDraw(@NonNull final int[] tex_ids,
			final float[] tex_matrix, final int offset) {

			// テクスチャ変換行列をセット
			if (muTexMatrixLoc2 >= 0) {
				GLES20.glUniformMatrix3fv(muTexMatrixLoc2, 1, false, texMatrix2, 0);
				GLHelper.checkGlError("glUniformMatrix3fv");
			}
			super.preDraw(tex_ids, tex_matrix, offset);
		}

		public void reset() {
			if (DEBUG) Log.v(TAG, "reset:");
			setTexProjection(new float[] {
				1.0f, 0.0f, 0.0f,
				0.0f, 1.0f, 0.0f,
				0.0f, 0.0f, 1.0f,
			});
		}

		/**
		 * テクスチャ座標を変換するための3x3行列をセットする
		 * @param matrix
		 */
		public void setTexProjection(final float[] matrix) {
//			if (DEBUG) Log.v(TAG, "setTexProjection:");
			synchronized (mSync) {
/*	 うまく動かない
				texMatrix2[0] = matrix[0];	// MSCALE_X
				texMatrix2[1] = matrix[3];	// MSKEW_Y
				texMatrix2[2] = matrix[6];	// MPERSP_0
				texMatrix2[3] = matrix[1];	// MSKEW_X
				texMatrix2[4] = matrix[4];	// MSCALE_Y
				texMatrix2[5] = matrix[7];	// MPERSP_1
				texMatrix2[6] = matrix[2];	// MTRANS_X
				texMatrix2[7] = matrix[5];	// MTRANS_Y
				texMatrix2[8] = matrix[8];	// MPERSP_2 */
/* うまく動かない.この値はcv::getPerspectiveTransformで計算したもの
				texMatrix2[0] = 1.000000f;	// 0
				texMatrix2[1] = 0.000000f;	// 3
				texMatrix2[2] = 0.000000f;	// 6
				texMatrix2[3] = 5.225964f;	// 1
				texMatrix2[4] = 7.009858f;	// 4
				texMatrix2[5] = 0.016331f;	// 7
				texMatrix2[6] = 0.000000f;	// 2
				texMatrix2[7] = -0.000000f;	// 5
				texMatrix2[8] = 1.000000f;	// 8 */
/*	うまく動かない
//				System.arraycopy(matrix, 0, texMatrix2, 0, 9); */
/*	うまく動かない
				texMatrix2[0] = matrix[6];
				texMatrix2[1] = matrix[3];
				texMatrix2[2] = matrix[0];
				texMatrix2[3] = matrix[7];
				texMatrix2[4] = matrix[4];
				texMatrix2[5] = matrix[1];
				texMatrix2[6] = matrix[8];
				texMatrix2[7] = matrix[5];
				texMatrix2[8] = matrix[2]; */
/* うまく動かない
				texMatrix2[0] = matrix[8];
				texMatrix2[1] = matrix[5];
				texMatrix2[2] = matrix[2];
				texMatrix2[3] = matrix[7];
				texMatrix2[4] = matrix[4];
				texMatrix2[5] = matrix[1];
				texMatrix2[6] = matrix[6];
				texMatrix2[7] = matrix[3];
				texMatrix2[8] = matrix[0]; */
/*
				texMatrix2[0] = matrix[8];
				texMatrix2[1] = matrix[7];
				texMatrix2[2] = matrix[6];
				texMatrix2[3] = matrix[5];
				texMatrix2[4] = matrix[4];
				texMatrix2[5] = matrix[3];
				texMatrix2[6] = matrix[2];
				texMatrix2[7] = matrix[1];
				texMatrix2[8] = matrix[0]; */
			}
		}
	}

	public static final String PROJ_VERTEX_SHADER = SHADER_VERSION +
		"uniform mat4 uMVPMatrix;\n" +		// モデルビュー変換行列
		"uniform mat4 uTexMatrix;\n" +		// テクスチャ変換行列
		"uniform mat3 uTexMatrix2;\n" +		// テクスチャ変換行列
		"attribute vec4 aPosition;\n" +		// 頂点座標
		"attribute vec4 aTextureCoord;\n" +	// テクスチャ情報
		"varying vec2 vTextureCoord;\n" +	// フラグメントシェーダーへ引き渡すテクスチャ座標
		"void main() {\n" +
			"gl_Position = uMVPMatrix * aPosition;\n" +				// 頂点座標を計算
			"vec3 tex_coord = vec3((uTexMatrix * aTextureCoord).xy, 1.0);\n" +		// テクスチャ座標を計算
			"vec3 temp = uTexMatrix2 * tex_coord;\n" +
			"vTextureCoord = temp.xy / temp.z;\n" +
//			"vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n" +
		"}\n";

	private static final String FRAGMENT_SHADER_BASE = SHADER_VERSION +
		"%s" +
		"#define KERNEL_SIZE3x3 " + MediaEffectKernel3x3Drawer.KERNEL_SIZE + "\n" +
		"precision highp float;\n" +
		"varying       vec2 vTextureCoord;\n" +
		"uniform %s    sTexture;\n" +
		"void main() {\n" +
			"gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
		"}\n";
	private static final String FRAGMENT_SHADER
		= String.format(FRAGMENT_SHADER_BASE, HEADER_2D, SAMPLER_2D);
	private static final String FRAGMENT_SHADER_EXT
		= String.format(FRAGMENT_SHADER_BASE, HEADER_OES, SAMPLER_OES);

	public MediaEffectTexProjection() {
		super(new MediaEffectTexProjectionDrawer(PROJ_VERTEX_SHADER, FRAGMENT_SHADER));
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
	}

	private final Matrix mat = new Matrix();
	private final float[] m = new float[9];
	/**
	 *
	 * @param src (x,y) pair, 4 pairs (4 points) = float[8]
	 * @param dst (x,y) pair, 4 pairs (4 points) = float[8]
	 */
	public void calcPerspectiveTransform(final float[] src, final float[] dst) {
//		if (DEBUG) Log.v(TAG, "calcPerspectiveTransform:");
		mat.reset();	// これはいらん?
		mat.setPolyToPoly(src, 0, dst, 0, 4);
		mat.getValues(m);
		((MediaEffectTexProjectionDrawer)mDrawer).setTexProjection(m);
	}

}
