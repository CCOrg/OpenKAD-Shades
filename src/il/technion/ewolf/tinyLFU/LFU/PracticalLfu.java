package il.technion.ewolf.tinyLFU.LFU;

import il.technion.ewolf.kbr.Node;
import il.technion.ewolf.tinyLFU.filter.Histogram;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class PracticalLfu<T> implements LFUCache<T> {

	// dependencies
	private final int size;
	private final int kBucketSize;
	private final Histogram<T> oracle;
	private int elements;
	// state
	private TreeSet<LfuCacheEntry<T>> cache;
	private final Map<T, LfuCacheEntry<T>> entryFromKey;

	@Inject
	public PracticalLfu(@Named("openkad.bucket.kbuckets.maxsize") final int kBucketSize,
			@Named("openkad.cache.size") final int size, @Named("tinyLfu.histogram") final Histogram<T> rgbf) {
		this.size = size;
		this.kBucketSize = kBucketSize;
		this.entryFromKey = new HashMap<T, LfuCacheEntry<T>>();
		this.cache = new TreeSet<LfuCacheEntry<T>>();
		this.oracle = rgbf;
	}

	@Override
	public T getLeastFrequentlyUsed() {
		if (cache.isEmpty())
			return null;
		return cache.first().getKey();
	}

	@Override
	public void RemoveMin() {
		cache.remove(cache.first());
	}

	@Override
	public void Increment(final T item) {
		final LfuCacheEntry<T> entry = this.entryFromKey.get(item);
		if (entry == null)
			return;
		this.cache.remove(entry);
		entry.touch();
		this.cache.add(entry);
	}

	@Override
	public void insert(final T item, final List<Node> Data) {

		if (this.oracle.add(item)) {
			// System.out.print("reset!");
			final TreeSet<LfuCacheEntry<T>> newCache = new TreeSet<LfuCacheEntry<T>>();
			for (final LfuCacheEntry<T> e : cache) {

				final LfuCacheEntry<T> newEntry = new LfuCacheEntry<T>(e.getNodes(), e.getKey(), e.getCount());
				newEntry.reset();
				newCache.add(newEntry);
				this.entryFromKey.remove(e.getKey());
				this.entryFromKey.put(e.getKey(), newEntry);
			}
			this.cache = newCache;
		}

		LfuCacheEntry<T> entry = this.entryFromKey.get(item);
		if (entry == null) {
			final int EntryGuess = this.oracle.howMany(item);
			int LFUGuess = 0;
			if (!cache.isEmpty())
				LFUGuess = this.oracle.howMany(cache.first().getKey());
			if (cache.size() < this.size || EntryGuess > LFUGuess) {
				entry = new LfuCacheEntry<T>(Data, item, (byte) (EntryGuess - 128));
				cache.add(entry);
				this.entryFromKey.put(item, entry);
			}

			if (cache.size() > this.size) {
				entry = cache.first();
				this.entryFromKey.remove(entry.getKey());
				this.cache.remove(entry);
			}
		} else
			Increment(item);
	}

	@Override
	public List<Node> search(final T item) {
		final LfuCacheEntry<T> entry = this.entryFromKey.get(item);
		if (entry == null)
			return null;
		return entry.getNodes();
	}

	@Override
	public List<Node> NoAddSearch(final T key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isNeeded(final T item) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "oldTinyLFU";
	}
}