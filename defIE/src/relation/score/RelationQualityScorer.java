package relation.score;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.google.common.collect.Maps;

import relation.Relation;

/**
 * Class modeling the score computation for a given {@link Relation}.
 * The score is based on the domain and range entropy value, the number of occurrences of the relation and the path length.
 * 
 * @author claudio
 */
public class RelationQualityScorer implements Scorer
{
	// Entropy value
	private Scorer entropyScorer;
	// Frequency (number of occurrences)
	private Scorer frequencyScorer;
	// Pattern length score
	private Scorer lengthScorer;
	
	// Entropy of the argument sets
	public static final Scorer ENTROPY_SCORER = r ->
	{
		try {
			return 0.5*r.getDomainTypes().entropy()+0.5*r.getRangeTypes().entropy();
		} catch (Relation.EmptyRelationException e) {
			System.err.println("[ "+RelationQualityScorer.class.getSimpleName()+" ] ERROR: Unable to compute entropy!");
			e.printStackTrace();
			System.exit(1);
		}
		return Double.NaN;
	};
	// Number of occurrences of the relation
	public static final Scorer FREQUENCY_SCORER = r -> (double) r.getExtractions().size();
	// Length of the relation pattern
	public static final Scorer LENGTH_SCORER = r -> (double) r.getPattern().patternLength();
	
	/**
	 * Default constructor.
	 * 
	 * Initializes all the {@link Scorer}s employed.
	 */
	public RelationQualityScorer()
	{
		this.entropyScorer = ENTROPY_SCORER;
		this.frequencyScorer = FREQUENCY_SCORER;
		this.lengthScorer = LENGTH_SCORER;
	}
	
	@Override
	public double score(Relation r)
	{
		// Empirical formula of the paper
		return frequencyScorer.score(r) / ((entropyScorer.score(r) + 1) * lengthScorer.score(r));
	}
	
	/**
	 * Assigns a score to a given {@link Relation}.
	 * Returns the complete score along with all sub-scores (entropy, frequency, length).
	 * 
	 * @param r {@link Relation} to be scored
	 * @return A {@link Score} object containing all the scoring information
	 */
	public Score scoreComplete(Relation r)
	{
		return new Score(this.score(r), entropyScorer.score(r), frequencyScorer.score(r), lengthScorer.score(r));
	}

	/**
	 * Scores a set of {@link Relation}s and orders them by decreasing score value.
	 * 
	 * @param rels Collection of {@link Relation}s to be scored
	 * @return An ordered map of those {@link Relation}s indexed by score value
	 */
	public Map<Score,Relation> scoreComplete(Collection<Relation> rels)
	{
		// Initialize ordered map
		Map<Score,Relation> scoreMap = Maps.newTreeMap(Collections.reverseOrder());
		// Score relations one-by-one and put them in the map
		rels.forEach(r -> scoreMap.put(this.scoreComplete(r), r));
		
		return scoreMap;
	}
	
	/**
	 * Auxiliary inner class for {@link RelationQualityScorer} holding the score value and all sub-scores (entropy, length, occurrences).
	 * 
	 * @author claudio
	 */
	public class Score implements Serializable, Comparable<Score>
	{	
		private static final long serialVersionUID = -9223315272289780288L;
		
		// Complete score
		private final double score;
		// Entropy score
		private final double entropy;
		// Frequency score
		private final double frequency;
		// Pattern length score
		private final double length;
		
		/**
		 * Constructor.
		 * Initializes all scores.
		 * 
		 * @param s Complete score
		 * @param e Entropy of the argument sets
		 * @param f Number of occurrences
		 * @param l Relation pattern length
		 */
		public Score(double s, double e, double f, double l)
		{
			this.score = s;
			this.entropy = e;
			this.frequency = f;
			this.length = l;
		}
		
		/**
		 * Getter for the complete score.
		 */
		public double getScore() 
		{
			return score;
		}
		
		/**
		 * Getter for the entropy score.
		 */
		public double getEntropy()
		{
			return entropy;
		}

		/**
		 * Getter for the frequency score.
		 */
		public double getFrequency()
		{
			return frequency;
		}
		
		/**
		 * Getter for the length score.
		 */
		public double getLength()
		{
			return length;
		}

		@Override
		public int compareTo(Score o)
		{
			return Double.compare(this.score, o.score);
		}
		
		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result;
			long temp;
			temp = Double.doubleToLongBits(entropy);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(frequency);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(length);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(score);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			return result;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			
			Score other = (Score) obj;
			if (Double.doubleToLongBits(entropy) != Double.doubleToLongBits(other.entropy)) return false;
			if (Double.doubleToLongBits(frequency) != Double.doubleToLongBits(other.frequency)) return false;
			if (Double.doubleToLongBits(length) != Double.doubleToLongBits(other.length)) return false;
			if (Double.doubleToLongBits(score) != Double.doubleToLongBits(other.score)) return false;
			
			return true;
		}

		@Override
		public String toString()
		{
			return Double.toString(score);
		}
		
		/**
		 * Verbose overload of toString().
		 * Prints the score with the format: [ score: S, entropy: E, frequency: F, length: L ].
		 * 
		 * @return String description of {@link RelationQualityScorer.Score} 
		 */
		public String toStringVerbose()
		{
			return "[ score: "+score+", entropy: "+entropy+", frequency: "+frequency+", length: "+length+" ]";
		}
	}

}
