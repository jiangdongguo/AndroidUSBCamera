package com.serenegiant.widget;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.CallSuper;
import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.preference.Preference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.serenegiant.common.R;

public class DialogPreferenceV7 extends Preference
	implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {
	
	private AlertDialog.Builder mBuilder;
	
	private CharSequence mDialogTitle;
	private CharSequence mDialogMessage;
	private Drawable mDialogIcon;
	private CharSequence mPositiveButtonText;
	private CharSequence mNegativeButtonText;
	private int mDialogLayoutResId;
	
	/**
	 * The dialog, if it is showing.
	 */
	private Dialog mDialog;
	
	/**
	 * Which button was clicked.
	 */
	private int mWhichButtonClicked;
	
	public DialogPreferenceV7(@NonNull final Context context) {
		this(context, null, 0, 0);
	}
	
	public DialogPreferenceV7(@NonNull final Context context,
		@Nullable final AttributeSet attrs) {

		this(context, attrs, 0, 0);
	}
	
	public DialogPreferenceV7(@NonNull final Context context,
		@Nullable final AttributeSet attrs, final int defStyleAttr) {
		
		this(context, attrs, defStyleAttr, 0);
	}
	
	public DialogPreferenceV7(@NonNull final Context context,
		@Nullable final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
		
		super(context, attrs, defStyleAttr, defStyleRes);
		final TypedArray a = context.obtainStyledAttributes(attrs,
			androidx.preference.R.styleable.DialogPreference,
			defStyleAttr, defStyleRes);
		
		mDialogTitle = a.getString(
			androidx.preference.R.styleable.DialogPreference_dialogTitle);
		if (mDialogTitle == null) {
			// Fall back on the regular title of the preference
			// (the one that is seen in the list)
			mDialogTitle = getTitle();
		}
		
		mDialogMessage = a.getString(
			androidx.preference.R.styleable.DialogPreference_dialogMessage);
		mDialogIcon = a.getDrawable(
			androidx.preference.R.styleable.DialogPreference_dialogIcon);
		mPositiveButtonText = a.getString(
			androidx.preference.R.styleable.DialogPreference_positiveButtonText);
		mNegativeButtonText = a.getString(
			androidx.preference.R.styleable.DialogPreference_negativeButtonText);
		mDialogLayoutResId = a.getResourceId(
			androidx.preference.R.styleable.DialogPreference_dialogLayout,
			mDialogLayoutResId);
		a.recycle();
	}
	
	/**
	 * Sets the title of the dialog. This will be shown on subsequent dialogs.
	 *
	 * @param dialogTitle The title.
	 */
	public void setDialogTitle(final CharSequence dialogTitle) {
		mDialogTitle = dialogTitle;
	}
	
	/**
	 * @param dialogTitleResId The dialog title as a resource.
	 * @see #setDialogTitle(CharSequence)
	 */
	public void setDialogTitle(@StringRes final int dialogTitleResId) {
		setDialogTitle(getContext().getString(dialogTitleResId));
	}
	
	/**
	 * Returns the title to be shown on subsequent dialogs.
	 *
	 * @return The title.
	 */
	@Nullable
	public CharSequence getDialogTitle() {
		return mDialogTitle;
	}
	
	/**
	 * Sets the message of the dialog. This will be shown on subsequent dialogs.
	 * <p>
	 * This message forms the content View of the dialog and conflicts with
	 * list-based dialogs, for example. If setting a custom View on a dialog via
	 * {@link #setDialogLayoutResource(int)}, include a text View with ID
	 * {@link android.R.id#message} and it will be populated with this message.
	 *
	 * @param dialogMessage The message.
	 */
	public void setDialogMessage(final CharSequence dialogMessage) {
		mDialogMessage = dialogMessage;
	}
	
	/**
	 * @param dialogMessageResId The dialog message as a resource.
	 * @see #setDialogMessage(CharSequence)
	 */
	public void setDialogMessage(@StringRes final int dialogMessageResId) {
		setDialogMessage(getContext().getString(dialogMessageResId));
	}
	
	/**
	 * Returns the message to be shown on subsequent dialogs.
	 *
	 * @return The message.
	 */
	public CharSequence getDialogMessage() {
		return mDialogMessage;
	}
	
	/**
	 * Sets the icon of the dialog. This will be shown on subsequent dialogs.
	 *
	 * @param dialogIcon The icon, as a {@link Drawable}.
	 */
	public void setDialogIcon(final Drawable dialogIcon) {
		mDialogIcon = dialogIcon;
	}
	
	/**
	 * Sets the icon (resource ID) of the dialog. This will be shown on
	 * subsequent dialogs.
	 *
	 * @param dialogIconRes The icon, as a resource ID.
	 */
	public void setDialogIcon(@DrawableRes final int dialogIconRes) {
		mDialogIcon = getContext().getResources().getDrawable(dialogIconRes);
	}
	
	/**
	 * Returns the icon to be shown on subsequent dialogs.
	 *
	 * @return The icon, as a {@link Drawable}.
	 */
	public Drawable getDialogIcon() {
		return mDialogIcon;
	}
	
	/**
	 * Sets the text of the positive button of the dialog. This will be shown on
	 * subsequent dialogs.
	 *
	 * @param positiveButtonText The text of the positive button.
	 */
	public void setPositiveButtonText(final CharSequence positiveButtonText) {
		mPositiveButtonText = positiveButtonText;
	}
	
	/**
	 * @param positiveButtonTextResId The positive button text as a resource.
	 * @see #setPositiveButtonText(CharSequence)
	 */
	public void setPositiveButtonText(@StringRes final int positiveButtonTextResId) {
		setPositiveButtonText(getContext().getString(positiveButtonTextResId));
	}
	
	/**
	 * Returns the text of the positive button to be shown on subsequent
	 * dialogs.
	 *
	 * @return The text of the positive button.
	 */
	public CharSequence getPositiveButtonText() {
		return mPositiveButtonText;
	}
	
	/**
	 * Sets the text of the negative button of the dialog. This will be shown on
	 * subsequent dialogs.
	 *
	 * @param negativeButtonText The text of the negative button.
	 */
	public void setNegativeButtonText(final CharSequence negativeButtonText) {
		mNegativeButtonText = negativeButtonText;
	}
	
	/**
	 * @param negativeButtonTextResId The negative button text as a resource.
	 * @see #setNegativeButtonText(CharSequence)
	 */
	public void setNegativeButtonText(@StringRes final int negativeButtonTextResId) {
		setNegativeButtonText(getContext().getString(negativeButtonTextResId));
	}
	
	/**
	 * Returns the text of the negative button to be shown on subsequent
	 * dialogs.
	 *
	 * @return The text of the negative button.
	 */
	public CharSequence getNegativeButtonText() {
		return mNegativeButtonText;
	}
	
	/**
	 * Sets the layout resource that is inflated as the {@link View} to be shown
	 * as the content View of subsequent dialogs.
	 *
	 * @param dialogLayoutResId The layout resource ID to be inflated.
	 * @see #setDialogMessage(CharSequence)
	 */
	public void setDialogLayoutResource(@LayoutRes final int dialogLayoutResId) {
		mDialogLayoutResId = dialogLayoutResId;
	}
	
	/**
	 * Returns the layout resource that is used as the content View for
	 * subsequent dialogs.
	 *
	 * @return The layout resource.
	 */
	@LayoutRes
	public int getDialogLayoutResource() {
		return mDialogLayoutResId;
	}
	
	/**
	 * Prepares the dialog builder to be shown when the preference is clicked.
	 * Use this to set custom properties on the dialog.
	 * <p>
	 * Do not {@link AlertDialog.Builder#create()} or
	 * {@link AlertDialog.Builder#show()}.
	 */
	protected void onPrepareDialogBuilder(@NonNull final AlertDialog.Builder builder) {
	}
	
	@Override
	protected void onClick() {
		if (mDialog != null && mDialog.isShowing()) return;
		
		showDialog(null);
	}
	
	/**
	 * Shows the dialog associated with this Preference. This is normally initiated
	 * automatically on clicking on the preference. Call this method if you need to
	 * show the dialog on some other event.
	 *
	 * @param state Optional instance state to restore on the dialog
	 */
	protected void showDialog(final Bundle state) {
		Context context = getContext();
		
		mWhichButtonClicked = DialogInterface.BUTTON_NEGATIVE;
		// XXX 本当はDialogFragmentにしたいけど親ActivityのFragmentManagerを取得する方法がないのでとりあえずAlartDialogのままにする
		mBuilder = new AlertDialog.Builder(context)
			.setTitle(mDialogTitle)
			.setIcon(mDialogIcon)
			.setPositiveButton(mPositiveButtonText, this)
			.setNegativeButton(mNegativeButtonText, this);
		
		View contentView = onCreateDialogView();
		if (contentView != null) {
			onBindDialogView(contentView);
			mBuilder.setView(contentView);
		} else {
			mBuilder.setMessage(mDialogMessage);
		}
		
		onPrepareDialogBuilder(mBuilder);

		// Create the dialog
		final Dialog dialog = mDialog = mBuilder.create();
		if (state != null) {
			dialog.onRestoreInstanceState(state);
		}
		if (needInputMethod()) {
			requestInputMethod(dialog);
		}
		dialog.setOnDismissListener(this);
		dialog.show();
	}
	
	/**
	 * Returns whether the preference needs to display a soft input method when the dialog
	 * is displayed. Default is false. Subclasses should override this method if they need
	 * the soft input method brought up automatically.
	 *
	 * @hide
	 */
	protected boolean needInputMethod() {
		return false;
	}
	
	/**
	 * Sets the required flags on the dialog window to enable input method window to show up.
	 */
	private void requestInputMethod(@NonNull final Dialog dialog) {
		Window window = dialog.getWindow();
		window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
	}
	
	/**
	 * Creates the content view for the dialog (if a custom content view is
	 * required). By default, it inflates the dialog layout resource if it is
	 * set.
	 *
	 * @return The content View for the dialog.
	 * @see #setLayoutResource(int)
	 */
	protected View onCreateDialogView() {
		if (mDialogLayoutResId == 0) {
			return null;
		}
		
		LayoutInflater inflater = LayoutInflater.from(mBuilder.getContext());
		return inflater.inflate(mDialogLayoutResId, null);
	}
	
	/**
	 * Binds views in the content View of the dialog to data.
	 * <p>
	 * Make sure to call through to the superclass implementation.
	 *
	 * @param view The content View of the dialog, if it is custom.
	 */
	@CallSuper
	protected void onBindDialogView(@NonNull final View view) {
		View dialogMessageView = view.findViewById(R.id.message);
		
		if (dialogMessageView != null) {
			final CharSequence message = getDialogMessage();
			int newVisibility = View.GONE;
			
			if (!TextUtils.isEmpty(message)) {
				if (dialogMessageView instanceof TextView) {
					((TextView) dialogMessageView).setText(message);
				}
				
				newVisibility = View.VISIBLE;
			}
			
			if (dialogMessageView.getVisibility() != newVisibility) {
				dialogMessageView.setVisibility(newVisibility);
			}
		}
	}
	
	@Override
	public void onClick(final DialogInterface dialog, int which) {
		mWhichButtonClicked = which;
	}
	
	@Override
	public void onDismiss(final DialogInterface dialog) {

		mDialog = null;
		onDialogClosed(mWhichButtonClicked == DialogInterface.BUTTON_POSITIVE);
	}
	
	@Override
	public void onAttached() {
		super.onAttached();
	}

	@Override
	public void onDetached() {
		onActivityDestroy();
		super.onDetached();
	}
	
	/**
	 * Called when the dialog is dismissed and should be used to save data to
	 * the {@link SharedPreferences}.
	 *
	 * @param positiveResult Whether the positive button was clicked (true), or
	 *                       the negative button was clicked or the dialog was canceled (false).
	 */
	protected void onDialogClosed(boolean positiveResult) {
	}
	
	/**
	 * Gets the dialog that is shown by this preference.
	 *
	 * @return The dialog, or null if a dialog is not being shown.
	 */
	@Nullable
	public Dialog getDialog() {
		return mDialog;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void onActivityDestroy() {
		
		if (mDialog == null || !mDialog.isShowing()) {
			return;
		}
		
		mDialog.dismiss();
	}
	
	@Override
	protected Parcelable onSaveInstanceState() {
		final Parcelable superState = super.onSaveInstanceState();
		if (mDialog == null || !mDialog.isShowing()) {
			return superState;
		}
		
		final SavedState myState = new SavedState(superState);
		myState.isDialogShowing = true;
		myState.dialogBundle = mDialog.onSaveInstanceState();
		return myState;
	}
	
	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		if (state == null || !state.getClass().equals(SavedState.class)) {
			// Didn't save state for us in onSaveInstanceState
			super.onRestoreInstanceState(state);
			return;
		}
		
		SavedState myState = (SavedState) state;
		super.onRestoreInstanceState(myState.getSuperState());
		if (myState.isDialogShowing) {
			showDialog(myState.dialogBundle);
		}
	}
	
	private static class SavedState extends android.preference.Preference.BaseSavedState {
		boolean isDialogShowing;
		Bundle dialogBundle;
		
		public SavedState(Parcel source) {
			super(source);
			isDialogShowing = source.readInt() == 1;
			dialogBundle = source.readBundle();
		}
		
		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeInt(isDialogShowing ? 1 : 0);
			dest.writeBundle(dialogBundle);
		}
		
		public SavedState(Parcelable superState) {
			super(superState);
		}
		
		public static final Parcelable.Creator<SavedState> CREATOR =
			new Parcelable.Creator<SavedState>() {
				public SavedState createFromParcel(Parcel in) {
					return new SavedState(in);
				}
				
				public SavedState[] newArray(int size) {
					return new SavedState[size];
				}
			};
	}
	
}
