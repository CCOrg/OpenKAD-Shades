package il.technion.ewolf.tinyLFU.LFU;

import il.technion.ewolf.kbr.Node;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class InMemoryLfu<T> implements LFUCache<T> {

	// dependencies
	private final int size;
	private final int kBucketSize;

	private int elements;
	int sample = 0;
	// state
	private final TreeSet<InMemoryLfuCacheEntry<T>> cache;
	private final Map<T, InMemoryLfuCacheEntry<T>> entryFromKey;

	@Inject
	public InMemoryLfu(@Named("openkad.bucket.kbuckets.maxsize") final int kBucketSize, @Named("openkad.cache.size") final int size) {
		this.size = size;
		this.kBucketSize = kBucketSize;
		this.entryFromKey = new HashMap<T, InMemoryLfuCacheEntry<T>>();
		this.cache = new TreeSet<InMemoryLfuCacheEntry<T>>();
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
		ChangeValue(item, 1);
	}

	public void ChangeValue(final T item, final int delta) {
		final InMemoryLfuCacheEntry<T> entry = this.entryFromKey.get(item);
		if (entry == null)
			return;
		this.cache.remove(entry);
		entry.touch(delta);
		this.cache.add(entry);
	}

	@Override
	public void insert(final T item, final List<Node> Data) {
		insert(item, Data, 0);

	}
	public void insert(final T item, final List<Node> Data, final int initialScore) {
		this.sample++;

		InMemoryLfuCacheEntry<T> entry = this.entryFromKey.get(item);
		if (entry == null) {

			entry = new InMemoryLfuCacheEntry<T>(Data, item, initialScore);
			cache.add(entry);
			this.entryFromKey.put(item, entry);

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
		final InMemoryLfuCacheEntry<T> entry = this.entryFromKey.get(item);
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
		return false;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "InMemoryLFU";
	}

}