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
import java.util.ArrayList;
import java.util.Arrays;

public class Hashtable<V> {

	private Object[] arr; //an array of Pair objects, where each pair contains the key and value stored in the hashtable
	private int max; //the size of arr. This should be a prime number
	private int itemCount = 0; //the number of items stored in arr
	private final double maxLoad = 0.6; //the maximum load factor

	public static enum PROBE_TYPE {
		LINEAR_PROBE, QUADRATIC_PROBE, DOUBLE_HASH;
	}

	PROBE_TYPE probeType; //the type of probe to use when dealing with collisions
	private final BigInteger DBL_HASH_K = BigInteger.valueOf(8);

	/**
	 * Create a new Hashtable with a given initial capacity and using a given probe type
	 * @param initialCapacity
	 * @param pt
	 */
	public Hashtable(int initialCapacity, PROBE_TYPE pt) {
		max = nextPrime((int) Math.ceil(initialCapacity / maxLoad));
		arr = new Object[max];
		probeType = PROBE_TYPE.LINEAR_PROBE;
		
		System.out.printf("%nArray size: %s, Probe type: %s%n", max, probeType);
	}
	
	/**
	 * Create a new Hashtable with a given initial capacity and using the default probe type
	 * @param initialCapacity
	 */
	public Hashtable(int initialCapacity) {
		long t1 = System.nanoTime();
		//Set max size so that max * maxLoad = initialCapacity
		max = nextPrime((int) Math.ceil(initialCapacity / maxLoad));		
		probeType = PROBE_TYPE.LINEAR_PROBE;
		
		arr = new Object[max];
		System.out.printf("%nArray size: %s, Probe type: %s%n", max, probeType);
				
		long t2 = System.nanoTime();
		System.out.println((t2 - t1) / 1000000000.0);
	}

	/**
	 * Store the value against the given key. If the loadFactor exceeds maxLoad, call the resize 
	 * method to resize the array. the If key already exists then its value should be overwritten.
	 * Create a new Pair item containing the key and value, then use the findEmpty method to find an unoccupied 
	 * position in the array to store the pair. Call findEmpty with the hashed value of the key as the starting
	 * position for the search, stepNum of zero and the original key.
	 * containing   
	 * @param key
	 * @param value
	 */
	public void put(String key, V value) {
		int index = hash(key);
		V existingValue = get(key);
		
		if (existingValue != null) {
			//System.out.printf("Key '%s' already exists%n", key);
			existingValue = value;
			//System.out.printf("Overwritten, new value: %s%n%n", value);
		} else {
			Pair pair = new Pair(key, value);
		
			//System.out.println("\nOriginal index: " + index);
			index = findEmpty(index, key, 0);
			//System.out.println("Empty index: " + index);
		
			//System.out.printf("Putting %s at index: %s%n", pair, index);
			arr[index] = pair;
			itemCount++;
			//System.out.println(Arrays.toString(arr));
			
			if (getLoadFactor() >= maxLoad) {
				//System.out.println("\nMax load exceeded, resizing\n===========================");
				resize();
			}
		}
	}

	/**
	 * Get the value associated with key, or return null if key does not exists. Use the find method to search the
	 * array, starting at the hashed value of the key, stepNum of zero and the original key.
	 * @param key
	 * @return
	 */
	public V get(String key) {
		int index = hash(key);
		return find(index, key, 0);
	}

	/**
	 * Return true if the Hashtable contains this key, false otherwise 
	 * @param key
	 * @return
	 */
	public boolean hasKey(String key) {
		if (get(key) == null) {
			return false;
		}
		return true;
	}

	/**
	 * Return all the keys in this Hashtable as a collection
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Collection<String> getKeys() {
		ArrayList<String> keyList = new ArrayList<String>();
		for (int i = 0; i < arr.length; i++) {
			Pair pair = (Pair) arr[i];
			if (pair != null) {
				keyList.add(pair.key);
			}
		}
		return keyList;
	}

	/**
	 * Return the load factor, which is the ratio of itemCount to max
	 * @return
	 */
	public double getLoadFactor() {
		//Round to 2 dp
		return Math.round((((double) itemCount / (double) max) * 100.0)) / 100.0;
	}

	/**
	 * return the maximum capacity of the Hashtable
	 * @return
	 */
	public int getCapacity() {
		return (int) Math.ceil(max * maxLoad);
	}
	
	/**
	 * Find the value stored for this key, starting the search at position startPos in the array. If
	 * the item at position startPos is null, the Hashtable does not contain the value, so return null. 
	 * If the key stored in the pair at position startPos matches the key we're looking for, return the associated 
	 * value. If the key stored in the pair at position startPos does not match the key we're looking for, this
	 * is a hash collision so use the getNextLocation method with an incremented value of stepNum to find 
	 * the next location to search (the way that this is calculated will differ depending on the probe type 
	 * being used). Then use the value of the next location in a recursive call to find.
	 * @param startPos
	 * @param key
	 * @param stepNum
	 * @return
	 */
	@SuppressWarnings("unchecked") //Remove pair warning
	private V find(int startPos, String key, int stepNum) {
		Pair pair = (Pair) arr[startPos];
		if (arr[startPos] == null) {
			return null;
		} else if (key.equals(pair.key)) {
			return pair.value;
		} else {
			stepNum++;
			startPos = getNextLocation(startPos, key, stepNum);
			return find(startPos, key, stepNum);
		}
	}

	/**
	 * Find the first unoccupied location where a value associated with key can be stored, starting the
	 * search at position startPos. If startPos is unoccupied, return startPos. Otherwise use the getNextLocation
	 * method with an incremented value of stepNum to find the appropriate next position to check 
	 * (which will differ depending on the probe type being used) and use this in a recursive call to findEmpty.
	 * @param startPos
	 * @param stepNum
	 * @param key
	 * @return
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

	/**
	 * Finds the next position in the Hashtable array starting at position startPos. If the linear
	 * probe is being used, we just increment startPos. If the double hash probe type is being used, 
	 * add the double hashed value of the key to startPos. If the quadratic probe is being used, add
	 * the square of the step number to startPos.
	 * @param i
	 * @param stepNum
	 * @param key
	 * @return
	 */
	private int getNextLocation(int startPos, String key, int stepNum) {
		int step = startPos;
		switch (probeType) {
		case LINEAR_PROBE:
			step++;
			break;
		case DOUBLE_HASH:
			step += doubleHash(key);
			break;
		case QUADRATIC_PROBE:
			step += stepNum * stepNum;
			break;
		default:
			break;
		}
		return step % max;
	}

	/**
	 * A secondary hash function which returns a small value (less than or equal to DBL_HASH_K)
	 * to probe the next location if the double hash probe type is being used
	 * @param key
	 * @return
	 */
	private int doubleHash(String key) {
		BigInteger hashVal = BigInteger.valueOf(key.charAt(0) - 96);
		for (int i = 0; i < key.length(); i++) {
			BigInteger c = BigInteger.valueOf(key.charAt(i) - 96);
			hashVal = hashVal.multiply(BigInteger.valueOf(27)).add(c);
		}
		return DBL_HASH_K.subtract(hashVal.mod(DBL_HASH_K)).intValue();
	}

	/**
	 * Return an int value calculated by hashing the key. See the lecture slides for information
	 * on creating hash functions. The return value should be less than max, the maximum capacity 
	 * of the array
	 * @param key
	 * @return
	 */
	public int hash(String key) {
		key = key.toLowerCase();
		BigInteger hugeKey = BigInteger.ZERO;
		BigInteger big27 = BigInteger.valueOf(27);
		int power = key.length() - 1;
		for (int i = 0; i < key.length(); i++) {
			int c = (int) key.charAt(i);
			BigInteger cBig = BigInteger.valueOf(c);
			hugeKey = hugeKey.add(cBig.multiply(big27.pow(power))); //hugeKey += c * (27^power)
			power -= 1;
		}
		
		return hugeKey.mod(BigInteger.valueOf(max)).intValue(); //hugekey % max
	}

	/**
	 * Return true if n is prime
	 * @param n
	 * @return
	 */
	private boolean isPrime(int n) {
		//Fermat primality test
		Random rand = new Random();
		BigInteger nBig = new BigInteger(Integer.toString(n)); //Convert n to BigInteger
		
		if (n == 1) {return false;}
		
		for (int i = 0; i < 200; i++) {
			//Random number from 1 -> n-1;
			BigInteger a;
			do {
				a = new BigInteger(nBig.bitLength(), rand);
			} while (!(a.compareTo(BigInteger.ONE) >= 0 && a.compareTo(nBig) == -1));
			
			BigInteger aModPow = a.modPow(nBig.subtract(BigInteger.ONE), nBig); //(a ^ (n-1)) % n
			
			if (aModPow.compareTo(BigInteger.ONE) != 0) {
				return false; //Definitely not a prime
			}			
		}
		return true; //Possibly a prime
	}

	/**
	 * Get the smallest prime number which is larger than n
	 * @param n
	 * @return
	 */
	private int nextPrime(int n) {
		int i;
		//Set i to closest odd number
		if (n % 2 == 0) {
			i = n + 1;
		} else {
			i = n;
		}
		
		//Check every odd number between n and 2*n
		for (; i < 2 * n; i += 2) {
			if (isPrime(i)) {
				break;
			}
		}
		
		return i;
	}

	/**
	 * Resize the hashtable, to be used when the load factor exceeds maxLoad. The new size of
	 * the underlying array should be the smallest prime number which is at least twice the size
	 * of the old array.
	 */
	@SuppressWarnings("unchecked") //Remove pair warning
	public void resize() {
		//Create a larger array
		int newMax = nextPrime(max * 2);
		Object[] newArr = new Object[newMax];
		Object[] oldArr = arr;
		
		arr = newArr;
		max = newMax;
		itemCount = 0;
		System.out.println("New array size: " + max);
		
		//Rehash each value in old array
		for (int i = 0; i < oldArr.length; i++) {
			if (oldArr[i] != null) {
				Pair pair = (Pair) oldArr[i];
				put(pair.key, pair.value);
			}
		}
		
		//System.out.println("\nResize finished, new array:");
		//System.out.println(Arrays.toString(arr));
		//System.out.println("===========================");
	}

	
	/**
	 * Instances of Pair are stored in the underlying array. We can't just store
	 * the value because we need to check the original key in the case of collisions.
	 * @author jb259
	 *
	 */
	private class Pair {
		private String key;
		private V value;

		public Pair(String key, V value) {
			this.key = key;
			this.value = value;
		}
		
		@Override
		public String toString() {
			String out = String.format("[%s, %s]", key, value);
			return out;
		}
	}

}