package org.pacesys.kbop;

/**
 * A Object Pool Entry which wraps the underlying borrowed Object as V.  
 * 
 * @param <V> the value type
 * @param <K> the pool key type
 * @author Jeremy Unruh
 */
public interface IPooledObject<V, K> {

  /**
   * The borrowed object this Pooled Object is associated with
   *
   * @return the actual borrowed Object V
   */
  V get();

  /**
   * The Date/Time in milliseconds when this Object was created and initially inserted into the Pool
   *
   * @return the created
   */
  long getCreated();

  /**
   * Releases this Object back into the Pool allowing others to access it
   */
  void release();

  /**
   * Releases the object from the pool and removes it.  If the key associated with this object no longer has available object(s)
   * to borrow against depending on the Pool Configuration then a new object will be created on the next request.
   */
  void invalidate();

  /**
   * The Key which is associated with this Pool Object
   * @return the Pool Key
   */
  PoolKey<K> getKey();
  
  /**
   * @return the user inner key
   */
  K getUserKey();
}