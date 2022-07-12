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

import android.media.effect.Effect;
import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import com.serenegiant.glutils.TextureOffscreen;

public class MediaEffect implements IEffect {
	protected final EffectContext mEffectContext;
	protected Effect mEffect;
	protected boolean mEnabled = true;
	/**
	 * コンストラクタ
	 * GLコンテキスト内で生成すること
	 * @param effect_context
	 */
	public MediaEffect(final EffectContext effect_context, final String effectName) {
		mEffectContext = effect_context;
		final EffectFactory factory = effect_context.getFactory();
		if (TextUtils.isEmpty(effectName)) {
			mEffect = null;
		} else {
			mEffect = factory.createEffect(effectName);
		}
	}

	@Override
	public void apply(@NonNull final int [] src_tex_ids,
		final int width, final int height, final int out_tex_id) {

		if (mEnabled && (mEffect != null)) {
			mEffect.apply(src_tex_ids[0], width, height, out_tex_id);
		}
	}

	@Override
	public void apply(@NonNull final int [] src_tex_ids,
		@NonNull final TextureOffscreen output) {

		if (mEnabled && (mEffect != null)) {
			mEffect.apply(src_tex_ids[0],
				output.getWidth(), output.getHeight(),
				output.getTexture());
		}
	}

	@Override
	public void apply(final ISource src) {
		if (mEnabled && (mEffect != null)) {
			mEffect.apply(src.getSourceTexId()[0],
				src.getWidth(), src.getHeight(),
				src.getOutputTexId());
		}
	}

	@Override
	public void release() {
		if (mEffect != null) {
			mEffect.release();
			mEffect = null;
		}
	}

	@Override
	public MediaEffect resize(final int width, final int height) {
		// ignore
		return this;
	}

	protected MediaEffect setParameter(final String parameterKey, final Object value) {
		if ((mEffect != null) && (value != null)) {
			mEffect.setParameter(parameterKey, value);
		}
		return this;
	}

	@Override
	public boolean enabled() {
		return mEnabled;
	}

	@Override
	public IEffect setEnable(final boolean enable) {
		mEnabled = enable;
		return this;
	}
}
