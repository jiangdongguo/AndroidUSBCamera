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
import android.widget.Checkable;
import android.widget.LinearLayout;

public class CheckableLinearLayout extends LinearLayout implements Checkable, Touchable {

//	private static final boolean DEBUG = false;	// FIXME 実働時にはfalseにすること
//	private static final String TAG = "CheckableLinearLayout";

	private boolean mChecked;

    private static final int[] CHECKED_STATE_SET = { android.R.attr.state_checked };

	public CheckableLinearLayout(final Context context) {
		this(context, null);
	}

	public CheckableLinearLayout(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CheckableLinearLayout(final Context context, final AttributeSet attrs, final int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	public boolean isChecked() {
		return mChecked;
	}

	@Override
	public void setChecked(final boolean checked) {
//		if (DEBUG) Log.v(TAG, "setChecked:" + checked);
		if (mChecked != checked) {
			mChecked = checked;
            final int n = this.getChildCount();
            View v;
            for (int i = 0; i < n; i++) {
            	v = this.getChildAt(i);
            	if (v instanceof Checkable)
            		((Checkable)v).setChecked(checked);
            }
            refreshDrawableState();
        }
	}

	@Override
	public void toggle() {
		setChecked(!mChecked);
	}

	@Override
    public int[] onCreateDrawableState(final int extraSpace) {
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
