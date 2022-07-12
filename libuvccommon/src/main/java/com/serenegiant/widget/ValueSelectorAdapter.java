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
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.serenegiant.common.R;

import java.util.ArrayList;
import java.util.List;


public class ValueSelectorAdapter extends ArrayAdapter<ValueSelectorAdapter.ValueEntry> {
	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = ValueSelectorAdapter.class.getSimpleName();

	public interface ValueSelectorAdapterListener {
		public void onTouch(final View view, final MotionEvent event, final int position);
	}

	public static class ValueEntry {
		public final String title;
		public final String value;

		private ValueEntry(final String title, final String value) {
			this.title = title;
			this.value = value;
		}
	}

	private static List<ValueEntry> createEntries(final Context context, final int entries_res, final int values_res) {
		final String[] entries = context.getResources().getStringArray(entries_res);
		final String[] values = context.getResources().getStringArray(values_res);
		final int n = Math.min(entries != null ? entries.length : 0, values != null ? values.length : 0);
		final List<ValueEntry> result = new ArrayList<ValueEntry>(n);
		for (int i = 0; i < n; i++) {
			result.add(new ValueEntry(entries[i], values[i]));
//			if (DEBUG) Log.v(TAG, String.format("createEntries:%d)%s=%s", i, entries[i], values[i]));
		}
		return result;
	}

	private final LayoutInflater mInflater;
	private final int mLayoutId;
	private final int mTitleId;
	private final ValueSelectorAdapterListener mListener;

	public ValueSelectorAdapter(final Context context,
		@LayoutRes final int layout_resource, final int title_id,
		final int entries_resource, final int values_resource,
		final ValueSelectorAdapterListener listener) {
		
		super(context, layout_resource, createEntries(context, entries_resource, values_resource));
		mInflater = LayoutInflater.from(context);
		mLayoutId = layout_resource;
		mTitleId = title_id;
		mListener = listener;
	}

	@NonNull
	@Override
	public View getView(final int position, final View convertView, @NonNull final ViewGroup parent) {
		View rootView = convertView;
		if (rootView == null) {
			final TextView label;
			rootView = mInflater.inflate(mLayoutId, parent, false);
			final ViewHolder holder = new ViewHolder();
			if (rootView instanceof TextView) {
				holder.titleTv = (TextView)rootView;
			} else {
				try {
					holder.titleTv = rootView.findViewById(mTitleId);
				} catch (final Exception e) {
					holder.titleTv = null;
				}
				if (holder.titleTv == null) {
					try {
						holder.titleTv = rootView.findViewById(R.id.title);
					} catch (final Exception e) {
						holder.titleTv = null;
					}
				}
			}
			rootView.setOnTouchListener(mOnTouchListener);
			rootView.setTag(holder);
		}
		final ViewHolder holder = (ViewHolder)rootView.getTag();
		// 指定行のデータを取得
		final ValueEntry item = getItem(position);
		if ((item != null) && (holder.titleTv != null)) {
			holder.titleTv.setText(item.title);
		}
		holder.position = position;
		return rootView;
	}

	@Override
	public View getDropDownView(final int position, final View convertView, @NonNull final ViewGroup parent) {
		return getView(position, convertView, parent);
	}

	public int getPosition(final int value) {
		int position = -1;
		final String _value = Integer.toString(value);
		final int n = getCount();
		for (int i = 0; i < n; i++) {
			final ValueEntry entry = getItem(i);
			if (_value.equals(entry.value)) {
				position = i;
				break;
			}
		}
		return position;
	}

	private View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(final View v, final MotionEvent event) {
			if (mListener != null) {
				final ViewHolder holder = (ViewHolder)v.getTag();
				final int position = holder != null ? holder.position : -1;
				try {
					mListener.onTouch(v, event, position);
				} catch (final Exception e) {
					if (DEBUG) Log.w(TAG, e);
				}
			}
			return false;
		}
	};

	private static class ViewHolder {
		int position;
		TextView titleTv;
	}

}
