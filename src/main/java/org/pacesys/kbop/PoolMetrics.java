package org.pacesys.kbop;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * Provides a metrics snapshot for a Pool
 * 
 * @author Jeremy Unruh
 */
public class PoolMetrics<K> implements Serializable {

  private static final long serialVersionUID = -2325874226714753991L;

  private int borrowedCount;
  private int waitingCount;
  private int maxObjectsPerKey;
  private int keyCount;
  private Date collectedDate;

  /**
   * Instantiates a new pool metrics.
   *
   * @param borrowedCount the borrowed count
   * @param waitingCount the waiting count
   * @param maxObjectsPerKey the max objects per key
   * @param keyCount the key count
   */
  public PoolMetrics(int borrowedCount, int waitingCount, int maxObjectsPerKey, int keyCount) {
	super();
	this.borrowedCount = borrowedCount;
	this.waitingCount = waitingCount;
	this.maxObjectsPerKey = maxObjectsPerKey;
	this.keyCount = keyCount;
	this.collectedDate = new Date();
  }

  /**
   * Gets the borrowed count.
   *
   * @return the borrowed count
   */
  public int getBorrowedCount() {
	return this.borrowedCount;
  }

  /**
   * Gets the waiting count.
   *
   * @return the waiting count
   */
  public int getWaitingCount() {
	return this.waitingCount;
  }

  /**
   * Gets the max objects per key.
   *
   * @return the max objects per key
   */
  public int getMaxObjectsPerKey() {
	return this.maxObjectsPerKey;
  }

  /**
   * Gets the key count.
   *
   * @return the key count
   */
  public int getKeyCount() {
	return this.keyCount;
  }

  /**
   * The date the metrics were collected
   * @return Metric collection date
   */
  public Date getCollectedDate() {
	return this.collectedDate;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
	return "PoolMetrics [collectedDate=" + this.collectedDate + ", borrowedCount=" + this.borrowedCount
		+ ", waitingCount=" + this.waitingCount + ", keyCount=" + this.keyCount + ", maxObjectsPerKey="
		+ this.maxObjectsPerKey + "]";
  }
  
  /**
   * Extends Pool Metrics providing extended Per-Key metrics
   */
  public static class PoolMultiMetrics<K> extends PoolMetrics<K> {

		private static final long serialVersionUID = 5188832983690021017L;
	  private Map<PoolKey<K>, KeyMetric> keyMetrics;

		public PoolMultiMetrics(int borrowedCount, int waitingCount, int maxObjectsPerKey, Map<PoolKey<K>, KeyMetric> keyMetrics) {
			super(borrowedCount, waitingCount, maxObjectsPerKey, keyMetrics.size());
			this.keyMetrics = keyMetrics;
		}
		
		 /**
	   * Only Object Pools with a maxItemsPerKey > 1 will populate Key Metrics.  Single Key to Object Pools do not populate this call
	   * so null is returned.
	   * @return Key Metric if this is a Multi Object Pool and the Key exists otherwise null
	   * @see #hasMetricsForKey(PoolKey)
	   */
	  public KeyMetric getKeyMetrics(PoolKey<K> key) {
		if (keyMetrics != null)
		  return keyMetrics.get(key);
		return null;
	  }

	  /**
	   * Determines if metrics have been populated for the specified Key
	   * @param key the Pool Key to query metrics for
	   * @return true if metrics are available for the given {@code key}
	   */
	  public boolean hasMetricsForKey(PoolKey<K> key) {
		return (keyMetrics != null && keyMetrics.containsKey(key));
	  }
  	
  }

  public static class KeyMetric implements Serializable {

	private static final long serialVersionUID = 916100737260197225L;
	private int allocationSize;
	private int borrowedCount;
	private int waitingCount;

	public KeyMetric(int allocationSize, int borrowedCount, int waitingCount) {
	  super();
	  this.allocationSize = allocationSize;
	  this.borrowedCount = borrowedCount;
	  this.waitingCount = waitingCount;
	}

	public int getAllocationSize() {
	  return this.allocationSize;
	}

	public int getBorrowedCount() {
	  return this.borrowedCount;
	}

	public int getWaitingCount() {
	  return this.waitingCount;
	}

	@Override
	public String toString() {
	  return "KeyMetric [allocationSize=" + this.allocationSize + ", borrowedCount=" + this.borrowedCount
		  + ", waitingCount=" + this.waitingCount + "]";
	}

  }

}
