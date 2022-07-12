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
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import com.serenegiant.utils.BuildCheck;

/**
 * パーミッション要求前に説明用のダイアログを表示するためのDialogFragment
 */
@SuppressWarnings("deprecation")
@Deprecated
public class MessageDialogFragment extends DialogFragment {
//	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = MessageDialogFragment.class.getSimpleName();

	public static interface MessageDialogListener {
		public void onMessageDialogResult(final MessageDialogFragment dialog, final int requestCode, final String[] permissions, final boolean result);
	}

	public static MessageDialogFragment showDialog(final Activity parent, final int requestCode, final int id_title, final int id_message, final String[] permissions) {
		final MessageDialogFragment dialog = newInstance(requestCode, id_title, id_message, permissions);
		dialog.show(parent.getFragmentManager(), TAG);
		return dialog;
	}

	public static MessageDialogFragment showDialog(final Fragment parent, final int requestCode, final int id_title, final int id_message, final String[] permissions) {
		final MessageDialogFragment dialog = newInstance(requestCode, id_title, id_message, permissions);
		dialog.setTargetFragment(parent, parent.getId());
		dialog.show(parent.getFragmentManager(), TAG);
		return dialog;
	}

	public static MessageDialogFragment newInstance(final int requestCode, final int id_title, final int id_message, final String[] permissions) {
		final MessageDialogFragment fragment = new MessageDialogFragment();
		final Bundle args = new Bundle();
		// ここでパラメータをセットする
		args.putInt("requestCode", requestCode);
		args.putInt("title", id_title);
		args.putInt("message", id_message);
		args.putStringArray("permissions", permissions != null ? permissions : new String[]{});
		fragment.setArguments(args);
		return fragment;
	}

	private MessageDialogListener mDialogListener;

	public MessageDialogFragment() {
		super();
		// デフォルトコンストラクタが必要
	}

	@SuppressLint("NewApi")
	@Override
	public void onAttach(final Activity activity) {
		super.onAttach(activity);
		// コールバックインターフェースを取得
		if (activity instanceof MessageDialogListener) {
			mDialogListener = (MessageDialogListener)activity;
		}
		if (mDialogListener == null) {
			final Fragment fragment = getTargetFragment();
			if (fragment instanceof MessageDialogListener) {
				mDialogListener = (MessageDialogListener)fragment;
			}
		}
		if (mDialogListener == null) {
			if (BuildCheck.isAndroid4_2()) {
				final Fragment target = getParentFragment();
				if (target instanceof MessageDialogListener) {
					mDialogListener = (MessageDialogListener)target;
				}
			}
		}
		if (mDialogListener == null) {
//			Log.w(TAG, "caller activity/fragment must implement PermissionDetailDialogFragmentListener");
        	throw new ClassCastException(activity.toString());
		}
	}

//	@Override
//    public void onCreate(final Bundle savedInstanceState) {
//    	super.onCreate(savedInstanceState);
//    	final Bundle args = savedInstanceState != null ? savedInstanceState : getArguments();
//	}

	@Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
		final Bundle args = savedInstanceState != null ? savedInstanceState : getArguments();
		final int requestCode = getArguments().getInt("requestCode");
		final int id_title = getArguments().getInt("title");
		final int id_message = getArguments().getInt("message");
		final String[] permissions = args.getStringArray("permissions");


		return new AlertDialog.Builder(getActivity())
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(id_title)
			.setMessage(id_message)
			.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog, final int whichButton) {
						// 本当はここでパーミッション要求をしたいだけどこのダイアログがdismissしてしまって結果を受け取れないので
						// 呼び出し側へ返してそこでパーミッション要求する。なのでこのダイアログは単にメッセージを表示するだけ
						try {
							mDialogListener.onMessageDialogResult(MessageDialogFragment.this, requestCode, permissions, true);
						} catch (final Exception e) {
							Log.w(TAG, e);
						}
					}
				}
			)
			.setNegativeButton(android.R.string.cancel,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog, int whichButton) {
						try {
							mDialogListener.onMessageDialogResult(MessageDialogFragment.this, requestCode, permissions, false);
						} catch (final Exception e) {
							Log.w(TAG, e);
						}
					}
				}
			)
			.create();
	}

}
