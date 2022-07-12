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
package com.serenegiant.notification;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.widget.RemoteViews;

import com.serenegiant.utils.BuildCheck;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * Notification生成/表示用のヘルパークラス
 * NotificationCompat#Builderを継承してNotificationChannel関係の設定メソッドと
 * 通知発行用のヘルパークラスを追加
 */
public abstract class NotificationBuilder extends NotificationCompat.Builder {
	private static final boolean DEBUG = false; // set false on production
	private static final String TAG = NotificationBuilder.class.getSimpleName();

	public interface IntentFactory {
		@Nullable
		public PendingIntent createContentIntent(
			@Nullable final PendingIntent intent);

		@Nullable
		public PendingIntent createDeleteIntent(@Nullable final PendingIntent intent);

		@Nullable
		public PendingIntent createFullScreenIntent(@Nullable final PendingIntent intent);
		
		public boolean isHighPriorityFullScreenIntent(final boolean highPriority);
		
	}

	public interface NotificationBuilderFactory extends IntentFactory {
		public void setupBuilder(
			@NonNull final NotificationBuilder builder);
	}

	public static class DefaultIntentFactory
		implements NotificationBuilderFactory {

		@Nullable
		public PendingIntent createContentIntent(
			@Nullable final PendingIntent intent) {
			
			return intent;
		}

		@Nullable
		@Override
		public PendingIntent createDeleteIntent(
			@Nullable final PendingIntent intent) {

			return intent;
		}

		@Nullable
		@Override
		public PendingIntent createFullScreenIntent(
			@Nullable final PendingIntent intent) {
		
			return intent;
		}

		@Override
		public boolean isHighPriorityFullScreenIntent(
			final boolean highPriority) {
		
			return highPriority;
		}
		
		public void setupBuilder(
			@NonNull final NotificationBuilder builder) {
		}
	}

	/**
	 * Notification発行用のヘルパーメソッド
	 * @param context
	 * @param notificationId
	 * @param channelId
	 * @param title
	 * @param content
	 * @param factory
	 */
	public static void showNotification(@NonNull final Context context,
		final int notificationId, @NonNull final String channelId,
		final String title, final String content,
		@DrawableRes final int smallIconId,
		@Nullable final IntentFactory factory) {
		
		showNotification(context, null, notificationId,
			channelId, title, content, smallIconId,
			factory);
	}
	
	/**
	 * Notification発行用のヘルパーメソッド
	 * @param context
	 * @param tag
	 * @param notificationId
	 * @param channelId
	 * @param title
	 * @param content
	 * @param smallIconId
	 * @param factory
	 */
	public static void showNotification(@NonNull final Context context,
		@Nullable final String tag,
		final int notificationId, @NonNull final String channelId,
		final String title, final String content,
		@DrawableRes final int smallIconId,
		@Nullable final IntentFactory factory) {
		
		final NotificationBuilder builder = new NotificationBuilder(context, channelId, smallIconId) {
			@Override
			protected PendingIntent createContentIntent() {
				return (factory != null)
					? factory.createContentIntent(getContentIntent()) : null;
			}
			
			@Override
			protected PendingIntent createDeleteIntent() {
				return (factory != null)
					? factory.createDeleteIntent(getDeleteIntent())
					: super.createDeleteIntent();
			}
			
			@Nullable
			@Override
			protected PendingIntent createFullScreenIntent() {
				return (factory != null)
					? factory.createFullScreenIntent(getFullScreenIntent())
					: super.createFullScreenIntent();
			}
			
			@Override
			public boolean isHighPriorityFullScreenIntent() {
				final boolean highPriority = super.isHighPriorityFullScreenIntent();
				return (factory != null)
					? factory.isHighPriorityFullScreenIntent(highPriority)
					: highPriority;
			}
		};
		builder.setContentTitle(title)
			.setContentText(content);
		if (factory instanceof NotificationBuilderFactory) {
			((NotificationBuilderFactory) factory).setupBuilder(builder);
		}
		builder.notify(notificationId);
	}

//================================================================================
	@IntDef({
		NotificationCompat.BADGE_ICON_NONE,
		NotificationCompat.BADGE_ICON_SMALL,
		NotificationCompat.BADGE_ICON_LARGE})
	@Retention(RetentionPolicy.SOURCE)
	public @interface BadgeIconType {}

	private static final int PRIORITY_UNSPECIFIED = NotificationCompat.PRIORITY_MIN - 1;

//================================================================================
	@NonNull
	private final Context mContext;
	@NonNull
	private final ChannelBuilder mChannelBuilder;
	@Nullable
	private PendingIntent mContentIntent;
	@Nullable
	private PendingIntent mDeleteIntent;
	@Nullable
	private PendingIntent mFullScreenIntent;
	private boolean mHighPriorityFullScreenIntent;
	private int mPriority = PRIORITY_UNSPECIFIED;
	
	/**
	 * コンストラクタ
	 * @param context
	 * @param channelId
	 */
	public NotificationBuilder(@NonNull Context context, @NonNull String channelId,
		@DrawableRes final int smallIconId) {

		super(context, channelId);
		mContext = context;
		mChannelBuilder = ChannelBuilder.getBuilder(context, channelId);
		setSmallIcon(smallIconId);
	}
	
	/**
	 * Notification生成用のファクトリーメソッド
	 * @return
	 */
	@SuppressLint("InlinedApi")
	public Notification build() {
		if (mChannelBuilder.getImportance() == NotificationManager.IMPORTANCE_NONE) {
			// importanceが設定されていないときでmPriorityがセットされていればそれに従う
			switch (mPriority) {
			case NotificationCompat.PRIORITY_MIN:
				mChannelBuilder.setImportance(NotificationManager.IMPORTANCE_MIN);
				break;
			case NotificationCompat.PRIORITY_LOW:
				mChannelBuilder.setImportance(NotificationManager.IMPORTANCE_LOW);
				break;
			case NotificationCompat.PRIORITY_DEFAULT:
				mChannelBuilder.setImportance(NotificationManager.IMPORTANCE_DEFAULT);
				break;
			case NotificationCompat.PRIORITY_HIGH:
				mChannelBuilder.setImportance(NotificationManager.IMPORTANCE_HIGH);
				break;
			case NotificationCompat.PRIORITY_MAX:
				mChannelBuilder.setImportance(NotificationManager.IMPORTANCE_HIGH);
				break;
			default:
				// ChannelBuilder側の設定に従う==IMPORTANCE_DEFAULT
				break;
			}
		}
		mChannelBuilder.build();
		super.setContentIntent(createContentIntent());
		super.setDeleteIntent(createDeleteIntent());
		super.setFullScreenIntent(createFullScreenIntent(), isHighPriorityFullScreenIntent());
		return super.build();
	}
	
	/**
	 * 通知を発行する
	 * @param notificationId
	 * @return
	 */
	public NotificationBuilder notify(final int notificationId) {
		notify(mContext, null, notificationId, build());
		return this;
	}
	
	/**
	 * 通知を発行する
 	 * @param tag
	 * @param notificationId
	 * @return
	 */
	public NotificationBuilder notify(@Nullable final String tag, final int notificationId) {
		notify(mContext, tag, notificationId, build());
		return this;
	}
	
	/**
	 * フォアグラウンドサービス用に通知を発行する
 	 * @param service
	 * @param notificationId
	 * @return
	 */
	public NotificationBuilder notifyForeground(@NonNull final Service service,
		final int notificationId) {
		
		return notifyForeground(service, null, notificationId);
	}
	
	/**
	 * フォアグラウンドサービス用に通知を発行する
 	 * @param service
	 * @param tag
	 * @param notificationId
	 * @return
	 */
	public NotificationBuilder notifyForeground(@NonNull final Service service,
		@Nullable final String tag, final int notificationId) {
		final Notification notification = build();
		service.startForeground(notificationId, notification);
		notify(service, tag, notificationId, notification);
		return this;
	}

//--------------------------------------------------------------------------------
	@NonNull
	public ChannelBuilder getChannelBuilder() {
		return mChannelBuilder;
	}
	
	@Override
	public NotificationBuilder setChannelId(@NonNull final String channelId) {
		super.setChannelId(channelId);
		mChannelBuilder.setId(channelId);
		return this;
	}

	public String getChannelId() {
		return mChannelBuilder.getId();
	}

	public NotificationBuilder setChannelName(@Nullable final CharSequence channelName) {
		mChannelBuilder.setName(channelName);
		return this;
	}

	@Nullable
	public CharSequence getChannelName() {
		return mChannelBuilder.getName();
	}

	public NotificationBuilder setImportance(@ChannelBuilder.Importance final int importance) {
		mChannelBuilder.setImportance(importance);
		return this;
	}

	@ChannelBuilder.Importance
	public int getImportance() {
		return mChannelBuilder.getImportance();
	}
	
	public NotificationBuilder setLockscreenVisibility(
		@ChannelBuilder.NotificationVisibility final int lockscreenVisibility) {

		mChannelBuilder.setLockscreenVisibility(lockscreenVisibility);
		return this;
	}

	@ChannelBuilder.NotificationVisibility
	public int getLockscreenVisibility() {
		return mChannelBuilder.getLockscreenVisibility();
	}

	/**
	 * マナーモードの上書き(オーバーロード)するかどうかをセット
	 * @param bypassDnd
	 * @return
	 */
	public NotificationBuilder setBypassDnd(final boolean bypassDnd) {
		mChannelBuilder.setBypassDnd(bypassDnd);
		return this;
	}
	
	/**
	 * チャネル詳細情報をセット
	 * @param description
	 * @return
	 */
	public NotificationBuilder setDescription(final String description) {
		mChannelBuilder.setDescription(description);
		return this;
	}
	
	/**
	 * 通知でLED表示が有効な場合のLED色をセット
	 * この設定はNotificationを発行する前に変更しないと有効にならない
	 * @param argb
	 * @return
	 */
	public NotificationBuilder setLightColor(final int argb) {
		mChannelBuilder.setLightColor(argb);
		return this;
	}
	
	/**
	 * 通知でのLED表示の有効無効をセット
	 * この設定はNotificationを発行する前に変更しないと有効にならない
	 * @param lights
	 * @return
	 */
	public NotificationBuilder enableLights(final boolean lights) {
		mChannelBuilder.enableLights(lights);
		return this;
	}
	
	/**
	 * ランチャーでバッジ表示をするかどうかをセット
	 * @param showBadge
	 * @return
	 */
	public NotificationBuilder setShowBadge(final boolean showBadge) {
		mChannelBuilder.setShowBadge(showBadge);
		return this;
	}
	
	/**
	 * バイブレーションのパターンをセット
	 * この設定はNotificationを発行する前に変更しないと有効にならない
 	 * @param vibrationPattern
	 * @return
	 */
	public NotificationBuilder setVibrationPattern(final long[] vibrationPattern) {
		mChannelBuilder.setVibrationPattern(vibrationPattern);
		return this;
	}

	/**
	 * バイブレーションを有効にするかどうかをセット
	 * #setVibrationPatternでバイブレーションパターンをセットすると自動的に有効になる
	 * この設定はNotificationを発行する前に変更しないと有効にならない
	 * @param vibration
	 * @return
	 */
	public NotificationBuilder enableVibration(final boolean vibration) {
		mChannelBuilder.enableVibration(vibration);
		return this;
	}

	/**
	 * 通知音をセット
	 * この設定はNotificationを発行する前に変更しないと有効にならない
	 * @param sound
	 * @param audioAttributes
	 * @return
	 */
	public NotificationBuilder setSound(final Uri sound, final AudioAttributes audioAttributes) {
		super.setSound(sound);
		mChannelBuilder.setSound(sound, audioAttributes);
		return this;
	}
	
	/**
	 * チャネルグループを設定
	 * @param groupId
	 * @return
	 */
	public NotificationBuilder setChannelGroup(@Nullable final String groupId) {
		mChannelBuilder.setGroup(groupId, mChannelBuilder.getGroupName());
		return this;
	}
	
	/**
	 * チャネルグループ名を設定
	 * @param groupId
	 * @param groupName
	 * @return
	 */
	public NotificationBuilder setChannelGroup(@Nullable final String groupId,
		@Nullable final String groupName) {

		mChannelBuilder.setGroup(groupId, groupName);
		return this;
	}

//--------------------------------------------------------------------------------
	/**
	 * 通知をタップした時に実行されるPendingIntentを設定
	 * @param intent
	 * @return
	 */
	@Override
	public NotificationBuilder setContentIntent(final PendingIntent intent) {
		super.setContentIntent(mContentIntent);
		mContentIntent = intent;
		return this;
	}
	
	/**
	 * #setContentIntentでセットしたPendingIntentを取得
	 * @return
	 */
	public PendingIntent getContentIntent() {
		return mContentIntent;
	}
	
	/**
	 * 通知をタップした時に実行されるPendingIntent生成メソッド
	 * #setContentIntentでセットしたPendingIntentは
	 * #getContentIntentで取得可能
	 * @return
	 */
	protected abstract PendingIntent createContentIntent();
	
	/**
	 * 通知を削除(スワイプアウト)した時に実行されるPendingIntentを設定
	 * @param intent
	 * @return
	 */
	@Override
	public NotificationBuilder setDeleteIntent(final PendingIntent intent) {
		super.setDeleteIntent(intent);
		mDeleteIntent = intent;
		return this;
	}
	
	/**
	 * #setDeleteIntentでセットしたPendingIntentを取得
	 * @return
	 */
	public PendingIntent getDeleteIntent() {
		return mDeleteIntent;
	}
	
	/**
	 * 通知を削除(スワイプアウト)した時に実行されるPendingIntent生成メソッド
	 * #setDeleteIntentでセットしたPendingIntentは
	 * #getDeleteIntentで取得可能
	 * @return
	 */
	protected PendingIntent createDeleteIntent() {
		return mDeleteIntent;
	}
	
	@Override
	public NotificationBuilder setWhen(final long when) {
		super.setWhen(when);
		return this;
	}
	
	@Override
	public NotificationBuilder setShowWhen(final boolean show) {
		super.setShowWhen(show);
		return this;
	}
	
	@Override
	public NotificationBuilder setUsesChronometer(final boolean b) {
		super.setUsesChronometer(b);
		return this;
	}
	
	@Override
	public NotificationBuilder setSmallIcon(final int icon) {
		super.setSmallIcon(icon);
		return this;
	}
	
	@Override
	public NotificationBuilder setSmallIcon(final int icon, final int level) {
		super.setSmallIcon(icon, level);
		return this;
	}
	
	@Override
	public NotificationBuilder setContentTitle(final CharSequence title) {
		super.setContentTitle(title);
		return this;
	}
	
	@Override
	public NotificationBuilder setContentText(final CharSequence text) {
		super.setContentText(text);
		return this;
	}
	
	@Override
	public NotificationBuilder setSubText(final CharSequence text) {
		super.setSubText(text);
		return this;
	}
	
	@Override
	public NotificationBuilder setRemoteInputHistory(final CharSequence[] text) {
		super.setRemoteInputHistory(text);
		return this;
	}
	
	@Override
	public NotificationBuilder setNumber(final int number) {
		super.setNumber(number);
		return this;
	}
	
	@Override
	public NotificationBuilder setContentInfo(final CharSequence info) {
		super.setContentInfo(info);
		return this;
	}
	
	@Override
	public NotificationBuilder setProgress(final int max, final int progress, final boolean indeterminate) {
		super.setProgress(max, progress, indeterminate);
		return this;
	}
	
	@Override
	public NotificationBuilder setContent(final RemoteViews views) {
		super.setContent(views);
		return this;
	}
	
	@Override
	public NotificationBuilder setFullScreenIntent(final PendingIntent intent,
		final boolean highPriority) {

		super.setFullScreenIntent(intent, highPriority);
		mFullScreenIntent = intent;
		mHighPriorityFullScreenIntent = highPriority;
		return this;
	}
	
	public PendingIntent getFullScreenIntent() {
		return mFullScreenIntent;
	}

	public boolean isHighPriorityFullScreenIntent() {
		return mHighPriorityFullScreenIntent;
	}

	@Nullable
	protected PendingIntent createFullScreenIntent() {
		return mFullScreenIntent;
	}
	
	@Override
	public NotificationBuilder setTicker(final CharSequence tickerText) {
		super.setTicker(tickerText);
		return this;
	}
	
	@Override
	public NotificationBuilder setTicker(final CharSequence tickerText, final RemoteViews views) {
		super.setTicker(tickerText, views);
		return this;
	}
	
	@Override
	public NotificationBuilder setLargeIcon(final Bitmap icon) {
		super.setLargeIcon(icon);
		return this;
	}
	
	/**
	 * Set the large icon that is shown in the ticker and notification.
	 * @param largeIconId
	 */
	public NotificationBuilder setLargeIcon(@DrawableRes final int largeIconId) {
		super.setLargeIcon(
			BitmapFactory.decodeResource(mContext.getResources(), largeIconId));
		return this;
	}
	
	@Override
	public NotificationBuilder setSound(final Uri sound) {
		super.setSound(sound);
		mChannelBuilder.setSound(sound, null);
		return this;
	}
	
	@SuppressLint("NewApi")
	@Override
	public NotificationBuilder setSound(final Uri sound, final int streamType) {
		super.setSound(sound, streamType);
		final AudioAttributes attribute;
		if (BuildCheck.isLollipop()) {
			attribute = new AudioAttributes.Builder().setLegacyStreamType(streamType).build();
		} else {
			attribute = null;
		}
		mChannelBuilder.setSound(sound, attribute);
		return this;
	}
	
	@Override
	public NotificationBuilder setVibrate(final long[] pattern) {
		super.setVibrate(pattern);
		mChannelBuilder.setVibrationPattern(pattern);
		return this;
	}
	
	@Override
	public NotificationBuilder setLights(final int argb, final int onMs, final int offMs) {
		super.setLights(argb, onMs, offMs);
		mChannelBuilder.setLightColor(argb);
		return this;
	}
	
	@Override
	public NotificationBuilder setOngoing(final boolean ongoing) {
		super.setOngoing(ongoing);
		return this;
	}
	
	@Override
	public NotificationBuilder setColorized(final boolean colorize) {
		super.setColorized(colorize);
		return this;
	}
	
	@Override
	public NotificationBuilder setOnlyAlertOnce(final boolean onlyAlertOnce) {
		super.setOnlyAlertOnce(onlyAlertOnce);
		return this;
	}
	
	@Override
	public NotificationBuilder setAutoCancel(final boolean autoCancel) {
		super.setAutoCancel(autoCancel);
		return this;
	}
	
	@Override
	public NotificationBuilder setLocalOnly(final boolean b) {
		super.setLocalOnly(b);
		return this;
	}
	
	@Override
	public NotificationBuilder setCategory(final String category) {
		super.setCategory(category);
		return this;
	}
	
	@Override
	public NotificationBuilder setDefaults(final int defaults) {
		super.setDefaults(defaults);
		return this;
	}
	
	@Override
	public NotificationBuilder setPriority(final int pri) {
		super.setPriority(pri);
		mPriority = pri;
		return this;
	}
	
	@Override
	public NotificationBuilder addPerson(final String uri) {
		super.addPerson(uri);
		return this;
	}
	
	/**
	 * 通知をグループ化
	 * ここでのグループ化はNotificationChannelGroupとは関係ない
	 * @param groupKey
	 * @return
	 */
	@Override
	public NotificationBuilder setGroup(final String groupKey) {
		super.setGroup(groupKey);
		return this;
	}
	
	/**
	 * 通知をグループ化するかどうか
	 * ここでのグループ化はNotificationChannelGroupとは関係ない
	 * @param isGroupSummary
	 * @return
	 */
	@Override
	public NotificationBuilder setGroupSummary(final boolean isGroupSummary) {
		super.setGroupSummary(isGroupSummary);
		return this;
	}
	
	@Override
	public NotificationBuilder setSortKey(final String sortKey) {
		super.setSortKey(sortKey);
		return this;
	}
	
	@Override
	public NotificationBuilder addExtras(final Bundle extras) {
		super.addExtras(extras);
		return this;
	}
	
	@Override
	public NotificationBuilder setExtras(final Bundle extras) {
		super.setExtras(extras);
		return this;
	}
	
	@Override
	public NotificationBuilder addAction(final int icon, final CharSequence title, final PendingIntent intent) {
		super.addAction(icon, title, intent);
		return this;
	}
	
	@Override
	public NotificationBuilder addAction(final NotificationCompat.Action action) {
		super.addAction(action);
		return this;
	}
	
	@Override
	public NotificationBuilder setStyle(final NotificationCompat.Style style) {
		super.setStyle(style);
		return this;
	}
	
	/**
	 * これは通知の背景の色として使われるらしい,　通知LEDの色ではない
	 * @param argb
	 * @return
	 */
	@Override
	public NotificationBuilder setColor(final int argb) {
		super.setColor(argb);
		return this;
	}
	
	@Override
	public NotificationBuilder setVisibility(final int visibility) {
		super.setVisibility(visibility);
		mChannelBuilder.setLockscreenVisibility(visibility);
		return this;
	}
	
	@Override
	public NotificationBuilder setPublicVersion(final Notification n) {
		super.setPublicVersion(n);
		return this;
	}
	
	@Override
	public NotificationBuilder setCustomContentView(final RemoteViews contentView) {
		super.setCustomContentView(contentView);
		return this;
	}
	
	@Override
	public NotificationBuilder setCustomBigContentView(final RemoteViews contentView) {
		super.setCustomBigContentView(contentView);
		return this;
	}
	
	@Override
	public NotificationBuilder setCustomHeadsUpContentView(final RemoteViews contentView) {
		super.setCustomHeadsUpContentView(contentView);
		return this;
	}
	
	@Override
	public NotificationBuilder setTimeoutAfter(final long durationMs) {
		super.setTimeoutAfter(durationMs);
		return this;
	}
	
	@Override
	public NotificationBuilder setShortcutId(final String shortcutId) {
		super.setShortcutId(shortcutId);
		return this;
	}
	
	@Override
	public NotificationBuilder setBadgeIconType(@BadgeIconType  final int icon) {
		super.setBadgeIconType(icon);
		return this;
	}
	
	@Override
	public NotificationBuilder setGroupAlertBehavior(final int groupAlertBehavior) {
		super.setGroupAlertBehavior(groupAlertBehavior);
		return this;
	}
	
	@Override
	public NotificationBuilder extend(final NotificationCompat.Extender extender) {
		super.extend(extender);
		return this;
	}
	
	/**
	 * 通知発行用のヘルパークラス
	 * @param context
	 * @param tag
	 * @param notificationId
	 * @param notification
	 */
	private static void notify(@NonNull final Context context,
		@Nullable final String tag, final int notificationId,
		@NonNull final Notification notification) {
		
		NotificationManagerCompat.from(context)
			.notify(tag, notificationId, notification);
	}
	
	
}
