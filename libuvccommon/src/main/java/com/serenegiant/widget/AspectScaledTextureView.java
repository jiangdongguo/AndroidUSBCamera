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
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

import com.serenegiant.common.R;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class AspectScaledTextureView extends TextureView
	implements TextureView.SurfaceTextureListener, IAspectRatioView, IScaledView, ITextureView {
	
	private static final String TAG = AspectScaledTextureView.class.getSimpleName();

	protected final Matrix mImageMatrix = new Matrix();
	private int mScaleMode = SCALE_MODE_KEEP_ASPECT;
	private double mRequestedAspect = -1.0;		// initially use default window size
	private volatile boolean mHasSurface;	// プレビュー表示用のSurfaceTextureが存在しているかどうか
	private final Set<SurfaceTextureListener> mListeners = new CopyOnWriteArraySet<SurfaceTextureListener>();

	public AspectScaledTextureView(final Context context) {
		this(context, null, 0);
	}

	public AspectScaledTextureView(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public AspectScaledTextureView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		final TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.AspectScaledTextureView, defStyleAttr, 0);
		try {
			mRequestedAspect = a.getFloat(R.styleable.AspectScaledTextureView_aspect_ratio, -1.0f);
			mScaleMode = a.getInt(R.styleable.AspectScaledTextureView_scale_mode, SCALE_MODE_KEEP_ASPECT);
		} finally {
			a.recycle();
		}
		super.setSurfaceTextureListener(this);
	}

	/**
	 * アスペクト比を保つように大きさを決める
	 */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//		if (DEBUG) Log.v(TAG, "onMeasure:mRequestedAspect=" + mRequestedAspect);
// 		要求されたアスペクト比が負の時(初期生成時)は何もしない
		if (mRequestedAspect > 0 && (mScaleMode == SCALE_MODE_KEEP_ASPECT)) {
			int initialWidth = MeasureSpec.getSize(widthMeasureSpec);
			int initialHeight = MeasureSpec.getSize(heightMeasureSpec);
			final int horizPadding = getPaddingLeft() + getPaddingRight();
			final int vertPadding = getPaddingTop() + getPaddingBottom();
			initialWidth -= horizPadding;
			initialHeight -= vertPadding;

			final double viewAspectRatio = (double)initialWidth / initialHeight;
			final double aspectDiff = mRequestedAspect / viewAspectRatio - 1;

			// 計算誤差が生じる可能性が有るので指定した値との差が小さければそのままにする
			if (Math.abs(aspectDiff) > 0.01) {
				if (aspectDiff > 0) {
					// 幅基準で高さを決める
					initialHeight = (int) (initialWidth / mRequestedAspect);
				} else {
					// 高さ基準で幅を決める
					initialWidth = (int) (initialHeight * mRequestedAspect);
				}
				initialWidth += horizPadding;
				initialHeight += vertPadding;
				widthMeasureSpec = MeasureSpec.makeMeasureSpec(initialWidth, MeasureSpec.EXACTLY);
				heightMeasureSpec = MeasureSpec.makeMeasureSpec(initialHeight, MeasureSpec.EXACTLY);
			}
		}

		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	private int prevWidth = -1;
	private int prevHeight = -1;
	@Override
	protected void onLayout(final boolean changed, final int left, final int top, final int right, final int bottom) {
		super.onLayout(changed, left, top, right, bottom);
//		if (DEBUG) Log.v(TAG, "onLayout:width=" + getWidth() + ",height=" + getHeight());
//		if view size(width|height) is zero(the view size not decided yet)
		if (getWidth() == 0 || getHeight() == 0) return;
		if (prevWidth != getWidth() || prevHeight != getHeight()) {
			prevWidth = getWidth();
			prevHeight = getHeight();
			onResize(prevWidth, prevHeight);
		}
		init();
	}
	
	private SurfaceTextureListener mSurfaceTextureListener;
	/**
	 * @param listener
	 */
	@Override
	public final void setSurfaceTextureListener(final SurfaceTextureListener listener) {
		unregister(mSurfaceTextureListener);
		mSurfaceTextureListener = listener;
		register(listener);
	}
	
	@Override
	public void register(final SurfaceTextureListener listener) {
		if (listener != null) {
			mListeners.add(listener);
		}
	}
	
	@Override
	public void unregister(final SurfaceTextureListener listener) {
		mListeners.remove(listener);
	}

	protected void onResize(final int width, final int height) {
	}

//================================================================================
// IAspectRatioView
//================================================================================
	@Override
	public void onSurfaceTextureAvailable(final SurfaceTexture surface, final int width, final int height) {
		mHasSurface = true;
		init();
		for (final SurfaceTextureListener listener: mListeners) {
			try {
				listener.onSurfaceTextureAvailable(surface, width, height);
			} catch (final Exception e) {
				mListeners.remove(listener);
				Log.w(TAG, e);
			}
		}
	}

	@Override
	public void onSurfaceTextureSizeChanged(final SurfaceTexture surface, final int width, final int height) {
		for (final SurfaceTextureListener listener: mListeners) {
			try {
				listener.onSurfaceTextureSizeChanged(surface, width, height);
			} catch (final Exception e) {
				mListeners.remove(listener);
				Log.w(TAG, e);
			}
		}
	}

	@Override
	public boolean onSurfaceTextureDestroyed(final SurfaceTexture surface) {
		mHasSurface = false;
		for (final SurfaceTextureListener listener: mListeners) {
			try {
				listener.onSurfaceTextureDestroyed(surface);
			} catch (final Exception e) {
				mListeners.remove(listener);
				Log.w(TAG, e);
			}
		}
		return false;
	}

	@Override
	public void onSurfaceTextureUpdated(final SurfaceTexture surface) {
		for (final SurfaceTextureListener listener: mListeners) {
			try {
				listener.onSurfaceTextureUpdated(surface);
			} catch (final Exception e) {
				mListeners.remove(listener);
				Log.w(TAG, e);
			}
		}
	}

//================================================================================
// IAspectRatioView
//================================================================================
	/**
	 * アスペクト比を設定する。アスペクト比=<code>幅 / 高さ</code>.
	 */
	@Override
	public void setAspectRatio(final double aspectRatio) {
//		if (DEBUG) Log.v(TAG, "setAspectRatio");
		if (mRequestedAspect != aspectRatio) {
			mRequestedAspect = aspectRatio;
			requestLayout();
		}
 	}

	@Override
	public void setAspectRatio(final int width, final int height) {
		setAspectRatio(width / (double)height);
	}

	@Override
	public double getAspectRatio() {
		return mRequestedAspect;
	}

//================================================================================
// IScaledView
//================================================================================

	@Override
	public void setScaleMode(final int scale_mode) {
		if (mScaleMode != scale_mode) {
			mScaleMode = scale_mode;
			requestLayout();
		}
	}

	@Override
	public int getScaleMode() {
		return mScaleMode;
	}

//================================================================================
// 実際の実装
//================================================================================
	/**
	 * 拡大縮小回転状態をリセット
	 */
	protected void init() {
		// update image size
		// current implementation of ImageView always hold its image as a Drawable
		// (that can get ImageView#getDrawable)
		// therefore update the image size from its Drawable
		// set limit rectangle that the image can move
		final int view_width = getWidth();
		final int view_height = getHeight();
		// apply matrix
		mImageMatrix.reset();
		switch (mScaleMode) {
		case SCALE_MODE_KEEP_ASPECT:
		case SCALE_MODE_STRETCH_TO_FIT:
			// 何もしない
			break;
		case SCALE_MODE_CROP: // FIXME もう少し式を整理できそう
			final double video_width = mRequestedAspect * view_height;
			final double video_height = view_height;
			final double scale_x = view_width / video_width;
			final double scale_y = view_height / video_height;
			final double scale = Math.max(scale_x,  scale_y);	// SCALE_MODE_CROP
//			final double scale = Math.min(scale_x, scale_y);	// SCALE_MODE_KEEP_ASPECT
			final double width = scale * video_width;
			final double height = scale * video_height;
//			Log.v(TAG, String.format("size(%1.0f,%1.0f),scale(%f,%f),mat(%f,%f)",
//				width, height, scale_x, scale_y, width / view_width, height / view_height));
			mImageMatrix.postScale((float)(width / view_width), (float)(height / view_height), view_width / 2, view_height / 2);
			break;
		}
		setTransform(mImageMatrix);
	}

}
