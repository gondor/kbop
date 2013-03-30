package org.paceys.kbop;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.pacesys.kbop.IKeyedObjectPool;
import org.pacesys.kbop.IPoolObjectFactory;
import org.pacesys.kbop.IPooledObject;
import org.pacesys.kbop.PoolKey;
import org.pacesys.kbop.PoolMetrics;
import org.pacesys.kbop.Pools;
import org.testng.annotations.Test;

/**
 * Keyed Single Object Pool functional Tests
 * 
 * @author Jeremy Unruh
 */
@Test(testName = "Keyed Single Object Pool Tests")
public class KeyedSingleObjectPoolTest extends
		AbstractPoolTest<IKeyedObjectPool.Single<String, String>> {

	/**
	 * Instantiates a new keyed single object pool test.
	 */
	public KeyedSingleObjectPoolTest() {
		super(1);
	}

	/**
	 * Wait for object with timeout test.
	 * 
	 * @throws Exception
	 *           the exception
	 */
	@Test(dependsOnMethods = { "singleBorrowAndRelease" })
	public void waitForObjectWithTimeoutTest() throws Exception {
		IPooledObject<String> obj = pool().borrow(POOL_KEY);
		try {
			ExecutorService es = Executors.newSingleThreadExecutor();
			Future<?> f = es.submit(new Runnable() {
				public void run() {
					try {
						IPooledObject<String> obj = pool().borrow(POOL_KEY, 500,
								TimeUnit.MILLISECONDS);
						obj.release();
						fail("Object was obtained");
					} catch (Exception e) {
						assertTrue(e instanceof TimeoutException);
					}
				}

			});

			// Test that the main thread can still acquire the object since it owns
			// the contract and hasn't releasd it yet
			assertNotNull(pool().borrow(POOL_KEY));

			// block until thread is done
			f.get();
		} finally {
			obj.release();
		}
	}

	/**
	 * Many threads blocking until obtained.
	 * 
	 * @throws Exception
	 *           the exception
	 */
	@Test(dependsOnMethods = { "waitForObjectWithTimeoutTest" })
	public void manyThreadsBlockingUntilObtained() throws Exception {
		Runnable r = new Runnable() {
			public void run() {
				IPooledObject<String> obj = null;
				try {
					obj = pool().borrow(POOL_KEY);
					assertNotNull(obj);
					Thread.sleep(20);
					assertNotNull(pool().borrow(POOL_KEY));
				} catch (Exception e) {
					fail("Failed to obtain Object: " + Thread.currentThread().getName());
				} finally {
					if (obj != null)
						obj.release();
				}
			}
		};
		ExecutorService es = Executors.newFixedThreadPool(10);
		for (int i = 0; i < 10; i++)
			es.submit(r);
		es.shutdown();
		es.awaitTermination(2, TimeUnit.SECONDS);
		verifyMetrics();
	}

	@Test
	public void singleBorrowAndRelease() throws Exception {
		IPooledObject<String> obj = pool().borrow(POOL_KEY);
		assertNotNull(obj);
		pool().release(obj);
		verifyMetrics();
	}

	@Test(dependsOnMethods = { "manyThreadsBlockingUntilObtained" })
	public void verifyMetrics() {
		PoolMetrics<String> metrics = pool().getPoolMetrics();
		assertNotNull(metrics);
		assertEquals(metrics.getBorrowedCount(), 0);
		assertEquals(metrics.getWaitingCount(), 0);
		assertEquals(metrics.getKeyCount(), 1);
	}

	/**
	 * Tests shutting down the pool and not allowing any more allocations
	 */
	@Test(dependsOnMethods = { "verifyMetrics" }, expectedExceptions = { IllegalStateException.class })
	public void testShutdown() {
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
	 * Tests the Factory Lifecycle to insure the pool is calling factory during
	 * each phase within the objects lifecycle
	 * 
	 * @throws Exception
	 */
	@Test
	public void testObjectLifecycle() throws Exception {
		TestLifecycleFactory factory = new TestLifecycleFactory();
		IKeyedObjectPool<String, Boolean> pool = Pools.createPool(factory);

		IPooledObject<Boolean> obj = pool.borrow(POOL_KEY);
		obj.release();
		obj = pool.borrow(POOL_KEY);
		obj.invalidate();

		assertEquals(factory.lifecycleCount, 4);
	}

	static class TestLifecycleFactory implements
			IPoolObjectFactory<String, Boolean> {
		int lifecycleCount;

		public Boolean create(PoolKey<String> key) {
			lifecycleCount++;
			return true;
		}

		public void activate(Boolean object) {
			lifecycleCount++;
		}

		public void passivate(Boolean object) {
			lifecycleCount++;
		}

		public void destroy(Boolean object) {
			lifecycleCount++;
		}
	}
}
