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

import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;

public class MediaEffectCrop extends MediaEffect {
	/**
	 * コンストラクタ
	 * GLコンテキスト内で生成すること
	 *
	 * @param effect_context
	 * @param x The origin's x-value. between 0 and width of the image.
	 * @param y The origin's y-value. between 0 and height of the image.
	 * @param width The width of the cropped image.
	 * 			between 1 and the width of the image minus xorigin.
	 * @param height The height of the cropped image.
	 * 			between 1 and the height of the image minus yorigin.
	 */
	public MediaEffectCrop(final EffectContext effect_context,
		final int x, final int y, final int width, final int height) {

		super(effect_context, EffectFactory.EFFECT_CROP);
		setParameter(x, y, width, height);
	}

	/**
	 * @param x The origin's x-value. between 0 and width of the image.
	 * @param y The origin's y-value. between 0 and height of the image.
	 * @param width The width of the cropped image.
	 * 			between 1 and the width of the image minus xorigin.
	 * @param height The height of the cropped image.
	 * 			between 1 and the height of the image minus yorigin.
	 * @return
	 */
	public MediaEffectCrop setParameter(
		final int x, final int y, final int width, final int height) {

		setParameter("xorigin", x);
		setParameter("yorigin", y);
		setParameter("width", width);
		setParameter("height", height);
		return this;
	}
}
