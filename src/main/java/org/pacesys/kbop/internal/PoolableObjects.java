package org.pacesys.kbop.internal;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.pacesys.kbop.IPooledObject;

/**
 * Defines an Key Object Pool which supports multiple objects available for leasing/acquiring
 * 
 * @param <V> Contained Object Type
 * @param <K> The pool key type
 */
class PoolableObjects<V, K> extends PoolableObject<V, K> {

	final Set<PoolableObject<V, K>> borrowed;
	private final LinkedList<PoolableObject<V, K>> available;
	final LinkedList<PoolWaitFuture<PoolableObject<V, K>>> waiting;

	/**
	 * Instantiates a new poolable objects.
	 */
	public PoolableObjects() {
		super(null);
		this.borrowed = new HashSet<>();
		this.available = new LinkedList<>();
		this.waiting = new LinkedList<>();
	}

	/**
	 * Frees the borrowed object from the internal Pool
	 *
	 * @param borrowedObject the borrowed object to free
	 * @param reusable true if the object can be recycled and used for future allocations
	 */
	public void free(IPooledObject<V, K> borrowedObject, boolean reusable) {
		if (borrowedObject == null) return;
		
		final PoolableObject<V, K> borrowedVK = (PoolableObject<V, K>) borrowedObject;
		if (borrowed.remove(borrowedVK))
		{
			((PoolableObject<V, K>)borrowedObject).releaseOwner();
			if (reusable)
				available.addFirst((PoolableObject<V, K>)borrowedObject);
		}
	}

	/**
	 * Finds an available Poolable Object to borrow
	 *
	 * @return Poolable Object or null if we couldn't allocate
	 */
	@Nullable
	public PoolableObject<V, K> getFree() {
		if (!borrowed.isEmpty()) {
			for (PoolableObject<V, K> bo : borrowed) {
				if (bo.isCurrentOwner())
					return bo;
			}
		}
		if (!available.isEmpty()) {
			PoolableObject<V, K> obj = available.remove();
			borrowed.add(obj);
			return obj;
		}
		return null;
	}

	/**
	 * Adds the Poolable Object to the borrowed list
	 *
	 * @param entry the entry
	 * @return the poolable object
	 */
	public PoolableObject<V, K> add(final PoolableObject<V, K> entry) {
		borrowed.add(entry);
		return entry;
	}

	/**
	 * Queues the {@code future} into the waiting list
	 *
	 * @param future the future who is waiting to borrow from this pool
	 */
	public void queue(final PoolWaitFuture<PoolableObject<V, K>> future) {
		if (future == null) return;
		waiting.add(future);
	}

	/**
	 * Removes the specified {@code future} from the current waiting queue
	 *
	 * @param future the future
	 */
	public void unqueue(final PoolWaitFuture<PoolableObject<V, K>> future) {
		if (future == null) return;
		waiting.remove(future);
	}


	/**
	 * Gets the allocation size.
	 *
	 * @return the allocation size
	 */
	public int getAllocationSize() {
		return available.size() + borrowed.size();
	}

	/**
	 * Finds the next Future who is waiting to borrow from this pool or null
	 *
	 * @return the future who has been waiting or null if no waiters
	 */
	@Nullable
	public PoolWaitFuture<PoolableObject<V, K>> nextWaiting() {
		return waiting.poll();
	}

	/**
	 * Cleans up current resources
	 */
	void shutdown() {
		available.clear();
		waiting.clear();
		//noinspection RedundantOperationOnEmptyContainer
		available.clear(); // FIXME is this intentional?
	}
}
