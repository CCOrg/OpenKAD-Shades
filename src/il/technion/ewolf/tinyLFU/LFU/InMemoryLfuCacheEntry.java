package il.technion.ewolf.tinyLFU.LFU;

import il.technion.ewolf.kbr.Node;
import java.util.List;

public class InMemoryLfuCacheEntry<T> implements Comparable<InMemoryLfuCacheEntry<T>>{
	private final List<Node> nodes;
	private final T key;
	private  Integer Count;
	InMemoryLfuCacheEntry(List<Node> nodes, T key,int Guess) {
		this.nodes = nodes;
		this.key = key;
		this.Count = Guess;
	}
	
	public List<Node> getNodes() {
		return nodes;
	}
	
	public T getKey() {
		return key;
	}
	
	public int getCount()
	{
		return this.Count;
	}
	public InMemoryLfuCacheEntry<T> touch(int howMuch)
	{
			this.Count+=howMuch;
		return this;
	}
	
	
	@Override
	public int compareTo(InMemoryLfuCacheEntry<T> o) {
		
		int j = Count.compareTo(o.Count);
		if(j!= 0)
			return j;
		return new Integer(key.hashCode()).compareTo(o.key.hashCode());
		
	}

	
}
