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
import android.text.TextUtils;

public class MediaEffectBackDropper extends MediaEffect {
	/**
	 * コンストラクタ
	 * GLコンテキスト内で生成すること
	 *
	 * @param effect_context
	 * @param source A URI for the background video to use.
	 * This parameter must be supplied before calling apply() for the first time.
	 */
	public MediaEffectBackDropper(final EffectContext effect_context, final String source) {
		super(effect_context, EffectFactory.EFFECT_BACKDROPPER);
		setParameter(source);
	}

	/**
	 * @param source A URI for the background video to use.
	 * This parameter must be supplied before calling apply() for the first time.
	 * @return
	 */
	public MediaEffectBackDropper setParameter(final String source) {
		if (!TextUtils.isEmpty(source))
			setParameter("source", source);
		return this;
	}
}
