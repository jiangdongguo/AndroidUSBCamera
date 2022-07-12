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

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import androidx.annotation.NonNull;

import java.util.List;

/**
 * WiFiP2pHelperからのコールバックリスナー
 */
public interface WiFiP2pListener {
	/**
	 * WiFi Directの有効・無効が切り替わった時のコールバックメソッド
	 * @param enabled
	 */
	public void onStateChanged(final boolean enabled);
	
	/**
	 * 周囲のWiFi Direct対応機器の状態が変化した時のコールバックメソッド
	 * @param devices
	 */
	public void onUpdateDevices(@NonNull final List<WifiP2pDevice> devices);
	
	/**
	 * 機器へ接続した時のコールバックメソッド
	 * @param info
	 */
	public void onConnect(@NonNull final WifiP2pInfo info);
	
	/**
	 * 切断された時のコールバックメソッド
	 */
	public void onDisconnect();
	
	/**
	 * WiFi Directの処理中に例外生成した時のコールバックメソッド
	 * @param e
	 */
	public void onError(final Exception e);
}
