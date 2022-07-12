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
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.serenegiant.common.R;
import com.serenegiant.glutils.IRendererCommon;

public class ZoomAspectScaledTextureView extends AspectScaledTextureView
	implements IRendererCommon {
	private boolean mHandleTouchEvent;
	// constants
	/**
	 * State: idle
	 */
	private static final int STATE_NON = 0;
	/**
	 * State: 待機中
	 */
	private static final int STATE_WAITING = 1;
	/**
	 * State: 平行移動中
	*/
	private static final int STATE_DRAGING = 2;
	/**
	 * State: 拡大縮小・回転開始待ち
	 */
	private static final int STATE_CHECKING = 3;
	/**
	 * State: 拡大縮小中
	*/
	private static final int STATE_ZOOMING = 4;
	/**
	 * State: 回転中
	 */
	private static final int STATE_ROTATING = 5;
	/**
	 * 最大拡大率
	*/
	private static final float DEFAULT_MAX_SCALE = 8.f;
	/**
	 * 最小縮小率
	 */
	private static final float DEFAULT_MIN_SCALE = 0.8f;
	/**
	 * ズーム無し時の初期拡大縮小率
	*/
	private static final float DEFAULT_SCALE = 1.f;
	/**
	 * ズーム/回転モードに入るときの最小タッチ距離
	 */
	private static final float MIN_DISTANCE = 15.f;
	private static final float MIN_DISTANCE_SQUARE = MIN_DISTANCE * MIN_DISTANCE;
	/**
	 * 移動時に画面からはみ出して見えなくなってしまうのを防ぐための閾値
	 */
	private static final float MOVE_LIMIT_RATE = 0.2f;	// =Viewの幅/高さのそれぞれ20%
	/**
     * 回転開始待ち時間(マルチタッチ時)またはリセット待機時間(シングルタッチ)、ミリ秒単位
	 */
    private static final int CHECK_TIMEOUT
    	= ViewConfiguration.getTapTimeout() + ViewConfiguration.getLongPressTimeout();
    private static final int TAP_TIMEOUT = ViewConfiguration.getTapTimeout() * 2;
    private static final int LONG_PRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();
    /**
	 * ラディアンを度に変換するための係数
	 */
	private static final float TO_DEGREE = 57.2957795130823f;	// = (1.0f / Math.PI) * 180.0f;
	/**
	 * 回転しているかどうかの閾値
	 */
	private static final float EPS = 0.1f;

	/**
	 * 拡大縮小回転移動のための射影行列のデフォルト値
	 */
	protected final Matrix mDefaultMatrix = new Matrix();
	/**
	 * 射影行列が変更されたかどうかのフラグ(キャッシュを更新するため)
	 */
	protected boolean mImageMatrixChanged;
	/**
	 * 毎回射影行列そのものにアクセスするとJNIのオーバーヘッドがあるのでJavaのfloat配列としてキャッシュ
	 */
	protected final float[] mMatrixCache = new float[9];
	/**
	 * タッチ操作開始時の射影行列保持用
	 */
	private final Matrix mSavedImageMatrix = new Matrix();
	/**
	 * 映像を移動可能な領域
	 */
	private final RectF mLimitRect = new RectF();
	/**
	 * 映像を移動可能な領域を示すLineSegment配列
	 */
	private final LineSegment[] mLimitSegments = new LineSegment[4];
	/**
	 * 表示されるViewの実際のサイズ
	 */
	private final RectF mImageRect = new RectF();
	/**
	 * scaled and moved and rotated corner coordinates of image
	 * [(left,top),(right,top),(right,bottom),(left.bottom)]
	 */
	private final float[] mTrans = new float[8];
	/**
	 * タッチイベントのID
	 */
	private int mPrimaryId, mSecondaryId;
	/**
	 * 1つ目のタッチでのx,y座標
	 */
	private float mPrimaryX, mPrimaryY;
	/**
	 * 2つ目のタッチでのx,y座標
	 */
	private float mSecondX, mSecondY;
	/**
	 * 拡大縮小・回転時に使用するピボット座標
	 */
	private float mPivotX, mPivotY;
	/**
	 * distance between touch points when start multi touch, for calculating zooming scale
	 */
	private float mTouchDistance;
	/**
	 * current rotating degree
	 */
	private float mCurrentDegrees;
	private boolean mIsRotating;
	/**
	 * Maximum zoom scale
	 */
	protected final float mMaxScale = DEFAULT_MAX_SCALE;
	/**
	 * Minimum zoom scale, set in #init as fit the image to this view bounds
	 */
	private float mMinScale = DEFAULT_MIN_SCALE;
	/**
	 * current state, -1/STATE_NON/STATE_WATING/STATE_DRAGING/STATE_CHECKING
	 * 					/STATE_ZOOMING/STATE_ROTATING
	 */
	private int mState = -1;
	/**
	 * Runnable instance to wait starting image reset
	 */
	private Runnable mWaitImageReset;
	/**
	 * Runnable instance to wait starting rotation
	 */
	private Runnable mStartCheckRotate;

	@MirrorMode
    private int mMirrorMode = MIRROR_NORMAL;

	public ZoomAspectScaledTextureView(final Context context) {
		this(context, null, 0);
	}

	public ZoomAspectScaledTextureView(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ZoomAspectScaledTextureView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		final TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ZoomAspectScaledTextureView, defStyleAttr, 0);
		try {
			mHandleTouchEvent = a.getBoolean(R.styleable.ZoomAspectScaledTextureView_handle_touch_event, true);
		} finally {
			a.recycle();
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(final MotionEvent event) {

		if (!mHandleTouchEvent) {
			return super.onTouchEvent(event);
		}

		final int actionCode = event.getActionMasked();	// >= API8

		switch (actionCode) {
		case MotionEvent.ACTION_DOWN:
			// single touch
			startWaiting(event);
			return true;
		case MotionEvent.ACTION_POINTER_DOWN:
		{
			// start multi touch, zooming/rotating
			switch (mState) {
			case STATE_WAITING:
				removeCallbacks(mWaitImageReset);
				// pass through
			case STATE_DRAGING:
				if (event.getPointerCount() > 1) {
					startCheck(event);
					return true;
				}
				break;
			}
			break;
		}
		case MotionEvent.ACTION_MOVE:
		{
			// moving with single and multi touch
			switch (mState) {
			case STATE_WAITING:
				if (checkTouchMoved(event)) {
					removeCallbacks(mWaitImageReset);
					setState(STATE_DRAGING);
					return true;
				}
				break;
			case STATE_DRAGING:
				if (processDrag(event))
					return true;
				break;
			case STATE_CHECKING:
				if (checkTouchMoved(event)) {
					startZoom(event);
					return true;
				}
				break;
			case STATE_ZOOMING:
				if (processZoom(event))
					return true;
				break;
			case STATE_ROTATING:
				if (processRotate(event))
					return true;
				break;
			}
			break;
		}
		case MotionEvent.ACTION_CANCEL:
			// pass through
		case MotionEvent.ACTION_UP:
			removeCallbacks(mWaitImageReset);
			removeCallbacks(mStartCheckRotate);
			if ((actionCode == MotionEvent.ACTION_UP) && (mState == STATE_WAITING)) {
				final long downTime = SystemClock.uptimeMillis() - event.getDownTime();
				if (downTime > LONG_PRESS_TIMEOUT) {
					performLongClick();
				} else if (downTime < TAP_TIMEOUT) {
					performClick();
				}
			}
			// pass through
		case MotionEvent.ACTION_POINTER_UP:
			setState(STATE_NON);
			break;
		}
		return super.onTouchEvent(event);
	}

//================================================================================
	/**
	 * TextureViewに関連付けられたSurfaceTextureが利用可能になった時の処理
	 */
	@Override
	public void onSurfaceTextureAvailable(final SurfaceTexture surface, final int width, final int height) {
		super.onSurfaceTextureAvailable(surface, width, height);
		setMirror(MIRROR_NORMAL);	// デフォルトだから適用しなくていいけど
	}

	/**
	 * SurfaceTextureのバッファーのサイズが変更された時の処理
	 */
	@Override
	public void onSurfaceTextureSizeChanged(final SurfaceTexture surface, final int width, final int height) {
		super.onSurfaceTextureSizeChanged(surface, width, height);
		applyMirrorMode();
	}

//	/**
//	 * SurfaceTextureが破棄される時の処理
//	 * trueを返すとこれ以降描画処理は行われなくなる
//	 * falseを返すと自分でSurfaceTexture#release()を呼び出さないとダメ
//	 * ほとんどのアプリではtrueを返すべきである
//	 */
//	@Override
//	public boolean onSurfaceTextureDestroyed(final SurfaceTexture surface) {
//		super.onSurfaceTextureDestroyed(surface)
//		return true;
//	}

//================================================================================
	@Override
	public void setMirror(@MirrorMode final int mirror) {
		if (mMirrorMode != mirror) {
			mMirrorMode = mirror;
			applyMirrorMode();
		}
	}

	@Override
	@MirrorMode
	public int getMirror() {
		return mMirrorMode;
	}
	
	public void setEnableHandleTouchEvent(final boolean enabled) {
		mHandleTouchEvent = enabled;
	}

	public void reset() {
		init();
	}

//================================================================================
	@Override
	protected void init() {
		// set the initial state to idle, get and save the internal Matrix.
		mState = -1; setState(STATE_NON);
		// get the internally calculated zooming scale to fit the view
		mMinScale = DEFAULT_MIN_SCALE; // getMatrixScale();
		mCurrentDegrees = 0.f;
		mIsRotating = Math.abs(((int)(mCurrentDegrees / 360.f)) * 360.f - mCurrentDegrees) > EPS;

		// update image size
		// current implementation of ImageView always hold its image as a Drawable
		// (that can get ImageView#getDrawable)
		// therefore update the image size from its Drawable
		// set limit rectangle that the image can move
		final int view_width = getWidth();
		final int view_height = getHeight();
		final Rect tmp = new Rect();
		getDrawingRect(tmp);
		mLimitRect.set(tmp);
		mLimitRect.inset((int)(MOVE_LIMIT_RATE * view_width), (int)(MOVE_LIMIT_RATE * view_height));
		mLimitSegments[0] = null;
		mImageRect.set(0, 0, tmp.width(), tmp.height());
		super.init();
		mDefaultMatrix.set(mImageMatrix);
	}

	/**
	 * set current state, get and save the internal Matrix int super class
	 * @param state:	-1/STATE_NON/STATE_DRAGING/STATECHECKING
	 * 					/STATE_ZOOMING/STATE_ROTATING
	 */
	private final void setState(final int state) {
		if (mState != state) {
			mState = state;
			// get and save the internal Matrix of super class
			getTransform(mSavedImageMatrix);
			if (!mImageMatrix.equals(mSavedImageMatrix)) {
				mImageMatrix.set(mSavedImageMatrix);
				mImageMatrixChanged = true;
			}
		}
	}

	/**
	 * start waiting
	 * @param event
	 */
	private final void startWaiting(final MotionEvent event) {
		mPrimaryId = 0;
		mSecondaryId = -1;
		mPrimaryX = mSecondX = event.getX();
		mPrimaryY = mSecondY = event.getY();
		if (mWaitImageReset == null) mWaitImageReset = new WaitImageReset();
		postDelayed(mWaitImageReset, CHECK_TIMEOUT);
		setState(STATE_WAITING);
	}

	/**
	 * move the image
	 * @param event
	 */
	private final boolean processDrag(final MotionEvent event) {
		float dx = event.getX() - mPrimaryX;
		float dy = event.getY() - mPrimaryY;

		// calculate the corner coordinates of image applied matrix
		// [(left,top),(right,top),(right,bottom),(left.bottom)]
		mTrans[0] = mTrans[6] = mImageRect.left;
		mTrans[1] = mTrans[3] = mImageRect.top;
		mTrans[5] = mTrans[7] = mImageRect.bottom;
		mTrans[2] = mTrans[4] = mImageRect.right;
		mImageMatrix.mapPoints(mTrans);
		for (int i = 0; i < 8; i += 2) {
			mTrans[i] += dx;
			mTrans[i+1] += dy;
		}
		// check whether the image can move
		// if we can ignore rotating, the limit check is more easy...
		boolean canMove
			// check whether at lease one corner of image bounds is in the limitRect
			 = mLimitRect.contains(mTrans[0], mTrans[1])
			|| mLimitRect.contains(mTrans[2], mTrans[3])
			|| mLimitRect.contains(mTrans[4], mTrans[5])
			|| mLimitRect.contains(mTrans[6], mTrans[7])
			// check whether at least one corner of limitRect is in the image bounds
			|| ptInPoly(mLimitRect.left, mLimitRect.top, mTrans)
			|| ptInPoly(mLimitRect.right, mLimitRect.top, mTrans)
			|| ptInPoly(mLimitRect.right, mLimitRect.bottom, mTrans)
			|| ptInPoly(mLimitRect.left, mLimitRect.bottom, mTrans);
		if (!canMove) {
			// when no corner is in, we need additional check whether at least
			// one side of image bounds intersect with the limit rectangle
			if (mLimitSegments[0] == null) {
				mLimitSegments[0] = new LineSegment(mLimitRect.left, mLimitRect.top, mLimitRect.right, mLimitRect.top);
				mLimitSegments[1] = new LineSegment(mLimitRect.right, mLimitRect.top, mLimitRect.right, mLimitRect.bottom);
				mLimitSegments[2] = new LineSegment(mLimitRect.right, mLimitRect.bottom, mLimitRect.left, mLimitRect.bottom);
				mLimitSegments[3] = new LineSegment(mLimitRect.left, mLimitRect.bottom, mLimitRect.left, mLimitRect.top);
			}
			final LineSegment side = new LineSegment(mTrans[0], mTrans[1], mTrans[2], mTrans[3]);
			canMove = checkIntersect(side, mLimitSegments);
			if (!canMove) {
				side.set(mTrans[2], mTrans[3], mTrans[4], mTrans[5]);
				canMove = checkIntersect(side, mLimitSegments);
				if (!canMove) {
					side.set(mTrans[4], mTrans[5], mTrans[6], mTrans[7]);
					canMove = checkIntersect(side, mLimitSegments);
					if (!canMove) {
						side.set(mTrans[6], mTrans[7], mTrans[0], mTrans[1]);
						canMove = checkIntersect(side, mLimitSegments);
					}
				}
			}
		}
		if (canMove) {
			// TODO we need adjust dx/dy not to penetrate into the limit rectangle
			// otherwise the image can not move when one side is on the border of limit rectangle.
			// only calculate without rotation now because its calculation is to heavy when rotation applied.
			if (!mIsRotating) {
				final float left = Math.min(Math.min(mTrans[0], mTrans[2]), Math.min(mTrans[4], mTrans[6]));
				final float right = Math.max(Math.max(mTrans[0], mTrans[2]), Math.max(mTrans[4], mTrans[6]));
				final float top = Math.min(Math.min(mTrans[1], mTrans[3]), Math.min(mTrans[5], mTrans[7]));
				final float bottom = Math.max(Math.max(mTrans[1], mTrans[3]), Math.max(mTrans[5], mTrans[7]));

				if (right < mLimitRect.left) {
					dx = mLimitRect.left - right;
				} else if (left + EPS > mLimitRect.right) {
					dx = mLimitRect.right - left - EPS;
				}
				if (bottom < mLimitRect.top) {
					dy = mLimitRect.top - bottom;
				} else if (top + EPS > mLimitRect.bottom) {
					dy = mLimitRect.bottom - top - EPS;
				}
			}
			if ((dx != 0) || (dy != 0)) {
//				if (DEBUG) Log.v(TAG, String.format("processDrag:dx=%f,dy=%f", dx, dy));
				// apply move
				if (mImageMatrix.postTranslate(dx, dy)) {
					// when image is really moved?
					mImageMatrixChanged = true;
					// apply to super class
					setTransform(mImageMatrix);
				}
			}
		}
		mPrimaryX = event.getX();
		mPrimaryY = event.getY();
		return canMove;
	}

	/**
	 * start checking whether zooming/rotating
	 * @param event
	 */
	private final void startCheck(final MotionEvent event) {

		if (event.getPointerCount() > 1) {
			// primary touch
			mPrimaryId = event.getPointerId(0);
			mPrimaryX = event.getX(0);
			mPrimaryY = event.getY(0);
			// secondary touch
			mSecondaryId = event.getPointerId(1);
			mSecondX = event.getX(1);
			mSecondY = event.getY(1);
			// calculate the distance between first and second touch
			final float dx = mSecondX - mPrimaryX;
			final float dy = mSecondY - mPrimaryY;
			final float distance = (float)Math.hypot(dx, dy);
			if (distance < MIN_DISTANCE) {
				//  ignore when the touch distance is too short
				return;
			}

			mTouchDistance = distance;
			// set pivot position to the middle coordinate
			mPivotX = (mPrimaryX + mSecondX) / 2.f;
			mPivotY = (mPrimaryY + mSecondY) / 2.f;
			//
			if (mStartCheckRotate == null)
				mStartCheckRotate = new StartCheckRotate();
			postDelayed(mStartCheckRotate, CHECK_TIMEOUT);
			setState(STATE_CHECKING); 		// start zoom/rotation check
		}
	}

	/**
	 * start zooming
	 * @param event
	 * @return
	 */
	private final void startZoom(final MotionEvent event) {

		removeCallbacks(mStartCheckRotate);
		setState(STATE_ZOOMING);
	}

	/**
	 * zooming
	 * @param event
	 * @return
	 */
	private final boolean processZoom(final MotionEvent event) {
		// restore the Matrix
		restoreMatrix();
		// get current zooming scale
		final float currentScale = getMatrixScale();
		// calculate the zooming scale from the distance between touched positions
		final float scale = calcScale(event);
		// calculate the applied zooming scale
		final float tmpScale = scale * currentScale;
		if (tmpScale < mMinScale) {
			// skip if the applied scale is smaller than minimum scale
			return false;
		} else if (tmpScale > mMaxScale) {
			// skip if the applied scale is bigger than maximum scale
			return false;
		}
		// change scale with scale value and pivot point
		if (mImageMatrix.postScale(scale, scale, mPivotX, mPivotY)) {
			// when Matrix is changed
			mImageMatrixChanged = true;
			// apply to super class
			setTransform(mImageMatrix);
		}
		return true;
	}

	/**
	 * calculate the zooming scale from the distance between touched position</br>
	 * this method ony use the index of 0 and 1 for touched position
	 * @param event
	 * @return
	 */
	private final float calcScale(final MotionEvent event) {
		final float dx = event.getX(0) - event.getX(1);
		final float dy = event.getY(0) - event.getY(1);
		final float distance = (float)Math.hypot(dx, dy);

		return distance / mTouchDistance;
	}

	/**
	 * check whether the touch position changed
	 * @param event
	 * @return true if the touch position changed
	 */
	private final boolean checkTouchMoved(final MotionEvent event) {
		final boolean result = true;
		final int ix0 = event.findPointerIndex(mPrimaryId);
		final int ix1 = event.findPointerIndex(mSecondaryId);
		if (ix0 >= 0) {
			// check primary touch
			float x = event.getX(ix0) - mPrimaryX;
			float y = event.getY(ix0) - mPrimaryY;
			if (x * x + y * y < MIN_DISTANCE_SQUARE) {
				// primary touch is at the almost same position
				if (ix1 >= 0) {
					// check secondary touch
					x = event.getX(ix1) - mSecondX;
					y = event.getY(ix1) - mSecondY;
					if (x * x + y * y < MIN_DISTANCE_SQUARE) {
						// secondary touch is also at the almost same position.
						return false;
					}
				} else {
					return false;
				}
			}
		}
		return result;
	}

	/**
	 * rotating image
	 * @param event
	 * @return
	 */
	private final boolean processRotate(final MotionEvent event) {
		if (checkTouchMoved(event)) {
			// restore the Matrix
			restoreMatrix();
			mCurrentDegrees = calcAngle(event);
			mIsRotating = Math.abs(((int)(mCurrentDegrees / 360.f)) * 360.f - mCurrentDegrees) > EPS;
			if (mIsRotating && mImageMatrix.postRotate(mCurrentDegrees, mPivotX, mPivotY)) {
				// when Matrix is changed
				mImageMatrixChanged = true;
				// apply to super class
				setTransform(mImageMatrix);
				return true;
			}
		}
		return false;
	}

	/**
	 * calculate the rotating angle</br>
	 * first vector Za=(X0,Y0), second vector Zb=(X1,Y1), angle between two vectors=φ</br>
	 * cos φ ＝ Za・Zb / (|Za| |Zb|)</br>
	 *  =(X0X1+Y0Y1) / √{(X0^2 + Y0^2)(X1^2 + Y1^2)}</br>
	 * ∴result angle φ=Arccos(cosφ)</br>
	 * the result of Arccos if 0-π[rad] therefor we need to convert to degree
	 * and adjust the rotating direction using cross-product of vector Za and Zb
	 * @param event
	 * @return
	 */
	private final float calcAngle(final MotionEvent event) {
		final int ix0 = event.findPointerIndex(mPrimaryId);
		final int ix1 = event.findPointerIndex(mSecondaryId);
		float angle = 0.f;
		if ((ix0 >= 0) && (ix1 >= 0)) {
			// first vector (using touch points when start rotating)
			final float x0 = mSecondX - mPrimaryX;
			final float y0 = mSecondY - mPrimaryY;
			// second vector (using current touch points)
			final float x1 = event.getX(ix1) - event.getX(ix0);
			final float y1 = event.getY(ix1) - event.getY(ix0);
			//
			final double s = (x0 * x0 + y0 * y0) * (x1 * x1 + y1 * y1);
			final double cos = dotProduct(x0, y0, x1, y1) / Math.sqrt(s);
			angle = TO_DEGREE * (float)Math.acos(cos) * Math.signum(crossProduct(x0, y0, x1, y1));
		}
		return angle;
	}

	private static final float dotProduct(final float x0, final float y0, final float x1, final float y1) {
		return x0 * x1 + y0 * y1;
	}

	private static final float crossProduct(final float x0, final float y0, final float x1, final float y1) {
		return x0 * y1 - x1 * y0;
	}

	private static final float crossProduct(final Vector v1, final Vector v2) {
		return v1.x * v2.y - v2.x * v1.y;
	}

	/**
	 * check whether the point is in the clockwise 2D polygon
	 * @param x
	 * @param y
	 * @param poly
	 * @return
	 */
	private static final boolean ptInPoly(final float x, final float y, final float[] poly) {

		final int n = poly.length & 0x7fffffff;
		// minimum 3 points(3 pair of x/y coordinates) need to calculate >> length >= 6
		if (n < 6) return false;
		boolean result = true;
		final Vector v1 = new Vector();
		final Vector v2 = new Vector();
		for (int i = 0; i < n; i += 2) {
			v1.set(x, y).dec(poly[i], poly[i + 1]);
			if (i + 2 < n) v2.set(poly[i + 2], poly[i + 3]);
			else v2.set(poly[0], poly[1]);
			v2.dec(poly[i], poly[i + 1]);
			if (crossProduct(v1, v2) > 0) {
//				if (DEBUG) Log.v(TAG, "pt is outside of a polygon:");
				result = false;
				break;
			}
		}
		return result;
	}

	/**
	 * helper for intersection check etc.
	 */
	private static final class Vector {
		public float x, y;
		public Vector() {
		}
/*		public Vector(Vector src) {
			set(src);
		} */
		public Vector(final float x, final float y) {
			set(x, y);
		}
		public Vector set(final float x, final float y) {
			this.x = x;
			this.y = y;
			return this;
		}
/*		public Vector set(Vector other) {
			x = other.x;
			y = other.y;
			return this;
		} */
/*		public Vector add(Vector other) {
			return new Vector(x + other.x, y + other.y);
		} */
/*		public Vector add(float x, float y) {
			return new Vector(this.x + x, this.y + y);
		} */
/*		public Vector inc(Vector other) {
			x += other.x;
			y += other.y;
			return this;
		} */
/*		public Vector inc(float x, float y) {
			this.x += x;
			this.y += y;
			return this;
		} */
		public Vector sub(final Vector other) {
			return new Vector(x - other.x, y - other.y);
		}
/*		public Vector sub(float x, float y) {
			return new Vector(this.x - x, this.y - y);
		} */
/*		public Vector dec(Vector other) {
			x -= other.x;
			y -= other.y;
			return this;
		} */
		public Vector dec(final float x, final float y) {
			this.x -= x;
			this.y -= y;
			return this;
		}
	}

	private static final class LineSegment {
		public final Vector p1;
		public final Vector p2;

		public LineSegment (final float x0, final float y0, final float x1, final float y1) {
			p1 = new Vector(x0, y0);
			p2 = new Vector(x1, y1);
		}
		public LineSegment set(final float x0, final float y0, final float x1, final float y1) {
			p1.set(x0, y0);
			p2.set(x1,  y1);
			return this;
		}
/*		@Override
		public String toString() {
			return String.format(Locale.US, "p1=(%f,%f),p2=(%f,%f)", p1.x, p1.y, p2.x, p2.y);
		} */
	}

	/**
	 * check whether line segment(seg) intersects with at least one of line segments in the array
	 * @param seg
	 * @param segs array of segment
	 * @return true if line segment intersects with at least one of other line segment.
	 */
	private static final boolean checkIntersect(final LineSegment seg, final LineSegment[] segs) {
		boolean result = false;
		final int n = segs != null ? segs.length : 0;

		final Vector a = seg.p2.sub(seg.p1);
		Vector b, c, d;
		for (int i= 0; i < n; i++) {
			c = segs[i].p1.sub(seg.p1);
			d = segs[i].p2.sub(seg.p1);
			result = crossProduct(a, c) * crossProduct(a, d) < EPS;
			if (result) {
				b = segs[i].p2.sub(segs[i].p1);
				c = seg.p1.sub(segs[i].p1);
				d = seg.p2.sub(segs[i].p1);
				result = crossProduct(b, c) * crossProduct(b, d) < EPS;
				if (result) {
					break;
				}
			}
		}
		return result;
	}

	/**
	 * get the zooming scale</br>
	 * return minimum one of MSCALE_X and MSCALE_Y
	 * @return return DEFAULT_SCALE when the scale is smaller than or equal to zero
	 */
	private final float getMatrixScale() {
		updateMatrixCache();
		final float scale = Math.min(mMatrixCache[Matrix.MSCALE_X], mMatrixCache[Matrix.MSCALE_X]);
		if (scale <= 0f) {	// for prevent disappearing or reversing
//			if (DEBUG) Log.w(TAG, "getMatrixScale:scale<=0, set to default");
			return DEFAULT_SCALE;
		}
		return scale;
	}

	/**
	 * restore the Matrix to the one when state changed
	 */
	private final void restoreMatrix() {
		mImageMatrix.set(mSavedImageMatrix);
		mImageMatrixChanged = true;
	}

	/**
	 * update the matrix caching float[]
	 */
	private final boolean updateMatrixCache() {
		if (mImageMatrixChanged) {
			mImageMatrix.getValues(mMatrixCache);
			mImageMatrixChanged = false;
			return true;
		}
		return false;
	}

//--------------------------------------------------------------------------------
	/**
	 * ミラーモードをTextureViewに適用
	 */
	private void applyMirrorMode() {
//		if (DEBUG) Log.v(TAG, "updateMatrix");
		switch (mMirrorMode) {
		case MIRROR_HORIZONTAL:
			setScaleX(-1.0f);
			setScaleY(1.0f);
			break;
		case MIRROR_VERTICAL:
			setScaleX(1.0f);
			setScaleY(-1.0f);
			break;
		case MIRROR_BOTH:
			setScaleX(-1.0f);
			setScaleY(-1.0f);
			break;
		case MIRROR_NORMAL:
		default:
			setScaleX(1.0f);
			setScaleY(1.0f);
			break;
		}
	}

	/**
	 * リセット待ちのためのRunnable
	 */
	private final class WaitImageReset implements Runnable {
		@Override
		public void run() {
			init();
		}
	}

	/**
	 * 回転待ちのためのRunnable
	 */
	private final class StartCheckRotate implements Runnable {
		@Override
		public void run() {
			if (mState == STATE_CHECKING) {
				setState(STATE_ROTATING);
			}
		}
	}

//================================================================================
}
