package il.technion.ewolf.tinyLFU.LFU;

import il.technion.ewolf.kbr.Node;

import java.util.List;

public interface LFUCache<T> {
	public T getLeastFrequentlyUsed();
	public void RemoveMin();
	public void Increment(T item);
	public void insert(T item, List<Node> Data);
	public List<Node> search(T item);
	public List<Node> NoAddSearch(T key);
	public void clear();
	public boolean isNeeded(T item);
	public String getName();
}
