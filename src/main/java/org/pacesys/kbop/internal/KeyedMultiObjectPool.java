package org.pacesys.kbop.internal;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.pacesys.kbop.IKeyedObjectPool;
import org.pacesys.kbop.IPoolObjectFactory;
import org.pacesys.kbop.IPooledObject;
import org.pacesys.kbop.PoolKey;
import org.pacesys.kbop.PoolMetrics.KeyMetric;
import org.pacesys.kbop.PoolMetrics.PoolMultiMetrics;

/**
 * Thread Safe - Single Key to Multiple Object Pool
 * 
 * @param <K> the key type
 * @param <V> the value type
 * @author Jeremy Unruh
 */
public class KeyedMultiObjectPool<K, V> extends AbstractKeyedObjectPool<K, V, PoolableObject<V>> implements IKeyedObjectPool.Multi<K, V>{

  private int maxPerKey;

  /**
   * Instantiates a new keyed multi object pool.
   *
   * @param factory the factory
   */
  public KeyedMultiObjectPool(IPoolObjectFactory<K, V> factory, int maxPerKey) {
	super(factory);
	this.maxPerKey = maxPerKey;
  }


  @SuppressWarnings("unchecked")
  protected void release(IPooledObject<V> borrowedObject, boolean reusable) {
	lock.lock();
	if (borrowed.remove(borrowedObject))
	{
	  PoolableObjects<V> pos = objectPool((PoolKey<K>) borrowedObject.getKey(), Boolean.FALSE);
	  if (pos != null) {
		if (reusable)
		  factory.passivate(borrowedObject.get());
		else
		  factory.destroy(borrowedObject.get());

		pos.free(borrowedObject, reusable);
	  }

	  notifyWaiting(pos);
	}
	lock.unlock();
  }

  protected void notifyWaiting(PoolableObjects<V> pooledObjects) {
	PoolWaitFuture<PoolableObject<V>> future = pooledObjects.nextWaiting();
	if (future != null)
	  waiting.remove(future);
	else
	  future = waiting.poll();

	if (future != null) {
	  future.wakeup();
	}
  }

  PoolableObjects<V> objectPool(PoolKey<K> key) {
	return objectPool(key, Boolean.TRUE);
  }

  PoolableObjects<V> objectPool(PoolKey<K> key, boolean createIfNotFound) {
	PoolableObjects<V> pobjs = (PoolableObjects<V>) pool.get(key);
	if (pobjs == null && createIfNotFound) {
	  pobjs = new PoolableObjects<V>().initialize(key, this);
	  pool.put(key, pobjs);
	}
	return pobjs;
  }


  @Override
  protected PoolableObject<V> createOrAttemptToBorrow(PoolKey<K> key) {

	PoolableObjects<V> pobjs = objectPool(key);
	PoolableObject<V> entry = pobjs.getFree();

	if (entry != null) {
	  borrowed.add(entry);
	  factory.activate(entry.get());
	  return entry;
	}

	if (pobjs.getAllocationSize() < maxPerKey) {
	  V obj = factory.create(key);
	  entry = pobjs.add(new PoolableObject<V>(obj).initialize(key, this).flagOwner());
	  borrowed.add(entry);
	  return entry;
	}
	return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean await(final PoolWaitFuture<PoolableObject<V>> future, final PoolKey<K> key, Date deadline) throws InterruptedException {
	PoolableObjects<V> pobjs = objectPool(key);
	try
	{
	  pobjs.queue(future);
	  waiting.add(future);
	  return future.await(deadline);
	}
	finally {
	  pobjs.unqueue(future);
	  waiting.remove(future);
	}
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PoolMultiMetrics<K> getPoolMetrics() {
	Map<PoolKey<K>, KeyMetric> keyMetrics = new HashMap<PoolKey<K>, KeyMetric>();
	for (PoolKey<K> k : pool.keySet()) {
	  PoolableObjects<V> pobjs = objectPool(k, Boolean.FALSE);
	  if (pobjs != null) {
		keyMetrics.put(k, new KeyMetric(pobjs.getAllocationSize(), pobjs.borrowed.size(), pobjs.waiting.size()));
	  }
	}
	return new PoolMultiMetrics<K>(borrowed.size(), waiting.size(), maxPerKey, keyMetrics);
  }


  /**
   * {@inheritDoc}
   */
  @Override
  protected void onShutDown() {
	for (PoolKey<K> k : pool.keySet()) {
	  PoolableObjects<V> pobjs = objectPool(k, Boolean.FALSE);
	  pobjs.shutdown();
	}
  }


}
