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

import com.serenegiant.glutils.ShaderConst;

import static com.serenegiant.glutils.ShaderConst.*;

public class MediaEffectKernel extends MediaEffectGLESBase {
	private static final boolean DEBUG = false;
	private static final String TAG = "MediaEffectKernel";

	public MediaEffectKernel() {
		super(new MediaEffectKernel3x3Drawer(false, VERTEX_SHADER, FRAGMENT_SHADER_2D));
	}

	public MediaEffectKernel(final float[] kernel) {
		this();
		setParameter(kernel, 0.0f);
	}

	public MediaEffectKernel(final float[] kernel, final float color_adjust) {
		this();
		setParameter(kernel, color_adjust);
	}

	@Override
	public MediaEffectKernel resize(final int width, final int height) {
		super.resize(width, height);
		setTexSize(width, height);
		return this;
	}

	public void setKernel(final float[] values, final float colorAdj) {
		((MediaEffectKernel3x3Drawer)mDrawer).setKernel(values, colorAdj);
	}

	public void setColorAdjust(final float adjust) {
		((MediaEffectKernel3x3Drawer)mDrawer).setColorAdjust(adjust);
	}

	/**
	 * Sets the size of the texture.  This is used to find adjacent texels when filtering.
	 */
	public void setTexSize(final int width, final int height) {
		mDrawer.setTexSize(width, height);
	}

	/**
	 * synonym of setKernel
	 * @param kernel
	 * @param color_adjust
	 * @return
	 */
	public MediaEffectKernel setParameter(final float[] kernel, final float color_adjust) {
		setKernel(kernel, color_adjust);
		return this;
	}
}
