package il.technion.ewolf.tinyLFU.filter;

import il.technion.ewolf.BloomFilters.Analysis.BloomBlock;
import il.technion.ewolf.BloomFilters.TestItems.BloomFilter;

import com.google.inject.Inject;
import com.google.inject.name.Named;

//TODO: change implementation.

public class TinyLfuFilter<T> extends BloomBlock<T> implements Histogram<T> {

	private static final long serialVersionUID = -7500467431104532953L;

	private final int numberOfInsertsBeforeReset;
	private int actualInserts;
	private final int DoorKeeper;
	public final BloomFilter<T> KeeperFilter;

	@Inject
	public TinyLfuFilter(@Named("tinyLfu.guesser.size") final int size, @Named("tinyLfu.windowSize") final int ibr,
			@Named("tinyLfu.falsepositive") final double fp, @Named("tinyLfu.doorkeeperSize") final int doorkeeperSize) {
		super(fp, size);

		this.numberOfInsertsBeforeReset = ibr;
		this.actualInserts = 0;
		this.DoorKeeper = doorkeeperSize;
		this.KeeperFilter = new BloomFilter<T>(fp, this.DoorKeeper);
		// final int max = (int) (Math.ceil(ibr / cachSize));
		// this.setMaxCount(max);

	}

	// make room for new stuff...
	// we divide all the values by two so future guesses will return smaller
	// results.
	/*
	 * (non-Javadoc)
	 * 
	 * @see il.technion.ewolf.vision.filter.Histogram#Reset()
	 */
	public synchronized int getSize() {
		int histogramSize = super.bitSetSize;

		histogramSize *= Math.ceil(Math.log(this.getMaxCount() + 128) / Math.log(2));
		// System.out.println("Bits per counter: "+Math.ceil(Math.log(MaxCount+128)/Math.log(2)));
		histogramSize += this.KeeperFilter.getBitSet().size();
		return histogramSize / 8;
	}
	@Override
	public synchronized void Reset() {
		this.KeeperFilter.clear();
		this.actualInserts /= 2;
		for (int i = 0; i < this.bitSetSize; i++)
			this.counterArray[i] /= 2;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see il.technion.ewolf.vision.filter.Histogram#add(T)
	 */
	@Override
	public synchronized boolean add(final T element) {
		boolean reset = false;

		this.actualInserts++;
		// to keep precision. - if there are to many inserts we simply divide
		// all the numbers by half.
		// it is an expensive operation though - so a large number of inserts
		// should be selected.
		if (this.numberOfInsertsBeforeReset < this.actualInserts) {
			// System.out.println("Reseting!!!");
			this.Reset();
			reset = true;
		}
		// we deal with first timers differently.
		if (!this.KeeperFilter.contains(element)) {
			this.KeeperFilter.add(element);
			return reset;
		}
		super.add(element);
		return reset;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see il.technion.ewolf.vision.filter.Histogram#Guess(T)
	 */
	@Override
	public synchronized int howMany(final T element) {
		if (element == null)
			return 0;
		int additional = 0;
		if (this.KeeperFilter.contains(element))
			additional = 1;
		return additional + super.guess(element);
	}

}
