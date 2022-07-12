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

package com.serenegiant.service;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.serenegiant.common.R;
import com.serenegiant.utils.BuildCheck;
import com.serenegiant.utils.HandlerThreadHandler;

import java.util.List;

public abstract class BaseService extends Service {
	private static final boolean DEBUG = false;	// FIXME set false on production
	private static final String TAG = BaseService.class.getSimpleName();

	private static final int NOTIFICATION_ID = R.string.service_name;

	protected final Object mSync = new Object();
	private final Handler mUIHandler = new Handler(Looper.getMainLooper());
	private Handler mAsyncHandler;
	private LocalBroadcastManager mLocalBroadcastManager;
	private volatile boolean mDestroyed;

	@Override
	public void onCreate() {
		super.onCreate();
		if (DEBUG) Log.v(TAG, "onCreate:");
		final Context app_context = getApplicationContext();
		synchronized (mSync) {
			mLocalBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
			final IntentFilter filter = createIntentFilter();
			if ((filter != null) && filter.countActions() > 0) {
				mLocalBroadcastManager.registerReceiver(mLocalBroadcastReceiver, filter);
			}
			if (mAsyncHandler == null) {
				mAsyncHandler = HandlerThreadHandler.createHandler(getClass().getSimpleName());
			}
		}
	}

	@Override
	public void onDestroy() {
		if (DEBUG) Log.v(TAG, "onDestroy:");
		mDestroyed = true;
		synchronized (mSync) {
			mUIHandler.removeCallbacksAndMessages(null);
			if (mAsyncHandler != null) {
				mAsyncHandler.removeCallbacksAndMessages(null);
				try {
					mAsyncHandler.getLooper().quit();
				} catch (final Exception e) {
					// ignore
				}
				mAsyncHandler = null;
			}
			if (mLocalBroadcastManager != null) {
				try {
					mLocalBroadcastManager.unregisterReceiver(mLocalBroadcastReceiver);
				} catch (final Exception e) {
					// ignore
				}
				mLocalBroadcastManager = null;
			}
		}
		super.onDestroy();
	}

	protected boolean isDestroyed() {
		return mDestroyed;
	}

	/**
	 * create IntentFilter to receive local broadcast
	 * @return null if you don't want to receive local broadcast
	 */
	protected abstract IntentFilter createIntentFilter();

	/** BroadcastReceiver to receive local broadcast */
	private final BroadcastReceiver mLocalBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			if (DEBUG) Log.v(TAG, "onReceive:" + intent);
			try {
				onReceiveLocalBroadcast(context, intent);
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}
	};

	protected abstract void onReceiveLocalBroadcast(final Context context, final Intent intent);

	/**
	 * local broadcast asynchronously
	 * @param intent
	 */
	protected void sendLocalBroadcast(final Intent intent) {
		synchronized (mSync) {
			if (mLocalBroadcastManager != null) {
				mLocalBroadcastManager.sendBroadcast(intent);
			}
		}
	}

//================================================================================
	
	/**
	 * Notification生成用のファクトリークラス
	 * XXX 単独クラスにするかも
	 */
	public static abstract class NotificationFactory {
	
		protected final String channelId;
		protected final String channelTitle;
		protected final int importance;
		protected final String groupId;
		protected final String groupName;
		@DrawableRes
		protected final int smallIconId;
		@DrawableRes
		protected final int largeIconId;
		
		@SuppressLint("InlinedApi")
		public NotificationFactory(
			@NonNull final String channelId, @Nullable final String channelTitle,
			@DrawableRes final int smallIconId) {
	
			this(channelId, channelId,
				BuildCheck.isAndroid7() ? NotificationManager.IMPORTANCE_NONE : 0,
				null, null, smallIconId, R.drawable.ic_notification);
		}

		@SuppressLint("InlinedApi")
		public NotificationFactory(
			@NonNull final String channelId, @Nullable final String channelTitle,
			@DrawableRes final int smallIconId, @DrawableRes final int largeIconId) {

			this(channelId, channelId,
				BuildCheck.isAndroid7() ? NotificationManager.IMPORTANCE_NONE : 0,
				null, null, smallIconId, largeIconId);
		}

		public NotificationFactory(
			@NonNull final String channelId,
			@Nullable final String channelTitle,
			final int importance,
			@Nullable final String groupId, @Nullable final String groupName,
			@DrawableRes final int smallIconId, @DrawableRes final int largeIconId) {
			
			this.channelId = channelId;
			this.channelTitle = TextUtils.isEmpty(channelTitle) ? channelId : channelTitle;
			this.importance = importance;
			this.groupId = groupId;
			this.groupName = TextUtils.isEmpty(groupName) ? groupId : groupName;
			this.smallIconId = smallIconId;
			this.largeIconId = largeIconId;
		}
		
		/**
		 * Notification生成用のファクトリーメソッド
		 * @param context
		 * @param title
		 * @param content
		 * @return
		 */
		@SuppressLint("NewApi")
		protected Notification createNotification(final Context context,
			@NonNull final CharSequence title, @NonNull final CharSequence content) {

			if (BuildCheck.isOreo()) {
				createNotificationChannel(context);
			}
			
			final NotificationCompat.Builder builder
				= createNotificationBuilder(context, title, content);
			
			return builder.build();
		}
		
		/**
		 * Android 8以降用にNotificationChannelを生成する処理
		 * NotificationManager#getNotificationChannelがnullを
		 * 返したときのみ新規に作成する
		 * #createNotificationrから呼ばれる
		 * showNotification
		 * 	-> createNotificationBuilder
		 * 		-> (createNotificationChannel
		 * 			-> (createNotificationChannelGroup)
		 * 			-> setupNotificationChannel)
		 * 	-> createNotification
		 * 	-> startForeground -> NotificationManager#notify
		 * @param context
		 * @return
		 */
		@TargetApi(Build.VERSION_CODES.O)
		protected void createNotificationChannel(
			@NonNull final Context context) {
	
			final NotificationManager manager
				= (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
			if (manager.getNotificationChannel(channelId) == null) {
				final NotificationChannel channel
					= new NotificationChannel(channelId, channelTitle, importance);
				if (!TextUtils.isEmpty(groupId)) {
					createNotificationChannelGroup(context, groupId, groupName);
					channel.setGroup(groupId);
				}
				channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
				manager.createNotificationChannel(setupNotificationChannel(channel));
			}
		}
		
		/**
		 * Android 8以降用にNotificationChannelを生成する処理
		 * NotificationManager#getNotificationChannelがnullを
		 * 返したときのみ新規に作成する
		 * NotificationManager#createNotificationChannelが呼ばれる直前に
		 * #createNotificationChannelrから呼ばれる
		 * @param channel
		 * @return
		 */
		@NonNull
		protected NotificationChannel setupNotificationChannel(
			@NonNull final NotificationChannel channel) {
			
			return channel;
		}
		
		/**
		 * Android 8以降用にNotificationChannelGroupを生成する処理
		 * NotificationManager#getNotificationChannelGroupsに同じグループidの
		 * ものが存在しない時のみ新規に作成する
		 * #createNotificationBuilderから呼ばれる
		 * showNotification
		 * 	-> createNotificationBuilder
		 * 		-> (createNotificationChannel
		 * 			-> (createNotificationChannelGroup)
		 * 			-> setupNotificationChannel)
		 * 	-> createNotification
		 * 	-> startForeground -> NotificationManager#notify
		 * @param groupId
		 * @param groupName
		 * @return
		 */
		@TargetApi(Build.VERSION_CODES.O)
		protected void createNotificationChannelGroup(
			@NonNull final Context context,
			@Nullable final String groupId, @Nullable final String groupName) {
			
			if (!TextUtils.isEmpty(groupId)) {
				final NotificationManager manager
					= (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
				final List<NotificationChannelGroup> groups
					= manager.getNotificationChannelGroups();
	
				NotificationChannelGroup found = null;
				for (final NotificationChannelGroup group: groups) {
					if (groupId.equals(group.getId())) {
						found = group;
						break;
					}
				}
				if (found == null) {
					found = new NotificationChannelGroup(groupId,
						TextUtils.isEmpty(groupName) ? groupId : groupName);
					manager.createNotificationChannelGroup(
						setupNotificationChannelGroup(found));
				}
			}
		}
		
		/**
		 * Android 8以降用にNotificationChannelGroupを生成する処理
		 * NotificationManager#getNotificationChannelGroupsに同じグループidの
		 * ものが存在しない時のみ新規に作成する
		 * NotificationManager#createNotificationChannelGroupが呼ばれる直前に
		 * #createNotificationChannelGroupから呼ばれる
		 * @param group
		 * @return
		 */
		@NonNull
		protected NotificationChannelGroup setupNotificationChannelGroup(
			@NonNull final NotificationChannelGroup group) {
			
			return group;
		}

		@SuppressLint("InlinedApi")
		protected NotificationCompat.Builder createNotificationBuilder(
			@NonNull final Context context,
			@NonNull final CharSequence title, @NonNull final CharSequence content) {
	
			final NotificationCompat.Builder builder
				= new NotificationCompat.Builder(context, channelId)
				.setContentTitle(title)
				.setContentText(content)
				.setSmallIcon(smallIconId)  // the status icon
				.setStyle(new NotificationCompat.BigTextStyle()
					.setBigContentTitle(title)
					.bigText(content)
					.setSummaryText(content));
			final PendingIntent contentIntent = createContentIntent();
			if (contentIntent != null) {
				builder.setContentIntent(contentIntent);
			}
			final PendingIntent deleteIntent = createDeleteIntent();
			if (deleteIntent != null) {
				builder.setDeleteIntent(deleteIntent);
			}
			if (!TextUtils.isEmpty(groupId)) {
				builder.setGroup(groupId);
				// XXX 最初だけbuilder.setGroupSummaryが必要かも?
			}
			if (largeIconId != 0) {
				builder.setLargeIcon(
					BitmapFactory.decodeResource(context.getResources(), largeIconId));
			}
			return builder;
		}
		
		protected boolean isForegroundService() {
			return true;
		}
		
		protected abstract PendingIntent createContentIntent();
		protected PendingIntent createDeleteIntent() {
			return null;
		}
	}

	/**
	 * 通知領域に指定したメッセージを表示する。フォアグラウンドサービスとして動作させる。
	 * @param smallIconId
	 * @param title
	 * @param content
	 * @param intent
	 */
	protected void showNotification(@DrawableRes final int smallIconId,
		@NonNull final CharSequence title, @NonNull final CharSequence content,
		final PendingIntent intent) {

		showNotification(NOTIFICATION_ID,
			getString(R.string.service_name),
			null, null,
			smallIconId, R.drawable.ic_notification,
			title, content,
			true, intent);
	}

	/**
	 * 通知領域に指定したメッセージを表示する。
	 * @param smallIconId
	 * @param title
	 * @param content
	 * @param isForegroundService フォアグラウンドサービスとして動作させるかどうか
	 * @param intent
	 */
	protected void showNotification(@DrawableRes final int smallIconId,
		@NonNull final CharSequence title, @NonNull final CharSequence content,
		final boolean isForegroundService,
		final PendingIntent intent) {

		showNotification(NOTIFICATION_ID,
			getString(R.string.service_name),
			null, null,
			smallIconId, R.drawable.ic_notification,
			title, content,
			isForegroundService, intent);
	}

	/**
	 * 通知領域に指定したメッセージを表示する。フォアグラウンドサービスとして動作させる。
	 * @param notificationId
	 * @param smallIconId
	 * @param title
	 * @param content
	 * @param intent
	 */
	protected void showNotification(final int notificationId,
		@NonNull final String channelId,
		@DrawableRes final int smallIconId,
		@DrawableRes final int largeIconId,
		@NonNull final CharSequence title, @NonNull final CharSequence content,
		final PendingIntent intent) {
		
		showNotification(notificationId, channelId, null, null,
			smallIconId, largeIconId,
			title, content,
			true, intent);
	}
	
	/**
	 * 通知領域に指定したメッセージを表示する。フォアグラウンドサービスとして動作させる。
	 * こっちはAndroid 8以降でのグループid/グループ名の指定可能
	 * @param notificationId
	 * @param groupId
	 * @param groupName
	 * @param smallIconId
	 * @param title
	 * @param content
	 * @param intent
	 */
	protected void showNotification(final int notificationId,
		@NonNull final String channelId,
		@Nullable final String groupId, @Nullable final String groupName,
		@DrawableRes final int smallIconId,
		@DrawableRes final int largeIconId,
		@NonNull final CharSequence title, @NonNull final CharSequence content,
		final PendingIntent intent) {
		
		showNotification(notificationId, channelId,
			groupId, groupName,
			smallIconId, largeIconId,
			title, content, true, intent);
	}

	/**
	 * 通知領域に指定したメッセージを表示する。
	 * こっちはAndroid 8以降でのグループid/グループ名の指定可能
	 * @param notificationId
	 * @param channelId
	 * @param groupId
	 * @param groupName
	 * @param smallIconId
	 * @param largeIconId
	 * @param title
	 * @param content
	 * @param isForegroundService フォアグラウンドサービスとして動作させるかどうか
	 * @param intent
	 */
	protected void showNotification(final int notificationId,
		@NonNull final String channelId,
		@Nullable final String groupId, @Nullable final String groupName,
		@DrawableRes final int smallIconId,
		@DrawableRes final int largeIconId,
		@NonNull final CharSequence title, @NonNull final CharSequence content,
		final boolean isForegroundService,
		final PendingIntent intent) {

		showNotification(notificationId, title, content,
			new NotificationFactory(channelId, channelId, 0,
				groupId, groupName, smallIconId, largeIconId) {

				@Override
					protected boolean isForegroundService() {
						return isForegroundService;
					}
					
					@Override
					protected PendingIntent createContentIntent() {
						return intent;
					}
			}
		);
	}
	
	/**
	 * 通知領域に指定したメッセージを表示する。
	 * @param notificationId
	 * @param title
	 * @param content
	 * @param factory
	 */
	protected void showNotification(final int notificationId,
		@NonNull final CharSequence title, @NonNull final CharSequence content,
		@NonNull final NotificationFactory factory) {
	
		final NotificationManager manager
			= (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		final Notification notification = factory.createNotification(this, content, title);
		if (factory.isForegroundService()) {
			startForeground(notificationId, notification);
		}
		manager.notify(notificationId, notification);
	}

	/**
	 * 通知領域を開放する。フォアグラウンドサービスとしての動作を終了する
	 */
	protected void releaseNotification() {
		releaseNotification(NOTIFICATION_ID,
			getString(R.string.service_name),
			R.drawable.ic_notification, R.drawable.ic_notification,
			getString(R.string.service_name), getString(R.string.service_stop));
	}
	
	/**
	 * 通知領域を開放する。フォアグラウンドサービスとしての動作を終了する
	 */
	@SuppressLint("NewApi")
	protected void releaseNotification(final int notificationId,
		@NonNull final String channelId,
		@DrawableRes final int smallIconId,
		@DrawableRes final int largeIconId,
		@NonNull final CharSequence title, @NonNull final CharSequence content) {

		showNotification(notificationId, channelId, smallIconId, largeIconId, title, content, null);
		releaseNotification(notificationId, channelId);
	}
	
	/**
	 * 通知領域を開放する。フォアグラウンドサービスとしての動作を終了する
	 */
	@SuppressLint("NewApi")
	protected void releaseNotification(final int notificationId,
		@NonNull final String channelId) {

		stopForeground(true);
		cancelNotification(notificationId, channelId);
	}

	/**
	 * 通知領域を開放する。フォアグラウンドサービスの状態は変化しない
	 * @param notificationId
	 * @param channelId Android 8以降でnull以外なら対応するNotificationChannelを削除する.
	 * 			nullまたはAndroid 8未満の場合は何もしない
	 */
	@SuppressLint("NewApi")
	protected void cancelNotification(final int notificationId,
		@Nullable final String channelId) {

		final NotificationManager manager
			= (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		manager.cancel(notificationId);
		releaseNotificationChannel(channelId);
	}

	/**
	 * 通知領域を開放する。
	 * フォアグラウンドサービスの状態は変化しない。
	 * Android 8以降のNotificationChannelを削除しない
	 * @param notificationId
	 */
	@SuppressLint("NewApi")
	protected void cancelNotification(final int notificationId) {

		cancelNotification(notificationId, null);
	}
	
	/**
	 * 指定したNotificationChannelを破棄する
	 * @param channelId
	 */
	@SuppressLint("NewApi")
	protected void releaseNotificationChannel(@Nullable final String channelId) {
		if (!TextUtils.isEmpty(channelId) && BuildCheck.isOreo()) {
			final NotificationManager manager
				= (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
			try {
				manager.deleteNotificationChannel(channelId);
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}
	}
	
	/**
	 * 指定したNotificationGroupを削除する
	 * @param groupId
	 */
	@SuppressLint("NewApi")
	protected void releaseNotificationGroup(@NonNull final String groupId) {

		if (!TextUtils.isEmpty(groupId) && BuildCheck.isOreo()) {
			final NotificationManager manager
				= (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
			try {
				manager.deleteNotificationChannelGroup(groupId);
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}
	}

	/**
	 * 通知領域からアクティビティを起動するためのインテントを生成する
	 * @return
	 */
	protected abstract PendingIntent contextIntent();

//================================================================================
	/**
	 * メインスレッド/UIスレッド上で処理を実行する
	 * @param task
	 * @throws IllegalStateException
	 */
	protected void runOnUiThread(final Runnable task) throws IllegalStateException {
		if (task == null) return;
		if (mDestroyed) throw new IllegalStateException("already destroyed");
		mUIHandler.removeCallbacks(task);
		mUIHandler.post(task);
	}

	/**
	 * メインスレッド/UIスレッド上で処理を実行する
	 * @param task
	 * @param delay
	 * @throws IllegalStateException
	 */
	protected void runOnUiThread(@Nullable final Runnable task, final long delay)
		throws IllegalStateException {

		if (task == null) return;
		if (mDestroyed) throw new IllegalStateException("already destroyed");
		mUIHandler.removeCallbacks(task);
		if (delay > 0) {
			mUIHandler.postDelayed(task, delay);
		} else {
			mUIHandler.post(task);
		}
	}

	/**
	 * メインスレッド/UIスレッドの実行予定の処理をキャンセルする
	 * @param task
	 */
	protected void removeFromUiThread(@Nullable final Runnable task) {
		mUIHandler.removeCallbacks(task);
	}
	
	/**
	 * メインスレッド/UIスレッドの未実行処理をキャンセルする
	 * @param token
	 */
	protected void removeFromUiThreadAll(@Nullable final Object token) {
		mUIHandler.removeCallbacksAndMessages(token);
	}

	/**
	 * ワーカースレッド上で処理を実行する
	 * @param task
	 * @throws IllegalStateException
	 */
	protected void queueEvent(@Nullable final Runnable task) throws IllegalStateException {
		if (task == null) return;
		if (mDestroyed) throw new IllegalStateException("already destroyed");
		queueEvent(task, 0);
	}

	/**
	 * ワーカースレッド上で処理を実行する
	 * @param task
	 * @param delay
	 * @throws IllegalStateException
	 */
	protected void queueEvent(@Nullable final Runnable task, final long delay) throws IllegalStateException {
		if (task == null) return;
		if (mDestroyed) throw new IllegalStateException("already destroyed");
		synchronized (mSync) {
			if (mAsyncHandler != null) {
				mAsyncHandler.removeCallbacks(task);
				if (delay > 0) {
					mAsyncHandler.postDelayed(task, delay);
				} else {
					mAsyncHandler.post(task);
				}
			} else {
				throw new IllegalStateException("worker thread is not ready");
			}
		}
	}

	/**
	 * ワーカースレッドの上の処理をキャンセルする
	 * @param task
	 */
	protected void removeEvent(@Nullable final Runnable task) {
		synchronized (mSync) {
			if (mAsyncHandler != null) {
				mAsyncHandler.removeCallbacks(task);
			}
		}
	}

	/**
	 * ワーカースレッド上の待機中の処理をキャンセルする
	 * @param token
	 */
	protected void removeEventAll(@Nullable final Object token) {
		synchronized (mSync) {
			if (mAsyncHandler != null) {
				mAsyncHandler.removeCallbacksAndMessages(token);
			}
		}
	}
	
	protected Handler getAsyncHandler() throws IllegalStateException {
		if (mDestroyed) throw new IllegalStateException("already destroyed");
		synchronized (mSync) {
			return mAsyncHandler;
		}
	}
}
