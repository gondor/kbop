package org.pacesys.kbop.internal;

import org.jetbrains.annotations.Nullable;
import org.pacesys.kbop.IKeyedObjectPool;
import org.pacesys.kbop.IPooledObject;
import org.pacesys.kbop.PoolKey;

/**
 * Internal Implementation of IPooledObject which holds onto the internal Object V, Key and Pool which created this Object
 * 
 * @param <V> the value type
 * @author Jeremy Unruh
 */
public class PoolableObject<V> implements IPooledObject<V> {

	private long created;
	private long expiry;
	private V object;
	private PoolKey<?> key;
	private IKeyedObjectPool<?, V> pool;
	private Thread owner;

	/**
	 * Instantiates a new poolable object.
	 *
	 * @param object the object
	 */
	public PoolableObject(@Nullable V object) {
		this.object = object;
		this.created = System.currentTimeMillis();
	}

	/**
	 * Initializes this Object when initially created by the Pool for allocation
	 *
	 * @param <K> The Key wrapped Type
	 * @param <E> the Entry Type
	 * @param key The Key which is associated with this Pool Object
	 * @param pool the pool creating this allocation
	 * @return Poolable Object
	 */
	@SuppressWarnings("unchecked")
	<K, E extends PoolableObject<V>> E initialize(PoolKey<K> key, IKeyedObjectPool<?, V> pool) {
		this.key = key;
		this.pool = pool;
		return (E) this;
	}

	/**
	 * Flags the current thread as the new Owner of this Object
	 *
	 * @param <K> the Key wrapped Type
	 * @param <E> the Entry Type
	 * @return PoolableObject for method chaining
	 */
	@SuppressWarnings("unchecked")
	<K, E extends PoolableObject<V>> E flagOwner() {
		this.owner = Thread.currentThread();
		return (E) this;
	}

	/**
	 * Releases the current owning thread from this Object
	 *
	 * @param <K> the Key wrapped Type
	 * @param <E> the Entry type
	 * @return PoolableObject for method chaining
	 */
	@SuppressWarnings("unchecked")
	<K, E extends PoolableObject<V>> E releaseOwner() {
		this.owner = null;
		return (E) this;
	}

	/**
	 * Determines if the current thread is the Owner of this object (current borrower)
	 *
	 * @return true, if current owner
	 */
	public boolean isCurrentOwner() {
		return Thread.currentThread().equals(owner);
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <K> PoolKey<K> getKey() {
		return (PoolKey<K>) key;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return "PoolableObject [created=" + created + ", expiry=" + expiry
				+ ", object=" + object + "]";
	}

	/**
	 * {@inheritDoc}
	 */
	public V get() {
		return object;
	}

	/**
	 * Determines if this Object has expired
	 *
	 * @param now time to compare against
	 * @return true, if is expired
	 */
	public boolean isExpired(long now) {
		return now >= expiry;
	}

	/**
	 * @return the expiry time for this Object
	 */
	public long getExpiry() {
		return expiry;
	}

	/**
	 * {@inheritDoc}
	 */
	public long getCreated() {
		return created;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void release() {
		pool.release(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void invalidate() {
		pool.invalidate(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <K> K getUserKey() {
		return (K) getKey().get();
	}

}
