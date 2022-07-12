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
import android.util.AttributeSet;
import android.widget.Checkable;

import androidx.appcompat.widget.AppCompatImageButton;

public class CheckableImageButton extends AppCompatImageButton implements Checkable {
	private static final boolean DEBUG = false; // 実同時はfalseにすること
	private static final String TAG = CheckableImageButton.class.getSimpleName();

	private boolean mIsChecked;
	private static final int[] CHECKED_STATE_SET = { android.R.attr.state_checked };

	public CheckableImageButton(Context context) {
		this(context, null, 0);
	}

	public CheckableImageButton(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CheckableImageButton(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	public void setChecked(boolean checked) {
		if (mIsChecked != checked) {
			mIsChecked = checked;
            refreshDrawableState();
        }
	}

	@Override
	public boolean isChecked() {
		return mIsChecked;
	}

	@Override
	public void toggle() {
		setChecked(!mIsChecked);
	}

	@Override
    public int[] onCreateDrawableState(final int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }

//	@Override
//	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//		updateParentDimension();
//		int width = MeasureSpec.getSize(widthMeasureSpec);
//		final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
//		switch (widthMode) {
//		case MeasureSpec.UNSPECIFIED:
//			// 好きな値を設定できる時
//			width = mLastWidth;
//			break;
//		case MeasureSpec.EXACTLY:
//			// この値にしないとダメな時・・・本来は変更しないんだけどignoreExactly=tueなら上書きする
//			if (ignoreExactly)
//				width = mLastWidth;
//			break;
//		case MeasureSpec.AT_MOST:
//			// 最大の大きさが指定された時
//			if (width > mLastWidth) width = mLastWidth;
//			break;
//		}
//		widthMeasureSpec = MeasureSpec.makeMeasureSpec(width, widthMode);
//
//		int height = MeasureSpec.getSize(heightMeasureSpec);
//		final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
//		switch (heightMode) {
//		case MeasureSpec.UNSPECIFIED:
//			// 好きな値を設定できる時
//			height = mLastHeight;
//			break;
//		case MeasureSpec.EXACTLY:
//			// この値にしないとダメな時・・・本来は変更しないんだけどignoreExactly=tueなら上書きする
//			if (ignoreExactly)
//				height = mLastHeight;
//			break;
//		case MeasureSpec.AT_MOST:
//			// 最大の大きさが指定された時
//			if (height > mLastHeight) height = mLastHeight;
//			break;
//		}
//		heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, heightMode);
//		setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
//		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//	}
//
//	@Override
//	protected void onLayout(final boolean changed, final int left, final int top, final int right, final int bottom) {
//		super.onLayout(changed, left, top, right, bottom);
//		ignoreExactly = false;
///*		if (mKeepAspect && (mAspectRatio < 0)) {
//			mAspectRatio = Math.abs((right -left) / (float)(bottom - top));
//		} */
//	}
//
//	/**
//	 * onMeasureでEXACPLYが来た時にもwidth/heightを上書きするためのフラグ
//	 */
//	private boolean ignoreExactly = false;
//	private final Rect mWorkBounds = new Rect();
//	/**
//	 * Viewの中心座標を指定座標に移動する。FrameLayoutに入れておく
//	 * @param x 親ViewGroup座標系でのx座標
//	 * @param y 親ViewGroup座標系でのy座標
//	 */
//	public void setPosition(final int x, final int y) {
//		if (DEBUG) Log.v(TAG, String.format("setPosition(%d,%d):", x, y) + this);
//		getHitRect(mWorkBounds);	// 親の座標系で矩形を取得
//		final int dx = x - mWorkBounds.centerX();
//		final int dy = y - mWorkBounds.centerY();
//		if ((dx != 0) || (dy != 0)) {
//
//			mWorkBounds.offset(dx, dy);
//			mLastWidth = mWorkBounds.width();
//			mLastHeight = mWorkBounds.height();
//			// View自体をリサイズ, これでセットするのは親の座標系での値
//			layout(mWorkBounds.left, mWorkBounds.top, mWorkBounds.right, mWorkBounds.bottom);
//			final Drawable drawable = getDrawable();
//			if (drawable != null) {
//				drawable.setBounds(mWorkBounds);
//			}
//			final ViewGroup.LayoutParams params = getLayoutParams();
//			if (params instanceof FrameLayout.LayoutParams) {
//				final FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams)params;
//				lp.setMargins(0, 0, 0, 0);
//				lp.leftMargin = mWorkBounds.left - mParentPaddingLeft;
//				lp.topMargin = mWorkBounds.top - mParentPaddingTop;
//				ignoreExactly = true;	// onMeasureでEXACPLYが来た時にもwidth/heightを上書きする
//				requestLayout();
//			}
//		}
//	}
//
//	public void setPosition(final int x, final int y, final int w, final int h) {
//		if (DEBUG) Log.v(TAG, String.format("setPosition(%d,%d,%d,%d):", x, y, w, h));
//		final int w2 = w >>> 1;
//		final int h2 = h >>> 1;
//		mLastWidth = w;
//		mLastHeight = h;
//		// View自体をリサイズ, これでセットするのは親の座標系での値
//		layout(x - w2, y - h2, x + w2, y + h2);
//		final Drawable drawable = getDrawable();
//		if (drawable != null) {
//			drawable.setBounds(x - w2, y - h2, x + w2, y + h2);
//		}
//		final ViewGroup.LayoutParams params = getLayoutParams();
//		if (params instanceof FrameLayout.LayoutParams) {
//			final FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams)params;
//			lp.leftMargin = x - w2  - mParentPaddingLeft;
//			lp.topMargin = y - h2  - mParentPaddingTop;
//			ignoreExactly = true;	// onMeasureでEXACPLYが来た時にもwidth/heightを上書きする
//			requestLayout();
//		}
//	}
//
//	private int mMaxWidth = -1, mMaxHeight = -1;
//	private int mParentPaddingLeft, mParentPaddingTop, mParentPaddingRight, mParentPaddingBottom;
//	private int mLastWidth, mLastHeight;
//	private void updateParentDimension() {
//		final ViewParent parent = getParent();
//		if ((parent != null) && (parent instanceof ViewGroup)) {
//			final ViewGroup vg = (ViewGroup)parent;
//			vg.getDrawingRect(mWorkBounds);
//			mParentPaddingLeft = vg.getPaddingLeft();
//			mParentPaddingTop = vg.getPaddingTop();
//			mParentPaddingRight = vg.getPaddingRight();
//			mParentPaddingBottom = vg.getPaddingBottom();
//			mMaxWidth = mWorkBounds.width() - mParentPaddingRight;
//			mMaxHeight = mWorkBounds.height() - mParentPaddingBottom;
//		} else
//			throw new RuntimeException("view parent not found");
//	}
//
}
