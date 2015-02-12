/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package il.technion.ewolf.tinyLFU.filter;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.TreeSet;

/**
 * Implementation of a Bloom-filter, as described here:
 * http://en.wikipedia.org/wiki/Bloom_filter
 * 
 * Inspired by the SimpleBloomFilter-class written by Ian Clarke. This
 * implementation provides a more evenly distributed Hash-function by using a
 * proper digest instead of the Java RNG. Many of the changes were proposed in
 * comments in his blog:
 * http://blog.locut.us/2008/01/12/a-decent-stand-alone-java
 * -bloom-filter-implementation/
 * 
 * @param <E>
 *            Object type that is to be inserted into the Bloom filter, e.g.
 *            String or Integer.
 * @author Magnus Skjegstad <magnus@skjegstad.com>
 */

// Gil: Extended the bloom filter in order to support my ScoreBored
// construction.
// in this construction, instead of simple bloom filter, I use a variant of
// Counting bloom filter in order to
// decide how many times each item appeared.
// that way I can in the future create a good cache filter utility.
public class GuessingBloomFilter<E> implements Serializable {
	/**
	 * 
	 */
	// in my construction - each time we increment we can increase the ratio.
	private Random rnd;
	// probobility of inserting at level
	private double ratio;

	public int[] numberOfAddedElementsPerLevel;
	private static final long serialVersionUID = 7783719836467415625L;
	protected Byte[] byteSet;
	protected int bitSetSize;
	protected double bitsPerElement;
	protected int expectedNumberOfFilterElements; // expected (maximum) number
													// of elements to be added
	protected int numberOfAddedElements; // number of elements actually added to
											// the Bloom filter
	protected int k; // number of hash functions

	static final Charset charset = Charset.forName("UTF-8"); // encoding used
																// for storing
																// hash values
																// as strings

	static final String hashName = "MD5"; // MD5 gives good enough accuracy in
											// most circumstances. Change to
											// SHA1 if it's needed
	static final MessageDigest digestFunction;
	static { // The digest method is reused between instances
		MessageDigest tmp;
		try {
			tmp = java.security.MessageDigest.getInstance(hashName);
		} catch (final NoSuchAlgorithmException e) {
			tmp = null;
		}
		digestFunction = tmp;
	}

	public GuessingBloomFilter<E> setRnd(final Random rnd) {
		this.rnd = rnd;
		return this;
	}
	public GuessingBloomFilter<E> setRatio(final double ratio) {
		this.ratio = ratio;
		return this;
	}
	/**
	 * Constructs an empty Bloom filter. The total length of the Bloom filter
	 * will be c*n.
	 * 
	 * @param c
	 *            is the number of bits used per element.
	 * @param n
	 *            is the expected number of elements the filter will contain.
	 * @param k
	 *            is the number of hash functions used.
	 */
	public GuessingBloomFilter(final double c, final int n, final int k) {
		this.expectedNumberOfFilterElements = n;
		this.ratio = 1;
		this.k = k;
		this.rnd = new Random();
		this.bitsPerElement = c;
		this.bitSetSize = (int) Math.ceil(c * n);
		this.byteSet = new Byte[bitSetSize];
		this.numberOfAddedElementsPerLevel = new int[256];
		// Workaround. let's use all the bits in the byte and start counting
		// from -128 to 127.
		// when reading the value, we will consider it by adding + 128.
		// (not the bitwise smartest but will suffice.
		for (int i = 0; i < bitSetSize; i++)
			byteSet[i] = -128;
		numberOfAddedElements = 0;

	}

	/**
	 * Constructs an empty Bloom filter. The optimal number of hash functions
	 * (k) is estimated from the total size of the Bloom and the number of
	 * expected elements.
	 * 
	 * @param bitSetSize
	 *            defines how many bits should be used in total for the filter.
	 * @param expectedNumberOElements
	 *            defines the maximum number of elements the filter is expected
	 *            to contain.
	 */
	public GuessingBloomFilter(final int bitSetSize, final int expectedNumberOElements) {
		this(bitSetSize / (double) expectedNumberOElements, expectedNumberOElements, (int) Math
				.round((bitSetSize / (double) expectedNumberOElements) * Math.log(2.0)));
	}

	/**
	 * Constructs an empty Bloom filter with a given false positive probability.
	 * The number of bits per element and the number of hash functions is
	 * estimated to match the false positive probability.
	 * 
	 * @param falsePositiveProbability
	 *            is the desired false positive probability.
	 * @param expectedNumberOfElements
	 *            is the expected number of elements in the Bloom filter.
	 */
	public GuessingBloomFilter(final double falsePositiveProbability, final int expectedNumberOfElements) {
		this(Math.ceil(-(Math.log(falsePositiveProbability) / Math.log(2))) / Math.log(2), // c
																							// =
																							// k
																							// /
																							// ln(2)
				expectedNumberOfElements, (int) Math.ceil(-(Math.log(falsePositiveProbability) / Math.log(2)))); // k
																													// =
																													// ceil(-log_2(false
																													// prob.))
	}

	/**
	 * Construct a new Bloom filter based on existing Bloom filter data.
	 * 
	 * @param bitSetSize
	 *            defines how many bits should be used for the filter.
	 * @param expectedNumberOfFilterElements
	 *            defines the maximum number of elements the filter is expected
	 *            to contain.
	 * @param actualNumberOfFilterElements
	 *            specifies how many elements have been inserted into the
	 *            <code>filterData</code> BitSet.
	 * @param filterData
	 *            a BitSet representing an existing Bloom filter.
	 */
	public GuessingBloomFilter(final int bitSetSize, final int expectedNumberOfFilterElements,
			final int actualNumberOfFilterElements, final Byte[] filterData) {
		this(bitSetSize, expectedNumberOfFilterElements);
		this.byteSet = filterData;
		this.numberOfAddedElements = actualNumberOfFilterElements;
	}

	/**
	 * Generates a digest based on the contents of a String.
	 * 
	 * @param val
	 *            specifies the input data.
	 * @param charset
	 *            specifies the encoding of the input data.
	 * @return digest as long.
	 */
	public static long createHash(final String val, final Charset charset) {
		return createHash(val.getBytes(charset));
	}

	/**
	 * Generates a digest based on the contents of a String.
	 * 
	 * @param val
	 *            specifies the input data. The encoding is expected to be
	 *            UTF-8.
	 * @return digest as long.
	 */
	public static long createHash(final String val) {
		return createHash(val, charset);
	}

	/**
	 * Generates a digest based on the contents of an array of bytes.
	 * 
	 * @param data
	 *            specifies input data.
	 * @return digest as long.
	 */
	public static long createHash(final byte[] data) {
		long h = 0;
		byte[] res;

		synchronized (digestFunction) {
			res = digestFunction.digest(data);
		}

		for (int i = 0; i < 4; i++) {
			h <<= 8;
			h |= res[i] & 0xFF;
		}
		return h;
	}

	/**
	 * Compares the contents of two instances to see if they are equal.
	 * 
	 * @param obj
	 *            is the object to compare to.
	 * @return True if the contents of the objects are equal.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(final Object obj) {
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final GuessingBloomFilter<E> other = (GuessingBloomFilter<E>) obj;
		if (this.expectedNumberOfFilterElements != other.expectedNumberOfFilterElements)
			return false;
		if (this.k != other.k)
			return false;
		if (this.bitSetSize != other.bitSetSize)
			return false;
		if (this.byteSet != other.byteSet && (this.byteSet == null || !this.byteSet.equals(other.byteSet)))
			return false;
		return true;
	}

	/**
	 * Calculates a hash code for this class.
	 * 
	 * @return hash code representing the contents of an instance of this class.
	 */
	@Override
	public int hashCode() {
		int hash = 7;
		hash = 61 * hash + (this.byteSet != null ? this.byteSet.hashCode() : 0);
		hash = 61 * hash + this.expectedNumberOfFilterElements;
		hash = 61 * hash + this.bitSetSize;
		hash = 61 * hash + this.k;
		return hash;
	}

	/**
	 * Calculates the expected probability of false positives based on the
	 * number of expected filter elements and the size of the Bloom filter. <br />
	 * <br />
	 * The value returned by this method is the <i>expected</i> rate of false
	 * positives, assuming the number of inserted elements equals the number of
	 * expected elements. If the number of elements in the Bloom filter is less
	 * than the expected value, the true probability of false positives will be
	 * lower.
	 * 
	 * @return expected probability of false positives.
	 */
	public double expectedFalsePositiveProbability() {
		return getFalsePositiveProbability(expectedNumberOfFilterElements);
	}

	/**
	 * Calculate the probability of a false positive given the specified number
	 * of inserted elements.
	 * 
	 * @param numberOfElements
	 *            number of inserted elements.
	 * @return probability of a false positive.
	 */
	public double getFalsePositiveProbability(final double numberOfElements) {
		// (1 - e^(-k * n / m)) ^ k
		return Math.pow((1 - Math.exp(-k * numberOfElements / bitSetSize)), k);

	}

	/**
	 * Get the current probability of a false positive. The probability is
	 * calculated from the size of the Bloom filter and the current number of
	 * elements added to it.
	 * 
	 * @return probability of false positives.
	 */
	public double getFalsePositiveProbability() {
		return getFalsePositiveProbability(numberOfAddedElements);
	}

	/**
	 * return the expected false positive probability based on the actual number
	 * of elements that are inserted per level.
	 * 
	 * @param level
	 * @return false positive probability for a certain level.
	 */
	public double getFalsePositiveProbability(final int level) {

		return getFalsePositiveProbability((double) numberOfAddedElementsPerLevel[level]);
	}

	/**
	 * Returns the value chosen for K.<br />
	 * <br />
	 * K is the optimal number of hash functions based on the size of the Bloom
	 * filter and the expected number of inserted elements.
	 * 
	 * @return optimal k.
	 */
	public int getK() {
		return k;
	}

	/**
	 * Sets all bits to false in the Bloom filter.
	 */
	public void clear() {
		for (int i = 0; i < this.bitSetSize; i++)
			this.byteSet[i] = -128;
		numberOfAddedElements = 0;
	}

	/**
	 * Adds an object to the Bloom filter. The output from the object's
	 * toString() method is used as input to the hash functions.
	 * 
	 * @param element
	 *            is an element to register in the Bloom filter.
	 * @return
	 */
	public boolean add(final E element) {
		long hash;
		final String valString = element.toString();
		final ArrayList<Integer> bitsToIncrement = new ArrayList<Integer>();
		for (int x = 0; x < k; x++) {
			hash = createHash(valString + Integer.toString(x));
			hash = hash % bitSetSize;
			hash = Math.abs((int) hash);
			bitsToIncrement.add((int) hash);
		}
		addToStatistics(bitsToIncrement, (byte) 1);
		return false;
	}
	public boolean add(final E element, final byte howmuch) {
		long hash;
		final String valString = element.toString();
		final ArrayList<Integer> bitsToIncrement = new ArrayList<Integer>();
		for (int x = 0; x < k; x++) {
			hash = createHash(valString + Integer.toString(x));
			hash = hash % bitSetSize;
			hash = Math.abs((int) hash);
			bitsToIncrement.add((int) hash);
		}
		addToStatistics(bitsToIncrement, howmuch);
		return false;
	}

	protected void addToStatistics(final ArrayList<Integer> bitsToIncrement, final byte howmuch) {
		final Byte min = getMinimalValue(bitsToIncrement);
		if (min == 127)
			return;
		if (NeedToIncrement(min)) {

			// count a general insert. and an insert to the apropriate level.
			numberOfAddedElementsPerLevel[min + 128]++;
			numberOfAddedElements++;
			for (final Integer index : bitsToIncrement)
				if (this.byteSet[index] == min)
					this.byteSet[index] = (byte) (this.byteSet[index] + howmuch);
		}
	}

	private Byte getMinimalValue(final ArrayList<Integer> bitsToIncrement) {
		final TreeSet<Byte> minimumIndexes = new TreeSet<Byte>();
		for (final Integer index : bitsToIncrement)
			minimumIndexes.add(this.byteSet[index]);
		final Byte min = minimumIndexes.first();
		return min;
	}
	private boolean NeedToIncrement(final Byte min) {
		final int minInt = min + 128;
		// I am not the expert so lets verify that I am not falling over my
		// face.
		// Assert.assertTrue(minInt>=0);
		// Assert.assertTrue(minInt<=255);

		// Double d = rnd.nextDouble();
		// return(d<(1/Math.pow(ratio, minInt)));
		return true;
	}

	/**
	 * Adds all elements from a Collection to the Bloom filter.
	 * 
	 * @param c
	 *            Collection of elements.
	 */
	public void addAll(final Collection<? extends E> c) {
		for (final E element : c)
			add(element);
	}

	/**
	 * Returns the expected number of times the value was inserted to the
	 * filter. Each time we increment the counter. (at a given probability.) so
	 * the estimation will be the expectancy. if ratio =1 then (if we don't have
	 * overflow), the counting will be accurate up until false positives.
	 * 
	 * @param element
	 *            element to check.
	 * @return true if the element could have been inserted into the Bloom
	 *         filter.
	 */
	public int guess(final E element) {
		long hash;
		final ArrayList<Integer> bitsToIncrement = new ArrayList<Integer>();
		final String valString = element.toString();
		for (int x = 0; x < k; x++) {
			hash = createHash(valString + Integer.toString(x));
			hash = hash % bitSetSize;
			hash = Math.abs((int) hash);
			bitsToIncrement.add((int) hash);
		}

		int score = this.getMinimalValue(bitsToIncrement);
		// count from zero;
		score += 128;
		int estimation = 0;
		for (int i = 0; i < score; i++)
			// since we pass level k at probability of 1/(ratio)^k we guess the
			// expectency.
			estimation += (Math.pow(this.ratio, i));
		return estimation;
	}

	public int size() {
		return this.bitSetSize;
	}

	/**
	 * Returns the number of elements added to the Bloom filter after it was
	 * constructed or after clear() was called.
	 * 
	 * @return number of elements added to the Bloom filter.
	 */
	public int count() {
		return this.numberOfAddedElements;
	}

	/**
	 * Returns the expected number of elements to be inserted into the filter.
	 * This value is the same value as the one passed to the constructor.
	 * 
	 * @return expected number of elements.
	 */
	public int getExpectedNumberOfElements() {
		return expectedNumberOfFilterElements;
	}

	/**
	 * Get expected number of bits per element when the Bloom filter is full.
	 * This value is set by the constructor when the Bloom filter is created.
	 * See also getBitsPerElement().
	 * 
	 * @return expected number of bits per element.
	 */
	public double getExpectedBitsPerElement() {
		return this.bitsPerElement;
	}

	/**
	 * Get actual number of bits per element based on the number of elements
	 * that have currently been inserted and the length of the Bloom filter. See
	 * also getExpectedBitsPerElement().
	 * 
	 * @return number of bits per element.
	 */
	public double getBitsPerElement() {
		return this.bitSetSize / (double) numberOfAddedElements;
	}
}