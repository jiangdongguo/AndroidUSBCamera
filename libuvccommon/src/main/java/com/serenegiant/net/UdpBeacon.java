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

import android.os.Handler;
import android.os.SystemClock;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import com.serenegiant.utils.HandlerThreadHandler;

import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * LAN内でのアドレス解決の為にUDPでビーコンをブロードキャストするためのクラス
 */
public class UdpBeacon {
//	private static final boolean DEBUG = BuildConfig.DEBUG && false;
	private static final String TAG = "UdpBeacon"; // UdpBeacon.class.getSimpleName();

	private static final int BEACON_UDP_PORT = 9999;
	private static final byte BEACON_VERSION = 0x01;
	public static final int BEACON_SIZE = 23;
	private static final long DEFAULT_BEACON_SEND_INTERVALS_MS = 3000;
	private static final Charset CHARSET = Charset.forName("UTF-8");
	/** ソケットのタイムアウト[ミリ秒] */
	private static final int SO_TIMEOUT_MS = 2000;

	/**
	 * ビーコン受信時のコールバック　FIXME extraを渡せるようにする
	 */
	public interface UdpBeaconCallback {
		/**
		 * ビーコンを受信した時の処理
		 * @param uuid 受信したビーコンのUUID
		 * @param remote ビーコンのアドレス文字列
		 * @param remote_port ビーコンのポート番号
		 */
		public void onReceiveBeacon(final UUID uuid, final String remote, final int remote_port);
		/**
		 * エラー発生時の処理
		 * @param e
		 */
		public void onError(final Exception e);
	}

	/**
	 * S A K I     4 bytes
	 * version     1 byte, %x01
	 * UUID        16 bytes
	 * port        2 bytes in network order = big endian = same as usual byte order of Java
	 * extra len   1 byte, optional
	 * extra       1-255 bytes, optional
	 */
	private static class Beacon {
		public static final String BEACON_IDENTITY = "SAKI";

		private final UUID uuid;
		private final int listenPort;
		private final int extraBytes;
		private final ByteBuffer extras;
		public Beacon(final ByteBuffer buffer) {
			uuid = new UUID(buffer.getLong(), buffer.getLong());
			final int port = buffer.getShort();
			listenPort = port < 0 ? (0xffff) & port : port;
			if (buffer.remaining() > 1) {
				// length(1byte) + extra data(max 255 bytes)
				extraBytes = buffer.get() & 0xff;
				if (extraBytes > 0) {
					extras = ByteBuffer.allocateDirect(extraBytes);
					extras.put(buffer);
					extras.flip();
				} else {
					extras = null;
				}
			} else {
				extraBytes = 0;
				extras = null;
			}
		}

		public Beacon(final UUID uuid, final int port) {
			this(uuid, port, 0);
		}

		public Beacon(final UUID uuid, final int port, final int extras) {
			this.uuid = uuid;
			listenPort = port;
			extraBytes = extras & 0xff;
			if (extraBytes > 0) {
				this.extras = ByteBuffer.allocateDirect(extraBytes);
			} else {
				this.extras = null;
			}
		}

		public byte[] asBytes() {
			final byte[] bytes = new byte[BEACON_SIZE + (extraBytes > 0 ? extraBytes + 1 : 0)];
			final ByteBuffer buffer = ByteBuffer.wrap(bytes);
			buffer.put(BEACON_IDENTITY.getBytes());
			buffer.put(BEACON_VERSION);
			buffer.putLong(uuid.getMostSignificantBits());
			buffer.putLong(uuid.getLeastSignificantBits());
			buffer.putShort((short) listenPort);
			if (extraBytes > 0) {
				buffer.put((byte)extraBytes);
				extras.clear();
				extras.flip();
				buffer.put(extras);
			}
			buffer.flip();
			return bytes;
		}

		public ByteBuffer extra() {
			return extras;
		}

		public void extra(final byte[] extra) {
			extra(ByteBuffer.wrap(extra));
		}

		public void extra(final ByteBuffer extra) {
			final int n = extra != null ? extra.remaining() : -1;
			if ((extraBytes > 0) && (extraBytes <= n)) {
				extras.clear();
				extras.put(extra);
				extras.flip();
			}
		}

		public String toString() {
			return String.format(Locale.US, "Beacon(%s,port=%d,extra=%d)", uuid.toString(), listenPort, extraBytes);
		}
	}

	private final Object mSync = new Object();
	private final CopyOnWriteArraySet<UdpBeaconCallback> mCallbacks = new CopyOnWriteArraySet<UdpBeaconCallback>();
	private Handler mAsyncHandler;
	private final UUID uuid;
	private final byte[] beaconBytes;
	private final long mBeaconIntervalsMs;
	private final long mRcvMinIntervalsMs;
	private Thread mBeaconThread;
	private boolean mReceiveOnly;
	private volatile boolean mIsRunning;
	private volatile boolean mReleased;

	/**
	 * コンストラクタ
	 * ビーコンポート番号は9999
	 * ビーコン送信周期は3000ミリ秒
	 * @param callback
	 */
	public UdpBeacon(@Nullable final UdpBeaconCallback callback) {
		this(callback, BEACON_UDP_PORT, DEFAULT_BEACON_SEND_INTERVALS_MS, false, 0);
	}

	/**
	 * コンストラクタ
	 * ビーコンポート番号は9999
	 * @param callback
	 * @param beacon_intervals_ms ビーコン送信周期[ミリ秒]
	 */
	public UdpBeacon(@Nullable final UdpBeaconCallback callback, final long beacon_intervals_ms) {
		this(callback, BEACON_UDP_PORT, beacon_intervals_ms, false, 0);
	}

	/**
	 * コンストラクタ
	 * ビーコンポート番号は9999
	 * ビーコン送信周期は3000ミリ秒
	 * @param callback
	 * @param receiveOnly ビーコンを送信せずに受信だけ行うかどうか, true:ビーコン送信しない
	 */
	public UdpBeacon(@Nullable final UdpBeaconCallback callback, final boolean receiveOnly) {
		this(callback, BEACON_UDP_PORT, DEFAULT_BEACON_SEND_INTERVALS_MS, false, 0);
	}

	/**
	 * コンストラクタ
	 * ビーコンポート番号は9999
	 * ビーコン送信周期は3000ミリ秒
	 * @param callback
	 * @param receiveOnly ビーコンを送信せずに受信だけ行うかどうか, true:ビーコン送信しない
	 * @param rcv_min_intervals_ms 最小受信間隔[ミリ秒]
	 */
	public UdpBeacon(@Nullable final UdpBeaconCallback callback,
		final boolean receiveOnly, final long rcv_min_intervals_ms) {
		
		this(callback, BEACON_UDP_PORT, DEFAULT_BEACON_SEND_INTERVALS_MS,
			false, rcv_min_intervals_ms);
	}

	/**
	 * コンストラクタ
	 * ビーコンポート番号は9999
	 * @param callback
	 * @param beacon_intervals_ms
	 * @param receiveOnly ビーコンを送信せずに受信だけ行うかどうか, true:ビーコン送信しない
	 */
	public UdpBeacon(@Nullable final UdpBeaconCallback callback,
		final long beacon_intervals_ms, final boolean receiveOnly) {
		
		this(callback, BEACON_UDP_PORT, beacon_intervals_ms, receiveOnly, 0);
	}

	/**
	 * コンストラクタ
	 * ビーコンポート番号は9999
	 * @param callback
	 * @param beacon_intervals_ms
	 * @param receiveOnly ビーコンを送信せずに受信だけ行うかどうか, true:ビーコン送信しない
	 * @param rcv_min_intervals_ms 最小受信間隔[ミリ秒]
	 */
	public UdpBeacon(@Nullable final UdpBeaconCallback callback,
		final long beacon_intervals_ms, final boolean receiveOnly,
		final long rcv_min_intervals_ms) {
		
		this(callback, BEACON_UDP_PORT, beacon_intervals_ms,
			receiveOnly, rcv_min_intervals_ms);
	}

	/**
	 * コンストラクタ
	 * @param callback
	 * @param port ビーコン用のポート番号
	 * @param beacon_intervals_ms ビーコン送信周期[ミリ秒], receiveOnly=trueなら無効
	 * @param receiveOnly ビーコンを送信せずに受信だけ行うかどうか, true:ビーコン送信しない
	 * @param rcv_min_intervals_ms 最小受信間隔[ミリ秒]
	 */
	public UdpBeacon(@Nullable final UdpBeaconCallback callback, final int port,
		final long beacon_intervals_ms, final boolean receiveOnly,
		final long rcv_min_intervals_ms) {
		
//		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		if (callback != null) {
			mCallbacks.add(callback);
		}
		mAsyncHandler = HandlerThreadHandler.createHandler("UdpBeaconAsync");
		uuid = UUID.randomUUID();
		final Beacon beacon = new Beacon(uuid, port);
		beaconBytes = beacon.asBytes();
		mBeaconIntervalsMs = beacon_intervals_ms;
		mReceiveOnly = receiveOnly;
		mRcvMinIntervalsMs = rcv_min_intervals_ms;
	}

	public void finalize() throws Throwable {
		try {
			release();
		} finally {
			super.finalize();
		}
	}

	/**
	 * ビーコンの送信・受信を停止して関係するリソースを破棄する, 再利用は出来ない
	 */
	public void release() {
//		if (DEBUG) Log.v(TAG, "release:");
		if (!mReleased) {
			mReleased = true;
			stop();
			mCallbacks.clear();
			synchronized (mSync) {
				if (mAsyncHandler != null) {
					try {
						mAsyncHandler.getLooper().quit();
					} catch (final Exception e) {
						// ignore
					}
					mAsyncHandler = null;
				}
			}
		}
	}

	public void addCallback(final UdpBeaconCallback callback) {
		if (callback != null) {
			mCallbacks.add(callback);
		}
	}

	public void removeCallback(final UdpBeaconCallback callback) {
		mCallbacks.remove(callback);
	}

	/**
	 * ビーコンの送信(receiveOnly=falseの時のみ)・受信を開始する
	 * @throws IllegalStateException 既に破棄されている
	 */
	public void start() {
//		if (DEBUG) Log.v(TAG, "start:");
		checkReleased();
		synchronized (mSync) {
			if (mBeaconThread == null) {
				mIsRunning = true;
				mBeaconThread = new Thread(mBeaconTask, "UdpBeaconTask");
				mBeaconThread.start();
			}
		}
	}

	/**
	 * ビーコンの送受信を停止する
	 */
	public void stop() {
		mIsRunning = false;
		Thread thread;
		synchronized (mSync) {
			thread = mBeaconThread;
			mBeaconThread = null;
			mSync.notifyAll();
		}
		if ((thread != null) && thread.isAlive()) {
			thread.interrupt();
			try {
				thread.join();
			} catch (final Exception e) {
				Log.d(TAG, e.getMessage());
			}
		}
	}

	/**
	 * 1回だけビーコンを送信
	 * @throws IllegalStateException 既に破棄されている
	 */
	public void shot() throws IllegalStateException {
		shot(1);
	}

	/**
	 * 指定回数だけビーコンを送信
	 * @throws IllegalStateException 既に破棄されている
	 */
	public void shot(final int n) throws IllegalStateException {
		checkReleased();
		synchronized (mSync) {
			new Thread(new BeaconShotTask(n), "UdpOneShotBeaconTask").start();
		}
	}

	/**
	 * ビーコン送受信中かどうか
	 * @return
	 */
	public boolean isActive() {
		return mIsRunning;
	}

	/**
	 * ビーコンを送信せずに受信だけ行うかどうかをセット
	 * ビーコン送受信中には変更できない。stopしてから呼ぶこと
	 * @param receiveOnly
	 * @throws IllegalStateException 破棄済みまたはビーコン送受信中ならIllegalStateExceptionを投げる
	 */
	public void setReceiveOnly(final boolean receiveOnly) throws IllegalStateException {
		checkReleased();
		synchronized (mSync) {
			if (mIsRunning) {
				throw new IllegalStateException("beacon is already active");
			}
			mReceiveOnly = receiveOnly;
		}
	}

	/**
	 * ビーコンを送信せずに受信だけ行うかどうか
	 * @return
	 */
	public boolean isReceiveOnly() {
		return mReceiveOnly;
	}

	/**
	 * 既に破棄されているかどうかをチェックして破棄済みならIllegalStateExceptionを投げる
	 * @throws IllegalStateException
	 */
	private void checkReleased() throws IllegalStateException {
		if (mReleased) {
			throw new IllegalStateException("already released");
		}
	}

	private final void callOnError(final Exception e) {
		if (mReleased) {
			Log.w(TAG, e);
			return;
		}
		synchronized (mSync) {
			if (mAsyncHandler != null) {
				mAsyncHandler.post(new Runnable() {
					@Override
					public void run() {
						for (final UdpBeaconCallback callback: mCallbacks) {
							try {
								callback.onError(e);
							} catch (final Exception e) {
								mCallbacks.remove(callback);
								Log.w(TAG, e);
							}
						}
					}
				});
			}
		}
	}
	
	/**
	 * 例外生成せずに一定時間待機する
	 * @param wait_time_ms
	 * @return true: インターラプトされた時
	 */
	private boolean waitWithoutException(final Object sync, final long wait_time_ms) {
		boolean result = false;
		synchronized (sync) {
			try {
				sync.wait(wait_time_ms);
			} catch (final InterruptedException e) {
				result = true;
			}
		}
		return result;
	}
	
	private final class BeaconShotTask implements Runnable {
		private final int shotNums;

		public BeaconShotTask(final int shotNums) {
			this.shotNums = shotNums;
		}

		@Override
		public void run() {
			try {
				final UdpSocket socket = new UdpSocket(BEACON_UDP_PORT);
				socket.setReuseAddress(true);		// 他のソケットでも同じアドレスを利用可能にする
				socket.setSoTimeout(SO_TIMEOUT_MS);	// タイムアウト
				try {
					for (int i = 0; i < shotNums; i++) {
						if (mReleased) break;
						sendBeacon(socket);
						if (waitWithoutException(this, mBeaconIntervalsMs)) {
							break;
						}
					}
				} finally {
					socket.release();
				}
			} catch (final SocketException e) {
				callOnError(e);
			}
		}
	}
	
	/**
	 * ビーコンの送信スレッド
	 */
	private final Runnable mBeaconTask = new Runnable() {
		@Override
		public void run() {
			final ByteBuffer buffer = ByteBuffer.allocateDirect(256);
			try {
				final UdpSocket socket = new UdpSocket(BEACON_UDP_PORT);
				socket.setReceiveBufferSize(256);
				socket.setReuseAddress(true);		// 他のソケットでも同じアドレスを利用可能にする
				socket.setSoTimeout(SO_TIMEOUT_MS);	// タイムアウト
				final Thread rcvThread = new Thread(new ReceiverTask(socket));
				rcvThread.start();
				long next_send = SystemClock.elapsedRealtime();
				try {
					for ( ; mIsRunning && !mReleased ; ) {
						if (!mReceiveOnly) {
							// 受信のみでなければ指定時間毎にビーコン送信
							final long t = next_send - SystemClock.elapsedRealtime();
							if (!mReceiveOnly && (t <= 0)) {
								next_send = SystemClock.elapsedRealtime() + mBeaconIntervalsMs;
								sendBeacon(socket);
							} else if (waitWithoutException(this, t)) {	// 残り時間を待機
								break;
							}
						} else if (waitWithoutException(this, SO_TIMEOUT_MS)) {	// ソケットタイムアウト時間待機
							break;
						}
					}
				} finally {
					mIsRunning = false;
					socket.release();
					try {
						rcvThread.interrupt();
					} catch (final Exception e) {
						Log.w(TAG, e);
					}
				}
			} catch (final Exception e) {
				callOnError(e);
			}
			mIsRunning = false;
			synchronized (mSync) {
				mBeaconThread = null;
			}
		}
	};
	
	/**
	 * ビーコンの受信スレッド
	 */
	private class ReceiverTask implements Runnable {
		private final UdpSocket mUdpSocket;
		private ReceiverTask(@NonNull final UdpSocket udpSocket) {
			mUdpSocket = udpSocket;
		}
		
		@Override
		public void run() {
			final ByteBuffer buffer = ByteBuffer.allocateDirect(256);
			final UdpSocket socket = mUdpSocket;
			long next_rcv = SystemClock.elapsedRealtime();
			for ( ; mIsRunning && !mReleased ; ) {
				if (mRcvMinIntervalsMs > 0) {
					final long t = next_rcv - SystemClock.elapsedRealtime();
					if (t > 0) {
						if (waitWithoutException(this, t)) {
							break;
						}
					}
					next_rcv = SystemClock.elapsedRealtime() + mRcvMinIntervalsMs;
				}
				// ゲスト端末からのブロードキャストを受け取る,
				// 受け取るまでは待ち状態になる...けどタイムアウトで抜けてくる
				// UDPなので同一ネットワークのUDPパケットはみんな受け取ってしまうので
				// 端末によっては負荷が高くなることがある。
				// できるだけ速く処理すること!
				try {
					buffer.clear();
					final int length = socket.receive(buffer);
					if (!mIsRunning) break;
					buffer.rewind();
					if (length == BEACON_SIZE) {
						if (buffer.get() != 'S'
							|| buffer.get() != 'A'
							|| buffer.get() != 'K'
							|| buffer.get() != 'I'
							|| buffer.get() != BEACON_VERSION) {
							continue;
						}
						final Beacon remote_beacon = new Beacon(buffer);
						if (!uuid.equals(remote_beacon.uuid)) {
							// 自分のuuidと違う時
							final String remoteAddr = socket.remote();
							final int remotePort = socket.remotePort();
							synchronized (mSync) {
								if (mAsyncHandler == null) break;
								mAsyncHandler.post(new Runnable() {
									@Override
									public void run() {
										for (final UdpBeaconCallback callback: mCallbacks) {
											try {
												callback.onReceiveBeacon(remote_beacon.uuid, remoteAddr, remotePort);
											} catch (final Exception e) {
												mCallbacks.remove(callback);
												Log.w(TAG, e);
											}
										}
									}
								});
							}
						}
					}
				} catch (final ClosedChannelException e) {
					// ソケットが閉じられた時
					break;
				} catch (final IOException e) {
					// タイムアウトで抜けてきた時, 無視する
				} catch (final IllegalStateException e) {
					break;
				} catch (final Exception e) {
					Log.w(TAG, e);
					break;
				}
			}
		}
	}

	/**
	 * UDPでビーコンをブロードキャストする
	 */
	private void sendBeacon(final UdpSocket socket) {
//		if (DEBUG) Log.v(TAG, "sendBeacon");
		try {
			socket.broadcast(beaconBytes);
		} catch (final IOException e) {
			Log.w(TAG, e);
		}
	}

}
