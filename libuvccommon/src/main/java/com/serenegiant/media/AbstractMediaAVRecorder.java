package com.serenegiant.media;
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
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import android.text.TextUtils;

import com.serenegiant.utils.FileUtils;
import com.serenegiant.utils.SAFUtils;
import com.serenegiant.utils.StorageInfo;
import com.serenegiant.utils.UriHelper;

import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * Recorderの実装用ヘルパー
 */
public abstract class AbstractMediaAVRecorder extends Recorder {
	protected final WeakReference<Context> mWeakContext;
	protected final int mSaveTreeId;	// SDカードへの出力を試みるかどうか
	protected String mOutputPath;
	protected DocumentFile mOutputFile;

	/**
	 * コンストラクタ
	 * @param context
	 * @param callback
	 * @param ext 出力ファイルの拡張子
	 * @param saveTreeId
	 * @throws IOException
	 */
	public AbstractMediaAVRecorder(@NonNull final Context context,
		@Nullable final RecorderCallback callback,
		final String ext, final int saveTreeId)
			throws IOException {

		this(context, callback, null, ext, saveTreeId);
	}
	
	/**
	 * コンストラクタ
	 * @param context
	 * @param callback
	 * @param prefix
	 * @param _ext
	 * @param saveTreeId
	 * @throws IOException
	 */
	public AbstractMediaAVRecorder(@NonNull final Context context,
		@Nullable final RecorderCallback callback,
		final String prefix, final String _ext, final int saveTreeId)
			throws IOException {

		super(callback);
		mWeakContext = new WeakReference<Context>(context);
		mSaveTreeId = saveTreeId;
		String ext = _ext;
		if (TextUtils.isEmpty(ext)) ext = ".mp4";
		if ((saveTreeId > 0) && SAFUtils.hasStorageAccess(context, saveTreeId)) {
			mOutputPath = FileUtils.getCaptureFile(context,
				Environment.DIRECTORY_MOVIES, prefix, ext, saveTreeId).toString();
			final String file_name = (TextUtils.isEmpty(prefix)
				? FileUtils.getDateTimeString()
				: prefix + FileUtils.getDateTimeString()) + ext;
			final int fd = SAFUtils.createStorageFileFD(context, saveTreeId, "*/*", file_name);
			setupMuxer(fd);
		} else {
			try {
				mOutputPath = FileUtils.getCaptureFile(context,
					Environment.DIRECTORY_MOVIES, prefix, ext, 0).toString();
			} catch (final Exception e) {
				throw new IOException("This app has no permission of writing external storage");
			}
			setupMuxer(mOutputPath);
		}
	}
	
	/**
	 * コンストラクタ
	 * @param context
	 * @param callback
	 * @param saveTreeId
	 * @param dirs savedTreeIdが示すディレクトリからの相対ディレクトリパス, nullならsavedTreeIdが示すディレクトリ
	 * @param fileName
	 * @throws IOException
	 */
	public AbstractMediaAVRecorder(@NonNull final Context context,
		@Nullable final RecorderCallback callback,
		final int saveTreeId, @Nullable final String dirs, @NonNull final String fileName)
			throws IOException {
		
		super(callback);
		mWeakContext = new WeakReference<Context>(context);
		mSaveTreeId = saveTreeId;
		if ((saveTreeId > 0) && SAFUtils.hasStorageAccess(context, saveTreeId)) {
			DocumentFile tree = SAFUtils.getStorageFile(context, saveTreeId, dirs, "*/*", fileName);
			if (tree != null) {
				mOutputPath = UriHelper.getPath(context, tree.getUri());
				final ParcelFileDescriptor pfd
					= context.getContentResolver().openFileDescriptor(
						tree.getUri(), "rw");
				try {
					if (pfd != null) {
						setupMuxer(pfd.getFd());
						return;
					} else {
						// ここには来ないはずだけど
						throw new IOException("could not create ParcelFileDescriptor");
					}
				} catch (final Exception e) {
					if (pfd != null) {
						pfd.close();
					}
					throw e;
				}
			}
		}
		// フォールバックはしない
		throw new IOException("path not found/can't write");
	}
	
	/**
	 * コンストラクタ
	 * @param context
	 * @param callback
	 * @param output
	 * @throws IOException
	 */
	public AbstractMediaAVRecorder(@NonNull final Context context,
		@Nullable final RecorderCallback callback,
		@NonNull final DocumentFile output) throws IOException {
		
		super(callback);
		mWeakContext = new WeakReference<Context>(context);
		mSaveTreeId = 0;
		mOutputFile = output;
		mOutputPath = UriHelper.getPath(context, output.getUri());
		setupMuxer(context, output);
	}
	
	/**
	 * コンストラクタ
	 * @param context
	 * @param callback
	 * @param outputPath
	 * @throws IOException
	 */
	public AbstractMediaAVRecorder(@NonNull final Context context,
		@Nullable final RecorderCallback callback,
		@NonNull final String outputPath)
			throws IOException {

		super(callback);
		mWeakContext = new WeakReference<Context>(context);
		mSaveTreeId = 0;
		mOutputPath = outputPath;
		if (TextUtils.isEmpty(outputPath)) {
			try {
				mOutputPath = FileUtils.getCaptureFile(context,
					Environment.DIRECTORY_MOVIES, null, ".mp4", 0).toString();
			} catch (final Exception e) {
				throw new IOException("This app has no permission of writing external storage");
			}
		}
		setupMuxer(mOutputPath);
	}

	@Nullable
	@Override
	public String getOutputPath() {
		return mOutputPath;
	}
	
	@Nullable
	@Override
	public DocumentFile getOutputFile() {
		return mOutputFile;
	}

	/**
	 * ディスクの空き容量をチェックして足りなければtrueを返す
	 * @return true: 空き容量が足りない
	 */
	@Override
	protected boolean check() {
		final Context context = mWeakContext.get();
		final StorageInfo info = mOutputFile != null
			? SAFUtils.getStorageInfo(context, mOutputFile) : null;
		if ((info != null) && (info.totalBytes != 0)) {
			return ((info.freeBytes/ (float)info.totalBytes) < FileUtils.FREE_RATIO)
				|| (info.freeBytes < FileUtils.FREE_SIZE);
		}
		return (context == null)
			|| ((mOutputFile == null)
				&& !FileUtils.checkFreeSpace(context,
					VideoConfig.maxDuration, mStartTime, mSaveTreeId));
	}
	
	@Nullable
	protected Context getContext() {
		return mWeakContext.get();
	}

	protected abstract void setupMuxer(final int fd) throws IOException;
	protected abstract void setupMuxer(
		@NonNull final String output)  throws IOException;
	protected abstract void setupMuxer(
		@NonNull final Context context,
		@NonNull final DocumentFile output)  throws IOException;
}
