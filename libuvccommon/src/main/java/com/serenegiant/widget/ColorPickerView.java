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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.serenegiant.graphics.BitmapHelper;
import com.serenegiant.graphics.ShaderDrawable;

/**
 * 色相円とアルファ・明度から色を選択するためのView
 */
public class ColorPickerView extends View {
//	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
//	private static final String TAG = "ColorPickerView";

	private static final float SQRT2 = (float)Math.sqrt(2);
	private static final float PI = (float)Math.PI;
	private static final float	BORDER_WIDTH_PX = 1;
	private static final int DEFAULT_WIDTH = 100;
	private static final int DEFAULT_HEIGHT = 100;
	private static final float RECTANGLE_TRACKER_OFFSET_DP = 2f;
	private static final int BORDER_COLOR = 0xff6E6E6E;
	private static final int TRACKER_COLOR = 0xff1c1c1c;
	/**
	 * 選択色の位置表示用円の半径初期値
	 */
	private static final int DEFAULT_SELECTED_RADIUS = 8;

	private static final int STATE_IDLE = 0;
	private static final int STATE_COLOR = 1;
	private static final int STATE_ALPHA = 2;
	private static final int STATE_VAL = 3;

	/**
	 * アルファ値変更スライダーを表示するかどうか
	 */
	private boolean mShowAlphaSlider = true;
	/**
	 * 明度スライダーを表示するかどうか
	 */
	private boolean mShowValSlider = true;

	/**
	 * 選択中の色を左上1/4円で表示するかどうか
	 */
	private boolean mShowSelectedColor = true;

	private final float RECTANGLE_TRACKER_OFFSET;

	private final float DENSITY;
	private final int[] COLORS = new int[360];	// 色相用の一次配列
	private final float[] HSV = new float[3];	// HSV変換用のワーク
	private final RectF mDrawingRect = new RectF();
	private final Paint mBorderPaint = new Paint();
	private final Paint mTrackerPaint = new Paint();

	private final ShaderDrawable mAlphaDrawable;
	private final Shader mAlphaShader;
	private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint mGradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint mSelectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final RectF mSelectionRect = new RectF();
	private final PointF mSelected = new PointF();
	private final RectF mSliderRect = new RectF();

	private final Paint mAlphaPaint = new Paint();
	private final RectF mAlphaRect = new RectF();

	private final Paint mValPaint = new Paint();
	private final RectF mValRect = new RectF();

	private int mState = STATE_IDLE;
	private ColorPickerListener mColorPickerListener;
	/**
	 * 右端(明度)・下端(アルファ)に表示するパネル幅
	 */
	private int slider_width = 32;
	/**
	 * 色相円の中央座標
	 */
	private int center_x, center_y;
	/**
	 * 色相円内で現在の色を表す円の半径
	 */
	private final float SELECTED_RADIUS;
	/**
	 * 色相円の外周半径
	 */
	private int radius;
	/**
	 * 色相円の外周半径の2乗(タッチ位置検出用)
	 */
	private int radius2;
	/**
	 * 選択中の色ARGB
	 */
	private int mColor = 0xffffffff;
	private int mAlpha = 0xff;
	private float mVal = 0.0f;
	private float mHue = 360.0f;
	private float mSat = 0.0f;

	public interface ColorPickerListener {
		public void onColorChanged(ColorPickerView view, int color);
	}

	public ColorPickerView(final Context context) {
		this(context, null, 0);
	}

	public ColorPickerView(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ColorPickerView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
		super(context, attrs, defStyleAttr);
//		if (DEBUG) Log.v(TAG, "ColorPickerView:");
		DENSITY = context.getResources().getDisplayMetrics().density;
		RECTANGLE_TRACKER_OFFSET = RECTANGLE_TRACKER_OFFSET_DP * DENSITY;
		SELECTED_RADIUS = DEFAULT_SELECTED_RADIUS * DENSITY;

		mAlphaShader = new BitmapShader(BitmapHelper.makeCheckBitmap(), TileMode.REPEAT, TileMode.REPEAT);
		mAlphaDrawable = new ShaderDrawable(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG, 0);
		mAlphaDrawable.setShader(mAlphaShader);

		radius = 0;
		internalSetColor(mColor, false);

		// 色相円用
		setHueColorArray(mAlpha, COLORS);
		mPaint.setShader(new SweepGradient(0, 0, COLORS, null));
		mPaint.setStyle(Paint.Style.FILL);
		mPaint.setStrokeWidth(0);

		// 選択色用
		mSelectionPaint.setColor(mColor);
		mSelectionPaint.setStrokeWidth(5);

		// スライダーのトラッカー表示用
		mTrackerPaint.setColor(TRACKER_COLOR);
		mTrackerPaint.setStyle(Paint.Style.STROKE);
		mTrackerPaint.setStrokeWidth(2f * DENSITY);
		mTrackerPaint.setAntiAlias(true);
	}

	@Override
	protected void onDraw(final Canvas canvas) {
		mSelectionPaint.setStyle(Paint.Style.FILL);
		if (mShowSelectedColor) {
			// 透過した時の背景(白黒市松模様)を描画
			mSelectionPaint.setShader(mAlphaShader);
			canvas.drawArc(mSelectionRect, 0f, 90f, true, mSelectionPaint);
			// 選択中の色を表示(左上1/4円)
			mSelectionPaint.setShader(null);
			mSelectionPaint.setColor(mColor);
			canvas.drawArc(mSelectionRect, 0f, 90f, true, mSelectionPaint);
		}

		final int count = canvas.save();
		try {
			canvas.translate(center_x, center_y);
			mSelectionPaint.setShader(mAlphaShader);
			canvas.drawCircle(0, 0, radius, mSelectionPaint);	// 色相円の背景に白黒市松模様を描画
			canvas.drawCircle(0, 0, radius, mPaint);			// 色相円
			canvas.drawCircle(0, 0, radius, mGradientPaint);	// 色相円の前景(satを反映させるために中心に向かって白くする)
		} finally {
			canvas.restoreToCount(count);
		}
		// 選択している位置
		mSelectionPaint.setShader(null);
		mSelectionPaint.setColor(~mColor | 0xff000000);	// 色を反転、アルファは常に0xffにする
		mSelectionPaint.setStyle(Paint.Style.STROKE);
		canvas.drawCircle(mSelected.x, mSelected.y, SELECTED_RADIUS, mSelectionPaint);
		//
		drawAlphaPanel(canvas);	// アルファ値スライダー(下端)
		drawValPanel(canvas);	// 明度スライダー(右端)
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width = MeasureSpec.getSize(widthMeasureSpec);
		int height = MeasureSpec.getSize(heightMeasureSpec);
		final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

		switch (widthMode) {
		case MeasureSpec.UNSPECIFIED:
			// 好きな値を設定できる時
			width = DEFAULT_WIDTH;
			break;
		case MeasureSpec.EXACTLY:
			// この値にしないとダメな時
			break;
		case MeasureSpec.AT_MOST:
			// 最大の大きさが指定された時
			break;
		}
		widthMeasureSpec = MeasureSpec.makeMeasureSpec(width, widthMode);

		switch (heightMode) {
		case MeasureSpec.UNSPECIFIED:
			// 好きな値を設定できる時
			height = DEFAULT_HEIGHT;
			break;
		case MeasureSpec.EXACTLY:
			// この値にしないとダメな時
			break;
		case MeasureSpec.AT_MOST:
			// 最大の大きさが指定された時
			break;
		}
		heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, heightMode);
		setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	@SuppressLint("DrawAllocation")
	@Override
	protected void onLayout(final boolean changed, final int left, final int top, final int right, final int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		if (getWidth() != 0 && getHeight() != 0) {
//			if (DEBUG) Log.v(TAG, String.format("onLayout:(%d,%d)(%d,%d)", left, top, right, bottom));
			final int width = getWidth();
			final int height = getHeight();
			mDrawingRect.set(0, 0, width, height);
			// 右端(明度)と下端(アルファ)にスライダーを表示するスペースを確保
			int dimeter = Math.min(width, height);
			slider_width = dimeter / 10;	// 表示可能領域の10%
			if (slider_width < 32) slider_width = (int)(32 * DENSITY);	// でも32dpを最小値とする
			final int space = slider_width + (int)(16 * DENSITY);	// 更に16dpのスペースを開ける
			// 色相円の直径
			dimeter -= space;
			// 色相円の外周半径
			radius = dimeter >>> 1;
			radius2 = radius * radius;	// 2乗
			// 色相円の中央座標
			center_x = (width - (mShowValSlider ? slider_width : 0)) >>> 1;
			center_y = (height - (mShowAlphaSlider ? slider_width : 0)) >>> 1;
			// 選択色表示用の円の半径
			final int selection_radius = (int)(Math.sqrt(center_x * center_x + center_y * center_y)) - radius;
			mSelectionRect.set(-selection_radius, -selection_radius, selection_radius, selection_radius);
			// 色相円用のグラデーションを初期化
			mGradientPaint.setShader(new RadialGradient(0, 0, radius, 0xffffffff, 0x00ffffff, TileMode.CLAMP));
			// スライダーの初期化
			setupAlphaRect();
			setUpValRect();
			// 選択色を更新
			setColor(mAlpha, mHue, mSat, mVal, true);
		}
	}

	/**
	 * 現在の色を取得
	 * @return
	 */
	public int getColor() {
		return mColor;
	}

	/**
	 * 色相を取得
	 * @return
	 */
	public float getHue() {
    	return mHue;
    }

	/**
	 * 彩度を取得
	 * @return
	 */
    public float getSat() {
    	return mSat;
    }

    /**
     * 明度を取得
     * @return
     */
    public float getVal() {
    	return mVal;
    }

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(final MotionEvent event) {
		final float x = event.getX();
		final float y = event.getY();
		final float dx = x - center_x;
		final float dy = y - center_y;
		// Math.Sqrtは遅いので色相円内かどうかは中心からの距離の2乗値で比較する
		final float d = dx * dx + dy * dy;
		final boolean inColorCircle = (d <= radius2);

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			if (inColorCircle)
				mState = STATE_COLOR;
			else if (mAlphaRect.contains(x, y))
				mState = STATE_ALPHA;
			else if (mValRect.contains(x, y))
				mState = STATE_VAL;
			else
				return false;
			// ここはrun throughする
		case MotionEvent.ACTION_MOVE:
			boolean modified = false;
			switch (mState) {
			case STATE_COLOR:
				final float angle = (float) Math.atan2(dy, dx);
				// [-PI ... PI]を[0....1]に変換する
				float unit = angle / (2 * PI);
				if (unit < 0) {
					unit += 1;
				}
				// x+軸に対する角度[度]が色相, 中心からの距離をノーマライズした値が彩度とする
				setColor(mAlpha, 360.0f - unit * 360.0f, (float)Math.sqrt(d) / radius, mVal, true);
				modified = true;
				break;
			case STATE_ALPHA:
				// アルファ値変更スライダーにタッチした時の処理
				if (modified = trackAlpha(x, y)) {
					setHueColorArray(mAlpha, COLORS);
					mPaint.setShader(new SweepGradient(0, 0, COLORS, null));
				}
				break;
			case STATE_VAL:
				// 明度変更スライダーにタッチした時の処理
				if (modified = trackVal(x, y)) {
					setHueColorArray(mAlpha, COLORS);
					mPaint.setShader(new SweepGradient(0, 0, COLORS, null));
				}
				break;
			}
			if (modified) {
				if (mColorPickerListener != null)
					mColorPickerListener.onColorChanged(this, mColor);
			}
			break;
		case MotionEvent.ACTION_UP:
			if (mColorPickerListener != null) {
				mColorPickerListener.onColorChanged(this, mColor);
			}
			mState = STATE_IDLE;
			break;
		}
		return true;
	}

	public void setColor(final int cl) {
		internalSetColor(cl, true);
	}
	/**
	 * 指定したARGB値をセットする
	 * @param cl	ARGB値
	 * @param force	強制更新するかどうか
	 */
	protected void internalSetColor(final int cl, final boolean force) {
		final int alpha = Color.alpha(cl);
		final int red = Color.red(cl);
		final int blue = Color.blue(cl);
		final int green = Color.green(cl);

		Color.RGBToHSV(red, green, blue, HSV);

		setColor(alpha, HSV[0], HSV[1], HSV[2], force);
	}

	/**
	 * 指定したAHSV値をセットする
	 * @param alpha アルファ値
	 * @param hue	色相
	 * @param sat	彩度
	 * @param val	明度
	 * @param force	強制更新するかどうか
	 */
	protected void setColor(final int alpha, final float hue, float sat, final float val, final boolean force) {
		if (sat > 1.0f) sat = 1.0f;
		if (force || (mAlpha != alpha) || (mHue != hue)
			|| (mSat != sat) || (mVal != val)) {

			mAlpha = alpha;
			mHue = hue;
			mSat = sat;
			mVal = val;
			mColor = HSVToColor(alpha, hue, sat, val);

			if (radius > 0) {
				final float r = radius * sat;
				final float d = hue / 180.0f * PI;
				mSelected.set(center_x + r * (float)Math.cos(d), center_y - r * (float)Math.sin(d));
				postInvalidate();
			}
		}
	}

	public void setColorPickerListener(final ColorPickerListener listener) {
		mColorPickerListener = listener;
	}

	public ColorPickerListener getColorPickerListener() {
		return mColorPickerListener;
	}

	/**
	 * 選択中の色を左上に1/4円で表示するかどうかを設定
	 * @param showSelectedColor
	 */
	public void setShowSelectedColor(final boolean showSelectedColor) {
		mShowSelectedColor = showSelectedColor;
	}

	/**
	 * 選択中の色を左上に1/4円で表示するかどうかを取得
	 * @return
	 */
	public boolean getShowSelectedColor() {
		return mShowSelectedColor;
	}

	/**
	 * アルファ値変更用のスライダーを表示するかどうかを設定
	 * @param showAlpha
	 */
	public void showAlpha(final boolean showAlpha) {
		if (mShowAlphaSlider != showAlpha) {
			mShowAlphaSlider = showAlpha;
			postInvalidate();
		}
	}

	/**
	 * アルファ値変更用のスライダーを表示するかどうかを取得
	 * @return
	 */
	public boolean isShowAlpha() {
		return mShowAlphaSlider;
	}

	/**
	 * 明度変更用のスライダーを表示するかどうかを設定
	 * @param showVal
	 */
	public void setShowVal(final boolean showVal) {
		if (mShowValSlider != showVal) {
			mShowValSlider = showVal;
			postInvalidate();
		}
	}

	/**
	 * 明度変更用のスライダーを表示するかどうかを取得
	 * @return
	 */
	public boolean isShowVal() {
		return mShowValSlider;
	}

	/**
	 * AHSVをARGBに変換
	 * @param hue 色相 [0-360)
	 * @param saturation 彩度 [0-1]
	 * @param value 明度 [0-1]
	 * @return
	 */
	private final int HSVToColor(final int alpha, float hue, float saturation, float value) {
		if (hue >= 360)
			hue = 359.99f;
		else if (hue < 0)
			hue = 0;

		if (saturation > 1)
			saturation = 1;
		else if (saturation < 0)
			saturation = 0;

		if (value > 1)
			value = 1;
		else if (value < 0)
			value = 0;

		HSV[0] = hue;
		HSV[1] = saturation;
		HSV[2] = value;

		return Color.HSVToColor(alpha, HSV);
	}

	/**
	 * 色相円表示用に色相配列をセット
	 * @param alpha
	 * @param colors
	 * @return
	 */
	private final int[] setHueColorArray(final int alpha, final int[] colors) {
		final int n = colors.length;
		final float resolution = 360.0f / n;
		HSV[1] = 1.0f;	// sat
		HSV[2] = mVal;	// val
		int count = 0;
		for (float i = 360.0f; i >= 0.0; i -= resolution, count++) {
			if (count >= n) break;
			HSV[0] = i;
			colors[count] = Color.HSVToColor(alpha, HSV);
		}
		HSV[0] = 0;
		colors[n-1] = Color.HSVToColor(alpha, HSV);
		return colors;
	}

	/**
	 * 水平スライダー用のトラッキングバーを表示
	 * @param canvas
	 * @param x
	 * @param y
	 * @param height
	 */
	private final void drawTrackerHorizontal(final Canvas canvas, final float x, final float y, final float height) {
		final float width = 4 * DENSITY / 2;
		mSliderRect.left = x - width;
		mSliderRect.right = x + width;
		mSliderRect.top = y - RECTANGLE_TRACKER_OFFSET;
		mSliderRect.bottom = y + height + RECTANGLE_TRACKER_OFFSET;

		mTrackerPaint.setColor(~TRACKER_COLOR | 0xff000000);
		mTrackerPaint.setStyle(Paint.Style.FILL);
		canvas.drawRoundRect(mSliderRect, 2, 2, mTrackerPaint);
		mTrackerPaint.setColor(TRACKER_COLOR);
		mTrackerPaint.setStyle(Paint.Style.STROKE);
		canvas.drawRoundRect(mSliderRect, 2, 2, mTrackerPaint);
	}

	/**
	 * 垂直スライダー用のトラッキングバーを表示
	 * @param canvas
	 * @param x
	 * @param y
	 * @param width
	 */
	private final void drawTrackerVertical(final Canvas canvas, final float x, final float y, final float width) {
		final float height = 4 * DENSITY / 2;
		mSliderRect.left = x - RECTANGLE_TRACKER_OFFSET;
		mSliderRect.right = x + width + RECTANGLE_TRACKER_OFFSET;
		mSliderRect.top = y - height;
		mSliderRect.bottom = y + height;

		mTrackerPaint.setColor(~TRACKER_COLOR | 0xff000000);
		mTrackerPaint.setStyle(Paint.Style.FILL);
		canvas.drawRoundRect(mSliderRect, 2, 2, mTrackerPaint);
		mTrackerPaint.setColor(TRACKER_COLOR);
		mTrackerPaint.setStyle(Paint.Style.STROKE);
		canvas.drawRoundRect(mSliderRect, 2, 2, mTrackerPaint);
	}
	/**
	 * アルファ値変更用スライダー(下端)の準備
	 */
	private void setupAlphaRect() {
		if(!mShowAlphaSlider) return;

		final RectF	dRect = mDrawingRect;
		mAlphaRect.set(
			dRect.left + BORDER_WIDTH_PX,
			dRect.bottom - slider_width + BORDER_WIDTH_PX,
			dRect.right - slider_width - BORDER_WIDTH_PX,
			dRect.bottom - BORDER_WIDTH_PX - RECTANGLE_TRACKER_OFFSET);
	}
	/**
	 * アルファ値変更スライダーの描画処理
	 * @param canvas
	 */
	private final void drawAlphaPanel(final Canvas canvas) {
		if(!mShowAlphaSlider) return;

		final RectF rect = mAlphaRect;

		if (BORDER_WIDTH_PX > 0) {
			mBorderPaint.setColor(BORDER_COLOR);
			canvas.drawRect(
				rect.left - BORDER_WIDTH_PX,
				rect.top - BORDER_WIDTH_PX,
				rect.right + BORDER_WIDTH_PX,
				rect.bottom + BORDER_WIDTH_PX,
				mBorderPaint);
		}

		mAlphaPaint.setShader(mAlphaShader);
		canvas.drawRect(rect, mAlphaPaint);

		final int color = HSVToColor(0xff, mHue, mSat, mVal);
		final int acolor = HSVToColor(0, mHue, mSat, mVal);

		final Shader alphaShader = new LinearGradient(
			rect.left, rect.top, rect.right, rect.top, color, acolor, TileMode.CLAMP);

		mAlphaPaint.setShader(alphaShader);
		canvas.drawRect(rect, mAlphaPaint);

		final Point p = alphaToPoint(mAlpha);

		drawTrackerHorizontal(canvas, p.x, p.y, rect.height());
	}
	/**
	 * スライダーでアルファ値を変更
	 * @param x
	 * @param y
	 * @return
	 */
	private final boolean trackAlpha(final float x, final float y) {
		boolean result = false;
		if (mShowAlphaSlider) {
			final int alpha = pointToAlpha((int)x);
			if (mAlpha != alpha) {
				setColor(alpha, mHue, mSat, mVal, true);
				result = true;
			}
		}
		return result;
	}
	/**
	 * アルファ値を座標に変換する
	 * @param alpha
	 * @return
	 */
	private final Point alphaToPoint(final int alpha) {
		final RectF r = mAlphaRect;
		final float w = r.width();

		return new Point((int) (w - (alpha * w / 0xff) + r.left), (int)r.top);
	}
	/**
	 * 座標値をアルファ値に変換する
	 * @param x
	 * @return
	 */
	private final int pointToAlpha(int x) {

		final RectF rect = mAlphaRect;
		final int width = (int) rect.width();

		if (x < rect.left) {
			x = 0;
		} else if (x > rect.right) {
			x = width;
		} else {
			x = x - (int)rect.left;
		}

		return 0xff - (x * 0xff / width);
	}

	/**
	 * 明度(右端)スライダーの準備
	 */
	private final void setUpValRect(){
		if (!mShowValSlider) return;

		final RectF	dRect = mDrawingRect;
		mValRect.set(
			dRect.right - slider_width + BORDER_WIDTH_PX,
			dRect.top + BORDER_WIDTH_PX + RECTANGLE_TRACKER_OFFSET,
			dRect.right - BORDER_WIDTH_PX - RECTANGLE_TRACKER_OFFSET,
			dRect.bottom - BORDER_WIDTH_PX - (mShowAlphaSlider ? (16 + slider_width) : 0));
	}

	/**
	 * 明度スライダーを描画
	 * @param canvas
	 */
	private final void drawValPanel(final Canvas canvas) {
		if (!mShowValSlider) return;

		final RectF rect = mValRect;

		if (BORDER_WIDTH_PX > 0){
			mBorderPaint.setColor(BORDER_COLOR);
			canvas.drawRect(
				rect.left - BORDER_WIDTH_PX,
				rect.top - BORDER_WIDTH_PX,
				rect.right + BORDER_WIDTH_PX,
				rect.bottom + BORDER_WIDTH_PX,
				mBorderPaint);
		}

		final int color = HSVToColor(0xff, mHue, mSat, 1.0f);
		final int acolor = HSVToColor(0xff, mHue, mSat, 0.0f);

		final Shader mValShader = new LinearGradient(
			rect.left, rect.top, rect.left, rect.bottom, color, acolor, TileMode.CLAMP);
		mValPaint.setShader(mValShader);
		canvas.drawRect(rect, mValPaint);

		final Point p = valToPoint(mVal);
		drawTrackerVertical(canvas, p.x, p.y, rect.width());
	}

	private final boolean trackVal(final float x, final float y) {
		boolean result = false;
		final float val = pointToVal(y);
		if (mVal != val) {
			setColor(mAlpha, mHue, mSat, val, true);
			result = true;
		}
		return result;
	}

	/**
	 * 明度を座標に変換
	 * @param val
	 * @return
	 */
	private final Point valToPoint(final float val) {
		final RectF rect = mValRect;
		final float height = rect.height();
		final Point p = new Point();
		p.y = (int) (height - (val * height) + rect.top);
		p.x = (int) rect.left;

		return p;
	}
	/**
	 * 座標から明度へ変換
	 * @param y
	 * @return
	 */
	private final float pointToVal(float y) {
		final RectF rect = mValRect;

		final float height = rect.height();
		if (y < rect.top) {
			y = 0f;
		}
		else if (y > rect.bottom) {
			y = height;
		}
		else{
			y = y - rect.top;
		}

		return 1.0f - (y * 1.0f / height);
	}

}
