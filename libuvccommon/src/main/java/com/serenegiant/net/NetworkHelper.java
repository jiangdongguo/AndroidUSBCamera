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

import android.util.Log;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;

public class NetworkHelper {
	private static final String TAG = NetworkHelper.class.getSimpleName();

	public static String getLocalIPv4Address() {
		try {
			for (final NetworkInterface intf: Collections.list(NetworkInterface.getNetworkInterfaces())) {
			
				for (final InetAddress addr: Collections.list(intf.getInetAddresses())) {
					if (!addr.isLoopbackAddress() && (addr instanceof Inet4Address)) {
						return addr.getHostAddress();
					}
				}
			}
			// フォールバックする, 最初に見つかったInet4Address以外のアドレスを返す,
			// Bluetooth PANやWi-Fi Directでの接続時など
			for (final NetworkInterface intf: Collections.list(NetworkInterface.getNetworkInterfaces())) {
			
				for (final InterfaceAddress addr: intf.getInterfaceAddresses()) {
					final InetAddress ad = addr.getAddress();
					
					if (!ad.isLoopbackAddress() && !(ad instanceof Inet4Address)) {
						return ad.getHostAddress();
					}
				}
			}
//			for (final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
//				 (interfaces != null)
// 					&& interfaces.hasMoreElements(); ) {
//
//				final NetworkInterface intf =
//					interfaces.nextElement();
//				for (final Enumeration<InetAddress> addrs = intf.getInetAddresses();
//					addrs.hasMoreElements(); ) {
//
//					final InetAddress addr = addrs.nextElement();
//					if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
//						return addr.getHostAddress();
//					}
//				}
//			}
		} catch (final SocketException | NullPointerException e) {
			Log.e(TAG, "getLocalIPv4Address", e);
		}
		return null;
	}

	public static String getLocalIPv6Address() {
		try {
			for (final NetworkInterface intf: Collections.list(NetworkInterface.getNetworkInterfaces())) {
			
				for (final InetAddress addr: Collections.list(intf.getInetAddresses())) {

					if (!addr.isLoopbackAddress() && (addr instanceof Inet6Address)) {
						return addr.getHostAddress();
					}
				}
			}
//			for (final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
//				 (interfaces != null)
//				 	&& interfaces.hasMoreElements(); ) {
//
//				final NetworkInterface intf =
//					interfaces.nextElement();
//				for (final Enumeration<InetAddress> addrs = intf.getInetAddresses();
//					 addrs.hasMoreElements(); ) {
//
//					final InetAddress addr = addrs.nextElement();
//					if (!addr.isLoopbackAddress() && addr instanceof Inet6Address) {
//						return addr.getHostAddress();
//					}
//				}
//			}
		} catch (final SocketException | NullPointerException e) {
			Log.w(TAG, "getLocalIPv6Address", e);
		}
		return null;
	}
}
