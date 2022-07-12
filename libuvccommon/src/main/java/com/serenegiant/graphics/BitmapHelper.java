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

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import com.serenegiant.utils.BitsHelper;
import com.serenegiant.utils.UriHelper;

public final class BitmapHelper {
//	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
//	private static final String TAG = "BitmapHelper";
	/**
	 * Bitmapをpng形式のbyte[]に変換して返す
	 * @param bitmap
	 * @return
	 */
	public static byte[] BitmapToByteArray(@NonNull final Bitmap bitmap) {
		final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		byte[] bytes = null;
        if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)) {
            bytes = byteArrayOutputStream.toByteArray();
        }
        return bytes;
	}

	/**
	 * byte[]をBitmapに変換して返す
	 * @param bytes
	 * @return
	 */
	public static Bitmap asBitmap(final byte[] bytes) {
		Bitmap bitmap = null;
		if (bytes != null)
			bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
		return bitmap;
	}

	/**
	 * byte[]を指定した幅・高さに最も近い大きさのBitmapに変換して返す
	 * @param bytes
	 * @param requestWidth
	 * @param requestHeight
	 * @return
	 */
	public static Bitmap asBitmap(final byte[] bytes, final int requestWidth, final int requestHeight) {
		Bitmap bitmap = null;
		if (bytes != null) {
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;	// Bitmapを生成せずにサイズ等の情報だけを取得する
			BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
			options.inJustDecodeBounds = false;
			options.inSampleSize = calcSampleSize(options, requestWidth, requestHeight);
			bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
		}
		return bitmap;
	}

	/**
	 * byte[]を指定した幅・高さのBitmapに変換して返す
	 * @param bytes
	 * @param requestWidth
	 * @param requestHeight
	 * @return
	 */
	public static Bitmap asBitmapStrictSize(final byte[] bytes, final int requestWidth, final int requestHeight) {
		Bitmap bitmap = null;
		if (bytes != null) {
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;	// Bitmapを生成せずにサイズ等の情報だけを取得する
			BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
			// 一番近いサイズになるSamplingSizeを計算
			final int calcedSampleSize = calcSampleSize(options, requestWidth, requestHeight);
			// 2のベキ乗に丸める=MSBを取得
			final int inSampleSize = 1 << BitsHelper.MSB(calcedSampleSize);
			options.inSampleSize = inSampleSize;
			options.inJustDecodeBounds = false;
			bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
			if ((inSampleSize != calcedSampleSize)
				|| (bitmap.getWidth() != requestWidth)
				|| (bitmap.getHeight() != requestHeight)) {

				final Bitmap newBitmap = scaleBitmap(bitmap, requestWidth, requestHeight);
				bitmap.recycle();
				bitmap = newBitmap;
			}
		}
		return bitmap;
	}

	/**
	 * ファイルからビットマップを読み込んでBitmapとして返す
	 * @param filePath
	 * @return
	 */
	public static Bitmap asBitmap(final String filePath) {
		Bitmap bitmap= null;
		if (!TextUtils.isEmpty(filePath)) {
			bitmap = BitmapFactory.decodeFile(filePath);
		}
		return bitmap;
	}

	/**
	 * ファイルからビットマップを読み込んで指定した幅・高さに最も近い大きさのBitmapとして返す
	 * @param filePath
	 * @param requestWidth
	 * @param requestHeight
	 * @return
	 */
	public static Bitmap asBitmap(final String filePath, final int requestWidth, final int requestHeight) {
		Bitmap bitmap = null;
		if (!TextUtils.isEmpty(filePath)) {
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;	// Bitmapを生成せずにサイズ等の情報だけを取得する
			BitmapFactory.decodeFile(filePath, options);
			options.inJustDecodeBounds = false;
			options.inSampleSize = calcSampleSize(options, requestWidth, requestHeight);
			bitmap = BitmapFactory.decodeFile(filePath, options);
		}
		return bitmap;
	}

	/**
	 * ファイルからビットマップデータを読み込んで指定した幅・高さのBitmapとして返す
	 * @param filePath
	 * @param requestWidth
	 * @param requestHeight
	 * @return
	 */
	public static Bitmap asBitmapStrictSize(final String filePath, final int requestWidth, final int requestHeight) {
		Bitmap bitmap = null;
		if (!TextUtils.isEmpty(filePath)) {
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;	// Bitmapを生成せずにサイズ等の情報だけを取得する
			BitmapFactory.decodeFile(filePath, options);
			// 一番近いサイズになるSamplingSizeを計算
			final int calcedSampleSize = calcSampleSize(options, requestWidth, requestHeight);
			// 2のベキ乗に丸める=MSBを取得
			final int inSampleSize = 1 << BitsHelper.MSB(calcedSampleSize);
			options.inSampleSize = inSampleSize;
//			options.inMutable = (inSampleSize != calcedSampleSize);	// サイズ変更する時はmutableにする
			options.inJustDecodeBounds = false;
			bitmap = BitmapFactory.decodeFile(filePath, options);
			if ((inSampleSize != calcedSampleSize)
				|| (bitmap.getWidth() != requestWidth)
				|| (bitmap.getHeight() != requestHeight)) {

				final Bitmap newBitmap = scaleBitmap(bitmap, requestWidth, requestHeight);
				bitmap.recycle();
				bitmap = newBitmap;
			}
		}
		return bitmap;
	}

	/**
	 * ファイルからビットマップを読み込んでBitmapとして返す
	 * @param fd
	 * @return
	 */
	public static Bitmap asBitmap(final FileDescriptor fd) {
		Bitmap bitmap= null;
		if (fd != null && fd.valid()) {
			bitmap = BitmapFactory.decodeFileDescriptor(fd);
		}
		return bitmap;
	}

	/**
	 * ファイルからビットマップを読み込んで指定した幅・高さに最も近い大きさのBitmapとして返す
	 * @param fd
	 * @param requestWidth
	 * @param requestHeight
	 * @return
	 */
	public static Bitmap asBitmap(final FileDescriptor fd, final int requestWidth, final int requestHeight) {
		Bitmap bitmap = null;
		if (fd != null && fd.valid()) {
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;	// Bitmapを生成せずにサイズ等の情報だけを取得する
			BitmapFactory.decodeFileDescriptor(fd, null, options);
			options.inJustDecodeBounds = false;
			options.inSampleSize = calcSampleSize(options, requestWidth, requestHeight);
			bitmap = BitmapFactory.decodeFileDescriptor(fd, null, options);
		}
		return bitmap;
	}

	/**
	 * ファイルからビットマップデータを読み込んで指定した幅・高さのBitmapとして返す
	 * @param fd
	 * @param requestWidth
	 * @param requestHeight
	 * @return
	 */
	public static Bitmap asBitmapStrictSize(final FileDescriptor fd, final int requestWidth, final int requestHeight) {
		Bitmap bitmap = null;
		if (fd != null && fd.valid()) {
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;	// Bitmapを生成せずにサイズ等の情報だけを取得する
			BitmapFactory.decodeFileDescriptor(fd, null, options);
			// 一番近いサイズになるSamplingSizeを計算
			final int calcedSampleSize = calcSampleSize(options, requestWidth, requestHeight);
			// 2のベキ乗に丸める=MSBを取得
			final int inSampleSize = 1 << BitsHelper.MSB(calcedSampleSize);
			options.inSampleSize = inSampleSize;
//			options.inMutable = (inSampleSize != calcedSampleSize);	// サイズ変更する時はmutableにする
			options.inJustDecodeBounds = false;
			bitmap = BitmapFactory.decodeFileDescriptor(fd, null, options);
			if ((inSampleSize != calcedSampleSize)
				|| (bitmap.getWidth() != requestWidth)
				|| (bitmap.getHeight() != requestHeight)) {

				final Bitmap newBitmap = scaleBitmap(bitmap, requestWidth, requestHeight);
				bitmap.recycle();
				bitmap = newBitmap;
			}
		}
		return bitmap;
	}

	/**
	 * ファイルからビットマップを読み込んでBitmapとして返す
	 * @param cr
	 * @return
	 * @throws FileNotFoundException
	 */
	public static Bitmap asBitmap(final ContentResolver cr, final Uri uri)
		throws FileNotFoundException, IOException {

		Bitmap bitmap= null;
		if (uri != null) {
			final ParcelFileDescriptor pfd = cr.openFileDescriptor(uri, "r");
			if (pfd != null) {
				bitmap = BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor());
				final int orientation = getOrientation(cr, uri);
				if (orientation != 0) {
					final Bitmap newBitmap = rotateBitmap(bitmap, orientation);
					bitmap.recycle();
					bitmap = newBitmap;
				}
			}
/*			final InputStream input = cr.openInputStream(uri);
			bitmap = BitmapFactory.decodeStream(input);
			input.close();
			final int orientation = getOrientation(cr, uri);
			if (orientation != 0) {
				final Bitmap newBitmap = rotateBitmap(bitmap, orientation);
				bitmap.recycle();
				bitmap = newBitmap;
			} */
		}
		return bitmap;
	}

	/**
	 * ファイルからビットマップを読み込んで指定した幅・高さに最も近い大きさのBitmapとして返す
	 * @param cr
	 * @param requestWidth
	 * @param requestHeight
	 * @return
	 * @throws FileNotFoundException
	 */
	public static Bitmap asBitmap(final ContentResolver cr, final Uri uri, final int requestWidth, final int requestHeight)
		throws FileNotFoundException, IOException {

		Bitmap bitmap = null;
		if (uri != null) {
			final ParcelFileDescriptor pfd = cr.openFileDescriptor(uri, "r");
			if (pfd != null) {
				final BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = true;	// Bitmapを生成せずにサイズ等の情報だけを取得する
				BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor(), null, options);
				options.inJustDecodeBounds = false;
				options.inSampleSize = calcSampleSize(options, requestWidth, requestHeight);
				bitmap = BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor(), null, options);
				final int orientation = getOrientation(cr, uri);
				if (orientation != 0) {
					final Bitmap newBitmap = rotateBitmap(bitmap, orientation);
					bitmap.recycle();
					bitmap = newBitmap;
				}
			}
/*			InputStream input = cr.openInputStream(uri);
			if (input != null) {
				final BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = true;	// Bitmapを生成せずにサイズ等の情報だけを取得する
				BitmapFactory.decodeStream(input, null, options);
				input.close(); input = null;
				options.inJustDecodeBounds = false;
				options.inSampleSize = calcSampleSize(options, requestWidth, requestHeight);
				input = cr.openInputStream(uri);
				bitmap = BitmapFactory.decodeStream(input, null, options);
				input.close();
				final int orientation = getOrientation(cr, uri);
				if (orientation != 0) {
					final Bitmap newBitmap = rotateBitmap(bitmap, orientation);
					bitmap.recycle();
					bitmap = newBitmap;
				}
			} */
		}
//		if (DEBUG) Log.v(TAG, "asBitmap:" + bitmap);
		return bitmap;
	}

	/**
	 * ファイルからビットマップデータを読み込んで指定した幅・高さのBitmapとして返す
	 * @param cr
	 * @param requestWidth
	 * @param requestHeight
	 * @return
	 * @throws FileNotFoundException
	 */
	public static Bitmap asBitmapStrictSize(final ContentResolver cr, final Uri uri, final int requestWidth, final int requestHeight)
		throws FileNotFoundException, IOException {

		Bitmap bitmap = null;
		if (uri != null) {
			final ParcelFileDescriptor pfd = cr.openFileDescriptor(uri, "r");
			if (pfd != null) {
				final BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = true;	// Bitmapを生成せずにサイズ等の情報だけを取得する
				BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor(), null, options);
				// 一番近いサイズになるSamplingSizeを計算
				final int calcedSampleSize = calcSampleSize(options, requestWidth, requestHeight);
				// 2のベキ乗に丸める=MSBを取得
				final int inSampleSize = 1 << BitsHelper.MSB(calcedSampleSize);
				options.inSampleSize = inSampleSize;
//				options.inMutable = (inSampleSize != calcedSampleSize);	// サイズ変更する時はmutableにする(API11以上)
				options.inJustDecodeBounds = false;
				bitmap = BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor(), null, options);
				final int orientation = getOrientation(cr, uri);
				if ((inSampleSize != calcedSampleSize)
						|| (orientation != 0)
						|| (bitmap.getWidth() != requestWidth)
						|| (bitmap.getHeight() != requestHeight)) {

					final Bitmap newBitmap = scaleRotateBitmap(bitmap, requestWidth, requestHeight, orientation);
					bitmap.recycle();
					bitmap = newBitmap;
				}
			}
/*			InputStream input = cr.openInputStream(uri);
			if (input != null) {
				final BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = true;	// Bitmapを生成せずにサイズ等の情報だけを取得する
				BitmapFactory.decodeStream(input, null, options);
				// 本当はmark/resetを使えばいいんだけどmarkのreadlimitに適当に十分大きい数字を入れるしか無いので一旦閉じて再度取得する
				// FileDescriptorを使えばファイルのサイズが取れそうだけど・・・それならdecodeFileDescriptor使うほうが良さそう？
				input.close(); input = null;
				// 一番近いサイズになるSamplingSizeを計算
				final int calcedSampleSize = calcSampleSize(options, requestWidth, requestHeight);
				// 2のベキ乗に丸める=MSBを取得
				final int inSampleSize = 1 << BitsHelper.MSB(calcedSampleSize);
				options.inSampleSize = inSampleSize;
//				options.inMutable = (inSampleSize != calcedSampleSize);	// サイズ変更する時はmutableにする(API11以上)
				options.inJustDecodeBounds = false;
				input = cr.openInputStream(uri);
				bitmap = BitmapFactory.decodeStream(input, null, options);
				input.close();
				final int orientation = getOrientation(cr, uri);
				if ((inSampleSize != calcedSampleSize) || (orientation != 0)) {
					Bitmap newBitmap = scaleRotateBitmap(bitmap, requestWidth, requestHeight, orientation);
					bitmap.recycle();
					bitmap = newBitmap;
				}
			} */
		}
		return bitmap;
	}

	/**
	 * ファイルからビットマップを読み込んでBitmapとして返す
	 * @param in
	 * @return
	 */
	public static Bitmap asBitmap(final InputStream in) {
		Bitmap bitmap = null;
		if (in != null) {
			bitmap = BitmapFactory.decodeStream(in);
		}
		return bitmap;
	}

	/**
	 * ファイルからビットマップを読み込んで指定した幅・高さに最も近い大きさのBitmapとして返す
	 // FIXME これはうまくいかない, リサイズするにはinから2回読み込まないといけない
	 * @param in
	 * @param requestWidth
	 * @param requestHeight
	 * @return
	 */
	@Deprecated
	public static Bitmap asBitmap(final InputStream in, final int requestWidth, final int requestHeight) {
		Bitmap bitmap = null;
		if (in != null) {
			final BitmapFactory.Options options = new BitmapFactory.Options();
			final Rect outPadding = new Rect();
			options.inJustDecodeBounds = true;	// Bitmapを生成せずにサイズ等の情報だけを取得する
			BitmapFactory.decodeStream(in, outPadding, options);
			options.inJustDecodeBounds = false;
			options.inSampleSize = calcSampleSize(options, requestWidth, requestHeight);
			bitmap = BitmapFactory.decodeStream(in, outPadding, options);
		}
		return bitmap;
	}

	/**
	 * ファイルからビットマップデータを読み込んで指定した幅・高さのBitmapとして返す
	 // FIXME これはうまくいかない, リサイズするにはinから2回読み込まないといけない
	 * @param in
	 * @param requestWidth
	 * @param requestHeight
	 * @return
	 */
	@Deprecated
	public static Bitmap asBitmapStrictSize(final InputStream in, final int requestWidth, final int requestHeight) {
		Bitmap bitmap = null;
		if (in != null) {
			final BitmapFactory.Options options = new BitmapFactory.Options();
			final Rect outPadding = new Rect();
			options.inJustDecodeBounds = true;	// Bitmapを生成せずにサイズ等の情報だけを取得する
			BitmapFactory.decodeStream(in, outPadding, options);
			// 一番近いサイズになるSamplingSizeを計算
			final int calcedSampleSize = calcSampleSize(options, requestWidth, requestHeight);
			// 2のベキ乗に丸める=MSBを取得
			final int inSampleSize = 1 << BitsHelper.MSB(calcedSampleSize);
			options.inSampleSize = inSampleSize;
//			options.inMutable = (inSampleSize != calcedSampleSize);	// サイズ変更する時はmutableにする
			options.inJustDecodeBounds = false;
			bitmap = BitmapFactory.decodeStream(in, outPadding, options);
			if ((inSampleSize != calcedSampleSize)
				|| (bitmap.getWidth() != requestWidth)
				|| (bitmap.getHeight() != requestHeight)) {

				final Bitmap newBitmap = scaleBitmap(bitmap, requestWidth, requestHeight);
				bitmap.recycle();
				bitmap = newBitmap;
			}
		}
		return bitmap;
	}


	public static int getOrientation(final ContentResolver cr, final Uri uri) {
//		if (DEBUG) Log.v(TAG, "getOrientation:" + uri);

        final ExifInterface exifInterface;
        try {
            exifInterface = new ExifInterface(UriHelper.getAbsolutePath(cr, uri));
        } catch (final Exception e) {	// IOException/IllegalArgumentException
//        	if (DEBUG) Log.w(TAG, e);
            return 0;
        }

//		if (DEBUG) Log.v(TAG, "getOrientation:exifInterface=" + exifInterface);
        final int exifR = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
        int orientation;
        switch (exifR) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                orientation = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                orientation = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                orientation = 270;
                break;
            default:
                orientation = 0;
                break;
        }
/*		// 元画像自体を回転させるのであればExif情報も変更しないとダメ
		if (orientation != 0) {
            exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, "0");
        	try {
				exifInterface.saveAttributes();
			} catch (IOException e) {
			}
        } */
//		if (DEBUG) Log.v(TAG, "getOrientation:orientation=" + orientation);
        return orientation;
    }

	/**
	 * 指定したサイズになるように拡大縮小したBitmapを生成して返す(Bitmap#createScaledBitmapと一緒かな?)
	 * @param bitmap
	 * @param requestWidth
	 * @param requestHeight
	 * @return
	 */
	public static Bitmap scaleBitmap(final Bitmap bitmap, final int requestWidth, final int requestHeight) {
		Bitmap newBitmap = null;
		if (bitmap != null) {
			final int width = bitmap.getWidth();
			final int height = bitmap.getHeight();
			final Matrix matrix = new Matrix();
			matrix.postScale(width / (float)requestWidth, height / (float)requestHeight);
			newBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
		}
//		if (DEBUG) Log.v(TAG, "scaleBitmap:bitmap=" + bitmap + " newBitmap=" + newBitmap);
		return newBitmap;
	}

	/**
	 * 指定した角度回転させたBitmapを生成して返す
	 * @param bitmap
	 * @param rotation
	 * @return
	 */
	public static Bitmap rotateBitmap(final Bitmap bitmap, final int rotation) {
		Bitmap newBitmap = null;
		if (bitmap != null) {
			final int width = bitmap.getWidth();
			final int height = bitmap.getHeight();
			final Matrix matrix = new Matrix();
			matrix.postRotate(rotation);
			newBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
		}
//		if (DEBUG) Log.v(TAG, "rotateBitmap:bitmap=" + bitmap + " newBitmap=" + newBitmap);
		return newBitmap;
	}

	/**
	 * 指定した大きさにした後指定した角度回転させたBitmapを生成して返す
	 * @param bitmap
	 * @param requestWidth
	 * @param requestHeight
	 * @param rotation
	 * @return
	 */
	public static Bitmap scaleRotateBitmap(final Bitmap bitmap, final int requestWidth, final int requestHeight, final int rotation) {
		Bitmap newBitmap = null;
		if (bitmap != null) {
			final int width = bitmap.getWidth();
			final int height = bitmap.getHeight();
			final Matrix matrix = new Matrix();
			matrix.postScale(width / (float)requestWidth, height / (float)requestHeight);
			matrix.postRotate(rotation);
			newBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
		}
//		if (DEBUG) Log.v(TAG, "scaleBitmap:bitmap=" + bitmap + " newBitmap=" + newBitmap);
		return newBitmap;
	}

    /* Options used internally. */
    private static final int OPTIONS_SCALE_UP = 0x1;
    public static final int OPTIONS_RECYCLE_INPUT = 0x2;
	/**
	 * 縦横の短い方に合わせて中央を切り出して指定した大きさにしたビットマップを生成して返す
	 * @param source
	 * @param width
	 * @param height
	 * @return
	 */
	public static Bitmap extractBitmap(final Bitmap source, final int width, final int height) {
		Bitmap newBitmap = null;
		if (source != null) {
	        float scale;
	        if (source.getWidth() < source.getHeight()) {
	            scale = width / (float) source.getWidth();
	        } else {
	            scale = height / (float) source.getHeight();
	        }
	        final Matrix matrix = new Matrix();
	        matrix.setScale(scale, scale);
	        newBitmap = transform(matrix, source, width, height,
	        	OPTIONS_RECYCLE_INPUT | OPTIONS_SCALE_UP);
		}
		return newBitmap;
	}

    /**
     * Transform source Bitmap to targeted width and height.
     */
    private static Bitmap transform(Matrix scaler,
            final Bitmap source,
            final int targetWidth, final int targetHeight,
            final int options) {
        final boolean scaleUp = (options & OPTIONS_SCALE_UP) != 0;
        final boolean recycle = (options & OPTIONS_RECYCLE_INPUT) != 0;

        final int deltaX = source.getWidth() - targetWidth;
        final int deltaY = source.getHeight() - targetHeight;
        if (!scaleUp && (deltaX < 0 || deltaY < 0)) {
            /*
            * In this case the bitmap is smaller, at least in one dimension,
            * than the target.  Transform it by placing as much of the image
            * as possible into the target and leaving the top/bottom or
            * left/right (or both) black.
            */
            final Bitmap b2 = Bitmap.createBitmap(targetWidth, targetHeight,
            		Bitmap.Config.ARGB_8888);
            final Canvas c = new Canvas(b2);

            final int deltaXHalf = Math.max(0, deltaX / 2);
            final int deltaYHalf = Math.max(0, deltaY / 2);
            final Rect src = new Rect(
            	deltaXHalf,
            	deltaYHalf,
            	deltaXHalf + Math.min(targetWidth, source.getWidth()),
            	deltaYHalf + Math.min(targetHeight, source.getHeight()));
            final int dstX = (targetWidth  - src.width())  / 2;
            final int dstY = (targetHeight - src.height()) / 2;
            final Rect dst = new Rect(
                    dstX,
                    dstY,
                    targetWidth - dstX,
                    targetHeight - dstY);
            c.drawBitmap(source, src, dst, null);
            if (recycle) {
                source.recycle();
            }
            c.setBitmap(null);
            return b2;
        }
        final float bitmapWidthF = source.getWidth();
        final float bitmapHeightF = source.getHeight();

        final float bitmapAspect = bitmapWidthF / bitmapHeightF;
        final float viewAspect   = (float) targetWidth / targetHeight;

        if (bitmapAspect > viewAspect) {
            final float scale = targetHeight / bitmapHeightF;
            if (scale < .9F || scale > 1F) {
                scaler.setScale(scale, scale);
            } else {
                scaler = null;
            }
        } else {
            final float scale = targetWidth / bitmapWidthF;
            if (scale < .9F || scale > 1F) {
                scaler.setScale(scale, scale);
            } else {
                scaler = null;
            }
        }

        Bitmap b1;
        if (scaler != null) {
            // this is used for minithumb and crop, so we want to filter here.
            b1 = Bitmap.createBitmap(source, 0, 0,
            source.getWidth(), source.getHeight(), scaler, true);
        } else {
            b1 = source;
        }

        if (recycle && b1 != source) {
            source.recycle();
        }

        final int dx1 = Math.max(0, b1.getWidth() - targetWidth);
        final int dy1 = Math.max(0, b1.getHeight() - targetHeight);

        final Bitmap b2 = Bitmap.createBitmap(b1,
                dx1 / 2,
                dy1 / 2,
                targetWidth,
                targetHeight);

        if (b2 != b1) {
            if (recycle || b1 != source) {
                b1.recycle();
            }
        }

        return b2;
    }

	/**
	 * 指定したサイズよりも小さくならない最大のサブサンプリング数を取得
	 * @param options
	 * @param requestWidth
	 * @param requestHeight
	 * @return
	 */
	public static int calcSampleSize(final BitmapFactory.Options options, final int requestWidth, final int requestHeight) {
		final int imageWidth = options.outWidth;
		final int imageHeight = options.outHeight;
		int inSampleSize = 1;	// サブサンプリングサイズ
		if ((imageHeight > requestHeight) || (imageWidth > requestWidth)) {
			if (imageWidth > imageHeight) {
				inSampleSize = (int)Math.floor(imageHeight / (float)requestHeight);
			} else {
				inSampleSize = (int)Math.floor(imageWidth / (float)requestWidth);
			}
		}
/*		if (DEBUG) Log.v("BitmapHelper", String.format("calcSampleSize:image=(%d,%d),request=(%d,%d),inSampleSize=%d",
				imageWidth, imageHeight, requestWidth, requestHeight, inSampleSize)); */
		return inSampleSize;
	}

	public static Bitmap copyBitmap(final Bitmap src, Bitmap dest) {
		if (src == null)
			throw new NullPointerException("src bitmap should not be null.");
		if (dest == null) {
			dest = Bitmap.createBitmap(src);
		} else if (!src.equals(dest)) {
			final Canvas canvas = new Canvas(dest);
			canvas.setBitmap(src);
		}
		return dest;
	}

	/**
	 * 透過部分の背景用に白とグレーの市松模様の単位パターンのビットマップを生成
	 * @return
	 */
	public static Bitmap makeCheckBitmap() {
        final Bitmap bm = Bitmap.createBitmap(40, 40, Bitmap.Config.RGB_565);
        final Canvas c = new Canvas(bm);
        c.drawColor(Color.WHITE);
        final Paint p = new Paint();
        p.setColor(Color.LTGRAY);
        c.drawRect(0, 0, 20, 20, p);
        c.drawRect(20, 20, 40, 40, p);
        return bm;
    }

}
