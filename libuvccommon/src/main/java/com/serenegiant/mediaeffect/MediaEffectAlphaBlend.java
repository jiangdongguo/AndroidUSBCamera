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

import static com.serenegiant.glutils.ShaderConst.*;

/**
 * 2枚のテクスチャをアルファブレンディング, mix[0.0, 1.0]
 * mix == 0.0ならテクスチャ1のみ
 * mix == 1.0ならテクスチャ2のみ
 * mix == 0.5なら半分ずつを合成
 */
public class MediaEffectAlphaBlend extends MediaEffectGLESBase {
	private static final boolean DEBUG = false;
	private static final String TAG = "MediaEffectAlphaBlend";

	private static final String FRAGMENT_SHADER_BASE = SHADER_VERSION +
		"%s" +
		"precision highp float;\n" +
		"varying       vec2 vTextureCoord;\n" +
		"uniform %s    sTexture;\n" +
		"uniform %s    sTexture2;\n" +
		"uniform float uMixRate;\n" +
		"void main() {\n" +
		"    highp vec4 tex1 = texture2D(sTexture, vTextureCoord);\n" +
		"    highp vec4 tex2 = texture2D(sTexture2, vTextureCoord);\n" +
		"    gl_FragColor = vec4(mix(tex1.rgb, tex2.rgb, tex2.a * uMixRate), tex1.a);\n" +
		"}\n";
	private static final String FRAGMENT_SHADER
		= String.format(FRAGMENT_SHADER_BASE, HEADER_2D, SAMPLER_2D, SAMPLER_2D);
	private static final String FRAGMENT_SHADER_EXT
		= String.format(FRAGMENT_SHADER_BASE, HEADER_OES, SAMPLER_OES, SAMPLER_OES);

	private static class MediaEffectAlphaBlendDrawer extends MediaEffectDrawer {
		private final int muMixRate;
		private float mMixRate;
		
		/**
		 * コンストラクタ
		 * @param initMixRate
		 */
		protected MediaEffectAlphaBlendDrawer(final float initMixRate) {
			this(initMixRate, false);
		}
		
		/**
		 * コンストラクタ
		 * @param initMixRate
		 * @param isOES
		 */
		protected MediaEffectAlphaBlendDrawer(final float initMixRate, final boolean isOES) {
			super(2, isOES, isOES ? FRAGMENT_SHADER_EXT : FRAGMENT_SHADER);
			int loc = GLES20.glGetUniformLocation(getProgram(), "uMixRate");
			if (loc < 0) {
				loc = -1;
			}
			muMixRate = loc;
			setMixRate(initMixRate);
		}

		/**
		 * アルファブレンディング比率セット
		 * @param mixRate
		 */
		public void setMixRate(final float mixRate) {
			synchronized (mSync) {
				mMixRate = mixRate;
			}
		}
		
		@Override
		protected void preDraw(@NonNull final int[] tex_ids,
			final float[] tex_matrix, final int offset) {

			super.preDraw(tex_ids, tex_matrix, offset);
			if (muMixRate >= 0) {
				synchronized (mSync) {
					GLES20.glUniform1f(muMixRate, mMixRate);
				}
			}
		}
	}
	
	/**
	 * コンストラクタ
	 * アルファブレンディング比率は0.5(半分ずつ)
	 */
	public MediaEffectAlphaBlend() {
		this(0.5f);
	}
	
	/**
	 * コンストラクタ
	 * @param mixRate アルファブレンディング比率セット
	 */
	public MediaEffectAlphaBlend(final float mixRate) {
		super(new MediaEffectAlphaBlendDrawer(mixRate));
		setParameter(mixRate);
	}

	/**
	 * アルファブレンディング比率セット
	 * @param mixRate [-1.0f,+1.0f]
	 * @return
	 */
	public MediaEffectAlphaBlend setParameter(final float mixRate) {
		setEnable(true);
		((MediaEffectAlphaBlendDrawer)mDrawer).setMixRate(mixRate);
		return this;
	}

}
