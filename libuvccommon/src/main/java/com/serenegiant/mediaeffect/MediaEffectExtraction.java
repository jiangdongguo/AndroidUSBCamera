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

import android.util.Log;

import static com.serenegiant.glutils.ShaderConst.*;

/** 色抽出フィルタ */
public class MediaEffectExtraction extends MediaEffectGLESBase {
	private static final boolean DEBUG = false;
	private static final String TAG = "MediaEffectExtraction";

	private static final String FRAGMENT_SHADER_BASE = SHADER_VERSION +
		"%s" +
		"#define KERNEL_SIZE3x3 " + KERNEL_SIZE3x3 + "\n" +
		"precision highp float;\n" +
		"varying       vec2 vTextureCoord;\n" +
		"uniform %s    sTexture;\n" +
		"uniform float uKernel[18];\n" +
		"uniform vec2  uTexOffset[KERNEL_SIZE3x3];\n" +
		"uniform float uColorAdjust;\n" +
		FUNC_RGB2HSV +
		FUNC_HSV2RGB +
//		"vec3 rgb2hsv(vec3 c) {\n" +
//		"    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);\n" +
//		"    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));\n" +
//		"    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));\n" +
//		"    float d = q.x - min(q.w, q.y);\n" +
//		"    float e = 1.0e-10;\n" +
//		"    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);\n" +
//		"}\n" +
//		"vec3 hsv2rgb(vec3 c) {\n" +
//		"    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);\n" +
//		"    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);\n" +
//		"    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);\n" +
//		"}\n" +
		"void main() {\n" +
		"    vec3 hsv = rgb2hsv(texture2D(sTexture, vTextureCoord).rgb);\n" +	// RGBをHSVに変換
		"    vec3 min = vec3(uKernel[0], uKernel[2], uKernel[4]);\n" +	// HSV下限
		"    vec3 max = vec3(uKernel[1], uKernel[3], uKernel[5]);\n" +	// HSV上限
		"    vec3 add = vec3(uKernel[6], uKernel[7], uKernel[8]);\n" +	// HSV加算
		"    float e = 1e-10;\n" +
		"    vec3 eps = vec3(e, e, e);\n" +
		"    vec3 v = hsv;\n" +
		"    if (hsv.r < min.r || hsv.r > max.r || hsv.g < min.g || hsv.g > max.g || hsv.b < min.b || hsv.b > max.b) {\n" +
		"        v = vec3(0.0);\n" +
		"    }\n" +
		"    hsv = v + add;\n" +
		"    if (uColorAdjust > 0.0) {\n" +
		"        hsv = step(vec3(1.0, 1.0, uColorAdjust), hsv);\n" +		// 2値化
//		"        hsv = hsv * step(vec3(0.0, 0.0, uColorAdjust), hsv);\n" +	// 2値化
		"    }\n" +
		"    gl_FragColor = vec4(hsv2rgb(clamp(hsv, 0.0, 1.0)), 1.0);\n" +
		"}\n";
	private static final String FRAGMENT_SHADER
		= String.format(FRAGMENT_SHADER_BASE, HEADER_2D, SAMPLER_2D);
	private static final String FRAGMENT_SHADER_EXT
		= String.format(FRAGMENT_SHADER_BASE, HEADER_OES, SAMPLER_OES);

	private final float[] mLimit = new float[KERNEL_SIZE3x3];

	public MediaEffectExtraction() {
		super(new MediaEffectKernel3x3Drawer(FRAGMENT_SHADER));
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		mLimit[0] = 0.0f;	mLimit[1] = 1.0f;	// H上下限
		mLimit[2] = 0.0f;	mLimit[3] = 1.0f;	// S上下限
		mLimit[4] = 0.0f; 	mLimit[5] = 1.0f;	// V上下限
		mLimit[6] = 0.0f; 	mLimit[7] = 0.0f;	mLimit[8] = 0.0f;	// 抽出後加算値HSV
		((MediaEffectKernel3x3Drawer)mDrawer).setKernel(mLimit, 0.0f);	// デフォルトは2値化しないのでcolorAdjは0
	}

	/**
	 * 色抽出の上下限をHSVで設定
	 * @param lowerH [0.0, 1.0]
	 * @param upperH [0.0, 1.0]
	 * @param lowerS [0.0, 1.0]
	 * @param upperS [0.0, 1.0]
	 * @param lowerV [0.0, 1.0]
	 * @param upperV [0.0, 1.0]
	 * @param color_adjust 0より大きければ2値化時のしきい値, 0以下なら2値化なし
	 * @return
	 */
	public MediaEffectExtraction setParameter(
		final float lowerH, final float upperH,
		final float lowerS, final float upperS,
		final float lowerV, final float upperV,
		final float color_adjust) {
		return setParameter(lowerH, upperH, lowerS, upperS, lowerV, upperV, 0.0f, 0.0f, 0.0f, color_adjust);
	}

	public MediaEffectExtraction setParameter(
		final float lowerH, final float upperH,
		final float lowerS, final float upperS,
		final float lowerV, final float upperV,
		final float addH, final float addS, final float addV,
		final float color_adjust) {

		mLimit[0] = Math.min(lowerH, upperH);
		mLimit[1] = Math.max(lowerH, upperH);
		mLimit[2] = Math.min(lowerS, upperS);
		mLimit[3] = Math.max(lowerS, upperS);
		mLimit[4] = Math.min(lowerV, upperV);
		mLimit[5] = Math.max(lowerV, upperV);
		mLimit[6] = addH;
		mLimit[7] = addS;
		mLimit[8] = addV;
		((MediaEffectKernel3x3Drawer)mDrawer).setKernel(mLimit, color_adjust);
		return this;
	}

	/**
	 * 色抽出の上下限をHSVで設定, 0,1がHの下限上限, 2,3がSの下限上限, 4,5がVの下限上限
	 * @param limit
	 * @param color_adjust 0より大きければ2値化時のしきい値, 0以下なら2値化なし
	 * @return
	 */
	public MediaEffectExtraction setParameter(final float[] limit, final float color_adjust) {
		if ((limit == null) || (limit.length < 6)) {
			throw new IllegalArgumentException("limit is null or short");
		}
		System.arraycopy(limit, 0, mLimit, 0, 6);
		((MediaEffectKernel3x3Drawer)mDrawer).setKernel(mLimit, color_adjust);
		return this;
	}
}
