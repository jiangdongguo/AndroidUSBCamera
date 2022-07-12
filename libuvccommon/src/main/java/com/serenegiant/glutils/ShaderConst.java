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

import android.opengl.GLES20;

/**
 * Created by saki on 16/08/26.
 * フラグメントシェーダーとかの文字列定数達を集める
 */
public class ShaderConst {
	public static final int GL_TEXTURE_EXTERNAL_OES	= 0x8D65;
	public static final int GL_TEXTURE_2D           = 0x0DE1;

	public static final String SHADER_VERSION = "#version 100\n";

	public static final String HEADER_2D = "";
	public static final String SAMPLER_2D = "sampler2D";

	public static final String HEADER_OES = "#extension GL_OES_EGL_image_external : require\n";
	public static final String SAMPLER_OES = "samplerExternalOES";

	public static final int KERNEL_SIZE3x3 = 9;
	public static final int KERNEL_SIZE5x５ = 25;

	public static final int NO_TEXTURE = -1;

	public static final int[] TEX_NUMBERS = {
		GLES20.GL_TEXTURE0, GLES20.GL_TEXTURE1,
		GLES20.GL_TEXTURE2, GLES20.GL_TEXTURE3,
		GLES20.GL_TEXTURE4, GLES20.GL_TEXTURE5,
		GLES20.GL_TEXTURE6, GLES20.GL_TEXTURE7,
		GLES20.GL_TEXTURE8, GLES20.GL_TEXTURE9,
		GLES20.GL_TEXTURE10, GLES20.GL_TEXTURE11,
		GLES20.GL_TEXTURE12, GLES20.GL_TEXTURE13,
		GLES20.GL_TEXTURE14, GLES20.GL_TEXTURE15,
		GLES20.GL_TEXTURE16, GLES20.GL_TEXTURE17,
		GLES20.GL_TEXTURE18, GLES20.GL_TEXTURE19,
		GLES20.GL_TEXTURE20, GLES20.GL_TEXTURE21,
		GLES20.GL_TEXTURE22, GLES20.GL_TEXTURE23,
		GLES20.GL_TEXTURE24, GLES20.GL_TEXTURE25,
		GLES20.GL_TEXTURE26, GLES20.GL_TEXTURE27,
		GLES20.GL_TEXTURE28, GLES20.GL_TEXTURE29,
		GLES20.GL_TEXTURE30, GLES20.GL_TEXTURE31,
	};

// 関数文字列定義
	/**
	 * RGBをHSVに変換
	 * {R[0.0-1.0], G[0.0-1.0], B([0.0-1.0]} => {H[0.0-1.0], S[0.0-1.0], V[0.0-1.0]}
	 */
	public static final String FUNC_RGB2HSV
		= "vec3 rgb2hsv(vec3 c) {\n" +
			"vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);\n" +
			"vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));\n" +
			"vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));\n" +
			"float d = q.x - min(q.w, q.y);\n" +
			"float e = 1.0e-10;\n" +
			"return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);\n" +
		"}\n";
	/**
	 * HSVをRGBに変換
	 * {H[0.0-1.0], S[0.0-1.0], V[0.0-1.0]} => {R[0.0-1.0], G[0.0-1.0], B([0.0-1.0]}
	 */
	public static final String FUNC_HSV2RGB
		= "vec3 hsv2rgb(vec3 c) {\n" +
			"vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);\n" +
			"vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);\n" +
			"return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);\n" +
		"}\n";

	/**
	 * RGBの輝度を取得
	 * 変換係数との内積を計算するだけ
	 * 係数は(0.2125, 0.7154, 0.0721)
	 */
	public static final String FUNC_GET_INTENSITY
		= "const highp vec3 luminanceWeighting = vec3(0.2125, 0.7154, 0.0721);\n" +
		"highp float getIntensity(vec3 c) {\n" +
			"return dot(c.rgb, luminanceWeighting);\n" +
		"}\n";

// 頂点シェーダー
	/**
	 * モデルビュー変換行列とテクスチャ変換行列適用するだけの頂点シェーダー
	 */
	public static final String VERTEX_SHADER = SHADER_VERSION +
		"uniform mat4 uMVPMatrix;\n" +				// モデルビュー変換行列
		"uniform mat4 uTexMatrix;\n" +				// テクスチャ変換行列
		"attribute highp vec4 aPosition;\n" +		// 頂点座標
		"attribute highp vec4 aTextureCoord;\n" +	// テクスチャ情報
		"varying highp vec2 vTextureCoord;\n" +		// フラグメントシェーダーへ引き渡すテクスチャ座標
		"void main() {\n" +
		"    gl_Position = uMVPMatrix * aPosition;\n" +
		"    vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n" +
		"}\n";

// フラグメントシェーダー
	public static final String FRAGMENT_SHADER_SIMPLE_OES
		= SHADER_VERSION
		+ HEADER_OES
		+ "precision mediump float;\n"
		+ "uniform samplerExternalOES sTexture;\n"
		+ "varying highp vec2 vTextureCoord;\n"
		+ "void main() {\n"
		+ "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n"
		+ "}";

	public static final String FRAGMENT_SHADER_SIMPLE
		= SHADER_VERSION
		+ HEADER_2D
		+ "precision mediump float;\n"
		+ "uniform sampler2D sTexture;\n"
		+ "varying highp vec2 vTextureCoord;\n"
		+ "void main() {\n"
		+ "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n"
		+ "}";

//
	// Simple fragment shader for use with "normal" 2D textures.
	private static final String FRAGMENT_SHADER_BASE = SHADER_VERSION +
		"%s" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform %s sTexture;\n" +
		"void main() {\n" +
		"    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
		"}\n";
	public static final String FRAGMENT_SHADER_2D
		= String.format(FRAGMENT_SHADER_BASE, HEADER_2D, SAMPLER_2D);
	public static final String FRAGMENT_SHADER_EXT
		= String.format(FRAGMENT_SHADER_BASE, HEADER_OES, SAMPLER_OES);

	// Fragment shader that converts color to black & white with a simple transformation.
	private static final String FRAGMENT_SHADER_BW_BASE = SHADER_VERSION +
		"%s" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform %s sTexture;\n" +
		"void main() {\n" +
		"    vec4 tc = texture2D(sTexture, vTextureCoord);\n" +
		"    float color = tc.r * 0.3 + tc.g * 0.59 + tc.b * 0.11;\n" +
		"    gl_FragColor = vec4(color, color, color, 1.0);\n" +
		"}\n";
	public static final String FRAGMENT_SHADER_BW
		= String.format(FRAGMENT_SHADER_BW_BASE, HEADER_2D, SAMPLER_2D);
	public static final String FRAGMENT_SHADER_EXT_BW
		= String.format(FRAGMENT_SHADER_BW_BASE, HEADER_OES, SAMPLER_OES);

	// Fragment shader that attempts to produce a high contrast image
	private static final String FRAGMENT_SHADER_NIGHT_BASE = SHADER_VERSION +
		"%s" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform %s sTexture;\n" +
		"void main() {\n" +
		"    vec4 tc = texture2D(sTexture, vTextureCoord);\n" +
		"    float color = ((tc.r * 0.3 + tc.g * 0.59 + tc.b * 0.11) - 0.5 * 1.5) + 0.8;\n" +
		"    gl_FragColor = vec4(color, color + 0.15, color, 1.0);\n" +
		"}\n";
	public static final String FRAGMENT_SHADER_NIGHT
		= String.format(FRAGMENT_SHADER_NIGHT_BASE, HEADER_2D, SAMPLER_2D);
	public static final String FRAGMENT_SHADER_EXT_NIGHT
		= String.format(FRAGMENT_SHADER_NIGHT_BASE, HEADER_OES, SAMPLER_OES);

	// Fragment shader that applies a Chroma Key effect, making green pixels transparent
	private static final String FRAGMENT_SHADER_CHROMA_KEY_BASE = SHADER_VERSION +
		"%s" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform %s sTexture;\n" +
		"void main() {\n" +
		"    vec4 tc = texture2D(sTexture, vTextureCoord);\n" +
		"    float color = ((tc.r * 0.3 + tc.g * 0.59 + tc.b * 0.11) - 0.5 * 1.5) + 0.8;\n" +
		"    if(tc.g > 0.6 && tc.b < 0.6 && tc.r < 0.6){ \n" +
		"        gl_FragColor = vec4(0, 0, 0, 0.0);\n" +
		"    }else{ \n" +
		"        gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
		"    }\n" +
		"}\n";
	public static final String FRAGMENT_SHADER_CHROMA_KEY
		= String.format(FRAGMENT_SHADER_CHROMA_KEY_BASE, HEADER_2D, SAMPLER_2D);
	public static final String FRAGMENT_SHADER_EXT_CHROMA_KEY
		= String.format(FRAGMENT_SHADER_CHROMA_KEY_BASE, HEADER_OES, SAMPLER_OES);

	private static final String FRAGMENT_SHADER_SQUEEZE_BASE = SHADER_VERSION +
		"%s" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform %s sTexture;\n" +
		"uniform vec2 uPosition;\n" +
		"void main() {\n" +
		"    vec2 texCoord = vTextureCoord.xy;\n" +
		"    vec2 normCoord = 2.0 * texCoord - 1.0;\n"+
		"    float r = length(normCoord); // to polar coords \n" +
		"    float phi = atan(normCoord.y + uPosition.y, normCoord.x + uPosition.x); // to polar coords \n"+
		"    r = pow(r, 1.0/1.8) * 0.8;\n"+  // Squeeze it
		"    normCoord.x = r * cos(phi); \n" +
		"    normCoord.y = r * sin(phi); \n" +
		"    texCoord = normCoord / 2.0 + 0.5;\n"+
		"    gl_FragColor = texture2D(sTexture, texCoord);\n"+
		"}\n";
	public static final String FRAGMENT_SHADER_SQUEEZE
		= String.format(FRAGMENT_SHADER_SQUEEZE_BASE, HEADER_2D, SAMPLER_2D);
	public static final String FRAGMENT_SHADER_EXT_SQUEEZE
		= String.format(FRAGMENT_SHADER_SQUEEZE_BASE, HEADER_OES, SAMPLER_OES);

	public static final String FRAGMENT_SHADER_EXT_TWIRL =
		"#version 100\n" +
		"#extension GL_OES_EGL_image_external : require\n" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform samplerExternalOES sTexture;\n" +
		"uniform vec2 uPosition;\n" +
		"void main() {\n" +
		"    vec2 texCoord = vTextureCoord.xy;\n" +
		"    vec2 normCoord = 2.0 * texCoord - 1.0;\n"+
		"    float r = length(normCoord); // to polar coords \n" +
		"    float phi = atan(normCoord.y + uPosition.y, normCoord.x + uPosition.x); // to polar coords \n"+
		"    phi = phi + (1.0 - smoothstep(-0.5, 0.5, r)) * 4.0;\n"+ // Twirl it
		"    normCoord.x = r * cos(phi); \n" +
		"    normCoord.y = r * sin(phi); \n" +
		"    texCoord = normCoord / 2.0 + 0.5;\n"+
		"    gl_FragColor = texture2D(sTexture, texCoord);\n"+
		"}\n";

	public static final String FRAGMENT_SHADER_EXT_TUNNEL = SHADER_VERSION +
		"#extension GL_OES_EGL_image_external : require\n" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform samplerExternalOES sTexture;\n" +
		"uniform vec2 uPosition;\n" +
		"void main() {\n" +
		"    vec2 texCoord = vTextureCoord.xy;\n" +
		"    vec2 normCoord = 2.0 * texCoord - 1.0;\n"+
		"    float r = length(normCoord); // to polar coords \n" +
		"    float phi = atan(normCoord.y + uPosition.y, normCoord.x + uPosition.x); // to polar coords \n"+
		"    if (r > 0.5) r = 0.5;\n"+ // Tunnel
		"    normCoord.x = r * cos(phi); \n" +
		"    normCoord.y = r * sin(phi); \n" +
		"    texCoord = normCoord / 2.0 + 0.5;\n"+
		"    gl_FragColor = texture2D(sTexture, texCoord);\n"+
		"}\n";

	public static final String FRAGMENT_SHADER_EXT_BULGE = SHADER_VERSION +
		"#extension GL_OES_EGL_image_external : require\n" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform samplerExternalOES sTexture;\n" +
		"uniform vec2 uPosition;\n" +
		"void main() {\n" +
		"    vec2 texCoord = vTextureCoord.xy;\n" +
		"    vec2 normCoord = 2.0 * texCoord - 1.0;\n"+
		"    float r = length(normCoord); // to polar coords \n" +
		"    float phi = atan(normCoord.y + uPosition.y, normCoord.x + uPosition.x); // to polar coords \n"+
		"    r = r * smoothstep(-0.1, 0.5, r);\n"+ // Bulge
		"    normCoord.x = r * cos(phi); \n" +
		"    normCoord.y = r * sin(phi); \n" +
		"    texCoord = normCoord / 2.0 + 0.5;\n"+
		"    gl_FragColor = texture2D(sTexture, texCoord);\n"+
		"}\n";

	public static final String FRAGMENT_SHADER_EXT_DENT = SHADER_VERSION +
		"#extension GL_OES_EGL_image_external : require\n" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform samplerExternalOES sTexture;\n" +
		"uniform vec2 uPosition;\n" +
		"void main() {\n" +
		"    vec2 texCoord = vTextureCoord.xy;\n" +
		"    vec2 normCoord = 2.0 * texCoord - 1.0;\n"+
		"    float r = length(normCoord); // to polar coords \n" +
		"    float phi = atan(normCoord.y + uPosition.y, normCoord.x + uPosition.x); // to polar coords \n"+
		"    r = 2.0 * r - r * smoothstep(0.0, 0.7, r);\n"+ // Dent
		"    normCoord.x = r * cos(phi); \n" +
		"    normCoord.y = r * sin(phi); \n" +
		"    texCoord = normCoord / 2.0 + 0.5;\n"+
		"    gl_FragColor = texture2D(sTexture, texCoord);\n"+
		"}\n";

	public static final String FRAGMENT_SHADER_EXT_FISHEYE = SHADER_VERSION +
		"#extension GL_OES_EGL_image_external : require\n" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform samplerExternalOES sTexture;\n" +
		"uniform vec2 uPosition;\n" +
		"void main() {\n" +
		"    vec2 texCoord = vTextureCoord.xy;\n" +
		"    vec2 normCoord = 2.0 * texCoord - 1.0;\n"+
		"    float r = length(normCoord); // to polar coords \n" +
		"    float phi = atan(normCoord.y + uPosition.y, normCoord.x + uPosition.x); // to polar coords \n"+
		"    r = r * r / sqrt(2.0);\n"+ // Fisheye
		"    normCoord.x = r * cos(phi); \n" +
		"    normCoord.y = r * sin(phi); \n" +
		"    texCoord = normCoord / 2.0 + 0.5;\n"+
		"    gl_FragColor = texture2D(sTexture, texCoord);\n"+
		"}\n";

	public static final String FRAGMENT_SHADER_EXT_STRETCH = SHADER_VERSION +
		"#extension GL_OES_EGL_image_external : require\n" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform samplerExternalOES sTexture;\n" +
		"uniform vec2 uPosition;\n" +
		"void main() {\n" +
		"    vec2 texCoord = vTextureCoord.xy;\n" +
		"    vec2 normCoord = 2.0 * texCoord - 1.0;\n"+
		"    vec2 s = sign(normCoord + uPosition);\n"+
		"    normCoord = abs(normCoord);\n"+
		"    normCoord = 0.5 * normCoord + 0.5 * smoothstep(0.25, 0.5, normCoord) * normCoord;\n"+
		"    normCoord = s * normCoord;\n"+
		"    texCoord = normCoord / 2.0 + 0.5;\n"+
		"    gl_FragColor = texture2D(sTexture, texCoord);\n"+
		"}\n";

	public static final String FRAGMENT_SHADER_EXT_MIRROR = SHADER_VERSION +
		"#extension GL_OES_EGL_image_external : require\n" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform samplerExternalOES sTexture;\n" +
		"uniform vec2 uPosition;\n" +
		"void main() {\n" +
		"    vec2 texCoord = vTextureCoord.xy;\n" +
		"    vec2 normCoord = 2.0 * texCoord - 1.0;\n"+
		"    normCoord.x = normCoord.x * sign(normCoord.x + uPosition.x);\n"+
		"    texCoord = normCoord / 2.0 + 0.5;\n"+
		"    gl_FragColor = texture2D(sTexture, texCoord);\n"+
		"}\n";

	public static final String FRAGMENT_SHADER_SOBEL_BASE = SHADER_VERSION +
		"%s" +
		"#define KERNEL_SIZE3x3 " + KERNEL_SIZE3x3 + "\n" +
		"precision highp float;\n" +
		"varying       vec2 vTextureCoord;\n" +
		"uniform %s    sTexture;\n" +
		"uniform float uKernel[18];\n" +
		"uniform vec2  uTexOffset[KERNEL_SIZE3x3];\n" +
		"uniform float uColorAdjust;\n" +
		"void main() {\n" +
		"    vec3 t0 = texture2D(sTexture, vTextureCoord + uTexOffset[0]).rgb;\n" +
		"    vec3 t1 = texture2D(sTexture, vTextureCoord + uTexOffset[1]).rgb;\n" +
		"    vec3 t2 = texture2D(sTexture, vTextureCoord + uTexOffset[2]).rgb;\n" +
		"    vec3 t3 = texture2D(sTexture, vTextureCoord + uTexOffset[3]).rgb;\n" +
		"    vec3 t4 = texture2D(sTexture, vTextureCoord + uTexOffset[4]).rgb;\n" +
		"    vec3 t5 = texture2D(sTexture, vTextureCoord + uTexOffset[5]).rgb;\n" +
		"    vec3 t6 = texture2D(sTexture, vTextureCoord + uTexOffset[6]).rgb;\n" +
		"    vec3 t7 = texture2D(sTexture, vTextureCoord + uTexOffset[7]).rgb;\n" +
		"    vec3 t8 = texture2D(sTexture, vTextureCoord + uTexOffset[8]).rgb;\n" +
		"    vec3 sumH = t0 * uKernel[0] + t1 * uKernel[1] + t2 * uKernel[2]\n" +
		"              + t3 * uKernel[3] + t4 * uKernel[4] + t5 * uKernel[5]\n" +
		"              + t6 * uKernel[6] + t7 * uKernel[7] + t8 * uKernel[8];\n" +
//		"    vec3 sumV = t0 * uKernel[ 9] + t1 * uKernel[10] + t2 * uKernel[11]\n" +
//		"              + t3 * uKernel[12] + t4 * uKernel[13] + t5 * uKernel[14]\n" +
//		"              + t6 * uKernel[15] + t7 * uKernel[16] + t8 * uKernel[17];\n" +
//		"    float mag = length(abs(sumH) + abs(sumV));\n" +
		"    float mag = length(sumH);\n" +
		"    gl_FragColor = vec4(vec3(mag), 1.0);\n" +
		"}\n";

	public static final String FRAGMENT_SHADER_SOBEL
		= String.format(FRAGMENT_SHADER_SOBEL_BASE, HEADER_2D, SAMPLER_2D);
	public static final String FRAGMENT_SHADER_EXT_SOBEL
		= String.format(FRAGMENT_SHADER_SOBEL_BASE, HEADER_OES, SAMPLER_OES);

	public static final float[] KERNEL_NULL = { 0f, 0f, 0f,  0f, 1f, 0f,  0f, 0f, 0f};
	public static final float[] KERNEL_SOBEL_H = { 1f, 0f, -1f, 2f, 0f, -2f, 1f, 0f, -1f, };	// ソーベル(1次微分)
	public static final float[] KERNEL_SOBEL_V = { 1f, 2f, 1f, 0f, 0f, 0f, -1f, -2f, -1f, };
	public static final float[] KERNEL_SOBEL2_H = { 3f, 0f, -3f, 10f, 0f, -10f, 3f, 0f, -3f, };
	public static final float[] KERNEL_SOBEL2_V = { 3f, 10f, 3f, 0f, 0f, 0f, -3f, -10f, -3f, };
	public static final float[] KERNEL_SHARPNESS = { 0f, -1f, 0f, -1f, 5f, -1f, 0f, -1f, 0f,};	// シャープネス
	public static final float[] KERNEL_EDGE_DETECT = { -1f, -1f, -1f, -1f, 8f, -1f, -1f, -1f, -1f, }; // エッジ検出
	public static final float[] KERNEL_EMBOSS = { 2f, 0f, 0f, 0f, -1f, 0f, 0f, 0f, -1f };	// エンボス, オフセット0.5f
	public static final float[] KERNEL_SMOOTH = { 1/9f, 1/9f, 1/9f, 1/9f, 1/9f, 1/9f, 1/9f, 1/9f, 1/9f, };	// 移動平均
	public static final float[] KERNEL_GAUSSIAN = { 1/16f, 2/16f, 1/16f, 2/16f, 4/16f, 2/16f, 1/16f, 2/16f, 1/16f, };	// ガウシアン(ノイズ除去/)
	public static final float[] KERNEL_BRIGHTEN = { 1f, 1f, 1f, 1f, 2f, 1f, 1f, 1f, 1f, };
	public static final float[] KERNEL_LAPLACIAN = { 1f, 1f, 1f, 1f, -8f, 1f, 1f, 1f, 1f, };	// ラプラシアン(2次微分)

	private static final String FRAGMENT_SHADER_FILT3x3_BASE = SHADER_VERSION +
		"%s" +
		"#define KERNEL_SIZE3x3 " + KERNEL_SIZE3x3 + "\n" +
		"precision highp float;\n" +
		"varying       vec2 vTextureCoord;\n" +
		"uniform %s    sTexture;\n" +
		"uniform float uKernel[18];\n" +
		"uniform vec2  uTexOffset[KERNEL_SIZE3x3];\n" +
		"uniform float uColorAdjust;\n" +
		"void main() {\n" +
		"    vec4 sum = vec4(0.0);\n" +
		"    sum += texture2D(sTexture, vTextureCoord + uTexOffset[0]) * uKernel[0];\n" +
		"    sum += texture2D(sTexture, vTextureCoord + uTexOffset[1]) * uKernel[1];\n" +
		"    sum += texture2D(sTexture, vTextureCoord + uTexOffset[2]) * uKernel[2];\n" +
		"    sum += texture2D(sTexture, vTextureCoord + uTexOffset[3]) * uKernel[3];\n" +
		"    sum += texture2D(sTexture, vTextureCoord + uTexOffset[4]) * uKernel[4];\n" +
		"    sum += texture2D(sTexture, vTextureCoord + uTexOffset[5]) * uKernel[5];\n" +
		"    sum += texture2D(sTexture, vTextureCoord + uTexOffset[6]) * uKernel[6];\n" +
		"    sum += texture2D(sTexture, vTextureCoord + uTexOffset[7]) * uKernel[7];\n" +
		"    sum += texture2D(sTexture, vTextureCoord + uTexOffset[8]) * uKernel[8];\n" +
		"    gl_FragColor = sum + uColorAdjust;\n" +
		"}\n";
	public static final String FRAGMENT_SHADER_FILT3x3
		= String.format(FRAGMENT_SHADER_FILT3x3_BASE, HEADER_2D, SAMPLER_2D);
	public static final String FRAGMENT_SHADER_EXT_FILT3x3
		= String.format(FRAGMENT_SHADER_FILT3x3_BASE, HEADER_OES, SAMPLER_OES);

}
