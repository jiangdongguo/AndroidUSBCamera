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
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

public class ProgressView extends View {

	private int mRotation = 90;
	/**
	 * progressの最小・最大値
	 * それぞれの値でサチュレーション計算する
	 */
	private int mMin = 0, mMax = 100;
	/**
	 * progressの値をlevelに変換するための係数
	 * ClipDrawableのsetLevelに指定する値は0が完全にクリップ、10000がクリップなし
	 */
	private float mScale = 100;
	/**
	 * progressの現在値
	 */
	private volatile int mProgress = 40;

	/**
	 * Drawableを指定しない時に使うprogress表示色
	 */
	private int mColor = 0xffff0000;
	/**
	 * progressを表示するDrawable
	 */
	private Drawable mDrawable;
	/**
	 * progressに応じてmDrawableをクリップするためのClipDrawable
	 */
	private ClipDrawable mClipDrawable;

	public ProgressView(final Context context) {
		super(context);
	}

	public ProgressView(final Context context, final AttributeSet attrs) {
		super(context, attrs);
	}

	public ProgressView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onDraw(final Canvas canvas) {
		super.onDraw(canvas);
		mClipDrawable.draw(canvas);
	}

	@Override
	protected void onLayout(final boolean changed, final int left, final int top, final int right, final int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		resize();
	}

	public void setMinMax(final int min, final int max) {
		if (((mMin != min) || (mMax != max)) && (min != max)) {
			mMin = Math.min(min, max);
			mMax = Math.max(min, max);
			resize();
		}
	}

	/**
	 * progress値を設定
	 * 最小値よりも小さければ最小値になる。最大値よりも大きければ最大値になる。
	 * @param progress
	 */
	public void setProgress(final int progress) {
		if (mProgress != progress) {
			mProgress = progress;
			// 前はpostInvalidateを呼び出せばUIスレッド以外でも更新できたと思ったんだけど
			// UIスレッドじゃないと更新できない機種/Androidのバージョンがあるのかも
			removeCallbacks(mUpdateProgressTask);
			post(mUpdateProgressTask);
		}
	}

	private final Runnable mUpdateProgressTask = new Runnable() {
		@Override
		public void run() {
			if (mClipDrawable != null)  {
				int level = (int)(mProgress * mScale) + mMin;
				if (level < 0) level = 0;
				if (level > 10000) level = 10000;
				mClipDrawable.setLevel(level);
			}
			invalidate();
		}
	};

	/**
	 * Push object to the top of its container, not changing its size.
	 */
	protected static final int GRAVITY_TOP = 0x30;
	/**
	 * Push object to the bottom of its container, not changing its size.
	 */
	protected static final int GRAVITY_BOTTOM = 0x50;
	/**
	 * Push object to the left of its container, not changing its size.
	 */
	protected static final int GRAVITY_LEFT = 0x03;
	/**
	 * Push object to the right of its container, not changing its size.
	 */
	protected static final int GRAVITY_RIGHT = 0x05;
	/**
	 * Place object in the vertical center of its container, not changing its size.
	 */
	protected static final int GRAVITY_CENTER_VERTICAL = 0x10;
	/**
	 * Grow the vertical size of the object if needed so it completely fills its container.
	 */
	protected static final int GRAVITY_FILL_VERTICAL = 0x70;
	/**
	 * Place object in the horizontal center of its container, not changing its size.
	 */
	protected static final int GRAVITY_CENTER_HORIZONTAL = 0x01;
	/**
	 * Grow the horizontal size of the object if needed so it completely fills its container.
	 */
	protected static final int GRAVITY_FILL_HORIZONTAL = 0x07;
	/**
	 * Place the object in the center of its container in both the vertical and horizontal axis, not changing its size.
	 */
	protected static final int GRAVITY_CENTER = 0x11;
	/**
	 * Grow the horizontal and vertical size of the object if needed so it completely fills its container.
	 */
	protected static final int GRAVITY_FILL = 0x77;
	/**
	 * Additional option that can be set to have the top and/or bottom edges of the child clipped to its container's bounds.
	 * The clip will be based on the vertical gravity: a top gravity will clip the bottom edge, a bottom gravity will clip the top edge, and neither will clip both edges.
	 */
	protected static final int GRAVITY_CLIP_VERTICAL = 0x80;
	/**
	 * Additional option that can be set to have the left and/or right edges of the child clipped to its container's bounds.
	 * The clip will be based on the horizontal gravity: a left gravity will clip the right edge, a right gravity will clip the left edge, and neither will clip both edges.
	 */
	protected static final int GRAVITY_CLIP_HORIZONTAL = 0x08;
	/**
	 * Push object to the beginning of its container, not changing its size.
	 */
	protected static final int GRAVITY_START = 0x00800003;
	/**
	 * Push object to the end of its container, not changing its size.
	 */
	protected static final int GRAVITY_END = 0x00800005;

	/**
	 * プログレスの回転方向を指定
	 * @param rotation 0:
	 */
	public void setRotation(int rotation) {
		rotation = ((rotation / 90) * 90) % 360;
		if (mRotation != rotation) {
			mRotation = rotation;
			resize();
		}
	}

	/**
	 * progress表示用の色を指定する。
	 * #setDrawableと#setColorは後から呼び出した方が優先される。
	 * @param color
	 */
	public void setColor(final int color) {
		if (mColor != color) {
			mColor = color;
			refreshDrawable(null);
		}
	}

	/**
	 * progress表示用のDrawableを指定する。
	 * #setDrawableと#setColorは後から呼び出した方が優先される。
	 * @param drawable
	 */
	public void setDrawable(final Drawable drawable) {
		if (mDrawable != drawable) {
			refreshDrawable(drawable);
		}
	}

	protected void resize() {
		final float progress = mProgress * mScale + mMin;
		mScale = 10000.0f / (mMax - mMin);
		mProgress = (int)((progress - mMin) / mScale);
		refreshDrawable(mDrawable);
	}

	protected void refreshDrawable(final Drawable drawable) {
		mDrawable = drawable;
		if (mDrawable == null) {
			mDrawable = new ColorDrawable(mColor);
		}
		int gravity = GRAVITY_FILL_VERTICAL | GRAVITY_LEFT;
		int orientation = ClipDrawable.HORIZONTAL;
		switch (mRotation) {
		case 90:
			gravity = GRAVITY_FILL_HORIZONTAL | GRAVITY_BOTTOM;
			orientation = ClipDrawable.VERTICAL;
			break;
		case 180:
			gravity = GRAVITY_FILL_VERTICAL | GRAVITY_RIGHT;
			orientation = ClipDrawable.HORIZONTAL;
			break;
		case 270:
			gravity = GRAVITY_FILL_HORIZONTAL | GRAVITY_TOP;
			orientation = ClipDrawable.VERTICAL;
			break;
		}
		mClipDrawable = new ClipDrawable(mDrawable, gravity, orientation);
		final Rect outRect = new Rect();
		getDrawingRect(outRect);
		mClipDrawable.setBounds(outRect);
		mClipDrawable.setLevel((int)(mProgress * mScale) + mMin);
		postInvalidate();
	}
}
