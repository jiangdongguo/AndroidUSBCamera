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

import com.serenegiant.utils.BuildCheck;

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

public class MessageDialogFragmentV4 extends DialogFragmentEx {
//	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = MessageDialogFragmentV4.class.getSimpleName();

	private static final String ARGS_KEY_PERMISSIONS = "permissions";
	
	/**
	 * ダイアログの表示結果を受け取るためのコールバックリスナー
	 */
	public static interface MessageDialogListener {
		public void onMessageDialogResult(
			@NonNull final MessageDialogFragmentV4 dialog, final int requestCode,
			@NonNull final String[] permissions, final boolean result);
	}

	/**
	 * ダイアログ表示のためのヘルパーメソッド
	 * @param parent
	 * @param requestCode
	 * @param id_title
	 * @param id_message
	 * @param permissions
	 * @return
	 * @throws IllegalStateException
	 */
	public static MessageDialogFragmentV4 showDialog(
		@NonNull final FragmentActivity parent, final int requestCode,
		@StringRes final int id_title, @StringRes final int id_message,
		@NonNull final String[] permissions) throws IllegalStateException {

		final MessageDialogFragmentV4 dialog
			= newInstance(requestCode, id_title, id_message, permissions);
		dialog.show(parent.getSupportFragmentManager(), TAG);
		return dialog;
	}
	
	/**
	 * ダイアログ表示のためのヘルパーメソッド
	 * @param parent
	 * @param requestCode
	 * @param id_title
	 * @param id_message
	 * @param permissions
	 * @return
	 * @throws IllegalStateException
	 */
	public static MessageDialogFragmentV4 showDialog(
		@NonNull final Fragment parent, final int requestCode,
		@StringRes final int id_title, @StringRes final int id_message,
		@NonNull final String[] permissions) throws IllegalStateException {

		final MessageDialogFragmentV4 dialog
			= newInstance(requestCode, id_title, id_message, permissions);
		dialog.setTargetFragment(parent, parent.getId());
		dialog.show(parent.requireFragmentManager(), TAG);
		return dialog;
	}

	/**
	 * ダイアログ生成のためのヘルパーメソッド
	 * ダイアログ自体を直接生成せずにこのメソッドを呼び出すこと
	 * @param requestCode
	 * @param id_title
	 * @param id_message
	 * @param permissions
	 * @return
	 */
	public static MessageDialogFragmentV4 newInstance(
		final int requestCode,
		@StringRes final int id_title, @StringRes final int id_message,
		@NonNull final String[] permissions) {

		final MessageDialogFragmentV4 fragment = new MessageDialogFragmentV4();
		final Bundle args = new Bundle();
		// ここでパラメータをセットする
		args.putInt(ARGS_KEY_REQUEST_CODE, requestCode);
		args.putInt(ARGS_KEY_ID_TITLE, id_title);
		args.putInt(ARGS_KEY_ID_MESSAGE, id_message);
		args.putStringArray(ARGS_KEY_PERMISSIONS, permissions);
		fragment.setArguments(args);
		return fragment;
	}

	private MessageDialogListener mDialogListener;

	/**
	 * コンストラクタ, 直接生成せずに#newInstanceを使うこと
	 */
	public MessageDialogFragmentV4() {
		super();
		// デフォルトコンストラクタが必要
	}

	@Override
	public void onAttach(final Context context) {
		super.onAttach(context);
		// コールバックインターフェースを取得
		if (context instanceof MessageDialogListener) {
			mDialogListener = (MessageDialogListener)context;
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
        	throw new ClassCastException(context.toString());
		}
	}

//	@Override
//	public void onCreate(final Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
//		final Bundle args = savedInstanceState != null ? savedInstanceState : getArguments();
//	}

	@NonNull
	@Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
		final Bundle args = savedInstanceState != null ? savedInstanceState : requireArguments();
		final int id_title = args.getInt(ARGS_KEY_ID_TITLE);
		final int id_message = args.getInt(ARGS_KEY_ID_MESSAGE);

		final Activity activity = requireActivity();
		return new AlertDialog.Builder(activity)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(id_title)
			.setMessage(id_message)
			.setPositiveButton(android.R.string.ok, mOnClickListener)
			.setNegativeButton(android.R.string.cancel, mOnClickListener)
			.create();
	}

	private final DialogInterface.OnClickListener mOnClickListener
		= new DialogInterface.OnClickListener() {
		@Override
		public void onClick(final DialogInterface dialog, final int which) {
			// 本当はここでパーミッション要求をしたいだけどこのダイアログがdismissしてしまって結果を受け取れないので
			// 呼び出し側へ返してそこでパーミッション要求する。なのでこのダイアログは単にメッセージを表示するだけ
			callOnMessageDialogResult(which == DialogInterface.BUTTON_POSITIVE);
		}
	};

	@Override
	public void onCancel(final DialogInterface dialog) {
		super.onCancel(dialog);
		callOnMessageDialogResult(false);
	}
	
	/**
	 * コールバックリスナー呼び出しのためのヘルパーメソッド
	 * @param result
	 */
	private void callOnMessageDialogResult(final boolean result)
		throws IllegalStateException {

		final Bundle args = requireArguments();
		final int requestCode = args.getInt(ARGS_KEY_REQUEST_CODE);
		final String[] permissions = args.getStringArray(ARGS_KEY_PERMISSIONS);
		try {
			mDialogListener.onMessageDialogResult(
				MessageDialogFragmentV4.this,
				requestCode, permissions, result);
		} catch (final Exception e) {
			Log.w(TAG, e);
		}
	}
}
