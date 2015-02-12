package il.technion.ewolf.tinyLFU.LFU;

import il.technion.ewolf.kbr.Node;
import il.technion.ewolf.tinyLFU.filter.Histogram;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class LazyLFU<T> implements LFUCache<T> {

	// dependencies
	private final int size;
	private final Histogram<T> LFUFilter;
	private int fingerIndex = -1;
	// state
	private final ArrayList<LfuCacheEntry<T>> cache;
	private final Map<T, LfuCacheEntry<T>> entryFromKey;
	private T lfuApprox = null;
	private int lfuApproxRate = 1000;

	@Inject
	public LazyLFU(@Named("openkad.cache.size") final int size, @Named("tinyLfu.histogram") final Histogram<T> lfufilter) {
		this.size = size;
		this.entryFromKey = new HashMap<T, LfuCacheEntry<T>>();
		this.cache = new ArrayList<LfuCacheEntry<T>>();
		this.LFUFilter = lfufilter;
	}

	@Override
	public synchronized T getLeastFrequentlyUsed() {

		if (this.cache.size() < this.size)
			return null;
		// check if is empty is needed.

		this.fingerIndex = (this.fingerIndex + 1) % this.size;

		final T candidate = this.cache.get(this.fingerIndex).getKey();

		if (this.lfuApprox == null) {
			this.lfuApprox = candidate;
			this.lfuApproxRate = this.LFUFilter.howMany(candidate);
			return this.lfuApprox;
		}

		final int candidateRate = this.LFUFilter.howMany(candidate);
		if (candidateRate < this.lfuApproxRate) {
			this.lfuApprox = candidate;
			this.lfuApproxRate = candidateRate;
		}
		return this.lfuApprox;

	}

	@Override
	public synchronized void RemoveMin() {

	}

	@Override
	public void Increment(final T item) {

	}

	@Override
	public synchronized void insert(final T item, final List<Node> Data) {
		this.LFUFilter.add(item);
		LfuCacheEntry<T> entry = this.entryFromKey.get(item);
		// item is not in cache.
		if (entry == null) {
			entry = new LfuCacheEntry<T>(Data, item, Byte.MIN_VALUE);
			if (this.cache.size() < this.size) {
				this.cache.add(entry);
				this.entryFromKey.put(item, entry);
			} else {
				// Assert.assertEquals(this.cache.size(), this.size);
				final int newItemFreq = this.LFUFilter.howMany(item);
				final T itemToRemove = this.getLeastFrequentlyUsed();

				// final double oldItemFreq = this.oracle.howMany(itemToRemove);
				if (newItemFreq > this.lfuApproxRate) {
					this.lfuApprox = item;
					this.lfuApproxRate = newItemFreq;
					this.cache.remove(this.entryFromKey.get(itemToRemove));
					this.entryFromKey.remove(itemToRemove);
					this.cache.add(entry);
					this.entryFromKey.put(item, entry);
					return;
				}
			}
		} else if (item == this.lfuApprox)
			this.lfuApproxRate++;

	}

	@Override
	public synchronized List<Node> search(final T item) {
		this.LFUFilter.add(item);
		return NoAddSearch(item);
	}

	@Override
	public synchronized List<Node> NoAddSearch(final T item) {
		final LfuCacheEntry<T> entry = this.entryFromKey.get(item);
		if (entry == null)
			return null;
		return entry.getNodes();
	}

	@Override
	public synchronized void clear() {
		this.cache.clear();
		this.entryFromKey.clear();
		this.LFUFilter.clear();
	}

	@Override
	public boolean isNeeded(final T item) {
		if (cache.size() < size)
			return true;
		return LFUFilter.howMany(item) > this.lfuApproxRate;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "LFU_Clock";
	}

}