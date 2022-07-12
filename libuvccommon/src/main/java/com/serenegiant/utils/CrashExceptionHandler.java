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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;

public final class CrashExceptionHandler implements UncaughtExceptionHandler {
	/* package */static final String LOG_NAME = "crashrepo.txt";
	/* package */static final String MAIL_TO = "t_saki@serenegiant.com";

	public static void registerCrashHandler(@NonNull final Context app_context) {
		Thread.setDefaultUncaughtExceptionHandler(new CrashExceptionHandler(app_context));
	}

/*	public static final void sendReport(final Context context) {
		final File file = getFileStreamPath(this, LOG_NAME);
		if (file.exists() && checkSdCardStatus()) {
		    final String attachmentFilePath = Environment.getExternalStorageDirectory().getPath() + File.separator + getString(R.string.appName) + File.separator + CrashExceptionHandler.FILE_NAME;
		    final File attachmentFile = new File(attachmentFilePath);
		    if(!attachmentFile.getParentFile().exists()){
		        attachmentFile.getParentFile().mkdirs();
		    }
		    file.renameTo(attachmentFile);
		    Intent intent = createSendMailIntent(MAIL_TO, "crash report", "********** crash report **********");
		    intent = addFile(intent, attachmentFile);
		    final Intent gmailIntent = createGmailIntent(intent);
		    if (canIntent(this, gmailIntent)){
		        startActivity(gmailIntent);
		    } else if (canIntent(this, intent)) {
		        startActivity(Intent.createChooser(intent, getString(R.string.sendCrashReport)));
		    } else {
		        showToast(context, R.string.mailerNotFound);
		    }
		    file.delete();
		}
	} */

	private final WeakReference<Context> mWeakContext;
	private final WeakReference<PackageInfo> mWeakPackageInfo;
	private final UncaughtExceptionHandler mHandler;

	private CrashExceptionHandler(@NonNull final Context context) {
		mWeakContext = new WeakReference<Context>(context);
		try {
			mWeakPackageInfo = new WeakReference<PackageInfo>(
				context.getPackageManager().getPackageInfo(context.getPackageName(), 0));
		} catch (final NameNotFoundException e) {
			throw new RuntimeException(e);
		}
		mHandler = Thread.getDefaultUncaughtExceptionHandler();
	}

	/**
	 * キャッチされなかった例外発生時に各種情報をJSONでテキストファイルに書き出す
	 */
	@Override
	public void uncaughtException(final Thread thread, final Throwable throwable) {
		final Context context = mWeakContext.get();
		if (context != null) {
			PrintWriter writer = null;
			try {
				final FileOutputStream outputStream = context.openFileOutput(LOG_NAME, Context.MODE_PRIVATE);
				writer = new PrintWriter(outputStream);
				final JSONObject json = new JSONObject();
				json.put("Build", getBuildInfo());
				json.put("PackageInfo", getPackageInfo());
				json.put("Exception", getExceptionInfo(throwable));
				json.put("SharedPreferences", getPreferencesInfo());
				writer.print(json.toString());
				writer.flush();
			} catch (final FileNotFoundException e) {
				e.printStackTrace();
			} catch (final JSONException e) {
				e.printStackTrace();
			} finally {
				if (writer != null) {
					writer.close();
				}
			}
		}
		try {
			if (mHandler != null)
				mHandler.uncaughtException(thread, throwable);
		} catch (final Exception e) {
			// ignore
		}
	}

	/**
	 * ビルド情報をJSONで返す
	 *
	 * @return
	 * @throws JSONException
	 */
	private JSONObject getBuildInfo() throws JSONException {
		final JSONObject buildJson = new JSONObject();
		buildJson.put("BRAND", Build.BRAND);	// キャリア、メーカー名など
		buildJson.put("MODEL", Build.MODEL);	// ユーザーに表示するモデル名
		buildJson.put("DEVICE", Build.DEVICE);	// デバイス名
		buildJson.put("MANUFACTURER", Build.MANUFACTURER);			// 製造者名
		buildJson.put("VERSION.SDK_INT", Build.VERSION.SDK_INT);	// フレームワークのバージョン情報
		buildJson.put("VERSION.RELEASE", Build.VERSION.RELEASE);	// ユーザーに表示するバージョン番号
		return buildJson;
	}

	/**
	 * パッケージ情報を返す
	 *
	 * @return
	 * @throws JSONException
	 */
	private JSONObject getPackageInfo() throws JSONException {
		final PackageInfo info = mWeakPackageInfo.get();
		final JSONObject packageInfoJson = new JSONObject();
		if (info != null) {
			packageInfoJson.put("packageName", info.packageName);
			packageInfoJson.put("versionCode", info.versionCode);
			packageInfoJson.put("versionName", info.versionName);
		}
		return packageInfoJson;
	}

	/**
	 * 例外情報を返す
	 *
	 * @param throwable
	 * @return
	 * @throws JSONException
	 */
	private JSONObject getExceptionInfo(final Throwable throwable) throws JSONException {
		final JSONObject exceptionJson = new JSONObject();
		exceptionJson.put("name", throwable.getClass().getName());
		exceptionJson.put("cause", throwable.getCause());
		exceptionJson.put("message", throwable.getMessage());
		// StackTrace
		final JSONArray stackTrace = new JSONArray();
		for (final StackTraceElement element : throwable.getStackTrace()) {
			stackTrace.put("at " + LogUtils.getMetaInfo(element));
		}
		exceptionJson.put("stacktrace", stackTrace);
		return exceptionJson;
	}

	/**
	 * Preferencesを返す
	 *
	 * @return
	 * @throws JSONException
	 */
	private JSONObject getPreferencesInfo() throws JSONException {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mWeakContext.get());
		final JSONObject preferencesJson = new JSONObject();
		final Map<String, ?> map = preferences.getAll();
		for (final Entry<String, ?> entry : map.entrySet()) {
			preferencesJson.put(entry.getKey(), entry.getValue());
		}
		return preferencesJson;
	}
}
