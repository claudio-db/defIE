package relation;

import java.io.Serializable;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

import graph.Node;
import graph.SemanticNode;
import graph.SynSemGraph;
import pattern.Pattern;
import pattern.Pattern.RelationPattern;
import utils.ProbabilityDistribution;

/**
 * Class modeling a semantic {@link Relation} obtained from {@link Pattern}s in a {@link SynSemGraph}.
 * Stores information about the relation pattern, the set of {@link ArgumentPair}s and the semantic types.
 * 
 * @author claudio
 */
public class Relation implements Serializable
{
	private static final long serialVersionUID = -4724410856532398372L;
	
	// Associated relation pattern
	private final RelationPattern pattern;
	// Associated set of extracted triples
	private Set<ArgumentPair> extractions;
	
	// Domain and range type distribution
	private ProbabilityDistribution<Concept> domainTypes, rangeTypes;
	// Hypernym level of both distributions
	private int hypernymLevel;
	
	/**
	 * Constructor #1.
	 * Initializes an empty {@link Relation} from a {@link RelationPattern} with no associated triples.
	 * 
	 * @param pattern Associated {@link RelationPattern}
	 * @param hypernymLevel Hypernym level associated with the type distributions
	 */
	protected Relation(RelationPattern pattern, int hypernymLevel)
	{
		this.pattern = pattern;
		this.extractions = Sets.newHashSet();
		this.domainTypes = null;
		this.rangeTypes = null;
		this.hypernymLevel = hypernymLevel;
	}
	
	/**
	 * Constructor #2.
	 * Initializes an empty {@link Relation} from a {@link RelationPattern} with no associated triples.
	 * Hypernym level is set to a default value of 1 (i.e. direct hypernyms)
	 * 
	 * @param pattern Associated {@link RelationPattern}
	 */
	protected Relation(RelationPattern pattern)
	{
		this(pattern, 1);
	}
	
	/**
	 * Getter for the {@link RelationPattern}.
	 */
	public RelationPattern getPattern()
	{
		return pattern;
	}
	
	/**
	 * Add a new {@link ArgumentPair} to the {@link Relation}.
	 * 
	 * @param newArgumentPair {@link ArgumentPair} to add
	 */
	protected void addArgumentPair(ArgumentPair newArgumentPair)
	{
		extractions.add(newArgumentPair);
		if(domainTypes != null) domainTypes = null;
		if(rangeTypes != null) domainTypes = null;
	}
	
	/**
	 * Compute the {@link SemanticTypeDistribution}s for domain and range of the {@link Relation}.
	 */
	protected void computeSemanticTypes()
	{
		this.domainTypes = new SemanticTypeDistribution(getDomain(), hypernymLevel);
		this.rangeTypes = new SemanticTypeDistribution(getRange(), hypernymLevel);
	}
	
	/**
	 * Change the hypernym level of both {@link SemanticTypeDistribution}s in the {@link Relation}.
	 * Both distributions are computed again from scratch using the new hypernym level.
	 * 
	 * @param newLevel new hypernym level of the {@link SemanticTypeDistribution}s
	 */
	public void setHypernymLevel(int newLevel)
	{
		this.hypernymLevel = newLevel;
		computeSemanticTypes();
	}
	
	/**
	 * Add a new set of {@link ArgumentPair}s to the {@link Relation}.
	 * 
	 * @param newArgumentPairs {@link ArgumentPair}s to add
	 */
	public void addExtractions(Iterable<ArgumentPair> newArgumentPairs)
	{
		newArgumentPairs.forEach(this::addArgumentPair);
		computeSemanticTypes();
	}
	
	/**
	 * Getter for the set of extracted {@link ArgumentPair}s.
	 */
	public Set<ArgumentPair> getExtractions()
	{
		return extractions;
	}

	/**
	 * List of left (domain) arguments of the {@link Relation}.
	 */
	public List<Concept> getDomain()
	{
		return extractions.stream().map(ArgumentPair::left).collect(Collectors.toList());
	}
	
	/**
	 * List of right (range) arguments of the {@link Relation}.
	 */
	public List<Concept> getRange()
	{
		return extractions.stream().map(ArgumentPair::right).collect(Collectors.toList());
	}
	
	/**
	 * Getter for the {@link ProbabilityDistribution} over domain types.
	 * 
	 * @throws EmptyRelationException
	 */
	public ProbabilityDistribution<Concept> getDomainTypes()
			throws EmptyRelationException
	{
		return Optional.ofNullable(domainTypes).orElseThrow(() -> new EmptyRelationException(this));
	}

	/**
	 * Getter for the {@link ProbabilityDistribution} over range types.
	 * 
	 * @throws EmptyRelationException
	 */
	public ProbabilityDistribution<Concept> getRangeTypes()
			throws EmptyRelationException
	{
		return Optional.ofNullable(rangeTypes).orElseThrow(() -> new EmptyRelationException(this));
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((pattern == null) ? 0 : pattern.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		
		Relation other = (Relation) obj;
		return pattern.equals(other.pattern);
	}

	public String toString()
	{
		// Include argument types (if available)
		String leftArgumentType = Optional.ofNullable(domainTypes).isPresent()? domainTypes.argMax().toString() : Pattern.leftPlaceholder;
		String rightArgumentType = Optional.ofNullable(rangeTypes).isPresent()? rangeTypes.argMax().toString() : Pattern.rightPlaceholder;
		
		// Relation pattern signature
		List<String> signatureTokens = pattern.getNodes().stream().map(Node::toString).collect(Collectors.toList());
		signatureTokens.set(pattern.leftArgumentIndex(), "<"+leftArgumentType+">");
		signatureTokens.set(pattern.rightArgumentIndex(), "<"+rightArgumentType+">");
		
		return signatureTokens.stream().collect(Collectors.joining(" "));
	}
	
	/**
	 * Create a {@link Relation} from a set of extracted {@link Pattern}s.
	 * 
	 * @param patterns Set of extracted {@link Pattern}s
	 * @param hypernymLevel Hypernym level associated with the type distributions
	 * @return The corresponding {@link Relation}
	 */
	public static Relation createRelationFromPatterns(Set<Pattern> patterns, int hypernymLevel)
	{
		try {
			// Initialize an empty relation using a relation pattern from the set
			Relation newRelation = new Relation(patterns.stream().findFirst().map(Pattern::getPattern).get(), hypernymLevel);
			
			// Convert each pattern into argument pair
			patterns.stream()
					.map(pattern ->
					{
						try {
							// Check pattern validity
							if(!pattern.getPattern().toStringLemmatized().equals(newRelation.getPattern().toStringLemmatized()))
								throw new IllegalRelationException(pattern.getPattern(), newRelation.getPattern());
							
							// Create argument pair
							return new ArgumentPair(
									new Concept(((SemanticNode) pattern.getExtremes().first()).getSenseAttachment().getSenseID(),
											((SemanticNode) pattern.getExtremes().first()).getTextFragment(true)),
									new Concept(((SemanticNode) pattern.getExtremes().second()).getSenseAttachment().getSenseID(),
											((SemanticNode) pattern.getExtremes().second()).getTextFragment(true)),
									pattern.getSource(), pattern.argumentScore());
						}
						catch(IllegalRelationException e) {
							System.err.println("[ "+Relation.class.getSimpleName()+" ] ERROR! Invalid set of patterns:\n\t"
									+ e.pattern1.toStringLemmatized()+" != "+e.pattern2.toStringLemmatized());
							e.printStackTrace();
							System.exit(1);
						}
						return null;
					})
					.filter(pattern -> Optional.ofNullable(pattern).isPresent())
					.forEach(newRelation::addArgumentPair);
			
			// Construct semantic type distributions
			newRelation.computeSemanticTypes();
			
			return newRelation;
		}
		catch(NoSuchElementException e) {
			System.err.println("[ "+Relation.class.getSimpleName()+" ] ERROR: Empty set of patterns!");
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}
	
	public static class EmptyRelationException extends Exception
	{
		private static final long serialVersionUID = 2601267009614000183L;
		
		// Empty relation that caused the exception
		private final Relation emptyRelation;
		
		public EmptyRelationException(Relation emptyRelation)
		{
			this.emptyRelation = emptyRelation;
		}
		
		public Relation getEmptyRelation()
		{
			return emptyRelation;
		}
	}
	
	static class IllegalRelationException extends Exception
	{
		private static final long serialVersionUID = -4553291336242752337L;
		
		// Pair of incompatible relation patterns
		private final RelationPattern pattern1, pattern2;
		
		public IllegalRelationException(RelationPattern pattern1, RelationPattern pattern2)
		{
			this.pattern1 = pattern1;
			this.pattern2 = pattern2;
		}
		
		public RelationPattern[] getIncompatiblePatterns()
		{
			return new RelationPattern[]{ pattern1, pattern2 };
		}
	}
}
