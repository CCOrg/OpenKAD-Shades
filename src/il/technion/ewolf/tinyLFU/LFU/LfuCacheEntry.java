package il.technion.ewolf.tinyLFU.LFU;

import il.technion.ewolf.kbr.Node;

import java.util.List;

public class LfuCacheEntry<T> implements Comparable<LfuCacheEntry<T>>{
	private final List<Node> nodes;
	private final T key;
	private  Byte Count;
	LfuCacheEntry(List<Node> nodes, T key,Byte Guess) {
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
	
	public byte getCount()
	{
		return this.Count;
	}
	public LfuCacheEntry<T> touch()
	{
		if(this.Count<Byte.MAX_VALUE)
			this.Count++;
		return this;
	}
	
	public LfuCacheEntry<T> reset()
	{
		int num = this.Count + 128;
		num/=2;
		this.Count= (byte)(this.Count- num);
		return this;
	}

	@Override
	public int compareTo(LfuCacheEntry<T> o) {
		
		int j = Count.compareTo(o.Count);
		if(j!= 0)
			return j;
		return new Integer(key.hashCode()).compareTo(o.key.hashCode());
		
	}

	
}
