package il.technion.ewolf.tinyLFU.LFU;

import il.technion.ewolf.kbr.Node;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class WindowLFU<T> implements LFUCache<T> {
	private final InMemoryLfu<T> cache;
	private final List<T> Window;
	private final int WindowSize;

	private final Map<T, Integer> Histogram;

	@Inject
	public WindowLFU(@Named("openkad.bucket.kbuckets.maxsize") final int kBucketSize, @Named("openkad.cache.size") final int size,
			@Named("openkad.cache.windowsize") final int windowsize) {
		this.cache = new InMemoryLfu<T>(kBucketSize, size);
		this.Window = new LinkedList<T>();
		this.WindowSize = windowsize;
		this.Histogram = new HashMap<T, Integer>();
	}

	@Override
	public T getLeastFrequentlyUsed() {

		return this.cache.getLeastFrequentlyUsed();
	}

	@Override
	public void RemoveMin() {
		this.cache.RemoveMin();

	}

	@Override
	public void Increment(final T item) {
		this.cache.Increment(item);

	}

	@Override
	public void insert(final T item, final List<Node> Data) {
		if (this.cache.search(item) != null)
			return;

		Integer Candidatescore = this.Histogram.get(item);
		if (Candidatescore == null)
			Candidatescore = 0;
		this.cache.insert(item, Data, Candidatescore);
		Window.add(item);

		Integer score = Histogram.remove(item);
		if (score == null)
			score = 0;
		score++;
		Histogram.put(item, score);

		if (Window.size() > this.WindowSize) {
			final T key = Window.remove(0);
			score = Histogram.remove(key);
			if (score == null)
				score = 0;
			if (score > 0)
				score--;
			Histogram.put(key, score);
			cache.ChangeValue(key, -1);
		}

	}

	@Override
	public List<Node> search(final T item) {

		return cache.search(item);
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
		return true;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "WindowLFU";
	}

}
