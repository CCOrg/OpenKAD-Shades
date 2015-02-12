package il.technion.ewolf.tinyLFU.filter;

public interface Histogram<T> {

	// make room for new stuff...
	// we divide all the values by two so future guesses will return smaller results.
	public abstract void Reset();

	public abstract boolean add(T element);

	public abstract int howMany(T element);

	public abstract void clear();

}