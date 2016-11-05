package relation;

import java.util.Collection;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import taxonomy.ConceptTaxonomy;
import utils.ProbabilityDistribution;

/**
 * Class representing a {@link ProbabilityDistribution} over semantic classes associated with a {@link Relation}.
 * Creating an instance of a {@link SemanticTypeDistribution} from a set of {@link Concept}s requires a {@link ConceptTaxonomy}.
 * 
 * @author claudio
 */
public class SemanticTypeDistribution extends ProbabilityDistribution<Concept>
{
	// Reference concept taxonomy
	private static ConceptTaxonomy taxonomy = ConceptTaxonomy.getInstance();
	// Hypernym level associated with the taxonomy
	private final int hypernymLevel;
	
	/**
	 * Constructor.
	 * Creates the {@link ProbabilityDistribution} from an input collection of {@link Concept}s by first mapping each {@link Concept} to its k-level superclass. 
	 * 
	 * @param entries Input collection of {@link Concept}s
	 * @param k Hypernym level of the distribution
	 */
	public SemanticTypeDistribution(Collection<Concept> entries, int k)
	{
		// Map each concept to the corresponding superclass
		super(entries.stream().map(concept -> Lists.reverse(taxonomy.taxonomize(concept, k)).get(0)).collect(Collectors.toList()));
		this.hypernymLevel = k;
	}
	
	/**
	 * Getter for the hypernym level of the distribution.
	 */
	public int getHypernymLevel()
	{
		return hypernymLevel;
	}
}
