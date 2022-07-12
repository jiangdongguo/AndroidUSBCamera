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

public class MediaEffectRotate extends MediaEffect {
	/**
	 * コンストラクタ
	 * GLコンテキスト内で生成すること
	 *
	 * @param effect_context
	 * @param angle The angle of rotation in degrees.
	 * 			This will be rounded to the nearest multiple of 90.
	 */
	public MediaEffectRotate(final EffectContext effect_context, final int angle) {
		super(effect_context, EffectFactory.EFFECT_ROTATE);
		setParameter(angle);
	}

	/**
	 * @param angle The angle of rotation in degrees.
	 * 			This will be rounded to the nearest multiple of 90.
	 * @return
	 */
	public MediaEffectRotate setParameter(final int angle) {
		setParameter("angle", angle);
		return this;
	}
}
