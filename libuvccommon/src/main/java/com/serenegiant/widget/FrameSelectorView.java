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
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.serenegiant.common.R;

import java.util.Locale;


public class FrameSelectorView extends LinearLayout {
//	private static final boolean DEBUG = false;	// 実同時はfalseにすること
	private static final String TAG = FrameSelectorView.class.getSimpleName();

	public interface FrameSelectorViewCallback {
		/**
		 * フレームを選択した時の処理
		 * @param view
		 * @param frame_type フレームの種類, FrameView#FRAME_TYPE_XXX
		 */
		public void onFrameSelected(final FrameSelectorView view, final int frame_type);
		/**
		 * 色を選択した時の処理
		 * @param view
		 * @param index	-1ならcolorは無効、色選択ダイアログを表示する
		 * @param color
		 */
		public void onColorSelected(final FrameSelectorView view, final int index, final int color);

		/**
		 * 目盛りの種類を選択した時の処理
		 * @param view
		 * @param scale_type
		 */
		public void onScaleSelected(final FrameSelectorView view, final int scale_type);

		/**
		 * スケールの線幅変更時の処理
		 * @param view
		 * @param line_width
		 */
		public void onLineWidthChanged(final FrameSelectorView view, final float line_width);

		/**
		 * スケールの線幅選択時の処理
		 * @param view
		 * @param line_width
		 */
		public void onLineWidthSelected(final FrameSelectorView view, final float line_width);
	}

	/** アイコンImageButtonのIDと数字の対応用 */
	private static final SparseIntArray sBUTTONS = new SparseIntArray();
	static {
		sBUTTONS.put(R.id.color1_button, 0);
		sBUTTONS.put(R.id.color2_button, 1);
		sBUTTONS.put(R.id.color3_button, 2);
		sBUTTONS.put(R.id.color4_button, 3);
		sBUTTONS.put(R.id.color5_button, 4);
		sBUTTONS.put(R.id.color6_button, 5);
		sBUTTONS.put(R.id.color7_button, 6);
		sBUTTONS.put(R.id.color8_button, 7);
		sBUTTONS.put(R.id.color_select_button, -1);
		//
		sBUTTONS.put(R.id.frame_frame_button, FrameView.FRAME_TYPE_FRAME);
		sBUTTONS.put(R.id.frame_cross_button, FrameView.FRAME_TYPE_CROSS_FULL);
		sBUTTONS.put(R.id.frame_cross_quarter_button, FrameView.FRAME_TYPE_CROSS_QUARTER);
		sBUTTONS.put(R.id.frame_circle_button, FrameView.FRAME_TYPE_CIRCLE);
		sBUTTONS.put(R.id.frame_circle2_button, FrameView.FRAME_TYPE_CIRCLE_2);
		sBUTTONS.put(R.id.frame_cross_circle_button, FrameView.FRAME_TYPE_CROSS_CIRCLE);
		sBUTTONS.put(R.id.frame_cross_circle2_button, FrameView.FRAME_TYPE_CROSS_CIRCLE2);
	}

	private static final int[] COLOR_BTN_IDS = {
		R.id.color1_button,
		R.id.color2_button,
		R.id.color3_button,
		R.id.color4_button,
		R.id.color5_button,
		R.id.color6_button,
		R.id.color7_button,
		R.id.color8_button,
	};
	/** 色配列 */
	private final int[] mColors = {
		0xffff0000,
		0xffffa500,
		0xffffff00,
		0xff008000,
		0xff0000ff,
		0xffffffff,
		0xffb1b1b1,
		0xff000000,
	};

	private FrameSelectorViewCallback mCallback;
	private final ImageButton[] mFrameButtons = new ImageButton[FrameView.FRAME_TYPE_NUMS - 1];
	private RadioGroup mScaleTypeRadioGroup;
	private TextView mLineWidthTv;
	private SeekBar mSeekBar;

//	private final int SCALE_TYPE_INCH_RADIO;
//	private final int SCALE_TYPE_MM_RADIO;
	public FrameSelectorView(final Context context) {
		this(context, null, 0);
	}

	public FrameSelectorView(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public FrameSelectorView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		setOrientation(LinearLayout.VERTICAL);

//		SCALE_TYPE_INCH_RADIO = R.id.scale_type_inch_radiobutton;
//		SCALE_TYPE_MM_RADIO = R.id.scale_type_mm_radiobutton;
		final LayoutInflater inflater = LayoutInflater.from(context);

		try {
			final View rootView = inflater.inflate(R.layout.view_frame_selector, this, true);
			initView(rootView);
		} catch (final Exception e) {
			//
		}
	}

	public void setCallback(final FrameSelectorViewCallback callback) {
		mCallback = callback;
	}

	public FrameSelectorViewCallback getCallback() {
		return mCallback;
	}

	/**
	 * フレーム種を選択する…今は何も起こらんと思う, 選択したフレーム種のImageButtonを#setSelected(true)にするだけ
	 * @param frame_type
	 */
	public void setFrameType(final int frame_type) {
		if ((frame_type > 0) && (frame_type < FrameView.FRAME_TYPE_NUMS)) {
			for (int i = 0; i < FrameView.FRAME_TYPE_NUMS - 1; i++) {
				mFrameButtons[i].setSelected(false);
			}
			mFrameButtons[frame_type-1].setSelected(true);
		}
	}

	/**
	 * スケール種を選択する
	 * @param scale_type
	 */
	public void setScaleType(final int scale_type) {
		if (mScaleTypeRadioGroup != null) {
			switch (scale_type) {
			case FrameView.SCALE_TYPE_INCH:
				mScaleTypeRadioGroup.check(R.id.scale_type_inch_radiobutton);
				break;
			case FrameView.SCALE_TYPE_MM:
				mScaleTypeRadioGroup.check(R.id.scale_type_mm_radiobutton);
				break;
			default:
				mScaleTypeRadioGroup.check(R.id.scale_type_non_radiobutton);
				break;
			}
		}
	}

	/**
	 * スケールのライン幅を設定する
	 * @param width
	 */
	public void setLineWidth(final float width) {
		if (mSeekBar != null) {
			mSeekBar.setProgress((int)(width * 10));
		}
	}

	/**
	 * 色配列をセットする
	 * @param colors 8個以上必要
	 */
	public void setColors(final int[] colors) {
		if ((colors != null) && (colors.length >= 8)) {
			System.arraycopy(mColors, 0, colors, 0, 8);
			updateColors(colors);
		}
	}

	/**
	 * 現在の色配列を取得する
	 * @return
	 */
	public int[] getColors() {
		return mColors;
	}

	/**
	 * 色配列に指定した色を追加する。先頭は破棄される
	 * @param color
	 */
	public void addColor(final int color) {
		int ix = 0;
		for (int i = 0; i < 8; i++) {
			if (mColors[i] == color) {
				ix = i;
				break;
			}
		}
//		for (int i = ix; i < 7; i++) {
//			mColors[i] = mColors[i + 1];
//		}
		System.arraycopy(mColors, ix, mColors, ix + 1, 7);
		mColors[7] = color;
		updateColors(mColors);
	}

	/**
	 * 色選択アイコンの色を更新する
	 * @param colors
	 */
	private void updateColors(final int[] colors) {
		post(new Runnable() {
			@Override
			public void run() {
				if ((colors != null) && (colors.length >= 8)) {
					for (int i = 0; i < 8; i++) {
						final int id = COLOR_BTN_IDS[i];
						final ImageButton button = findViewById(id);
						if (button != null) {
							button.setBackgroundColor(colors[i]);
						}
					}
				}
			}
		});
	}

	/**
	 * Viewを初期化する
	 * @param rootView
	 */
	private void initView(final View rootView) {
		// フレーム選択ボタン
		ImageButton button;
		mFrameButtons[0] = rootView.findViewById(R.id.frame_frame_button);
		mFrameButtons[0].setOnClickListener(mOnFrameClickListener);
		mFrameButtons[1] = rootView.findViewById(R.id.frame_cross_button);
		mFrameButtons[1].setOnClickListener(mOnFrameClickListener);
		mFrameButtons[2] = rootView.findViewById(R.id.frame_cross_quarter_button);
		mFrameButtons[2].setOnClickListener(mOnFrameClickListener);
		mFrameButtons[3] = rootView.findViewById(R.id.frame_circle_button);
		mFrameButtons[3].setOnClickListener(mOnFrameClickListener);
		mFrameButtons[4] = rootView.findViewById(R.id.frame_circle2_button);
		mFrameButtons[4].setOnClickListener(mOnFrameClickListener);
		mFrameButtons[5] = rootView.findViewById(R.id.frame_cross_circle_button);
		mFrameButtons[5].setOnClickListener(mOnFrameClickListener);
		mFrameButtons[6] = rootView.findViewById(R.id.frame_cross_circle2_button);
		mFrameButtons[6].setOnClickListener(mOnFrameClickListener);
		// 色選択ボタン
		button = rootView.findViewById(R.id.color1_button);
		button.setOnClickListener(mOnColorClickListener);
		button = rootView.findViewById(R.id.color2_button);
		button.setOnClickListener(mOnColorClickListener);
		button = rootView.findViewById(R.id.color3_button);
		button.setOnClickListener(mOnColorClickListener);
		button = rootView.findViewById(R.id.color4_button);
		button.setOnClickListener(mOnColorClickListener);
		button = rootView.findViewById(R.id.color5_button);
		button.setOnClickListener(mOnColorClickListener);
		button = rootView.findViewById(R.id.color6_button);
		button.setOnClickListener(mOnColorClickListener);
		button = rootView.findViewById(R.id.color7_button);
		button.setOnClickListener(mOnColorClickListener);
		button = rootView.findViewById(R.id.color8_button);
		button.setOnClickListener(mOnColorClickListener);
		button = rootView.findViewById(R.id.color_select_button);
		button.setOnClickListener(mOnColorClickListener);
		// スケール選択ラジオボタン
		mScaleTypeRadioGroup = rootView.findViewById(R.id.scale_type_radiogroup);
		mScaleTypeRadioGroup.setOnCheckedChangeListener(mOnCheckedChangeListener);
		// ライン幅
		mLineWidthTv = rootView.findViewById(R.id.line_width_textview);
		mSeekBar = rootView.findViewById(R.id.line_width_seekbar);
		mSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
	}

	/**
	 * フレームアイコンをクリックした時の処理
	 */
	private final OnClickListener mOnFrameClickListener = new OnClickListener() {
		@Override
		public void onClick(final View view) {
			if (mCallback != null) {
				try {
					mCallback.onFrameSelected(FrameSelectorView.this, sBUTTONS.get(view.getId()));
				} catch (final Exception e) {
					Log.w(TAG, e);
				}
			}
		}
	};

	/**
	 * 色選択アイコンをクリックした時の処理
	 */
	private final OnClickListener mOnColorClickListener = new OnClickListener() {
		@Override
		public void onClick(final View view) {
			if (mCallback != null) {
				try {
					final int ix = sBUTTONS.get(view.getId());
					if ((ix >= 0) && (ix < 8)) {
						mCallback.onColorSelected(FrameSelectorView.this, ix, mColors[ix]);
					} else {
						mCallback.onColorSelected(FrameSelectorView.this, -1, 0);
					}
				} catch (final Exception e) {
					Log.w(TAG, e);
				}
			}
		}
	};

	private final RadioGroup.OnCheckedChangeListener mOnCheckedChangeListener
		= new RadioGroup.OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(final RadioGroup group, final int checkedId) {
			int scale_type = FrameView.SCALE_TYPE_NONE;
			if (checkedId == R.id.scale_type_inch_radiobutton) {
				scale_type = FrameView.SCALE_TYPE_INCH;
			} else if (checkedId == R.id.scale_type_mm_radiobutton) {
				scale_type = FrameView.SCALE_TYPE_MM;
			}
			if (mCallback != null) {
				try {
					mCallback.onScaleSelected(FrameSelectorView.this, scale_type);
				} catch (final Exception e) {
					Log.w(TAG, e);
				}
			}
		}
	};

	private final SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
		@Override
		public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
			if (mLineWidthTv != null) {
				mLineWidthTv.setText(String.format(Locale.US, "%4.1fpx", progress / 10.0f));
			}
			if (fromUser && (mCallback != null)) {
				try {
					mCallback.onLineWidthChanged(FrameSelectorView.this, seekBar.getProgress() / 10.0f);
				} catch (final Exception e) {
					Log.w(TAG, e);
				}
			}
		}

		@Override
		public void onStartTrackingTouch(final SeekBar seekBar) {
		}

		@Override
		public void onStopTrackingTouch(final SeekBar seekBar) {
			if (mCallback != null) {
				try {
					mCallback.onLineWidthSelected(FrameSelectorView.this, seekBar.getProgress() / 10.0f);
				} catch (final Exception e) {
					Log.w(TAG, e);
				}
			}
		}
	};
}
