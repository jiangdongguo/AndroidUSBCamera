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

import android.annotation.TargetApi;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.shapes.Shape;
import android.os.Build;

/**
 * 四角形シェイプ
 */
public class BaseShape extends Shape {
	private static final boolean DEBUG = false;
	protected final String TAG = getClass().getSimpleName();

    protected final RectF mBoundsRect = new RectF();
    /**
     * このShapeの描画内容の位置関係を設定する際のサイズ(任意)
     * Shape内描画座標系の右下座標と同じ
     */
    protected final float mStdWidth, mStdHeight;
    /**
     * Shape内描画座標系の中央座標(mStdWidth/mStdHeightのそれぞれ1/2の値)
     */
    protected final float mStdWidth2, mStdHeight2;
    /**
     * 素のShape座標系をShape内描画座標系に変換する係数
     * (設定用のサイズと実際の描画時のサイズ変換用変数)
     * #onResizeで更新される
     */
    protected float mScaleX, mScaleY;
    /**
     * 表示時の回転角度
     */
    protected float mRotation;
    /**
     * 回転・拡大縮小をShapeの中心位置で行うための変数
     * Shape座標系(Shape内描画座標系ではない素のShape座標系)
     * #onResizeで設定する
     */
    protected float mPivotX, mPivotY;

    /**
     * 標準サイズを指定して生成する
     * 描画の際には実際のサイズに合うように拡大縮小してから描画されるので、
     * このShapeの描画内容の位置関係を設定するのに都合の良いサイズをしてすれば良い。
     * @param std_width
     * @param std_height
     */
	public BaseShape(final float std_width, final float std_height) {
        mStdWidth = std_width;
        mStdHeight = std_height;
        mStdWidth2 = std_width / 2.0f;
        mStdHeight2 = mStdHeight / 2.0f;
	}

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
	@Override
    public void getOutline(final Outline outline) {
        final RectF rect = boundsRect();
        outline.setRect((int) Math.ceil(rect.left), (int) Math.ceil(rect.top),
                (int) Math.floor(rect.right), (int) Math.floor(rect.bottom));
    }

    @Override
    protected void onResize(final float width, final float height) {
        mBoundsRect.set(0, 0, width, height);
        mScaleX = width / mStdWidth;
        mScaleY = height / mStdHeight;
        mPivotX = width / 2.0f;
        mPivotY = height / 2.0f;
    }

    /**
     * 外形矩形を取得
     * @return
     */
    protected final RectF boundsRect() {
        return mBoundsRect;
    }

    @Override
    public BaseShape clone() throws CloneNotSupportedException {
        final BaseShape shape = (BaseShape)super.clone();
        shape.mBoundsRect.set(mBoundsRect);
        return shape;
    }

    public float getScaleX() {
    	return mScaleX;
    }

    public float getScaleY() {
    	return mScaleY;
    }

    public void setRotation(final float rotation) {
    	mRotation = rotation;
    }

    public float getRotation() {
    	return mRotation;
    }

    final Paint debugPaint = new Paint();
    /**
     * Shapeの描画処理
     * ここではcanvasの状態保存＆復帰、回転・拡大縮小を適用してから#doDrawを呼び出す
     */
	@Override
	public void draw(final Canvas canvas, final Paint paint) {
		final int count = canvas.save();
		canvas.translate(mPivotX, mPivotY);	// 原点を中心に移動(この時点はViewと同じスケール)
		canvas.rotate(mRotation);			// 原点周りで回転
		canvas.scale(mScaleX, mScaleY);		// 原点周りで拡大縮小=Viewの座標系のスケールからShape内表示内容座標系にスケール変換
		canvas.translate(-mStdWidth2, -mStdHeight2);	// 原点を元の位置に戻す(ここで動かすのはShape内座標系での値)
        doDraw(canvas, paint);
        canvas.restoreToCount(count);
        if (DEBUG) {
			debugPaint.setColor(Color.RED);
			debugPaint.setStyle(Paint.Style.STROKE);
			canvas.drawRect(mBoundsRect, debugPaint);
			canvas.drawLine(mBoundsRect.left - 20, mBoundsRect.height() / 2, mBoundsRect.right + 20, mBoundsRect.height() / 2, debugPaint);
			canvas.drawLine(mBoundsRect.width() / 2, mBoundsRect.top - 20, mBoundsRect.width() / 2, mBoundsRect.bottom + 20, debugPaint);
        }

	}

	/**
	 * Shapeの実際の描画処理　BaseShapeでは四角を描画する。回転・スケール変換済みのShape内描画座標系なので注意
	 * 必要に応じて下位クラスでOverrideすること
	 * @param canvas
	 * @param paint
	 */
	protected void doDraw(final Canvas canvas, final Paint paint) {
        canvas.drawRect(mBoundsRect, paint);
	}
}
