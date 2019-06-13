package org.paceys.kbop;

import org.junit.Test;
import org.pacesys.kbop.IKeyedObjectPool;
import org.pacesys.kbop.IPoolObjectFactory;
import org.pacesys.kbop.IPooledObject;
import org.pacesys.kbop.PoolKey;
import org.pacesys.kbop.PoolMetrics;
import org.pacesys.kbop.Pools;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class KeyedSingleObjectPoolTest extends AbstractPoolTest<IKeyedObjectPool.Single<String, String>> {
	
	/**
	 * Instantiates a new keyed single object pool test.
	 */
	public KeyedSingleObjectPoolTest() {
		super(1);
	}
	
	@Test
	public void waitForObjectWithTimeoutTest() throws Exception {
		singleBorrowAndRelease();
		
		IPooledObject<String, String> obj = pool().borrow(POOL_KEY);
		try {
			ExecutorService es = Executors.newSingleThreadExecutor();
			Future<?> f = es.submit(new Runnable() {
				public void run() {
					try {
						IPooledObject<String, String> obj = pool().borrow(POOL_KEY, 500,
								TimeUnit.MILLISECONDS);
						obj.release();
						fail("Object was obtained");
					} catch (Exception e) {
						assertThat(e instanceof TimeoutException).isTrue();
					}
				}
				
			});
			
			// Test that the main thread can still acquire the object since it owns
			// the contract and hasn't releasd it yet
			assertThat(pool().borrow(POOL_KEY)).isNotNull();
			
			// block until thread is done
			f.get();
		} finally {
			obj.release();
		}
	}
	
	@Test
	public void manyThreadsBlockingUntilObtained() throws Exception {
		waitForObjectWithTimeoutTest();
		
		Runnable r = new Runnable() {
			public void run() {
				IPooledObject<String, String> obj = null;
				try {
					obj = pool().borrow(POOL_KEY);
					assertThat(obj).isNotNull();
					Thread.sleep(20);
					assertThat(pool().borrow(POOL_KEY)).isNotNull();
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
		IPooledObject<String, String> obj = pool().borrow(POOL_KEY);
		assertThat(obj).isNotNull();
		pool().release(obj);
		verifyMetrics();
	}
	
	private void verifyMetrics() {
		PoolMetrics metrics = pool().getPoolMetrics();
		assertThat(metrics).isNotNull();
		assertThat(metrics.getBorrowedCount()).isZero();
		assertThat(metrics.getWaitingCount()).isZero();
		assertThat(metrics.getKeyCount()).isEqualTo(1);
	}
	
	@Test
	public void testMetrics() throws Exception {
		manyThreadsBlockingUntilObtained();
		
		PoolMetrics metrics = pool().getPoolMetrics();
		assertThat(metrics).isNotNull();
		assertThat(metrics.getBorrowedCount()).isZero();
		assertThat(metrics.getWaitingCount()).isZero();
		assertThat(metrics.getKeyCount()).isEqualTo(1);
	}
	
	/**
	 * Tests shutting down the pool and not allowing any more allocations
	 */
	@Test(expected = IllegalStateException.class)
	public void testShutdown() throws Exception {
		testMetrics();
		
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
	 * Tests the Factory Lifecycle to insure the pool is calling factory during each phase within the objects lifecycle
	 */
	@Test
	public void testObjectLifecycle() throws Exception {
		TestLifecycleFactory factory = new TestLifecycleFactory();
		IKeyedObjectPool<String, Boolean> pool = Pools.createPool(factory);
		
		IPooledObject<Boolean, String> obj = pool.borrow(POOL_KEY);
		obj.release();
		obj = pool.borrow(POOL_KEY);
		obj.invalidate();
		
		assertThat(factory.lifecycleCount).isEqualTo(4);
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
