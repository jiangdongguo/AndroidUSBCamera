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

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;

import com.serenegiant.common.R;
import com.serenegiant.utils.BuildCheck;

/**
 * 2つの子Viewを指定した位置に自動配置するViewGroup
 */
public class TwoPainViewGroup extends FrameLayout {
//	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = TwoPainViewGroup.class.getSimpleName();

	/**
	 * 横分割する時
	 */
	public static final int HORIZONTAL = 0;
	/**
	 * 縦分割する時
	 */
	public static final int VERTICAL = 1;

	/**
	 * 2分割表示
	 */
	public static final int MODE_SPLIT = 0x00;
	/**
	 * 左Viewを全画面で表示, 右Viewは小さく表示
	 */
	public static final int MODE_SELECT_1 = 0x01;
	/**
	 * 右Viewを全画面で表示, 左Viewは小さく表示
	 */
	public static final int MODE_SELECT_2 = 0x02;
	/**
	 * 左Viewを全画面で表示, 右Viewは非表示
	 */
	public static final int MODE_SINGLE_1 = 0x03;
	/**
	 * 右Viewを全画面で表示, 左Viewは非表示
	 */
	public static final int MODE_SINGLE_2 = 0x04;

	private static final int DEFAULT_WIDTH = 200;
	private static final int DEFAULT_HEIGHT = 200;

	/**
	 * サブウインドウ表示の大きさの割合
	 */
	private static final float DEFAULT_SUB_WINDOW_SCALE = 0.2f;

	private static final int DEFAULT_CHILD_GRAVITY = Gravity.CENTER;

//********************************************************************************
	/**
	 * 設定変更時の排他制御用オブジェクト
	 */
	private final Object mSync = new Object();
	/**
	 * 分割表示時の方向
	 * HORIZONTAL(横分割), VERTICAL(縦分割)
	 */
	private int mOrientation;
	/**
	 * 表示モード
	 * MODE_SPLIT(分割表示), MODE_SELECT_1(1番目=左のViewを全画面表示), MODE_SELECT_2(2番目=右のViewを全画面表示)
	 */
	private int mDisplayMode;
	/**
	 * 表示モードがMODE_SELECT_1の時にもう1つのViewをサブウインドウとして表示するかどうか
	 * true: サブウインドウ表示する、false: 非表示にする
	 */
	private boolean mEnableSubWindow;
	/**
	 * 子Viewの位置を入れ替えるかどうか
	 * true: View1とView2の位置を入れ替える
	 */
	private boolean mFlipChildPos;
	/**
	 * サブウインドウ表示する時の大きさの比率
	 */
	private float mSubWindowScale;
	/**
	 * 表示モード切替時のアニメーション用
	 */
	private ObjectAnimator mAnimator;
	private View mChild1, mChild2;

//--------------------------------------------------------------------------------
	public TwoPainViewGroup(final Context context) {
		this(context, null, 0);
	}

	public TwoPainViewGroup(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public TwoPainViewGroup(final Context context, final AttributeSet attrs, final int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TwoPainViewGroup, defStyleAttr, 0);
		mOrientation = a.getInt(R.styleable.TwoPainViewGroup_orientation, HORIZONTAL);
		mDisplayMode = a.getInt(R.styleable.TwoPainViewGroup_displayMode, MODE_SPLIT);
		mEnableSubWindow = a.getBoolean(R.styleable.TwoPainViewGroup_enableSubWindow, true);
		mFlipChildPos = a.getBoolean(R.styleable.TwoPainViewGroup_flipChildPos, false);
		mSubWindowScale = a.getFloat(R.styleable.TwoPainViewGroup_subWindowScale, DEFAULT_SUB_WINDOW_SCALE);
		if ((mSubWindowScale <= 0) || (mSubWindowScale >= 1.0f)) {
			mSubWindowScale = DEFAULT_SUB_WINDOW_SCALE;
		}
		a.recycle();
	}

//--------------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 *
	 * @throws IllegalStateException 2個以上追加しようとするとIllegalStateExceptionを投げる
	 */
	@Override
	public void addView(final View child, final int index, final ViewGroup.LayoutParams params) {
//		if (DEBUG) Log.v(TAG, "addView:index=" + index);
		if (getChildCount() >= 2) {
			throw new IllegalStateException("Can't add more than 2 views to a ViewSwitcher");
		}
		super.addView(child, index, params);
		final int n = getChildCount();
		if ((n > 0) && (mChild1 == null)) {
			mChild1 = getChildAt(0);
		}
		if ((n > 1) && (mChild2 == null)) {
			mChild2 = getChildAt(1);
		}
	}

	@Override
	public void onViewRemoved(final View child) {
		super.onViewRemoved(child);
		if (child == mChild1) {
			mChild1 = null;
		} else if (child == mChild2) {
			mChild2 = null;
		}
	}

	@Override
	public void onInitializeAccessibilityEvent(final AccessibilityEvent event) {
		super.onInitializeAccessibilityEvent(event);
		event.setClassName(TwoPainViewGroup.class.getName());
	}

	@Override
	public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
		super.onInitializeAccessibilityNodeInfo(info);
		info.setClassName(TwoPainViewGroup.class.getName());
	}

//--------------------------------------------------------------------------------
	/**
	 * 分割方向を指定
	 * @param orientation VERTICALかHORIZONTAL
	 */
	public void setOrientation(final int orientation) {
//		if (DEBUG) Log.v(TAG, "setOrientation:orientation=" + orientation);
		synchronized (mSync) {
			if (mOrientation != (orientation % 2)) {
				mOrientation = (orientation % 2);
				startLayout();
			}
		}
	}

	/**
	 * 分割方向を取得
	 * @return
	 */
	public int getOrientation() {
		synchronized (mSync) {
			return mOrientation;
		}
	}

	/**
	 * サブウインドウ表示を有効にするかどうか
	 * @param enable
	 */
	public void setEnableSubWindow(final boolean enable) {
//		if (DEBUG) Log.v(TAG, "setEnableSubWindow:enable=" + enable);
		synchronized (mSync) {
			if (mEnableSubWindow != enable) {
				mEnableSubWindow = enable;
				startLayout();
			}
		}
	}

	/**
	 * サブウインドウ表示が有効かどうかを取得
	 * @return
	 */
	public boolean getEnableSubWindow() {
		synchronized (mSync) {
			return mEnableSubWindow;
		}
	}

	/**
	 * 表示モードを設定
	 * @param mode MODE_SPLIT(分割表示), MODE_SELECT_1(1番目=左のViewを全画面表示), MODE_SELECT_2(2番目=右のViewを全画面表示)
	 */
	public void setDisplayMode(final int mode) {
//		if (DEBUG) Log.v(TAG, "setDisplayMode:mode=" + mode);
		synchronized (mSync) {
			if (mDisplayMode != mode) {
				mDisplayMode = mode;
				startLayout();
			}
		}
	}

	/**
	 * 表示モードを取得
	 * @return
	 */
	public int getDisplayMode() {
		synchronized (mSync) {
			return mDisplayMode;
		}
	}

	/**
	 * サブウインドウの表示サイズ比率を設定
	 * @param scale 0以下または1以上ならデフォルト(0.2)になる
	 */
	public void setSubWindowScale(final float scale) {
		float _scale = scale;
		if ((_scale <= 0) || (_scale >= 1.0f)) {
			_scale = DEFAULT_SUB_WINDOW_SCALE;
		}
		synchronized (mSync) {
			if (_scale != mSubWindowScale) {
				mSubWindowScale = _scale;
				startLayout();
			}
		}
	}

	/**
	 * サブウインドウの表示サイズ比率を取得
	 * @return
	 */
	public float getSubWindowScale() {
		synchronized (mSync) {
			return mSubWindowScale;
		}
	}

	/**
	 * 子Viewの位置を入れ替えるかどうかを設定
	 * @param flip
	 */
	public void setFlipChildPos(final boolean flip) {
		synchronized (mSync) {
			if (flip != mFlipChildPos) {
				mFlipChildPos = flip;
				startLayout();
			}
		}
	}

	/**
	 * 子Viewの位置を入れ替えるかどうかを取得
	 * @return
	 */
	public boolean getFlipChildPos() {
		synchronized (mSync) {
			return mFlipChildPos;
		}
	}

//--------------------------------------------------------------------------------
	@Override
	protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
		// 自分の大きさを決定
		// 子Viewに合わせて大きさを変更するかどうか
		final boolean measureMatchParentChildren =
			MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY ||
			MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY;

		int width = MeasureSpec.getSize(widthMeasureSpec);
		int height = MeasureSpec.getSize(heightMeasureSpec);
		if (mDisplayMode == MODE_SPLIT) {
//			if (DEBUG) Log.v(TAG, String.format("onMeasure:spec(%dx%d)", width, height));
			if (mOrientation == VERTICAL) {
				height = height >>> 1;
			} else {
				width = width >>> 1;
			}
//			if (DEBUG) Log.v(TAG, String.format("onMeasure:spec(%dx%d)", width, height));
		}
		final int childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.getMode(widthMeasureSpec));
		final int childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.getMode(heightMeasureSpec));

		int maxHeight = 0;
		int maxWidth = 0;
		int childState = 0;

		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			final View child = getChildAt(i);
			if (child.getVisibility() != GONE) {
				measureChildWithMargins(child, childWidthSpec, 0, childHeightSpec, 0);
				if ((mDisplayMode == MODE_SPLIT)
					|| (((mDisplayMode == MODE_SELECT_1) || (mDisplayMode == MODE_SINGLE_1)) && (child == mChild1))
					|| (((mDisplayMode == MODE_SELECT_2) || (mDisplayMode == MODE_SINGLE_1)) && (child == mChild2)) ) {

					final MarginLayoutParams lp = (MarginLayoutParams)child.getLayoutParams();
					maxWidth = Math.max(maxWidth,
						child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin);
					maxHeight = Math.max(maxHeight,
						child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin);
				}
				childState = combineMeasuredStates(childState, child.getMeasuredState());
//				if (measureMatchParentChildren) {
//					if (lp.width == LayoutParams.MATCH_PARENT ||
//						lp.height == LayoutParams.MATCH_PARENT) {
//						mMatchParentChildren.add(child);
//					}
//				}
			}
  		}

		if (mDisplayMode == MODE_SPLIT) {
//			if (DEBUG) Log.v(TAG, String.format("onMeasure:max(%dx%d)", maxWidth, maxHeight));
			if (mOrientation == VERTICAL) {
				maxHeight = maxHeight << 1;
			} else {
				maxWidth = maxWidth << 1;
			}
//			if (DEBUG) Log.v(TAG, String.format("onMeasure:max(%dx%d)", maxWidth, maxHeight));
		}

		// パディング分を追加
		maxWidth += getPaddingLeft() + getPaddingRight();
		maxHeight += getPaddingTop() + getPaddingBottom();

		// 自分の最小幅・最小高さ設定を考慮する
		maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
		maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

		// フォアグラウンドDrawableの最小幅最小高さを考慮する
		final Drawable drawable = getForeground();
		if (drawable != null) {
			maxHeight = Math.max(maxHeight, drawable.getMinimumHeight());
			maxWidth = Math.max(maxWidth, drawable.getMinimumWidth());
		}

		// 自分の大きさをセット
		setMeasuredDimension(
			resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
			resolveSizeAndState(maxHeight, heightMeasureSpec, childState << MEASURED_HEIGHT_STATE_SHIFT) );

//		if (DEBUG) Log.v(TAG, String.format("onMeasure:measured(%dx%d)", getMeasuredWidth(), getMeasuredHeight()));
		// ここから子Viewの大きさを決定
		// パディング分を差し引いて子View用のサイズを計算
		final int maxChildWidth = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
		final int maxChildHeight = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();
		final int n = getChildCount();
		if (n == 1) {
			// 1個しか子Viewが無い時
			callChildMeasure(mChild1, maxChildWidth, maxChildHeight, widthMeasureSpec, heightMeasureSpec);
		} else if (n > 0) {
			// 子Viewが2個ある時
			switch (mDisplayMode) {
			case MODE_SELECT_1:
			case MODE_SINGLE_1:
				onMeasureSelect1(maxChildWidth, maxChildHeight, widthMeasureSpec, heightMeasureSpec);
				break;
			case MODE_SELECT_2:
			case MODE_SINGLE_2:
				onMeasureSelect2(maxChildWidth, maxChildHeight, widthMeasureSpec, heightMeasureSpec);
				break;
//			case MODE_SPLIT:
			default:
				onMeasureSplit(maxChildWidth, maxChildHeight, widthMeasureSpec, heightMeasureSpec);
				break;
			}
		} else {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		}
	}

	private void onMeasureSplit(final int maxWidth, final int maxHeight, final int widthMeasureSpec, final int heightMeasureSpec) {
//		if (DEBUG) Log.v(TAG, "onMeasureSplit:");
		switch (mOrientation) {
		case VERTICAL:	// 縦分割
			onMeasureVertical(maxWidth, maxHeight, widthMeasureSpec, heightMeasureSpec);
			break;
		default:		// 横分割
			onMeasureHorizontal(maxWidth, maxHeight, widthMeasureSpec, heightMeasureSpec);
			break;
		}
	}

	private void onMeasureSelect1(final int maxWidth, final int maxHeight, final int widthMeasureSpec, final int heightMeasureSpec) {
//		if (DEBUG) Log.v(TAG, "onMeasureSelect1:");
		final View ch1 = mFlipChildPos ? mChild2 : mChild1;
		final View ch2 = mFlipChildPos ? mChild1 : mChild2;
		callChildMeasure(ch1, maxWidth, maxHeight, widthMeasureSpec, heightMeasureSpec);
		if (mEnableSubWindow) {
			callChildMeasure(ch2, (int)(maxWidth * mSubWindowScale), (int)(maxHeight * mSubWindowScale), widthMeasureSpec, heightMeasureSpec);
		}
	}

	private void onMeasureSelect2(final int maxWidth, final int maxHeight, final int widthMeasureSpec, final int heightMeasureSpec) {
//		if (DEBUG) Log.v(TAG, "onMeasureSelect2:");
		final View ch1 = mFlipChildPos ? mChild2 : mChild1;
		final View ch2 = mFlipChildPos ? mChild1 : mChild2;
		callChildMeasure(ch2, maxWidth, maxHeight, widthMeasureSpec, heightMeasureSpec);
		if (mEnableSubWindow) {
			callChildMeasure(ch1, (int)(maxWidth * mSubWindowScale), (int)(maxHeight * mSubWindowScale), widthMeasureSpec, heightMeasureSpec);
		}
	}

	/**
	 * 横分割時の子Viewサイズを設定
	 * @param maxWidth
	 * @param maxHeight
	 * @param widthMeasureSpec
	 * @param heightMeasureSpec
	 */
	private void onMeasureHorizontal(final int maxWidth, final int maxHeight, final int widthMeasureSpec, final int heightMeasureSpec) {
//		if (DEBUG) Log.v(TAG, "onMeasureHorizontal:");
		final View ch1 = mFlipChildPos ? mChild2 : mChild1;
		final View ch2 = mFlipChildPos ? mChild1 : mChild2;
		callChildMeasure(ch1, maxWidth >>> 1, maxHeight, widthMeasureSpec, heightMeasureSpec);
		callChildMeasure(ch2, maxWidth >>> 1, maxHeight, widthMeasureSpec, heightMeasureSpec);
	}

	/**
	 * 縦分割時の子Viewサイズを設定
	 * @param maxWidth
	 * @param maxHeight
	 * @param widthMeasureSpec
	 * @param heightMeasureSpec
	 */
	private void onMeasureVertical(final int maxWidth, final int maxHeight, final int widthMeasureSpec, final int heightMeasureSpec) {
//		if (DEBUG) Log.v(TAG, "onMeasureVertical:");
		final View ch1 = mFlipChildPos ? mChild2 : mChild1;
		final View ch2 = mFlipChildPos ? mChild1 : mChild2;
		callChildMeasure(ch1, maxWidth, maxHeight >>> 1, widthMeasureSpec, heightMeasureSpec);
		callChildMeasure(ch2, maxWidth, maxHeight >>> 1, widthMeasureSpec, heightMeasureSpec);
	}

	/**
	 * 指定した子Viewの#measureを呼び出す
	 * @param child
	 * @param widthMeasureSpec
	 * @param heightMeasureSpec
	 */
	private void callChildMeasure(final View child, final int maxWidth, final int maxHeight, final int widthMeasureSpec, final int heightMeasureSpec) {
		final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

		final int childWidthMeasureSpec;
		if (lp.width == LayoutParams.MATCH_PARENT) {
			final int width = Math.min(maxWidth, getMeasuredWidth()
				- getPaddingLeft() - getPaddingRight()
				- lp.leftMargin - lp.rightMargin);
			childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
		} else {
			final int spec = getChildMeasureSpec(widthMeasureSpec,
				getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin,
				lp.width);
			final int w = MeasureSpec.getSize(spec);
			childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(Math.min(w, maxWidth), MeasureSpec.getMode(spec));
		}

		final int childHeightMeasureSpec;
		if (lp.height == LayoutParams.MATCH_PARENT) {
			final int height = Math.min(maxHeight, getMeasuredHeight()
              - getPaddingTop() - getPaddingBottom()
              - lp.topMargin - lp.bottomMargin);
			childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
		} else {
			final int spec = getChildMeasureSpec(heightMeasureSpec,
				getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin,
				lp.height);
			final int h = MeasureSpec.getSize(spec);
			childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(Math.min(h, maxHeight), MeasureSpec.getMode(spec));
		}

		child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
	}

//--------------------------------------------------------------------------------
	@Override
	protected void onLayout(final boolean changed, final int left, final int top, final int right, final int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		final int _left = left + getPaddingLeft();
		final int _top = top + getPaddingTop();
		final int _right = right - getPaddingRight();
		final int _bottom = bottom - getPaddingBottom();
		// ここで子Viewの位置を決定
		// 分割配置
		final int n = getChildCount();
		if (n == 1) {
			// 1個しか子Viewが無い時
			callChildLayout(mChild1, changed, _left, _top, _right, _bottom);
		} else if (n > 0) {
			// 子Viewが2個ある時
			switch (mDisplayMode) {
			case MODE_SELECT_1:
			case MODE_SINGLE_1:
				onLayoutSelect1(changed, _left, _top, _right, _bottom);
				break;
			case MODE_SELECT_2:
			case MODE_SINGLE_2:
				onLayoutSelect2(changed, _left, _top, _right, _bottom);
				break;
//			case MODE_SPLIT:
			default:
				onLayoutSplit(changed, _left, _top, _right, _bottom);
				break;
			}
		} else {
			super.onLayout(changed, left, top, right, bottom);
		}
	}

	private void onLayoutSplit(final boolean changed, final int left, final int top, final int right, final int bottom) {
//		if (DEBUG) Log.v(TAG, "onLayoutSplit:");
		switch(mOrientation) {
		case VERTICAL:	// 縦分割
			onLayoutVertical(changed, left, top, right, bottom);
			break;
		default:		// 横分割
			onLayoutHorizontal(changed, left, top, right, bottom);
		}
	}

	private final Rect mChildRect = new Rect();
	private void onLayoutSelect1(final boolean changed, final int left, final int top, final int right, final int bottom) {
//		if (DEBUG) Log.v(TAG, "onLayoutSelect1:");
		final View ch1 = mFlipChildPos ? mChild2 : mChild1;
		final View ch2 = mFlipChildPos ? mChild1 : mChild2;
		final int paddingLeft = getPaddingLeft();
		final int paddingTop = getPaddingTop();
		callChildLayout(ch1, changed, left - paddingLeft, top - paddingTop, right - paddingLeft, bottom - paddingTop);
		if (mEnableSubWindow) {
			// child2の位置はchild1の左下
			final int _bottom = ch1.getBottom();
			final int _right = ch1.getRight();
			final int w = ch2.getMeasuredWidth(); // int)((right - left) * mSubWindowScale);
			final int h = ch2.getMeasuredHeight(); // int)((bottom - top) * mSubWindowScale);
			switch (mOrientation) {
			case VERTICAL:
				callChildLayout(ch2, changed, _right - w, _bottom - h, _right, _bottom);
				break;
//			case HORIZONTAL:
			default:
				callChildLayout(ch2, changed, _right - w, _bottom - h, _right, _bottom);
			}
		}
	}

	private void onLayoutSelect2(final boolean changed, final int left, final int top, final int right, final int bottom) {
//		if (DEBUG) Log.v(TAG, "onLayoutSelect2:");
		final View ch1 = mFlipChildPos ? mChild2 : mChild1;
		final View ch2 = mFlipChildPos ? mChild1 : mChild2;
		final int paddingLeft = getPaddingLeft();
		final int paddingTop = getPaddingTop();
		callChildLayout(ch2, changed, left - paddingLeft, top - paddingTop, right - paddingLeft, bottom - paddingTop);
		if (mEnableSubWindow) {
			// child1の位置はchild2の左下か右上
			final int _left = ch2.getLeft();
			final int _top = ch2.getTop();
			final int _right = ch2.getRight();
			final int _bottom = ch2.getBottom();
			final int w = ch1.getMeasuredWidth(); // int)((right - left) * mSubWindowScale);
			final int h = ch1.getMeasuredHeight(); // int)((bottom - top) * mSubWindowScale);
			switch (mOrientation) {
			case VERTICAL:
				callChildLayout(ch1, changed, _right - w, _top, _right, _top + h);
				break;
//			case HORIZONTAL:
			default:
				callChildLayout(ch1, changed, _left, _bottom - h, _left + w, _bottom);
				break;
			}
		}
	}

	/**
	 * 横分割時に子Viewの位置を設定
	 * @param changed
	 * @param left
	 * @param top
	 * @param right
	 * @param bottom
	 */
	private void onLayoutHorizontal(final boolean changed, final int left, final int top, final int right, final int bottom) {
//		if (DEBUG) Log.v(TAG, "onLayoutHorizontal:");
		final View ch1 = mFlipChildPos ? mChild2 : mChild1;
		final View ch2 = mFlipChildPos ? mChild1 : mChild2;
		final int w2 = (right - left) >>> 1;
		final int paddingLeft = getPaddingLeft();
		final int paddingTop = getPaddingTop();
		callChildLayout(ch1, changed, left - paddingLeft, top - paddingTop, left - paddingLeft + w2, bottom - paddingTop);
		callChildLayout(ch2, changed, left - paddingLeft + w2, top - paddingTop, right - paddingLeft, bottom - paddingTop);
	}

	/**
	 * 縦分割時に子Viewの位置を設定
	 * @param changed
	 * @param left
	 * @param top
	 * @param right
	 * @param bottom
	 */
	private void onLayoutVertical(final boolean changed, final int left, final int top, final int right, final int bottom) {
//		if (DEBUG) Log.v(TAG, "onLayoutVertical:");
		final View ch1 = mFlipChildPos ? mChild2 : mChild1;
		final View ch2 = mFlipChildPos ? mChild1 : mChild2;
		final int h2 = (bottom - top) >>> 1;
		final int paddingLeft = getPaddingLeft();
		final int paddingTop = getPaddingTop();
		callChildLayout(ch1, changed, left - paddingLeft, top - paddingTop, right - paddingLeft, top - paddingTop + h2);
		callChildLayout(ch2, changed, left - paddingLeft, top - paddingTop + h2, right -paddingLeft, bottom - paddingTop);
	}

	/**
	 * 指定した子Viewの#layoutを呼び出す
	 * @param child
	 * @param changed
	 * @param left
	 * @param top
	 * @param right
	 * @param bottom
	 */
	@SuppressLint("NewApi")
	private void callChildLayout(final View child, final boolean changed, final int left, final int top, final int right, final int bottom) {
		final LayoutParams lp = (LayoutParams) child.getLayoutParams();

		final int width = child.getMeasuredWidth();
		final int height = child.getMeasuredHeight();

		int childLeft;
		int childTop;

		int gravity = lp.gravity;
		if (gravity == -1) {
			gravity = DEFAULT_CHILD_GRAVITY;
		}

		final int layoutDirection = BuildCheck.isAndroid4_2() ? getLayoutDirection() : 0;
		final int absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection);
		final int verticalGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;

		switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
		case Gravity.CENTER_HORIZONTAL:
			childLeft = left + (right - left - width) / 2 +
				lp.leftMargin - lp.rightMargin;
			break;
		case Gravity.RIGHT:
			childLeft = right - width - lp.rightMargin;
			break;
		case Gravity.LEFT:
		default:
			childLeft = left + lp.leftMargin;
		}

		switch (verticalGravity) {
		case Gravity.TOP:
			childTop = top + lp.topMargin;
			break;
		case Gravity.CENTER_VERTICAL:
			childTop = top + (bottom - top - height) / 2 +
				lp.topMargin - lp.bottomMargin;
			break;
		case Gravity.BOTTOM:
			childTop = bottom - height - lp.bottomMargin;
			break;
		default:
			childTop = top + lp.topMargin;
		}

		child.layout(childLeft, childTop, childLeft + width, childTop + height);
	}

//--------------------------------------------------------------------------------
	public void startLayout() {
		if (isInEditMode() || (getChildCount() < 2)) {
			requestLayout();
		}
		post(new Runnable() {
			@Override
			public void run() {
				startLayoutOnUI();
			}
		});
	}

	private void startLayoutOnUI() {
		// FIXME ここでアニメーションを実行する。LayoutAnimationControllerを使う?
		// 今はrequestLayoutを呼び出しているのですぐに大きさが切り替わる
		final View ch1 = mFlipChildPos ? mChild2 : mChild1;
		final View ch2 = mFlipChildPos ? mChild1 : mChild2;
		try {
			switch (mDisplayMode) {
			case MODE_SELECT_1:
			case MODE_SINGLE_1:
				removeView(ch1);
				addView(ch1, 0);
				ch1.setVisibility(VISIBLE);
				ch2.setVisibility(mEnableSubWindow && (mDisplayMode != MODE_SINGLE_1) ? VISIBLE: INVISIBLE);
				break;
			case MODE_SELECT_2:
			case MODE_SINGLE_2:
				removeView(ch2);
				addView(ch2, 0);
				ch1.setVisibility(mEnableSubWindow && (mDisplayMode != MODE_SINGLE_2) ? VISIBLE: INVISIBLE);
				ch2.setVisibility(VISIBLE);
				break;
			case MODE_SPLIT:
				ch1.setVisibility(VISIBLE);
				ch2.setVisibility(VISIBLE);
				break;
			}
		} finally {
			mChild1 = mFlipChildPos ? ch2 : ch1;
			mChild2 = mFlipChildPos ? ch1 : ch2;
		}

		requestLayout();
	}

	private void cancelAnimation() {
		synchronized (mSync) {
			if (mAnimator != null) {
				mAnimator.cancel();
				mAnimator = null;
			}
		}
	}

	/**
	 * 表示モード切替時のアニメーション用コールバックリスナー
	 */
	private final Animator.AnimatorListener mAnimatorListener
		= new Animator.AnimatorListener() {
		@Override
		public void onAnimationStart(final Animator animator) {

		}

		@Override
		public void onAnimationEnd(final Animator animator) {
			synchronized (mSync) {
				mAnimator = null;
			}
			// 実際のレイアウト処理を要求する
			requestLayout();
		}

		@Override
		public void onAnimationCancel(final Animator animator) {
			synchronized (mSync) {
				mAnimator = null;
			}
			// 実際のレイアウト処理を要求する
			requestLayout();
		}

		@Override
		public void onAnimationRepeat(final Animator animator) {

		}
	};
}
