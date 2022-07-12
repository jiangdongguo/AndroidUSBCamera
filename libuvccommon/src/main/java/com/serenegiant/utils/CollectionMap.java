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

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Map holds multiple values for each key
 * This class use HashMap as Map and ArrayList as Collection as default.
 * You can override this by overriding #createContentsMap and #createCollection.
 * @param <K>
 * @param <V>
 */
public class CollectionMap<K, V> implements Map<K, Collection<V>> {
	private final Map<K, Collection<V>> contents;

	public CollectionMap() {
		contents = createContentsMap();
	}

	protected Map<K, Collection<V>> createContentsMap() {
		return new HashMap<>();
	}

	protected Collection<V> createCollection() {
		return new ArrayList<>();
	}

	@Override
	public void clear() {
		contents.clear();
	}

	@Override
	public boolean containsKey(final Object key) {
		return contents.containsKey(key);
	}

	@Override
	public boolean containsValue(final Object value) {
		return contents.containsValue(value);
	}

	public boolean containsInValue(final V value) {
		for (final Collection<V> collection : contents.values()) {
			if (collection.contains(value)) {
				return true;
			}
		}
		return false;
	}

	@Override
	@NonNull
	public Set<Entry<K, Collection<V>>> entrySet() {
		return contents.entrySet();
	}

	@Override
	public Collection<V> get(final Object key) {
		return contents.get(key);
	}

	@Override
	public boolean isEmpty() {
		return contents.isEmpty();
	}

	@Override
	@NonNull
	public Set<K> keySet() {
		return contents.keySet();
	}

	@Override
	public Collection<V> put(final K key, final Collection<V> value) {
		return contents.put(key, value);
	}

	public boolean add(final K key, final V value) {
		Collection<V> collection = contents.get(key);
		if (collection == null) {
			collection = createCollection();
			contents.put(key, collection);
		}
		return collection.add(value);
	}

	@Override
	public void putAll(@NonNull final Map<? extends K, ? extends Collection<V>> m) {
		contents.putAll(m);
	}

	public void addAll(final Map<? extends K, ? extends Collection<V>> m) {
		for (final Entry<? extends K, ? extends Collection<V>> entry : m.entrySet()) {
			addAll(entry.getKey(), entry.getValue());
		}
	}

	public boolean addAll(final K key, final Collection<? extends V> values) {
		Collection<V> collection = contents.get(key);
		if (collection == null) {
			collection = createCollection();
			contents.put(key, collection);
		}
		return collection.addAll(values);
	}

	@Override
	public Collection<V> remove(final Object key) {
		return contents.remove(key);
	}

	public boolean remove(final Object key, final Object value) {
		final Collection<?> collection = contents.get(key);
		return collection != null && collection.remove(value);
	}

	@Override
	public int size() {
		return contents.size();
	}

	public int size(final K key) {
		Collection<V> collection = contents.containsKey(key) ? contents.get(key) : null;
		return collection != null ? collection.size() : 0;
	}

	@Override
	@NonNull
	public Collection<Collection<V>> values() {
		return contents.values();
	}

	public Collection<V> valuesAll() {
		final Collection<V> result = createCollection();
		for (final Collection<V> v: values()) {
			result.addAll(v);
		}
		return result;
	}
}
