package il.technion.ewolf.tinyLFU.LFU;

import il.technion.ewolf.kbr.Node;

import java.util.List;

public class DummyCache<T> implements LFUCache<T> {
	public DummyCache() {

	}

	@Override
	public T getLeastFrequentlyUsed() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void RemoveMin() {
		// TODO Auto-generated method stub

	}

	@Override
	public void Increment(final T item) {
		// TODO Auto-generated method stub

	}

	@Override
	public void insert(final T item, final List<Node> Data) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<Node> search(final T item) {
		// TODO Auto-generated method stub
		return null;
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
		return "dummy";
	}

}
