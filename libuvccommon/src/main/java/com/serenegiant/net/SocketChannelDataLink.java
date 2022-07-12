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

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import android.util.Log;

import com.serenegiant.utils.BuildCheck;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.ByteChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class SocketChannelDataLink extends AbstractChannelDataLink {
	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = SocketChannelDataLink.class.getSimpleName();
	
	public static final int DEFAULT_SERVER_PORT = 6000;
	/** クライアントからの接続待ちを行うためのスレッドの実行部 */
	private ServerTask mServerTask;
	
	/**
	 * コンストラクタ
	 */
	public SocketChannelDataLink() {
		super();
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
	}
	
	/**
	 * コンストラクタ
	 * @param callback
	 */
	public SocketChannelDataLink(final Callback callback) {
		super(callback);
	}
	
	public void release() {
		stop();
		super.release();
	}
				
	public Client connectTo(final String addr) throws IOException {
		return connectTo(addr, DEFAULT_SERVER_PORT);
	}
	
	/**
	 * 指定したアドレス・ポートへ接続する, クライアントからの接続待ち中で無くても構わない
	 * @param addr
	 * @param port
	 * @return
	 * @throws IOException
	 */
	public Client connectTo(final String addr, final int port) throws IOException {
		if (DEBUG) Log.v(TAG, "connectTo:addr=" + addr + ",port=" + port);
		Client result;
		try {
			final InetAddress address = InetAddress.getByName(addr);
			result = new Client(this, addr, port);
			add(result);
		} catch (final UnknownHostException e) {
			throw new IOException(e.getMessage());
		}
		return result;
	}
	
	public synchronized boolean isRunning() {
		return mServerTask != null;
	}
	
	/**
	 * クライアントからの接続待ちを開始する
	 * 待受けポートはDEFAULT_SERVER_PORT
	 * @throws IllegalStateException
	 */
	public void start()
		throws IllegalStateException {
		
		start(DEFAULT_SERVER_PORT, null);
	}
		
	/**
	 * クライアントからの接続待ちを開始する
	 * 待受けポートはDEFAULT_SERVER_PORT
	 * @param callback
	 * @throws IllegalStateException
	 */
	public void start(final Callback callback)
		throws IllegalStateException {
		
		start(DEFAULT_SERVER_PORT, callback);
	}
	
	/**
	 * クライアントからの接続待ちを開始する
	 * @param port
	 * @param callback
	 * @throws IllegalStateException
	 */
	public synchronized void start(final int port, final Callback callback)
		throws IllegalStateException {
		
		if (DEBUG) Log.v(TAG, "start");
		add(callback);
		if (mServerTask == null) {
			mServerTask = new ServerTask(port);
			new Thread(mServerTask).start();
		} else {
			Log.d(TAG, "already started");
		}
	}
	
	/**
	 * クライアントからの接続待ちを終了
	 */
	public synchronized void stop() {
		if (DEBUG) Log.v(TAG, "stop");
		if (mServerTask != null) {
			mServerTask.release();
			mServerTask = null;
		}
	}
	
	/**
	 * 通信クライアント
	 */
	public static class Client extends AbstractClient {
		private String mAddr;
		private int mPort;
		
		public Client(@NonNull final SocketChannelDataLink parent, @NonNull final ByteChannel channel) {
			super(parent, channel);
			if (DEBUG) Log.v(TAG, "Client#コンストラクタ:channel=" + channel);
			internalStart();
		}
		
		public Client(@NonNull final SocketChannelDataLink parent, final String addr, final int port) {
			super(parent, null);
			if (DEBUG) Log.v(TAG, "Client#コンストラクタ:addr=" + addr + ",port=" + port);
			mAddr = addr;
			mPort = port;
			internalStart();
		}
		
		/**
		 * このClientのアドレスを取得
		 * @return
		 */
		public synchronized String getAddress() {
			final Socket socket = (mChannel instanceof SocketChannel)
				? ((SocketChannel)mChannel).socket() : null;
			final InetAddress address = socket != null ? socket.getInetAddress() : null;
			return address != null ? address.getHostAddress() : null;
		}
		
		/**
		 * このClientのポートを取得
		 * @return
		 */
		public synchronized int getPort() {
			final Socket socket = (mChannel instanceof SocketChannel)
				? ((SocketChannel)mChannel).socket() : null;
			return socket != null ? socket.getPort() : 0;
		}
		
		public synchronized boolean isConnected() {
			return (mChannel instanceof SocketChannel)
				&& ((SocketChannel)mChannel).isConnected();
		}
		
		/**
		 * 初期化処理, 受信用ワーカースレッド上で実行
		 * @throws IOException
		 */
		protected synchronized void init() throws IOException {
			if (DEBUG) Log.v(TAG, "Client#init:");
			try {
				if (mChannel == null) {
					final InetSocketAddress address = new InetSocketAddress(mAddr, mPort);
					mChannel = SocketChannel.open(address);
				}
				setInit(true);
			} finally {
				notifyAll();
			}
			if (DEBUG) Log.v(TAG, "Client#init:finished");
		}
		
	}
	
	/**
	 * クライアントからの接続待ちを行うためのスレッド
	 */
	private class ServerTask implements Runnable {
		private final int mPort;
		private volatile boolean mIsRunning;
		private ServerSocketChannel mServerChannel;
		
		public ServerTask(final int port) {
			mPort = port;
		}
		
		public synchronized void release() {
		if (DEBUG) Log.v(TAG, "ServerTask#release:");
			mIsRunning = false;
			if (mServerChannel != null) {
				try {
					mServerChannel.close();
				} catch (final IOException e) {
					// ignore
				}
				mServerChannel = null;
			}
			if (DEBUG) Log.v(TAG, "ServerTask#release:finished");
		}
		
		/**
		 * クライアントからの接続待ち処理
		 */
		@Override
		public void run() {
			if (DEBUG) Log.v(TAG, "ServerTask#run:");
			try {
				init();
				for (; mIsRunning; ) {
					serverLoop();
				}
			} catch (final Exception e) {
				Log.w(TAG, e);
			} finally {
				release();
			}
			if (DEBUG) Log.v(TAG, "ServerTask#run:finished");
		}
		
		@SuppressLint("NewApi")
		private synchronized void init() throws IOException {
			if (DEBUG) Log.v(TAG, "ServerTask#init:");
			final String addr = NetworkHelper.getLocalIPv4Address();
			final SocketAddress address = new InetSocketAddress(addr, mPort);
			mServerChannel = ServerSocketChannel.open();
			if (BuildCheck.isNougat()) {
				mServerChannel.bind(address);
			} else {
				final ServerSocket socket = mServerChannel.socket();
				socket.bind(address);
			}
			
			mIsRunning = true;
		}
		
		/**
		 * クライアントからの接続待ちループ
 		 */
		private void serverLoop() {
			
			if (DEBUG) Log.v(TAG, "ServerTask#serverLoop:");
			
			for (; mIsRunning ; ) {
				// クライアントからの接続待ち
				try {
					// 接続して来たクライアント毎に別スレッドで処理を行う
					if (DEBUG) Log.v(TAG, "ServerTask#serverLoop:ServerSocket#acceptでブロッキング");
					final SocketChannel clientChannel = mServerChannel.accept();
					if (DEBUG) Log.v(TAG, "ServerTask#serverLoop:ServerSocket#acceptを抜けた");
					final Client client = new Client(SocketChannelDataLink.this, clientChannel);
					add(client);
					if (DEBUG) Log.v(TAG, "ServerTask#serverLoop:クライアント開始");
				} catch (final IOException e) {
					break;
				}
			}
			
			if (DEBUG) Log.v(TAG, "ServerTask#serverLoop:finished");
		}
		
	}
}
