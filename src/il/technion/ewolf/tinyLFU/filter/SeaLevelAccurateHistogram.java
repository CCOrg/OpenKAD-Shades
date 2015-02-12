package il.technion.ewolf.tinyLFU.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SeaLevelAccurateHistogram<T> implements Histogram<T>{

	private final Map<T,Integer> histogram;
	private final int insertsPerReset;
	private int actualInserts;
	public SeaLevelAccurateHistogram(int ibr)
	{	
		this.insertsPerReset = ibr;
		histogram = new HashMap<T,Integer>();
	}
	@Override
	public void Reset() {
		
		//System.out.println("Before: "+ histogram.keySet().size());
		List<T> toDelete = new ArrayList<T>();
		for (T key : this.histogram.keySet()) {
			if(histogram.get(key)>1)
			{
				histogram.put(key, histogram.get(key)-1);
			}
			else
			{
				toDelete.add(key);
			}
		}
		for (T key : toDelete) {
			histogram.remove(key);
		}
		//System.out.println("After: "+ histogram.keySet().size());

		
	}

	@Override
	public boolean add(T element) {
		
		Integer score = howMany(element);
		if(score<36)
			score++;
		histogram.put(element, score);
		actualInserts++;
		if(actualInserts>insertsPerReset)
		{
			actualInserts/=2;
			this.Reset();
			return true;
		}
		return false;
	}

	@Override
	public int howMany(T element) {
		Integer score = histogram.get(element);
		if(score == null)
			score = 0;
		return score;
	}
	@Override
	public void clear() {
		histogram.clear();
	}

}
