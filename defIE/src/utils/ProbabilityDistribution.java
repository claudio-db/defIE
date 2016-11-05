package utils;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Lists;

/**
 * Simple class modeling a probability distribution.
 * 
 * @author claudio
 */
public class ProbabilityDistribution<T> implements Iterable<Entry<T,Double>>
{	
	// Entries
	private Map<T,Double> entries;
	// Logspace flag
	private boolean logSpace;
	
	// Comprator by value
	public final Comparator<Entry<T,Double>> COMPARE_BY_VALUE = Comparator.comparingDouble(Entry<T,Double>::getValue);
	
	/**
	 * Constructor #1.
	 * Generates a probability distribution from a map to double.
	 * Unless provided already in log space, values are normalized to build a proper probability.
	 * 
	 * @param entries Set of entries as Map to double
	 */
	public ProbabilityDistribution(Map<T,Double> entries, boolean logSpace)
	{
		this.entries = entries.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey(), e -> logSpace? e.getValue() : Math.abs(e.getValue())));
		this.logSpace = logSpace;
		
		// Normalize to generate a probability distribution
		if(!logSpace) normalize();
	}

	/**
	 * Constructor #2.
	 * Generates a probability distribution from a collection of entries.
	 * 
	 * @param entries Set of entries
	 */
	public ProbabilityDistribution(Collection<T> entries)
	{
		// Generate the map
		this.entries = new HashMap<T,Double>();
		for(T entry : entries)
			this.entries.put(entry, 1.0 + this.entries.getOrDefault(entry, 0.0));
		this.logSpace = false;
		// Normalize to generate a probability distribution
		normalize();
	}
	
	/**
	 * Normalizes the entries to get a legal probability distribution.
	 */
	private void normalize()
	{
		// Compute normalizing factor and normalize
		normalize(entries.values().stream().reduce(0.0, Double::sum));
	}
	
	/**
	 * Normalizes all the entries using a given factor.
	 * 
	 * @param normalizingFactor
	 */
	private void normalize(double normalizingFactor)
	{
		entries.keySet().forEach(key -> entries.put(key, entries.get(key)/normalizingFactor));
	}

	/**
	 * Number of entries for which the probability distribution is defined.
	 */
	public final int numberOfEntries()
	{
		return entries.size();
	}
	
	/**
	 * Return the entry with the maximum probability.
	 */
	public T argMax()
	{
		return entries.entrySet().stream()
				.max(COMPARE_BY_VALUE).orElseThrow(()-> new NullPointerException()).getKey();
	}
	
	/**
	 * Return the entry with the minimum probability.
	 */
	public T argMin()
	{
		return entries.entrySet().stream()
				.min(COMPARE_BY_VALUE).orElseThrow(()-> new NullPointerException()).getKey();
	}
	
	/**
	 * Get the probability of a given entry according to the distribution.
	 * The method returns 0 (or -∞ if logspace) for undefined entries.
	 * 
	 * @param entry
	 * @return probability of entry
	 */
	public double probabilityOf(T entry)
	{
		return entries.getOrDefault(entry, logSpace ? Double.NEGATIVE_INFINITY : 0.0);
	}
	
	/**
	 * Check whether the distribution is defined for a given entry.
	 * 
	 * @param entry
	 */
	public boolean isDefinedFor(T entry)
	{
		return entries.containsKey(entry);
	}
	
	/**
	 * Descending iterator over probability distribution entries.
	 * 
	 * @return an iterator over key entries
	 */
	public Iterator<Entry<T,Double>> descendingIterator()
	{
		return stream().iterator();
	}

	/**
	 * Stream over probability distribution entries.
	 * 
	 * @return an iterator over key entries
	 */
	public Stream<Entry<T,Double>> stream()
	{
		return entries.entrySet().stream().sorted(COMPARE_BY_VALUE.reversed());
	}
	
	/**
	 * Ascending iterator over probability distribution entries.
	 * 
	 * @return an iterator over key entries
	 */
	public Iterator<Entry<T,Double>> ascendingIterator()
	{
		return entries.entrySet().stream().sorted(COMPARE_BY_VALUE).iterator();
	}

	@Override
	public Iterator<Entry<T,Double>> iterator()
	{
		return descendingIterator();
	}

	/**
	 * Sample an entry from the distribution.
	 * 
	 * @return Extracted entry
	 */
	public T sample()
	{
		return sample(new Random());
	}

	/**
	 * Sample an entry from the distribution using a given seed.
	 * 
	 * @return Extracted entry
	 */
	public T sample(final Random rand)
	{
		// Extracts a random number
		double sum = 0.0;
		double r = rand.nextDouble();
		
		// Generate a shuffled version of the key set
		List<T> sample = Lists.newArrayList(entries.keySet());
		Collections.shuffle(sample);
		
		for(T entry : sample)
		{
			sum += entries.get(entry);
			if(r < sum) return entry;
		}
		return null;
	}
	
	/**
	 * Converts the probability distribution to logarithmic space.
	 */
	public ProbabilityDistribution<T> toLogSpace()
	{		
		if(!this.logSpace)
		{
			entries = entries.keySet().stream().collect(Collectors.toMap(key -> key, key -> Math.log(entries.get(key))));
			this.logSpace = true;
		}
		return this;
	}
	
	/**
	 * Converts the probability distribution back from logarithmic space.
	 */
	public ProbabilityDistribution<T> toLinearSpace()
	{		
		if(this.logSpace)
		{
			entries = entries.keySet().stream().collect(Collectors.toMap(key -> key, key -> Math.exp(entries.get(key))));
			this.logSpace = false;
		}
		return this;
	}

	/**
	 * Compute the entropy of the distribution.
	 */
	public double entropy()
	{
		return entries.values().stream().mapToDouble(value -> logSpace? -1.0 * Math.exp(value) * value : -1.0 * value * Math.log(value)).sum();
	}
	
	/**
	 * Returns the probability distribution as {@link Map<T,Double>}.
	 */
	public Map<T,Double> asMap()
	{
		final Map<T,Double> entryMap = entries;
		return entryMap;
	}
	
	/**
	 * Remove from the distribution all entries with probability below a given threshold.
	 * 
	 * @param probabilityThreshold
	 */
	public void pruneBelow(double probabilityThreshold)
	{
		prune(k -> (entries.get(k) < probabilityThreshold));
	}
	
	/**
	 * Remove from the distribution all entries that satisfy a given constraint.
	 * 
	 * @param constraint Constraint over T entries
	 */
	public void prune(Predicate<T> constraint)
	{
		boolean currentLogSpace = logSpace;
		if(currentLogSpace) toLinearSpace();
		entries.keySet().stream().filter(constraint).collect(Collectors.toList()).forEach(entries::remove);
		normalize();
		if(currentLogSpace) toLogSpace();
		
	}
}
