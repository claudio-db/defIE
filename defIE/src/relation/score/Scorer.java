package relation.score;

import java.util.Arrays;

import relation.Relation;

/**
 * Functional interface modeling the scoring for a given {@link Relation}.
 * 
 * @author claudio
 */
@FunctionalInterface
public interface Scorer
{	
	/**
	 * Scoring function.
	 * Given a {@link Relation}, compute a score (as double).
	 * 
	 * @param r {@link Relation} object to be evaluated
	 */
	public double score(Relation r);
	
	/**
	 * Overload of the scoring function for an array of {@link Relation}s.
	 * 
	 * @param rels {@link Relation}s object to be evaluated
	 * @return Arrays of scores (as double)
	 */
	default public double[] score(Relation... rels)
	{
		return Arrays.stream(rels).mapToDouble(this::score).toArray();		
	}
	
}
