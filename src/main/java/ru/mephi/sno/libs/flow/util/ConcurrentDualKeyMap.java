package ru.mephi.sno.libs.flow.util;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentDualKeyMap<K1, K2, V> {

	private final Map<K1, ValInfo<K1, K2, V>> mapByKey1;
	private final Map<K2, ValInfo<K1, K2, V>> mapByKey2;

	private record ValInfo<K1, K2, V>(K1 key1, K2 key2, V value) {}

	public ConcurrentDualKeyMap() {
		mapByKey1 = new ConcurrentHashMap<>();
		mapByKey2 = new ConcurrentHashMap<>();
	}

	public void put(K1 key1, K2 key2, V value) {
		if (key1 == null || key2 == null || value == null)
			throw new NullPointerException("key1 and key2 and value cannot be null");

		mapByKey1.put(key1, new ValInfo<>(key1, key2, value));
		mapByKey2.put(key2, new ValInfo<>(key1, key2, value));
	}

	public boolean containsKey1(K1 key1) {
		return mapByKey1.containsKey(key1);
	}

	public boolean containsKey2(K2 key2) {
		return mapByKey2.containsKey(key2);
	}

	public boolean containsKey(K1 key1, K2 key2) {
		return containsKey1(key1) || containsKey2(key2);
	}

	public V getByKey1(K1 key) {
		return mapByKey1.get(key).value;
	}

	public V getByKey2(K2 key) {
		return mapByKey2.get(key).value;
	}

	public Set<K1> key1Set() {
		return mapByKey1.keySet();
	}

	public Set<K2> key2Set() {
		return mapByKey2.keySet();
	}

	public K2 associateByKey1(K1 key1) {
		return mapByKey1.get(key1).key2;
	}

	public K1 associateByKey2(K2 key2) {
		return mapByKey2.get(key2).key1;
	}
}
