package org.pacesys.kbop;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.pacesys.kbop.PoolMetrics.PoolMultiMetrics;

/**
 * Keyed Object Pool which associates Object(s) available to borrow against a Key.  When an object is borrowed the thread who obtained it has full rights
 * to use the object as well as the ability to keep borrowing until the Object is finally released back into the pool signally other waiting threads the right to
 * borrow the same object.
 * 
 * @param <K> the key type
 * @param <V> the Object being borrowed Type
 * @author Jeremy Unruh
 */
public interface IKeyedObjectPool<K, V> {

	/**
	 * Determines if the Pool is currently Shutdown (borrowing is prohibited)
	 *
	 * @return true, if is shutdown
	 */
	boolean isShutdown();

	/**
	 * Attempts to borrow an Object from the Pool with the given Key.  Please note that upon successful retrieval of the borrowed object it is
	 * up to the borrower to {@link #release(IPooledObject)} the object back into the Pool when complete.  This pool has no cleanup threads
	 * and it is the responsibility of the borrower to properly release the borrowed Object.  Failure to do so when prevent other threads from borrowing the
	 * object.
	 * 
	 * This call will block until the Object associated with the Key is available.  If more control is desired then see {@link #borrow(K, long, TimeUnit)} which 
	 * allows a max time to wait
	 *
	 * @param key the Pool Key used to lookup the Object to borrow
	 * @return the IPooledObject which is a wrapper for the borrowed Object
	 * @throws IllegalStateException if the Pool has been shutdown
	 * @throws Exception  if the thread was interrupted or an error occurred during the creation of a new Object which didn't exist in the Pool.
	 */
	IPooledObject<V, K> borrow(K key) throws Exception;

	/**
	 * Attempts to borrow an Object from the Pool with the given Key.  Please note that upon successful retrieval of the borrowed object it is
	 * up to the borrower to {@link #release(IPooledObject)} the object back into the Pool when complete.  This pool has no cleanup threads
	 * and it is the responsibility of the borrower to properly release the borrowed Object.  Failure to do so when prevent other threads from borrowing the
	 * object.
	 * 
	 * This call will Block until the specified {@code timeout and unit}.  If the object is not available during the specified time then a TimeoutException
	 * will be thrown.
	 *
	 * @param key the Pool Key used to lookup the Object to borrow
	 * @param  timeout the maximum time to wait
	 * @param unit the time unit of the timeout argument
	 * @return the IPooledObject which is a wrapper for the borrowed Object
	 * @throws TimeoutException if the wait timed out
	 * @throws IllegalStateException if the Pool has been shutdown
	 * @throws Exception if the thread was interrupted or an error occurred during the creation of a new Object which didn't exist in the Pool.
	 */
	IPooledObject<V, K> borrow(K key, long timeout, TimeUnit unit) throws TimeoutException, Exception;

	/**
	 * Releases the Borrowed Object back into the Pool and makes it available for borrowing
	 *
	 * @param borrowedObject the object to release
	 */
	void release(IPooledObject<V, K> borrowedObject);

	/**
	 * Releases the object from the pool and removes it.  If the key associated with this object no longer has available object(s)
	 * to borrow against depending on the Pool Configuration then a new object will be created on the next request.
	 * 
	 * @param borrowedObject the object to release and invalidate
	 */
	void invalidate(IPooledObject<V, K> borrowedObject);

	/**
	 * Clears the specified pool, removing all pooled instances corresponding to the given key.  Depending on the underlying Pool Implementation this
	 * method call may be ignored.  
	 * @param key the key to clear
	 */
	void clear(K key);

	/**
	 * Shuts down the current Pool stopping Allocations
	 */
	void shutdown();

	/**
	 * Single Key to Multi Object Pool.  See {@link IKeyedObjectPool} for extended documentation
	 * 
	 * @param <K> the key type
	 * @param <V> the Object being borrowed Type
	 * @author Jeremy Unruh
	 */
	interface Multi<K, V> extends IKeyedObjectPool<K, V> {
		/**
		 * Calculates current metrics and returns a snapshot of Borrowed, Waiting counts and more
		 * @return PoolMetrics
		 */
		PoolMultiMetrics<K> getPoolMetrics();
	}

	/**
	 * Single Key to Single Object Pool.  See {@link IKeyedObjectPool} for extended documentation
	 * 
	 * @param <K> the key type
	 * @param <V> the Object being borrowed Type
	 * @author Jeremy Unruh
	 */
	interface Single<K, V> extends IKeyedObjectPool<K, V> {
		/**
		 * Calculates current metrics and returns a snapshot of Borrowed, Waiting counts and more
		 * @return PoolMetrics
		 */
		PoolMetrics<K> getPoolMetrics();
	}

}