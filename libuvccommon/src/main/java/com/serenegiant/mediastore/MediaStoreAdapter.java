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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.collection.LruCache;
import androidx.cursoradapter.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.serenegiant.common.R;
import com.serenegiant.utils.ThreadPool;

import static com.serenegiant.mediastore.MediaStoreHelper.*;

public class MediaStoreAdapter extends CursorAdapter {

	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = MediaStoreAdapter.class.getSimpleName();

	// for thumbnail cache(in memory)
	// rate of memory usage for cache, 'CACHE_RATE = 8' means use 1/8 of available memory for image cache
	private static final int CACHE_RATE = 8;
	private static LruCache<String, Bitmap> sThumbnailCache;
	private int mThumbnailWidth = 200, mThumbnailHeight = 200;

	private final LayoutInflater mInflater;
	private final ContentResolver mCr;
	private final int mMemClass;
	private final int mLayoutId;
	private final MyAsyncQueryHandler mQueryHandler;
	private final int mHashCode = hashCode();
	private Cursor mMediaInfoCursor;
	private String mSelection;
	private String[] mSelectionArgs;
	private boolean mShowTitle = false;
	private int mMediaType = MEDIA_ALL;
	private final MediaInfo info = new MediaInfo();


	public MediaStoreAdapter(final Context context, final int id_layout) {
		super(context, null, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
	    mInflater = LayoutInflater.from(context);
	    mCr = context.getContentResolver();
	    mQueryHandler = new MyAsyncQueryHandler(mCr, this);
		// getMemoryClass return the available memory amounts for app as mega bytes(API >= 5)
		mMemClass = ((ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
		mLayoutId = id_layout;
		onContentChanged();
	}

	@Override
	public View newView(final Context context,
		final Cursor cursor, final ViewGroup parent) {

		// this method is called within UI thread and should return as soon as possible
		final View view = mInflater.inflate(mLayoutId, parent, false);
		getViewHolder(view);
		return view;
	}

	protected LoaderDrawable createLoaderDrawable(final ContentResolver cr) {
		return new ThumbnailLoaderDrawable(cr, mThumbnailWidth, mThumbnailHeight);
	}

	@Override
	public void bindView(final View view, final Context context, final Cursor cursor) {
		// this method is called within UI thread and should return as soon as possible
		final ViewHolder holder = getViewHolder(view);
		// ローカルキャッシュ
		final ImageView iv = holder.mImageView;
		final TextView tv = holder.mTitleView;
		Drawable drawable = iv.getDrawable();
		if (!(drawable instanceof LoaderDrawable)) {
			drawable = createLoaderDrawable(mCr);
			iv.setImageDrawable(drawable);
		}
		((LoaderDrawable)drawable).startLoad(
			cursor.getInt(PROJ_INDEX_MEDIA_TYPE), mHashCode, cursor.getLong(PROJ_INDEX_ID));
		if (tv != null) {
			tv.setVisibility(mShowTitle ? View.VISIBLE : View.GONE);
			if (mShowTitle) {
				tv.setText(cursor.getString(PROJ_INDEX_TITLE));
			}
		}
	}

	private ViewHolder getViewHolder(final View view) {
		ViewHolder holder;
		// you can use View#getTag()/setTag() instead of using View#getTag(int)/setTag(int)
		// but we assume that using getTag(int)/setTag(int) and keeping getTag()/setTag() left for user is better.
		holder = (ViewHolder)view.getTag(R.id.mediastorephotoadapter);
		if (holder == null) {
			holder = new ViewHolder();
			if (view instanceof ImageView) {
				holder.mImageView = (ImageView)view;
				view.setTag(R.id.mediastorephotoadapter, holder);
			} else {
				View v = view.findViewById(R.id.thumbnail);
				if (v instanceof ImageView)
					holder.mImageView = (ImageView)v;
				v = view.findViewById(R.id.title);
				if (v instanceof TextView)
					holder.mTitleView = (TextView)v;
				view.setTag(R.id.mediastorephotoadapter, holder);
			}
		}
		return holder;
	}
	
	@Override
	protected void finalize() throws Throwable {
		changeCursor(null);
		if (mMediaInfoCursor != null) {
			mMediaInfoCursor.close();
			mMediaInfoCursor = null;
		}
		super.finalize();
	}

	@Override
	protected void onContentChanged() {
		createBitmapCache(false);
		mQueryHandler.requery();
	}

	/**
	 * return thumbnail image at specific position.
	 * this method is synchronously executed and may take time
	 * @return null if the position value is out of range etc. 
	 */
	@Override
	public Bitmap getItem(final int position) {
		Bitmap result = null;

		getMediaInfo(position, info);
		if (info.mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
			// 静止画の場合のサムネイル取得
			try {
				result = getImageThumbnail(mCr, mHashCode,
					getItemId(position), mThumbnailWidth, mThumbnailHeight);
			} catch (final FileNotFoundException e) {
				Log.w(TAG, e);
			} catch (final IOException e) {
				Log.w(TAG, e);
			}
		} else if (info.mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
			// 動画の場合のサムネイル取得
			try {
				result = getVideoThumbnail(mCr, mHashCode,
					getItemId(position), mThumbnailWidth, mThumbnailHeight);
			} catch (final FileNotFoundException e) {
				Log.w(TAG, e);
			} catch (final IOException e) {
				Log.w(TAG, e);
			}
		}
		if (DEBUG && (result == null)) {
			Log.w(TAG, "failed to getItem(" + info.title + ") at position=" + position);
		}
		return result;
	}

	public int getPositionFromId(final long id) {
		int result = -1;
		final int n = getCount();
		final MediaInfo info = new MediaInfo();
		for (int i = 0; i < n; i++) {
			getMediaInfo(i, info);
			if (info.id == id) {
				result = i;
				break;
			}
		}
		return result;
	}

	/**
	 * return image with specific size(only scale-down or original size are available now)
	 * if width=0 and height=0, return image with original size.
	 * this method is synchronously executed and may take time
	 * @return null if the position value is out of range etc. 
	 * @param position
	 * @param width
	 * @param height
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public Bitmap getImage(final int position, final int width, final int height)
		throws FileNotFoundException, IOException {

		return MediaStoreHelper.getImage(mCr, getItemId(position), width, height);
	}
	
	/**
	 * get MediaInfo at specified position
	 * @param position
	 * @return
	 */
	public MediaInfo getMediaInfo(final int position) {
		return getMediaInfo(position, null);
	}

	public synchronized MediaInfo getMediaInfo(final int position, final MediaInfo info) {
		final MediaInfo _info = info != null ? info : new MediaInfo();

/*		// if you don't need to frequently call this method, temporary query may be better to reduce memory usage.
		// but it will take more time.
		final Cursor cursor = mCr.query(
			ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, getItemId(position)),
			PROJ_IMAGE, mSelection, mSelectionArgs, MediaStore.Images.Media.DEFAULT_SORT_ORDER);
		if (cursor != null) {
			try {
				if (cursor.moveToFirst()) {
					info = readMediaInfo(cursor, new MediaInfo());
				}
			} finally {
				cursor.close();
			}
		} */
		if (mMediaInfoCursor == null) {
			mMediaInfoCursor = mCr.query(
				QUERY_URI, PROJ_MEDIA,
				mSelection, mSelectionArgs, null);
		}
		if (mMediaInfoCursor.moveToPosition(position)) {
			_info.loadFromCursor(mMediaInfoCursor);
		}
		return _info;
	}

	/**
	 * set thumbnail size, if you set size to zero, the size is 96x96(MediaStore.Images.Thumbnails.MICRO_KIND)
	 * @param size
	 */
	public void setThumbnailSize(final int size) {
		if ((mThumbnailWidth != size) || (mThumbnailHeight != size)) {
			mThumbnailWidth = mThumbnailHeight = size;
			createBitmapCache(true);
			onContentChanged();
		}
	}

	/**
	 * set thumbnail size, if you set both width and height to zero, the size is 96x96(MediaStore.Images.Thumbnails.MICRO_KIND)
	 * @param width
	 * @param height
	 */
	public void setThumbnailSize(final int width, final int height) {
		if ((mThumbnailWidth != width) || (mThumbnailHeight != height)) {
			mThumbnailWidth = width;
			mThumbnailHeight = height;
			createBitmapCache(true);
			onContentChanged();
		}
	}

	public void setShowTitle(final boolean showTitle) {
		if (mShowTitle != showTitle) {
			mShowTitle = showTitle;
			onContentChanged();
		}
	}
	
	public boolean getShowTitle() {
		return mShowTitle;
	}

	public int getMediaType() {
		return mMediaType % MEDIA_TYPE_NUM;
	}
	
	public void setMediaType(final int media_type) {
		if (mMediaType != (media_type % MEDIA_TYPE_NUM)) {
			mMediaType = media_type % MEDIA_TYPE_NUM;
			onContentChanged();
		}
	}

	/**
	 * if you finish using this adapter class in your app,
	 * you can call this method to free internal thumbnail cache
	 */
	public static void destroy() {
		if (sThumbnailCache != null) {
			sThumbnailCache.evictAll();
			sThumbnailCache = null;
		}
	}

	private static final class MyAsyncQueryHandler extends AsyncQueryHandler {
		private final MediaStoreAdapter mAdapter;
		public MyAsyncQueryHandler(final ContentResolver cr, final MediaStoreAdapter adapter) {
			super(cr);
			mAdapter = adapter;
		}
		
		public void requery() {
			synchronized (mAdapter) {
				if (mAdapter.mMediaInfoCursor != null) {
					mAdapter.mMediaInfoCursor.close();
					mAdapter.mMediaInfoCursor = null;
				}
				mAdapter.mSelection = SELECTIONS[mAdapter.mMediaType % MEDIA_TYPE_NUM];
				mAdapter.mSelectionArgs = null;
				startQuery(0, mAdapter, QUERY_URI, PROJ_MEDIA,
					mAdapter.mSelection, mAdapter.mSelectionArgs, null);
			}
		}

		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
//			super.onQueryComplete(token, cookie, cursor);	// this is empty method
			final Cursor oldCursor = mAdapter.swapCursor(cursor);
			if ((oldCursor != null) && !oldCursor.isClosed())
				oldCursor.close();
		}

	}
	
	/**
	 * create thumbnail cache
	 */
	@SuppressLint("NewApi")
	private final void createBitmapCache(final boolean clear) {
		if (clear && (sThumbnailCache != null)) {
			clearBitmapCache(hashCode());
		}
		if (sThumbnailCache == null) {
			// use 1/CACHE_RATE of available memory as memory cache
			final int cacheSize = 1024 * 1024 * mMemClass / CACHE_RATE;	// [MB] => [bytes]
			sThumbnailCache = new LruCache<String, Bitmap>(cacheSize) {
				@Override
				protected int sizeOf(@NonNull String key, @NonNull Bitmap bitmap) {
					// control memory usage instead of bitmap counts
					return bitmap.getRowBytes() * bitmap.getHeight();	// [bytes]
				}
			};
		}
		ThreadPool.preStartAllCoreThreads();
	}

	private static void clearBitmapCache(final int hashCode) {
		if (sThumbnailCache != null) {
			if (hashCode != 0) {
				// 指定したhashCodeのMediaStoreAdapterインスタンスのキャッシュをクリアする
				final Map<String, Bitmap> snapshot = sThumbnailCache.snapshot();
				final String key_prefix = String.format(Locale.US, "%d_", hashCode);
				final Set<String> keys = snapshot.keySet();
				for (final String key : keys) {
					if (key.startsWith(key_prefix)) {
						// このインスタンスのキーが見つかった
						sThumbnailCache.remove(key);
					}
				}
			} else {
				// 他のMediaStoreAdapterインスタンスのキャッシュも含めてすべてクリアする
				sThumbnailCache.evictAll();
			}
			System.gc();
		}
	}

	private static String getKey(final long hashCode, final long id) {
		return String.format(Locale.US, "%d_%d", hashCode, id);
	}

	private static final Bitmap getImageThumbnail(final ContentResolver cr,
		final long hashCode, final long id, final int requestWidth, final int requestHeight)
			throws IOException {
		
		// try to get from internal thumbnail cache(in memory), this may be redundant
		final String key = getKey(hashCode, id);
		Bitmap result = sThumbnailCache.get(key);
		if (result == null) {
			if ((requestWidth <= 0) || (requestHeight <= 0)) {
				result = MediaStoreHelper.getImage(cr, id, requestWidth, requestHeight);
			} else {
				BitmapFactory.Options options = null;
				int kind = MediaStore.Images.Thumbnails.MICRO_KIND;
				if ((requestWidth > 96) || (requestHeight > 96) || (requestWidth * requestHeight > 128 * 128))
					kind = MediaStore.Images.Thumbnails.MINI_KIND;
				try {
					result = MediaStore.Images.Thumbnails.getThumbnail(cr, id, kind, options);
				} catch (final Exception e) {
					if (DEBUG) Log.w(TAG, e);
				}
			}
			if (result != null) {
				if (DEBUG) Log.v(TAG, String.format("getImageThumbnail:id=%d(%d,%d)",
					id, result.getWidth(), result.getHeight()));
				// add to internal thumbnail cache(in memory)
				sThumbnailCache.put(key, result);
			}
			
		}
		return result;
	}
	
	private static final Bitmap getVideoThumbnail(final ContentResolver cr,
		final long hashCode, final long id,
		final int requestWidth, final int requestHeight)
			throws FileNotFoundException, IOException {

		// try to get from internal thumbnail cache(in memory), this may be redundant
		final String key = getKey(hashCode, id);
		Bitmap result = sThumbnailCache.get(key);
		if (result == null) {
			BitmapFactory.Options options = null;
			int kind = MediaStore.Video.Thumbnails.MICRO_KIND;
			if ((requestWidth > 96) || (requestHeight > 96) || (requestWidth * requestHeight > 128 * 128))
				kind = MediaStore.Video.Thumbnails.MINI_KIND;
			try {
				result = MediaStore.Video.Thumbnails.getThumbnail(cr, id, kind, options);
			} catch (final Exception e) {
				if (DEBUG) Log.w(TAG, e);
			}
			if (result != null) {
				if (DEBUG) Log.v(TAG, String.format("getVideoThumbnail:id=%d(%d,%d)",
					id, result.getWidth(), result.getHeight()));
				// add to internal thumbnail cache(in memory)
				sThumbnailCache.put(key, result);
			} else {
				Log.w(TAG, "failed to get video thumbnail ofr id=" + id);
			}

		}
		return result;
	}

	private static final class ViewHolder {
		TextView mTitleView;
		ImageView mImageView;
	}

	private static class ThumbnailLoaderDrawable extends LoaderDrawable {
		public ThumbnailLoaderDrawable(final ContentResolver cr,
			final int width, final int height) {

			super(cr, width, height);
		}

		@Override
		protected ImageLoader createThumbnailLoader() {
			return new ThumbnailLoader(this);
		}

		@Override
		protected Bitmap checkBitmapCache(final int hashCode, final long id) {
			return sThumbnailCache.get(getKey(hashCode, id));
		}
	}

	private static class ThumbnailLoader extends ImageLoader {
		public ThumbnailLoader(final ThumbnailLoaderDrawable parent) {
			super(parent);
		}

		@Override
		protected Bitmap loadBitmap(final ContentResolver cr,
			final int mediaType, final int hashCode, final long id,
			final int requestWidth, final int requestHeight) {

			Bitmap result = null;
			try {
				switch (mediaType) {
				case MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE:
					result = getImageThumbnail(cr, hashCode, id, requestWidth, requestHeight);
					break;
				case MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO:
					result = getVideoThumbnail(cr, hashCode, id, requestWidth, requestHeight);
					break;
				}
			} catch (final IOException e) {
				Log.w(TAG, e);
			}
			return result;
		}
	}
}
