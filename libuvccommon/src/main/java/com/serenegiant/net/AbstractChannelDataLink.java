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
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import com.serenegiant.io.ChannelHelper;
import com.serenegiant.utils.HandlerThreadHandler;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public abstract class AbstractChannelDataLink {
	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = AbstractChannelDataLink.class.getSimpleName();

	private static final Charset UTF8 = Charset.forName("UTF-8");

	public interface Callback {
		public void onConnect(final AbstractClient client);
		public void onDisconnect();
		public void onReceive(final AbstractClient client, @Nullable final Object msg);
		public void onError(final AbstractClient client, final Exception e);
	}
	
	private static final int REQ_RELEASE = -9;
	private static final int TYPE_UNKNOWN = -1;
	private static final int TYPE_NULL = 0;
	private static final int TYPE_BYTE_BUFFER = 1;
	private static final int TYPE_STRING = 2;
	private static final int TYPE_BOOL = 10;
	private static final int TYPE_INT = 11;
	private static final int TYPE_LONG = 12;
	private static final int TYPE_FLOAT = 20;
	private static final int TYPE_DOUBLE = 21;
	private static final int TYPE_BYTE_ARRAY = 30;
	private static final int TYPE_BOOL_ARRAY = 31;
	private static final int TYPE_INT_ARRAY = 32;
	private static final int TYPE_LONG_ARRAY = 33;
	private static final int TYPE_FLOAT_ARRAY = 40;
	private static final int TYPE_DOUBLE_ARRAY = 41;

	private final Set<AbstractClient> mClients = new CopyOnWriteArraySet<AbstractClient>();
	private final Set<Callback> mCallbacks = new CopyOnWriteArraySet<Callback>();

	/**
	 * コンストラクタ
	 */
	public AbstractChannelDataLink() {
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
	}
	
	/**
	 * コンストラクタ
	 * @param callback
	 */
	public AbstractChannelDataLink(final Callback callback) {
		this();
		add(callback);
	}
		
	@Override
	protected void finalize() throws Throwable {
		try {
			release();
		} finally {
			super.finalize();
		}
	}
	
	/**
	 * 接続中のクライアントを全て切断、接続待ちも終了する
	 */
	public void release() {
		if (DEBUG) Log.v(TAG, "release:");
		for (final AbstractClient client: mClients) {
			client.release();
		}
		mClients.clear();
	}

	/**
	 * データ受信時のコールバックを登録
	 * @param callback
	 */
	public void add(final Callback callback) {
		if (DEBUG) Log.v(TAG, "add:callback=" + callback);
		if (callback != null) {
			mCallbacks.add(callback);
		}
	}
	
	/**
	 * データ受信時のコールバックを登録解除
	 * @param callback
	 */
	public void remove(final Callback callback) {
		if (DEBUG) Log.v(TAG, "remove:callback=" + callback);
		mCallbacks.remove(callback);
	}
	
	/**
	 * 受信用のクライアントを追加
	 * @param client
	 */
	protected void add(final AbstractClient client) {
		if (DEBUG) Log.v(TAG, "add:client=" + client);
		if (client != null) {
			mClients.add(client);
		}
	}
	
	/**
	 * 受信用のクライアントを登録解除
	 * @param client
	 */
	public void remove(final AbstractClient client) {
		if (DEBUG) Log.v(TAG, "remove:client=" + client);
		mClients.remove(client);
	}

	/**
	 * 受信スレッドの実行部
	 */
	public static abstract class AbstractClient implements Runnable, Handler.Callback {
		private final WeakReference<AbstractChannelDataLink> mWeakParent;
		protected ByteChannel mChannel;
		private volatile boolean mIsRunning = true;
		private volatile boolean mIsInit;
		/** 送信データをワーカースレッド上で処理するためのHandler */
		private Handler mSenderHandler;
		
		public AbstractClient(@NonNull final AbstractChannelDataLink parent,
			@Nullable final ByteChannel channel) {

			if (DEBUG) Log.v(TAG, "Client#コンストラクタ:channel=" + channel);
			mWeakParent = new WeakReference<AbstractChannelDataLink>(parent);
			mSenderHandler = HandlerThreadHandler.createHandler(this);
			mChannel = channel;
		}
		
		@Override
		protected void finalize() throws Throwable {
			if (DEBUG) Log.v(TAG, "Client#finalize");
			try {
				release(-1);
			} finally {
				super.finalize();
			}
		}
		
		/**
		 * 接続を切断して関係するリソースを開放する
		 * 再利用は出来ない
		 */
		public void release() {
			release(500);
		}
		
		/**
		 * 指定時間後に破棄する
		 * @param delay 0以下なら時間指定は無効
		 */
		public synchronized void release(final long delay) {
			if (DEBUG) Log.v(TAG, "Client#release");
			if (mSenderHandler != null) {
				try {
					if (delay > 0) {
						mSenderHandler.sendEmptyMessageDelayed(REQ_RELEASE, delay);
					} else {
						mSenderHandler.sendEmptyMessage(REQ_RELEASE);
					}
				} catch (final Exception e) {
				}
			}
		}
		
		/**
		 * 実際の開始処理
		 */
		protected void internalStart() {
			if (DEBUG) Log.v(TAG, "Client#internalStart:");
			synchronized (this) {
				new Thread(this).start();
				for (; mIsRunning && !mIsInit ;) {
					try {
						this.wait(300);
					} catch (final InterruptedException e) {
						// ignore
					}
				}
			}
			if (DEBUG) Log.v(TAG, "Client#internalStart:finished");
		}
		
		/**
		 * 実際の破棄処理
		 */
		private void internalRelease() {
			if (DEBUG) Log.v(TAG, "Client#internalRelease:");
			mIsRunning = mIsInit = false;
			ByteChannel channel;
			synchronized (this) {
				channel = mChannel;
				mChannel = null;
				notifyAll();
			}
			if (channel != null) {
				try {
					channel.close();
				} catch (final Exception e) {
					if (DEBUG) Log.w(TAG, e);
				}
			}
			synchronized (this) {
				if (mSenderHandler != null) {
					try {
						mSenderHandler.getLooper().quit();
					} catch (final Exception e) {
						// ignore
					}
					mSenderHandler = null;
				}
			}
			if (DEBUG) Log.v(TAG, "Client#internalRelease:finished");
		}
		
		/**
		 * データを送信
		 * @param value
		 * @throws IOException
		 */
		public void send(final boolean value) throws IOException {
			send(TYPE_BOOL, value);
		}
				
		/**
		 * データを送信
		 * @param value
		 * @throws IOException
		 */
		public synchronized void send(final int value) throws IOException {
			send(TYPE_INT, value);
		}
		
		/**
		 * データを送信
		 * @param value
		 * @throws IOException
		 */
		public  void send(final long value) throws IOException {
			send(TYPE_LONG, value);
		}

		/**
		 * データを送信
		 * @param value
		 * @throws IOException
		 */
		public void send(final float value) throws IOException {
			send(TYPE_FLOAT, value);
		}
		
		/**
		 * データを送信
		 * @param value
		 * @throws IOException
		 */
		public void send(final double value) throws IOException {
			send(TYPE_DOUBLE, value);
		}
		
		/**
		 * データを送信
		 * @param value
		 * @throws IOException
		 */
		public void send(@NonNull final String value) throws IOException {
			send(TYPE_STRING, value);
		}
		
		/**
		 * データを送信
		 * @param values
		 * @throws IOException
		 */
		public void send(@NonNull final byte[] values) throws IOException {
			send(TYPE_BYTE_ARRAY, values);
		}
		
		/**
		 * データを送信
		 * @param values
		 * @throws IOException
		 */
		public void sent(@NonNull final boolean[] values) throws IOException {
			send(TYPE_BOOL_ARRAY, values);
		}
		
		/**
		 * データを送信
		 * @param values
		 * @throws IOException
		 */
		public void send(@NonNull final int[] values) throws IOException {
			send(TYPE_INT_ARRAY, values);
		}

		/**
		 * データを送信
		 * @param values
		 * @throws IOException
		 */
		public void send(@NonNull final long[] values) throws IOException {
			send(TYPE_LONG_ARRAY, values);
		}

		/**
		 * データを送信
		 * @param values
		 * @throws IOException
		 */
		public void send(@NonNull final float[] values) throws IOException {
			send(TYPE_FLOAT_ARRAY, values);
		}

		/**
		 * データを送信
		 * @param values
		 * @throws IOException
		 */
		public void send(@NonNull final double[] values) throws IOException {
			send(TYPE_DOUBLE_ARRAY, values);
		}
		
		/**
		 * データを送信
		 * @param value
		 * @throws IOException
		 */
		public void send(@NonNull final ByteBuffer value) throws IOException {
			send(TYPE_BYTE_BUFFER, value);
		}
		
		/**
		 * データを送信
		 * @param value
		 * @throws IOException
		 */
		public void send(@Nullable final Object value) throws IOException {
			if (value == null) {
				send(TYPE_NULL, null);
			} else if (value instanceof ByteBuffer) {
				send(TYPE_BYTE_BUFFER, value);
			} else if (value instanceof String) {
				send(TYPE_STRING, value);
			} else if (value instanceof CharSequence) {
				send(TYPE_STRING, value.toString());
			} else if (value instanceof Boolean) {
				send(TYPE_BOOL, value);
			} else if (value instanceof Integer) {
				send(TYPE_INT, value);
			} else if (value instanceof Long) {
				send(TYPE_LONG, value);
			} else if (value instanceof Float) {
				send(TYPE_FLOAT, value);
			} else if (value instanceof Double) {
				send(TYPE_DOUBLE, value);
			} else if (value instanceof byte[]) {
				send(TYPE_BYTE_ARRAY, value);
			} else if (value instanceof boolean[]) {
				send(TYPE_BOOL_ARRAY, value);
			} else if (value instanceof int[]) {
				send(TYPE_INT_ARRAY, value);
			} else if (value instanceof long[]) {
				send(TYPE_LONG_ARRAY, value);
			} else if (value instanceof float[]) {
				send(TYPE_FLOAT_ARRAY, value);
			} else if (value instanceof double[]) {
				send(TYPE_DOUBLE_ARRAY, value);
			} else {
				throw new IOException("unknown type of object");
			}
		}
		
		/**
		 * データ送信時のヘルパーメソッド
		 * データ送信用のスレッドのHandlerへ投げる
		 * @param type
		 * @param msg
		 * @throws IOException
		 */
		private synchronized void send(final int type, @Nullable final Object msg) throws IOException {
			if (DEBUG) Log.v(TAG, "Client#send:");
			if ((mSenderHandler == null) || !mIsRunning || !mIsInit) throw new IOException();
			mSenderHandler.sendMessage(mSenderHandler.obtainMessage(type, msg));
		}
		
		/**
		 * 初期化処理, 受信用ワーカースレッド上で実行
		 * @throws IOException
		 */
		protected abstract void init() throws IOException;
		
		protected void setInit(final boolean init) {
			mIsInit = init;
		}
		
		/**
		 * 受信用ワーカースレッドでの処理
		 */
		@Override
		public void run() {
			if (DEBUG) Log.v(TAG, "Client#run:");
			try {
				init();
				callOnConnect();
				doReceiveLoop();
				callOnDisconnect();
			} catch (final Exception e) {
				callOnError(e);
			} finally {
				mIsRunning = false;
				final AbstractChannelDataLink parent = mWeakParent.get();
				if (parent != null) {
					parent.mClients.remove(this);
				}
				release(-1);
			}
			if (DEBUG) Log.v(TAG, "Client#run:finished");
		}
		
		/**
		 * 受信ループ
		 * @throws IOException
		 * @throws ClassNotFoundException
		 */
		private void doReceiveLoop()
			throws IOException, ClassNotFoundException {
			
			if (DEBUG) Log.v(TAG, "Client#doReceiveLoop:");
			
			for (; mIsRunning; ) {
				try {
					// 先頭は種類
					final int type = ChannelHelper.readInt(mChannel);
					if (DEBUG) Log.v(TAG, "Client#doReceiveLoop:type=" + type);
					switch (type) {
					case TYPE_NULL:
						callOnReceive(null);
						break;
					case TYPE_BYTE_BUFFER:
						callOnReceive(ChannelHelper.readByteBuffer(mChannel));
						break;
					case TYPE_BOOL:
						callOnReceive(ChannelHelper.readBoolean(mChannel));
						break;
					case TYPE_INT:
						callOnReceive(ChannelHelper.readInt(mChannel));
						break;
					case TYPE_LONG:
						callOnReceive(ChannelHelper.readLong(mChannel));
						break;
					case TYPE_FLOAT:
						callOnReceive(ChannelHelper.readFloat(mChannel));
						break;
					case TYPE_DOUBLE:
						callOnReceive(ChannelHelper.readDouble(mChannel));
						break;
					case TYPE_STRING:
						callOnReceive(ChannelHelper.readString(mChannel));
						break;
					case TYPE_BYTE_ARRAY:
						callOnReceive(ChannelHelper.readByteArray(mChannel));
						break;
					case TYPE_BOOL_ARRAY:
						callOnReceive(ChannelHelper.readBooleanArray(mChannel));
						break;
					case TYPE_INT_ARRAY:
						callOnReceive(ChannelHelper.readIntArray(mChannel));
						break;
					case TYPE_LONG_ARRAY:
						callOnReceive(ChannelHelper.readLongArray(mChannel));
						break;
					case TYPE_FLOAT_ARRAY:
						callOnReceive(ChannelHelper.readFloatArray(mChannel));
						break;
					case TYPE_DOUBLE_ARRAY:
						callOnReceive(ChannelHelper.readDoubleArray(mChannel));
						break;
					}
				} catch (final SocketException | ClosedChannelException e) {
					break;
				} catch (final IOException e) {
//					if (DEBUG) Log.w(TAG, e);
					break;
				}
			}
			
			if (DEBUG) Log.v(TAG, "Client#doReceiveLoop:finished");
		}
		
		protected void callOnConnect() {
			if (DEBUG) Log.v(TAG, "callOnConnect:");
			final AbstractChannelDataLink parent = mWeakParent.get();
			if (parent != null) {
				for (final Callback callback : parent.mCallbacks) {
					try {
						callback.onConnect(this);
					} catch (final Exception e) {
						parent.mCallbacks.remove(callback);
					}
				}
			}
		}

		protected void callOnDisconnect() {
			if (DEBUG) Log.v(TAG, "callOnDisconnect:");
			final AbstractChannelDataLink parent = mWeakParent.get();
			if (parent != null) {
				for (final Callback callback : parent.mCallbacks) {
					try {
						callback.onDisconnect();
					} catch (final Exception e) {
						parent.mCallbacks.remove(callback);
					}
				}
			}
		}
		
		protected void callOnReceive(@Nullable final Object msg) {
			if (DEBUG) Log.v(TAG, "callOnReceive:msg=" + msg);
			final AbstractChannelDataLink parent = mWeakParent.get();
			if (parent != null) {
				for (final Callback callback : parent.mCallbacks) {
					try {
						callback.onReceive(this, msg);
					} catch (final Exception e) {
						parent.mCallbacks.remove(callback);
					}
				}
			}
		}
		
		protected void callOnError(final Exception err) {
			if (DEBUG) Log.v(TAG, "callOnError:");
			final AbstractChannelDataLink parent = mWeakParent.get();
			if (parent != null) {
				for (final Callback callback : parent.mCallbacks) {
					try {
						callback.onError(this, err);
					} catch (final Exception e) {
						parent.mCallbacks.remove(callback);
					}
				}
			}
		}
		
		@Override
		public synchronized boolean handleMessage(final Message msg) {
			if (!mIsRunning || (mChannel == null)) return false;
			if (DEBUG) Log.v(TAG, "handleMessage:msg=" + msg);
			try {
				// 内部コマンド
				switch (msg.what) {
				case REQ_RELEASE:
					internalRelease();
					return true;
				}
				// データ送信
				switch (msg.what) {
				case TYPE_NULL:
					ChannelHelper.write(mChannel, TYPE_NULL);
					return true;
				case TYPE_BYTE_BUFFER:
					if (msg.obj instanceof ByteBuffer) {
						ChannelHelper.write(mChannel, TYPE_BYTE_BUFFER);
						ChannelHelper.write(mChannel, (ByteBuffer)msg.obj);
					}
					return true;
				case TYPE_BOOL:
					if (msg.obj instanceof Boolean) {
						ChannelHelper.write(mChannel, TYPE_BOOL);
						ChannelHelper.write(mChannel, (boolean)msg.obj);
						return true;
					}
					break;
				case TYPE_INT:
					if (msg.obj instanceof Integer) {
						ChannelHelper.write(mChannel, TYPE_INT);
						ChannelHelper.write(mChannel, (int)msg.obj);
						return true;
					}
					break;
				case TYPE_LONG:
					if (msg.obj instanceof Long) {
						ChannelHelper.write(mChannel, TYPE_LONG);
						ChannelHelper.write(mChannel, (long)msg.obj);
						return true;
					}
					break;
				case TYPE_FLOAT:
					if (msg.obj instanceof Float) {
						ChannelHelper.write(mChannel, TYPE_FLOAT);
						ChannelHelper.write(mChannel, (float)msg.obj);
						return true;
					}
					break;
				case TYPE_DOUBLE:
					if (msg.obj instanceof Double) {
						ChannelHelper.write(mChannel, TYPE_DOUBLE);
						ChannelHelper.write(mChannel, (double)msg.obj);
						return true;
					}
					break;
				case TYPE_STRING:
					if (msg.obj instanceof String) {
						ChannelHelper.write(mChannel, TYPE_STRING);
						ChannelHelper.write(mChannel, (String)msg.obj);
						return true;
					}
					break;
				case TYPE_BYTE_ARRAY:
					if (msg.obj instanceof byte[]) {
						ChannelHelper.write(mChannel, TYPE_BYTE_ARRAY);
						ChannelHelper.write(mChannel, (byte[])msg.obj);
						return true;
					}
					break;
				case TYPE_BOOL_ARRAY:
					if (msg.obj instanceof boolean[]) {
						ChannelHelper.write(mChannel, TYPE_BOOL_ARRAY);
						ChannelHelper.write(mChannel, (boolean[])msg.obj);
						return true;
					}
					break;
				case TYPE_INT_ARRAY:
					if (msg.obj instanceof int[]) {
						ChannelHelper.write(mChannel, TYPE_INT_ARRAY);
						ChannelHelper.write(mChannel, (int[])msg.obj);
						return true;
					}
					break;
				case TYPE_LONG_ARRAY:
					if (msg.obj instanceof long[]) {
						ChannelHelper.write(mChannel, TYPE_LONG_ARRAY);
						ChannelHelper.write(mChannel, (long[])msg.obj);
						return true;
					}
					break;
				case TYPE_FLOAT_ARRAY:
					if (msg.obj instanceof float[]) {
						ChannelHelper.write(mChannel, TYPE_FLOAT_ARRAY);
						ChannelHelper.write(mChannel, (float[])msg.obj);
						return true;
					}
					break;
				case TYPE_DOUBLE_ARRAY:
					if (msg.obj instanceof double[]) {
						ChannelHelper.write(mChannel, TYPE_DOUBLE_ARRAY);
						ChannelHelper.write(mChannel, (double[])msg.obj);
						return true;
					}
					break;
				}
			} catch (final SocketException e) {
				if (DEBUG) Log.w(TAG, e);
			} catch (final IOException e) {
				callOnError(e);
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
			
			return false;
		}
		
	}

}
