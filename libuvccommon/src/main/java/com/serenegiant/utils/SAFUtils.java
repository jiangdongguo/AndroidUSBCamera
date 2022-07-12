package com.serenegiant.utils;
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
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.StatFs;
import android.provider.DocumentsContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.documentfile.provider.DocumentFile;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class SAFUtils {
	private static final String TAG = SAFUtils.class.getSimpleName();

//********************************************************************************
// Storage Access Framework関係
//********************************************************************************
	
	/**
	 * ActivityまたはFragmentの#onActivityResultメソッドの処理のうち
	 * Storage Access Framework関係の処理を行うためのdelegater
	 */
	public interface handleOnResultDelegater {
		public boolean onResult(final int requestCode, final Uri uri, final Intent data);
		public void onFailed(final int requestCode, final Intent data);
	}
	
	/**
	 * ActivityまたはFragmentの#onActivityResultメソッドの処理をdelegaterで
	 * 処理するためのヘルパーメソッド
	 * @param context
	 * @param requestCode
	 * @param resultCode
	 * @param data
	 * @param delegater
	 * @return true if successfully handled, false otherwise
	 */
	public static boolean handleOnResult(@NonNull final Context context,
		final int requestCode, final int resultCode,
		final Intent data, @NonNull final SAFUtils.handleOnResultDelegater delegater) {

		if (data != null) {
			if (resultCode == Activity.RESULT_OK) {
				final Uri uri = data.getData();
				if (uri != null) {
					try {
						return delegater.onResult(requestCode, uri, data);
					} catch (final Exception e) {
						Log.w(TAG, e);
					}
				}
			}
		}
		try {
			clearUri(context, getKey(requestCode));
			delegater.onFailed(requestCode, data);
		} catch (final Exception e) {
			Log.w(TAG, e);
		}
		return false;
	}
	
	/**
	 * uriを保存する際に使用する共有プレファレンスのキー名を要求コードから生成する
	 * @param requestCode
	 * @return
	 */
	@NonNull
	private static String getKey(final int requestCode) {
		return String.format(Locale.US, "SDUtils-%d", requestCode);	// XXX ここは互換性維持のためにSDUtilsの名を残す
	}
	
	/**
	 * uriを共有プレファレンスに保存する
	 * @param context
	 * @param key
	 * @param uri
	 */
	private static void saveUri(@NonNull final Context context,
		@NonNull final String key, @NonNull final Uri uri) {
		final SharedPreferences pref = context.getSharedPreferences(context.getPackageName(), 0);
		if (pref != null) {
			pref.edit().putString(key, uri.toString()).apply();
		}
	}
	
	/**
	 * 共有プレファレンスの保存しているuriを取得する
	 * @param context
	 * @param key
	 * @return
	 */
	@Nullable
	private static Uri loadUri(@NonNull final Context context, @NonNull final String key) {
		Uri result = null;
		final SharedPreferences pref = context.getSharedPreferences(context.getPackageName(), 0);
		if ((pref != null) && pref.contains(key)) {
			try {
				result = Uri.parse(pref.getString(key, null));
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}
		return result;
	}
	
	/**
	 * 共有プレファレンスに保存しているuriを消去する
	 * @param context
	 * @param key
	 */
	private static void clearUri(@NonNull final Context context, @Nullable final String key) {
		final SharedPreferences pref = context.getSharedPreferences(context.getPackageName(), 0);
		if ((pref != null) && pref.contains(key)) {
			try {
				pref.edit().remove(key).apply();
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}
	}
	
	/**
	 * ファイル読み込み用のUriを要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param activity
	 * @param mime
	 * @param requestCode
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public static void requestOpenDocument(@NonNull final Activity activity,
		final String mime, final int requestCode) {

		if (BuildCheck.isKitKat()) {
			activity.startActivityForResult(prepareOpenDocumentIntent(mime), requestCode);
		}
	}
	
	/**
	 * ファイル読み込み用のUriを要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param activity
	 * @param mime
	 * @param requestCode
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public static void requestOpenDocument(@NonNull final FragmentActivity activity,
		final String mime, final int requestCode) {

		if (BuildCheck.isKitKat()) {
			activity.startActivityForResult(prepareOpenDocumentIntent(mime), requestCode);
		}
	}
	
	/**
	 * ファイル読み込み用のUriを要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param fragment
	 * @param mime
	 * @param requestCode
	 */
	@Deprecated
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public static void requestOpenDocument(@NonNull final android.app.Fragment fragment,
		final String mime, final int requestCode) {

		if (BuildCheck.isKitKat()) {
			fragment.startActivityForResult(prepareOpenDocumentIntent(mime), requestCode);
		}
	}
	
	/**
	 * ファイル読み込み用のUriを要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param fragment
	 * @param mime
	 * @param requestCode
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public static void requestOpenDocument(@NonNull final Fragment fragment,
		final String mime, final int requestCode) {

		if (BuildCheck.isKitKat()) {
			fragment.startActivityForResult(prepareOpenDocumentIntent(mime), requestCode);
		}
	}
	
	/**
	 * ファイル読み込み用のUriを要求するヘルパーメソッド
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param mime
	 * @return
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	private static Intent prepareOpenDocumentIntent(@NonNull final String mime) {
		final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		intent.setType(mime);
		return intent;
	}
	
	/**
	 * ファイル保存用のUriを要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param activity
	 * @param mime
	 * @param requestCode
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public static void requestCreateDocument(@NonNull final Activity activity,
		final String mime, final int requestCode) {

		if (BuildCheck.isKitKat()) {
			activity.startActivityForResult(prepareCreateDocument(mime, null), requestCode);
		}
	}
	
	/**
	 * ファイル保存用のUriを要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param activity
	 * @param mime
	 * @param defaultName
	 * @param requestCode
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public static void requestCreateDocument(@NonNull final Activity activity,
		final String mime, final String defaultName, final int requestCode) {

		if (BuildCheck.isKitKat()) {
			activity.startActivityForResult(prepareCreateDocument(mime, defaultName), requestCode);
		}
	}
	
	/**
	 * ファイル保存用のUriを要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param activity
	 * @param mime
	 * @param requestCode
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public static void requestCreateDocument(@NonNull final FragmentActivity activity,
		final String mime, final int requestCode) {

		if (BuildCheck.isKitKat()) {
			activity.startActivityForResult(prepareCreateDocument(mime, null), requestCode);
		}
	}
	
	/**
	 * ファイル保存用のUriを要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param activity
	 * @param mime
	 * @param defaultName
	 * @param requestCode
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public static void requestCreateDocument(@NonNull final FragmentActivity activity,
		final String mime, final String defaultName, final int requestCode) {

		if (BuildCheck.isKitKat()) {
			activity.startActivityForResult(prepareCreateDocument(mime, defaultName), requestCode);
		}
	}
	
	/**
	 * ファイル保存用のUriを要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param fragment
	 * @param mime
	 * @param requestCode
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public static void requestCreateDocument(@NonNull final android.app.Fragment fragment,
		final String mime, final int requestCode) {

		if (BuildCheck.isKitKat()) {
			fragment.startActivityForResult(prepareCreateDocument(mime, null), requestCode);
		}
	}
	
	/**
	 * ファイル保存用のUriを要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param fragment
	 * @param mime
	 * @param defaultName
	 * @param requestCode
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public static void requestCreateDocument(@NonNull final android.app.Fragment fragment,
		final String mime, final String defaultName, final int requestCode) {

		if (BuildCheck.isKitKat()) {
			fragment.startActivityForResult(prepareCreateDocument(mime, defaultName), requestCode);
		}
	}
	
	/**
	 * ファイル保存用のUriを要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param fragment
	 * @param mime
	 * @param requestCode
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public static void requestCreateDocument(@NonNull final Fragment fragment,
		final String mime, final int requestCode) {

		if (BuildCheck.isKitKat()) {
			fragment.startActivityForResult(prepareCreateDocument(mime, null), requestCode);
		}
	}
	
	/**
	 * ファイル保存用のUriを要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
 	 * @param fragment
	 * @param mime
	 * @param defaultName
	 * @param requestCode
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public static void requestCreateDocument(@NonNull final Fragment fragment,
		final String mime, final String defaultName, final int requestCode) {

		if (BuildCheck.isKitKat()) {
			fragment.startActivityForResult(prepareCreateDocument(mime, defaultName), requestCode);
		}
	}
	
	/**
	 * ファイル保存用のUriを要求するヘルパーメソッド
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param mime
	 * @param defaultName
	 * @return
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	private static Intent prepareCreateDocument(final String mime, final String defaultName) {
		final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		intent.setType(mime);
		if (!TextUtils.isEmpty(defaultName)) {
			intent.putExtra(Intent.EXTRA_TITLE, defaultName);
		}
		return intent;
	}

	/**
	 * ファイル削除要求
	 * KITKAT以降で個別のファイル毎にパーミッション要求する場合
	 * @param context
	 * @param uri
	 * @return
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public static boolean requestDeleteDocument(@NonNull final Context context, final Uri uri) {
		try {
			return BuildCheck.isKitKat()
				&& DocumentsContract.deleteDocument(context.getContentResolver(), uri);
		} catch (final FileNotFoundException e) {
			return false;
		}
	}

	/**
	 * requestCodeに対応するUriへアクセス可能かどうか
	 * @param context
	 * @param requestCode
	 * @return
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public static boolean hasStorageAccess(@NonNull final Context context,
		final int requestCode) {

		boolean found = false;
		if (BuildCheck.isLollipop()) {
			final Uri uri = loadUri(context, getKey(requestCode));
			if (uri != null) {
				// 恒常的に保持しているUriパーミッションの一覧を取得する
				final List<UriPermission> list
					= context.getContentResolver().getPersistedUriPermissions();
				for (final UriPermission item: list) {
					if (item.getUri().equals(uri)) {
						// requestCodeに対応するUriへのパーミッションを恒常的に保持していた時
						found = true;
						break;
					}
				}
			}
		}
		return found;
	}

	/**
	 * requestCodeに対応するUriへのアクセス要求を行う
	 * @param activity
	 * @param requestCode
	 * @return 既にrequestCodeに対応するUriが存在していればそれを返す, 存在していなければパーミッション要求をしてnullを返す
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static Uri requestStorageAccess(@NonNull final Activity activity,
		final int requestCode) {

		if (BuildCheck.isLollipop()) {
			final Uri uri = getStorageUri(activity, requestCode);
			if (uri == null) {
				// requestCodeに対応するUriへのパーミッションを保持していない時は要求してnullを返す
				activity.startActivityForResult(prepareStorageAccessPermission(), requestCode);
			}
			return uri;
		}
		return null;
	}

	/**
	 * requestCodeに対応するUriへのアクセス要求を行う
	 * @param activity
	 * @param requestCode
	 * @return 既にrequestCodeに対応するUriが存在していればそれを返す, 存在していなければパーミッション要求をしてnullを返す
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static Uri requestStorageAccess(@NonNull final FragmentActivity activity,
		final int requestCode) {

		if (BuildCheck.isLollipop()) {
			final Uri uri = getStorageUri(activity, requestCode);
			if (uri == null) {
				// requestCodeに対応するUriへのパーミッションを保持していない時は要求してnullを返す
				activity.startActivityForResult(prepareStorageAccessPermission(), requestCode);
			}
			return uri;
		}
		return null;
	}

	/**
	 * requestCodeに対応するUriへのアクセス要求を行う
	 * @param fragment
	 * @param requestCode
	 * @return 既にrequestCodeに対応するUriが存在していればそれを返す, 存在していなければパーミッション要求をしてnullを返す
	 */
	@Deprecated
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static Uri requestStorageAccess(@NonNull final android.app.Fragment fragment,
		final int requestCode) {

		final Uri uri = getStorageUri(fragment.getActivity(), requestCode);
		if (uri == null) {
			// requestCodeに対応するUriへのパーミッションを保持していない時は要求してnullを返す
			fragment.startActivityForResult(prepareStorageAccessPermission(), requestCode);
		}
		return uri;
	}

	/**
	 * requestCodeに対応するUriへのアクセス要求を行う
	 * @param fragment
	 * @param requestCode
	 * @return 既にrequestCodeに対応するUriが存在していればそれを返す, 存在していなければパーミッション要求をしてnullを返す
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static Uri requestStorageAccess(@NonNull final Fragment fragment,
		final int requestCode) {

		if (BuildCheck.isLollipop()) {
			final Activity activity = fragment.getActivity();
			final Uri uri = activity != null ? getStorageUri(activity, requestCode) : null;
			if (uri == null) {
				// requestCodeに対応するUriへのパーミッションを保持していない時は要求してnullを返す
				fragment.startActivityForResult(prepareStorageAccessPermission(), requestCode);
			}
			return uri;
		}
		return null;
	}

	/**
	 * requestCodeに対応するUriが存在していて恒常的パーミッションがあればそれを返す, なければnullを返す
	 * @param context
	 * @param requestCode
	 * @return
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	@Nullable
	public static Uri getStorageUri(@NonNull final Context context,
		final int requestCode) {

		if (BuildCheck.isLollipop()) {
			final Uri uri = loadUri(context, getKey(requestCode));
			if (uri != null) {
				boolean found = false;
				// 恒常的に保持しているUriパーミッションの一覧を取得する
				final List<UriPermission> list
					= context.getContentResolver().getPersistedUriPermissions();
				for (final UriPermission item: list) {
					if (item.getUri().equals(uri)) {
						// requestCodeに対応するUriへのパーミッションを恒常的に保持していた時
						found = true;
						break;
					}
				}
				if (found) {
					return uri;
				}
			}
		}
		return null;
	}

	/**
	 * requestStorageAccessの下請け
	 * @return
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private static Intent prepareStorageAccessPermission() {
		return new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
	}

	/**
	 * 恒常的にアクセスできるようにパーミッションを要求する
	 * @param context
	 * @param treeUri
	 * @return
	 */
	@Nullable
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static Uri requestStorageAccessPermission(@NonNull final Context context,
		final int requestCode, final Uri treeUri) {

		return requestStorageAccessPermission(context,
			requestCode, treeUri,
			Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
	}
	
	/**
	 * 恒常的にアクセスできるようにパーミッションを要求する
	 * @param context
	 * @param treeUri
	 * @param flags
	 * @return
	 */
	@Nullable
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static Uri requestStorageAccessPermission(@NonNull final Context context,
		final int requestCode, final Uri treeUri, final int flags) {

		if (BuildCheck.isLollipop()) {
			context.getContentResolver().takePersistableUriPermission(treeUri, flags);
			saveUri(context, getKey(requestCode), treeUri);
			return treeUri;
		} else {
			return null;
		}
	}
	
	/**
	 * 恒常的にアクセスできるように取得したパーミッションを開放する
	 * @param context
	 * @param requestCode
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static void releaseStorageAccessPermission(@NonNull final Context context,
		final int requestCode) {

		if (BuildCheck.isLollipop()) {
			final String key = getKey(requestCode);
			final Uri uri = loadUri(context, key);
			if (uri != null) {
				context.getContentResolver().releasePersistableUriPermission(uri,
					Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
				clearUri(context, key);
			}
		}
	}

//================================================================================
	public interface FileFilter {
		public boolean accept(@NonNull final DocumentFile file);
	}
	
	/**
	 * 指定したidに対応するUriが存在する時に対応するDocumentFileを返す
	 * @param context
	 * @param treeId
	 * @return
	 */
	@Nullable
	public static DocumentFile getStorage(@NonNull final Context context,
		final int treeId) throws IOException {
		
		return getStorage(context, treeId, null);
	}
	
	/**
	 * 指定したidに対応するUriが存在して書き込み可能であればその下にディレクトリを生成して
	 * そのディレクトリを示すDocumentFileオブジェクトを返す
	 * @param context
	 * @param treeId
	 * @param dirs スラッシュ(`/`)で区切られたパス文字列
	 * @return 一番下のディレクトリに対応するDocumentFile, Uriが存在しないときや書き込めない時はnull
	 */
	@Nullable
	public static DocumentFile getStorage(@NonNull final Context context,
		final int treeId, @Nullable final String dirs) throws IOException {

		if (BuildCheck.isLollipop()) {
			final Uri treeUri = getStorageUri(context, treeId);
			if (treeUri != null) {
				DocumentFile tree = DocumentFile.fromTreeUri(context, treeUri);
				if (!TextUtils.isEmpty(dirs)) {
					final String[] dir = dirs.split("/");
					for (final String d: dir) {
						if (!TextUtils.isEmpty(d)) {
							final DocumentFile t = tree.findFile(d);
							if ((t != null) && t.isDirectory()) {
								// 既に存在している時は何もしない
								tree = t;
							} else if (t == null) {
								if (tree.canWrite()) {
									// 存在しないときはディレクトリを生成
									tree = tree.createDirectory(d);
								} else {
									throw new IOException("can't create directory");
								}
							} else {
								throw new IOException("can't create directory, file with same name already exists");
							}
						}
					}
				}
				return tree;
			}
		}
		return null;
	}
	
	/**
	 * 指定したDocumentFileが書き込み可能であればその下にディレクトリを生成して
	 * そのディレクトリを示すDocumentFileオブジェクトを返す
	 * @param context
	 * @param parent
	 * @param dirs
	 * @return 書き込みができなければnull
	 */
	public static DocumentFile getStorage(@NonNull final Context context,
		@NonNull final DocumentFile parent, @Nullable final String dirs)
			throws IOException {
		
		DocumentFile tree = parent;
		if (!TextUtils.isEmpty(dirs)) {
			final String[] dir = dirs.split("/");
			for (final String d: dir) {
				if (!TextUtils.isEmpty(d)) {
					final DocumentFile t = tree.findFile(d);
					if ((t != null) && t.isDirectory()) {
						// 既に存在している時は何もしない
						tree = t;
					} else if (t == null) {
						if (tree.canWrite()) {
							// 存在しないときはディレクトリを生成
							tree = tree.createDirectory(d);
						} else {
							throw new IOException("can't create directory");
						}
					} else {
						throw new IOException("can't create directory, file with same name already exists");
					}
				}
			}
		}
		return tree;
	}
	
	/**
	 * 指定したディレクトリ配下に存在するファイルの一覧を取得
	 * @param context
	 * @param dir
	 * @param filter nullなら存在するファイルを全て追加
	 * @return
	 * @throws IOException
	 */
	@NonNull
	public static Collection<DocumentFile> listFiles(@NonNull final Context context,
		@NonNull final DocumentFile dir,
		@Nullable final SAFUtils.FileFilter filter) throws IOException {

		final Collection<DocumentFile> result = new ArrayList<DocumentFile>();
		if (dir.isDirectory()) {
			final DocumentFile[] files = dir.listFiles();
			for (final DocumentFile file: files) {
				if ((filter == null) || (filter.accept(file))) {
					result.add(file);
				}
			}
		}
		return result;
	}
	
	/**
	 * 全容量と空き容量を返す
	 * ディレクトリでない場合やアクセス出来ない場合はnullを返す
	 * @param context
	 * @param dir
	 * @return
	 */
	@SuppressLint("NewApi")
	@Nullable
	public static StorageInfo getStorageInfo(@NonNull final Context context,
		@NonNull final DocumentFile dir) {
		
		try {
			final String path = UriHelper.getPath(context, dir.getUri());
			if (path != null) {
				// FIXME もしプライマリーストレージの場合はアクセス権無くても容量取得できるかも
				final File file = new File(path);
				if (file.isDirectory() && file.canRead()) {
					final long total = file.getTotalSpace();
					long free = file.getFreeSpace();
					if (free < file.getUsableSpace()) {
						free = file.getUsableSpace();
					}
					return new StorageInfo(total, free);
				}
			}
		} catch (final Exception e) {
			// ignore
		}
		if (BuildCheck.isJellyBeanMR2()) {
			try {
				final String path = UriHelper.getPath(context, dir.getUri());
				final StatFs fs = new StatFs(path);
				return new StorageInfo(fs.getTotalBytes(), fs.getAvailableBytes());
			} catch (final Exception e) {
				// ignore
			}
		}
		return null;
	}
	
	/**
	 * 指定したUriが存在する時に対応するファイルを参照するためのDocumentFileオブジェクトを生成する
	 * @param context
	 * @param treeId
	 * @param mime
	 * @param name
	 * @return
	 */
	@Nullable
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static DocumentFile getStorageFile(@NonNull final Context context,
		final int treeId, final String mime, final String name) throws IOException {

		return getStorageFile(context, treeId, null, mime, name);
	}

	/**
	 * 指定したUriが存在する時に対応するファイルを参照するためのDocumentFileオブジェクトを生成する
	 * @param context
	 * @param treeId
	 * @param mime
	 * @param name
	 * @return
	 */
	@Nullable
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static DocumentFile getStorageFile(@NonNull final Context context,
		final int treeId, @Nullable final String dirs,
		final String mime, final String name) throws IOException {

		if (BuildCheck.isLollipop()) {
			final DocumentFile tree = getStorage(context, treeId, dirs);
			if (tree != null) {
				final DocumentFile file = tree.findFile(name);
				if (file != null) {
					if (file.isFile()) {
						return file;
					} else {
						throw new IOException("directory with same name already exists");
					}
				} else {
					return tree.createFile(mime, name);
				}
			}
		}
		return null;
	}
	
	/**
	 * 指定したDocumentFileの下にファイルを生成する
	 * dirsがnullまたは空文字列ならDocumentFile#createFileを呼ぶのと同じ
	 * @param context
	 * @param parent
	 * @param dirs
	 * @param mime
	 * @param name
	 * @return
	 */
	public static DocumentFile getStorageFile(@NonNull final Context context,
		@NonNull final DocumentFile parent, @Nullable final String dirs,
		final String mime, final String name) throws IOException {
		
		final DocumentFile tree = getStorage(context, parent, dirs);
		if (tree != null) {
			final DocumentFile file = tree.findFile(name);
			if (file != null) {
				if (file.isFile()) {
					return file;
				} else {
					throw new IOException("directory with same name already exists");
				}
			} else {
				return tree.createFile(mime, name);
			}
		}
		return null;
	}
	
	/**
	 * 指定したUriが存在する時にその下に出力用ファイルを生成してOutputStreamとして返す
	 * @param context
	 * @param treeId
	 * @param mime
	 * @param name
	 * @return
	 * @throws FileNotFoundException
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static OutputStream getStorageOutputStream(@NonNull final Context context,
													  final int treeId,
													  final String mime, final String name) throws IOException {
		
		return getStorageOutputStream(context, treeId, null, mime, name);
	}
	
	/**
	 * 指定したUriが存在する時にその下に出力用ファイルを生成してOutputStreamとして返す
	 * @param context
	 * @param treeId
	 * @param dirs
	 * @param mime
	 * @param name
	 * @return
	 * @throws FileNotFoundException
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static OutputStream getStorageOutputStream(@NonNull final Context context,
		final int treeId, @Nullable final String dirs,
		final String mime, final String name) throws IOException {

		if (BuildCheck.isLollipop()) {
			final DocumentFile tree = getStorage(context, treeId, dirs);
			if (tree != null) {
				final DocumentFile file = tree.findFile(name);
				if (file != null) {
					if (file.isFile()) {
						return context.getContentResolver().openOutputStream(
							file.getUri());
					} else {
						throw new IOException("directory with same name already exists");
					}
				} else {
					return context.getContentResolver().openOutputStream(
						tree.createFile(mime, name).getUri());
				}
			}
		}
		throw new FileNotFoundException();
	}

	/**
	 * 指定したUriが存在する時にその下に出力用ファイルを生成してOutputStreamとして返す
	 * @param context
	 * @param parent
	 * @param dirs
	 * @param mime
	 * @param name
	 * @return
	 * @throws FileNotFoundException
	 */
	public static OutputStream getStorageOutputStream(@NonNull final Context context,
		@NonNull final DocumentFile parent, @Nullable final String dirs,
		final String mime, final String name) throws IOException {

		final DocumentFile tree = getStorage(context, parent, dirs);
		if (tree != null) {
			final DocumentFile file = tree.findFile(name);
			if (file != null) {
				if (file.isFile()) {
					return context.getContentResolver().openOutputStream(
						file.getUri());
				} else {
					throw new IOException("directory with same name already exists");
				}
			} else {
				return context.getContentResolver().openOutputStream(
					tree.createFile(mime, name).getUri());
			}
		}
		throw new FileNotFoundException();
	}

	/**
	 * 指定したUriが存在する時にその下に入力用ファイルを生成してInputStreamとして返す
	 * @param context
	 * @param treeId
	 * @param mime
	 * @param name
	 * @return
	 * @throws FileNotFoundException
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static InputStream getStorageInputStream(@NonNull final Context context,
													final int treeId,
													final String mime, final String name) throws IOException {
		
		return getStorageInputStream(context, treeId, null, mime, name);
	}
	
	/**
	 * 指定したUriが存在する時にその下の入力用ファイルをInputStreamとして返す
	 * @param context
	 * @param treeId
	 * @param dirs
	 * @param mime
	 * @param name
	 * @return
	 * @throws FileNotFoundException
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static InputStream getStorageInputStream(@NonNull final Context context,
		final int treeId, @Nullable final String dirs,
		final String mime, final String name) throws IOException {

		if (BuildCheck.isLollipop()) {
			final DocumentFile tree = getStorage(context, treeId, dirs);
			if (tree != null) {
				final DocumentFile file = tree.findFile(name);
				if (file != null) {
					if (file.isFile()) {
						return context.getContentResolver().openInputStream(
							file.getUri());
					} else {
						throw new IOException("directory with same name already exists");
					}
				}
			}
		}
		throw new FileNotFoundException();
	}
	
	/**
	 * 指定したUriが存在する時にその下に出力用ファイルを生成してOutputStreamとして返す
	 * @param context
	 * @param parent
	 * @param dirs
	 * @param mime
	 * @param name
	 * @return
	 * @throws FileNotFoundException
	 */
	public static InputStream getStorageInputStream(@NonNull final Context context,
		@NonNull final DocumentFile parent, @Nullable final String dirs,
		final String mime, final String name) throws IOException {

		final DocumentFile tree = getStorage(context, parent, dirs);
		if (tree != null) {
			final DocumentFile file = tree.findFile(name);
			if (file != null) {
				if (file.isFile()) {
					return context.getContentResolver().openInputStream(
						file.getUri());
				} else {
					throw new IOException("directory with same name already exists");
				}
			}
		}
		throw new FileNotFoundException();
	}

	/**
	 * 指定したUriが存在する時にその下に入力用ファイルを生成して入出力用のファイルディスクリプタを返す
	 * @param context
	 * @param treeId
	 * @param dirs
	 * @param mime
	 * @param name
	 * @return
	 * @throws FileNotFoundException
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static ParcelFileDescriptor getStorageFileFD(@NonNull final Context context,
														final int treeId, @Nullable final String dirs,
														final String mime, final String name) throws IOException {

		if (BuildCheck.isLollipop()) {
			final DocumentFile tree = getStorage(context, treeId, dirs);
			if (tree != null) {
				final DocumentFile file = tree.findFile(name);
				if (file != null) {
					if (file.isFile()) {
						return context.getContentResolver().openFileDescriptor(
							file.getUri(), "rw");
					} else {
						throw new IOException("directory with same name already exists");
					}
				} else {
					return context.getContentResolver().openFileDescriptor(
						tree.createFile(mime, name).getUri(), "rw");
				}
			}
		}
		throw new FileNotFoundException();
	}
	
	/**
	 * 指定したDocumentFileの示すディレクトリが存在していれば入出力用のファイルディスクリプタを返す
	 * @param context
	 * @param parent
	 * @param dirs
	 * @param mime
	 * @param name
	 * @return
	 * @throws IOException
	 */
	public static ParcelFileDescriptor getStorageFileFD(@NonNull final Context context,
		@NonNull final DocumentFile parent, @Nullable final String dirs,
		final String mime, final String name) throws IOException {

		final DocumentFile tree = getStorage(context, parent, dirs);
		if (tree != null) {
			final DocumentFile file = tree.findFile(name);
			if (file != null) {
				if (file.isFile()) {
					return context.getContentResolver().openFileDescriptor(
						file.getUri(), "rw");
				} else {
					throw new IOException("directory with same name already exists");
				}
			} else {
				return context.getContentResolver().openFileDescriptor(
					tree.createFile(mime, name).getUri(), "rw");
			}
		}
		throw new FileNotFoundException();
	}
	
//================================================================================
	/**
	 * 指定したidに対応するUriが存在する時にその下にファイルを生成するためのpathを返す
	 * @param context
	 * @param treeId
	 * @return
	 */
	@Nullable
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static File createStorageDir(@NonNull final Context context,
		final int treeId) {

		if (BuildCheck.isLollipop()) {
			final Uri treeUri = getStorageUri(context, treeId);
			if (treeUri != null) {
				final DocumentFile saveTree = DocumentFile.fromTreeUri(context, treeUri);
				final String path = UriHelper.getPath(context, saveTree.getUri());
				if (!TextUtils.isEmpty(path)) {
					return new File(path);
				}
			}
		}
		return null;
	}

	/**
	 * 指定したidに対応するUriが存在する時にその下に指定したFileを生成して返す
	 * @param context
	 * @param treeId
	 * @param mime
	 * @param fileName
	 * @return
	 */
	@Nullable
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static File createStorageFile(@NonNull final Context context,
		final int treeId, final String mime, final String fileName) {

		return createStorageFile(context, getStorageUri(context, treeId), mime, fileName);
	}

	/**
	 * 指定したUriが存在する時にその下にファイルを生成するためのpathを返す
	 * @param context
	 * @param treeUri
	 * @param mime
	 * @param fileName
	 * @return
	 */
	@Nullable
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static File createStorageFile(@NonNull final Context context,
		final Uri treeUri, final String mime, final String fileName) {
		Log.i(TAG, "createStorageFile:" + fileName);

		if (BuildCheck.isLollipop()) {
			if ((treeUri != null) && !TextUtils.isEmpty(fileName)) {
				final DocumentFile saveTree = DocumentFile.fromTreeUri(context, treeUri);
				final DocumentFile target = saveTree.createFile(mime, fileName);
				final String path = UriHelper.getPath(context, target.getUri());
				if (!TextUtils.isEmpty(path)) {
					return new File(path);
				}
			}
		}
		return null;
	}

	/**
	 * 指定したidに対応するUriが存在する時にその下に生成したファイルのrawファイルディスクリプタを返す
	 * @param context
	 * @param treeId
	 * @param mime
	 * @param fileName
	 * @return
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static int createStorageFileFD(@NonNull final Context context,
		final int treeId, final String mime, final String fileName) {

		Log.i(TAG, "createStorageFileFD:" + fileName);
		return createStorageFileFD(context, getStorageUri(context, treeId), mime, fileName);
	}

	/**
	 * 指定したidに対応するUriが存在する時にその下に生成したファイルのrawファイルディスクリプタを返す
	 * @param context
	 * @param treeUri
	 * @param mime
	 * @param fileName
	 * @return
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static int createStorageFileFD(@NonNull final Context context,
		final Uri treeUri, final String mime, final String fileName) {

		Log.i(TAG, "createStorageFileFD:" + fileName);
		if (BuildCheck.isLollipop()) {
			if ((treeUri != null) && !TextUtils.isEmpty(fileName)) {
				final DocumentFile saveTree = DocumentFile.fromTreeUri(context, treeUri);
				final DocumentFile target = saveTree.createFile(mime, fileName);
				try {
					final ParcelFileDescriptor fd
						= context.getContentResolver().openFileDescriptor(target.getUri(), "rw");
					return fd != null ? fd.getFd() : 0;
				} catch (final FileNotFoundException e) {
					Log.w(TAG, e);
				}
			}
		}
		return 0;
	}

	/**
	 * 指定したDocumentFileが指し示すフォルダの下に指定した相対パスのディレクトリ階層を生成する
	 * フォルダが存在していない時に書き込み可能でなければIOExceptionを投げる
	 * @param context
	 * @param baseDoc
	 * @param dirs
	 * @return
	 * @throws IOException
	 */
	public static DocumentFile getDocumentFile(@NonNull Context context,
		@NonNull final DocumentFile baseDoc, @Nullable final String dirs)
			throws IOException {
		
		DocumentFile tree = baseDoc;
		if (!TextUtils.isEmpty(dirs)) {
			final String[] dir = dirs.split("/");
			for (final String d: dir) {
				if (!TextUtils.isEmpty(d)) {
					final DocumentFile t = tree.findFile(d);
					if ((t != null) && t.isDirectory()) {
						// 既に存在している時は何もしない
						tree = t;
					} else if (t == null) {
						if (tree.canWrite()) {
							// 存在しないときはディレクトリを生成
							tree = tree.createDirectory(d);
						} else {
							throw new IOException("can't create directory");
						}
					} else {
						throw new IOException("can't create directory, file with same name already exists");
					}
				}
			}
		}
		return tree;
	}
	
	/**
	 * 指定したUriがDocumentFileの下に存在するフォルダを指し示していれば
	 * 対応するDocumentFileを取得して返す
	 * フォルダが存在していない時に書き込み可能でなければIOExceptionを投げる
	 * @param context
	 * @param baseDoc
	 * @param uri
	 * @return
	 * @throws IOException
	 */
	public static DocumentFile getDocumentFile(@NonNull Context context,
		@NonNull final DocumentFile baseDoc, @Nullable final Uri uri)
			throws IOException {
		
		if (uri != null) {
			final String basePathString = UriHelper.getPath(context, baseDoc.getUri());
			final String uriString = UriHelper.getPath(context, uri);
			if (!TextUtils.isEmpty(basePathString)
				&& !TextUtils.isEmpty(uriString)
				&& uriString.startsWith(basePathString)) {
				
				return getDocumentFile(context, baseDoc,
					uriString.substring(basePathString.length()));
			}
		}
		return null;
	}
}
