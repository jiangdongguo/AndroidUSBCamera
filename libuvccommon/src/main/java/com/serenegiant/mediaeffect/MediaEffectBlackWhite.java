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

public class MediaEffectBlackWhite extends MediaEffect {
	/**
	 * コンストラクタ
	 * GLコンテキスト内で生成すること
	 *
	 * @param effect_context
	 */
	public MediaEffectBlackWhite(final EffectContext effect_context) {
		this(effect_context, 0.0f, 1.0f);
	}

	/**
	 * コンストラクタ
	 * GLコンテキスト内で生成すること
	 *
	 * @param effect_context
	 * @param black The value of the minimal pixel. 0-1
	 * @param white The value of the maximal pixel. 0-1
	 */
	public MediaEffectBlackWhite(final EffectContext effect_context, final float black, final float white) {
		super(effect_context, EffectFactory.EFFECT_BLACKWHITE);
		setParameter(black, white);
	}

	/**
	 * @param black The value of the minimal pixel. 0-1
	 * @param white The value of the maximal pixel. 0-1
	 * @return
	 */
	public MediaEffectBlackWhite setParameter(final float black, final float white) {
		setParameter("black", black);
		setParameter("white", white);
		return this;
	}
}
