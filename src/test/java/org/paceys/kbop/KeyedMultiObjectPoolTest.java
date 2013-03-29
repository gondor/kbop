package org.paceys.kbop;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.*;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.pacesys.kbop.IKeyedObjectPool;
import org.pacesys.kbop.IPooledObject;
import org.pacesys.kbop.PoolKey;
import org.pacesys.kbop.PoolMetrics.PoolMultiMetrics;
import org.testng.annotations.Test;

/**
 * Test against Keyed Multi Object Pools
 * 
 * @author Jeremy Unruh
 */
@Test(testName="Keyed Multi Object Pool Tests")
public class KeyedMultiObjectPoolTest extends AbstractPoolTest<IKeyedObjectPool.Multi<String, String>> {

	private static final int MAX_ITEMS_PER_KEY = 8;
	private static final int OVER_LIMIT_DELTA = 15;
	private static final int OVER_LIMIT_BLOCK_THREAD_COUNT = MAX_ITEMS_PER_KEY + OVER_LIMIT_DELTA;

	public KeyedMultiObjectPoolTest() {
		super(MAX_ITEMS_PER_KEY);
	}

	/**
	 * Max allocated test against same key.
	 */
	@Test
	public void maxAllocatedTestAgainstSameKey() {
		ExecutionContext context = ExecutionContext.get();
		executeAndWait(createThreadedExecution(POOL_KEY, OVER_LIMIT_BLOCK_THREAD_COUNT, context));
		assertEquals(context.getTimeOutCount(), OVER_LIMIT_DELTA);
	}

	/**
	 * Allocation size should not grow after invalidate.
	 */
	@Test(dependsOnMethods = { "maxAllocatedTestAgainstSameKey" })
	public void allocationSizeShouldNotGrowAfterInvalidate() {
		executeAndWait(createThreadedExecution(POOL_KEY, MAX_ITEMS_PER_KEY, ExecutionContext.get().failIfUnableToBorrow(true).sleepTime(20)));

		// Make sure we are fully Allocated
		PoolMultiMetrics<String> metrics = pool().getPoolMetrics();
		assertEquals(metrics.getKeyMetrics(POOL_KEY).getAllocationSize(), MAX_ITEMS_PER_KEY);

		// Run again and invalidate vs release
		executeAndWait(createThreadedExecution(POOL_KEY, MAX_ITEMS_PER_KEY, ExecutionContext.get().failIfUnableToBorrow(true).reusableAfterAllocation(false).sleepTime(20)));
		metrics = pool().getPoolMetrics();
		assertEquals(metrics.getKeyMetrics(POOL_KEY).getAllocationSize(), 0);
	}

	/**
	 * Allocations against multiple keys fail if timeout occurs.
	 */
	@Test(dependsOnMethods = { "maxAllocatedTestAgainstSameKey" })
	public void allocationsAgainstMultipleKeysFailIfTimeoutOccurs() {
		ExecutionContext context = ExecutionContext.get().failIfUnableToBorrow(true);
		Set<Thread> threads = createThreadedExecution(POOL_KEY, MAX_ITEMS_PER_KEY, context);
		threads.addAll(createThreadedExecution(POOL_KEY2, MAX_ITEMS_PER_KEY, context));
		executeAndWait(threads);
	}

	/**
	 * High concurrency volume test.
	 */
	@Test(dependsOnMethods = { "maxAllocatedTestAgainstSameKey" })
	public void highConcurrencyVolumeTest() {
		ExecutionContext context = ExecutionContext.get().maxWaitTime(0).sleepTime(10);
		executeAndWait(createThreadedExecution(POOL_KEY, 100, context));

		assertTrue(context.getTimeOutCount() == 0);

		PoolMultiMetrics<String> metrics = pool().getPoolMetrics();
		assertEquals(metrics.getKeyMetrics(POOL_KEY).getAllocationSize(), MAX_ITEMS_PER_KEY);
	}

	/**
	 * Test pool sizes.
	 */
	@Test(dependsOnMethods = { "allocationsAgainstMultipleKeysFailIfTimeoutOccurs" })
	public void testPoolSizes() {
		PoolMultiMetrics<String> metrics = pool().getPoolMetrics();
		assertNotNull(metrics);
		assertEquals(metrics.getBorrowedCount(), 0);
		assertEquals(metrics.getWaitingCount(), 0);
		assertEquals(metrics.getKeyCount(), 2);
	}

	/**
	 * Tests shutting down the pool and not allowing any more allocations
	 */
	@Test(dependsOnMethods = { "testPoolSizes" }, expectedExceptions = { IllegalStateException.class })
	public void testShutdown()  {
		pool().shutdown();
		try {
			pool().borrow(POOL_KEY);
		} catch (IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates the threaded execution.
	 *
	 * @param key the key
	 * @param threadCount the thread count
	 * @param context the context
	 * @return the sets the
	 */
	private Set<Thread> createThreadedExecution(final PoolKey<String> key, int threadCount, final ExecutionContext context) {
		Set<Thread> threads = new HashSet<Thread>(threadCount);
		for (int i = 0; i < threadCount; i++)
		{
			threads.add(new Thread(new Runnable() {
				public void run() {
					validateBorrowFromPool(key, context);
				}

			}));
		}
		return threads;
	}

	/**
	 * Execute and wait.
	 *
	 * @param threads the threads
	 */
	private void executeAndWait(Set<Thread> threads)  {
		for (Thread t : threads) t.start();
		for (Thread t : threads)
		{
			try
			{
				t.join();
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e.getMessage());
			}
		}
	}

	/**
	 * Validate borrow from pool.
	 *
	 * @param key the key
	 * @param context the context
	 */
	private void validateBorrowFromPool(PoolKey<String> key, ExecutionContext context)  {
		IPooledObject<String> obj = null;
		try
		{
			obj = pool().borrow(key, context.maxWaitTime, TimeUnit.MILLISECONDS);
			Thread.sleep(context.sleepTime);
			if (context.verifyCanGetSameInstance) {
				IPooledObject<String> obj2 = pool().borrow(key, context.maxWaitTime, TimeUnit.MILLISECONDS);
				assertTrue(obj.equals(obj2));
			}
			context.validate(obj);
		}
		catch (TimeoutException e) {
			if (context.failIfUnableToBorrow)
				fail("Unable to borrow object : " + e.getMessage());
			else
				assertTrue(pool().getPoolMetrics().getKeyMetrics(key).getBorrowedCount() == MAX_ITEMS_PER_KEY);

			context.incrementTimeOutCount();
		}
		catch (Exception e) {
			fail(e.getMessage(), e);
		}
		finally {
			if (obj != null)
			{
				if (context.reusable)
					obj.release();
				else
					obj.invalidate();
			}
		}
	}

}
