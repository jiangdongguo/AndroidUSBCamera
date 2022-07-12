package com.serenegiant.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
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
public class ReentrantReadWriteList<V> implements List<V> {
	private final ReentrantReadWriteLock mSensorLock = new ReentrantReadWriteLock();
	private final Lock mReadLock = mSensorLock.readLock();
	private final Lock mWriteLock = mSensorLock.writeLock();
	/** hold key/value pairs */
	private final List<V> mList = new ArrayList<V>();
	
	/**
	 *
	 * @param ix
	 * @return
	 * @throws IndexOutOfBoundsException
	 */
	@Nullable
	public V get(final int ix) throws IndexOutOfBoundsException {
		mReadLock.lock();
		try {
			return mList.get(ix);
		} finally {
			mReadLock.unlock();
		}
	}
	
	@Nullable
	public V tryGet(final int ix) {
		if (mReadLock.tryLock()) {
			try {
				return ix >= 0 && ix < mList.size() ? mList.get(ix) : null;
			} finally {
				mReadLock.unlock();
			}
		}
		return null;
	}

	/**
	 *
	 * @param ix
	 * @param value
	 * @return
	 * @throws IndexOutOfBoundsException
	 */
	@Nullable
	@Override
	public V set(final int ix, final V value) throws IndexOutOfBoundsException {
		mWriteLock.lock();
		try {
			return mList.set(ix, value);
		} finally {
			mWriteLock.unlock();
		}
	}
	
	/**
	 *
	 * @param ix
	 * @param value
	 * @throws IndexOutOfBoundsException
	 */
	@Override
	public void add(final int ix, final V value) throws IndexOutOfBoundsException {
		mWriteLock.lock();
		try {
			mList.add(ix, value);
		} finally {
			mWriteLock.unlock();
		}
	}
	
	@Override
	public V remove(final int ix) {
		mWriteLock.lock();
		try {
			return mList.remove(ix);
		} finally {
			mWriteLock.unlock();
		}
	}
	
	@Override
	public int indexOf(final Object o) {
		mReadLock.lock();
		try {
			return mList.indexOf(o);
		} finally {
			mReadLock.unlock();
		}
	}
	
	@Override
	public int lastIndexOf(final Object o) {
		mReadLock.lock();
		try {
			return mList.lastIndexOf(o);
		} finally {
			mReadLock.unlock();
		}
	}
	
	/**
	 * can not modify underlying list using returned iterator
	 * @return
	 */
	@NonNull
	@Override
	public ListIterator<V> listIterator() {
		mReadLock.lock();
		try {
			return Collections.unmodifiableList(mList).listIterator();
		} finally {
			mReadLock.unlock();
		}
	}
	
	@NonNull
	@Override
	public ListIterator<V> listIterator(final int ix) {
		mReadLock.lock();
		try {
			return Collections.unmodifiableList(mList).listIterator(ix);
		} finally {
			mReadLock.unlock();
		}
	}
	
	/**
	 * can not modify underlying list
	 * @param fromIx
	 * @param toIx
	 * @return
	 */
	@NonNull
	@Override
	public List<V> subList(final int fromIx, final int toIx) {
		List<V> result;
		mReadLock.lock();
		try {
			result = Collections.unmodifiableList(mList).subList(fromIx, toIx);
		} finally {
			mReadLock.unlock();
		}
		return result;
	}
	
	/**
	 * put specific value into this list
	 * @param value
	 * @return
	 */
	@Override
	public boolean add(@NonNull final V value) {
		Boolean result;
		mWriteLock.lock();
		try {
			result = mList.add(value);
		} finally {
			mWriteLock.unlock();
		}
		return result;
	}
	
	@Override
	public boolean remove(final Object value) {
		Boolean result;
		mWriteLock.lock();
		try {
			result = mList.remove(value);
		} finally {
			mWriteLock.unlock();
		}
		return result;
	}
	
	@Override
	public boolean containsAll(@NonNull final Collection<?> collection) {
		boolean result;
		mReadLock.lock();
		try {
			result = mList.containsAll(collection);
		} finally {
			mReadLock.unlock();
		}
		return result;
	}
	
	@Override
	public boolean addAll(@NonNull final Collection<? extends V> collection) {
		Boolean result;
		mWriteLock.lock();
		try {
			result = mList.addAll(collection);
		} finally {
			mWriteLock.unlock();
		}
		return result;
	}
	
	@Override
	public boolean addAll(final int ix, @NonNull final Collection<? extends V> collection) {
		Boolean result;
		mWriteLock.lock();
		try {
			result = mList.addAll(ix, collection);
		} finally {
			mWriteLock.unlock();
		}
		return result;
	}
	
	@Override
	public boolean removeAll(@NonNull final Collection<?> collection) {
		Boolean result;
		mWriteLock.lock();
		try {
			result = mList.removeAll(collection);
		} finally {
			mWriteLock.unlock();
		}
		return result;
	}
	
	@Override
	public boolean retainAll(@NonNull final Collection<?> collection) {
		Boolean result;
		mWriteLock.lock();
		try {
			result = mList.retainAll(collection);
		} finally {
			mWriteLock.unlock();
		}
		return result;
	}
	
	/**
	 * If the specified value does not exist in this list add it and return true
	 * otherwise return false
	 * @param value
	 * @return
	 */
	public boolean addIfAbsent(final V value) {
		boolean result;
		mWriteLock.lock();
		try {
			result = !mList.contains(value);
			if (!result) {
				mList.add(value);
			}
		} finally {
			mWriteLock.unlock();
		}
		return result;
	}

	@Override
	public void clear() {
		mWriteLock.lock();
		try {
			mList.clear();
		} finally {
			mWriteLock.unlock();
		}
	}

	@Override
	public int size() {
		mReadLock.lock();
		try {
			return mList.size();
		} finally {
			mReadLock.unlock();
		}
	}

	@Override
	public boolean contains(final Object value) {
		mReadLock.lock();
		try {
			return mList.contains(value);
		} finally {
			mReadLock.unlock();
		}
	}

	@Override
	public boolean isEmpty() {
		mReadLock.lock();
		try {
			return mList.isEmpty();
		} finally {
			mReadLock.unlock();
		}
	}
	
	/**
	 * can not modify this list using this iterator
	 * @return
	 */
	@NonNull
	@Override
	public Iterator<V> iterator() {
		mReadLock.lock();
		try {
			return Collections.unmodifiableList(mList).iterator();
		} finally {
			mReadLock.unlock();
		}
	}
	
	@NonNull
	@Override
	public Object[] toArray() {
		mReadLock.lock();
		try {
			if (mList.isEmpty()) {
				return new Object[0];
			} else {
				final Object[] values = new Object[mList.size()];
				int ix = 0;
				for (final V value: mList) {
					values[ix++] = value;
				}
				return values;
			}
		} finally {
			mReadLock.unlock();
		}
	}
	
	@NonNull
	@Override
	public <T> T[] toArray(@NonNull final T[] ts) {
		mReadLock.lock();
		try {
			return mList.toArray(ts);
		} finally {
			mReadLock.unlock();
		}
	}
	
	/**
	 * return copy of this list
	 * @return
	 */
	@NonNull
	public Collection<V> values() {
		mReadLock.lock();
		try {
			return Collections.unmodifiableCollection(mList);
		} finally {
			mReadLock.unlock();
		}
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
	 * get underlying List
	 * call this between #readLock - #readUnlock or #writeLock - #writeUnlock
	 * @return
	 */
	protected List<V> Locked() {
		return mList;
	}
}
