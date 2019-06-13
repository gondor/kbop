package org.pacesys.kbop.internal;

import org.jetbrains.annotations.Nullable;
import org.pacesys.kbop.IKeyedObjectPool;
import org.pacesys.kbop.IPoolObjectFactory;
import org.pacesys.kbop.IPooledObject;
import org.pacesys.kbop.PoolKey;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-Safe - Abstract synchronous (blocking) pool of Objects which provides the base implementation for single key to single object and single key to multiple object pool implementations.
 *
 * @param <K> The internal pool key type
 * @param <V> the object to Borrow
 * @param <E> the pool object holder containing the pooled object
 */
public abstract class AbstractKeyedObjectPool<K, V, E extends PoolableObject<V, K>> implements IKeyedObjectPool<K, V> {
	
	final Lock lock;
	final ConcurrentMap<PoolKey<K>, E> pool;
	final Set<PoolableObject<V, K>> borrowed;
	final LinkedList<PoolWaitFuture<E>> waiting;
	final IPoolObjectFactory<K, V> factory;
	private volatile boolean isShutDown;
	
	
	/**
	 * Instantiates a new abstract keyed object pool.
	 */
	AbstractKeyedObjectPool(IPoolObjectFactory<K, V> factory) {
		this.lock = new ReentrantLock();
		this.waiting = new LinkedList<>();
		this.borrowed = new HashSet<>();
		this.pool = new ConcurrentHashMap<>();
		this.factory = factory;
	}
	
	/**
	 * Creates the PoolableObject entry based on the provided key
	 *
	 * @param key the PoolKey
	 *
	 * @return the Poolable Object
	 */
	E create(PoolKey<K> key) {
		throw new IllegalStateException("Method not implemented");
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isShutdown() {
		return isShutDown;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public IPooledObject<V, K> borrow(K key) throws Exception {
		return createFuture(new PoolKey<>(key)).get();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public IPooledObject<V, K> borrow(K key, long timeout, TimeUnit unit) throws Exception {
		return createFuture(new PoolKey<>(key)).get(timeout, unit);
	}
	
	/**
	 * Creates a Future which will wait for the Keyed Object to become available or timeout
	 *
	 * @param key the Pool Key
	 *
	 * @return PoolWaitFuture
	 */
	private PoolWaitFuture<E> createFuture(final PoolKey<K> key) {
		return new PoolWaitFuture<E>(lock) {
			protected E getPoolObject(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
				return getBlockingUntilAvailableOrTimeout(key, timeout, unit, this);
			}
		};
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void release(IPooledObject<V, K> borrowedObject) {
		release(borrowedObject, Boolean.TRUE);
	}
	
	protected void release(IPooledObject<V, K> borrowedObject, boolean reusable) {
		lock.lock();
		final PoolableObject<V, K> borrowedVK = (PoolableObject<V, K>) borrowedObject;
		if (borrowed.remove(borrowedVK)) {
			((PoolableObject<V, K>) borrowedObject).releaseOwner();
			if (!reusable) {
				factory.destroy(borrowedObject.get());
				pool.remove(borrowedObject.getKey());
			} else
				factory.passivate(borrowedObject.get());
			
			PoolWaitFuture<E> future = waiting.poll();
			if (future != null) {
				future.wakeup();
			}
		}
		lock.unlock();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void invalidate(IPooledObject<V, K> borrowedObject) {
		release(borrowedObject, Boolean.FALSE);
	}
	
	/**
	 * Internal: Blocks until the object to be borrowed based on the key is available or until the max timeout specified has lapsed.
	 *
	 * @param key     the Pool Key used to lookup the Object to borrow
	 * @param timeout the maximum time to wait
	 * @param unit    the time unit of the timeout argument
	 * @param future  the current future waiting on the object to become available
	 *
	 * @return the Object which was successfully borrowed.
	 * @throws InterruptedException  if the thread was interrupted
	 * @throws IllegalStateException if the pool has been shutdown
	 * @throws TimeoutException      if the wait timed out
	 */
	private E getBlockingUntilAvailableOrTimeout(final PoolKey<K> key, final long timeout, final TimeUnit unit, final PoolWaitFuture<E> future) throws InterruptedException, TimeoutException {
		
		Date deadline = null;
		if (timeout > 0) {
			deadline = new Date(System.currentTimeMillis() + unit.toMillis(timeout));
		}
		lock.lock();
		try {
			do {
				validateShutdown();
				E entry = createOrAttemptToBorrow(key);
				if (entry != null)
					return entry.flagOwner();
			} while (await(future, key, deadline) || deadline == null || deadline.getTime() > System.currentTimeMillis());
			throw new TimeoutException("Timeout waiting for Pool for Key: " + key);
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * Default Single Key to Single Object implementation.  Advanced Pools extending this class can override this behavior.  If the key does not exist then an entry should be created and returned. If
	 * the key exists and is not borrowed then the entry should be returned.
	 * <p>
	 * If the key exists and is already borrowed then null should be returned.
	 * <p>
	 * It is up to the implementation of this method to update the borrowed queue
	 *
	 * @param key the Pool lookup key
	 *
	 * @return Entry if available
	 */
	E createOrAttemptToBorrow(final PoolKey<K> key) {
		E entry;
		if (!pool.containsKey(key)) {
			entry = create(key).initialize(key, this);
			pool.put(key, entry);
			borrowed.add(entry);
			return entry;
		}
		
		entry = pool.get(key);
		
		if (borrowed.add(entry)) {
			factory.activate(entry.get());
			return entry;
		}
		
		return entry.isCurrentOwner() ? entry : null;
	}
	
	/**
	 * Adds the current PoolWaitFuture into the waiting list.  The future will wait up until the specified deadline.  If the future is woken up before the specified deadline then true is returned
	 * otherwise false.  The future will always be removed from the wait list regardless of the outcome.
	 *
	 * @param future   the PoolWaitFuture who is waiting for an object
	 * @param key      the Pool Key associated with this wait
	 * @param deadline the max timeout to wait for
	 *
	 * @return true if
	 * @throws InterruptedException the interrupted exception
	 */
	boolean await(final PoolWaitFuture<E> future, final PoolKey<K> key, @Nullable Date deadline) throws InterruptedException {
		try {
			waiting.add(future);
			return future.await(deadline);
		} finally {
			waiting.remove(future);
		}
	}
	
	/**
	 * Validates the shutdown state. If true then a IllegalStateException is thrown
	 */
	private void validateShutdown() {
		if (isShutdown())
			throw new IllegalStateException("Pool has been shutdown");
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void shutdown() {
		if (isShutDown)
			return;
		
		isShutDown = Boolean.TRUE;
		lock.lock();
		try {
			onShutDown();
			waiting.clear();
			pool.clear();
			borrowed.clear();
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * Used as a hook for extending Pools to trigger cleanup
	 */
	protected abstract void onShutDown();
	
}
