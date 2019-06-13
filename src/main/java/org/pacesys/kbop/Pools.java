package org.pacesys.kbop;

import org.pacesys.kbop.internal.KeyedMultiObjectPool;
import org.pacesys.kbop.internal.KeyedSingleObjectPool;

/**
 * Static utility methods pertaining to  {@link IKeyedObjectPool} instances
 */
public class Pools {
	
	/**
	 * Creates a new Single Key to Object Pool
	 *
	 * @param factory the factory which creates new Objects (T) when needed
	 *
	 * @return IKeyedObjectPool
	 */
	public static <K, T> IKeyedObjectPool.Single<K, T> createPool(IPoolObjectFactory<K, T> factory) {
		return new KeyedSingleObjectPool<>(factory);
	}
	
	/**
	 * Creates a new Single or Multi Object Pool depending on the maxItemsPerKey size.  If the {@code maxItemsPerKey} is > 1 then a Multi Object to Key Pool is created.
	 *
	 * @param factory        the factory which creates new Objects (T) when needed
	 * @param maxItemsPerKey the size of pooled object for a single given key
	 *
	 * @return IKeyedObjectPool
	 */
	public static <K, T> IKeyedObjectPool.Multi<K, T> createMultiPool(IPoolObjectFactory<K, T> factory, int maxItemsPerKey) {
		return new KeyedMultiObjectPool<>(factory, maxItemsPerKey);
		
	}
}
