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

/** 彩度調整([-1.0f,+1.0f]), 0だと無調整 */
public class MediaEffectSaturateGLES extends MediaEffectGLESBase {
	private static final boolean DEBUG = false;
	private static final String TAG = "MediaEffectBrightness";

	private static final String FRAGMENT_SHADER_BASE = SHADER_VERSION +
		"%s" +
		"precision highp float;\n" +
		"varying       vec2 vTextureCoord;\n" +
		"uniform %s    sTexture;\n" +
		"uniform float uColorAdjust;\n" +
		FUNC_GET_INTENSITY +
		"void main() {\n" +
		"    highp vec4 tex = texture2D(sTexture, vTextureCoord);\n" +
		"    highp float intensity = getIntensity(tex.rgb);\n" +
		"    highp vec3 greyScaleColor = vec3(intensity, intensity, intensity);\n" +
		"    gl_FragColor = vec4(mix(greyScaleColor, tex.rgb, uColorAdjust), tex.w);\n" +
		"}\n";
	private static final String FRAGMENT_SHADER
		= String.format(FRAGMENT_SHADER_BASE, HEADER_2D, SAMPLER_2D);
	private static final String FRAGMENT_SHADER_EXT
		= String.format(FRAGMENT_SHADER_BASE, HEADER_OES, SAMPLER_OES);

	public MediaEffectSaturateGLES() {
		this(0.0f);
	}

	public MediaEffectSaturateGLES(final float saturation) {
		super(new MediaEffectColorAdjustDrawer(FRAGMENT_SHADER));
		setParameter(saturation);
	}

	/**
	 * 彩度調整
	 * @param saturation [-1.0f,+1.0f], 0なら無調整)
	 * @return
	 */
	public MediaEffectSaturateGLES setParameter(final float saturation) {
		((MediaEffectColorAdjustDrawer)mDrawer).setColorAdjust(saturation + 1.0f);
		return this;
	}
}
