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

import androidx.annotation.NonNull;

import com.serenegiant.glutils.TextureOffscreen;

public interface IEffect {
	public void apply(@NonNull final int[] src_tex_ids,
		final int width, final int height, final int out_tex_id);
	public void apply(@NonNull final int[] src_tex_ids,
		@NonNull final TextureOffscreen output);
	public void apply(ISource src);
	public void release();
	public IEffect resize(final int width, final int height);
	public boolean enabled();
	public IEffect setEnable(final boolean enable);
}
