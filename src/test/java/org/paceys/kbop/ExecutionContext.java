package org.paceys.kbop;

import java.util.concurrent.atomic.AtomicInteger;

import org.pacesys.kbop.IPooledObject;

/**
 * Concurrent Test Execution Context which allows for various options to be passed through to a Runnable cutting down on Test Boiler Plate code
 * 
 * @author Jeremy Unruh
 */
public class ExecutionContext {

  public interface PostValidation {

	<V> void validate(IPooledObject<V> borrowedObject);
  }

  /** Max time to block waiting for an Object in Milliseconds */
  public long maxWaitTime = 100;

  /** Time to sleep after a successful borrow in Milliseconds */
  public long sleepTime = 600;

  /** Insure we can re-borrow the previously borrowed object from the current thread and that we get back the same instance */
  public boolean verifyCanGetSameInstance = Boolean.TRUE;

  /** The fail if unable to borrow. */
  public boolean failIfUnableToBorrow;

  public boolean reusable = Boolean.TRUE;

  private PostValidation validator;

  private AtomicInteger timeoutCount = new AtomicInteger();

  public static ExecutionContext get() {
	return new ExecutionContext();
  }

  /**
   * Associates a PostValidation handler to allow extra constraint tests after a success object has been borrowed
   * @param validator the post validation validator
   * @return the execution context
   */
  public ExecutionContext postValidator(PostValidation validator) {
	this.validator = validator;
	return this;
  }

  /**
   * Time to sleep after a successful borrow in Milliseconds
   *
   * @param sleepTime the sleep time
   * @return the execution context
   */
  public ExecutionContext sleepTime(long sleepTime) {
	this.sleepTime = sleepTime;
	return this;
  }

  /**
   * Max time to block waiting for an Object in Milliseconds
   *
   * @param maxWaitTime the max wait time
   * @return the execution context
   */
  public ExecutionContext maxWaitTime(long maxWaitTime) {
	this.maxWaitTime = maxWaitTime;
	return this;
  }

  /**
   * Fail if unable to borrow.
   *
   * @param failIfUnableToBorrow the fail if unable to borrow
   * @return the execution context
   */
  public ExecutionContext failIfUnableToBorrow(boolean failIfUnableToBorrow) {
	this.failIfUnableToBorrow = failIfUnableToBorrow;
	return this;
  }

  /**
   * If false then the Object will be invalidated back to the Pool vs Released
   *
   * @param reusable true to release, false to invalidate
   * @return the execution context
   */
  public ExecutionContext reusableAfterAllocation(boolean reusable) {
	this.reusable = reusable;
	return this;
  }

  /**
   * @return the number of times we timed out during a threaded execution
   */
  public int getTimeOutCount() {
	return timeoutCount.get();
  }

  /**
   * Increments the current timeout counter
   * @return the new timeout
   */
  public int incrementTimeOutCount() {
	return timeoutCount.incrementAndGet();
  }

  /**
   * Insure we can re-borrow the previously borrowed object from the current thread and that we get back the same instance
   *
   * @param verifyCanGetSameInstance the verify can get same instance
   * @return the execution context
   */
  public ExecutionContext verifyCanGetSameInstance(boolean verifyCanGetSameInstance) {
	this.verifyCanGetSameInstance = verifyCanGetSameInstance;
	return this;
  }

  public <V> void validate(IPooledObject<V> borrowedObject) {
	if (validator != null)
	  validator.validate(borrowedObject);
  }
}
