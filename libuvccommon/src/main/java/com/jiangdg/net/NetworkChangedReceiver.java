package com.jiangdg.net;
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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

import com.jiangdg.utils.BuildCheck;
import com.jiangdg.utils.ComponentUtils;

/**
 * ネットワークの接続状態が変更された時のブロードキャストを処理するためのBroadcastReceiver
 * システムグローバルなブロードキャストを受信するためのBroadcastReceiverはアプリ内で1つだけ登録して
 * それ以外は(グローバルBroadcastReceiverがブロードキャストする)ローカルブロードキャストを受信すること
 * これのためだけにsupport library v4が要るにゃぁ
 */
@Deprecated
@SuppressLint("MissingPermission")
public class NetworkChangedReceiver extends BroadcastReceiver {
	private static final boolean DEBUG = false; // FIXME 実働時はfalseにすること
	private static final String TAG = NetworkChangedReceiver.class.getSimpleName();

	public static final String KEY_NETWORK_CHANGED_IS_CONNECTED_OR_CONNECTING = "KEY_NETWORK_CHANGED_IS_CONNECTED_OR_CONNECTING";
	public static final String KEY_NETWORK_CHANGED_IS_CONNECTED = "KEY_NETWORK_CHANGED_IS_CONNECTED";
	public static final String KEY_NETWORK_CHANGED_ACTIVE_NETWORK_MASK = "KEY_NETWORK_CHANGED_ACTIVE_NETWORK_MASK";

	/**
	 * The Mobile data connection.  When active, all data traffic
	 * will use this network type's interface by default
	 * (it has a default route)
	 */
	public static final int NETWORK_TYPE_MOBILE = 1 << ConnectivityManager.TYPE_MOBILE;	// 1 << 0
	/**
	 * The WIFI data connection.  When active, all data traffic
	 * will use this network type's interface by default
	 * (it has a default route).
	 */
	public static final int NETWORK_TYPE_WIFI = 1 << ConnectivityManager.TYPE_WIFI;	// 1 << 1

	/**
	 * An MMS-specific Mobile data connection.  This network type may use the
	 * same network interface as TYPE_MOBILE or it may use a different
	 * one.  This is used by applications needing to talk to the carrier's
	 * Multimedia Messaging Service servers.
	 */
	public static final int NETWORK_TYPE_MOBILE_MMS = 1 << ConnectivityManager.TYPE_MOBILE_MMS;	// 1 << 2

	/**
	 * A SUPL-specific Mobile data connection.  This network type may use the
	 * same network interface as TYPE_MOBILE or it may use a different
	 * one.  This is used by applications needing to talk to the carrier's
	 * Secure User Plane Location servers for help locating the device.
	 */
	public static final int NETWORK_TYPE_MOBILE_SUPL = 1 << ConnectivityManager.TYPE_MOBILE_SUPL;	// 1 << 3

	/**
	 * A DUN-specific Mobile data connection.  This network type may use the
	 * same network interface as TYPE_MOBILE or it may use a different
	 * one.  This is sometimes by the system when setting up an upstream connection
	 * for tethering so that the carrier is aware of DUN traffic.
	 */
	public static final int NETWORK_TYPE_MOBILE_DUN = 1 << ConnectivityManager.TYPE_MOBILE_DUN;	// 1 << 4

	/**
	 * A High Priority Mobile data connection.  This network type uses the
	 * same network interface as TYPE_MOBILE but the routing setup
	 * is different.  Only requesting processes will have access to the
	 * Mobile DNS servers and only IP's explicitly requested via requestRouteToHost
	 * will route over this interface if no default route exists.
	 */
	public static final int NETWORK_TYPE_MOBILE_HIPRI = 1 << ConnectivityManager.TYPE_MOBILE_HIPRI;	// 1 << 5

	/**
	 * The WiMAX data connection.  When active, all data traffic
	 * will use this network type's interface by default
	 * (it has a default route).
	 */
	public static final int NETWORK_TYPE_WIMAX = 1 << ConnectivityManager.TYPE_WIMAX;	// 1 << 6

	/**
	 * The Bluetooth data connection.  When active, all data traffic
	 * will use this network type's interface by default
	 * (it has a default route).
	 * XXX 単にBluetooth機器を検出しただけじゃこの値は来ない, Bluetooth経由のネットワークに接続しないとダメみたい
	 */
	public static final int NETWORK_TYPE_BLUETOOTH = 1 << ConnectivityManager.TYPE_BLUETOOTH;	// 1 << 7

	/**
	 * The Ethernet data connection.  When active, all data traffic
	 * will use this network type's interface by default
	 * (it has a default route).
	 */
	public static final int NETWORK_TYPE_ETHERNET = 1 << ConnectivityManager.TYPE_ETHERNET;	// 1 << 9

	/**
	 * A virtual network using one or more native bearers.
	 * It may or may not be providing security services.
	 */
//	public static final int NETWORK_TYPE_VPN = 1 << ConnectivityManager.TYPE_VPN;	// 1 << 17

	private static final int NETWORK_MASK_INTERNET_WIFI = NETWORK_TYPE_WIFI | NETWORK_TYPE_WIMAX | NETWORK_TYPE_BLUETOOTH | NETWORK_TYPE_ETHERNET;

	public interface OnNetworkChangedListener {
		/**
		 * @param isConnectedOrConnecting 接続中かread/write可能
		 * @param isConnected read/write可能
		 * @param activeNetworkMask アクティブなネットワークの選択マスク 接続しているネットワークがなければ0
		 */
		public void onNetworkChanged(
			final int isConnectedOrConnecting, final int isConnected, final int activeNetworkMask);
	}

	@Deprecated
	public static void enable(final Context context) {
		ComponentUtils.enable(context, NetworkChangedReceiver.class);
	}

	@Deprecated
	public static void disable(final Context context) {
		ComponentUtils.disable(context, NetworkChangedReceiver.class);
	}

	/**
	 * システムグローバルブロードキャスト受信用のレシーバーを登録する
	 * @param context application contextを渡すこと
	 * @return
	 */
	public static NetworkChangedReceiver registerGlobal(final Context context) {
		if (DEBUG) Log.v(TAG, "registerGlobal:");
		return registerGlobal(context, null);
	}

	/**
	 * システムグローバルブロードキャスト受信用のレシーバーを登録する
	 * @param context
	 * @param listener
	 * @return
	 */
	public static NetworkChangedReceiver registerGlobal(@NonNull final Context context,
		@Nullable final OnNetworkChangedListener listener) {

		if (DEBUG) Log.v(TAG, "registerGlobal:");
		final NetworkChangedReceiver receiver = new NetworkChangedReceiver(listener);
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ACTION_GLOBAL_CONNECTIVITY_CHANGE);
		synchronized (sSync) {
			context.registerReceiver(receiver, intentFilter);
			sGlobalReceiverNum++;
		}
		return receiver;
	}

	public static boolean isGlobalRegistered() {
		synchronized (sSync) {
			return sGlobalReceiverNum > 0;
		}
	}

	/**
	 * システムグローバルブロードキャスト受信用のレシーバーを登録解除するヘルパーメソッド
	 * Context#unregisterReceiverを自前で呼び出すかNetworkChangedReceiver#unregisterを呼び出しても良い
	 * @param context
	 * @param receiver　#registerGlobalが返したNetworkChangedReceiver
	 */
	public static void unregisterGlobal(@NonNull final Context context,
		@NonNull final NetworkChangedReceiver receiver) {

		if (DEBUG) Log.v(TAG, "unregisterGlobal:");
		synchronized (sSync) {
			if (receiver != null) {
				sGlobalReceiverNum--;
				try {
					context.unregisterReceiver(receiver);
				} catch (final Exception e) {
					// ignore
				}
			}
		}
	}

	/**
	 * LocalBroadcastManagerにローカルブロードキャスト受信用のレシーバーを登録する
	 * @param context
	 * @param listener
	 * @return
	 */
	public static NetworkChangedReceiver registerLocal(@NonNull final Context context,
		@NonNull final OnNetworkChangedListener listener) {

		if (DEBUG) Log.v(TAG, "registerLocal:");
		final NetworkChangedReceiver receiver = new NetworkChangedReceiver(listener);
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ACTION_LOCAL_CONNECTIVITY_CHANGE);
		final LocalBroadcastManager broadcastManager
			= LocalBroadcastManager.getInstance(context.getApplicationContext());
		broadcastManager.registerReceiver(receiver, intentFilter);
		Handler handler = null;
		try {
			handler = new Handler(Looper.getMainLooper());
		} catch (final Exception e) {
			//
		}
		final int isConnectedOrConnecting;
		final int isConnected;
		final int activeNetworkMask;
		synchronized (sSync) {
			isConnectedOrConnecting = sIsConnectedOrConnecting;
			isConnected = sIsConnected;
			activeNetworkMask = sActiveNetworkMask;
		}
		if (handler != null) {
			handler.post(new Runnable() {
				@Override
				public void run() {
					receiver.callOnNetworkChanged(isConnectedOrConnecting, isConnected, activeNetworkMask);
				}
			});
		} else {
			receiver.callOnNetworkChanged(isConnectedOrConnecting, isConnected, activeNetworkMask);
		}
		return receiver;
	}

	/**
	 * LocalBroadcastManagerからローカルブロードキャスト受信用のレシーバーを登録解除するためのヘルパーメソッド
	 * LocalBroadcastManager#unregisterReceiverを自前で呼び出すかNetworkChangedReceiver#unregisterを呼び出しても良い
	 * @param context
	 * @param receiver
	 */
	public static void unregisterLocal(@NonNull final Context context,
		@NonNull final NetworkChangedReceiver receiver) {

		if (DEBUG) Log.v(TAG, "unregisterLocal:");
		final LocalBroadcastManager broadcastManager
			= LocalBroadcastManager.getInstance(context.getApplicationContext());
		try {
			broadcastManager.unregisterReceiver(receiver);
		} catch (final Exception e) {
			// ignore
		}
	}

//================================================================================
	private static final Object sSync = new Object();
	private static int sGlobalReceiverNum;
	/** 接続中または接続準備中のネットワークのフラグ */
	private static int sIsConnectedOrConnecting = 0;
	/** 接続中のネットワークのフラグ */
	private static int sIsConnected = 0;
	/** アクティブなネットワークのフラグ */
	private static int sActiveNetworkMask = 0;

	/** システムグローバルブロードキャスト用のインテントフィルター文字列 */
	public static final String ACTION_GLOBAL_CONNECTIVITY_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE";
	/** アプリケーションローカルブロードキャスト用のインテントフィルター文字列 */
	public static final String ACTION_LOCAL_CONNECTIVITY_CHANGE = "com.serenegiant.net.CONNECTIVITY_CHANGE";

	/** コールバックリスナー */
	@Nullable
	private OnNetworkChangedListener mListener;

	/**
	 * デフォルトコンストラクタ
	 * AndroidManifest.xmlで登録する場合
	 */
	@Deprecated
	public NetworkChangedReceiver() {
		mListener = null;
	}

	/** コンストラクタ */
	private NetworkChangedReceiver(@Nullable final OnNetworkChangedListener listener) {
		mListener = listener;
	}

	/** ローカル/グローバルブロードキャストの登録を解除 */
	public void unregister(final Context context) {
		try {
			unregisterGlobal(context, this);
		} catch (final Exception e) {
			// ignore
		}
		try {
			unregisterLocal(context, this);
		} catch (final Exception e) {
			// ignore
		}
	}

	/** ネットワーク種とそのビットマスク対の配列 */
	private static final int[] NETWORKS;
	static {
		NETWORKS = new int[] {
			ConnectivityManager.TYPE_MOBILE, NETWORK_TYPE_MOBILE,
			ConnectivityManager.TYPE_WIFI, NETWORK_TYPE_WIFI,
			ConnectivityManager.TYPE_MOBILE_MMS, NETWORK_TYPE_MOBILE_MMS,
			ConnectivityManager.TYPE_MOBILE_SUPL, NETWORK_TYPE_MOBILE_SUPL,
			ConnectivityManager.TYPE_MOBILE_DUN, NETWORK_TYPE_MOBILE_DUN,
			ConnectivityManager.TYPE_MOBILE_HIPRI, NETWORK_TYPE_MOBILE_HIPRI,
			ConnectivityManager.TYPE_WIMAX, NETWORK_TYPE_WIMAX,
			ConnectivityManager.TYPE_BLUETOOTH, NETWORK_TYPE_BLUETOOTH,
			ConnectivityManager.TYPE_ETHERNET, NETWORK_TYPE_ETHERNET,
//			ConnectivityManager.TYPE_VPN, NETWORK_TYPE_VPN,
		};
	}

	@Override
	public void onReceive(final Context context, final Intent intent) {
		final String action = intent != null ? intent.getAction() : null;
		if (ACTION_GLOBAL_CONNECTIVITY_CHANGE.equals(action)) {
			onReceiveGlobal(context, intent);
		} else if (ACTION_LOCAL_CONNECTIVITY_CHANGE.equals(action)) {
			onReceiveLocal(context, intent);
		}
	}

	/**
	 * システムグローバルブロードキャスト受信時の処理
	 * @param context
	 * @param intent
	 */
	@SuppressLint("NewApi")
	private void onReceiveGlobal(final Context context, final Intent intent) {
		final ConnectivityManager connMgr
			= (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		final LocalBroadcastManager broadcastManager
			= LocalBroadcastManager.getInstance(context.getApplicationContext());

		int isConnectedOrConnecting = 0;
		int isConnected = 0;

		if (BuildCheck.isAndroid5()) {	// API>=21
			final Network[] networks = connMgr.getAllNetworks();
			if (networks != null) {
				for (final Network network: networks) {
					final NetworkInfo info = connMgr.getNetworkInfo(network);
					if (info != null) {
						isConnectedOrConnecting |= info.isConnectedOrConnecting() ? (1 << info.getType()) : 0;
						isConnected |= info.isConnected() ? (1 << info.getType()) : 0;
					}
				}
			}
		} else {
			final int n = NETWORKS.length;
			for (int i = 0; i < n; i += 2) {
				final NetworkInfo info = connMgr.getNetworkInfo(NETWORKS[i]);
				if (info != null) {
					isConnectedOrConnecting |= info.isConnectedOrConnecting() ? NETWORKS[i + 1] : 0;
					isConnected |= info.isConnected() ? NETWORKS[i + 1] : 0;
				}
			}
		}
		final NetworkInfo activeNetworkInfo = connMgr.getActiveNetworkInfo();
		final int activeNetworkMask = (activeNetworkInfo != null ? 1 << activeNetworkInfo.getType() : 0);
		synchronized (sSync) {
			sIsConnectedOrConnecting = isConnectedOrConnecting;
			sIsConnected = isConnected;
			sActiveNetworkMask = activeNetworkMask;
			sSync.notifyAll();
		}
		if (DEBUG) Log.v(TAG, String.format("onNetworkChanged:isConnectedOrConnecting=%08x,isConnected=%08x,activeNetworkMask=%08x",
			isConnectedOrConnecting, isConnected, activeNetworkMask));
		// コールバックリスナーを呼び出す
		callOnNetworkChanged(isConnectedOrConnecting, isConnected, activeNetworkMask);
		// ローカルブロードキャスト
		final Intent networkChangedIntent = new Intent(ACTION_LOCAL_CONNECTIVITY_CHANGE);
		networkChangedIntent.putExtra(KEY_NETWORK_CHANGED_IS_CONNECTED_OR_CONNECTING, isConnectedOrConnecting);
		networkChangedIntent.putExtra(KEY_NETWORK_CHANGED_IS_CONNECTED, isConnected);
		networkChangedIntent.putExtra(KEY_NETWORK_CHANGED_ACTIVE_NETWORK_MASK, activeNetworkMask);
		broadcastManager.sendBroadcast(networkChangedIntent);
	}

	/**
	 * ローカルブロードキャスト受信時の処理.
	 * コールバックリスナーを呼び出す
	 */
	private void onReceiveLocal(final Context context, final Intent intent) {
		final int isConnectedOrConnecting = intent.getIntExtra(KEY_NETWORK_CHANGED_IS_CONNECTED_OR_CONNECTING, 0);
		final int isConnected = intent.getIntExtra(KEY_NETWORK_CHANGED_IS_CONNECTED, 0);
		final int activeNetworkInfo = intent.getIntExtra(KEY_NETWORK_CHANGED_ACTIVE_NETWORK_MASK, 0);
		callOnNetworkChanged(isConnectedOrConnecting, isConnected, activeNetworkInfo);
	}

	private void callOnNetworkChanged(final int isConnectedOrConnecting, final int isConnected, final int activeNetworkInfo) {
		if (mListener != null) {
			try {
				mListener.onNetworkChanged(isConnectedOrConnecting, isConnected, activeNetworkInfo);
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}
	}

//================================================================================
// ここ以下はローカルブロードキャストを使わずにポーリングでネットワーク状態をチェックするためのスタティックメソッド
// ローカルブロードキャストと併用可
//================================================================================
	/**
	 * WiFiネットワークが使用可能かどうかを返す
	 * システムグローバルブロードキャストレシーバーを登録している時のみ有効な値を返す
	 * @return
	 */
	public static boolean isWifiNetworkReachable() {
		final int isConnectedOrConnecting;
		synchronized (sSync) {
			isConnectedOrConnecting = sIsConnectedOrConnecting & sActiveNetworkMask;
		}
		return (isConnectedOrConnecting & NETWORK_MASK_INTERNET_WIFI) != 0;
	}

	/**
	 * WiFiネットワークが使用可能かどうかを返す
	 * このメソッドはブロードキャストレシーバーの登録の有無と関係なく使用可
	 * @param context
	 * @return
	 */
	public static boolean isWifiNetworkReachable(final Context context) {
		final ConnectivityManager connMgr
			= (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		final NetworkInfo activeNetworkInfo = connMgr.getActiveNetworkInfo();
		if ((activeNetworkInfo != null) && (activeNetworkInfo.isConnectedOrConnecting())) {
			final int type = activeNetworkInfo.getType();
			return (type == ConnectivityManager.TYPE_WIFI)
				|| (type == ConnectivityManager.TYPE_WIMAX)
				|| (type == ConnectivityManager.TYPE_BLUETOOTH)
				|| (type == ConnectivityManager.TYPE_ETHERNET);
		}
		return false;
	}

	/**
	 * モバイルネットワークが使用可能かどうかを返す
	 * システムグローバルブロードキャストレシーバーを登録している時のみ有効な値を返す
	 * @return
	 */
	public static boolean isMobileNetworkReachable() {
		final int isConnectedOrConnecting;
		synchronized (sSync) {
			isConnectedOrConnecting = sIsConnectedOrConnecting & sActiveNetworkMask;
		}
		return (isConnectedOrConnecting & NETWORK_TYPE_MOBILE) != 0;
	}

	/**
	 * モバイルネットワークが使用可能かどうかを返す
	 * このメソッドはブロードキャストレシーバーの登録の有無と関係なく使用可
	 * @param context
	 * @return
	 */
	public static boolean isMobileNetworkReachable(final Context context) {
		final ConnectivityManager connMgr
			= (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		final NetworkInfo activeNetworkInfo = connMgr.getActiveNetworkInfo();
		if ((activeNetworkInfo != null) && (activeNetworkInfo.isConnectedOrConnecting())) {
			final int type = activeNetworkInfo.getType();
			return (type == ConnectivityManager.TYPE_MOBILE);
		}
		return false;
	}

	/**
	 * ネットワークが使用可能かどうかをチェック
	 * システムグローバルブロードキャストレシーバーを登録している時のみ有効な値を返す
	 * @return
	 */
	public static boolean isNetworkReachable() {
		final int isConnectedOrConnecting;
		synchronized (sSync) {
			isConnectedOrConnecting = sIsConnectedOrConnecting & sActiveNetworkMask;
		}
		return isConnectedOrConnecting != 0;
	}

	/**
	 * ネットワークが使用可能かどうかをチェック
	 * このメソッドはブロードキャストレシーバーの登録の有無と関係なく使用可
	 */
	public static boolean isNetworkReachable(final Context context) {
		final ConnectivityManager connMgr
			= (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		final NetworkInfo activeNetworkInfo = connMgr.getActiveNetworkInfo();
		return (activeNetworkInfo != null) && (activeNetworkInfo.isConnectedOrConnecting());
	}
}
