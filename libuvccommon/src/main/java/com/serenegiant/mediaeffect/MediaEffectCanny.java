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

/** Cannyエッジ検出フィルタ */
public class MediaEffectCanny extends MediaEffectGLESBase {
	private static final boolean DEBUG = false;
	private static final String TAG = "MediaEffectCanny";

	private static final String FRAGMENT_SHADER_BASE = SHADER_VERSION +
		"%s" +
		"#define KERNEL_SIZE3x3 " + KERNEL_SIZE3x3 + "\n" +
		"precision highp float;\n" +
		"varying       vec2 vTextureCoord;\n" +
		"uniform %s    sTexture;\n" +
		"uniform float uKernel[18];\n" +
		"uniform vec2  uTexOffset[KERNEL_SIZE3x3];\n" +
		"uniform float uColorAdjust;\n" +
		"const float lowerThreshold = 0.4;\n" +	// lowerとupperの値を入れ替えると白黒反転する
		"const float upperThreshold = 0.8;\n" +
		"void main() {\n" +
		"    vec4 magdir = texture2D(sTexture, vTextureCoord);\n" +
		"    vec2 offset = ((magdir.gb * 2.0) - 1.0) * uTexOffset[8];\n" +
		"    float first = texture2D(sTexture, vTextureCoord + offset).r;\n" +
		"    float second = texture2D(sTexture, vTextureCoord - offset).r;\n" +
		"    float multiplier = step(first, magdir.r);\n" +
		"    multiplier = multiplier * step(second, magdir.r);\n" +
		"    float threshold = smoothstep(lowerThreshold, upperThreshold, magdir.r);\n" +
		"    multiplier = multiplier * threshold;\n" +
		"    gl_FragColor = vec4(multiplier, multiplier, multiplier, 1.0);\n" +
		"}\n";
//		"void main() {\n" +
//		"    vec4 magdir = texture2D(sTexture, vTextureCoord);\n" +
//		"    float a = 0.5 / sin(3.14159 / 8.0); \n" +	// eight directions on grid
//		"    vec2 alpha = vec2(a);\n" +
//		"    vec2 offset = floor(alpha.xx * magdir.xy / magdir.zz);\n" +
//		"    vec4 fwdneighbour, backneighbour;\n" +
//		"    fwdneighbour = texture2D(sTexture, vTextureCoord + offset);\n" +
//		"    backneighbour = texture2D(sTexture, vTextureCoord + offset);\n" +
//		"    vec4 colorO;\n" +
//		"    if (fwdneighbour.z > magdir.z || backneighbour.z > magdir.z)\n" +
//		"        colorO = vec4(0.0, 0.0, 0.0, 0.0);\n" +	// not an edgel
//		"    else\n" +
//		"        colorO = vec4(1.0, 1.0, 1.0, 1.0);\n" +	// is an edgel
//		"    if (magdir.z < uColorAdjust)\n" +
//		"        colorO  = vec4(0.0, 0.0, 0.0, 0.0);\n" +	// thresholding
//		"    gl_FragColor = colorO;\n" +
//		"}\n";
//----
//		"const float threshold = 0.2;\n" +
//		"const vec2 unshift = vec2(1.0 / 256.0, 1.0);\n" +
//		"const float atan0   = 0.414213;\n" +
//		"const float atan45  = 2.414213;\n" +
//		"const float atan90  = -2.414213;\n" +
//		"const float atan135 = -0.414213;\n" +
//		"vec2 atanForCanny(float x) {\n" +
//		"    if (x < atan0 && x > atan135) {\n" +
//		"        return vec2(1.0, 0.0);\n" +
//		"    }\n" +
//		"    if (x < atan90 && x > atan45) {\n" +
//		"        return vec2(0.0, 1.0);\n" +
//		"    }\n" +
//		"    if (x > atan135 && x < atan90) {\n" +
//		"        return vec2(-1.0, 1.0);\n" +
//		"    }\n" +
//		"    return vec2(1.0, 1.0);\n" +
//		"}\n" +
//		"vec4 cannyEdge(vec2 coords) {\n" +
//		"    vec4 color = texture2D(sTexture, coords);\n" +
//		"    color.z = dot(color.zw, unshift);\n" +
//		"    if (color.z > threshold) {\n" +
//		"        color.x -= 0.5;\n" +
//		"        color.y -= 0.5;\n" +
//		"        vec2 offset = atanForCanny(color.y / color.x);\n" +
//		"        offset.x *= uTexOffset[7];\n" +
//		"        offset.y *= uTexOffset[8];\n" +
//		"        vec4 forward  = texture2D(sTexture, coords + offset);\n" +
//		"        vec4 backward = texture2D(sTexture, coords - offset);\n" +
//		"        forward.z  = dot(forward.zw, unshift);\n" +
//		"        backward.z = dot(backward.zw, unshift);\n" +
//		"        if (forward.z >= color.z ||\n" +
//		"            backward.z >= color.z) {\n" +
//		"            return vec4(0.0, 0.0, 0.0, 1.0);\n" +
//		"        } else {\n" +
//		"            color.x += 0.5; color.y += 0.5;\n" +
//		"            return vec4(1.0, color.x, color.y, 1.0);\n" +
//		"        }\n" +
//		"    }\n" +
//		"    return vec4(0.0, 0.0, 0.0, 1.0);\n" +
//		"}\n" +
//		"void main() {\n" +
//		"    gl_FragColor = cannyEdge(vTextureCoord);\n" +
//		"}\n";
	private static final String FRAGMENT_SHADER
		= String.format(FRAGMENT_SHADER_BASE, HEADER_2D, SAMPLER_2D);
	private static final String FRAGMENT_SHADER_EXT
		= String.format(FRAGMENT_SHADER_BASE, HEADER_OES, SAMPLER_OES);

	public MediaEffectCanny() {
		super(new MediaEffectKernel3x3Drawer(false, FRAGMENT_SHADER));
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
	}

	public MediaEffectCanny(final float threshold) {
		this();
		setParameter(threshold);
	}

	public MediaEffectCanny setParameter(final float threshold) {
		((MediaEffectKernel3x3Drawer)mDrawer).setColorAdjust(threshold);
		return this;
	}
}
