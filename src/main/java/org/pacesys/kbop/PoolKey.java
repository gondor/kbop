package org.pacesys.kbop;

import lombok.Value;

import java.io.Serializable;

/**
 * Key used to point to the Object Pool Entry(s)
 *
 * @param <K> the key type
 * @author Jeremy Unruh
 */
@Value
public class PoolKey<K> implements Cloneable, Serializable {
	private static final long serialVersionUID = 4383493776548641532L;
	private final K key;
}