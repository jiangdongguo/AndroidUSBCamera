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
import androidx.annotation.NonNull;

import com.serenegiant.glutils.GLHelper;

import static com.serenegiant.glutils.ShaderConst.*;

/** Dilation(膨張)フィルタ */
public class MediaEffectDilation extends MediaEffectGLESBase {
	private static final boolean DEBUG = false;
	private static final String TAG = "MediaEffectDilation";

	private static class MediaEffectDilationDrawer
		extends MediaEffectDrawer.MediaEffectSingleDrawer {

		private final int muTexOffsetLoc;	// テクスチャオフセット
		private final float[] mTexOffset = new float[82];
		private float mTexWidth;
		private float mTexHeight;
		public MediaEffectDilationDrawer(final String fss) {
			super(false, VERTEX_SHADER, fss);
			muTexOffsetLoc = GLES20.glGetUniformLocation(getProgram(), "uTexOffset");
			GLHelper.checkLocation(muTexOffsetLoc, "uTexOffset");
		}

		@Override
		protected void preDraw(@NonNull final int[] tex_ids,
			final float[] tex_matrix, final int offset) {

			super.preDraw(tex_ids, tex_matrix, offset);
			// テクセルオフセット
			if (muTexOffsetLoc >= 0) {
				GLES20.glUniform2fv(muTexOffsetLoc, 41, mTexOffset, 0);
			}
		}

		/**
		 * Sets the size of the texture.  This is used to find adjacent texels when filtering.
		 */
		public void setTexSize(final int width, final int height) {
			synchronized (mSync) {
				mTexHeight = height;
				mTexWidth = width;
				final float rw = 1.0f / width;
				final float rh = 1.0f / height;

				// 中央
				mTexOffset[0] = 0;		mTexOffset[1] = 0;
				// 1ステップ
				mTexOffset[2] = 0;		mTexOffset[3] = -rh;
				mTexOffset[4] = 0;		mTexOffset[5] = +rh;
				mTexOffset[6] = -rw;	mTexOffset[7] = 0;
				mTexOffset[8] = +rw;	mTexOffset[9] = 0;
				// 2ステップ
				mTexOffset[10] = 0;				mTexOffset[11] = -rh * 2.0f;
				mTexOffset[12] = 0;				mTexOffset[13] = +rh * 2.0f;
				mTexOffset[14] = -rw * 2.0f;	mTexOffset[15] = 0;
				mTexOffset[16] = +rw * 2.0f;	mTexOffset[17] = 0;
				mTexOffset[18] = -rw;			mTexOffset[19] = -rh;
				mTexOffset[20] = -rw;			mTexOffset[21] = +rh;
				mTexOffset[22] = +rw;			mTexOffset[23] = -rh;
				mTexOffset[24] = +rw;			mTexOffset[25] = +rh;
				// 3ステップ
				mTexOffset[26] = 0;				mTexOffset[27] = -rh * 3.0f;
				mTexOffset[28] = 0;				mTexOffset[29] = +rh * 3.0f;
				mTexOffset[30] = -rw * 3.0f;	mTexOffset[31] = 0;
				mTexOffset[32] = +rw * 3.0f;	mTexOffset[33] = 0;
				mTexOffset[34] = -rw * 2.0f;	mTexOffset[35] = -rh;
				mTexOffset[36] = -rw * 2.0f;	mTexOffset[37] = +rh;
				mTexOffset[38] = +rw * 2.0f;	mTexOffset[39] = -rh;
				mTexOffset[40] = +rw * 2.0f;	mTexOffset[41] = +rh;
				mTexOffset[42] = -rw;			mTexOffset[43] = -rh * 2.0f;
				mTexOffset[44] = -rw;			mTexOffset[45] = +rh * 2.0f;
				mTexOffset[46] = +rw;			mTexOffset[47] = -rh * 2.0f;
				mTexOffset[48] = +rw;			mTexOffset[49] = +rh * 2.0f;
				// 4ステップ
				mTexOffset[50] = 0;				mTexOffset[51] = -rh * 4.0f;
				mTexOffset[52] = 0;				mTexOffset[53] = +rh * 4.0f;
				mTexOffset[54] = -rw * 4.0f;	mTexOffset[55] = 0;
				mTexOffset[56] = +rw * 4.0f;	mTexOffset[57] = 0;
				mTexOffset[58] = -rw * 3.0f;	mTexOffset[59] = -rh;
				mTexOffset[60] = -rw * 3.0f;	mTexOffset[61] = +rh;
				mTexOffset[62] = +rw * 3.0f;	mTexOffset[63] = -rh;
				mTexOffset[64] = +rw * 3.0f;	mTexOffset[65] = +rh;
				mTexOffset[66] = -rw * 2.0f;	mTexOffset[67] = -rh * 2.0f;
				mTexOffset[68] = -rw * 2.0f;	mTexOffset[69] = +rh * 2.0f;
				mTexOffset[70] = +rw * 2.0f;	mTexOffset[71] = -rh * 2.0f;
				mTexOffset[72] = +rw * 2.0f;	mTexOffset[73] = +rh * 2.0f;
				mTexOffset[74] = -rw;			mTexOffset[75] = -rh * 3.0f;
				mTexOffset[76] = -rw;			mTexOffset[77] = +rh * 3.0f;
				mTexOffset[78] = +rw;			mTexOffset[79] = -rh * 3.0f;
				mTexOffset[80] = +rw;			mTexOffset[81] = +rh * 3.0f;
			}
		}
	}

	public static final String FRAGMENT_SHADER_1 =
		"precision lowp float;\n" +
		"varying       vec2 vTextureCoord;\n" +
		"uniform vec2  uTexOffset[41];\n" +
		"uniform sampler2D sTexture;\n" +
		"\n" +
		"void main()\n" +
		"{\n" +
			"vec4 maxValue = texture2D(sTexture, vTextureCoord + uTexOffset[0] );\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[1] ));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[2] ));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[3] ));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[4] ));\n" +
			"\n" +
			"gl_FragColor = vec4(maxValue.rgb, 1.0);\n" +
		"}\n";

	public static final String FRAGMENT_SHADER_2 =
		"precision lowp float;\n" +
		"\n" +
		"varying       vec2 vTextureCoord;\n" +
		"uniform vec2  uTexOffset[41];\n" +
		"uniform sampler2D sTexture;\n" +
		"\n" +
		"void main()\n" +
		"{\n" +
			"vec4 maxValue = texture2D(sTexture, vTextureCoord + uTexOffset[0]);\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[1]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[2]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[3]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[4]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[5]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[6]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[7]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[8]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[9]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[10]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[11]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[12]));\n" +
			"\n" +
			"gl_FragColor = vec4(maxValue.rgb, 1.0);\n" +
		"}\n";

	public static final String FRAGMENT_SHADER_3 =
		"precision lowp float;\n" +
		"varying       vec2 vTextureCoord;\n" +
		"uniform vec2  uTexOffset[41];\n" +
		"uniform sampler2D sTexture;\n" +
		"\n" +
		"void main()\n" +
		"{\n" +
			"vec4 maxValue = texture2D(sTexture, vTextureCoord + uTexOffset[0]);\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[1]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[2]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[3]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[4]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[5]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[6]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[7]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[8]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[9]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[10]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[11]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[12]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[13]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[14]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[15]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[16]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[17]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[18]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[19]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[20]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[21]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[22]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[23]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[24]));\n" +
			"\n" +
			"gl_FragColor = vec4(maxValue.rgb, 1.0);\n" +
		"}\n";

	public static final String FRAGMENT_SHADER_4 =
		"precision lowp float;\n" +
		"varying       vec2 vTextureCoord;\n" +
		"uniform vec2  uTexOffset[41];\n" +
		"uniform sampler2D sTexture;\n" +
		"\n" +
		"void main()\n" +
		"{\n" +
			"vec4 maxValue = texture2D(sTexture, vTextureCoord + uTexOffset[0]);\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[1]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[2]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[3]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[4]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[5]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[6]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[7]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[8]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[9]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[10]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[11]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[12]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[13]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[14]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[15]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[16]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[17]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[18]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[19]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[20]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[21]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[22]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[23]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[24]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[25]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[26]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[27]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[28]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[29]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[30]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[31]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[32]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[33]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[34]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[35]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[36]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[37]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[38]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[39]));\n" +
			"maxValue = max(maxValue, texture2D(sTexture, vTextureCoord + uTexOffset[40]));\n" +
			"\n" +
			"gl_FragColor = vec4(maxValue.rgb, 1.0);\n" +
		"}\n";

	private static String getFragmentShader(final int radius) {
		switch (radius) {
		case 0:
		case 1:
			return FRAGMENT_SHADER_1;
		case 2:
			return FRAGMENT_SHADER_2;
		case 3:
			return FRAGMENT_SHADER_3;
		default:
			return FRAGMENT_SHADER_4;
		}
	}

	public MediaEffectDilation() {
		this(1);
	}

	/**
	 * 膨張範囲を指定して生成
	 * @param radius 1, 2, 3, 4
	 */
	public MediaEffectDilation(final int radius) {
		super(new MediaEffectDilationDrawer(getFragmentShader(radius)));
		setTexSize(256, 256);
	}

	@Override
	public MediaEffectDilation resize(final int width, final int height) {
		super.resize(width, height);
		setTexSize(width, height);
		return this;
	}

	/**
	 * Sets the size of the texture.  This is used to find adjacent texels when filtering.
	 */
	public void setTexSize(final int width, final int height) {
		mDrawer.setTexSize(width, height);
	}
}
