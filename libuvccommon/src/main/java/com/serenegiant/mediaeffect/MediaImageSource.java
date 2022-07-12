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

import android.graphics.Bitmap;

import com.serenegiant.glutils.TextureOffscreen;

public class MediaImageSource extends MediaSource {
	private TextureOffscreen mImageOffscreen;
	private boolean isReset;
	/**
	 * コンストラクタ
	 * GLコンテキスト内で生成すること
	 */
	public MediaImageSource(final Bitmap src) {
		super(src.getWidth(), src.getHeight());
		mImageOffscreen = new TextureOffscreen(mWidth, mHeight, false);
		setSource(src);
	}

	public ISource setSource(final Bitmap bitmap) {
		mImageOffscreen.loadBitmap(bitmap);
		reset();
		return this;
	}

	@Override
	public ISource reset() {
		super.reset();
		isReset = true;
		mSrcTexIds[0] = mImageOffscreen.getTexture();
		return this;
	}

	@Override
	public ISource apply(IEffect effect) {
		if (mSourceScreen != null) {
			if (isReset) {
				isReset = false;
				needSwap = true;
			} else {
				if (needSwap) {
					final TextureOffscreen temp = mSourceScreen;
					mSourceScreen = mOutputScreen;
					mOutputScreen = temp;
					mSrcTexIds[0] = mSourceScreen.getTexture();
				}
				needSwap = !needSwap;
			}
			effect.apply(mSrcTexIds,
				mOutputScreen.getTexWidth(), mOutputScreen.getTexHeight(),
				mOutputScreen.getTexture());
		}
		return this;
	}

}
