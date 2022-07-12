package com.serenegiant.dialog;
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
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import com.serenegiant.common.R;
import com.serenegiant.widget.ColorPickerView;
import com.serenegiant.widget.ColorPickerView.ColorPickerListener;

public class ColorPickerDialogV4 extends DialogFragmentEx {
	private static final boolean DEBUG = false;
	private static final String TAG = "ColorPickerDialog";

	private static final String KEY_COLOR_INIT = "initial_color";
	private static final String KEY_COLOR_CURRENT = "current_color";

	private static final int DEFAULT_COLOR = 0xffffffff;
	private OnColorChangedListener mListener;
	private int mTitleResId;
	private int mInitialColor = DEFAULT_COLOR;
	private int mCurrentColor = DEFAULT_COLOR;
	private boolean isCanceled;

	/**
	 * 色が変更されたときのコールバックリスナー
	 */
	public interface OnColorChangedListener {
		void onColorChanged(ColorPickerDialogV4 dialog, int color);
		void onCancel(ColorPickerDialogV4 dialog);
		void onDismiss(ColorPickerDialogV4 dialog, int color);
	}

	/**
	 * ダイアログ表示のためのヘルパーメソッド
	 * @param parent
	 * @param titleResId
	 * @param initialColor
	 * @return
	 */
	public static ColorPickerDialogV4 show(
		@NonNull final FragmentActivity parent,
		@StringRes final int titleResId, final int initialColor) {

		final ColorPickerDialogV4 dialog = newInstance(titleResId, initialColor);
		dialog.show(parent.getSupportFragmentManager(), TAG);
		return dialog;
	}

	/**
	 * ダイアログ表示のためのヘルパーメソッド
	 * @param parent
	 * @param titleResId
	 * @param initialColor
	 * @return
	 */
	public static ColorPickerDialogV4 show(
		@NonNull final Fragment parent,
		@StringRes final int titleResId, final int initialColor) {
	
		final ColorPickerDialogV4 dialog = newInstance(titleResId, initialColor);
		dialog.setTargetFragment(parent, 0);
		dialog.show(parent.getFragmentManager(), TAG);
		return dialog;
	}

	/**
	 * フラグメント生成のためのヘルパーメソッド
	 * @param titleResId
	 * @param initialColor
	 * @return
	 */
	public static ColorPickerDialogV4 newInstance(
		@StringRes final int titleResId, final int initialColor) {
	
		final ColorPickerDialogV4 dialog = new ColorPickerDialogV4();
		dialog.setArguments(titleResId, initialColor);
		return dialog;
	}

	/**
	 * コンストラクタ, 直接生成せずに#newInstanceを使うこと
	 */
	public ColorPickerDialogV4() {
		super();
		// デフォルトコンストラクタが必要
	}

	public void setArguments(@StringRes final int titleResId, final int initialColor) {
		Bundle args = getArguments();
		if (args == null) {
			args = new Bundle();
		}
		args.putInt(ARGS_KEY_ID_TITLE, titleResId);
		args.putInt(KEY_COLOR_INIT, initialColor);
		args.remove(KEY_COLOR_CURRENT);
		setArguments(args);
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (DEBUG) Log.v(TAG, "onCreate:");
		// 通常起動の場合はsavedInstanceState==null,
		// システムに破棄されたのが自動生成した時は
		// onSaveInstanceStateで保存した値が入ったBundleオブジェクトが入っている
		final Bundle args = requireArguments();
		mTitleResId = args.getInt(ARGS_KEY_ID_TITLE);
		mCurrentColor = mInitialColor = args.getInt(KEY_COLOR_INIT, DEFAULT_COLOR);
		if (savedInstanceState != null) {
			mCurrentColor = savedInstanceState.getInt(KEY_COLOR_CURRENT, mInitialColor);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull final Bundle outState) {
		super.onSaveInstanceState(outState);
		if (DEBUG) Log.v(TAG, "onSaveInstanceState:");
		outState.putInt(KEY_COLOR_CURRENT, mCurrentColor);
	}

	@Override
	public void onAttach(final Context context) {
		super.onAttach(context);
		isCanceled = false;
		if (DEBUG) Log.v(TAG, "onAttach:");
		// コールバックインターフェースを取得
		try {
			// 親がフラグメントの場合
			mListener = (OnColorChangedListener) getTargetFragment();
		} catch (final NullPointerException e1) {
			// ignore
		} catch (final ClassCastException e) {
			// ignore
		}
		if (mListener == null)
		try {
			// 親がフラグメントの場合
			mListener = (OnColorChangedListener) getParentFragment();
		} catch (final NullPointerException e1) {
			// ignore
		} catch (final ClassCastException e) {
			// ignore
		}
		if (mListener == null)
		try {
			// 親がActivityの場合
			mListener = (OnColorChangedListener) context;
		} catch (final ClassCastException e) {
			// ignore
		} catch (final NullPointerException e1) {
			// ignore
		}
		if (mListener == null) {
			// FIXME 呼び出し元がコールバックメソッドを実装していない時
			// throw new ClassCastException(activity.toString() +
			// " must implement OnColorChangedListener");
			Log.w(TAG, "must implement OnColorChangedListener");
		}
	}

	@NonNull
	@SuppressLint("InflateParams")
	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		if (DEBUG) Log.v(TAG, "onCreateDialog:");
		if (DEBUG) Log.i(TAG, String.format("onCreateDialog:mCurrentColor=%x", mCurrentColor));

		final Activity activity = requireActivity();
		final FrameLayout rootView
			= (FrameLayout)LayoutInflater.from(activity)
				.inflate(R.layout.color_picker, null);
		final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
			FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
		final ColorPickerView view = new ColorPickerView(activity);
		view.setColor(mCurrentColor);
		view.setColorPickerListener(mColorPickerListener);
		rootView.addView(view, params);
		final AlertDialog dialog = new AlertDialog.Builder(activity)
			.setPositiveButton(R.string.color_picker_select, mOnClickListener)
			.setNegativeButton(R.string.color_picker_cancel, mOnClickListener)
			.setTitle(mTitleResId != 0 ? mTitleResId : R.string.color_picker_default_title)
			.setView(rootView)
			.create();
		dialog.setCanceledOnTouchOutside(true);
		return dialog;
	}

	@Override
	public void onCancel(final DialogInterface dialog) {
		super.onCancel(dialog);
		if (DEBUG) Log.v(TAG, "onCancel:");
		isCanceled = true;
	}

	@Override
	public void onDismiss(final DialogInterface dialog) {
		super.onDismiss(dialog);
		if (mListener != null) {
			if (isCanceled) {
				if (DEBUG) Log.v(TAG, "call #onCancel:");
				mListener.onCancel(this);
			} else {
				if (DEBUG) Log.v(TAG, "call #nDismiss:");
				mListener.onDismiss(this, mCurrentColor);
			}
		}
	}

	private final ColorPickerListener mColorPickerListener = new ColorPickerListener() {
		@Override
		public void onColorChanged(final ColorPickerView view, final int color) {
			if (mCurrentColor != color) {
				mCurrentColor = color;
				if (mListener != null) {
					mListener.onColorChanged(ColorPickerDialogV4.this, color);
				}
			}
		}
	};

	private final DialogInterface.OnClickListener mOnClickListener
		= new DialogInterface.OnClickListener() {

		@Override
		public void onClick(final DialogInterface dialog, final int which) {
			if (DEBUG) Log.v(TAG, "onClick:which=" + which);
			switch (which) {
			case DialogInterface.BUTTON_POSITIVE:
				dialog.dismiss();
				break;
			case DialogInterface.BUTTON_NEGATIVE:
				dialog.cancel();
				break;
			}
		}
	};

}
