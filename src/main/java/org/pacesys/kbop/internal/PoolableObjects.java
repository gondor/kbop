package org.pacesys.kbop.internal;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import org.pacesys.kbop.IPooledObject;


/**
 * Defines an Key Object Pool which supports multiple objects available for leasing/acquiring
 * 
 * <p>Copyright (c) 2013 by Dorado Software, Inc. All Rights Reserved.
 *
 * @param <V> Contained Object Type
 * @author Jeremy Unruh
 */
public class PoolableObjects<V> extends PoolableObject<V> {

  protected final Set<PoolableObject<V>> borrowed;
  protected final LinkedList<PoolableObject<V>> available;
  protected final LinkedList<PoolWaitFuture<PoolableObject<V>>> waiting;

  /**
   * Instantiates a new poolable objects.
   */
  public PoolableObjects() {
	super(null);
	this.borrowed = new HashSet<PoolableObject<V>>();
	this.available = new LinkedList<PoolableObject<V>>();
	this.waiting = new LinkedList<PoolWaitFuture<PoolableObject<V>>>();
  }

  /**
   * Frees the borrowed object from the internal Pool
   *
   * @param borrowedObject the borrowed object to free
   * @param reusable true if the object can be recycled and used for future allocations
   */
  public void free(IPooledObject<V> borrowedObject, boolean reusable) {
	if (borrowedObject == null) return;

	if (borrowed.remove(borrowedObject))
	{
	  ((PoolableObject<V>)borrowedObject).releaseOwner();
	  if (reusable)
		available.addFirst((PoolableObject<V>)borrowedObject);
	}
  }

  /**
   * Finds an available Poolable Object to borrow
   *
   * @return Poolable Object or null if we couldn't allocate
   */
  public PoolableObject<V> getFree() {
	if (!borrowed.isEmpty()) {
	  for (PoolableObject<V> bo : borrowed) {
		if (bo.isCurrentOwner())
		  return bo;
	  }
	}
	if (!available.isEmpty()) {
	  Iterator<PoolableObject<V>> it = available.iterator();
	  while (it.hasNext()) {
		PoolableObject<V> obj = it.next();
		it.remove();
		borrowed.add(obj);
		return obj;
	  }
	}
	return null;
  }

  /**
   * Adds the Poolable Object to the borrowed list
   *
   * @param entry the entry
   * @return the poolable object
   */
  public PoolableObject<V> add(final PoolableObject<V> entry) {
	borrowed.add(entry);
	return entry;
  }

  /**
   * Queues the {@code future} into the waiting list
   *
   * @param future the future who is waiting to borrow from this pool
   */
  public void queue(final PoolWaitFuture<PoolableObject<V>> future) {
	if (future == null) return;
	waiting.add(future);
  }

  /**
   * Removes the specified {@code future} from the current waiting queue
   *
   * @param future the future
   */
  public void unqueue(final PoolWaitFuture<PoolableObject<V>> future) {
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
  public PoolWaitFuture<PoolableObject<V>> nextWaiting() {
	return waiting.poll();
  }

  /**
   * Cleans up current resources
   */
  void shutdown() {
	available.clear();
	waiting.clear();
	available.clear();
  }
}
