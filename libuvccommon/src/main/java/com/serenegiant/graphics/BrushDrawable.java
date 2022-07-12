package com.serenegiant.graphics;
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

import android.graphics.Canvas;
import android.graphics.DrawFilter;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.graphics.Xfermode;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.Shape;
import android.util.Log;

public class BrushDrawable extends ShapeDrawable {
	private static final boolean DEBUG = false;
	private static final String TAG = "BrushDrawable";

	public static final int ERASE_ELIPSE = -1;
	public static final int ERASE_LINE = -2;
	public static final int ERASE_TRIANGLE = -3;
	public static final int ERASE_RECTANGLE = -4;
	public static final int BRUSH_ELIPSE = 1;
	public static final int BRUSH_LINE = 2;
	public static final int BRUSH_TRIANGLE = 3;
	public static final int BRUSH_RECTANGLE = 4;

	private final PointF mPivot = new PointF();
	private final Paint mPaint;
    private final DrawFilter mDrawFilter;
    private final Xfermode mClearXfermode;
	private Shader mShader;
	private float mRotation = 0;

	public BrushDrawable(final int type, final int width, final int height) {
		this(type, width, height, 0/*Paint.DITHER_FLAG*/, 0);
	}

	public BrushDrawable(final int type, final int width, final int height, final int clearflags, final int setFlags) {
		super();
		mPaint = new Paint();
		mDrawFilter = new PaintFlagsDrawFilter(clearflags, setFlags);
		mClearXfermode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
		init(100, 100);
		setType(type, width, height);
	}

	private final void init(final int width, final int height) {
		// #getIntrinsicWidth/#getIntrinsicHeightが0より大きい値を返さないと
		// ちゃんと表示されないのでここで設定する
		setIntrinsicWidth(width > 0 ? width : 100);
		setIntrinsicHeight(height > 0 ? height : 100);
		mPivot.set(getIntrinsicWidth() / 2, getIntrinsicHeight() / 2);
	}

	/**
	 * #drawをOverrideしても良いけど、色々処理をしているので#onDrawの方をOverrideする
	 * (#onDrawは#drawから呼び出される)
	 */
	@Override
	protected void onDraw(final Shape shape, final Canvas canvas, final Paint paint) {
		canvas.rotate(mRotation, mPivot.x, mPivot.y);
		// これを入れると背景が透過する(backgroundの指定してても見えなくなる)
//		canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
		final int count = canvas.save();
//		final DrawFilter org = canvas.getDrawFilter();
		try {
//	        canvas.setDrawFilter(mDrawFilter);
	        mPaint.setShader(mShader);
/*	        paint.setColor(Color.TRANSPARENT);	// 消しゴムの時
	        paint.setXfermode(mClearXfermode); */
//	        canvas.drawPaint(mPaint);
	        super.onDraw(shape, canvas, paint);	// 描画自体は上位に任せる(実際はShape#drawに任せる)
		} finally {
//	        canvas.setDrawFilter(org);
			canvas.restoreToCount(count);
		}
	}

	public void setPivot(final float pivotX, final float pivotY) {
		if (mPivot.x != pivotX || mPivot.y != pivotY) {
			mPivot.set(pivotX, pivotY);
			invalidateSelf();
		}
	}

	public PointF getPivot() {
		return mPivot;
	}

	public float getPivotX() {
		return mPivot.x;
	}

	public float getPivotY() {
		return mPivot.y;
	}

	public float getRotation() {
		final Shape shape = getShape();
		return (shape instanceof BaseShape) ? ((BaseShape)shape).getRotation() : mRotation;
	}

	public void setRotation(final float rotation) {
//		if (DEBUG) Log.v(TAG, "setRotation:" + rotation);
		final Shape shape = getShape();
		if (shape instanceof BaseShape) {
			((BaseShape)shape).setRotation(rotation);
			mRotation = 0;
		} else {
			if (mRotation != rotation) {
				mRotation = rotation;
			}
		}
		invalidateSelf();
	}

	public void setType(final int type, final int width, final int height) {
		Shape shape = null;
		switch (type) {
		case BRUSH_ELIPSE:
			shape = new OvalShape();
			break;
		case BRUSH_LINE:
			break;
		case BRUSH_TRIANGLE:
			shape = new IsoscelesTriangleShape(width, height);
			break;
		case BRUSH_RECTANGLE:
			shape = new BaseShape(width, height);
			break;
		}
		if (shape != null) {
			mRotation = 0;
			shape.resize(width, height);
			setShape(shape);
		}
	}

	public void setColor(final int color) {
		if (DEBUG) Log.v(TAG, "setColor:color=" + color);
		final Paint paint = getPaint();
		paint.setColor(color);
		invalidateSelf();
	}

	public void setPaintAlpha(final int alpha) {
		getPaint().setAlpha(alpha);
	}

	public int getPaintAlpha() {
		return getPaint().getAlpha();
	}

	public Shader getShader() {
		return getPaint().getShader();
	}

	public void setShader(final Shader shader) {
		getPaint().setShader(shader);
		invalidateSelf();
	}

	public void setPaintStyle(final Style style) {
		getPaint().setStyle(style);
		invalidateSelf();
	}

/*	@Override
	public void setPadding(final int left, final int top, final int right, final int bottom) {
		if (DEBUG) Log.v(TAG, String.format("setPadding:(%d,%d,%d,%d)", left, top, right, bottom));
		super.setPadding(left, top, right, bottom);
	} *

/*	@Override
	public void setPadding(final Rect padding) {
		if (DEBUG) Log.v(TAG, "setPadding:" + padding);
		super.setPadding(padding);
	} */

/*	@Override
	public void setIntrinsicWidth(final int width) {
		if (DEBUG) Log.v(TAG, "setIntrinsicWidth:" + width);
		super.setIntrinsicWidth(width);
	} */

/*	@Override
	public void setIntrinsicHeight(final int height) {
		if (DEBUG) Log.v(TAG, "setIntrinsicHeight:" + height);
		super.setIntrinsicHeight(height);
	} */

/*	@Override
	public boolean getPadding(final Rect padding) {
		if (DEBUG) Log.v(TAG, "getPadding:");
		return super.getPadding(padding);
	} */

/*	@Override
	protected void onBoundsChange(final Rect bounds) {
		if (DEBUG) Log.v(TAG, "onBoundsChange:" + bounds);
		super.onBoundsChange(bounds);
	} */

/*	@Override
	public void setBounds(final int left, final int top, final int right, final int bottom) {
		if (DEBUG) Log.v(TAG, String.format("setBounds:(%d,%d,%d,%d)", left, top, right, bottom));
		super.setBounds(left, top, right, bottom);
	} */

/*	@Override
	public void setBounds(final Rect bounds) {
		if (DEBUG) Log.v(TAG, "setBounds:" + bounds);
		super.setBounds(bounds);
	} */

/*	@Override
	public int getMinimumWidth() {
		if (DEBUG) Log.v(TAG, "getMinimumWidth:" + super.getMinimumWidth());
		return super.getMinimumWidth();
	} */

/*	@Override
	public int getMinimumHeight() {
		if (DEBUG) Log.v(TAG, "getMinimumHeight:" + super.getMinimumHeight());
		return super.getMinimumHeight();
	} */
}
