package com.serenegiant.mediastore;
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

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.serenegiant.common.R;

import java.io.IOException;

import static com.serenegiant.mediastore.MediaStoreHelper.*;

public class MediaStoreImageAdapter extends PagerAdapter {
	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = MediaStoreImageAdapter.class.getSimpleName();

	private final LayoutInflater mInflater;
	private final int mLayoutId;
	private final ContentResolver mCr;
	private final MyAsyncQueryHandler mQueryHandler;
	protected boolean mDataValid;
	protected int mRowIDColumn;
	protected ChangeObserver mChangeObserver;
	protected DataSetObserver mDataSetObserver;
	private Cursor mCursor;
	private String mSelection = SELECTIONS[MEDIA_IMAGE];	// 静止画のみ有効
	private String[] mSelectionArgs = null;

	private boolean mShowTitle;

	public MediaStoreImageAdapter(final Context context, final int id_layout) {
		this(context, id_layout, true);
	}

	public MediaStoreImageAdapter(final Context context,
		final int id_layout, final boolean needQuery) {

		mInflater = LayoutInflater.from(context);
		mLayoutId = id_layout;
		mCr = context.getContentResolver();
		mQueryHandler = new MyAsyncQueryHandler(mCr, this);
		if (needQuery) {
			startQuery();
		}
	}

	@Override
	protected void finalize() throws Throwable {
		changeCursor(null);
		super.finalize();
	}

	@Override
	public int getCount() {
		if (mDataValid && mCursor != null) {
			return mCursor.getCount();
		} else {
			return 0;
		}
	}

	@NonNull
	@Override
	public Object instantiateItem(@NonNull final ViewGroup container,
		final int position) {

		if (DEBUG) Log.v(TAG, "instantiateItem:position=" + position);
		final View view = mInflater.inflate(mLayoutId, container, false);
		ViewHolder holder;
		if (view != null) {
			container.addView(view);
			holder = (ViewHolder)view.getTag();
			if (holder == null) {
				holder = new ViewHolder();
			}
			final TextView tv = holder.mTitleView = view.findViewById(R.id.title);
			final ImageView iv = holder.mImageView = view.findViewById(R.id.thumbnail);
			if (holder.info == null) {
				holder.info = new MediaStoreHelper.MediaInfo();
			}
			holder.info.loadFromCursor(getCursor(position));
			// ローカルキャッシュ
			Drawable drawable = iv.getDrawable();
			if (!(drawable instanceof MediaStoreHelper.LoaderDrawable)) {
				drawable = createLoaderDrawable(mCr, holder.info);
				iv.setImageDrawable(drawable);
			}
			((MediaStoreHelper.LoaderDrawable)drawable).startLoad(holder.info.mediaType, 0, holder.info.id);
			if (tv != null) {
				tv.setVisibility(mShowTitle ? View.VISIBLE : View.GONE);
				if (mShowTitle) {
					tv.setText(holder.info.title);
				}
			}
		}
		return view;
	}

	@Override
	public void destroyItem(@NonNull final ViewGroup container,
		final int position, @NonNull final Object object) {

		if (DEBUG) Log.v(TAG, "destroyItem:position=" + position);
		if (object instanceof View) {
			container.removeView((View)object);
		}
	}

	@Override
	public int getItemPosition(@NonNull final Object object) {
		// FIXME ここはobject=ViewからMediaInfo#idを取得してpositionを検索し直さないとだめかも
		return super.getItemPosition(object);
	}

	public int getItemPositionFromID(final long id) {
		if (DEBUG) Log.v(TAG, "getItemPositionFromID:id=" + id);
		int result = -1;
		final Cursor cursor = mCr.query(QUERY_URI, PROJ_MEDIA, mSelection, mSelectionArgs, null);
		if (cursor != null) {
			try {
				if (cursor.moveToFirst()) {
					for (int ix = 0 ; ; ix++) {
						if (cursor.getLong(PROJ_INDEX_ID) == id) {
							if (DEBUG) Log.v(TAG, "getItemPositionFromID:found=" + ix);
							result = ix;
							break;
						}
						if (!cursor.moveToNext()) break;
					}
				}
			} finally {
				cursor.close();
			}
		}
		return result;
	}

	@Override
	public boolean isViewFromObject(@NonNull final View view,
		@NonNull final Object object) {

		return view.equals(object);
	}

	protected void changeCursor(@Nullable final Cursor cursor) {
		final Cursor old = swapCursor(cursor);
		if ((old != null) && !old.isClosed()) {
			old.close();
		}
	}

	protected Cursor getCursor(final int position) {
		if (mDataValid && mCursor != null) {
			mCursor.moveToPosition(position);
			return mCursor;
		} else {
			return null;
  		}
	}

	protected Cursor swapCursor(final Cursor newCursor) {
		if (newCursor == mCursor) {
			return null;
		}
		Cursor oldCursor = mCursor;
		if (oldCursor != null) {
			if (mChangeObserver != null) oldCursor.unregisterContentObserver(mChangeObserver);
			if (mDataSetObserver != null) oldCursor.unregisterDataSetObserver(mDataSetObserver);
		}
		mCursor = newCursor;
		if (newCursor != null) {
			if (mChangeObserver != null) newCursor.registerContentObserver(mChangeObserver);
			if (mDataSetObserver != null) newCursor.registerDataSetObserver(mDataSetObserver);
			mRowIDColumn = newCursor.getColumnIndexOrThrow("_id");
			mDataValid = true;
			// notify the observers about the new cursor
			notifyDataSetChanged();
		} else {
			mRowIDColumn = -1;
			mDataValid = false;
			// notify the observers about the lack of a data set
			notifyDataSetInvalidated();
		}
		return oldCursor;
	}

	public void notifyDataSetInvalidated() {
//		mDataSetObservable.notifyInvalidated();
	}

	public void startQuery() {
		mQueryHandler.requery();
	}

	protected MediaStoreHelper.LoaderDrawable createLoaderDrawable(
		final ContentResolver cr, final MediaInfo info) {

		return new ImageLoaderDrawable(cr, info.width, info.height);
	}

	private static final class ViewHolder {
		TextView mTitleView;
		ImageView mImageView;
		MediaStoreHelper.MediaInfo info;
	}

	private static final class MyAsyncQueryHandler extends AsyncQueryHandler {
		private final MediaStoreImageAdapter mAdapter;
		public MyAsyncQueryHandler(final ContentResolver cr,
			final MediaStoreImageAdapter adapter) {

			super(cr);
			mAdapter = adapter;
		}

		public void requery() {
			synchronized (mAdapter) {
				startQuery(0, mAdapter, QUERY_URI, PROJ_MEDIA,
					mAdapter.mSelection, mAdapter.mSelectionArgs, null);
			}
		}

		@Override
		protected void onQueryComplete(final int token,
			final Object cookie, final Cursor cursor) {

//			super.onQueryComplete(token, cookie, cursor);	// this is empty method
			final Cursor oldCursor = mAdapter.swapCursor(cursor);
			if ((oldCursor != null) && !oldCursor.isClosed())
				oldCursor.close();
		}

	}

	private class ChangeObserver extends ContentObserver {
		public ChangeObserver() {
			super(new Handler());
		}

		@Override
		public boolean deliverSelfNotifications() {
			return true;
		}

		@Override
		public void onChange(boolean selfChange) {
			startQuery();
		}
	}

	private class MyDataSetObserver extends DataSetObserver {
		@Override
		public void onChanged() {
			mDataValid = true;
			notifyDataSetChanged();
		}

		@Override
		public void onInvalidated() {
			mDataValid = false;
			notifyDataSetInvalidated();
		}
	}

	private static class ImageLoaderDrawable extends MediaStoreHelper.LoaderDrawable {
		public ImageLoaderDrawable(final ContentResolver cr,
			final int width, final int height) {

			super(cr, width, height);
		}

		@Override
		protected MediaStoreHelper.ImageLoader createThumbnailLoader() {
			return new MyImageLoader(this);
		}

		@Override
		protected Bitmap checkBitmapCache(final int hashCode, final long id) {
			return null;
		}
	}

	private static class MyImageLoader extends MediaStoreHelper.ImageLoader {
		public MyImageLoader(final ImageLoaderDrawable parent) {
			super(parent);
		}

		@Override
		protected Bitmap loadBitmap(final ContentResolver cr,
			final int mediaType, final int hashCode,
			final long id, final int requestWidth, final int requestHeight) {

			Bitmap result = null;
			try {
				result = MediaStoreHelper.getImage(cr, id, requestWidth, requestHeight);
				if (result != null) {
					final int w = result.getWidth();
					final int h = result.getHeight();
					final Rect bounds = new Rect();
					mParent.copyBounds(bounds);
					final int cx = bounds.centerX();
					final int cy = bounds.centerY();
					bounds.set(cx - w / 2, cy - h / w, cx + w / 2, cy + h / 2);
					mParent.onBoundsChange(bounds);
				}
			} catch (final IOException e) {
				Log.w(TAG, e);
			}
			return result;
		}
	}

}
