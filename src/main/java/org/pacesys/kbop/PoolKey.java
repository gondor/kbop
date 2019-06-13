package org.pacesys.kbop;

import lombok.Value;

/**
 * Key used to point to the Object Pool Entry(s)
 *
 * @param <K> the key type
 * @author Jeremy Unruh
 */
@Value
public class PoolKey<K> {
	private final K key;
}