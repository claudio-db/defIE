package utils;

import java.io.Serializable;
import java.util.Comparator;

import edu.stanford.nlp.util.Triple;

/**
 * Some utility functions for handling disambiguated glosses and sense-level information.
 * 
 * @author claudio
 */
public class DisambiguateUtils
{
	public static final String SEPARATOR = "_";
	
	/**
	 * Auxiliary class to store information about a disambiguation output.
	 */	
	public static class Disambiguation implements Comparable<Disambiguation>, Serializable
	{
		private static final long serialVersionUID = 2841196730181256586L;
		
		// Sense mapping
		private final Triple<Integer,Integer,String> senseMap;
		// Confidence score
		private final double confidenceScore;
		
		/**
		 * Constructor #1.
		 * 
		 * @param smp Sense mapping as triple < start token offset (inclusive), end token offset (exclusive), sense ID >
		 */
		public Disambiguation(Triple<Integer,Integer,String> smp)
		{
			this.senseMap = smp;
			this.confidenceScore = 0.0;
		}
		
		/**
		 * Constructor #2.
		 * 
		 * @param smp Sense mapping as triple < start token offset (inclusive), end token offset (exclusive), sense ID >
		 * @param cs Confidence score
		 */
		public Disambiguation(Triple<Integer,Integer,String> smp, double cs)
		{
			this.senseMap = smp;
			this.confidenceScore = cs;
		}

		/**
		 * Getter for the starting token offset (inclusive).
		 */
		public int getStartingTokenOffset()
		{
			return this.senseMap.first();
		}

		/**
		 * Getter for the ending token offset (exclusive).
		 */
		public int getEndingTokenOffset()
		{
			return this.senseMap.second();
		}

		/**
		 * Getter for the length of the match.
		 */
		public int getMatchLength()
		{
			return this.senseMap.second() - this.senseMap.first();
		}
		
		/**
		 * Getter for the sense ID.
		 */
		public String getSenseID()
		{
			return this.senseMap.third();
		}
		
		/**
		 * Getter for the confidence score.
		 */
		public double getConfidence()
		{
			return this.confidenceScore;
		}

		@Override
		public int compareTo(Disambiguation o)
		{
			return CONFIDENCE_COMPARE.thenComparing(LENGTH_COMPARE).compare(this, o);
		}
		
		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			long temp;
			temp = Double.doubleToLongBits(confidenceScore);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			result = prime * result + ((senseMap == null) ? 0 : senseMap.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			
			Disambiguation other = (Disambiguation) obj;
			return (Double.doubleToLongBits(confidenceScore) == Double.doubleToLongBits(other.confidenceScore)) && senseMap.equals(other.senseMap);
		}

		@Override
		public String toString()
		{
			return senseMap.first()+SEPARATOR+senseMap.second()+SEPARATOR+senseMap.third()+SEPARATOR+confidenceScore;
		}
	}
	
	// Comparator based on the confidence score
	public static final Comparator<Disambiguation> CONFIDENCE_COMPARE = Comparator.comparingDouble(Disambiguation::getConfidence);
	// Comparator based on the match length
	public static final Comparator<Disambiguation> LENGTH_COMPARE = Comparator.comparingInt(Disambiguation::getMatchLength);
	
	/**
	 * Check if two given {@link Disambiguation}s are overlapping.
	 * 
	 * @param d1 First {@link Disambiguation}
	 * @param d2 Second {@link Disambiguation}
	 * @return 'true' if the {@link Disambiguation}s are overlapping, 'false' otherwise
	 */
	public static boolean overlap(Disambiguation d1, Disambiguation d2)
	{
		return (d2.getStartingTokenOffset() >= d1.getStartingTokenOffset()) &&
			   (d2.getStartingTokenOffset() <= d1.getEndingTokenOffset());
	}
}
