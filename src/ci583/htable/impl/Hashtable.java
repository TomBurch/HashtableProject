package ci583.htable.impl;

/**
 * A HashTable with no deletions allowed. Duplicates overwrite the existing value. Values are of
 * type V and keys are strings -- one extension is to adapt this class to use other types as keys.
 * 
 * The underlying data is stored in the array `arr', and the actual values stored are pairs of 
 * (key, value). This is so that we can detect collisions in the hash function and look for the next 
 * location when necessary.
 */

import java.math.BigInteger;
import java.util.Collection;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.ArrayList;

public class Hashtable<V> {
	/** An array of Pair objects, where each pair contains the key and value stored.
	 */
	private Object[] arr;
	
	/** The size of arr.<br>
	 *  This should be a prime number.
	 */
	private int max;
	
	/** The number of Pair objects stored in arr
	 */
	private int itemCount = 0;
	
	/** The maximum load factor.<br> 
	 *  When {@code (itemCount >= max * maxLoad)} arr is resized.
	 *  @see resize
	 */
	private final double maxLoad = 0.6;
	
	/** All characters that are allowed in keys
	 *  @see encode
	 */
	private final Pattern charPattern = Pattern.compile("[a-zA-Z0-9:!?#%&*+_./:<>=@ ]");

	public static enum PROBE_TYPE {
		LINEAR_PROBE, QUADRATIC_PROBE, DOUBLE_HASH;
	}

	/** The type of probe to use when dealing with collisions,<br>
	 *  Defaults to {@code LINEAR_PROBE}
	 */
	PROBE_TYPE probeType;
	
	private final BigInteger DBL_HASH_K = BigInteger.valueOf(8);

	/** Creates a new Hashtable with a given initial capacity and using a given {@code PROBE_TYPE}
	 * 
	 * @param initialCapacity 	Initial size of the Hashtable
	 * @param pt				{@code PROBE_TYPE} to use
	 */
	public Hashtable(int initialCapacity, PROBE_TYPE pt) {
		max = nextPrime(initialCapacity);
		probeType = pt;
		arr = new Object[max];
		
		System.out.printf("%nArray size: %s, Probe type: %s%n", max, probeType);
	}
	
	/** Creates a new Hashtable with a given initial capacity and using<br>
	 * the default {@code PROBE_TYPE}: {@code LINEAR_PROBE}
	 * 
	 * @param initialCapacity	Initial size of the Hashtable
	 */
	public Hashtable(int initialCapacity) {
		max = nextPrime(initialCapacity);
		probeType = PROBE_TYPE.LINEAR_PROBE;	
		arr = new Object[max];
		
		System.out.printf("%nArray size: %s, Probe type: %s%n", max, probeType);
	}

	/** Store the given value against the given key.<br>
	 * 	If the loadFactor exceeds {@link maxLoad}, then Hashtable is resized.<br>
	 * 	If the key already exists then its value is overwritten.<br>
	 * @see resize
	 * @see getLoadFactor
	 * 	
	 * @param key	Key used to store value 
	 * @param value	Value of the key
	 */
	public void put(String key, V value) {
		int index = hash(key);
		V existingValue = get(key);
		
		//If the key already exists, overwrite its value
		if (existingValue != null) {	
			existingValue = value;
		} else {
			//Otherwise create a new pair and store at next empty index
			Pair pair = new Pair(key, value);
			index = findEmpty(index, key, 0);
			arr[index] = pair;
			itemCount++;
			
			//Check if resize is necessary
			if (getLoadFactor() >= maxLoad) {
				resize();
			}
		}
	}

	/** Get the value associated with key.
	 * 
	 * @param key Key to search for
	 * @return Associated value, or {@code null} if key is invalid
	 */
	public V get(String key) {
		int index = hash(key);
		return find(index, key, 0);
	}

	/** Check if Hashtable contains the given key.
	 * 
	 * @param key
	 * @return (bool) {@code true} if Hashtable contains key, {@code false} otherwise
	 */
	public boolean hasKey(String key) {
		if (get(key) == null) {
			return false;
		}
		return true;
	}

	/** Return all the keys in the Hashtable as a collection.
	 * 
	 * @return ArrayList of keys
	 */
	public Collection<String> getKeys() {
		ArrayList<String> keyList = new ArrayList<String>();
		
		//Loop through each Pair in arr and add their key to keyList
		for (int i = 0; i < arr.length; i++) {
			Pair pair = (Pair) arr[i];
			if (pair != null) {
				keyList.add(pair.key);
			}
		}
		return keyList;
	}

	/**Return the load factor, which is the ratio of {@link itemCount} to {@link max}
	 * 
	 * @return
	 */
	public double getLoadFactor() {
		//Round (itemCount / max) to 2 dp
		return Math.round((((double) itemCount / (double) max) * 100.0)) / 100.0;
	}

	/** Return the maximum capacity of the Hashtable
	 * 
	 * @return {@link max}
	 */
	public int getCapacity() {
		return max;
	}
	
	/** Recursively find the value stored for the given key.
	 * @see getNextLocation
	 * 
	 * @param startPos	Index to start at, usually hash of key.
	 * @param key		Key being searched for
	 * @param stepNum	Used as increment in {@code QUADRATIC_PROBE}
	 * @return value stored with key if found, {@code null} otherwise
	 */
	private V find(int startPos, String key, int stepNum) {
		Pair pair = (Pair) arr[startPos];
		
		//If pair is null, key is not stored
		if (arr[startPos] == null) {
			return null;
		} else if (key.equals(pair.key)) {
			//If key is found, return its value
			return pair.value;
		} else {
			//Hash collision, find next index to check
			stepNum++;
			startPos = getNextLocation(startPos, key, stepNum);
			return find(startPos, key, stepNum);
		}
	}

	/** Recursively find the first unoccupied location where a value associated with key can be stored.
	 * @see getNextLocation
	 * 
	 * @param startPos	Index to start at, usually hash of key
	 * @param stepNum	Used as increment in {@code QUADRATIC_PROBE}
	 * @param key		Key used for search
	 * @return (int) Empty index where key can be stored
	 */
	@SuppressWarnings("unchecked") //Remove pair warning
	private int findEmpty(int startPos, String key, int stepNum) {
		Pair pair = (Pair) arr[startPos];		
		if (pair == null) {
			return startPos;
		}
		stepNum++;
		startPos = getNextLocation(startPos, key, stepNum);
		return findEmpty(startPos, key, stepNum);
	}

	/** Finds the next index in the Hashtable depending on probe_type {@code PROBE_TYPE}.
	 * 
	 * @param startPos 	Index to start with
	 * @param key		Key used by {@code DOUBLE_HASH}	
	 * @param stepNum	Used as increment by {@code QUADRATIC_PROBE}
	 * @return (int) Next index
	 * 
	 * @author jb259
	 */
	private int getNextLocation(int startPos, String key, int stepNum) {
		int step = startPos;
		switch (probeType) {
		case LINEAR_PROBE:
			//Just increment startPos
			step++;
			break;
		case DOUBLE_HASH:
			//Add the double hashed value of key
			step += doubleHash(key);
			break;
		case QUADRATIC_PROBE:
			//Add the square of stepNum
			step += stepNum * stepNum;
			break;
		default:
			break;
		}
		return step % max;
	}

	/** A secondary hash function which returns a small value (<= {@code DBL_HASH_K})
	 * 	to probe the next location if the {@code DOUBLE_HASH} probe type is being used
	 * 
	 * @param key
	 * @return
	 * 
	 * @author jb259
	 */
	private int doubleHash(String key) {
		BigInteger hashVal = BigInteger.valueOf(key.charAt(0) - 96);
		for (int i = 0; i < key.length(); i++) {
			BigInteger c = BigInteger.valueOf(key.charAt(i) - 96);
			hashVal = hashVal.multiply(BigInteger.valueOf(27)).add(c);
		}
		return DBL_HASH_K.subtract(hashVal.mod(DBL_HASH_K)).intValue();
	}

	/** Hash the given key
	 * 
	 * @param key Key to hash
	 * @return (int) Hashed key
	 */
	private int hash(String key) {
		BigInteger hugeKey = BigInteger.ZERO;
		BigInteger radix = BigInteger.valueOf(79); //Number of allowed characters (charPattern)
		int power = key.length() - 1;
		
		//For each character in key...
		for (int i = 0; i < key.length(); i++) {
			int c = encode(key.charAt(i)); //Convert character to ASCII value
			BigInteger cBig = BigInteger.valueOf(c); //Convert ASCII value to BigInteger
			hugeKey = hugeKey.add(cBig.multiply(radix.pow(power))); //hugeKey += c * (radix ^ power)
			power -= 1;
		}
		
		return hugeKey.mod(BigInteger.valueOf(max)).intValue(); //return hugeKey % max
	}
	
	/**	Encode the given char for use in hashing.
	 * @see hash
	 * 
	 * @param c Character to encode
	 * @return (int) Character converted to ASCII
	 * @throws IllegalArgumentException If character is not valid
	 */
	private int encode(char c) throws IllegalArgumentException {
		//If char is in charPattern, convert to ASCII and return
		if (charPattern.matcher(String.valueOf(c)).find()) {
			return (int) c;
		} else {
			throw new IllegalArgumentException("Invalid character in key: '" + c + "'");
		}
	}

	/** Check if int is prime using Fermat primality test
	 * @see https://en.wikipedia.org/wiki/Fermat_primality_test
	 * 
	 * @param n	Int to check primality of
	 * @return {@code true} if int is probably prime, {@code false} otherwise
	 */
	private boolean isPrime(int n) {
		//Base cases
		if (n == 0 || n == 1) { return false; }
		if (n == 2) { return true; }
		if (n % 2 == 0) { return false; }
		
		Random rand = new Random();
		BigInteger nBig = BigInteger.valueOf(n); //Convert n to BigInteger
		
		for (int i = 0; i < 200; i++) {
			//Generate random number from 1 -> n-1;
			//Credit to https://stackoverflow.com/questions/2290057/how-to-generate-a-random-biginteger-value-in-java
			BigInteger a;
			do {
				a = new BigInteger(nBig.bitLength(), rand);
			} while (!(a.compareTo(BigInteger.ONE) >= 0 && a.compareTo(nBig) == -1)); 
			
			BigInteger aModPow = a.modPow(nBig.subtract(BigInteger.ONE), nBig); //(a ^ (n-1)) % n
			
			if (aModPow.compareTo(BigInteger.ONE) != 0) {
				return false; //Definitely not a prime
			}			
		}
		return true; //Probably a prime
	}

	/** Get the smallest prime number which is larger than n
	 * 
	 * @param n
	 * @return (int) Prime number
	 */
	private int nextPrime(int n) {
		int i;
		//Set i to next odd number
		if (n % 2 == 0) {
			i = n + 1;
		} else {
			i = n;
		}
		
		//Check primality of every odd number between n and 2*n
		for (; i < 2 * n; i += 2) {
			if (isPrime(i)) {
				break;
			}
		}
		return i;
	}

	/**	Resize the hashtable, to be used when the load factor exceeds {@link maxLoad}.<br>
	 *  The new size of the array is the smallest prime number which is at least twice the size of the old array.
	 */
	public void resize() {
		//Create the larger array
		int newMax = nextPrime(max * 2);
		Object[] newArr = new Object[newMax];
		Object[] oldArr = arr;
		
		//Change Hashtable values to reflect new array
		arr = newArr;
		max = newMax;
		itemCount = 0;
		
		//Put each value of the old array in the new array
		for (int i = 0; i < oldArr.length; i++) {
			if (oldArr[i] != null) {
				Pair pair = (Pair) oldArr[i];
				put(pair.key, pair.value);
			}
		}
	}

	
	/**Pair object used to store keys and values
	 */
	private class Pair {
		private String key; //The key used to store the Pair, to check for hash collisions
		private V value;

		public Pair(String key, V value) {
			this.key = key;
			this.value = value;
		}
		
		@Override
		public String toString() {
			return String.format("[%s, %s]", key, value);
		}
	}

}