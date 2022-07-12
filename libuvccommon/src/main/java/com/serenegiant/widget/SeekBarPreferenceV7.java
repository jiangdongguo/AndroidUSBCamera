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
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.serenegiant.common.R;

import java.util.Locale;

public final class SeekBarPreferenceV7 extends Preference {
//	private static final boolean DEBUG = false;
//	private static final String TAG = "BrightnessOffsetSeekBarPreference";
	private static int sDefaultValue = 1;

	private final int mSeekbarLayoutId;
	private final int mSeekbarId;
	private final int mLabelTvId;
	private final int mMinValue, mMaxValue, mDefaultValue;
	private final float mScaleValue;
	private final String mFmtStr;
	private int preferenceValue;
	private TextView mTextView;

	// setWidgetLayoutResource()はPreference画面のpreferenceリストの右側に表示されるView
	// (例えばチェックボックスとか)のレイアウトを差し替えるときに使う
	// preference全体を変えてしまう時は、onCreateViewで必要なViewを生成する
	//			setWidgetLayoutResource(R.layout.seekbar_preference);

	public SeekBarPreferenceV7(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public SeekBarPreferenceV7(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
		TypedArray attribs = context.obtainStyledAttributes(attrs, R.styleable.SeekBarPreference, defStyle, 0);
		mSeekbarLayoutId = attribs.getResourceId(R.styleable.SeekBarPreference_seekbar_layout, R.layout.seekbar_preference);
		mSeekbarId = attribs.getResourceId(R.styleable.SeekBarPreference_seekbar_id, R.id.seekbar);
		mLabelTvId = attribs.getResourceId(R.styleable.SeekBarPreference_seekbar_label_id, R.id.seekbar_value_label);
		mMinValue = attribs.getInt(R.styleable.SeekBarPreference_min_value, 0);
		mMaxValue = attribs.getInt(R.styleable.SeekBarPreference_max_value, 100);
		mDefaultValue = attribs.getInt(R.styleable.SeekBarPreference_default_value, mMinValue);
		mScaleValue = attribs.getFloat(R.styleable.SeekBarPreference_scale_value, 1.0f);
		String fmt = attribs.getString(R.styleable.SeekBarPreference_value_format);
		try {
			final String dummy = String.format(fmt, 1.0f);
		} catch (final Exception e) {
			fmt = "%f";
		}
		mFmtStr = !TextUtils.isEmpty(fmt) ? fmt : "%f";
		attribs.recycle();
	}

	@Override
	public void onBindViewHolder(final PreferenceViewHolder holder) {
		super.onBindViewHolder(holder);
//		if (DEBUG) Log.w(TAG, "onBindView:");
		if ((mSeekbarLayoutId == 0) || (mSeekbarId == 0)) return;
		RelativeLayout parent = null;
		final ViewGroup group;
		if (holder.itemView instanceof ViewGroup) {
			group = (ViewGroup)holder.itemView;
			for (int i = group.getChildCount() - 1; i >= 0; i--) {
				final View v = group.getChildAt(i);
				if (v instanceof RelativeLayout) {
					parent = (RelativeLayout)v;
					break;
				}
			}
		} else {
			group = null;
		}
		if (parent == null) return;	// これにかかることはないはず
		// Seekbar(と値表示用のラベル入ったレイアウト)を生成する
        final LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        final View extraview = layoutInflater.inflate(mSeekbarLayoutId, group, false);
        if (extraview != null) {
			// summaryの下に挿入する
			final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.MATCH_PARENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
			params.addRule(RelativeLayout.BELOW, android.R.id.summary);
			parent.addView(extraview, params);

			final SeekBar seekBar = extraview.findViewById(mSeekbarId);
			if (seekBar != null) {
				seekBar.setMax(mMaxValue - mMinValue);
				final int progress = preferenceValue - mMinValue;
				seekBar.setProgress(progress);
				seekBar.setSecondaryProgress(progress);
				seekBar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
				seekBar.setEnabled(isEnabled());
			}
			mTextView = extraview.findViewById(R.id.seekbar_value_label);
			if (mTextView != null) {
				setValueLabel(preferenceValue, false);
				mTextView.setEnabled(isEnabled());
			}
		}
	}

	@Override
	protected Object onGetDefaultValue(final TypedArray a, final int index) {
//		if (DEBUG) Log.w(TAG, "onGetDefaultValue:" + a.getInt(index, sDefaultValue));
		return a.getInt(index, mDefaultValue);
	}

	@Override
	protected void onSetInitialValue(final boolean restorePersistedValue, final Object defaultValue) {
//		if (DEBUG) Log.w(TAG, "onSetInitialValue:restorePersistedValue=" + restorePersistedValue + ",defaultValue=" + defaultValue);
		try {
			preferenceValue = (Integer)defaultValue;
		} catch (final Exception e) {
			preferenceValue = mDefaultValue;
		}
		if (restorePersistedValue) {
			preferenceValue = getPersistedInt(preferenceValue);
		}
		persistInt(preferenceValue);
	}

	private final SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener
		= new SeekBar.OnSeekBarChangeListener() {

		@Override
		public void onStopTrackingTouch(final SeekBar seekBar) {
			// トラッキング終了時の処理
			final int newValue = seekBar.getProgress();
        	if (callChangeListener(newValue)){
        		preferenceValue = newValue + mMinValue;
        		persistInt(preferenceValue);
				setValueLabel(preferenceValue, false);
        	}
		}

		@Override
		public void onStartTrackingTouch(final SeekBar seekBar) {
			// トラッキング開始時の処理
		}

		@Override
		public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
			setValueLabel(progress + mMinValue, fromUser);
		}
	};

	private void setValueLabel(final int value, final boolean fromUser) {
		if (mTextView != null) {
			mTextView.setText(formatValueLabel(value, fromUser));
		}
	}

	protected String formatValueLabel(final int value, final boolean fromUser) {
		try {
			return String.format(mFmtStr, value * mScaleValue);
		} catch (final Exception e) {
			return String.format(Locale.US, "%f", value * mScaleValue);
		}
	}
}
