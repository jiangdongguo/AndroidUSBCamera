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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.serenegiant.common.R;

public class RecycleViewWithEmptyView extends RecyclerView {
	private static final boolean DEBUG = false;	// FIXME set false on production
	private static final String TAG = RecycleViewWithEmptyView.class.getSimpleName();

   	@Nullable private View mEmptyView;

	public RecycleViewWithEmptyView(final Context context) {
		this(context, null, 0);
	}

	public RecycleViewWithEmptyView(final Context context,
									@Nullable final AttributeSet attrs) {

		this(context, attrs, 0);
	}

	@SuppressLint("WrongConstant")
	public RecycleViewWithEmptyView(final Context context,
		@Nullable final AttributeSet attrs, final int defStyle) {

		super(context, attrs, defStyle);
		Drawable divider = null;
		if (attrs != null) {
			int defStyleRes = 0;
			final TypedArray attribs = context.obtainStyledAttributes(
				attrs, R.styleable.RecycleViewWithEmptyView, defStyle, defStyleRes);
			try {
				if (attribs.hasValue(R.styleable.RecycleViewWithEmptyView_listDivider)) {
					divider = attribs.getDrawable(R.styleable.RecycleViewWithEmptyView_listDivider);
				}
			} catch (final Exception e) {
				if (DEBUG) Log.w(TAG, e);
			}
			attribs.recycle();
		}
		if (DEBUG) Log.v(TAG, "divider=" + divider);
		int orientation = LinearLayoutManager.VERTICAL;
		if (getLayoutManager() instanceof LinearLayoutManager) {
			orientation = ((LinearLayoutManager)getLayoutManager()).getOrientation();
		}
		final DividerItemDecoration deco = new DividerItemDecoration(context, divider);
		deco.setOrientation(orientation);
		addItemDecoration(deco);
	}

	@Override
	public void setAdapter(final Adapter adapter) {
		if (getAdapter() != adapter) {
			try {
				if (getAdapter() != null) {
					getAdapter().unregisterAdapterDataObserver(mAdapterDataObserver);
				}
			} catch (final Exception e) {
				// ignore
			}
			super.setAdapter(adapter);
			if (adapter != null) {
				adapter.registerAdapterDataObserver(mAdapterDataObserver);
			}
		}
		updateEmptyView();
	}

	public void setEmptyView(final View empty_view) {
		if (mEmptyView != empty_view) {
			mEmptyView = empty_view;
			updateEmptyView();
		}
	}

	protected void updateEmptyView() {
		if (mEmptyView != null) {
			final Adapter adapter = getAdapter();
			post(new Runnable() {
				@Override
				public void run() {
					mEmptyView.setVisibility((adapter == null) || (adapter.getItemCount() == 0)
						? VISIBLE : GONE);
				}
			});
		}
	}

	private final AdapterDataObserver mAdapterDataObserver = new AdapterDataObserver() {
		@Override
		public void onChanged() {
			super.onChanged();
			updateEmptyView();
		}

		@Override
		public void onItemRangeChanged(final int positionStart, final int itemCount) {
			super.onItemRangeChanged(positionStart, itemCount);
			updateEmptyView();
		}

//		@Override
//		public void onItemRangeChanged(final int positionStart,
// 				final int itemCount, Object payload) {
//
//			// fallback to onItemRangeChanged(positionStart, itemCount) if app
//			// does not override this method.
//			onItemRangeChanged(positionStart, itemCount);
//		}

//		@Override
//		public void onItemRangeInserted(final int positionStart, final int itemCount) {
//			updateEmptyView();
//		}

		@Override
		public void onItemRangeRemoved(final int positionStart, final int itemCount) {
			super.onItemRangeRemoved(positionStart, itemCount);
			updateEmptyView();
		}

//		@Override
//		public void onItemRangeMoved(final int fromPosition, final int toPosition,
// 			final int itemCount) {
//		}

	};

}
