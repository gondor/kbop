package org.pacesys.kbop;

import java.io.Serializable;

/**
 * Key used to point to the Object Pool Entry(s)
 * 
 * <p>Copyright (c) 2013 by Dorado Software, Inc. All Rights Reserved.
 *
 * @param <K> the key type
 * @author Jeremy Unruh
 */
public class PoolKey<K> implements Cloneable, Serializable {

  private static final long serialVersionUID = 4383493776548641532L;
  private final K key;
  private final int hashCode;

  private PoolKey(K key) {
	this.key = key;
	this.hashCode = computeHashCode();
  }

  /**
   * Creates a new Pool Key wrapping the specified key of type {@code K}
   *
   * @param <K> the wrapped inner pool key type
   * @param key the wrapped key
   * @return PoolKey
   */
  public static <K> PoolKey<K> lookup(K key) {
	if (key == null) 
	  throw new IllegalStateException("Key must not be null");
	return new PoolKey<K>(key);
  }

  /**
   * @return the inner wrapped pool key
   */
  public K get() {
	return key;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
	return hashCode;
  }

  private int computeHashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((key == null) ? 0 : key.hashCode());
	return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
	if (this == obj)
	  return true;
	if (obj == null)
	  return false;
	if (getClass() != obj.getClass())
	  return false;
	PoolKey<?> other = (PoolKey<?>) obj;
	if (key == null) {
	  if (other.key != null)
		return false;
	} else if (!key.equals(other.key))
	  return false;
	return true;
  }
}
