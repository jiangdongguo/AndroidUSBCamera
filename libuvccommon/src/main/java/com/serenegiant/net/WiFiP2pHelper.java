package com.serenegiant.net;
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import androidx.annotation.NonNull;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WiFi directでp2p接続を行うためのヘルパークラス
 * AndroidManifest.xmlに以下の記述が必要。
 *
 * <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
 * <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
 * <uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
 * <uses-permission android:name="android.permission.INTERNET" />
 * <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
 * <uses-permission android:name="android.permission.READ_PHONE_STATE" />
 * <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
 *
 * <uses-feature android:name="android.hardware.wifi.direct"/>
 */
public class WiFiP2pHelper {
	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = WiFiP2pHelper.class.getSimpleName();

	private static WiFiP2pHelper sWiFiP2PHelper;
	
	/**
	 * シングルトンアクセスする
	 * @param context
	 * @return
	 */
	public static synchronized WiFiP2pHelper getInstance(@NonNull final Context context) {
		if ((sWiFiP2PHelper == null) || (sWiFiP2PHelper.mWeakContext.get() == null)) {
			sWiFiP2PHelper = new WiFiP2pHelper(context);
		}
		return sWiFiP2PHelper;
	}
	
	/**
	 * インスタンスを解放
	 */
	public static synchronized void release() {
		sWiFiP2PHelper = null;
	}
	
	/** コールバックリスナー */
	private final Set<WiFiP2pListener> mListeners = new CopyOnWriteArraySet<WiFiP2pListener>();
	/** 周囲に存在するWiFi Direct対応機器リスト */
	private final List<WifiP2pDevice> mAvailableDevices = new ArrayList<WifiP2pDevice>();
	/** Context */
	private final WeakReference<Context> mWeakContext;
	private final WifiP2pManager mWifiP2pManager;
	private WifiP2pManager.Channel mChannel;
	private WiFiDirectBroadcastReceiver mReceiver;
	/** WiFi Directが有効かどうか */
	private boolean mIsWifiP2pEnabled;
	
	/**
	 * コンストラクタ
	 * @param context
	 */
	private WiFiP2pHelper(@NonNull final Context context) {
		mWeakContext = new WeakReference<Context>(context);
		mWifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
	}
	
	/**
	 * WiFiP2pHelperインスタンスをシステムに登録
	 */
	public synchronized void register() {
		if (DEBUG) Log.v(TAG, "register:");
		final Context context = mWeakContext.get();
		if ((context != null) & (mReceiver == null)) {
			mChannel = mWifiP2pManager.initialize(context,
				context.getMainLooper(), mChannelListener);
			mReceiver = new WiFiDirectBroadcastReceiver(mWifiP2pManager, mChannel, this);
			final IntentFilter intentFilter = new IntentFilter();
			intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
			intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
			intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
			intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
			context.registerReceiver(mReceiver, intentFilter);
		}
	}
	
	/**
	 * WiFiP2pHelperインスタンスをシステムから登録解除
	 */
	public synchronized void unregister() {
		if (DEBUG) Log.v(TAG, "unregister:");
		mIsWifiP2pEnabled = false;
		mRetryCount = 0;
		internalDisconnect(null);
		if (mReceiver != null) {
			final Context context = mWeakContext.get();
			try {
				context.unregisterReceiver(mReceiver);
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
			mReceiver = null;
		}
	}
	
	/**
	 * コールバックを登録
	 * @param listener
	 */
	public void add(@NonNull final WiFiP2pListener listener) {
		mListeners.add(listener);
	}
	
	/**
	 * コールバックの登録を解除
	 * @param listener
	 */
	public void remove(@NonNull final WiFiP2pListener listener) {
		mListeners.remove(listener);
	}
	
	/**
	 * WiFi Directに対応した機器探索を開始
	 * @throws IllegalStateException
	 */
	public synchronized void startDiscovery() throws IllegalStateException {
		if (DEBUG) Log.v(TAG, "startDiscovery:");
		if (mChannel != null) {
			mWifiP2pManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
				@Override
				public void onSuccess() {
				}
				@Override
				public void onFailure(final int reason) {
					callOnError(new RuntimeException("failed to start discovery, reason=" + reason));
				}
			});
		} else {
			throw new IllegalStateException("not registered");
		}
	}
	
	/**
	 * 指定したMACアドレスの機器へ接続を試みる
	 * @param remoteMacAddress
	 */
	public void connect(@NonNull final String remoteMacAddress) {
		if (DEBUG) Log.v(TAG, "connect:remoteMacAddress=" + remoteMacAddress);
		final WifiP2pConfig config = new WifiP2pConfig();
		config.deviceAddress = remoteMacAddress;
		config.wps.setup = WpsInfo.PBC;
		connect(config);
	}
	
	/**
	 * 指定した機器へ接続を試みる
	 * @param device
	 */
	public void connect(@NonNull final WifiP2pDevice device) {
		if (DEBUG) Log.v(TAG, "connect:device=" + device);
		final WifiP2pConfig config = new WifiP2pConfig();
		config.deviceAddress = device.deviceAddress;
		config.wps.setup = WpsInfo.PBC;
		connect(config);
	}
	
	/**
	 * 指定した機器へ接続を試みる
	 * @param config
	 * @throws IllegalStateException
	 */
	public void connect(@NonNull final WifiP2pConfig config) throws IllegalStateException {
		if (DEBUG) Log.v(TAG, "connect:config=" + config);
		if (mChannel != null) {
			mWifiP2pManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
				@Override
				public void onSuccess() {
					// WiFiDirectBroadcastReceiver will notify us. Ignore for now.
				}
				@Override
				public void onFailure(int reason) {
					callOnError(new RuntimeException("failed to connect, reason=" + reason));
				}
			});
		} else {
			throw new IllegalStateException("not registered");
		}
	}
	
	/**
	 * 切断する
	 */
	protected void internalDisconnect(final WifiP2pManager.ActionListener listener) {
		if (DEBUG) Log.v(TAG, "internalDisconnect:");
		if (mWifiP2pManager != null) {
			if ((mWifiP2pDevice == null)
				|| (mWifiP2pDevice.status == WifiP2pDevice.CONNECTED)) {
				// 接続されていないか、既に接続済みの時
				if (mChannel != null) {
					mWifiP2pManager.removeGroup(mChannel, listener);
				}
			} else if (mWifiP2pDevice.status == WifiP2pDevice.AVAILABLE
				|| mWifiP2pDevice.status == WifiP2pDevice.INVITED) {

				// ネゴシエーション中の時
				mWifiP2pManager.cancelConnect(mChannel, listener);
			}
		}
	}
	
	/**
	 * 切断する
	 */
	public synchronized void disconnect() {
		if (DEBUG) Log.v(TAG, "disconnect:");
		internalDisconnect(new WifiP2pManager.ActionListener() {
			@Override
			public void onSuccess() {
			}
			@Override
			public void onFailure(final int reason) {
				callOnError(new RuntimeException("failed to disconnect, reason=" + reason));
			}
		});
	}
	
	/**
	 * WiFi Directが有効かどうかを取得する
	 * @return
	 */
	public synchronized boolean isWiFiP2pEnabled() {
		return (mChannel != null) && mIsWifiP2pEnabled;
	}
	
	/**
	 * WiFi Directが有効かどうかをセットする
	 * @param enabled
	 */
	private synchronized void setIsWifiP2pEnabled(final boolean enabled) {
		mIsWifiP2pEnabled = enabled;
		callOnStateChanged(enabled);
	}
	
	/**
	 * 接続が解除された時の処理
	 */
	private synchronized void resetData() {
		if (DEBUG) Log.v(TAG, "resetData:");
		if (isConnectedOrConnecting()) {
			callOnDisconnect();
		}
	}
	
	/** 自分のWifiP2pDevice */
	private WifiP2pDevice mWifiP2pDevice;
	
	/**
	 * 自分のWifiP2pDeviceが更新された時の処理
	 * @param device
	 */
	private synchronized void updateDevice(final WifiP2pDevice device) {
		if (DEBUG) Log.v(TAG, "updateDevice:device=" + device);
		mWifiP2pDevice = device;
	}
	
	/**
	 * WiFi Directで接続されているかどうか
	 * @return
	 */
	public synchronized boolean isConnected() {
		return (mWifiP2pDevice != null) && (mWifiP2pDevice.status == WifiP2pDevice.CONNECTED);
	}
	
	/**
	 * WiFi Directで接続を試みているか接続しているかどうか
	 * @return
	 */
	public synchronized boolean isConnectedOrConnecting() {
		return (mWifiP2pDevice != null)
			&& ((mWifiP2pDevice.status == WifiP2pDevice.CONNECTED)
				|| (mWifiP2pDevice.status == WifiP2pDevice.INVITED));
	}
	
//================================================================================
// コールバックの呼び出し
	
	/**
	 * WiFi Directが有効・無効になった時のコールバックリスナーを呼び出す
	 * @param enabled
	 */
	protected void callOnStateChanged(final boolean enabled) {
		if (DEBUG) Log.v(TAG, "callOnStateChanged:enabled=" + enabled);
		for (final WiFiP2pListener listener: mListeners) {
			try {
				listener.onStateChanged(enabled);
			} catch (final Exception e1) {
				Log.w(TAG, e1);
				mListeners.remove(listener);
			}
		}
	}
	
	/**
	 * 周辺のWiFi Direct機器の状態が変化した時のコールバックリスナーを呼び出す
	 * @param devices
	 */
	protected void callOnUpdateDevices(@NonNull final List<WifiP2pDevice> devices) {
		if (DEBUG) Log.v(TAG, "callOnUpdateDevices:");
		for (final WiFiP2pListener listener: mListeners) {
			try {
				listener.onUpdateDevices(devices);
			} catch (final Exception e1) {
				Log.w(TAG, e1);
				mListeners.remove(listener);
			}
		}
	}
	
	/**
	 * 接続した時のコールバックリスナーを呼び出す
	 * @param info
	 */
	protected void callOnConnect(@NonNull final WifiP2pInfo info) {
		if (DEBUG) Log.v(TAG, "callOnConnect:");
		for (final WiFiP2pListener listener: mListeners) {
			try {
				listener.onConnect(info);
			} catch (final Exception e1) {
				Log.w(TAG, e1);
				mListeners.remove(listener);
			}
		}
	}
	
	/**
	 * 切断された時のコールバックリスナーを呼び出す
	 */
	protected void callOnDisconnect() {
		if (DEBUG) Log.v(TAG, "callOnDisconnect:");
		for (final WiFiP2pListener listener: mListeners) {
			try {
				listener.onDisconnect();
			} catch (final Exception e1) {
				Log.w(TAG, e1);
				mListeners.remove(listener);
			}
		}
	}
	
	/**
	 * エラー発生時のコールバックリスナーを呼び出す
	 * @param e
	 */
	protected void callOnError(final Exception e) {
		if (DEBUG) Log.w(TAG, "callOnError:", e);
		for (final WiFiP2pListener listener: mListeners) {
			try {
				listener.onError(e);
			} catch (final Exception e1) {
				Log.w(TAG, e1);
				mListeners.remove(listener);
			}
		}
	}
	
//================================================================================
	private int mRetryCount;
	/** チャネルから切断された時のコールバックリスナー */
	private final WifiP2pManager.ChannelListener mChannelListener
		= new WifiP2pManager.ChannelListener() {
		@Override
		public void onChannelDisconnected() {
			if (DEBUG) Log.v(TAG, "onChannelDisconnected:");
			setIsWifiP2pEnabled(false);
			resetData();
			synchronized (WiFiP2pHelper.this) {
				mChannel = null;
			}
			if (mRetryCount == 0) {
				// 1回だけリトライする
				mRetryCount++;
				final Context context = mWeakContext.get();
				if ((context != null) & (mReceiver == null)) {
				
					// 再初期化
					mChannel = mWifiP2pManager.initialize(context,
						context.getMainLooper(), mChannelListener);
				}
			}
		}
	};
	
	/** 周囲のWiFi Direct対応機器の状態が変化した時のコールバックリスナー */
	private final WifiP2pManager.PeerListListener mPeerListListener
		= new WifiP2pManager.PeerListListener() {
		@Override
		public void onPeersAvailable(final WifiP2pDeviceList peers) {
			if (DEBUG) Log.v(TAG, "onPeersAvailable:peers=" + peers);
			final Collection<WifiP2pDevice> devices = peers.getDeviceList();
			synchronized (mAvailableDevices) {
				mAvailableDevices.clear();
				mAvailableDevices.addAll(devices);
			}
			callOnUpdateDevices(mAvailableDevices);
		}
	};
	
	/** WiFi Direct対応機器と接続した時のコールバックリスナー */
	private final WifiP2pManager.ConnectionInfoListener
		mConnectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
		@Override
		public void onConnectionInfoAvailable(final WifiP2pInfo info) {
			if (DEBUG) Log.v(TAG, "onConnectionInfoAvailable:info=" + info);
			if (info != null) {
				callOnConnect(info);
			}
		}
	};
	
	/** WiFi direct関係のブロードキャストを受け取るためのBroadcastReceiver */
	private static class WiFiDirectBroadcastReceiver extends BroadcastReceiver {
	
	    @NonNull private WifiP2pManager mManager;
	    @NonNull private WifiP2pManager.Channel mChannel;
	    @NonNull private WiFiP2pHelper mParent;
	
	    /**
	     * @param manager WifiP2pManager system service
	     * @param channel Wifi p2p channel
	     * @param parent associated with the receiver
	     */
	    public WiFiDirectBroadcastReceiver(
	    	@NonNull final WifiP2pManager manager, @NonNull final WifiP2pManager.Channel channel,
			@NonNull final WiFiP2pHelper parent) {
			
	        super();
	        mManager = manager;
	        mChannel = channel;
	        mParent = parent;
	    }
	
	    @Override
	    public void onReceive(final Context context, final Intent intent) {
	        final String action = (intent != null) ? intent.getAction() : null;
	        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
	        	if (DEBUG) Log.v(TAG, "onReceive:WIFI_P2P_STATE_CHANGED_ACTION");
	        	// p2pの有効・無効状態が変化した時
	        	try {
					final int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
					if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
						// Wifi Direct mode is enabled
						mParent.setIsWifiP2pEnabled(true);
						if (DEBUG) Log.d(TAG, "P2P state changed, enabled");
					} else {
						mParent.setIsWifiP2pEnabled(false);
						mParent.resetData();
						if (DEBUG) Log.d(TAG, "P2P state changed, disabled");
					}
				} catch (final Exception e) {
					mParent.callOnError(e);
				}
	        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
				if (DEBUG) Log.v(TAG, "onReceive:WIFI_P2P_PEERS_CHANGED_ACTION");
	        	// p2pピアが変化した時
	            // request available peers from the wifi p2p manager. This is an
	            // asynchronous call and the calling activity is notified with a
	            // callback on PeerListListener.onPeersAvailable()
	            try {
                	mManager.requestPeers(mChannel, mParent.mPeerListListener);
				} catch (final Exception e) {
					mParent.callOnError(e);
				}
	            if (DEBUG) Log.d(TAG, "P2P peers changed");
	        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
				// ピアとの接続状態が変化した時
				try {
					final NetworkInfo networkInfo = intent.getParcelableExtra(
						WifiP2pManager.EXTRA_NETWORK_INFO);
		
					if (DEBUG) Log.v(TAG, "onReceive:WIFI_P2P_CONNECTION_CHANGED_ACTION, networkInfo=" + networkInfo);

					if (networkInfo.isConnected()) {
						// we are connected with the other device, request connection
						// info to find group owner IP
						mManager.requestConnectionInfo(mChannel, mParent.mConnectionInfoListener);
					} else {
						// It's a disconnect
						mParent.resetData();
					}
				} catch (final Exception e) {
					mParent.callOnError(e);
				}
	        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
				if (DEBUG) Log.v(TAG, "onReceive:WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
	        	// 自デバイスの状態が変化した時
	        	try {
					final WifiP2pDevice device = intent.getParcelableExtra(
						WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
					mParent.updateDevice(device);
				} catch (final Exception e) {
					mParent.callOnError(e);
				}
	        }
	    }
	}
	
}
