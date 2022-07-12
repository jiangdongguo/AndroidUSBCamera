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

import static com.serenegiant.glutils.ShaderConst.*;

/**
 * テクスチャAとテクスチャBとマスク用テクスチャMを用いて
 * C = A x (1-α) + B x α (α = M.a)
 * で合成するためのクラス
 */

public class MediaEffectMaskedAlphaBlend extends MediaEffectGLESBase {
	private static final boolean DEBUG = false;
	private static final String TAG = "MediaEffectMaskedAlphaBlend";

	private static final String FRAGMENT_SHADER_BASE = SHADER_VERSION +
		"%s" +
		"precision highp float;\n" +
		"varying       vec2 vTextureCoord;\n" +
		"uniform %s    sTexture;\n" +	// 入力テクスチャA
		"uniform %s    sTexture2;\n" +	// 入力テクスチャB
		"uniform %s    sTexture3;\n" +	// マスクM
		"void main() {\n" +
		"    highp vec4 tex1 = texture2D(sTexture, vTextureCoord);\n" +
		"    highp vec4 tex2 = texture2D(sTexture2, vTextureCoord);\n" +
		"    highp float alpha = texture2D(sTexture3, vTextureCoord).a;\n" +
		"    gl_FragColor = vec4(mix(tex1.rgb, tex2.rgb, tex2.a * alpha), tex1.a);\n" +
		"}\n";
	private static final String FRAGMENT_SHADER
		= String.format(FRAGMENT_SHADER_BASE, HEADER_2D,
			SAMPLER_2D, SAMPLER_2D, SAMPLER_2D);
	private static final String FRAGMENT_SHADER_EXT
		= String.format(FRAGMENT_SHADER_BASE, HEADER_OES,
			SAMPLER_OES, SAMPLER_OES, SAMPLER_OES);

	/**
	 * コンストラクタ
	 */
	public MediaEffectMaskedAlphaBlend() {
		this(false);
	}
	
	/**
	 * コンストラクタ
	 * @param isOES
	 */
	public MediaEffectMaskedAlphaBlend(final boolean isOES) {
		super(MediaEffectDrawer.newInstance(3, isOES,
			isOES ? FRAGMENT_SHADER_EXT : FRAGMENT_SHADER));
	}
}
