package com.jiangdg.dialog;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

public abstract class DialogFragmentEx extends DialogFragment {
	protected static final String ARGS_KEY_REQUEST_CODE = "requestCode";
	protected static final String ARGS_KEY_ID_TITLE = "title";
	protected static final String ARGS_KEY_ID_MESSAGE = "message";
	protected static final String ARGS_KEY_TAG = "tag";

	@Override
	public void onSaveInstanceState(@NonNull final Bundle outState) {
		super.onSaveInstanceState(outState);
		final Bundle args = getArguments();
		if (args != null) {
			outState.putAll(args);
		}
	}

	@NonNull
	protected Bundle requireArguments() throws IllegalStateException {
		final Bundle args = getArguments();
		if (args == null) {
			throw new IllegalStateException();
		}
		return args;
	}
}
