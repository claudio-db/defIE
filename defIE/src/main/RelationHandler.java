package main;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Maps;

import relation.Relation;
import relation.Relation.EmptyRelationException;
import relation.score.RelationQualityScorer;
import relation.score.RelationQualityScorer.Score;
import relation.score.Scorer;
import taxonomy.RelationTaxonomy;
import taxonomy.RelationTaxonomy.GeneralizationStrategy;
import taxonomy.generalization.Generalizer;

/**
 * Class modeling the scoring and taxonomization of a set of {@link Relation}s.
 * 
 * @author claudio
 */
public class RelationHandler
{
	// Relations
	private Set<Relation> relations;
	// Relation scorer
	private Scorer scorer;
	// Scored relations
	private Map<Relation,Double> relationMap;
	
	// Relation generalizer
	private Generalizer generalizer;
	
	/**
	 * Constructor.
	 * 
	 * @param relations Input {@link Relation}s to be processed
	 * @param scorer Relation {@link Scorer}
	 * @param log Execution log
	 */
	public RelationHandler(Set<Relation> relations, Scorer scorer, Generalizer generalizer)
	{
		this.relations = relations;
		this.scorer = scorer;
		this.generalizer = generalizer;
		this.relationMap = Maps.newTreeMap(Comparator.comparingDouble(relationMap::get));
	}
	
	/**
	 * Getter for the input set of {@link Relation}s.
	 */
	public Set<Relation> inputRelations()
	{
		return relations;
	}
	
	/**
	 * Getter for the current {@link Scorer}.
	 */
	public Scorer currentScorer()
	{
		return scorer;
	}
	
	/**
	 * Getter for the current {@link Generalizer}.
	 */	
	public Generalizer currentGeneralizer()
	{
		return generalizer;
	}
	
	private void scoreRelations()
	{
		relations.forEach(relation -> relationMap.put(relation, scorer.score(relation)));
	}
	
	/**
	 * Getter for the scored set of input {@link Relation}s.
	 * 
	 * @return A sorted map indexed by {@link Relation}
	 */
	public Map<Relation,Double> getScoredRelations()
	{
		if (relationMap.isEmpty()) scoreRelations();
		return relationMap;
	}
	
	/**
	 * Score the input {@link Relation}s using a {@link RelationQualityScorer}.
	 * 
	 * @return A sorted map indexed by {@link Relation}
	 */
	public Map<Relation,Score> getQualityScoredRelations()
	{
		Map<Relation,Score> relationQualityMap = Maps.newTreeMap(Comparator.comparingDouble(relationMap::get));
		RelationQualityScorer qualityScorer = new RelationQualityScorer();
		relations.stream().forEach(relation -> relationQualityMap.put(relation, qualityScorer.scoreComplete(relation)));
		
		return relationQualityMap;
	}	

	/**
	 * Build a {@link RelationTaxonomy} from the input {@link Relation}s.
	 */
	public RelationTaxonomy getRelationTaxonomy()
	{
		return generalizer.taxonomize(relations, false);
	}

	@SuppressWarnings("resource")
	public static void printRelationSemanticTypes(Collection<Relation> relations, String outputFile)
	{
		try {
			// Open output stream
			PrintWriter out = new PrintWriter(new FileWriter(outputFile));
			// Write distribution of types for each relation
			relations.stream().map(r ->
			{
				// Relation pattern
				String relPattern = r.getPattern().toStringLemmatized();
				String domainString = "", rangeString = "";
				try {
					// Domain and range distribution strings
					domainString = r.getDomainTypes().asMap().entrySet().stream().map(e -> e.getKey()+"_"+e.getValue()).collect(Collectors.joining("\t"));
					rangeString = r.getRangeTypes().asMap().entrySet().stream().map(e -> e.getKey()+"_"+e.getValue()).collect(Collectors.joining("\t"));
				}
				catch (EmptyRelationException e1)
				{
					System.err.println("[ "+RelationBuilder.class.getSimpleName()+" ] ERROR: unable to retrieve domain and range types from '"+relPattern+"'!");
					e1.printStackTrace();
					System.exit(1);
				}
				
				return relPattern+"_X\t"+domainString+"\n"+relPattern+"_Y\t"+rangeString;
			}).forEach(out::println);	
			out.close();
		} 
		catch(IOException e)
		{
			System.err.println("[ "+RelationBuilder.class.getSimpleName()+" ] ERROR: unable to write output to '"+outputFile+"'!");
			System.exit(1);
		}
	}
	
	/**
	 * Print the {@link Relation} ranking in human-readable form.
	 * The output format is one {@link Relation} per line, from the top scoring downwards:
	 * 
	 * 		RELATION_SIGNATURE	\t	SCORE	\t ENTROPY	\t 	FREQUENCY	\t	PATTERN_LENGTH
	 * 
	 * @param relation {@link Relation} to print
	 * @param outputFile Target file
	 */
	public static void printScoredRelationToFile(Map<Relation,Score> relations, String outputFile)
	{
		try {
			// Open output stream
			PrintWriter out = new PrintWriter(new FileWriter(outputFile));	
			// Write relation ranking
			relations.entrySet().stream()
				.sorted(Comparator.comparing(Entry<Relation,Score>::getValue).reversed())
				.map(e -> e.getKey().toString()+"\t"+e.getValue().getScore()+"\t"+e.getValue().getEntropy()+"\t"+e.getValue().getFrequency()+"\t"+e.getValue().getLength())
				.forEach(out::println);
			
			// Clean up
			out.close();
		}
		catch(IOException e)
		{
			System.err.println("[ "+RelationBuilder.class.getSimpleName()+" ] ERROR: unable to write output to '"+outputFile+"'!");
			System.exit(1);
		}
	}

	/**
	 * Print the {@link RelationTaxonomy} in human-readable form.
	 * The output format is an adjacency list, i.e. one node per line, as follows:
	 * 
	 * 		SOURCE	\t	TARGET1_TYPE1	\t	...	 \t	TARGETN_TYPEN
	 * 
	 * where SOURCE is the source {@link Relation}, TARGET1..nN are the target {@link Relation}s and TYPE1...N are the {@link GeneralizationStrategy} types.
	 * 
	 * @param taxonomy {@link RelationTaxonomy} to print
	 * @param outputFile Target file
	 */
	public static void printRelationTaxonomy(RelationTaxonomy taxonomy, String outputFile)
	{
		try {
			// Open output stream
			PrintWriter out = new PrintWriter(new FileWriter(outputFile));	
			// Write taxonomy one edge list per line
			taxonomy.iterator().forEachRemaining(node -> {
				String edgeString = taxonomy.getAllEdges(node).stream().map(e -> e.toString()).collect(Collectors.joining("\t"));
				out.println(node+"\t"+edgeString);
			});			
			// Clean up
			out.close();
		}
		catch(IOException e)
		{
			System.err.println("[ "+RelationBuilder.class.getSimpleName()+" ] ERROR: unable to write output to '"+outputFile+"'!");
			System.exit(1);
		}		
	}
}
