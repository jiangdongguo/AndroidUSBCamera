package com.serenegiant.utils;
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

import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReentrantReadWriteWeakRef<T> {
	private final ReentrantReadWriteLock mSensorLock = new ReentrantReadWriteLock();
	private final Lock mReadLock = mSensorLock.readLock();
	private final Lock mWriteLock = mSensorLock.writeLock();

	private WeakReference<T> mWeakRef;
	
	/**
	 * constructor
	 */
	public ReentrantReadWriteWeakRef() {
	}
	
	/**
	 * constructor
	 * @param obj
	 */
	public ReentrantReadWriteWeakRef(@Nullable final T obj) {
		set(obj);
	}
	
	/**
	 * copy constructor
	 * @param ref
	 */
	public ReentrantReadWriteWeakRef(@Nullable final WeakReference<T> ref) {
		set(ref);
	}
	
	/**
	 * copy constructor
	 * @param ref
	 */
	public ReentrantReadWriteWeakRef(@Nullable final ReentrantReadWriteWeakRef<T> ref) {
		if (ref != null) {
			set(ref.get());
		}
	}
	
	/**
	 * get reference with read locked,
	 * if this ReentrantReadWriteWeakRef in write lock,
	 * this will block current thread
	 * @return
	 */
	@Nullable
	public T get() {
		mReadLock.lock();
		try {
			return mWeakRef != null ? mWeakRef.get() : null;
		} finally {
			mReadLock.unlock();
		}
	}
	
	/**
	 * try to get reference with read locked
	 * if this ReentrantReadWriteWeakRef is not in write lock.
	 * @return
	 */
	@Nullable
	public T tryGet() {
		if (mReadLock.tryLock()) {
			try {
				return mWeakRef != null ? mWeakRef.get() : null;
			} finally {
				mReadLock.unlock();
			}
		}
		return null;
	}
	
	/**
	 * set object as weak reference with write lock
	 * @param obj
	 * @return previous referenced object, will be null
	 */
	@Nullable
	public T set(@Nullable final T obj) {
		T prev;
		mWriteLock.lock();
		try {
			prev = mWeakRef != null ? mWeakRef.get() : null;
			if (obj != null) {
				mWeakRef = new WeakReference<T>(obj);
			} else {
				mWeakRef = null;
			}
		} finally {
			mWriteLock.unlock();
		}
		return prev;
	}
	
	/**
	 * set object as weak reference with write lock
	 * @param ref
	 * @return previous referenced object, will be null
	 */
	@Nullable
	public T set(@Nullable final WeakReference<T> ref) {
		final T obj = ref != null ? ref.get() : null;
		T prev;
		mWriteLock.lock();
		try {
			prev = mWeakRef != null ? mWeakRef.get() : null;
			if (obj != null) {
				mWeakRef = new WeakReference<T>(obj);
			} else {
				mWeakRef = null;
			}
		} finally {
			mWriteLock.unlock();
		}
		return prev;
	}
	
	/**
	 * set object as weak reference with write lock
	 * @param ref
	 * @return previous referenced object, will be null
	 */
	@Nullable
	public T set(final ReentrantReadWriteWeakRef<T> ref) {
		return set(ref != null ? ref.get() : null);
	}
	
	/**
	 * clear reference with write lock
	 * @return previous referenced object, will be null
	 */
	@Nullable
	public T clear() {
		return set((T)null);
	}
	
	/**
	 * swap reference with write lock
	 * @param ref
	 * @return previous referenced object, will be null
	 */
	@Nullable
	public T swap(final ReentrantReadWriteWeakRef<T> ref) {
		return set(ref);
	}

	/**
	 * return whether this holds reference or not
	 * if other thread holds write lock, this will return true
	 * @return
	 */
	public boolean isEmpty() {
		return tryGet() == null;
	}

//================================================================================
	/**
	 * lock for read access,
	 * never forget to call #readUnlock
	 */
	public void readLock() {
		mReadLock.lock();
	}

	/**
	 * unlock read access
	 */
	public void readUnlock() {
		mReadLock.unlock();
	}

	/**
	 * lock for write access
	 * never forget to call writeUnlock
	 */
	public void writeLock() {
		mWriteLock.lock();
	}

	/**
	 * unlock write access
	 */
	public void writeUnlock() {
		mWriteLock.unlock();
	}

}
