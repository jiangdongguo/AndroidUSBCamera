/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright (C) 2011 The yanzm Custom View Project
 *      Yuki Anzai, uPhyca Inc.
 *      http://www.uphyca.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.serenegiant.widget;

import java.util.Locale;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.NumberKeyListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.serenegiant.common.R;

import androidx.annotation.NonNull;

/**
 * A view for selecting a number or string-array
 */
public final class ItemPicker extends LinearLayout {

    /**
     * The callback interface used to indicate the item value has been adjusted.
     */
	public interface OnChangedListener {
        /**
         * @param picker The NumberPicker associated with this listener.
         * @param oldVal The previous value.
         * @param newVal The new value.
         */
        void onChanged(ItemPicker picker, int oldVal, int newVal);
    }

    /**
     * Interface used to format the item into a string for presentation
     */
    public interface Formatter {
        String toString(int value);
    }

    private final Handler mHandler = new Handler();
    private final Runnable mRunnable = new Runnable() {
        @Override
		public void run() {
            if (mIncrement) {
                changeCurrent(mCurrentValue + 1);
                mHandler.postDelayed(this, mSpeed);
            } else if (mDecrement) {
                changeCurrent(mCurrentValue - 1);
                mHandler.postDelayed(this, mSpeed);
            }
        }
    };

    private final EditText mText;
    private final InputFilter mNumberInputFilter;

    private String[] mDisplayedValues;

    /**
     * Lower value of the range of numbers allowed for the ItemPicker
     */
    private int mMinValue;

    /**
     * Upper value of the range of numbers allowed for the ItemPicker
     */
    private int mMaxValue;

    /**
     * Current value of this ItemPicker
     */
    private int mCurrentValue;

    /**
     * Previous value of this ItemPicker.
     */
    private int mPrevValue;

    private OnChangedListener mListener;
    private Formatter mFormatter;

    private long mSpeed = 300;
    private boolean mIncrement;
    private boolean mDecrement;

    /**
     * Create a new item picker
     * @param context the application environment
     */
    public ItemPicker(final Context context) {
        this(context, null);
    }

    public ItemPicker(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	/**
     * Create a new item picker
     * @param context the application environment
     * @param attrs a collection of attributes
     * @param defStyle
     */
	public ItemPicker(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);

        final LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.item_picker, this, true);

        TypedArray a = context.obtainStyledAttributes(
        		attrs, R.styleable.ItemPicker, defStyle, 0);

        final int minValue = a.getInt(R.styleable.ItemPicker_ItemPickerMinItemValue, -1);
        final int maxValue = a.getInt(R.styleable.ItemPicker_ItemPickerMaxItemValue, -1);
        final int displayedValueId = a.getResourceId(R.styleable.ItemPicker_ItemPickerDisplayedValue, -1);
        final String[] displayedValue = (displayedValueId > -1) ?
        		getResources().getStringArray(displayedValueId) : null;

        final int incrementBackground = a.getResourceId(R.styleable.ItemPicker_ItemPickerIncrementBackground, -1);
        final int decrementBackground = a.getResourceId(R.styleable.ItemPicker_ItemPickerDecrementBackground, -1);
        final int incrementSrc = a.getResourceId(R.styleable.ItemPicker_ItemPickerIncrementSrc, -1);
        final int decrementSrc = a.getResourceId(R.styleable.ItemPicker_ItemPickerDecrementSrc, -1);
        final int editTextBackground = a.getResourceId(R.styleable.ItemPicker_ItemPickerEditTextBackground, -1);
        final int currentValue = a.getInt(R.styleable.ItemPicker_ItemPickerCurrentItemValue, -1);
        final int speed = a.getInt(R.styleable.ItemPicker_ItemPickerSpeed, -1);
        a.recycle();

        final OnClickListener clickListener = new OnClickListener() {
        	@Override
            public void onClick(final View v) {
                validateInput(mText);
                if (!mText.hasFocus()) mText.requestFocus();

                // now perform the increment/decrement
                if (R.id.increment == v.getId()) {
                    changeCurrent(mCurrentValue + 1);
                } else if (R.id.decrement == v.getId()) {
                    changeCurrent(mCurrentValue - 1);
                }
            }
        };

        final OnFocusChangeListener focusListener = new OnFocusChangeListener() {
        	@Override
            public void onFocusChange(final View v, final boolean hasFocus) {

                /* When focus is lost check that the text field
                 * has valid values.
                 */
                if (!hasFocus) {
                    validateInput(v);
                }
            }
        };

        final OnLongClickListener longClickListener = new OnLongClickListener() {
            /**
             * We start the long click here but rely on the {@link ItemPickerButton}
             * to inform us when the long click has ended.
             */
        	@Override
            public boolean onLongClick(final View v) {
                /* The text view may still have focus so clear it's focus which will
                 * trigger the on focus changed and any typed values to be pulled.
                 */
                mText.clearFocus();

                if (R.id.increment == v.getId()) {
                    mIncrement = true;
                    mHandler.post(mRunnable);
                } else if (R.id.decrement == v.getId()) {
                    mDecrement = true;
                    mHandler.post(mRunnable);
                }
                return true;
            }
        };

        final InputFilter inputFilter = new NumberPickerInputFilter();
        mNumberInputFilter = new NumberRangeKeyListener();
        mIncrementButton = findViewById(R.id.increment);
        mIncrementButton.setOnClickListener(clickListener);
        mIncrementButton.setOnLongClickListener(longClickListener);
        mIncrementButton.setNumberPicker(this);
        if (incrementBackground != -1)
        	mIncrementButton.setBackgroundResource(incrementBackground);
        if (incrementSrc != -1)
        	mIncrementButton.setImageResource(incrementSrc);

        mDecrementButton = findViewById(R.id.decrement);
        mDecrementButton.setOnClickListener(clickListener);
        mDecrementButton.setOnLongClickListener(longClickListener);
        mDecrementButton.setNumberPicker(this);
        if (decrementBackground != -1)
        	mDecrementButton.setBackgroundResource(decrementBackground);
        if (decrementSrc != -1)
        	mDecrementButton.setImageResource(decrementSrc);

        mText = findViewById(R.id.input);
        mText.setOnFocusChangeListener(focusListener);
        mText.setFilters(new InputFilter[] {inputFilter});
        mText.setRawInputType(InputType.TYPE_CLASS_NUMBER);
        if (editTextBackground != -1)
        	mText.setBackgroundResource(editTextBackground);

        if (!isEnabled()) {
            setEnabled(false);
        }

        if (minValue > -1 && maxValue > -1) {
        	if(displayedValue != null) {
        		setRange(minValue, maxValue, displayedValue);
        	}
        	else {
        		setRange(minValue, maxValue);
        	}
        }

        if (currentValue > -1)
        	setValue(currentValue);

        if (speed > -1)
        	setSpeed(speed);
    }

    /**
     * Set the enabled state of this view. The interpretation of the enabled
     * state varies by subclass.
     *
     * @param enabled True if this view is enabled, false otherwise.
     */
    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        mIncrementButton.setEnabled(enabled);
        mDecrementButton.setEnabled(enabled);
        mText.setEnabled(enabled);
    }


    @Override
	public void setOnKeyListener(final OnKeyListener listener) {
		super.setOnKeyListener(listener);
		mIncrementButton.setOnKeyListener(listener);
		mDecrementButton.setOnKeyListener(listener);
		mText.setOnKeyListener(listener);
	}

	/**
     * Set the callback that indicates the item has been adjusted by the user.
     * @param listener the callback, should not be null.
     */
    public void setOnChangeListener(final OnChangedListener listener) {
        mListener = listener;
    }

    /**
     * Set the formatter that will be used to format the item for presentation
     * @param formatter the formatter object.  If formatter is null, String.valueOf()
     * will be used
     */
    public void setFormatter(final Formatter formatter) {
        mFormatter = formatter;
    }

    /**
     * Set the range of items allowed for the item picker. The current
     * value will be automatically set to the start.
     *
     * @param min the start of the range (inclusive)
     * @param max the end of the range (inclusive)
     */
    public void setRange(final int min, final int max) {
        setRange(min, max, null/*displayedValues*/);
    }

    /**
     * Set the range of items allowed for the item picker. The current
     * value will be automatically set to the start. Also provide a mapping
     * for values used to display to the user.
     *
     * @param min the start of the range (inclusive)
     * @param max the end of the range (inclusive)
     * @param displayedValues the values displayed to the user.
     */
    public void setRange(final int min, final int max, final String[] displayedValues) {
        mDisplayedValues = displayedValues;
        mMinValue = min;
        mMaxValue = max;
        if ((mCurrentValue < min) || (mCurrentValue > max))
        	mCurrentValue = min;
        updateView();

        if (displayedValues != null) {
            // Allow text entry rather than strictly numeric entry.
            mText.setRawInputType(InputType.TYPE_CLASS_TEXT |
                    InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        }
    }

    /**
     * Set the current value for the item picker.
     *
     * @param value the current value the start of the range (inclusive)
     * @throws IllegalArgumentException when current is not within the range
     *         of of the item picker
     */
    public void setValue(int value) {
        if (value < mMinValue || value > mMaxValue) {
/*            throw new IllegalArgumentException(
            	String.format("current(%d) should be >= start(%d) and <= end(%d)",
            		value, mMinValue, mMaxValue)); */
        	Log.w("ItemPicker", String.format("current(%d) should be between min(%d) to max(%d) changed to min",
            		value, mMinValue, mMaxValue));
        	value = mMinValue;
        }
        mCurrentValue = value;
        updateView();
    }

    /**
     * Sets the speed at which the numbers will scroll when the +/-
     * buttons are longpressed
     *
     * @param speed The speed (in milliseconds) at which the numbers will scroll
     * default 300ms
     */
    public void setSpeed(final long speed) {
        mSpeed = speed;
    }

    private String formatNumber(final int value) {
        return (mFormatter != null)
                ? mFormatter.toString(value)
                : String.valueOf(value);
    }

    /**
     * Sets the current value of this ItemPicker, and sets mPrevious to the previous
     * value.  If current is greater than mEnd less than mStart, the value of mCurrent
     * is wrapped around.
     *
     * Subclasses can override this to change the wrapping behavior
     *
     * @param current the new value of the ItemPicker
     */
    protected void changeCurrent(int current) {
        // Wrap around the values if we go past the start or end
        if (current > mMaxValue) {
            current = mMinValue;
        } else if (current < mMinValue) {
            current = mMaxValue;
        }
        mPrevValue = mCurrentValue;
        mCurrentValue = current;
        notifyChange();
        updateView();
    }

    /**
     * Notifies the listener, if registered, of a change of the value of this
     * ItemPicker.
     */
    private void notifyChange() {
        if (mListener != null) {
            mListener.onChanged(this, mPrevValue, mCurrentValue);
        }
    }

    /**
     * Updates the view of this ItemPicker.  If displayValues were specified
     * in {@link #setRange}, the string corresponding to the index specified by
     * the current value will be returned.  Otherwise, the formatter specified
     * in setFormatter will be used to format the number.
     */
    private void updateView() {
        /* If we don't have displayed values then use the
         * current number else find the correct value in the
         * displayed values for the current number.
         */
        if (mDisplayedValues == null) {
            mText.setText(formatNumber(mCurrentValue));
        } else {
            mText.setText(mDisplayedValues[mCurrentValue - mMinValue]);
        }
        mText.setSelection(mText.getText().length());
    }

    private void validateCurrentView(final CharSequence str) {
        final int val = getSelectedPos(str.toString());
        if ((val >= mMinValue) && (val <= mMaxValue)) {
            if (mCurrentValue != val) {
                mPrevValue = mCurrentValue;
                mCurrentValue = val;
                notifyChange();
            }
        }
        updateView();
    }

    private void validateInput(final View v) {
        final String str = String.valueOf(((TextView) v).getText());
        if (TextUtils.isEmpty(str)) {

            // Restore to the old value as we don't allow empty values
            updateView();
        } else {

            // Check the new value and ensure it's in range
            validateCurrentView(str);
        }
    }

    /**
     * @hide
     */
    public void cancelIncrement() {
        mIncrement = false;
    }

    /**
     * @hide
     */
    public void cancelDecrement() {
        mDecrement = false;
    }

    private static final char[] DIGIT_CHARACTERS = new char[] {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
    };

    private ItemPickerButton mIncrementButton;
    private ItemPickerButton mDecrementButton;

    private class NumberPickerInputFilter implements InputFilter {
        @Override
		public CharSequence filter(final CharSequence source, final int start, final int end,
                final Spanned dest, final int dstart, final int dend) {
            if (mDisplayedValues == null) {
                return mNumberInputFilter.filter(source, start, end, dest, dstart, dend);
            }
            final CharSequence filtered = String.valueOf(source.subSequence(start, end));
            final String result = String.valueOf(dest.subSequence(0, dstart))
                    + filtered
                    + dest.subSequence(dend, dest.length());
            final String str = String.valueOf(result).toLowerCase(Locale.US);
            for (String val : mDisplayedValues) {
                val = val.toLowerCase(Locale.US);
                if (val.startsWith(str)) {
                    return filtered;
                }
            }
            return "";
        }
    }

    private class NumberRangeKeyListener extends NumberKeyListener {

        // XXX This doesn't allow for range limits when controlled by a
        // soft input method!
        @Override
		public int getInputType() {
            return InputType.TYPE_CLASS_NUMBER;
        }

        @NonNull
        @Override
        protected char[] getAcceptedChars() {
            return DIGIT_CHARACTERS;
        }

        @Override
        public CharSequence filter(final CharSequence source, final int start, final int end,
                final Spanned dest, final int dstart, final int dend) {

            CharSequence filtered = super.filter(source, start, end, dest, dstart, dend);
            if (filtered == null) {
                filtered = source.subSequence(start, end);
            }

            final String result = String.valueOf(dest.subSequence(0, dstart))
                    + filtered
                    + dest.subSequence(dend, dest.length());

            if ("".equals(result)) {
                return result;
            }
            final int val = getSelectedPos(result);

            /* Ensure the user can't type in a value greater
             * than the max allowed. We have to allow less than min
             * as the user might want to delete some numbers
             * and then type a new number.
             */
            if (val > mMaxValue) {
                return "";
            } else {
                return filtered;
            }
        }
    }

    private int getSelectedPos(String str) {
        if (mDisplayedValues == null) {
            try {
                return Integer.parseInt(str);
            } catch (final NumberFormatException e) {
                /* Ignore as if it's not a number we don't care */
            }
        } else {
            for (int i = 0; i < mDisplayedValues.length; i++) {
                /* Don't force the user to type in jan when ja will do */
                str = str.toLowerCase(Locale.US);
                if (mDisplayedValues[i].toLowerCase(Locale.US).startsWith(str)) {
                    return mMinValue + i;
                }
            }

            /* The user might have typed in a number into the month field i.e.
             * 10 instead of OCT so support that too.
             */
            try {
                return Integer.parseInt(str);
            } catch (final NumberFormatException e) {

                /* Ignore as if it's not a number we don't care */
            }
        }
        return mMinValue;
    }

    /**
     * Returns the current value of the ItemPicker
     * @return the current value.
     */
    public int getValue() {
        return mCurrentValue;
    }

    /**
     * Returns the upper value of the range of the ItemPicker
     * @return the uppper number of the range.
     */
    protected int getEndRange() {
        return mMaxValue;
    }

    /**
     * Returns the lower value of the range of the ItemPicker
     * @return the lower number of the range.
     */
    protected int getBeginRange() {
        return mMinValue;
    }
}
