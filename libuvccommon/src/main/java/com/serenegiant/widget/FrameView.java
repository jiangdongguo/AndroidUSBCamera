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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

import com.serenegiant.common.R;

public class FrameView extends View {
//	private static final boolean DEBUG = false;	// FIXME 実同時はfalseにすること
	private static final String TAG = FrameView.class.getSimpleName();

	public static final float MAX_SCALE = 10.0f;
	public static final int FRAME_TYPE_NONE = 0;
	public static final int FRAME_TYPE_FRAME = 1;
	public static final int FRAME_TYPE_CROSS_FULL = 2;
	public static final int FRAME_TYPE_CROSS_QUARTER = 3;
	public static final int FRAME_TYPE_CIRCLE = 4;
	public static final int FRAME_TYPE_CROSS_CIRCLE = 5;
	public static final int FRAME_TYPE_CIRCLE_2 = 6;
	public static final int FRAME_TYPE_CROSS_CIRCLE2 = 7;
	public static final int FRAME_TYPE_NUMS = 8;

	public static final int SCALE_TYPE_NONE = 0;
	public static final int SCALE_TYPE_INCH = 1;
	public static final int SCALE_TYPE_MM = 2;
	public static final int SCALE_TYPE_NUMS = 3;

	private static final float DEFAULT_FRAME_WIDTH_DP = 3.0f;

	private final Paint mPaint = new Paint();
	private final RectF mBoundsRect = new RectF();
	private final DisplayMetrics metrics;
	private final float defaultFrameWidth;
	private int mFrameType, mFrameColor;
	private float mFrameWidth;
	private int mScaleType, mScaleColor, mTickColor;
	private float mScaleWidth;
	private float mRotation;
	private float mScale;
	private float mCenterX, mCenterY;
	private float mWidth, mHeight;
	private float mRadius, mRadius2, mRadius4, mRadiusQ;

	public FrameView(final Context context) {
		this(context, null, 0);
	}

	public FrameView(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public FrameView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		metrics = getContext().getResources().getDisplayMetrics();
		defaultFrameWidth = DEFAULT_FRAME_WIDTH_DP * metrics.density;
		TypedArray attribs = context.obtainStyledAttributes(attrs, R.styleable.FrameView, defStyleAttr, 0);
		mFrameType = attribs.getInt(R.styleable.FrameView_frame_type, FRAME_TYPE_NONE);
		mFrameWidth = attribs.getDimension(R.styleable.FrameView_frame_width, defaultFrameWidth);
		mFrameColor = attribs.getColor(R.styleable.FrameView_frame_color, 0xffb1b1b1);
		mScaleType = attribs.getInt(R.styleable.FrameView_scale_type, SCALE_TYPE_NONE);
		mScaleWidth = attribs.getDimension(R.styleable.FrameView_scale_width, mFrameWidth);
		mScaleColor = attribs.getColor(R.styleable.FrameView_scale_color, mFrameColor);
		mTickColor = attribs.getColor(R.styleable.FrameView_tick_color, mScaleColor);
		mRotation = attribs.getFloat(R.styleable.FrameView_scale_rotation, 0);
		mScale = attribs.getFloat(R.styleable.FrameView_scale_scale, 1.0f);
		attribs.recycle();
		mPaint.setStyle(Paint.Style.STROKE);
//		if (DEBUG) Log.v(TAG, "mFrameWidth=" + mFrameWidth);
	}

	@Override
	protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	@Override
	protected void onLayout(final boolean changed, final int left, final int top, final int right, final int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		// ここの引数の座標は親ViewGroupの座標系
//		if (DEBUG) Log.v(TAG, String.format("(%d,%d)-(%d,%d)", left, top, right, bottom));
		final float w2 = mFrameWidth / 2;
		mBoundsRect.set(getPaddingLeft() + w2, getPaddingTop() + w2,
			getWidth() - getPaddingRight() - w2, getHeight() - getPaddingBottom() - w2);
//		if (DEBUG) Log.v(TAG, "mBoundsRect=" + mBoundsRect);
		mCenterX = mBoundsRect.centerX();
		mCenterY = mBoundsRect.centerY();
		mWidth = mBoundsRect.width();
		mHeight = mBoundsRect.height();
		mRadius = Math.min(mWidth, mHeight) * 0.9f;
		mRadius2 = mRadius / 2.0f;
		mRadius4 = mRadius / 4.0f;
		mRadiusQ = (float)(mRadius4 / Math.sqrt(2));
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (mFrameType != FRAME_TYPE_NONE) {
			mPaint.setStrokeWidth(mFrameWidth);
			mPaint.setColor(mFrameColor);
			// 外周枠を描画
			canvas.drawRect(mBoundsRect, mPaint);
			mPaint.setStrokeWidth(mScaleWidth);
			mPaint.setColor(mScaleColor);
			final float centerX = mCenterX;
			final float centerY = mCenterY;
			final float r2 = mRadius2;
			final float r4 = mRadius4;
			final float rq = mRadiusQ;
			final int saveCount = canvas.save();
			try {
				canvas.rotate(mRotation, centerX, centerY);
				canvas.scale(mScale, mScale, centerX, centerY);
				switch (mFrameType) {
				case FRAME_TYPE_CROSS_FULL:
					switch (mScaleType) {
					case SCALE_TYPE_NONE:
						canvas.drawLine(centerX, mBoundsRect.top, centerX, mBoundsRect.bottom, mPaint);
						canvas.drawLine(mBoundsRect.left, centerY, mBoundsRect.right, centerY, mPaint);
						break;
					case SCALE_TYPE_INCH:
						draw_scale_full(canvas, mWidth, mHeight, metrics.xdpi / 10.0f, metrics.ydpi / 10.0f, 10);
						break;
					case SCALE_TYPE_MM:
						draw_scale_full(canvas, mWidth, mHeight, metrics.xdpi / 12.7f, metrics.ydpi / 12.7f, 5);
						break;
					}
					break;
				case FRAME_TYPE_CROSS_QUARTER:
					switch (mScaleType) {
					case SCALE_TYPE_NONE:
						canvas.drawLine(centerX, centerY - r4, centerX, centerY + r4, mPaint);
						canvas.drawLine(centerX - r4, centerY, centerX + r4, centerY, mPaint);
						break;
					case SCALE_TYPE_INCH:
						draw_scale_full(canvas, r2, r2, metrics.xdpi / 10.0f, metrics.ydpi / 10.0f, 10);
						break;
					case SCALE_TYPE_MM:
						draw_scale_full(canvas, r2, r2, metrics.xdpi / 12.7f, metrics.ydpi / 12.7f, 5);
						break;
					}
					break;
				case FRAME_TYPE_CIRCLE:
					canvas.drawCircle(mCenterX, centerY, r4, mPaint);
					break;
				case FRAME_TYPE_CROSS_CIRCLE:
					switch (mScaleType) {
					case SCALE_TYPE_NONE:
						canvas.drawLine(centerX, centerY - r4, centerX, centerY + r4, mPaint);
						canvas.drawLine(centerX - r4, centerY, centerX + r4, centerY, mPaint);
						break;
					case SCALE_TYPE_INCH:
						draw_scale_full(canvas, r2, r2, metrics.xdpi / 10.0f, metrics.ydpi / 10.0f, 10);
						break;
					case SCALE_TYPE_MM:
						draw_scale_full(canvas, r2, r2, metrics.xdpi / 12.7f, metrics.ydpi / 12.7f, 5);
						break;
					}
					canvas.drawCircle(centerX, centerY, r4, mPaint);
					break;
				case FRAME_TYPE_CIRCLE_2:
					canvas.drawCircle(centerX, centerY, r4 / 2, mPaint);
					canvas.drawCircle(centerX, centerY, r4, mPaint);
					break;
				case FRAME_TYPE_CROSS_CIRCLE2:
					switch (mScaleType) {
					case SCALE_TYPE_NONE:
						canvas.drawLine(centerX, centerY - r4, centerX, centerY + r4, mPaint);
						canvas.drawLine(centerX - r4, centerY, centerX + r4, centerY, mPaint);
						break;
					case SCALE_TYPE_INCH:
						draw_scale_full(canvas, r2, r2, metrics.xdpi / 10.0f, metrics.ydpi / 10.0f, 10);
						break;
					case SCALE_TYPE_MM:
						draw_scale_full(canvas, r2, r2, metrics.xdpi / 12.7f, metrics.ydpi / 12.7f, 5);
						break;
					}
					canvas.drawCircle(centerX, centerY, r4 / 2, mPaint);
					canvas.drawCircle(centerX, centerY, r4, mPaint);
					break;
				}
			} finally {
				canvas.restoreToCount(saveCount);
			}
		}
	}

	/**
	 * 目盛り付きでフレームを描画
	 * @param canvas 描画先のCanvas
	 * @param width フレームの幅
	 * @param height フレームの高さ
	 * @param step_x 1目盛り分の幅
	 * @param step_y 1目盛り分の高さ
	 * @param unit 目盛りの長さを長くする間隔
	 */
	private void draw_scale_full(final Canvas canvas, final float width, final float height, final float step_x, final float step_y, final int unit) {
		final float centerX = mCenterX;
		final float centerY = mCenterY;
		final float len4 = mScaleWidth > defaultFrameWidth ? mScaleWidth * 4 : defaultFrameWidth * 4;
		final float len2 = mScaleWidth > defaultFrameWidth ? mScaleWidth * 2 : defaultFrameWidth * 2;
		final float w2 = width  /2;
		final float h2 = height  /2;
		final int nx = (int)(w2 / step_x);
		final int ny = (int)(h2 / step_y);
		canvas.drawLine(centerX, centerY - h2, centerX, centerY + h2, mPaint);
		canvas.drawLine(centerX - w2, centerY, centerX + w2, centerY, mPaint);
		mPaint.setColor(mTickColor);
		for (int i = 0; i < nx; i++) {
			final float l = (i % unit) == 0 ? len4 : len2;
			final float xp = centerX + i * step_x;
			canvas.drawLine(xp, centerY - l, xp, centerY + l, mPaint);
			final float xm = centerX - i * step_x;
			canvas.drawLine(xm, centerY - l, xm, centerY + l, mPaint);
		}
		for (int i = 0; i < ny; i++) {
			final float l = (i % unit) == 0 ? len4 : len2;
			final float yp = centerY + i * step_y;
			canvas.drawLine(centerX - l, yp, centerX + l, yp, mPaint);
			final float ym = centerY - i * step_y;
			canvas.drawLine(centerX - l, ym, centerX + l, ym, mPaint);
		}
		mPaint.setColor(mScaleColor);
	}

	/**
	 * フレームの種類を設定
	 * @param type 範囲外なら無視される
	 */
	public void setFrameType(final int type) {
		if ((mFrameType != type) && (type >= FRAME_TYPE_NONE) && (type < FRAME_TYPE_NUMS)) {
			mFrameType = type;
			postInvalidate();
		}
	}

	/**
	 * フレームの種類を取得
	 * @return
	 */
	public int getFrameType() {
		return mFrameType;
	}

	/**
	 * フレームの描画色を設定
	 * スケールの描画色がフレームの描画色と同じならスケールの描画色も変更する
	 * @param cl
	 */
	public void setFrameColor(final int cl) {
		if (mFrameColor != cl) {
			if (mFrameColor == mScaleColor) {
				setScaleColor(cl);
			}
			mFrameColor = cl;
			postInvalidate();
		}
	}

	/**
	 * フレームの描画色を取得
	 * @return
	 */
	public int getFrameColor() {
		return mFrameColor;
	}

	/**
	 * フレームの描画幅を設定
	 * フレームの描画線幅とスケールの描画線幅が同じならスケールの描画線幅も変更する
	 * @param width 0以上[ピクセル]
	 */
	public void setFrameWidth(final float width) {
		float w = (width <= 1.0f) ? 0 : width;	// 細すぎると表示できなくなるので1px以下は0=ヘアラインにする
		if ((mFrameWidth != w) && (w >= 0)) {
			if (mFrameWidth == mScaleWidth) {
				setScaleWidth(w);
			}
			mFrameWidth = w;
			postInvalidate();
		}
	}

	/**
	 * フレームの描画幅を取得
	 * @return
	 */
	public float getFrameWidth() {
		return mFrameWidth;
	}

	/**
	 * スケールの描画色を設定
 	 * 目盛りの描画色がスケールの描画色と同じなら目盛りの描画色も変更する
	 * @param cl
	 */
	public void setScaleColor(final int cl) {
		if (mScaleColor != cl) {
			if (mScaleColor == mTickColor) {
				setTickColor(cl);
			}
			mScaleColor = cl;
			postInvalidate();
		}
	}

	/**
	 * スケールの描画色を取得
	 * @return
	 */
	public int getScaleColor() {
		return mScaleColor;
	}

	/**
	 * 目盛りの種類を設定
	 * @param type
	 */
	public void setScaleType(final int type) {
		if ((mScaleType != type) && (type >= 0) && (type < SCALE_TYPE_NUMS)) {
			mScaleType = type;
			postInvalidate();
		}
	}

	/**
	 * 目盛りの種類を取得
	 * @return
	 */
	public int getScaleType() {
		return mScaleType;
	}

	/**
	 * 目盛りの線幅を設定
	 * @param width
	 */
	public void setScaleWidth(final float width) {
		float w = (width <= 1.0f) ? 0 : width;	// 細すぎると表示できなくなるので1px以下は0=ヘアラインにする
		if (mScaleWidth != w) {
			mScaleWidth = w;
			postInvalidate();
		}
	}

	/**
	 * 目盛りの線幅を取得
	 * @return
	 */
	public float getScaleWidth() {
		return mScaleWidth;
	}

	/**
	 * 目盛りの描画色を設定
	 * @param cl
	 */
	public void setTickColor(final int cl) {
		if (mTickColor != cl) {
			mTickColor = cl;
			postInvalidate();
		}
	}

	/**
	 * 目盛りの描画色を取得
	 * @return
	 */
	public int getTickColor() {
		return mTickColor;
	}

	/**
	 * スケールの回転角度を設定(外枠は回転しない)
	 * @param degree [度]
	 */
	public void setRotation(final float degree) {
		float d = degree;
		for (; d > 360; d -= 360) {}
		for (; d < -360; d += 360) {}
		if (mRotation != d) {
			mRotation = d;
			postInvalidate();
		}
	}

	/**
	 * スケールの回転角度を取得
	 * @return [度]
	 */
	public float getRotation() {
		return mRotation;
	}

	/**
	 * スケールの拡大縮小率を設定
	 * @param scale (0-10]
	 */
	public void setScale(final float scale) {
		if ((mScale != scale) && (scale > 0) && (scale <= MAX_SCALE)) {
			mScale = scale;
			postInvalidate();
		}
	}

	/**
	 * スケールの拡大縮小率を取得
	 * @return
	 */
	public float getScale() {
		return mScale;
	}
}
