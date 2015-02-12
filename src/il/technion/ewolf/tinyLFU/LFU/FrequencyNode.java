package il.technion.ewolf.tinyLFU.LFU;

import java.util.ArrayList;
import java.util.List;

public class FrequencyNode<T> implements Comparable<FrequencyNode<T>>
{
	private int frequency;
	private final List<T> entries;
	public boolean isEmpty()
	{
		return this.entries.isEmpty();
	}
	
	public int getFrequency() {
		return frequency;
	}
	public T getFirstEntry()
	{
		if(entries.isEmpty())
			return null;
		return entries.get(0);
		
	}
	public FrequencyNode<T> addEntry(T entry)
	{
		this.entries.add(entry);
		return this;
	}
	
	public T removeEntry(T entry)
	{
		this.entries.remove(entry);
		return entry;
	}
	
	
	
	FrequencyNode(int frequency)
	{
		entries = new ArrayList<T>();
		this.frequency = frequency;
	}
	public FrequencyNode<T> setFrequency(int frequency) {
		this.frequency = frequency;
		return this;
	}

	@Override
	public int compareTo(FrequencyNode<T> arg0) {
		
		return new Integer(this.frequency).compareTo(arg0.frequency);
	}
}
