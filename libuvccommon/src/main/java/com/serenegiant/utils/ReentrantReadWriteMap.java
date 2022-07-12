package com.serenegiant.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
public class ReentrantReadWriteMap<K, V> {
	private final ReentrantReadWriteLock mSensorLock = new ReentrantReadWriteLock();
	private final Lock mReadLock = mSensorLock.readLock();
	private final Lock mWriteLock = mSensorLock.writeLock();
	/** hold key/value pairs */
	private final Map<K, V> mMap = new HashMap<K, V>();

	@Nullable
	public V get(@NonNull final K key) {
		mReadLock.lock();
		try {
			return mMap.containsKey(key) ? mMap.get(key) : null;
		} finally {
			mReadLock.unlock();
		}
	}

	@Nullable
	public V tryGet(@NonNull final K key) {
		if (mReadLock.tryLock()) {
			try {
				return mMap.containsKey(key) ? mMap.get(key) : null;
			} finally {
				mReadLock.unlock();
			}
		}
		return null;
	}

	/**
	 * put specific value into this map
	 * @param key
	 * @param value
	 * @return the previous value associated with key or null if no value mapped.
	 */
	public V put(@NonNull final K key, @NonNull final V value) {
		V prev;
		mWriteLock.lock();
		try {
			prev = mMap.remove(key);
			mMap.put(key, value);
		} finally {
			mWriteLock.unlock();
		}
		return prev;
	}

	/**
	 * If the specified key is not already associated with a value (or is mapped to null)
	 * associates it with the given value and returns null, else returns the current value.
	 * @param key
	 * @param value
	 * @return
	 */
	public V putIfAbsent(final K key, final V value) {
		V v;
		mWriteLock.lock();
		try {
			v = mMap.get(key);
			if (v == null) {
				 v = mMap.put(key, value);
			}
		} finally {
			mWriteLock.unlock();
		}
		return v;
	}

	public void putAll(@NonNull final Map<? extends K, ? extends V> map) {
		mWriteLock.lock();
		try {
			mMap.putAll(map);
		} finally {
			mWriteLock.unlock();
		}
	}

	public V remove(@NonNull final K key) {
		mWriteLock.lock();
		try {
			return mMap.remove(key);
		} finally {
			mWriteLock.unlock();
		}
	}

	/**
	 * Removes the entry for the specified key only if it is currently mapped to the specified value.
	 * @param key
	 * @param value
	 * @return specific removed value or null if no mapping existed
	 */
	public V remove(@NonNull final K key, final V value) {
		V v = null;
		mWriteLock.lock();
		try {
			if (mMap.containsKey(key) && isEquals(mMap.get(key), value)) {
				v = mMap.remove(key);
			}
		} finally {
			mWriteLock.unlock();
		}
		return v;
	}

	public Collection<V> removeAll() {
		final Collection<V> result = new ArrayList<V>();
		mWriteLock.lock();
		try {
			result.addAll(mMap.values());
			mMap.clear();
		} finally {
			mWriteLock.unlock();
		}
		return result;
	}

	public void clear() {
		mWriteLock.lock();
		try {
			mMap.clear();
		} finally {
			mWriteLock.unlock();
		}
	}

	public int size() {
		mReadLock.lock();
		try {
			return mMap.size();
		} finally {
			mReadLock.unlock();
		}
	}

	public boolean containsKey(final K key) {
		mReadLock.lock();
		try {
			return mMap.containsKey(key);
		} finally {
			mReadLock.unlock();
		}
	}

	public boolean containsValue(final Object value) {
		mReadLock.lock();
		try {
			return mMap.containsValue(value);
		} finally {
			mReadLock.unlock();
		}
	}

	public V getOrDefault(final K key, @Nullable final V defaultValue) {
		mReadLock.lock();
		try {
			return mMap.containsKey(key) ? mMap.get(key) : defaultValue;
		} finally {
			mReadLock.unlock();
		}
	}

	public boolean isEmpty() {
		mReadLock.lock();
		try {
			return mMap.isEmpty();
		} finally {
			mReadLock.unlock();
		}
	}

	/**
	 * return copy of keys
	 * @return
	 */
	@NonNull
	public Collection<K> keys() {
		final Collection<K> result = new ArrayList<K>();
		mReadLock.lock();
		try {
			result.addAll(mMap.keySet());
		} finally {
			mReadLock.unlock();
		}
		return result;
	}

	/**
	 * return copy of mapped values
	 * @return
	 */
	@NonNull
	public Collection<V> values() {
		final Collection<V> result = new ArrayList<V>();
		mReadLock.lock();
		try {
			if (!mMap.isEmpty()) {
				result.addAll(mMap.values());
			}
		} finally {
			mReadLock.unlock();
		}
		return result;
	}

	/**
	 * return copy of entries
	 * @return
	 */
	@NonNull
	public Set<Map.Entry<K, V>> entrySet() {
		final Set<Map.Entry<K, V>> result = new HashSet<>();
		mReadLock.lock();
		try {
			result.addAll(mMap.entrySet());
		} finally {
			mReadLock.unlock();
		}
		return result;
	}
//================================================================================
	private static final boolean isEquals(final Object a, final Object b) {
		return (a == b) || (a != null && a.equals(b));
	}

	/**
	 * lock for read access,
	 * never forget to call #readUnlock
	 */
	protected void readLock() {
		mReadLock.lock();
	}

	/**
	 * unlock read access
	 */
	protected void readUnlock() {
		mReadLock.unlock();
	}

	/**
	 * lock for write access
	 * never forget to call writeUnlock
	 */
	protected void writeLock() {
		mWriteLock.lock();
	}

	/**
	 * unlock write access
	 */
	protected void writeUnlock() {
		mWriteLock.unlock();
	}

	/**
	 * get underlying Collection of values
	 * call this between #readLock - #readUnlock or #writeLock - #writeUnlock
	 * @return
	 */
	protected Collection<V> valuesLocked() {
		return mMap.values();
	}

	/**
	 * get underlying Set of keys
	 * call this between #readLock - #readUnlock or #writeLock - #writeUnlock
	 * @return
	 */
	protected Set<K> keysLocked() {
		return mMap.keySet();
	}

	/**
	 * get underlying Map of key-value pairs
	 * call this between #readLock - #readUnlock or #writeLock - #writeUnlock
	 * @return
	 */
	protected Map<K, V> mapLocked() {
		return mMap;
	}
}
