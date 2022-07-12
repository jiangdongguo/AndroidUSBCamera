package com.serenegiant.view.animation;
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
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

/**
 * Viewのりサイズを行うAnimationクラス
 * 見た目だけじゃなくて実際のサイズも変更する
 */

public class ResizeAnimation extends Animation {
	@NonNull
	private final View mTargetView;
	private final int mStartWidth, mStartHeight;
	private final int mDiffWidth, mDiffHeight;
	
	
	public ResizeAnimation(@NonNull final View view,
		final int startWidth, final int startHeight,
		final int endWidth, final int endHeight) {

		mTargetView = view;
		mStartWidth = startWidth;
		mStartHeight = startHeight;
		mDiffWidth = endWidth - startWidth;
		mDiffHeight = endHeight - startHeight;
	}
	
	@Override
	protected void applyTransformation(final float interpolatedTime,
		final Transformation t) {

		super.applyTransformation(interpolatedTime, t);	// this is empty method now
		
		final int newWidth = (int)(mStartWidth + mDiffWidth * interpolatedTime);
		final int newHeight = (int)(mStartHeight + mDiffHeight * interpolatedTime);
		
		mTargetView.getLayoutParams().width = newWidth;
		mTargetView.getLayoutParams().height = newHeight;
		mTargetView.requestLayout();
	}
	
	@Override
	public void initialize(final int width, final int height,
		final int parentWidth, final int parentHeight) {

		super.initialize(width, height, parentWidth, parentHeight);
	}
	
	@Override
	public boolean willChangeBounds() {
		return (mDiffWidth != 0) || (mDiffHeight != 0);
	}
}
