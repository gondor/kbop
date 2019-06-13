package org.pacesys.kbop.internal;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * A future which waits until the Pooled Object is available or times out
 * 
 * @param <T> the generic type
 * @author Jeremy Unruh
 */
public abstract class PoolWaitFuture<T> implements Future<T> {

  private final Lock lock;
  private final Condition condition;
  private volatile boolean cancelled;
  private volatile boolean completed;
  private T result;

  /**
   * Instantiates a new pool wait future.
   *
   * @param lock the lock
   */
  public PoolWaitFuture(Lock lock) {
	this.lock = lock;
	this.condition = lock.newCondition();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
	this.lock.lock();
	try {
	  if (this.completed) {
		return false;
	  }
	  this.completed = true;
	  this.cancelled = true;
	  this.condition.signalAll();
	  return true;
	} finally {
	  this.lock.unlock();
	}
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isCancelled() {
	return this.cancelled;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDone() {
	return this.completed;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T get() throws InterruptedException, ExecutionException {
	try {
	  return get(0, TimeUnit.MILLISECONDS);
	} catch (TimeoutException ex) {
	  throw new ExecutionException(ex);
	}
  }

  /**
   * Attempts to borrow/get the Pool Object from the Pool
   *
   * @param timeout the timeout
   * @param unit the unit
   * @return the entry
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws InterruptedException the interrupted exception
   * @throws TimeoutException the timeout exception
   */
  protected abstract T getPoolObject(long timeout, TimeUnit unit) throws IOException, InterruptedException, TimeoutException;

  /**
   * {@inheritDoc}
   */
  @Override
  public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
	this.lock.lock();
	try {
	  if (this.completed) {
		return this.result;
	  }
	  this.result = getPoolObject(timeout, unit);
	  this.completed = true;
	  return result;
	} catch (IOException ex) {
	  this.completed = true;
	  this.result = null;
	  throw new ExecutionException(ex);
	} finally {
	  this.lock.unlock();
	}
  }

  /**
   * Waits/Blocks until the specified deadline or condition
   *
   * @param deadline the deadline
   * @return true, if successful
   * @throws InterruptedException the interrupted exception
   */
  public boolean await(@Nullable final Date deadline) throws InterruptedException {
	this.lock.lock();
	try {
	  if (this.cancelled) {
		throw new InterruptedException("Operation interrupted");
	  }
	  boolean success = false;
	  if (deadline != null) {
		success = this.condition.awaitUntil(deadline);
	  } else {
		this.condition.await();
		success = true;
	  }
	  if (this.cancelled) {
		throw new InterruptedException("Operation interrupted");
	  }
	  return success;
	} finally {
	  this.lock.unlock();
	}

  }

  /**
   * Wakes up the current listener
   */
  public void wakeup() {
	this.lock.lock();
	try {
	  this.condition.signalAll();
	} finally {
	  this.lock.unlock();
	}
  }


}
