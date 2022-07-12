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

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import androidx.annotation.NonNull;

import com.serenegiant.utils.ThreadPool;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.FutureTask;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class MediaStoreHelper {
	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = MediaStoreHelper.class.getSimpleName();

	public static final int MEDIA_ALL = 0;
	public static final int MEDIA_IMAGE = 1;
	public static final int MEDIA_VIDEO = 2;
	protected static final int MEDIA_TYPE_NUM = 3;

	protected static final String[] PROJ_MEDIA = {
		MediaStore.Files.FileColumns._ID,				// index=0 for Cursor, column number=1 in SQL statement
		MediaStore.Files.FileColumns.TITLE,				// index=1 for Cursor, column number=2 in SQL statement
		MediaStore.Files.FileColumns.MEDIA_TYPE,		// index=2 for Cursor, column number=2 in SQL statement
		// MEDIA_TYPE_NONE, MEDIA_TYPE_IMAGE, MEDIA_TYPE_AUDIO, MEDIA_TYPE_VIDEO, MEDIA_TYPE_PLAYLIST
		MediaStore.Files.FileColumns.MIME_TYPE,			// index=3 for Cursor, column number=2 in SQL statement
		MediaStore.Files.FileColumns.DATA,				// index=4 for Cursor, column number=2 in SQL statement
		MediaStore.Files.FileColumns.DISPLAY_NAME,		// index=5 for Cursor, column number=2 in SQL statement
		MediaStore.Files.FileColumns.WIDTH,
		MediaStore.Files.FileColumns.HEIGHT,
//		MediaStore.Files.FileColumns.DATE_MODIFIED,						// index=8 for Cursor, column number=2 in SQL statement
//		MediaStore.Files.FileColumns.DATE_ADDED,						// index=9 for Cursor, column number=2 in SQL statement
	};

	protected static final String SELECTION_MEDIA_ALL
		= MediaStore.Files.FileColumns.MEDIA_TYPE + "="
		+ MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
		+ " OR "
		+ MediaStore.Files.FileColumns.MEDIA_TYPE + "="
		+ MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

	protected static final String SELECTION_MEDIA_IMAGE
		= MediaStore.Files.FileColumns.MEDIA_TYPE + "="
		+ MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;

	protected static final String SELECTION_MEDIA_VIDEO
		= MediaStore.Files.FileColumns.MEDIA_TYPE + "="
		+ MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

	// MEDIA_ALL, MEDIA_IMAGE, MEDIA_VIDEOの順にすること
	protected static final String[] SELECTIONS = {SELECTION_MEDIA_ALL, SELECTION_MEDIA_IMAGE, SELECTION_MEDIA_VIDEO};

	// these values should be fit to PROJ_MEDIA
	protected static final int PROJ_INDEX_ID = 0;
	protected static final int PROJ_INDEX_TITLE = 1;
	protected static final int PROJ_INDEX_MEDIA_TYPE = 2;
	protected static final int PROJ_INDEX_MIME_TYPE = 3;
	protected static final int PROJ_INDEX_DATA = 4;
	protected static final int PROJ_INDEX_DISPLAY_NAME = 5;
	protected static final int PROJ_INDEX_WIDTH = 6;
	protected static final int PROJ_INDEX_HEIGHT = 7;
//	protected static final int PROJ_INDEX_DATE_MODIFIED = 8;
//	protected static final int PROJ_INDEX_DATE_ADDED = 9;

	protected static final Uri QUERY_URI = MediaStore.Files.getContentUri("external");

	public static class MediaInfo {
		public long id;
		public String data;
		public String title;
		public String mime;
		public String displayName;
		public int mediaType;
		public int width;
		public int height;

		protected MediaInfo loadFromCursor(final Cursor cursor) {
			if (cursor != null) {
				id = cursor.getLong(PROJ_INDEX_ID);
				data = cursor.getString(PROJ_INDEX_DATA);
				title = cursor.getString(PROJ_INDEX_TITLE);
				mime = cursor.getString(PROJ_INDEX_MIME_TYPE);
				displayName = cursor.getString(PROJ_INDEX_DISPLAY_NAME);
				mediaType = cursor.getInt(PROJ_INDEX_MEDIA_TYPE);
				try {
					width = cursor.getInt(PROJ_INDEX_WIDTH);
					height = cursor.getInt(PROJ_INDEX_HEIGHT);
				} catch (final Exception e) {
					// ignore
				}
			}
			return this;
		}

		@NonNull
		@Override
		public String toString() {
			return String.format("MediaInfo(title=%s,displayName=%s, mediaType=%s,mime=%s,data=%s)",
				title, displayName, mediaType(), mime, data);
		}

		private String mediaType() {
			switch (mediaType) {
			case MediaStore.Files.FileColumns.MEDIA_TYPE_NONE:
				return "none";
			case MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE:
				return "image";
			case MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO:
				return "audio";
			case MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO:
				return "video";
			case MediaStore.Files.FileColumns.MEDIA_TYPE_PLAYLIST:
				return "playlist";
			default:
				return String.format(Locale.US, "unknown:%d", mediaType);
			}
		}
	}

	/**
	 * LoaderDrawable is a descendent of Drawable to load image asynchronusly and draw
	 * We want to use BitmapDrawable but we can't because it has no public/protected method
	 * to set Bitmap after construction.
	 *
	 * Most code of LoaderDrawable came from BitmapJobDrawable.java in Android Gallery app
	 *
	 * Copyright (C) 2013 The Android Open Source Project
	 *
	 * Licensed under the Apache License, Version 2.0 (the "License");
	 * you may not use this file except in compliance with the License.
	 * You may obtain a copy of the License at
	 *
	 *      http://www.apache.org/licenses/LICENSE-2.0
	 *
	 * Unless required by applicable law or agreed to in writing, software
	 * distributed under the License is distributed on an "AS IS" BASIS,
	 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	 * See the License for the specific language governing permissions and
	 * limitations under the License.
	 */
	protected abstract static class LoaderDrawable extends Drawable implements Runnable {
		private final ContentResolver mContentResolver;
	    private final Paint mPaint = new Paint();
	    private final Paint mDebugPaint = new Paint();
	    private final Matrix mDrawMatrix = new Matrix();
		private Bitmap mBitmap;
	    private int mRotation = 0;
	    private ImageLoader mLoader;
	    private final int mWidth, mHeight;

		public LoaderDrawable(final ContentResolver cr, final int width, final int height) {
			mContentResolver = cr;
			mDebugPaint.setColor(Color.RED);
			mDebugPaint.setTextSize(18);
			mWidth = width;
			mHeight = height;
		}

	    @Override
	    protected void onBoundsChange(final Rect bounds) {
	        super.onBoundsChange(bounds);
	        updateDrawMatrix(getBounds());
	    }

	    @Override
		public void draw(@NonNull final Canvas canvas) {
	        final Rect bounds = getBounds();
	        if (mBitmap != null) {
	            canvas.save();
	            canvas.clipRect(bounds);
	            canvas.concat(mDrawMatrix);
	            canvas.rotate(mRotation, bounds.centerX(), bounds.centerY());
	            canvas.drawBitmap(mBitmap, 0, 0, mPaint);
	            canvas.restore();
	        } else {
	            mPaint.setColor(0xFFCCCCCC);
	            canvas.drawRect(bounds, mPaint);
	        }
            if (DEBUG) {
	            canvas.drawText(Long.toString(mLoader != null ? mLoader.mId : -1),
	            	bounds.centerX(), bounds.centerY(), mDebugPaint);
            }
		}

		private void updateDrawMatrix(final Rect bounds) {
		    if (mBitmap == null || bounds.isEmpty()) {
		        mDrawMatrix.reset();
		        return;
		    }

		    final float dwidth = mBitmap.getWidth();
		    final float dheight = mBitmap.getHeight();
		    final int vwidth = bounds.width();
		    final int vheight = bounds.height();

		    float scale;
		    int dx = 0, dy = 0;

		    // Calculates a matrix similar to ScaleType.CENTER_CROP
            if (dwidth * vheight > vwidth * dheight) {
                scale = vheight / dheight;
				dx = (int) ((vwidth - dwidth * scale) * 0.5f + 0.5f);
            } else {
                scale = vwidth / dwidth;
				dy = (int) ((vheight - dheight * scale) * 0.5f + 0.5f);
            }
/*		    // Calculates a matrix similar to ScaleType.CENTER_INSIDE
            if (dwidth <= vwidth && dheight <= vheight) {
                scale = 1.0f;
            } else {
                scale = Math.min((float) vwidth / (float) dwidth,
                        (float) vheight / (float) dheight);
            }
            dx = (int) ((vwidth - dwidth * scale) * 0.5f + 0.5f);
            dy = (int) ((vheight - dheight * scale) * 0.5f + 0.5f); */
			mDrawMatrix.setScale(scale, scale);
			mDrawMatrix.postTranslate(dx, dy);

		    invalidateSelf();
		}

		@Override
		public void setAlpha(final int alpha) {
	        int oldAlpha = mPaint.getAlpha();
	        if (alpha != oldAlpha) {
	            mPaint.setAlpha(alpha);
	            invalidateSelf();
	        }
		}

		@Override
		public void setColorFilter(final ColorFilter cf) {
	        mPaint.setColorFilter(cf);
	        invalidateSelf();
		}

	    @Override
	    public int getIntrinsicWidth() {
	    	return mWidth;
	    }

	    @Override
	    public int getIntrinsicHeight() {
	    	return mHeight;
	    }

		@Override
		public int getOpacity() {
	        Bitmap bm = mBitmap;
	        return (bm == null || bm.hasAlpha() || mPaint.getAlpha() < 255) ?
	                PixelFormat.TRANSLUCENT : PixelFormat.OPAQUE;
		}

	    /**
	     * callback to set bitmap on UI thread after asyncronus loading
	     * request call this callback in ThumbnailLoader#run at the end of asyncronus loading
	     */
		@Override
		public void run() {
			setBitmap(mLoader.getBitmap());
		}

		protected abstract ImageLoader createThumbnailLoader();
		protected abstract Bitmap checkBitmapCache(final int hashCode, final long id);

		/**
		 * start loading image asynchronusly
		 * @param id
		 */
		public void startLoad(final int media_type, final int hashCode, final long id) {

			if (mLoader != null)
				mLoader.cancelLoad();

			// try to get from internal thumbnail cache
			final Bitmap newBitmap = checkBitmapCache(hashCode, id);
			if (newBitmap == null) {
				// only start loading if the thumbnail does not exist in internal thumbnail cache
				mBitmap = null;
				// re-using ThumbnailLoader will cause several problems on some devices...
				mLoader = createThumbnailLoader();
				mLoader.startLoad(media_type, hashCode, id);
			} else {
				setBitmap(newBitmap);
			}
			invalidateSelf();
		}

		private void setBitmap(final Bitmap bitmap) {
			if (bitmap != mBitmap) {
				mBitmap = bitmap;
	            updateDrawMatrix(getBounds());
			}
		}
	}

	/**
	 * Runnable to load image asynchronously
	 */
	protected abstract static class ImageLoader implements Runnable {
		protected final LoaderDrawable mParent;
		private final FutureTask<Bitmap> mTask;
		private int mMediaType;
		private int mHashCode;
		private long mId;
		private Bitmap mBitmap;

	    public ImageLoader(final LoaderDrawable parent) {
	    	mParent = parent;
			mTask = new FutureTask<Bitmap>(this, null);
	    }

	    /**
		 * start loading
		 * @param hashCode
	     * @param id
	     */
		public synchronized void startLoad(final int media_type, final int hashCode, final long id) {
			mMediaType = media_type;
			mHashCode = hashCode;
			mId = id;
			mBitmap = null;
			ThreadPool.queueEvent(mTask);
		}

		/**
		 * cancel loading
		 */
		public void cancelLoad() {
			mTask.cancel(true);
		}

		protected abstract Bitmap loadBitmap(final ContentResolver cr, final int mediaType, final int hashCode, final long id, final int requestWidth, final int requestHeight);

		@Override
		public void run() {
			int mediaType;
			int hashCode;
			long id;
			synchronized(this) {
				mediaType = mMediaType;
				hashCode = mHashCode;
				id = mId;
			}
			if (!mTask.isCancelled()) {
				mBitmap = loadBitmap(mParent.mContentResolver, mediaType, hashCode, id, mParent.mWidth, mParent.mHeight);
			}
			if (mTask.isCancelled() || (id != mId) || (mBitmap == null)) {
				return;	// return without callback
			}
			// set callback
			mParent.scheduleSelf(mParent, 0);
		}

		public Bitmap getBitmap() {
			return mBitmap;
		}
	}

	protected static final Bitmap getImage(final ContentResolver cr, final long id, final int requestWidth, final int requestHeight)
		throws IOException {

		Bitmap result = null;
		final ParcelFileDescriptor pfd = cr.openFileDescriptor(
			ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id), "r");
		if (pfd != null) {
			try {
				final BitmapFactory.Options options = new BitmapFactory.Options();
				// just decode to get image size
				options.inJustDecodeBounds = true;
				BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor(), null, options);
				// calculate sub-sampling
				options.inSampleSize = calcSampleSize(options, requestWidth, requestHeight);
				options.inJustDecodeBounds = false;
				result = BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor(), null, options);
			} finally {
				pfd.close();
			}
		}
		return result;
	}

	/**
	 * calculate maximum sub-sampling size that the image size is greater or equal to requested size
	 * @param options
	 * @param requestWidth
	 * @param requestHeight
	 * @return maximum sub-sampling size
	 */
	protected static final int calcSampleSize(final BitmapFactory.Options options, final int requestWidth, final int requestHeight) {
		final int imageWidth = options.outWidth;
		final int imageHeight = options.outHeight;
		int reqWidth = requestWidth, reqHeight = requestHeight;
		if (requestWidth <= 0) {
			if (requestHeight > 0)
				reqWidth = (int)(imageWidth * requestHeight / (float)imageHeight);
			else
				reqWidth = imageWidth;
		}
		if (requestHeight <= 0) {
			if (requestWidth > 0)
				reqHeight = (int)(imageHeight * requestWidth / (float)imageHeight);
			else
				reqHeight = imageHeight;
		}
		int inSampleSize = 1;
		if ((imageHeight > reqHeight) || (imageWidth > reqWidth)) {
			if (imageWidth > imageHeight) {
				inSampleSize = Math.round(imageHeight / (float)reqHeight);	// Math.floor
			} else {
				inSampleSize = Math.round(imageWidth / (float)reqWidth);	// Math.floor
			}
		}
/*		if (DEBUG) Log.v(TAG, String.format("calcSampleSize:image=(%d,%d),request=(%d,%d),inSampleSize=%d",
				imageWidth, imageHeight, reqWidth, reqHeight, inSampleSize)); */
		return inSampleSize;
	}

}
