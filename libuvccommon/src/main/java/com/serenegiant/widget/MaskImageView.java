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
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import androidx.appcompat.widget.AppCompatImageView;
import android.util.AttributeSet;

/**
 * Drawableで指定したMaskの不透過部分に対応するイメージを表示するImageView
 */
public class MaskImageView extends AppCompatImageView {

	private final Paint mMaskedPaint = new Paint();
	private final Paint mCopyPaint = new Paint();
	private final Rect mMaskBounds = new Rect();
	private final RectF mViewBoundsF = new RectF();
	private Drawable mMaskDrawable;

	public MaskImageView(final Context context) {
		this(context, null, 0);
	}

	public MaskImageView(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public MaskImageView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mMaskedPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
		mMaskDrawable = null;
	}

	/**
	 * マスク用のDrawableを指定, nullなら指定なし(元イメージ全部が表示される)
	 * アルファがセットされている部分に対応するイメージのみ表示される。アルファ以外の色は無視される。
	 * アルファが1より小さければそのアルファが適用されたイメージが表示される(半透過する)
	 * @param mask_drawable
	 */
	public synchronized void setMaskDrawable(final Drawable mask_drawable) {
		if (mMaskDrawable != mask_drawable) {
			mMaskDrawable = mask_drawable;
			if (mMaskDrawable != null) {
				mMaskDrawable.setBounds(mMaskBounds);
			}
			postInvalidate();
		}
	}

	@Override
    protected synchronized void onSizeChanged(int width, int height, int old_width, int old_height) {
    	super.onSizeChanged(width, height, old_width, old_height);
    	if ((width == 0) || (height == 0)) return;
    	// paddingを考慮してマスク用のDrawableのサイズを計算
    	final int padding_left = getPaddingLeft();
    	final int padding_top = getPaddingTop();
    	final int sz = Math.min(width - padding_left - getPaddingRight(), height - padding_top - getPaddingBottom());
		final int left =  (width - sz) / 2 + padding_left;
		final int top = (height - sz) / 2 + padding_top;
		mMaskBounds.set(left, top, left + sz, top + sz);
		if (sz > 3) {
			mMaskedPaint.setMaskFilter(new BlurMaskFilter(sz * 2 / 3.0f, BlurMaskFilter.Blur.NORMAL));
		}

        // View自体のサイズはそのまま
		mViewBoundsF.set(0, 0, width, height);
        if (mMaskDrawable != null) {
			mMaskDrawable.setBounds(mMaskBounds);
		}
    }

	@Override
	protected synchronized void onDraw(final Canvas canvas) {
		final int saveCount = canvas.saveLayer(mViewBoundsF, mCopyPaint,
			Canvas.ALL_SAVE_FLAG);
		try {
			if (mMaskDrawable != null) {
				mMaskDrawable.draw(canvas);
				canvas.saveLayer(mViewBoundsF, mMaskedPaint, 0);
			}
			super.onDraw(canvas);
		} finally {
			canvas.restoreToCount(saveCount);
		}
	}
}
