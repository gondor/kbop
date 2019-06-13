package org.pacesys.kbop;

/**
 * A Factory which is responsible for creating the Object (V) based on the Pool Key.  The returned Object will be wrapped in a {@link IPooledObject} and inserted into the
 * Pool for access
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public interface IPoolObjectFactory<K, V> {

  /**
   * Creates the defined Object V based on the current Pool Key.
   *
   * @param key the key being requested
   * @return the Object to be inserted into the Pool
   */
  V create(PoolKey<K> key);

  /**
   * Reinitialize an instance to be returned to the borrower
   *
   * @param object the object being borrowed
   */
  void activate(V object);

  /**
   * Uninitialize an instance which has been released back to the pool
   *
   * @param object the object to which has just beel released
   */
  void passivate(V object);

  /**
   * Destroy an instance no longer needed by the pool
   *
   * @param object the object to destroy
   */
  void destroy(V object);

}
