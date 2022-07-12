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

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import android.text.TextUtils;

import java.io.IOException;

/**
 * Created by saki on 2018/03/21.
 *
 * @Deprecated SAFUtilsクラスへ移行すること
 */
@Deprecated
public class DocumentFileHelper {
	
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
