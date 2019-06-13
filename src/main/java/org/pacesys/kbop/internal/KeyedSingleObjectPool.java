package org.pacesys.kbop.internal;

import org.pacesys.kbop.IKeyedObjectPool;
import org.pacesys.kbop.IPoolObjectFactory;
import org.pacesys.kbop.PoolKey;
import org.pacesys.kbop.PoolMetrics;

/**
 * Thread-Safe Single Key to Object based Blocking Pool.
 * 
 * @param <K> the key type
 * @param <V> the pooled object
 * 
 * @author Jeremy Unruh
 */
public class KeyedSingleObjectPool<K,V> extends AbstractKeyedObjectPool<K, V, PoolableObject<V, K>> implements IKeyedObjectPool.Single<K, V> {

	public KeyedSingleObjectPool(IPoolObjectFactory<K, V> factory) {
		super(factory);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected PoolableObject<V, K> create(PoolKey<K> key) {
		return new PoolableObject<>(factory.create(key));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PoolMetrics getPoolMetrics() {
		return new PoolMetrics(this.borrowed.size(), this.waiting.size(), 1, pool.keySet().size());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onShutDown() { }

}