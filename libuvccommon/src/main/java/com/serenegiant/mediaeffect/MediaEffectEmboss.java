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

public class MediaEffectEmboss extends MediaEffectKernel {

	private float mIntensity;
	public MediaEffectEmboss() {
		this(1.0f);
	}

	public MediaEffectEmboss(final float intensity) {
		super(new float[] {
				intensity * (-2.0f), -intensity, 0.0f,
				-intensity, 1.0f, intensity,
				0.0f, intensity, intensity * 2.0f,
			});
		mIntensity = intensity;
	}

	public MediaEffectEmboss setParameter(final float intensity) {
		if (mIntensity != intensity) {
			mIntensity = intensity;
			setParameter(new float[] {
				intensity * (-2.0f), -intensity, 0.0f,
				-intensity, 1.0f, intensity,
				0.0f, intensity, intensity * 2.0f,
			}, 0.0f);
		}
		return this;
	}
}
