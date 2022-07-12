package com.serenegiant.widget;
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

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.serenegiant.common.R;

/**
 * Created by saki on 2016/12/03.
 *
 */
public class AspectRatioFrameLayout extends FrameLayout implements IAspectRatioView {

	private double mRequestedAspect = -1.0;		// initially use default window size

	public AspectRatioFrameLayout(final Context context) {
		this(context, null, 0);
	}

	public AspectRatioFrameLayout(final Context context, final AttributeSet attrs) {
		super(context, attrs, 0);
	}

	public AspectRatioFrameLayout(final Context context, final AttributeSet attrs, final int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		final TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.IAspectRatioView, defStyleAttr, 0);
		try {
			mRequestedAspect = a.getFloat(R.styleable.IAspectRatioView_aspect_ratio, -1.0f);
		} finally {
			a.recycle();
		}
	}

	/**
	 * アスペクト比を保つように大きさを決める
	 */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// 要求されたアスペクト比が負の時(初期生成時)は何もしない
		if (mRequestedAspect > 0) {
			int initialWidth = MeasureSpec.getSize(widthMeasureSpec);
			int initialHeight = MeasureSpec.getSize(heightMeasureSpec);
			final int horizPadding = getPaddingLeft() + getPaddingRight();
			final int vertPadding = getPaddingTop() + getPaddingBottom();
			initialWidth -= horizPadding;
			initialHeight -= vertPadding;

			final double viewAspectRatio = (double)initialWidth / initialHeight;
			final double aspectDiff = mRequestedAspect / viewAspectRatio - 1;

			// 計算誤差が生じる可能性が有るので指定した値との差が小さければそのままにする
			if (Math.abs(aspectDiff) > 0.01) {
				if (aspectDiff > 0) {
					// 幅基準で高さを決める
					initialHeight = (int) (initialWidth / mRequestedAspect);
				} else {
					// 高さ基準で幅を決める
					initialWidth = (int) (initialHeight * mRequestedAspect);
				}
				initialWidth += horizPadding;
				initialHeight += vertPadding;
				widthMeasureSpec = MeasureSpec.makeMeasureSpec(initialWidth, MeasureSpec.EXACTLY);
				heightMeasureSpec = MeasureSpec.makeMeasureSpec(initialHeight, MeasureSpec.EXACTLY);
			}
		}

	     super.onMeasure(widthMeasureSpec, heightMeasureSpec);
 	}

	@Override
	public void setAspectRatio(final double aspectRatio) {
		if (mRequestedAspect != aspectRatio) {
			mRequestedAspect = aspectRatio;
			requestLayout();
		}
	}

	@Override
	public void setAspectRatio(final int width, final int height) {
		setAspectRatio(width / (double)height);
	}

	@Override
	public double getAspectRatio() {
		return mRequestedAspect;
	}

}
