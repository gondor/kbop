package org.paceys.kbop;

import org.junit.Test;
import org.pacesys.kbop.IKeyedObjectPool;
import org.pacesys.kbop.IPooledObject;
import org.pacesys.kbop.PoolMetrics.PoolMultiMetrics;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class KeyedMultiObjectPoolTest extends
		AbstractPoolTest<IKeyedObjectPool.Multi<String, String>> {
	
	private static final int MAX_ITEMS_PER_KEY = 8;
	private static final int OVER_LIMIT_DELTA = 15;
	private static final int OVER_LIMIT_BLOCK_THREAD_COUNT = MAX_ITEMS_PER_KEY + OVER_LIMIT_DELTA;
	
	public KeyedMultiObjectPoolTest() {
		super(MAX_ITEMS_PER_KEY);
	}
	
	@Test
	public void maxAllocatedTestAgainstSameKey() {
		ExecutionContext context = ExecutionContext.get();
		executeAndWait(createThreadedExecution(POOL_KEY, OVER_LIMIT_BLOCK_THREAD_COUNT, context));
		assertThat(context.getTimeOutCount()).isEqualTo(OVER_LIMIT_DELTA);
	}
	
	@Test
	@SuppressWarnings("ConstantConditions")
	public void allocationSizeShouldNotGrowAfterInvalidate() {
		maxAllocatedTestAgainstSameKey();
		
		executeAndWait(createThreadedExecution(POOL_KEY, MAX_ITEMS_PER_KEY,
				ExecutionContext.get().failIfUnableToBorrow(true).sleepTime(20)));
		
		// Make sure we are fully Allocated
		PoolMultiMetrics<String> metrics = pool().getPoolMetrics();
		assertThat(metrics.getKeyMetrics(POOL_KEY).getAllocationSize()).isEqualTo(MAX_ITEMS_PER_KEY);
		
		// Run again and invalidate vs release
		executeAndWait(createThreadedExecution(POOL_KEY, MAX_ITEMS_PER_KEY, ExecutionContext.get().failIfUnableToBorrow(true)
				.reusableAfterAllocation(false).sleepTime(20)));
		metrics = pool().getPoolMetrics();
		assertThat(metrics.getKeyMetrics(POOL_KEY).getAllocationSize()).isZero();
	}
	
	@Test
	public void allocationsAgainstMultipleKeysFailIfTimeoutOccurs() {
		maxAllocatedTestAgainstSameKey();
		
		ExecutionContext context = ExecutionContext.get().failIfUnableToBorrow(true);
		Set<Thread> threads = createThreadedExecution(POOL_KEY, MAX_ITEMS_PER_KEY, context);
		threads.addAll(createThreadedExecution(POOL_KEY2, MAX_ITEMS_PER_KEY, context));
		executeAndWait(threads);
	}
	
	@Test
	@SuppressWarnings("ConstantConditions")
	public void highConcurrencyVolumeTest() {
		maxAllocatedTestAgainstSameKey();
		
		ExecutionContext context = ExecutionContext.get().maxWaitTime(0)
				.sleepTime(10);
		executeAndWait(createThreadedExecution(POOL_KEY, 100, context));
		
		assertThat(context.getTimeOutCount() == 0).isTrue();
		
		PoolMultiMetrics<String> metrics = pool().getPoolMetrics();
		assertThat(metrics.getKeyMetrics(POOL_KEY).getAllocationSize()).isEqualTo(MAX_ITEMS_PER_KEY);
	}
	
	@Test
	public void testPoolSizes() {
		allocationsAgainstMultipleKeysFailIfTimeoutOccurs();
		
		PoolMultiMetrics<String> metrics = pool().getPoolMetrics();
		assertThat(metrics).isNotNull();
		assertThat(metrics.getBorrowedCount()).isZero();
		assertThat(metrics.getWaitingCount()).isZero();
		assertThat(metrics.getKeyCount()).isEqualTo(2);
	}
	
	@Test
	public void testShutdown() {
		testPoolSizes();
		
		pool().shutdown();
		try {
			pool().borrow(POOL_KEY);
		} catch (IllegalStateException e) {
			// OK
		} catch (Exception e) {
			fail(e.getMessage(), e);
		}
	}
	
	private Set<Thread> createThreadedExecution(final String key,
												int threadCount, final ExecutionContext context) {
		Set<Thread> threads = new HashSet<>(threadCount);
		for (int i = 0; i < threadCount; i++) {
			threads.add(new Thread(new Runnable() {
				public void run() {
					validateBorrowFromPool(key, context);
				}
				
			}));
		}
		return threads;
	}
	
	private void executeAndWait(Set<Thread> threads) {
		for (Thread t : threads)
			t.start();
		for (Thread t : threads) {
			try {
				t.join();
			} catch (InterruptedException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}
	}
	
	@SuppressWarnings("ConstantConditions")
	private void validateBorrowFromPool(String key, ExecutionContext context) {
		IPooledObject<String, String> obj = null;
		try {
			obj = pool().borrow(key, context.maxWaitTime, TimeUnit.MILLISECONDS);
			Thread.sleep(context.sleepTime);
			if (context.verifyCanGetSameInstance) {
				IPooledObject<String, String> obj2 = pool().borrow(key, context.maxWaitTime,
						TimeUnit.MILLISECONDS);
				assertThat(obj.equals(obj2)).isTrue();
			}
			context.validate(obj);
		} catch (TimeoutException e) {
			if (context.failIfUnableToBorrow)
				fail("Unable to borrow object : " + e.getMessage(), e);
			else
				assertThat(pool().getPoolMetrics().getKeyMetrics(key)
						.getBorrowedCount() == MAX_ITEMS_PER_KEY).isTrue();
			
			context.incrementTimeOutCount();
		} catch (Exception e) {
			fail(e.getMessage(), e);
		} finally {
			if (obj != null) {
				if (context.reusable)
					obj.release();
				else
					obj.invalidate();
			}
		}
	}
}