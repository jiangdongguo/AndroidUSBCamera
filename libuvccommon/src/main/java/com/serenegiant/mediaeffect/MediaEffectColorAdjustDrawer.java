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
 * MediaEffectSingleDrawerを継承しているので、使用できるテクスチャは1つだけ
 */
public class MediaEffectColorAdjustDrawer
	extends MediaEffectDrawer.MediaEffectSingleDrawer {

	private int muColorAdjustLoc;		// 色調整
	private float mColorAdjust;

	public MediaEffectColorAdjustDrawer(final String fss) {
		this(false, VERTEX_SHADER, fss);
	}

	public MediaEffectColorAdjustDrawer(final boolean isOES,
		final String fss) {

		this(isOES, VERTEX_SHADER, fss);
	}

	public MediaEffectColorAdjustDrawer(final boolean isOES,
		final String vss, final String fss) {

		super(isOES, vss, fss);
		muColorAdjustLoc = GLES20.glGetUniformLocation(getProgram(), "uColorAdjust");
		if (muColorAdjustLoc < 0) {
			muColorAdjustLoc = -1;
		}
	}

	public void setColorAdjust(final float adjust) {
		synchronized (mSync) {
			mColorAdjust = adjust;
		}
	}

	@Override
	protected void preDraw(@NonNull final int[] tex_ids,
		final float[] tex_matrix, final int offset) {

		super.preDraw(tex_ids, tex_matrix, offset);
		// 色調整オフセット
		if (muColorAdjustLoc >= 0) {
			synchronized (mSync) {
				GLES20.glUniform1f(muColorAdjustLoc, mColorAdjust);
			}
		}
	}
}
