package ru.mephi.sno.libs.flow.util;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentDualKeyMap<K1, K2, V> {

	private final Map<K1, V> mapByKey1;
	private final Map<K2, V> mapByKey2;

	public ConcurrentDualKeyMap() {
		mapByKey1 = new ConcurrentHashMap<>();
		mapByKey2 = new ConcurrentHashMap<>();
	}

	public V put(K1 key1, K2 key2, V value) {
		V value1 = mapByKey1.put(key1, value);
		V value2 = mapByKey2.put(key2, value);

		if (value1 != value2)
			throw new RuntimeException("Can't async put values into maps. Value1=" + value1 + ", value2=" + value2 + ", original=" + value);
		return value;
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
		return mapByKey1.get(key);
	}

	public V getByKey2(K2 key) {
		return mapByKey2.get(key);
	}

	public Set<K1> key1Set() {
		return mapByKey1.keySet();
	}

	public Set<K2> key2Set() {
		return mapByKey2.keySet();
	}
}
