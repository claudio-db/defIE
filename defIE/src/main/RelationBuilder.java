package main;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import definition.Definition;
import graph.Edge;
import graph.Node;
import graph.SynSemGraph;
import pattern.Pattern;
import pattern.PatternFilter;
import pattern.PatternIdentifier;
import relation.Relation;
import utils.FileUtils;

/**
 * Class modeling the extraction of semantic {@link Relation}s from {@link SynSemGraph}s.
 * 
 * @author claudio
 */
public class RelationBuilder
{
	// Syntactic-semantic graphs
	private Iterable<SynSemGraph> graphs;
	// Filter for relation patterns
	private PatternFilter filter;
	//  Relation pattern maps (grouped by thread)
	private Map<Integer,Map<String,Set<Pattern>>> relationMap;
	
	// Hypernym level for the relation type distributions
	private int hypernymLevel;
	
	// Execution logger
	private Optional<Writer> log;
	// Multithreading service
	private ExecutorService service;
	private int batchSize;
	
	// State of the relation extraction process
	private State state;
	public enum State
	{
		READY,
		PATTERN_EXTRACTION,
		RELATION_BUILDING
	}
	
	/**
	 * Constructor #1.
	 * 
	 * @param graphs Input collection of {@link SynSemGraph}s
	 * @param filter {@link PatternFilter} for relation patterns
	 * @param hypernymLevel Hypernym level for the {@link Relation} type distributions
	 * @param log Execution logger
	 * @param batchSize Number of graphs to be processed in parallel
	 */
	public RelationBuilder(Iterable<SynSemGraph> graphs, PatternFilter filter, int hypernymLevel, Optional<Writer> log, int batchSize)
	{
		this.graphs = graphs;
		this.filter = filter;
		this.hypernymLevel = hypernymLevel;
		this.log = log;
		this.relationMap = Maps.newHashMap();
		
		// Initialize executor
		service = Executors.newCachedThreadPool();
		this.batchSize = batchSize;
		
		this.state = State.READY;
	}

	/**
	 * Constructor #2.
	 * Set the default hypernym level at 1.
	 * 
	 * @param graphs Input collection of {@link SynSemGraph}s
	 * @param log Execution logger
	 * @param batchSize Number of graphs to be processed in parallel
	 */
	public RelationBuilder(Iterable<SynSemGraph> graphs, Optional<Writer> log, int batchSize)
	{
		this(graphs, new PatternFilter(), 1, log, batchSize);
	}

	/**
	 * Getter for the input {@link SynSemGraph}s.
	 * 
	 * @return The provided {@link SynSemGraph}s
	 */
	public Iterable<SynSemGraph> inputGraphs()
	{
		return graphs;
	}
	
	/**
	 * Getter for the current {@link State} of the {@link RelationBuilder}.
	 */
	public State currentState()
	{
		return state;
	}
		
	/**
	 * Getter for the current {@link PatternFilter}.
	 */
	public PatternFilter currentPatternFilter()
	{
		return filter;
	}
			
	/**
	 * Execute the {@link Pattern} extraction process for all the provided {@link SynSemGraph}s.
	 */
	protected void extractPatterns()
	{
		// Start execution
		long startTime = System.currentTimeMillis();
		log.ifPresent(logger -> { try {
				logger.write("[ "+this.getClass().getSimpleName()+" ] ------ PATTERN EXTRACTION: START ------ ("+new Date().toString()+")\n");
			} catch(IOException e) {}
		});
		
		// Process all definitions in batches
		int threadCounter = 1;
		Set<SynSemGraph> currentBatch = Sets.newHashSet();
		List<Future<PatternExtractionThreadOutput>> futures = Lists.newArrayList();
		for(SynSemGraph graph : graphs)
		{
			// Fill up current batch
			if(currentBatch.size() < batchSize) currentBatch.add(graph);
			// Run current batch of graphs
			else {
				futures.add(service.submit(new PatternExtractionThread(threadCounter, currentBatch)));
				threadCounter++;
				currentBatch.clear();
			}
		}
		
		// Gather results
		for(Future<PatternExtractionThreadOutput> future : futures)
		{
			try {
				// Thread log
				PatternExtractionThreadOutput output = future.get();
				String threadLog = output.getThreadLog();
				log.ifPresent(logger -> { try { logger.write(threadLog+"\n"); } catch(IOException e) {} });
				// Store patterns
				relationMap.put(output.getThreadID(), output.getOutput());
			} catch (InterruptedException | ExecutionException e) {
				System.err.println("[ "+this.getClass().getSimpleName()+"::extractPatterns ] ERROR: interrupted while retrieving output from thread "+futures.indexOf(future)+":");
				e.printStackTrace();
			}
		}
		
		// Shutting down the service
		service.shutdown();
		// Wait until all threads are finish
		while (!service.isTerminated()) {}
		
		// Finish execution
		log.ifPresent(logger -> {
			try {
				logger.write("[ "+this.getClass().getSimpleName()+" ] ------ PATTERN EXTRACTION: END ------ ("+new Date().toString()+")\n"
						+ "Elapsed time: "+((System.currentTimeMillis()-startTime)/1000)+" s.\n");
				logger.close();
			} catch(IOException e) {}
		});
		
		this.state = State.PATTERN_EXTRACTION;
	}

	/**
	 * Get the set of {@link Relation}s corresponding to the input {@link SynSemGraph}s provided.
	 * 
	 * @param folderTextualPath (optional) folder where a textual file for each {@link Relation} is created
	 * @param folderSerializedPath (optional) folder where a serialized file for each {@link Relation} is created
	 * @return A set of {@link Relation}s
	 */
	public Set<Relation> getRelations(Optional<String> folderTextualPath, Optional<String> folderSerializedPath)
	{
		// Run the pattern extraction step if needed
		if(!state.equals(State.PATTERN_EXTRACTION)) extractPatterns();
		// Re-initialize executor
		service = Executors.newCachedThreadPool();
		
		// Start execution
		long startTime = System.currentTimeMillis();
		log.ifPresent(logger -> { try {
				logger.write("[ "+this.getClass().getSimpleName()+" ] ------ RELATION BUILDING: START ------ ("+new Date().toString()+")\n");
			} catch(IOException e) {}
		});
		
		// Process all pattern sets
		List<Future<Relation>> futures = Lists.newArrayList();
		relationMap.values().stream().flatMap(map -> map.keySet().stream()).distinct()
				// Retrieve all pattern sets with a given string key
				.map(patternKey -> relationMap.values().stream()
						.flatMap(map -> map.entrySet().stream()).filter(e -> e.getKey().equals(patternKey))
						.flatMap(e -> e.getValue().stream()).collect(Collectors.toSet())
					)
				// Use a thread for each relation
				.forEach(patternSet -> futures.add(service.submit(new RelationThread(patternSet))));
		
		// Gather results
		Set<Relation> relations = Sets.newHashSet();
		for(Future<Relation> future : futures)
		{
			try {
				Relation output = future.get();
				// Write relation in textual form
				folderTextualPath.ifPresent(path ->
					RelationBuilder.printRelationToFile(output, path+"/"+output.getPattern().toStringLemmatized().replaceAll(" ", "_")+".txt"));
				// Write relation serialized
				folderSerializedPath.ifPresent(path ->
					FileUtils.writeObjectToRawFile(output, path+"/"+output.getPattern().toStringLemmatized().replaceAll(" ", "_")+".ser", false));
				
				relations.add(output);
			} catch (InterruptedException | ExecutionException e) {
				System.err.println("[ "+this.getClass().getSimpleName()+"::getRelations ] ERROR: interrupted while retrieving output from thread "+futures.indexOf(future)+":");
				e.printStackTrace();
			}
		}
		
		// Shutting down the service
		service.shutdown();
		// Wait until all threads are finish
		while (!service.isTerminated()) {}
		
		// Finish execution
		log.ifPresent(logger -> {
			try {
				logger.write("[ "+this.getClass().getSimpleName()+" ] ------ RELATION BUILDING: END ------ ("+new Date().toString()+")\n"
						+ "Elapsed time: "+((System.currentTimeMillis()-startTime)/1000)+" s.\n");
				logger.close();
			} catch(IOException e) {}
		});
		
		this.state = State.RELATION_BUILDING;
		
		return null;		
	}
	
	/**
	 * Print all the content of a {@link Relation} in human-readable form.
	 * 
	 * @param relation {@link Relation} to print
	 * @param outputFile Target file
	 */
	protected static void printRelationToFile(Relation relation, String outputFile)
	{
		try {
			// Open output stream
			PrintWriter out = new PrintWriter(new FileWriter(outputFile));	
			// Write relation
			out.println(relation.getPattern().toString());
			out.println(relation.getPattern().getNodes().stream().map(Node::toStringVerbose).collect(Collectors.joining("\t")));
			out.println(relation.getPattern().getEdgePath().stream().map(Edge::toString).collect(Collectors.joining("\t")));
			// Write arguments
			relation.getExtractions().stream()
					.map(ap -> ap.toString()+"\t"+ap.getDisambiguationConfidence()+"\t"+ap.extractionSource().map(Definition::toStringVerbose).orElse(""))
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
		
	class PatternExtractionThreadOutput
	{
		final String threadLog;
		final int threadID;
		final Map<String,Set<Pattern>> output;
		
		public PatternExtractionThreadOutput(String threadLog, int threadID, Map<String,Set<Pattern>> output)
		{
			this.threadLog = threadLog;
			this.threadID = threadID;
			this.output = output;
		}
		
		public String getThreadLog()
		{
			return threadLog;
		}
		
		public int getThreadID()
		{
			return threadID;
		}
		
		public Map<String,Set<Pattern>> getOutput()
		{
			return output;
		}
	}
	
	
	/**
	 * Thread that extracts {@link Pattern} from a batch of {@link SynSemGraph}s.
	 * It is inizialized by a {@link RelationBuilder} with an incremental thread ID and a set of {@link SynSemGraph}s.
	 * It creates a {@link PatternIdentifier} for each {@link SynSemGraph} and returns a set of {@link Pattern}s indexed by pattern string.
	 * 
	 * @author claudio
	 */
	class PatternExtractionThread implements Callable<PatternExtractionThreadOutput>
	{
		// Thread identifier
		private final int threadID;
		// Input graphs to process
		private final Collection<SynSemGraph> input;
		
		// Logger
		private StringWriter logger;
		// Start time
		private long startTime;
		
		/**
		 * Constructor.
		 * 
		 * @param id Thread identifier
		 * @param input Input {@link SynSemGraph}s
		 */
		public PatternExtractionThread(int id, Collection<SynSemGraph> input)
		{
			this.threadID = id;
			this.input = input;
			this.logger = new StringWriter();
			this.startTime = System.currentTimeMillis();
			
			logger.write("[ "+this.getClass().getSimpleName()+" "+id+" ] Execution started on "+input.size()+" definitions: ("+new Date().toString()+")\n");
		}
		
		@Override
		public PatternExtractionThreadOutput call() throws Exception
		{
			// Process each graph with a pattern identifier
			Map<String,Set<Pattern>> outputPatterns = input.stream().flatMap(graph ->
			{
				logger.write("[ "+this.getClass().getSimpleName()+" "+threadID+" ] Processing '"+graph.sourceDefinition().getID()+"'...\n");
				logger.write("[ "+this.getClass().getSimpleName()+" "+threadID+" ] Text: "+graph.sourceDefinition().getText()+"\n");				
				// Pattern extraction
				return new PatternIdentifier(graph, filter, Optional.of(logger)).identifyPatterns().stream();
			})
			.collect(Collectors.groupingBy(Pattern::toString, Collectors.toSet()));
			// Finish and return
			logger.write("[ "+this.getClass().getSimpleName()+" "+threadID+" ] Execution finished in "+
					((System.currentTimeMillis()-startTime)/1000)+" seconds. ("+new Date().toString()+")\n");		
			return new PatternExtractionThreadOutput(logger.toString(), threadID, outputPatterns);
		}
		
	}
	
	/**
	 * Thread that turns a set of {@link Pattern}s into a {@link Relation}.
	 * 
	 * @author claudio
	 */
	class RelationThread implements Callable<Relation>
	{
		// Input patterns
		private final Set<Pattern> input;		

		/**
		 * Constructor.
		 * 
		 * @param input Input set of {@link Pattern}s
		 */
		public RelationThread(Set<Pattern> input)
		{
			this.input = input;
		}
		
		@Override
		public Relation call() throws Exception
		{
			return Relation.createRelationFromPatterns(input, hypernymLevel);
		}
		
	}
}
