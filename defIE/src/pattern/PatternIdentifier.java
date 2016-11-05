package pattern;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

import graph.SemanticNode;
import graph.SynSemGraph;
import main.GraphBuilder;
import pattern.Pattern.InvalidPatternException;

/**
 * Class modeling the pattern identification process on a given {@link SynSemGraph}.
 * 
 * @author claudio
 */
public class PatternIdentifier
{
	// Syntactic-semantic graph
	private final SynSemGraph graph;
	// Set of extracted patterns
	private Set<Pattern> patterns;
	
	// Execution logger
	private Optional<Writer> log;
	// Status flag
	private boolean done;
	
	// Pattern filter
	private PatternFilter filter;
	
	// Function to generate pair IDs
	private final BiFunction<SemanticNode,SemanticNode,String> nodePairID = (n1,n2) ->
		n1+"_"+n1.getToken().index()+"_"+n2+"_"+n2.getToken().index();
		
	/**
	 * Constructor #1.
	 * Initializes the graph and empty set of patterns.
	 * 
	 * @param graph A {@link SynSemGraph} where patterns are to be identifed
	 * @param filter A {@link PatternFilter} to discard {@link Pattern}s according to some rule
	 * @param log An (optional) execution log
	 */		
	public PatternIdentifier(SynSemGraph graph, PatternFilter filter, Optional<Writer> log)
	{
		this.graph = graph;
		this.patterns = Sets.newHashSet();
		this.filter = filter;
		this.log = log;
		this.done = false;
		
		this.log.ifPresent(logger -> { try {
				logger.write("[ "+this.getClass().getSimpleName()+" ] Target definition: '"+this.graph.sourceDefinition()+"'\n");
			} catch(IOException e) {}
		});
	}
	
	/**
	 * Constructor #2.
	 * Initializes the graph and empty set of patterns.
	 * Does not specify any {@link PatternFilter}.
	 * 
	 * @param graph A {@link SynSemGraph} where patterns are to be identifed
	 * @param log An (optional) execution log
	 */		
	public PatternIdentifier(SynSemGraph graph, Optional<Writer> log)
	{
		this(graph, new PatternFilter(), log);
	}

	/**
	 * Getter for the current {@link PatternFilter}.
	 */
	public PatternFilter getFilter()
	{
		return filter;
	}
	
	/**
	 * Set a new {@link PatternFilter}.
	 * 
	 * @param filter New {@link PatternFilter}
	 */
	public void setFilter(PatternFilter filter)
	{
		this.filter = filter;
	}
	
	/**
	 * Run the pattern identification algorithm and retrieve all the {@link Pattern}s.
	 * 
	 * @return A set of extracted {@link Pattern}s
	 */
	private Set<Pattern> run()
	{
		// Start execution
		log.ifPresent(logger -> { try {
				logger.write("[ "+this.getClass().getSimpleName()+" ] ------ START ------ ("+new Date().toString()+")\n");
			} catch(IOException e) {}
		});
		long startTime = System.currentTimeMillis();
		// Consider all noun semantic nodes in the graph
		Set<String> covered = Sets.newHashSet();
		for (SemanticNode subjectArgument : graph.getSemanticNodes()) {
			for (SemanticNode objectArgument : graph.getSemanticNodes()) {
				if(subjectArgument.equals(objectArgument)) continue;
				// Discard all node pairs already covered
				if(!covered.contains(nodePairID.apply(subjectArgument, objectArgument)) && !covered.contains(nodePairID.apply(objectArgument, subjectArgument))
					// Left arguments should be on the left, right arguments should be on the right	
					&& (subjectArgument.getToken().index() < objectArgument.getToken().index()))
				{
					log.ifPresent(logger -> { try {
							logger.write("[ "+this.getClass().getSimpleName()+" ]\t Argument pair: <"+subjectArgument+", "+objectArgument+">\n");
						} catch(IOException e) {}
					});
					Optional.ofNullable(graph.shortestPath(subjectArgument, objectArgument)).ifPresent(path ->
					{
						try {
							Pattern p = new Pattern(path, graph.sourceDefinition());
							this.patterns.add(p);
							covered.add(nodePairID.apply(subjectArgument, objectArgument));
							log.ifPresent(logger -> { try {
									logger.write("[ "+this.getClass().getSimpleName()+" ]\t\tValid pattern: "+p+"\n");
								} catch(IOException e) {}
							});
						}
						// Discard patterns with no verb or copula
						catch (InvalidPatternException e) {
							log.ifPresent(logger -> { try {
									logger.write("[ "+this.getClass().getSimpleName()+" ]\t\tPattern "+
											e.getInvalidPath().stream().limit(e.getInvalidPath().size()-1)
											.map(edge -> edge.getTarget().getToken().toString()).collect(Collectors.joining(" "))+" discarded: "+e.getCauseMessage()+"\n");
								} catch(IOException e1) {}
							});
						}
					});
				}
			}
		}
		int nExtracted = this.patterns.size();
		log.ifPresent(logger -> { try {
				logger.write("[ "+this.getClass().getSimpleName()+" ]\t Extracted "+nExtracted+" patterns in total\n");
			} catch(IOException e) {}
		});
		// Discarding bad patterns according to the filter
		this.patterns = Sets.newHashSet(filter.apply(patterns));
		log.ifPresent(logger -> { try {
				logger.write("[ "+this.getClass().getSimpleName()+" ]\t Filtered out "+(nExtracted - this.patterns.size())+" bad patterns ("
						+this.patterns.size()+" remaining)\n");
			} catch(IOException e) {}
		});
		
		// Finish execution
		log.ifPresent(logger -> {
			try {
				logger.write("[ "+this.getClass().getSimpleName()+" ] ------ END ------ ("+new Date().toString()+")\n"
						+ "Elapsed time: "+(System.currentTimeMillis()-startTime)+" ms.\n");
				logger.close();
			} catch(IOException e) {}
		});
		this.done = true;
		return this.patterns;
	}
	
	/**
	 * Retrieve all the {@link Pattern}s found in the provided {@link SynSemGraph}.
	 * 
	 * @return A set of extracted {@link Pattern}s
	 */
	public Set<Pattern> identifyPatterns()
	{
		return done? patterns : run();
	}

	/**
	 * Pattern identification unit test with the example sentence of the paper.
	 * Constructs the {@link SynSemGraph} from the corresponding {@link GraphBuilder} unit test.
	 * 
	 * @return A set of extracted {@link Pattern}s
	 */
	public static Set<Pattern> unitTest()
	{
		// Construct the syntactic-semantic graph
		SynSemGraph graph = GraphBuilder.unitTest();
		
		// Extract patterns
		System.out.println("\n---------- Pattern Extraction: ----------");
		StringWriter stringlog = new StringWriter();
		@SuppressWarnings("unchecked")
		Set<Pattern> patterns = new PatternIdentifier(graph,
				new PatternFilter().addFilters(Pattern.NOMINAL_ARGUMENTS.negate(), Pattern.ENDS_WITH_MODIFIER, Pattern.STARTS_WITH_MODIFIER),
				Optional.of(stringlog))
				.identifyPatterns();
				
		// Print everything
		System.out.print(stringlog.toString());
		System.out.println("Obtained patterns (edges):");
		patterns.stream().map(pattern -> pattern.getEdgePath().stream().map(edge -> edge.getSource()+" "+edge.toString()+" "+edge.getTarget()).collect(Collectors.joining("\t"))).forEach(System.out::println);
		System.out.println("Triples:");
		patterns.stream().map(pattern -> pattern.toString()+"\n\tSubject: "+pattern.startingNode.toStringVerbose()+"\tObject: "+pattern.endingNode.toStringVerbose()).forEach(System.out::println);
		
		return patterns;
	}
	
	/**
	 * Pattern identification unit test with the example sentence of the paper.
	 * Constructs the {@link SynSemGraph} from the corresponding {@link GraphBuilder} unit test.
	 * 
	 * @return A set of extracted {@link Pattern}s
	 */
	public static Set<Pattern> unitTest2()
	{
		// Construct the syntactic-semantic graph
		SynSemGraph graph = GraphBuilder.unitTest2();
		
		// Extract patterns
		System.out.println("\n---------- Pattern Extraction: ----------");
		StringWriter stringlog = new StringWriter();
		@SuppressWarnings("unchecked")
		Set<Pattern> patterns = new PatternIdentifier(graph,
				new PatternFilter().addFilters(Pattern.NOMINAL_ARGUMENTS.negate(), Pattern.ENDS_WITH_MODIFIER, Pattern.STARTS_WITH_MODIFIER),
				Optional.of(stringlog))
				.identifyPatterns();
				
		// Print everything
		System.out.print(stringlog.toString());
		System.out.println("Obtained patterns (edges):");
		patterns.stream().map(pattern -> pattern.getEdgePath().stream().map(edge -> edge.getSource()+" "+edge.toString()+" "+edge.getTarget()).collect(Collectors.joining("\t"))).forEach(System.out::println);
		System.out.println("Triples:");
		patterns.stream().map(pattern -> pattern.toString()+"\n\tSubject: "+pattern.startingNode.toStringVerbose()+"\tObject: "+pattern.endingNode.toStringVerbose()).forEach(System.out::println);
		
		return patterns;
	}
	
	/**
	 * Main execution routine.
	 * 
	 * @param args
	 */
	public static void main(String[] args)
	{
		unitTest2();
	}
}
