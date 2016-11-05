package taxonomy.generalization;

import relation.Concept;
import relation.Relation;
import taxonomy.ConceptTaxonomy;
import taxonomy.RelationTaxonomy.GeneralizationStrategy;

/**
 * {@link Generalizer} subclass that implements the hypernym generalization procedure.
 * Using the {@link HypernymGeneralizer} requires a {@link ConceptTaxonomy}.
 * 
 * @author claudio
 */
public class HypernymGeneralizer extends Generalizer
{
	// Reference concept taxonomy
	private static ConceptTaxonomy taxonomy = ConceptTaxonomy.getInstance();
	
	/**
	 * Constructor.
	 * Initializes the {@link Generalizer} with the hypernym {@link GeneralizationStrategy}.
	 */
	public HypernymGeneralizer()
	{
		super(GeneralizationStrategy.HYPERNYM);
	}

	@Override
	public boolean isGeneralization(Relation r1, Relation r2)
	{
		// First check if the two relation patterns are comparable
		if(ONLY_ONE_SEMANTIC_NODE.test(r1.getPattern()) && ONLY_ONE_SEMANTIC_NODE.test(r2.getPattern()) &&
				EQUAL_EXCEPT_FOR_SEMANTIC_NODES.apply(r1.getPattern(), r2.getPattern()))
			// Look if the concept corresponding to the unique semantic node of r1 is a subclass of the one of r2
			return taxonomy.hasSuperclass(
					new Concept(r1.getPattern().getSemanticNodes().get(0).getSense()),
					new Concept(r2.getPattern().getSemanticNodes().get(0).getSense()));
		else
			return false;
	}
}
