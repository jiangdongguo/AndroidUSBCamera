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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.RelativeLayout;

public class CheckableRelativeLayout extends RelativeLayout implements Checkable, Touchable {

	private boolean mIsChecked;
	private static final int[] CHECKED_STATE_SET = { android.R.attr.state_checked };

	public CheckableRelativeLayout(final Context context) {
		this(context, null);
	}

	public CheckableRelativeLayout(final Context context, final AttributeSet attrs) {
		super(context, attrs);
	}


	@Override
	public boolean isChecked() {
		return mIsChecked;
	}

	@Override
	public void setChecked(final boolean checked) {
		if (mIsChecked != checked) {
			mIsChecked = checked;
			updateChildState(this, checked);
            refreshDrawableState();
        }
	}

	protected void updateChildState(final ViewGroup group, final boolean checked) {
		final int n = group.getChildCount();
		for (int i = 0; i < n; i++) {
			final View child = group.getChildAt(i);
			if (child instanceof Checkable) {
				((Checkable)child).setChecked(checked);
			}
		}
	}

	@Override
	public void toggle() {
		setChecked(!mIsChecked);
	}

	@Override
    protected int[] onCreateDrawableState(final int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }

	private float mTouchX, mTouchY;
	@Override
	public boolean onInterceptTouchEvent(final MotionEvent ev) {
		mTouchX = ev.getX();
		mTouchY = ev.getY();
		return super.onInterceptTouchEvent(ev);
	}

	@Override
	public float touchX() { return mTouchX; }
	@Override
	public float touchY() { return mTouchY; }
}
