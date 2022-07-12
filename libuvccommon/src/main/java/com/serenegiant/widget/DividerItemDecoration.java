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
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

import com.serenegiant.common.R;

public class DividerItemDecoration extends RecyclerView.ItemDecoration  {
//	private static final boolean DEBUG = false;	// FIXME set false on production
//	private static final String TAG = DividerItemDecoration.class.getSimpleName();

	public static final int HORIZONTAL_LIST = LinearLayoutManager.HORIZONTAL;
    public static final int VERTICAL_LIST = LinearLayoutManager.VERTICAL;

	private static final int[] ATTRS = new int[] {
		android.R.attr.listDivider
	};

	private Drawable mDivider;
	private int mOrientation = VERTICAL_LIST;

	public DividerItemDecoration(final Context context) {
		Drawable divider = null;
		final TypedArray a = context.obtainStyledAttributes(ATTRS);
		try {
			divider = a.getDrawable(0);
		} catch (final Exception e) {
			// ignore
		}
		a.recycle();
		init(divider);
	}

	public DividerItemDecoration(final Context context, @DrawableRes final int divider) {
		init(context.getResources().getDrawable(divider));
	}

	public DividerItemDecoration(final Context context, final Drawable divider) {
		init(divider);
	}

	private void init(final Drawable divider) {
		mDivider = divider;
	}

	@Override
	public void onDraw(@NonNull final Canvas canvas,
		@NonNull final RecyclerView parent, @NonNull final RecyclerView.State state) {

		if (mDivider == null) return;
		if (mOrientation == VERTICAL_LIST) {
			drawVertical(canvas, parent);
		} else {
			drawHorizontal(canvas, parent);
		}
	}

	protected void drawVertical(final Canvas canvas, final RecyclerView parent) {
		final RecyclerView.LayoutManager manager = parent.getLayoutManager();

		final int left = parent.getPaddingLeft();
		final int right = parent.getWidth() - parent.getPaddingRight();

		final int childCount = parent.getChildCount() - 1;
		for (int i = 0; i < childCount; i++) {
			final View child = parent.getChildAt(i);
			if (hasDivider(child)) {
				final int top = child.getBottom();
				final int bottom = top + mDivider.getIntrinsicHeight();
				mDivider.setBounds(left, top, right, bottom);
				mDivider.draw(canvas);
			}
		}
	}

	protected void drawHorizontal(final Canvas canvas, final RecyclerView parent) {
		final RecyclerView.LayoutManager manager = parent.getLayoutManager();

		final int top = parent.getPaddingTop();
		final int bottom = parent.getHeight() - parent.getPaddingBottom();

		final int childCount = parent.getChildCount() - 1;
		for (int i = 0; i < childCount; i++) {
			final View child = parent.getChildAt(i);
			if (hasDivider(child)) {
				final int left = child.getLeft();
				final int right = left + mDivider.getIntrinsicWidth();
				mDivider.setBounds(left, top, right, bottom);
				mDivider.draw(canvas);
			}
		}
	}

	@Override
	public void getItemOffsets(@NonNull final Rect outRect, @NonNull final View view,
		@NonNull final RecyclerView parent, @NonNull final RecyclerView.State state) {

		final int position = parent.getChildAdapterPosition(view);
		if (mDivider == null) {
			outRect.set(0, 0, 0, 0);
		} else {
			if (hasDivider(view)) {
				if (mOrientation == VERTICAL_LIST) {
					outRect.set(0, 0, 0, mDivider.getIntrinsicHeight());
				} else {
					outRect.set(0, 0, mDivider.getIntrinsicWidth(), 0);
				}
			} else {
				outRect.set(0, 0, 0, 0);
			}
		}
	}

	public void setOrientation(final int orientation) {
		if (orientation != HORIZONTAL_LIST && orientation != VERTICAL_LIST) {
			throw new IllegalArgumentException("invalid orientation");
		}
		mOrientation = orientation;
	}

	/**
	 * helper method to get whether decorator needs to show divider for the specific view
	 * @param view
	 * @return
	 */
	protected boolean hasDivider(final View view) {
		if (view instanceof Dividable) {
			return ((Dividable)view).hasDivider();
		} else {
			final Boolean b = (Boolean)view.getTag(R.id.has_divider);
			return ((b != null) && b);
		}
	}
}
