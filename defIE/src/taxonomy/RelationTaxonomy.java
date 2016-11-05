package taxonomy;

import java.util.Map;
import java.util.function.Supplier;

import relation.Relation;
import taxonomy.generalization.Generalizer;
import taxonomy.generalization.HypernymGeneralizer;
import taxonomy.generalization.SubstringGeneralizer;

/**
 * Class modeling a {@link Taxonomy} of {@link Relation}s.
 * Its edges are categorized in two classes: hypernym generalization and substring generalization (see paper).
 * 
 * @author claudio
 */
public class RelationTaxonomy extends Taxonomy<Relation, RelationTaxonomy.GeneralizationStrategy>
{
	/**
	 * Constructor.
	 * Takes as input the taxonomy as adjacency list.
	 * 
	 * @param edgeMap Nodes and edges of the {@link Taxonomy}.
	 */
	public RelationTaxonomy(Map<Relation, Map<Relation, GeneralizationStrategy>> edgeMap)
	{
		super(edgeMap);
	}

	/**
	 * Type of generalization.
	 */
	public enum GeneralizationStrategy
	{
		HYPERNYM(() -> new HypernymGeneralizer()),
		SUBSTRING(() -> new SubstringGeneralizer());
		
		// Corresponding generalizer
		private final Supplier<Generalizer> generalizer;
		
		private GeneralizationStrategy(Supplier<Generalizer> generalizer)
		{
			this.generalizer = generalizer;
		}
		
		public Generalizer generalizer()
		{
			return generalizer.get();
		}
	}
}
