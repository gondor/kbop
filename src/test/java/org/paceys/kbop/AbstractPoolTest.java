package org.paceys.kbop;

import org.pacesys.kbop.IKeyedObjectPool;
import org.pacesys.kbop.IPoolObjectFactory;
import org.pacesys.kbop.PoolKey;
import org.pacesys.kbop.Pools;


/**
 * Abstract Test Case for Pooled Objects
 * 
 * @author Jeremy Unruh
 */
public abstract class AbstractPoolTest<P extends IKeyedObjectPool<String,String>> {

  static PoolKey<String> POOL_KEY = PoolKey.lookup("Test");
  static PoolKey<String> POOL_KEY2 = PoolKey.lookup("Test2");

  private P pool;


  @SuppressWarnings("unchecked")
	public AbstractPoolTest(int maxItemsPerKey) {
	super();
	IPoolObjectFactory<String, String> factory = new IPoolObjectFactory<String, String>() {
	  public String create(PoolKey<String> key) {
		return "This is a Test : " + key.get();
	  }

	  @Override
	  public void activate(String object) {
	  }

	  @Override
	  public void passivate(String object) {
	  }

	  @Override
	  public void destroy(String object) {
	  }
	};
	this.pool = (P) ((maxItemsPerKey > 1) ? Pools.createMultiPool(factory, maxItemsPerKey) : Pools.createPool(factory));
  }

  P pool() {
	return pool;
  }

}
