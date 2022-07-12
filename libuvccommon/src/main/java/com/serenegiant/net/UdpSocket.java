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

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Collections;
import java.util.Enumeration;

/**
 * UDP経由での送受信を行うためのヘルパークラス
 */
public class UdpSocket {
	private DatagramChannel channel;
	private final InetSocketAddress broadcast;
	private SocketAddress sender;
	private String localAddress;
	private String remoteAddress;
	private int remotePort;

	/**
	 * コンストラクタ, バインドするポート
	 * @param port
	 */
	public UdpSocket(final int port) throws SocketException {
		try {
			InetAddress address = null;
			//  Create UDP socket
			channel = DatagramChannel.open();
			channel.configureBlocking(false);
			final DatagramSocket sock = channel.socket();

			// ブロードキャストする
			sock.setBroadcast(true);
			// 複数のプロセスがバインド出来るようにする
			sock.setReuseAddress(true);

			// 自分のアドレスを取得
			final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			for (final NetworkInterface intf : Collections.list(interfaces)) {

				if (intf.isLoopback()) {
					continue;
				}

				final Enumeration<InetAddress> inetAddresses = intf.getInetAddresses();
				for (final InetAddress addr : Collections.list(inetAddresses)) {
					if (addr instanceof Inet4Address)
						address = addr;
				}
			}
			localAddress = address.getHostAddress();
			// 任意のアドレスでバインド
			sock.bind(new InetSocketAddress(InetAddress.getByAddress(new byte[]{0, 0, 0, 0}), port));
			final byte[] addr = address.getAddress();
			// ブロードキャストアドレスを取得
			addr[3] = (byte) 255;
			final InetAddress broadcast_addr = InetAddress.getByAddress(addr);
			broadcast = new InetSocketAddress(broadcast_addr, port);
		} catch (final Exception e) {
			throw new SocketException("UdpSocket#constructor:" + e);
		}
	}

	/**
	 * 関係するリソースを破棄する, 再利用は出来ない
	 */
	public void release() {
		if (channel != null) {
			try {
				try {
					setSoTimeout(200);
				} catch (final Exception e) {
					// ignore
				}
				channel.close();
			} catch (final Exception e) {
				// ignore
			}
		}
		channel = null;
	}

	public DatagramChannel channel() {
		return channel;
	}

	public DatagramSocket socket() {
		return channel.socket();
	}

	public void setReceiveBufferSize(final int sz) throws SocketException {
		final DatagramSocket socket = channel != null ? channel.socket() : null;
		if (socket != null) {
			socket.setReceiveBufferSize(sz);
		}
	}

	/**
	 * 同じアドレスを他のソケットでも使用可能にするかどうか
	 * @param reuse
	 * @throws SocketException
	 */
	public void setReuseAddress(final boolean reuse) throws SocketException {
		final DatagramSocket socket = channel != null ? channel.socket() : null;
		if (socket != null) {
			socket.setReuseAddress(reuse);
		}
	}

	/**
	 * 同じアドレスを他のソケットでも利用可能かどうかを取得
	 * @return
	 * @throws SocketException
	 * @throws IllegalStateException
	 */
	public boolean getReuseAddress() throws SocketException, IllegalStateException {
		final DatagramSocket socket = channel != null ? channel.socket() : null;
		if (socket != null) {
			return socket.getReuseAddress();
		} else {
			throw new IllegalStateException("already released");
		}
	}

	/**
	 * ブロードキャストするかどうかをセット
	 * @param broadcast
	 * @throws SocketException
	 */
	public void setBroadcast(final boolean broadcast) throws SocketException {
		final DatagramSocket socket = channel != null ? channel.socket() : null;
		if (socket != null) {
			socket.setBroadcast(broadcast);
		}
	}

	/**
	 * ブロードキャストするかどうかを取得
	 * @return
	 * @throws SocketException
	 * @throws IllegalStateException
	 */
	public boolean getBroadcast() throws SocketException, IllegalStateException {
		final DatagramSocket socket = channel != null ? channel.socket() : null;
		if (socket != null) {
			return socket.getBroadcast();
		} else {
			throw new IllegalStateException("already released");
		}
	}

	/**
	 * タイムアウトをセット, 0ならタイムアウトなし
	 * @param timeout [ミリ秒]
	 * @throws SocketException
	 */
	public void setSoTimeout(final int timeout) throws SocketException {
		final DatagramSocket socket = channel != null ? channel.socket() : null;
		if (socket != null) {
			socket.setSoTimeout(timeout);
		}
	}

	/**
	 * タイムアウトを取得
	 * @return ミリ秒
	 * @throws SocketException
	 * @throws IllegalStateException
	 */
	public int getSoTimeout() throws SocketException, IllegalStateException {
		final DatagramSocket socket = channel != null ? channel.socket() : null;
		if (socket != null) {
			return socket.getSoTimeout();
		} else {
			throw new IllegalStateException("already released");
		}
	}

	/**
	 * 自分のアドレス文字列を取得
	 * @return
	 */
	public String local() {
		return localAddress;
	}

	/**
	 * 最後に受け取ったメッセージのアドレス文字列を取得
	 * @return
	 */
	public String remote() {
		return remoteAddress;
	}

	/**
	 * 最後に受け取ったメッセージのポート番号
	 * @return
	 */
	public int remotePort() {
		return remotePort;
	}

	/**
	 * 指定したメッセージをブロードキャストする
	 * @param buffer
	 * @throws IOException
	 * @throws IllegalStateException
	 */
	public void broadcast(final ByteBuffer buffer) throws IOException, IllegalStateException {
		if (channel == null) {
			throw new IllegalStateException("already released");
		}
		channel.send(buffer, broadcast);
	}

	/**
	 * 指定したメッセージをブロードキャストする
	 * @param bytes
	 * @throws IOException
	 * @throws IllegalStateException
	 */
	public void broadcast(final byte[] bytes) throws IOException, IllegalStateException {
		broadcast(ByteBuffer.wrap(bytes));
	}

	/**
	 * 受信待ち, タイムアウトまたはメッセージを受信するまで呼び出しスレッドをブロックする
	 * @param buffer
	 * @return
	 * @throws IOException
	 * @throws IllegalStateException
	 */
	public int receive(final ByteBuffer buffer) throws IOException, IllegalStateException {
		if (channel == null) {
			throw new IllegalStateException("already released");
		}
		final int read = buffer.remaining();
		sender = channel.receive(buffer);
		if (sender == null) {
			return -1;
		}
		final InetSocketAddress remote = (InetSocketAddress) sender;
		remoteAddress = remote.getAddress().getHostAddress();
		remotePort = remote.getPort();
		return read - buffer.remaining();
	}
}
