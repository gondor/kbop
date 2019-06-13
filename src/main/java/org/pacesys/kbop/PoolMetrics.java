package org.pacesys.kbop;

import lombok.Value;
import lombok.experimental.NonFinal;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * Provides a metrics snapshot for a Pool
 * 
 * @author Jeremy Unruh
 */
@NonFinal@Value
public class PoolMetrics implements Serializable {

	private static final long serialVersionUID = -2325874226714753991L;

	private final int borrowedCount;
	private final int waitingCount;
	private final int maxObjectsPerKey;
	private final int keyCount;
	private final Date collectedDate = new Date();
	
	/**
	 * Extends Pool Metrics providing extended Per-Key metrics
	 */
	public static class PoolMultiMetrics<K> extends PoolMetrics {

		private static final long serialVersionUID = 5188832983690021017L;
		private final Map<PoolKey<K>, KeyMetric> keyMetrics;

		public PoolMultiMetrics(int borrowedCount, int waitingCount, int maxObjectsPerKey, Map<PoolKey<K>, KeyMetric> keyMetrics) {
			super(borrowedCount, waitingCount, maxObjectsPerKey, keyMetrics.size());
			this.keyMetrics = keyMetrics;
		}

		/**
		 * Only Object Pools with a maxItemsPerKey > 1 will populate Key Metrics.  Single Key to Object Pools do not populate this call
		 * so null is returned.
		 * @return Key Metric if this is a Multi Object Pool and the Key exists otherwise null
		 * @see #hasMetricsForKey(Object)
		 */
		@Nullable
		public KeyMetric getKeyMetrics(K key) {
			if (keyMetrics != null)
				return keyMetrics.get(new PoolKey<>(key));
			return null;
		}

		/**
		 * Determines if metrics have been populated for the specified Key
		 * @param key the Pool Key to query metrics for
		 * @return true if metrics are available for the given {@code key}
		 */
		public boolean hasMetricsForKey(K key) {
			return keyMetrics != null && keyMetrics.containsKey(new PoolKey<>(key));
		}

	}

	@Value
	public static class KeyMetric implements Serializable {
		private static final long serialVersionUID = 916100737260197225L;
		private final int allocationSize;
		private final int borrowedCount;
		private final int waitingCount;
	}
}