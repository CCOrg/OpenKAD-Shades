package il.technion.ewolf.tinyLFU.LFU;

import il.technion.ewolf.kbr.Node;
import il.technion.ewolf.tinyLFU.filter.Histogram;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class TinyLfuCache<T> implements LFUCache<T> {

	// dependencies
	private final int size;
	private final Histogram<T> histogram;
	private final AtomicInteger cacheTreshold;
	// state
	private TreeSet<LfuCacheEntry<T>> cache;
	private final Map<T, LfuCacheEntry<T>> entryFromKey;

	@Inject
	public TinyLfuCache(@Named("openkad.shades.cachetreshold") final AtomicInteger treshold,
			@Named("openkad.cache.size") final int size, @Named("tinyLfu.histogram") final Histogram<T> histogram) {
		this.size = size;
		this.entryFromKey = new HashMap<T, LfuCacheEntry<T>>();
		this.cache = new TreeSet<LfuCacheEntry<T>>();
		this.histogram = histogram;
		this.cacheTreshold = treshold;

	}
	public synchronized int howmany(final T key) {
		return this.histogram.howMany(key);
	}

	@Override
	public synchronized T getLeastFrequentlyUsed() {
		if (this.cache.isEmpty())
			return null;
		return this.cache.first().getKey();
	}

	@Override
	public synchronized void RemoveMin() {
		this.cache.remove(this.cache.first());
	}

	@Override
	public synchronized void Increment(final T item) {
		final LfuCacheEntry<T> entry = this.entryFromKey.get(item);
		if (entry == null)
			return;
		this.cache.remove(entry);
		entry.touch();
		this.cache.add(entry);
	}

	@Override
	public synchronized void insert(final T item, final List<Node> Data) {
		LfuCacheEntry<T> entry = this.entryFromKey.get(item);
		addToHistogram(item);

		if (entry == null) // not in cache
		{
			final int EntryGuess = this.histogram.howMany(item);
			int LFUGuess = 0;
			if (!this.cache.isEmpty()) {
				LFUGuess = this.histogram.howMany(this.cache.first().getKey());
				this.cacheTreshold.set(LFUGuess);
			}
			if (this.cache.size() < this.size || EntryGuess > LFUGuess) {
				entry = new LfuCacheEntry<T>(Data, item, (byte) (EntryGuess - 128));
				this.cache.add(entry);
				this.entryFromKey.put(item, entry);
			}

			if (this.cache.size() > this.size) {
				entry = this.cache.first();
				this.entryFromKey.remove(entry.getKey());
				this.cache.remove(entry);
			}
		} else
			Increment(item);
	}

	private synchronized void addToHistogram(final T item) {
		if (this.histogram.add(item)) {

			final TreeSet<LfuCacheEntry<T>> newCache = new TreeSet<LfuCacheEntry<T>>();
			for (final LfuCacheEntry<T> e : this.cache) {
				final LfuCacheEntry<T> newEntry = new LfuCacheEntry<T>(e.getNodes(), e.getKey(), e.getCount());
				newEntry.reset();
				newCache.add(newEntry);
				this.entryFromKey.remove(e.getKey());
				this.entryFromKey.put(e.getKey(), newEntry);

			}
			this.cache = newCache;
		}
	}

	@Override
	public synchronized List<Node> search(final T item) {
		Increment(item);
		addToHistogram(item);

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
		this.histogram.clear();
	}

	@Override
	public synchronized boolean isNeeded(final T item) {

		if (this.cache.size() < this.size)
			return true;
		final int EntryGuess = this.histogram.howMany(item);
		final int LFUGuess = this.histogram.howMany(this.cache.first().getKey());
		// System.out.println("TinyLfuCache- EntryGuess: " + EntryGuess +
		// " LFUGuess: " + LFUGuess);
		// System.out.println("TinyLfuCache- notFull: " + (cache.size() <
		// size));
		// System.out.println("TinyLfuCache- needed: " + (EntryGuess >
		// LFUGuess));
		return EntryGuess > LFUGuess;
	}

	public synchronized boolean isFull() {
		return (this.cache.size() == this.size);
	}
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "TLFU";
	}

}