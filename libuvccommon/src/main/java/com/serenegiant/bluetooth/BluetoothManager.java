package com.serenegiant.bluetooth;
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
import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.serenegiant.utils.HandlerThreadHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Created by saki on 16/08/30.
 * Bluetooth機器の探索・接続・接続待ち・通信処理を行うためのヘルパークラス・メソッド
 * 	<uses-permission android:name="android.permission.BLUETOOTH" />
 *	<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
 * のパーミッションが必要
 */
@SuppressLint("MissingPermission")
public class BluetoothManager {
//	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = BluetoothManager.class.getSimpleName();

	/**
	 * 端末がBluetoothに対応しているかどうかを確認
	 * @return true Bluetoothに対応している
	 */
	public static boolean isAvailable() {
		try {
			final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
			return adapter != null;
		} catch (final Exception e) {
			Log.w(TAG, e);
		}
		return false;
	}

	/**
	 * 端末がBluetoothに対応していてBluetoothが有効になっているかどうかを確認
	 * パーミッションがなければfalse
	 * @return true Bluetoothが有効
	 */
	public static boolean isEnabled() {
		try {
			final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
			return adapter != null && adapter.isEnabled();
		} catch (final Exception e) {
			Log.w(TAG, e);
		}
		return false;
	}

	/**
	 * 端末がBluetoothに対応しているが無効になっていれば有効にするように要求する
	 * 前もってbluetoothAvailableで対応しているかどうかをチェックしておく
	 * Bluetoothを有効にするように要求した時は#onActivityResultメソッドで結果を受け取る
	 * 有効にできればRESULT_OK, ユーザーがキャンセルするなどして有効に出来なければRESULT_CANCELEDが返る
	 * @param activity
	 * @param requestCode
	 * @return true Bluetoothに対応していて既に有効になっている
	 * @throws SecurityException パーミッションがなければSecurityExceptionが投げられる
	 */
	public static boolean requestBluetoothEnable(@NonNull final Activity activity, final int requestCode) throws SecurityException {
		final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		if ((adapter != null) && !adapter.isEnabled()) {
			final Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			activity.startActivityForResult(intent, requestCode);
		}
		return adapter != null && adapter.isEnabled();
	}

	/**
	 * 端末がBluetoothに対応しているが無効になっていれば有効にするように要求する
	 * 前もってbluetoothAvailableで対応しているかどうかをチェックしておく
	 * Bluetoothを有効にするように要求した時は#onActivityResultメソッドで結果を受け取る
	 * 有効にできればRESULT_OK, ユーザーがキャンセルするなどして有効に出来なければRESULT_CANCELEDが返る
	 * @param fragment
	 * @param requestCode
	 * @return true Bluetoothに対応していて既に有効になっている
	 * @throws SecurityException パーミッションがなければSecurityExceptionが投げられる
	 */
	public static boolean requestBluetoothEnable(@NonNull final Fragment fragment, final int requestCode) throws SecurityException {
		final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		if ((adapter != null) && !adapter.isEnabled()) {
			final Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			fragment.startActivityForResult(intent, requestCode);
		}
		return adapter != null && adapter.isEnabled();
	}

	/**
	 * 端末がBluetoothに対応しているが無効になっていれば有効にするように要求する
	 * 前もってbluetoothAvailableで対応しているかどうかをチェックしておく
	 * Bluetoothを有効にするように要求した時は#onActivityResultメソッドで結果を受け取る
	 * 有効にできればRESULT_OK, ユーザーがキャンセルするなどして有効に出来なければRESULT_CANCELEDが返る
	 * @param fragment
	 * @param requestCode
	 * @return true Bluetoothに対応していて既に有効になっている
	 * @throws SecurityException パーミッションがなければSecurityExceptionが投げられる
	 */
	public static boolean requestBluetoothEnable(@NonNull final androidx.fragment.app.Fragment fragment, final int requestCode) throws SecurityException {
		final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		if ((adapter != null) && !adapter.isEnabled()) {
			final Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			fragment.startActivityForResult(intent, requestCode);
		}
		return adapter != null && adapter.isEnabled();
	}

	/**
	 * ペアリング済みのBluetooth機器一覧を取得する
	 * @return Bluetoothに対応していないまたは無効ならnull
	 */
	@Nullable
	public static Set<BluetoothDevice> getBondedDevices() {
		final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		if ((adapter != null) && adapter.isEnabled()) {
			return adapter.getBondedDevices();
		}
		return null;
	}

	/**
	 * 他の機器から探索可能になるように要求する
	 * bluetoothに対応していないか無効になっている時はIllegalStateException例外を投げる
	 * @param activity
	 * @param duration 探索可能時間[秒]
	 * @return 既に探索可能であればtrue
	 * @throws IllegalStateException
	 */
	public static boolean requestDiscoverable(@NonNull final Activity activity, final int duration) throws IllegalStateException {
		final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		if ((adapter == null) || !adapter.isEnabled()) {
			throw new IllegalStateException("bluetoothに対応していないか無効になっている");
		}
		if (adapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			final Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, duration);
			activity.startActivity(intent);
		}
		return adapter.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE;
	}

	/**
	 * 他の機器から探索可能になるように要求する
	 * bluetoothに対応していないか無効になっている時はIllegalStateException例外を投げる
	 * @param fragment
	 * @param duration 0以下ならデフォルトの探索可能時間で120秒、 最大300秒まで設定できる
	 * @return
	 * @throws IllegalStateException
	 */
	public static boolean requestDiscoverable(@NonNull final Fragment fragment, final int duration) throws IllegalStateException {
		final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		if ((adapter == null) || !adapter.isEnabled()) {
			throw new IllegalStateException("bluetoothに対応していないか無効になっている");
		}
		if (adapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			final Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			if ((duration > 0) && (duration <= 300)) {
				intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, duration);
			}
			fragment.startActivity(intent);
		}
		return adapter.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE;
	}

//********************************************************************************
//
//********************************************************************************
	/** SPP(Serial Port Profile)の場合のUUID */
	public static final UUID UUID_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	// 接続状態
	public static final int STATE_RELEASED = -1;
	public static final int STATE_NONE = 0;       // we're doing nothing
	public static final int STATE_LISTEN = 1;     // now listening for incoming connections
	public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
	public static final int STATE_CONNECTED = 3;  // now onConnect to a remote device

	public interface BluetoothManagerCallback {
		/**
		 * Bluetooth機器探索結果が更新された時
		 * @param devices
		 */
		public void onDiscover(final Collection<BluetoothDeviceInfo> devices);
		/**
		 * リモート機器と接続した時
		 * #startListenによる接続待ち中にリモート機器から接続されたか
		 * #connect呼び出しによるリモート機器への接続要求が成功した
		 * @param name
		 * @param remoteAddr
		 */
		public void onConnect(final String name, final String remoteAddr);
		/**
		 * リモート機器との接続が解除された時
		 */
		public void onDisconnect();
		/**
		 * リモート機器への接続要求が失敗した時(#connectの呼び出しが失敗した時)
		 */
		public void onFailed();
		/**
		 * データを受信した時
		 * @param message
		 * @param length
		 */
		public void onReceive(final byte[] message, final int length);
	}

	@NonNull
	private final Object mSync = new Object();
	@NonNull
	private final WeakReference<Context> mWeakContext;
	/** コールバックリスナー保持用 */
	private final Set<BluetoothManagerCallback> mCallbacks = new CopyOnWriteArraySet<BluetoothManagerCallback>();
	/**
	 * セキュア接続に使用するプロトコル(プロファイル)を識別するためのUUID
	 * Android同士でつなぐなら任意で可。
	 * PC等のBluetoothシリアル通信を行うならUUID_SPPを使う
	 */
	@NonNull
	private final UUID mSecureProfileUUID;
	/**
	 * インセキュア接続に使用するプロトコル(プロファイル)を識別するためのUUID
	 * Android同士でつなぐなら任意で可。
	 * PC等のBluetoothシリアル通信を行うならUUID_SPPを使う
	 */
	@NonNull
	private final UUID mInSecureProfileUUID;
	@NonNull
	private final BluetoothAdapter mAdapter;
	/** サービス名, 任意 */
	private final String mName;
	private volatile int mState;

	/** セキュア接続待ちスレッド */
	private ListeningThread mSecureListeningThread;
	/** インセキュア接続待ちスレッド */
	private ListeningThread mInSecureListeningThread;
	/** 接続要求スレッド */
	private ConnectingThread mConnectingThread;
	/** 受信待ちスレッド */
	private ReceiverThread mReceiverThread;
	/** ワーカースレッド上での非同期処理(主にコールバック呼び出し)のためのHandler */
	private Handler mAsyncHandler;

	private final List<BluetoothDeviceInfo> mDiscoveredDeviceList = new ArrayList<BluetoothDeviceInfo>();

	/**
	 * コンストラクタ
	 * @param context
	 * @param name　サービス名, 任意, nullまたは空文字列ならば端末のモデルとIDを使う
	 * @param secureProfileUUID 接続に使用するプロトコル(プロファイル)を識別するためのUUID。セキュア接続用。Android同士でつなぐなら任意で可。PC等のBluetoothシリアル通信を行うならUUID_SPPを使う
	 * @param callback
	 */
	public BluetoothManager(@NonNull final Context context, final String name,
		@NonNull final UUID secureProfileUUID,
		@NonNull final BluetoothManagerCallback callback) {

		this(context, name, secureProfileUUID, null, callback);
	}

	/**
	 * コンストラクタ
	 * @param context
	 * @param name　サービス名, 任意, nullまたは空文字列ならば端末のモデルとIDを使う
	 * @param secureProfileUUID 接続に使用するプロトコル(プロファイル)を識別するためのUUID。セキュア接続用。Android同士でつなぐなら任意で可。PC等のBluetoothシリアル通信を行うならUUID_SPPを使う
	 * @param inSecureProfileUUID 接続に使用するプロトコル(プロファイル)を識別するためのUUID。インセキュア接続用。Android同士でつなぐなら任意で可。PC等のBluetoothシリアル通信を行うならUUID_SPPを使う nullならsecureProfileUUIDを使う
	 * @param callback
	 */
	public BluetoothManager(@NonNull final Context context, final String name,
		@NonNull final UUID secureProfileUUID,
		@Nullable final UUID inSecureProfileUUID,
		@NonNull final BluetoothManagerCallback callback) {

		mWeakContext = new WeakReference<Context>(context);
		mName = !TextUtils.isEmpty(name) ? name : Build.MODEL + "_" + Build.ID;
		mSecureProfileUUID = secureProfileUUID;
		mInSecureProfileUUID = inSecureProfileUUID != null ? inSecureProfileUUID : secureProfileUUID;
		if (callback != null) {
			mCallbacks.add(callback);
		}
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		if ((mAdapter == null) || !mAdapter.isEnabled()) {
			throw new IllegalStateException("bluetoothに対応していないか無効になっている");
		}
		mState = STATE_NONE;
		mAsyncHandler = HandlerThreadHandler.createHandler(TAG);
		final IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		context.registerReceiver(mBroadcastReceiver, filter);
	}

	/**
	 * 関連するリソースを破棄する
	 */
	public void release() {
		mCallbacks.clear();
		synchronized (mSync) {
			if (mState != STATE_RELEASED) {
				mState = STATE_RELEASED;
				stop();
				if (mAsyncHandler != null) {
					try {
						mAsyncHandler.getLooper().quit();
					} catch (final Exception e) {
						// ignore
					}
					mAsyncHandler = null;
				}
				try {
					getContext().unregisterReceiver(mBroadcastReceiver);
				} catch (final Exception e) {
					// ignore
				}
			}
		}
	}

	public void addCallback(final BluetoothManagerCallback callback) {
		if (callback != null) {
			mCallbacks.add(callback);
		}
	}

	public void removeCallback(final BluetoothManagerCallback callback) {
		mCallbacks.remove(callback);
	}

	/**
	 * 既にペアリング済みのBluetooth機器一覧を取得する
	 * @return
	 */
	public Collection<BluetoothDeviceInfo> getPairedDevices() {
//		if (DEBUG) Log.v(TAG, "getPairedDevices:");
		checkReleased();
		final List<BluetoothDeviceInfo> result = new ArrayList<BluetoothDeviceInfo>();
		synchronized (mSync) {
			if (mAdapter.isDiscovering()) {
				// 探索中ならキャンセルする
				mAdapter.cancelDiscovery();
			}
			// 既にペアリング済みのBluetooth機器一覧を取得する
			final Set<BluetoothDevice> pairedDevices = mAdapter.getBondedDevices();
			if (pairedDevices.size() > 0) {
				for (final BluetoothDevice device : pairedDevices) {
					result.add(new BluetoothDeviceInfo(device));
				}
			}
		}
		return result;
	}

	/**
	 * Bluetooth機器の探索を開始する。
	 * bluetoothに対応していないか無効になっている時はIllegalStateException例外を投げる
	 * 新しく機器が見つかった時はBluetoothDevice.ACTION_FOUNDがブロードキャストされるので
	 * ブロードキャストレシーバーを登録しておく必要がある
	 * @throws IllegalStateException
	 */
	public void startDiscovery() throws IllegalStateException {
//		if (DEBUG) Log.v(TAG, "startDiscovery:");
		synchronized (mSync) {
			if (mAdapter.isDiscovering()) {
				// 既に探索中なら一旦キャンセルする
				mAdapter.cancelDiscovery();
			}
			// 既にペアリング済みのBluetooth機器一覧を取得する
			final Set<BluetoothDevice> pairedDevices = mAdapter.getBondedDevices();
			synchronized (mDiscoveredDeviceList) {
				mDiscoveredDeviceList.clear();
				if (pairedDevices.size() > 0) {
					for (final BluetoothDevice device : pairedDevices) {
						mDiscoveredDeviceList.add(new BluetoothDeviceInfo(device));
					}
					callOnDiscover();
				}
			}
//			if (DEBUG) Log.v(TAG, "startDiscovery:探索開始");
			mAdapter.startDiscovery();
		}
	}

	/**
	 * Bluetooth機器の探索中ならキャンセルする
	 */
	public void stopDiscovery() {
//		if (DEBUG) Log.v(TAG, "stopDiscovery:");
		synchronized (mSync) {
			if (mAdapter.isDiscovering()) {
				// Bluetoothに対応していて有効になっていて探索中ならキャンセルする
				mAdapter.cancelDiscovery();
			}
		}
	}

	/**
	 * 接続待ち(サーバーサイド)
	 */
	public void startListen() {
//		if (DEBUG) Log.v(TAG, "startListen:");
		synchronized (mSync) {
			checkReleased();
			internalStartListen();
		}
	}

	/**
	 * 指定したリモートBluetooth機器へ接続開始する(クライアントサイド)
	 * @param info
	 * @throws IllegalStateException
	 */
	public void connect(final BluetoothDeviceInfo info) throws IllegalStateException {
		checkReleased();
		connect(mAdapter.getRemoteDevice(info.address));
	}

	/**
	 * 指定したMACアドレスを持つリモートBluetooth機器へ接続開始する(クライアントサイド)
	 * @param macAddress
	 * @throws IllegalArgumentException アドレスが不正
	 * @throws IllegalStateException
	 */
	public void connect(final String macAddress)
		throws IllegalArgumentException, IllegalStateException {

		checkReleased();
		connect(mAdapter.getRemoteDevice(macAddress));
	}

	/**
	 * 指定したリモートBluetooth機器に接続開始する(クライアントサイド)
	 * @param device
	 * @throws IllegalStateException
	 */
	public void connect(final BluetoothDevice device) throws IllegalStateException {
//		if (DEBUG) Log.d(TAG, "connect to: " + device);
		synchronized (mSync) {
			checkReleased();
			internalCancel(STATE_CONNECTING, false);

			// 接続スレッドを生成する
			try {
				// セキュア接続を試みる
				mConnectingThread = new ConnectingThread(device, true);
			} catch (final IOException e) {
				try {
					// セキュア接続できなければインセキュア接続を試みる
					mConnectingThread = new ConnectingThread(device, false);
				} catch (final IOException e1) {
					throw new IllegalStateException(e1);
				}
			}
			mConnectingThread.start();
		}
	}

	/**
	 * 接続中なら切断する
	 */
	public void stop() {
//		if (DEBUG) Log.d(TAG, "stop");
		synchronized (mSync) {
			internalCancel(STATE_NONE, true);
		}
	}

	/**
	 * 指定したデータを送信する
	 * @param message
	 * @throws IllegalStateException
	 */
	public void send(final byte[] message) throws IllegalStateException {
//		if (DEBUG) Log.d(TAG, "send");
		synchronized (mSync) {
			checkReleased();
			if (mReceiverThread != null) {
				mReceiverThread.write(message);
			}
		}
	}

	/**
	 * 指定したデータを送信する
	 * @param message
	 * @param offset
	 * @param len
	 * @throws IllegalStateException
	 */
	public void send(final byte[] message, final int offset, final int len)
		throws IllegalStateException {

//		if (DEBUG) Log.d(TAG, "send");
		synchronized (mSync) {
			checkReleased();
			if (mReceiverThread != null) {
				mReceiverThread.write(message, offset, len);
			}
		}
	}

	/**
	 * 現在の接続状態を取得
	 * @return
	 */
	public int getState() {
		synchronized (mSync) {
			return mState;
		}
	}

	/**
	 * 既に破棄されているかどうかを取得
	 * @return
	 */
	public boolean isReleased() {
		synchronized (mSync) {
			return mState == STATE_RELEASED;
		}
	}

	/**
	 * 接続されているかどうかを取得
	 * @return
	 */
	public boolean isConnected() {
		synchronized (mSync) {
			return mState == STATE_CONNECTED;
		}
	}

	/**
	 * 接続待機中かどうかを取得
	 * @return
	 */
	public boolean isListening() {
		synchronized (mSync) {
			return mState == STATE_LISTEN;
		}
	}

//================================================================================
// 共通部分
//================================================================================
	protected Context getContext() {
		return mWeakContext.get();
	}

	private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
//			if (DEBUG) Log.v(TAG, "onReceive:intent=" + intent);
			final String action = intent != null ? intent.getAction() : null;
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				// ペアリング済みのものは既に追加されているので無視して、それ以外のものを追加する
				if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
					synchronized (mDiscoveredDeviceList) {
						mDiscoveredDeviceList.add(new BluetoothDeviceInfo(device));
					}
					callOnDiscover();
				}
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				// 探索終了
				callOnDiscover();
			}
		}
	};

	/**
	 * 既にreleaseされているかどうかをチェックしてreleaseされていればIllegalStateExceptionを投げる
	 * @throws IllegalStateException
	 */
	private void checkReleased() throws IllegalStateException {
		if (mState == STATE_RELEASED) {
			throw new IllegalStateException("already released");
		}
	}

	/**
	 * 接続待ち(サーバーサイド)の実態
	 * releaseされてても例外生成しない
	 */
	private void internalStartListen() {
//		if (DEBUG) Log.v(TAG, "internalStartListen:");
		synchronized (mSync) {
			internalCancel(STATE_LISTEN, false);
			if (isReleased()) return;
			if (mSecureListeningThread == null) {
				mSecureListeningThread = new ListeningThread(true);
				mSecureListeningThread.start();
			}
			if (mInSecureListeningThread == null) {
				mInSecureListeningThread = new ListeningThread(false);
				mInSecureListeningThread.start();
			}
		}
	}

	/**
	 * 実行中のスレッドをキャンセルする
	 * mSyncをロックして呼び出すこと
	 */
	private void internalCancel(final int newState, final boolean cancelListening) {
		// 探索中ならキャンセルする
		if (mAdapter.isDiscovering()) {
			mAdapter.cancelDiscovery();
		}
		// 接続中ならキャンセルする
		if (mConnectingThread != null) {
			mConnectingThread.cancel();
			mConnectingThread = null;
		}
		// 通信中ならキャンセルする
		if (mReceiverThread != null) {
			mReceiverThread.cancel();
			mReceiverThread = null;
		}
		// 接続待ち中ならキャンセルする
		if ((mState == STATE_RELEASED) || cancelListening) {
			if (mSecureListeningThread != null) {
				mSecureListeningThread.cancel();
				mSecureListeningThread = null;
			}
			if (mInSecureListeningThread != null) {
				mInSecureListeningThread.cancel();
				mInSecureListeningThread = null;
			}
		}
		setState(newState);
	}

//--------------------------------------------------------------------------------
	/**
	 * #startDiscoveryの呼び出しによって新しいリモート機器が見つかった時
	 */
	protected void callOnDiscover() {
//		if (DEBUG) Log.v(TAG, "callOnDiscover:");
		final List<BluetoothDeviceInfo> devices;
		synchronized (mDiscoveredDeviceList) {
			devices = new ArrayList<BluetoothDeviceInfo>(mDiscoveredDeviceList);
		}
		synchronized (mSync) {
			if (mAsyncHandler != null) {
				mAsyncHandler.post(() -> {
					for (final BluetoothManagerCallback callback: mCallbacks) {
						try {
							callback.onDiscover(devices);
						} catch (final Exception e) {
							mCallbacks.remove(callback);
							Log.w(TAG, e);
						}
					}
				});
			}
		}
	}

	/**
	 * 接続待ち中にリモート機器から接続された、
	 * または#connectによるリモート機器への接続要求が成功した時
	 * @param device
	 * @throws IllegalStateException
	 */
	protected void callOnConnect(final BluetoothDevice device) throws IllegalStateException {
//		if (DEBUG) Log.v(TAG, "callOnConnect:");
		synchronized (mSync) {
			if (isReleased()) return;
			if (mAsyncHandler != null) {
				mAsyncHandler.post(() -> {
					for (final BluetoothManagerCallback callback: mCallbacks) {
						try {
							callback.onConnect(device.getName(), device.getAddress());
						} catch (final Exception e) {
							mCallbacks.remove(callback);
							Log.w(TAG, e);
						}
					}
				});
			}
		}
	}

	/**
	 * リモート機器との接続が切断された時の処理
	 */
	protected void callOnDisConnect() {
//		if (DEBUG) Log.v(TAG, "callOnDisConnect:");
		synchronized (mSync) {
			if (isReleased()) return;
			if (mAsyncHandler != null) {
				mAsyncHandler.post(() -> {
					for (final BluetoothManagerCallback callback: mCallbacks) {
						try {
							callback.onDisconnect();
						} catch (final Exception e) {
							mCallbacks.remove(callback);
							Log.w(TAG, e);
						}
					}
				});
			}
		}
		// 再度接続待ちする
		if (!isReleased()) {
			internalStartListen();
		}
	}

	/**
	 * #connectによるリモート機器との接続要求が失敗した時
	 */
	protected void callOnFailed() {
//		if (DEBUG) Log.v(TAG, "callOnFailed:");
		// 接続に失敗した時の処理
		synchronized (mSync) {
			if (isReleased()) return;
			if (mAsyncHandler != null) {
				mAsyncHandler.post(() -> {
					for (final BluetoothManagerCallback callback: mCallbacks) {
						try {
							callback.onFailed();
						} catch (final Exception e) {
							mCallbacks.remove(callback);
							Log.w(TAG, e);
						}
					}
				});
			}
		}
		// 接続待ちする
		if (!isReleased()) {
			internalStartListen();
		}
	}

	/**
	 * リモート機器からデータを受信した時
	 * @param message
	 * @param length
	 */
	protected void callOnReceive(final byte[] message, final int length) {
//		if (DEBUG) Log.v(TAG, "callOnReceive:");
		final byte[] msg = new byte[length];
		System.arraycopy(message, 0, msg, 0, length);
		synchronized (mSync) {
			if (isReleased()) return;
			if (mAsyncHandler != null) {
				mAsyncHandler.post(() -> {
					for (final BluetoothManagerCallback callback: mCallbacks) {
						try {
							callback.onReceive(msg, length);
						} catch (final Exception e) {
							mCallbacks.remove(callback);
							Log.w(TAG, e);
						}
					}
				});
			}
		}
	}

//--------------------------------------------------------------------------------
	/**
	 * 接続状態をセット
	 * @param state
	 */
	private void setState(int state) {
		synchronized (mSync) {
			if (mState != STATE_RELEASED) {
//				if (DEBUG) Log.d(TAG, "setState() " + mState + " -> " + state);
				mState = state;
			}
		}
	}

	/**
	 * 通信スレッドを生成して通信開始
	 * 接続待ちスレッドまたは接続要求スレッドでリモート機器との接続に成功した時に呼ばれる
	 * @param socket
	 * @param device
	 */
	protected void onConnect(final BluetoothSocket socket, final BluetoothDevice device) {
//		if (DEBUG) Log.d(TAG, "onConnect");

		synchronized (mSync) {
			internalCancel(STATE_CONNECTED, true);
			// 通信スレッドを生成&開始
			mReceiverThread = new ReceiverThread(socket);
			mReceiverThread.start();

			// 接続した相手とともにコールバックを呼び出す
			callOnConnect(device);
		}
	}

//--------------------------------------------------------------------------------
	/**
	 * 通信スレッドおよび接続要求スレッドのベースクラス
	 */
	private abstract static class BluetoothSocketThread extends Thread {
		protected final BluetoothSocket mmSocket;
		protected volatile boolean mIsCanceled;

		public BluetoothSocketThread(final String name, final BluetoothSocket socket) {
			super(name);
			mmSocket = socket;
		}

		/**
		 * 待機状態をキャンセルする
		 * このスレッドで保持しているBluetoothSocketをcloseする
		 */
		public void cancel() {
//			if (DEBUG) Log.d(TAG, "cancel:" + this);
			mIsCanceled = true;
			try {
				mmSocket.close();
			} catch (final IOException e) {
				Log.e(TAG, "failed to call BluetoothSocket#close", e);
			}
		}
	}

	/**
	 * 通信スレッド
	 */
	private class ReceiverThread extends BluetoothSocketThread {
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ReceiverThread(final BluetoothSocket socket) {
			super("ReceiverThread:" + mName, socket);
//			if (DEBUG) Log.d(TAG, "create ReceiverThread:");
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// 受信待ち用のInputStreamと送信用のOutputStreamを取得する
			// 普通のInputStreamはBufferedInputStreamで、OutputStreamは
			// BufferedOutputStreamでラップするけどここで取得されるStreamを
			// ラップすると上手く動かないみたい
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (final IOException e) {
				Log.e(TAG, "temp sockets not created", e);
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		@Override
		public void run() {
//			if (DEBUG) Log.v(TAG, "ReceiverThread#run:");
			final byte[] buffer = new byte[1024];
			int bytes;

			// 受信ループ
			for ( ; mState == STATE_CONNECTED ; ) {
				try {
					//
					bytes = mmInStream.read(buffer);
//					if (DEBUG) Log.v(TAG, "ReceiverThread#run:read:bytes=" + bytes);
					if (bytes > 0) {
						callOnReceive(buffer, bytes);
					}
				} catch (final IOException e) {
					if (!mIsCanceled) Log.d(TAG, "disconnected", e);
					callOnDisConnect();
					break;
				}
			}

//			if (DEBUG) Log.v(TAG, "ReceiverThread#run:finished");
		}

		/**
		 * Bluetooth機器へ送信
		 * @param buffer 送信データ
		 */
		public void write(final byte[] buffer) throws IllegalStateException {
//			if (DEBUG) Log.d(TAG, "ReceiverThread#write:");
			if (mState != STATE_CONNECTED) {
				throw new IllegalStateException("already disconnected");
			}
			try {
				mmOutStream.write(buffer);
			} catch (final IOException e) {
				if (!mIsCanceled) {
					throw new IllegalStateException(e);
				}
			}
		}

		/**
		 * Bluetooth機器へ送信
		 * @param buffer
		 * @param offset
		 * @param len
		 * @throws IllegalStateException
		 */
		public void write(final byte[] buffer, final int offset, final int len) throws IllegalStateException {
//			if (DEBUG) Log.d(TAG, "ReceiverThread#write:");
			if (mState != STATE_CONNECTED) {
				throw new IllegalStateException("already disconnected");
			}
			try {
				mmOutStream.write(buffer, offset, len);
			} catch (final IOException e) {
				if (!mIsCanceled) {
					throw new IllegalStateException(e);
				}
			}
		}
	}

//================================================================================
// サーバーサイドの実装
//================================================================================
	/**
	 * 接続待ちスレッド
	 */
	private class ListeningThread extends Thread {
		// The local server socket
		private final BluetoothServerSocket mmServerSocket;
		private volatile boolean mIsCanceled;

		/**
		 * コンストラクタ
		 * @param secure セキュア接続を待機するならtrue
		 */
		public ListeningThread(final boolean secure) {
			super("ListeningThread:" + mName);

			// Create a new listening server socket
			BluetoothServerSocket tmp = null;
			try {
				if (secure) {
				// セキュアな接続を行うためのBluetoothServerSocketを生成
					tmp = mAdapter.listenUsingRfcommWithServiceRecord(mName, mSecureProfileUUID);
				} else {
					tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(mName, mInSecureProfileUUID);
				}
			} catch (final IOException e) {
				Log.w(TAG, e);
			}
			mmServerSocket = tmp;
		}

		@Override
	    public void run() {
//	        if (DEBUG) Log.d(TAG, "ListeningThread#run:");
	        BluetoothSocket socket;

	        // 接続待ちループ
LOOP:		for ( ; mState != STATE_CONNECTED ; ) {
	            try {
					// 接続完了するか例外生成(#cancel内でcloseされた時など)するまでブロックする
	                socket = mmServerSocket.accept();
	            } catch (final IOException e) {
	                if (!mIsCanceled) Log.d(TAG, e.getMessage());
	                break;
	            }

	            // 接続が受け入れられた時
	            if (socket != null) {
					switch (mState) {
					case STATE_LISTEN:
					case STATE_CONNECTING:
						// 正常に接続された時
						onConnect(socket, socket.getRemoteDevice());
						break LOOP;
					case STATE_NONE:
					case STATE_CONNECTED:
						// 接続できなかったか既に別の接続がある時, 新しいのは終了させる
						try {
							socket.close();
						} catch (final IOException e) {
							Log.w(TAG, "Could not close unwanted socket", e);
						}
						break;
					}
	            }
	        }
//	        if (DEBUG) Log.i(TAG, "ListeningThread#run:finished");
	    }

		/**
		 * 接続待ちをキャンセルする
		 */
	    public void cancel() {
//	        if (DEBUG) Log.d(TAG, "cancel:" + this);
			mIsCanceled = true;
	        try {
	            mmServerSocket.close();
	        } catch (IOException e) {
	            Log.e(TAG, "close() of server failed", e);
	        }
	    }
	}

//================================================================================
// クライアントサイドの実装
//================================================================================
	/**
	 * ConnectingThread用のBluetoothSocketを生成するためのヘルパーメソッド
	 * @param device
	 * @param secure セキュア接続用のBluetoothSocketを生成するならtrue
	 * @return
	 */
	private BluetoothSocket createBluetoothSocket(final BluetoothDevice device,
		final boolean secure) throws IOException {

		// 接続を行うためのBluetoothSocketを生成
		return secure
			? device.createRfcommSocketToServiceRecord(mSecureProfileUUID)				// セキュア接続
			: device.createInsecureRfcommSocketToServiceRecord(mInSecureProfileUUID);	// インセキュア接続
	}

	/**
	 * 接続スレッド
	 */
	private class ConnectingThread extends BluetoothSocketThread {
		private final BluetoothDevice mmDevice;

		/**
		 * コンストラクタ
		 * @param device
		 * @param secure セキュア接続用のBluetoothSocketを生成するならtrue
		 * @throws IOException
		 */
		public ConnectingThread(final BluetoothDevice device, final boolean secure)
			throws IOException {

			super("ConnectingThread:" + mName,
				createBluetoothSocket(device, secure));

			mmDevice = device;
		}

		@Override
		public void run() {
//			if (DEBUG) Log.i(TAG, "ConnectingThread#run:");

			// 機器探索が動いたままだと遅くなるのでキャンセルする
			if (mAdapter.isDiscovering()) {
				mAdapter.cancelDiscovery();
			}

			// 接続待ち
			try {
				// 接続完了するか例外生成(#cancel内でcloseされた時など)するまでブロックする
				mmSocket.connect();
			} catch (final IOException e) {
				Log.w(TAG, e);
				try {
					mmSocket.close();
				} catch (final IOException e1) {
					if (!mIsCanceled) Log.w(TAG, "failed to close socket", e1);
				}
				callOnFailed();
				return;
			}

			synchronized (mSync) {
				mConnectingThread = null;
			}

			// Start the onConnect thread
			onConnect(mmSocket, mmDevice);
		}
	}

}
