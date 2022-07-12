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

/**
 * カーネル行列を用いた映像フィルタ処理
 * MediaEffectSingleDrawerを継承しているので、使用できるテクスチャは1つだけ
 */
public class MediaEffectKernel3x3Drawer extends MediaEffectColorAdjustDrawer {

	public static final int KERNEL_SIZE = 9;
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

	private final int muKernelLoc;		// カーネル行列(float配列)
	private final int muTexOffsetLoc;	// テクスチャオフセット(カーネル行列用)
	private final float[] mKernel = new float[KERNEL_SIZE * 2];	// Inputs for convolution filter based shaders
	private final float[] mTexOffset = new float[KERNEL_SIZE * 2];
	private float mTexWidth;
	private float mTexHeight;

	private static final String FRAGMENT_SHADER_FILT3x3_BASE = SHADER_VERSION +
		"%s" +
		"#define KERNEL_SIZE3x3 " + KERNEL_SIZE + "\n" +
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
	private static final String FRAGMENT_SHADER_FILT3x3
		= String.format(FRAGMENT_SHADER_FILT3x3_BASE, HEADER_2D, SAMPLER_2D);
	private static final String FRAGMENT_SHADER_EXT_FILT3x3
		= String.format(FRAGMENT_SHADER_FILT3x3_BASE, HEADER_OES, SAMPLER_OES);

	public MediaEffectKernel3x3Drawer(final String fss) {
		this(false, VERTEX_SHADER, fss);
	}

	public MediaEffectKernel3x3Drawer(final boolean isOES, final String fss) {
		this(isOES, VERTEX_SHADER, fss);
	}

	public MediaEffectKernel3x3Drawer(final boolean isOES, final String vss, final String fss) {
		super(isOES, vss, fss);
		muKernelLoc = GLES20.glGetUniformLocation(getProgram(), "uKernel");
		if (muKernelLoc < 0) {
			// no kernel in this one
			muTexOffsetLoc = -1;
		} else {
			// has kernel, must also have tex offset and color adj
			muTexOffsetLoc = GLES20.glGetUniformLocation(getProgram(), "uTexOffset");
//			GLHelper.checkLocation(muTexOffsetLoc, "uTexOffset");	// 未使用だと削除されてしまうのでチェックしない

			setKernel(KERNEL_NULL, 0f);
			setTexSize(256, 256);

//			GLHelper.checkLocation(muColorAdjustLoc, "uColorAdjust");	// 未使用だと削除されてしまうのでチェックしない
		}
	}

	@Override
	protected void preDraw(@NonNull final int[] tex_ids,
		final float[] tex_matrix, final int offset) {

		super.preDraw(tex_ids, tex_matrix, offset);
		// カーネル関数(行列)
		if (muKernelLoc >= 0) {
			GLES20.glUniform1fv(muKernelLoc, KERNEL_SIZE, mKernel, 0);
			GLHelper.checkGlError("set kernel");
		}
		// テクセルオフセット
		if (muTexOffsetLoc >= 0) {
			GLES20.glUniform2fv(muTexOffsetLoc, KERNEL_SIZE, mTexOffset, 0);
		}
	}

	public void setKernel(final float[] values, final float colorAdj) {
		if ((values == null) || (values.length < KERNEL_SIZE)) {
			throw new IllegalArgumentException("Kernel size is "
				+ (values != null ? values.length : 0) + " vs. " + KERNEL_SIZE);
		}
		synchronized (mSync) {
			System.arraycopy(values, 0, mKernel, 0, KERNEL_SIZE);
			setColorAdjust(colorAdj);
		}
	}

	/**
	 * Sets the size of the texture.  This is used to find adjacent texels when filtering.
	 */
	public void setTexSize(final int width, final int height) {
		synchronized (mSync) {
			if ((mTexWidth != width) || (mTexHeight != height)) {
				mTexHeight = height;
				mTexWidth = width;
				final float rw = 1.0f / width;
				final float rh = 1.0f / height;

				mTexOffset[0] = -rw;	mTexOffset[1] = -rh;
				mTexOffset[2] = 0f;		mTexOffset[3] = -rh;
				mTexOffset[4] = rw;		mTexOffset[5] = -rh;

				mTexOffset[6] = -rw;	mTexOffset[7] = 0f;
				mTexOffset[8] = 0f;		mTexOffset[9] = 0f;
				mTexOffset[10] = rw;	mTexOffset[11] = 0f;

				mTexOffset[12] = -rw;	mTexOffset[13] = rh;
				mTexOffset[14] = 0f;	mTexOffset[15] = rh;
				mTexOffset[16] = rw;	mTexOffset[17] = rh;
			}
		}
	}

}
