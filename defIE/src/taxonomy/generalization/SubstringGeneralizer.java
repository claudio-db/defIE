package taxonomy.generalization;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import graph.SemanticNode;
import relation.Relation;
import taxonomy.RelationTaxonomy.GeneralizationStrategy;

/**
 * {@link Generalizer} subclass that implements the substring generalization procedure.
 * 
 * @author claudio
 */
public class SubstringGeneralizer extends Generalizer
{
	// Check if s1 is a syntactic generalization of s2 (i.e. same head and a subset of modifiers)
	public static final BiFunction<SemanticNode,SemanticNode,Boolean> NODES_INCLUSION = (s1,s2) ->
	{
		// Heads have to be equal
		if(s1.getHead().lemma().equals(s2.getHead().lemma())) 
		{
			// Node lemmas of both node subgraphs
			Set<String> lemmasS1 = s1.getNodeSubgraph().vertexSet().stream().map(n -> n.getToken().lemma()).collect(Collectors.toSet());
			Set<String> lemmasS2 = s2.getNodeSubgraph().vertexSet().stream().map(n -> n.getToken().lemma()).collect(Collectors.toSet());
			// True if all nodes of s1 are contained in s2
			return lemmasS1.stream().allMatch(lemmasS2::contains);
		}
		else return false;
	};
	
	/**
	 * Constructor.
	 * Initializes the {@link Generalizer} with the substring {@link GeneralizationStrategy}.
	 */
	public SubstringGeneralizer()
	{
		super(GeneralizationStrategy.SUBSTRING);
	}

	@Override
	public boolean isGeneralization(Relation r1, Relation r2)
	{
		// First check if the two relation patterns are comparable
				if(ONLY_ONE_SEMANTIC_NODE.test(r1.getPattern()) && ONLY_ONE_SEMANTIC_NODE.test(r2.getPattern()) &&
						EQUAL_EXCEPT_FOR_SEMANTIC_NODES.apply(r1.getPattern(), r2.getPattern()))
					// Look if the (unique) semantic nodes in their patterns are generalization of one another 
					return NODES_INCLUSION.apply(r2.getPattern().getSemanticNodes().get(0), r1.getPattern().getSemanticNodes().get(0));
				else
					return false;
	}
}
