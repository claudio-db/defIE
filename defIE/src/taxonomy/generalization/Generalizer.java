package taxonomy.generalization;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import graph.SemanticNode;
import pattern.Pattern.RelationPattern;
import relation.Relation;
import taxonomy.RelationTaxonomy;
import taxonomy.RelationTaxonomy.GeneralizationStrategy;
import taxonomy.Taxonomy.Edge;

/**
 * Abstract class modeling the {@link Relation} generalization procedure.
 * 
 * @author claudio
 */
public abstract class Generalizer
{
	// Generalizer precondition #1
	public static final Predicate<RelationPattern> ONLY_ONE_SEMANTIC_NODE = r -> r.getSemanticNodes().size() == 1;
	// Generalizer precondition #2
	public static final BiFunction<RelationPattern, RelationPattern, Boolean> EQUAL_EXCEPT_FOR_SEMANTIC_NODES = (r1,r2) ->
			IntStream.range(0, r1.getNodes().size()).mapToObj(i ->
						{
							try {
								// Don't compare semantic nodes
								if ((r1.getNodes().get(i) instanceof SemanticNode) && (r2.getNodes().get(i) instanceof SemanticNode))
									return true;
								else
									return r1.getNodes().get(i).getToken().lemma().equals(r2.getNodes().get(i).getToken().lemma());
							}
							// If pattern lengths don't match return false
							catch(IndexOutOfBoundsException e) { return false; }
						})
						.reduce(true, (check1, check2) -> check1 && check2);
		
	// Generalizer type
	protected final GeneralizationStrategy type;
	
	/**
	 * Constructor.
	 * 
	 * @param type {@link GeneralizationStrategy} of the {@link Generalizer}
	 */
	public Generalizer(GeneralizationStrategy type)
	{
		this.type = type;
	}
		
	/**
	 * Generalization function.
	 * Given a pair of {@link Relation}s, decides whether a hypernymy/superclass relationship holds between them.
	 * 
	 * @param r1 Candidate hyponym {@link Relation}
	 * @param r2 Candidate hypernym {@link Relation}
	 * @return 'true' if r2 is a generalization/hypernym/superclass of r1
	 */
	public abstract boolean isGeneralization(Relation r1, Relation r2);
	
	/**
	 * Generates an {@link Edge} if a superclass relationship is found between two given {@link Relation}s.
	 * 
	 * @param r1 Source {@link Relation}
	 * @param r2 Target {@link Relation}
	 * @return The {@link GeneralizationStrategy} of the {@link Generalizer} if an edge is found
	 */
	public Optional<GeneralizationStrategy> taxonomize(Relation r1, Relation r2)
	{
		return Optional.ofNullable(isGeneralization(r1, r2) ? this.type : null);
	}
	
	/**
	 * Generates a {@link RelationTaxonomy} from a starting set of {@link Relation}s.
	 * 
	 * @param r Set of {@link Relation} nodes
	 * @param verbose Verbose flag
	 * @return The resulting {@link RelationTaxonomy}
	 */
	public RelationTaxonomy taxonomize(Set<Relation> r, boolean verbose)
	{
		// Initialize edge map
		Map<Relation, Map<Relation, GeneralizationStrategy>> edgeMap = Maps.newHashMap();
		
		// Check all possible (distinct) pairs of relations 
		Set<String> done = Sets.newHashSet();
		if(verbose) System.err.println("[ "+this.getClass().getSimpleName()+" ] Starting set of relation nodes: "+r.size()+" elements.");
		int count = 0;
		for(Relation rel1 : r)
		{
			for(Relation rel2 : r)
			{
				// Check for already processed pairs
				if( !rel1.equals(rel2) &&
						!done.contains(rel1.toString()+"_"+rel2.toString()) &&
						!done.contains(rel2.toString()+"_"+rel1.toString()) )
				{
					// Compute taxonomy edge rel1-to-rel2
					taxonomize(rel1,rel2).ifPresent(e -> {
						edgeMap.putIfAbsent(rel1, Maps.newHashMap());
						edgeMap.get(rel1).put(rel2, this.type);
						done.add(rel1.toString()+"_"+rel2.toString());
					});
					// Compute taxonomy edge rel2-to-rel1
					taxonomize(rel2,rel1).ifPresent(e -> {
						edgeMap.putIfAbsent(rel2, Maps.newHashMap());
						edgeMap.get(rel2).put(rel1, this.type);
						done.add(rel2.toString()+"_"+rel1.toString());
					});
				}
			}
			++count;
			if(verbose && (count%10000 == 0)) System.err.println("[ "+this.getClass().getSimpleName()+" ] "+count+" relation processed...");
		}
		if(verbose) System.err.println("[  "+this.getClass().getSimpleName()+" ] Done. Generated taxonomy with "+
				edgeMap.size()+" nodes and "+edgeMap.values().stream().mapToInt(Map::size).sum()+" edges.");
		
		// Generate relation taxomomy
		return new RelationTaxonomy(edgeMap);
	}
	
}
