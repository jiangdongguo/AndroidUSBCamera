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
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.serenegiant.widget.ItemPicker.OnChangedListener;

public final class ItemPickerPreference extends Preference {
	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = ItemPickerPreference.class.getSimpleName();

	private int preferenceValue;
	private int mMinValue = 1, mMaxValue = 100;
	private ItemPicker mItemPicker;

	public ItemPickerPreference(final Context context) {
		super(context);
	}

	public ItemPickerPreference(final Context context, final AttributeSet attrs) {
		super(context, attrs);
	}

	public ItemPickerPreference(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onBindView(final View view) {
//		if (DEBUG) Log.v(TAG, "onBindView:");
		super.onBindView(view);
		RelativeLayout parent = null;
		final ViewGroup group = (ViewGroup)view;
		for (int i = group.getChildCount() - 1; i >= 0; i--) {
			final View v = group.getChildAt(i);
			if (v instanceof RelativeLayout) {
				parent = (RelativeLayout)v;
				break;
			}
		}
		// ItemPickerを生成
		mItemPicker = new ItemPicker(getContext());
		// summaryの下に挿入する
        final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
        	RelativeLayout.LayoutParams.MATCH_PARENT,
        	RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.BELOW, android.R.id.summary);
        parent.addView(mItemPicker, params);

		mItemPicker.setRange(mMinValue, mMaxValue);
		mItemPicker.setValue(preferenceValue);
		preferenceValue = mItemPicker.getValue();
		persistInt(preferenceValue);
		mItemPicker.setOnChangeListener(mOnChangeListener);
	}

	@Override
	protected Object onGetDefaultValue(final TypedArray a, final int index) {
//		if (DEBUG) Log.v(TAG, "onGetDefaultValue:");
//		return super.onGetDefaultValue(a, index);
		return a.getInt(index, 0);
	}

	@Override
	protected void onSetInitialValue(final boolean restorePersistedValue, final Object defaultValue) {
//		if (DEBUG) Log.v(TAG, "onSetInitialValue:");
		int def = preferenceValue;
		if (defaultValue instanceof Integer) {
			def = (Integer)defaultValue;
		} else if (defaultValue instanceof String) {
			try {
				def = Integer.parseInt((String)defaultValue);
			} catch (final Exception e) {
				if (DEBUG) Log.w(TAG, e);
			}
		}
		if (restorePersistedValue) {
			preferenceValue = getPersistedInt(def);
		} else {
			preferenceValue = def;
			persistInt(preferenceValue);
		}
	}

	private final OnChangedListener mOnChangeListener = new OnChangedListener() {
		@Override
		public void onChanged(final ItemPicker picker, final int oldVal, final int newVal) {
//			if (DEBUG) Log.v(TAG, "onChanged:");
			callChangeListener(newVal);
			preferenceValue = newVal;
    		persistInt(preferenceValue);
		}
	};

	public void setRange(int min, int max) {
//		if (DEBUG) Log.v(TAG, "setRange:");
		if (min > max) {
			final int w = min;
			min = max;
			max = w;
		}
		if ((mMinValue != min) || (mMaxValue != max) ) {
			mMaxValue = max;
			mMinValue = min;
			if (mItemPicker != null) {
				mItemPicker.setRange(mMinValue, mMaxValue);
				mItemPicker.setValue(preferenceValue);
				preferenceValue = mItemPicker.getValue();
				persistInt(preferenceValue);
			}
		}
	}
}
