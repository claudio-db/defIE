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
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import definition.DefinitionReader;
import definition.ProcessedDefinition;
import edu.stanford.nlp.util.Triple;
import graph.Graph;
import graph.Node;
import graph.SynSemGraph;
import utils.DisambiguateUtils.Disambiguation;
import utils.ParseUtils;
import utils.ParseUtils.Dependency;
import utils.ParseUtils.Token;

/**
 * Class modeling the construction of syntactic-semantic graphs from a corpus of textual definitions.
 * Requires a list of {@link ProcessedDefinition} objects and provides a {@link SynSemGraph} for each.
 * 
 * @author claudio
 */
public class GraphBuilder
{	
	// Definition reader
	private DefinitionReader reader;
	
	// Syntactic-semantic graphs
	private List<SynSemGraph> graphs;
	// Execution logger
	private Optional<Writer> log;
	
	// Filter on glosses
	private Predicate<ProcessedDefinition> definitionFilter;
	
	// Multithreading service
	private ExecutorService service;
	private int batchSize;
	
	// Status flag
	private boolean done;
	
	// Syntactic-semantic graph construction 
	public static final Function<ProcessedDefinition,SynSemGraph> BUILD_GRAPH = d ->
	{
		Graph depGraph = new Graph(d);
		return SynSemGraph.buildFrom(depGraph, GraphBuilder.matchNodesWithSenses(depGraph.getNodes(), d.getSenses()));
	};
	
	/**
	 * Constructor.
	 * 
	 * @param reader {@link DefinitionReader} that provides {@link ProcessedDefinition}s
	 * @param log Execution logger
	 * @param batchSize Number of definitions to be processed in parallel
	 */
	public GraphBuilder(DefinitionReader reader, Optional<Writer> log, int batchSize)
	{
		// Load glosses
		this.reader = reader;	
		this.log = log;		
		this.graphs = Lists.newArrayList();
		this.definitionFilter = g -> true;
		
		// Initialize executor
		service = Executors.newCachedThreadPool();
		this.batchSize = batchSize;
		done = false;
	}

	/**
	 * Create a mapping from {@link Node}s in a dependency {@link Graph} and {@link Disambiguation}s of the corresponding sentence.
	 * Given two overlapping {@link Disambiguation}s the longest is selected.
	 * 
	 * @param nodes Collection of {@link Node}s from the dependency {@link Graph} of the sentence
	 * @param matches Collection of {@link Disambiguation}s for the sentence
	 * @return A map from {@link Node} to corresponding {@link Disambiguation}
	 */
	protected static Map<Node,Disambiguation> matchNodesWithSenses(Collection<Node> nodes, Collection<Disambiguation> matches)
	{
		Map<Node,Disambiguation> matchedNodes = Maps.newHashMap();
		for (Node node : nodes)
		{
			// Check each match for inclusion
			for (Disambiguation match : matches)
			{
				// Match found: check overlapping mentions and keep the longest
				if((node.getToken().index() >= match.getStartingTokenOffset()) && (node.getToken().index() < match.getEndingTokenOffset()))
				{
					if(!matchedNodes.containsKey(node))
						matchedNodes.put(node, match);
					else if(match.getMatchLength() > matchedNodes.get(node).getMatchLength())
						matchedNodes.put(node, match);
				}
			}
		}
		return matchedNodes;
	}
	
	/**
	 * Execute the graph building process for all the {@link ProcessedDefinition}s provided by the {@link DefinitionReader}.
	 */
	protected void run()
	{
		// Start execution
		long startTime = System.currentTimeMillis();
		log.ifPresent(logger -> { try {
				logger.write("[ "+this.getClass().getSimpleName()+" ] ------ START ------ ("+new Date().toString()+")\n");
			} catch(IOException e) {}
		});
	
		// Process all definitions in batches
		int threadCounter = 1;
		Set<ProcessedDefinition> currentBatch = Sets.newHashSet();
		List<Future<GraphBuilderThreadOutput>> futures = Lists.newArrayList();
		for(ProcessedDefinition definition : reader)
		{
			// Fill up current batch
			if(currentBatch.size() < batchSize) currentBatch.add(definition);
			// Run current batch of definitions
			else {
				futures.add(service.submit(new GraphBuilderThread(threadCounter, currentBatch)));
				threadCounter++;
				currentBatch.clear();
			}
		}
		
		// Gather results
		for(Future<GraphBuilderThreadOutput> future : futures)
		{
			try {
				// Thread log
				GraphBuilderThreadOutput output = future.get();
				String threadLog = output.getThreadLog();
				log.ifPresent(logger -> { try { logger.write(threadLog+"\n"); } catch(IOException e) {} });
				// Store graphs
				graphs.addAll(output.getOutput());
			} catch (InterruptedException | ExecutionException e) {
				System.err.println("[ "+this.getClass().getSimpleName()+" ] ERROR: interrupted while retrieving output from thread "+futures.indexOf(future)+":");
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
				logger.write("[ "+this.getClass().getSimpleName()+" ] ------ END ------ ("+new Date().toString()+")\n"
						+ "Elapsed time: "+((System.currentTimeMillis()-startTime)/1000)+" s.\n");
				logger.close();
			} catch(IOException e) {}
		});
		done = true;
	}
	
	/**
	 * Retrieve the resulting {@link SynSemGraph}s.
	 * 
	 */
	public Collection<SynSemGraph> getSynSemGraphs()
	{
		if(!done) run();
		return graphs;
	}

	/**
	 * Print all {@link SynSemGraph} in human-readable form.
	 * 
	 * @param outputFile Path to output file
	 */
	public void printGraphsToFile(String outputFile)
	{
		try {
			// Open output stream
			PrintWriter out = new PrintWriter(new FileWriter(outputFile));	
			// Write graphs one by one
			getSynSemGraphs().stream().map(SynSemGraph::toStringVerbose).forEach(out::println);
			// Clean up
			out.close();
		}
		catch(IOException e)
		{
			System.err.println("[ "+this.getClass().getSimpleName()+" ] ERROR: unable to write output to '"+outputFile+"'!");
			System.exit(1);
		}
	}

	class GraphBuilderThreadOutput
	{
		final String theadLog;
		final Collection<SynSemGraph> output;
		
		public GraphBuilderThreadOutput(String threadLog, Collection<SynSemGraph> output)
		{
			this.theadLog = threadLog;
			this.output = output;
		}
		
		public String getThreadLog()
		{
			return theadLog;
		}
		
		public Collection<SynSemGraph> getOutput()
		{
			return output;
		}
	}
	
	/**
	 * Thread that turns a batch of {@link ProcessedDefinition}s into {@link SynSemGraph}s.
	 * It is initialized by {@link GraphBuilder} with an incremental ID and the input {@link ProcessedDefinition}s 
	 * and returns the corresponding {@link SynSemGraph}s together with an execution log.
	 * 
	 * @author claudio
	 */
	class GraphBuilderThread implements Callable<GraphBuilderThreadOutput>
	{
		// Thread identifier
		private final int threadID;
		// Input definitions to process
		private final Collection<ProcessedDefinition> input;
		
		// Logger
		private StringWriter logger;
		// Start time
		private long startTime;
		
		/**
		 * Constructor.
		 * 
		 * @param id Thread identifier
		 * @param input Input {@link ProcessedDefinition}s
		 */
		public GraphBuilderThread(int id, Collection<ProcessedDefinition> input)
		{
			this.threadID = id;
			this.input = input;
			this.logger = new StringWriter();
			this.startTime = System.currentTimeMillis();
			
			logger.write("[ "+this.getClass().getSimpleName()+" "+id+" ] Execution started on "+input.size()+" definitions: ("+new Date().toString()+")\n");
		}

		@Override
		public GraphBuilderThreadOutput call() throws Exception
		{
			List<SynSemGraph> outputGraphs = input.stream().filter(definitionFilter)
					.map(definition ->
					{
						logger.write("[ "+this.getClass().getSimpleName()+" "+threadID+" ] Processing '"+definition.getID()+"'...\n");
						logger.write("[ "+this.getClass().getSimpleName()+" "+threadID+" ] Text: "+definition.getText()+"\n");
						
						// Generate dependency graph
						Graph dependencyGraph = new Graph(definition);
						logger.write("[ "+this.getClass().getSimpleName()+" "+threadID+" ] Dependency graph:\n");
						dependencyGraph.edgeIterator().forEachRemaining(e ->
							logger.write("[ "+this.getClass().getSimpleName()+" "+threadID+" ]\t"+e.getSource().toStringVerbose()+" "+e.toString()+" "+e.getTarget().toStringVerbose()+"\n"));
						
						// Generate syntactic-semantic graph
						SynSemGraph sGraph = SynSemGraph.buildFrom(dependencyGraph,
								GraphBuilder.matchNodesWithSenses(dependencyGraph.getNodes(), definition.getSenses()));
						logger.write("[ "+this.getClass().getSimpleName()+" "+threadID+" ] Syntactic-semantic graph:\n");
						sGraph.edgeIterator().forEachRemaining(e ->
						logger.write("[ "+this.getClass().getSimpleName()+" "+threadID+" ]\t"+e.getSource().toStringVerbose()+" "+e.toString()+" "+e.getTarget().toStringVerbose()+"\n"));
					
						// Internal structure of semantic nodes
						logger.write("[ "+this.getClass().getSimpleName()+" "+threadID+" ] Semantic nodes:\n");
						sGraph.forEach(semanticNode -> {
							logger.write("[ "+this.getClass().getSimpleName()+" "+threadID+" ]\t"+semanticNode.toStringVerbose()+"\n");
							semanticNode.getNodeSubgraph().edgeSet().forEach(e ->
								logger.write("[ "+this.getClass().getSimpleName()+" "+threadID+" ]\t\t"+e.getSource().toStringVerbose()+" "+e.toString()+" "+e.getTarget().toStringVerbose()));
						});						
						return sGraph;
					})
					.collect(Collectors.toList());
			
			logger.write("[ "+this.getClass().getSimpleName()+" "+threadID+" ] Execution finished in "+
					((System.currentTimeMillis()-startTime)/1000)+" seconds. ("+new Date().toString()+")\n");
			return new GraphBuilderThreadOutput(logger.toString(), outputGraphs);
		}
	}

	/**
	 * Graph construction unit test with the example sentence of the paper.
	 * 
	 * @return The corresponding {@link SynSemGraph}
	 */
	public static SynSemGraph unitTest()
	{
		// Definition
		String text = "Atom Heart Mother is the fifth album by English band Pink Floyd .";
		// Dependencies
		List<Dependency> deps = Lists.newArrayList();
		deps.add(new Dependency("nn", new Token("Mother", "mother", "NNP", 2), new Token("Atom", "atom", "NNP", 0)));
		deps.add(new Dependency("nn", new Token("Mother", "mother", "NNP", 2), new Token("Heart", "heart", "NNP", 1)));
		deps.add(new Dependency("nsubj", new Token("album", "album", "NN", 6), new Token("Mother", "mother", "NNP", 2)));
		deps.add(new Dependency("cop", new Token("album", "album", "NN", 6), new Token("is", "be", "VBZ", 3)));
		deps.add(new Dependency("det", new Token("album", "album", "NN", 6), new Token("the", "the", "DT", 4)));
		deps.add(new Dependency("amod", new Token("album", "album", "NN", 6), new Token("fifth", "fifth", "JJ", 5)));
		deps.add(new Dependency("prep", new Token("album", "album", "NN", 6), new Token("by", "by", "IN", 7)));
		deps.add(new Dependency("pobj", new Token("by", "by", "IN", 7), new Token("Floyd", "Floyd", "NNP", 11)));
		deps.add(new Dependency("nn", new Token("Floyd", "Floyd", "NNP", 11), new Token("English", "English", "JJ", 8)));
		deps.add(new Dependency("nn", new Token("Floyd", "Floyd", "NNP", 11), new Token("band", "band", "NN", 9)));
		deps.add(new Dependency("nn", new Token("Floyd", "Floyd", "NNP", 11), new Token("Pink", "pink", "JJ", 10)));
		deps.add(new Dependency("punct", new Token("album", "album", "NN", 6), new Token(".", ".", ".", 12)));
		// Disambiguations
		List<Disambiguation> senses = Lists.newArrayList();
		senses.add(new Disambiguation(new Triple<>(0, 3, "bn:02070902n"), 1.0));
		senses.add(new Disambiguation(new Triple<>(6, 7, "bn:00002488n"), 1.0));
		senses.add(new Disambiguation(new Triple<>(8, 9, "bn:00102248a"), 1.0));
		senses.add(new Disambiguation(new Triple<>(9, 10, "bn:00008280n"), 1.0));
		senses.add(new Disambiguation(new Triple<>(10, 12, "bn:03292767n"), 1.0));

		// Initialize definition
		ProcessedDefinition definition = new ProcessedDefinition("test", text, ParseUtils.fixDependencies(deps), senses);
		System.out.println("Definition: "+definition);
		System.out.println("\nDependency graph:");
		new Graph(definition).edgeIterator().forEachRemaining(e -> System.out.println(e.getSource().toStringVerbose()+" "+e.toString()+" "+e.getTarget().toStringVerbose()));
		
		// Node-to-sense mappings
		System.out.println("\nSense mappings:");
		GraphBuilder.matchNodesWithSenses(new Graph(definition).getNodes(), senses)
					.entrySet().stream().map(e -> e.getKey()+" --> "+e.getValue()).forEach(System.out::println);
		
		// Syntactic-semantic graph
		System.out.println("\nSyntactic-semantic graph:");
		SynSemGraph sGraph = GraphBuilder.BUILD_GRAPH.apply(definition);
		sGraph.edgeIterator().forEachRemaining(e -> System.out.println(e.getSource().toStringVerbose()+" "+e.toString()+" "+e.getTarget().toStringVerbose()));
		
		// Internal structure of semantic nodes
		System.out.println("\nSemantic nodes:");
		sGraph.forEach(semanticNode -> {
			System.out.println(semanticNode.toStringVerbose());
			semanticNode.getNodeSubgraph().edgeSet().forEach(e -> System.out.println("\t"+e.getSource().toStringVerbose()+" "+e.toString()+" "+e.getTarget().toStringVerbose()));
		});
		
		return sGraph;
	}
	
	public static SynSemGraph unitTest2()
	{
		// Definition
		String text = "Jon Nelson is a sound collage artist and a radio show host for Some Assembly Required .";
		// Dependencies
		List<Dependency> deps = Lists.newArrayList();
		deps.add(new Dependency("nn", new Token("Nelson", "Nelson", "NNP", 1), new Token("Jon", "Jon", "NNP", 0)));
		deps.add(new Dependency("nsubj", new Token("artist", "artist", "NN", 6), new Token("Nelson", "Nelson", "NNP", 1)));
		deps.add(new Dependency("cop", new Token("artist", "artist", "NN", 6), new Token("is", "be", "VBZ", 2)));
		deps.add(new Dependency("det", new Token("artist", "artist", "NN", 6), new Token("a", "a", "DT", 3)));
		deps.add(new Dependency("nn", new Token("artist", "artist", "NN", 6), new Token("sound", "sound", "NN", 4)));
		deps.add(new Dependency("nn", new Token("artist", "artist", "NN", 6), new Token("collage", "collage", "NN", 5)));
		deps.add(new Dependency("cc", new Token("artist", "artist", "NN", 6), new Token("and", "and", "CC", 7)));
		deps.add(new Dependency("conj", new Token("artist", "artist", "NN", 6), new Token("host", "host", "NN", 11)));
		deps.add(new Dependency("det", new Token("host", "host", "NN", 11), new Token("a", "a", "DT", 8)));
		deps.add(new Dependency("nn", new Token("host", "host", "NN", 11), new Token("radio", "radio", "NN", 9)));
		deps.add(new Dependency("nn", new Token("host", "host", "NN", 11), new Token("show", "show", "NN", 10)));
		deps.add(new Dependency("prep", new Token("host", "host", "NN", 11), new Token("for", "for", "IN", 12)));
		deps.add(new Dependency("pobj", new Token("for", "for", "IN", 12), new Token("Required", "Required", "NNP", 15)));
		deps.add(new Dependency("nn", new Token("Required", "Required", "NNP", 15), new Token("Some", "Some", "NNP", 13)));
		deps.add(new Dependency("nn", new Token("Required", "Required", "NNP", 15), new Token("Assembly", "Assembly", "NNP", 14)));
		deps.add(new Dependency("punct", new Token("artist", "artist", "NN", 6), new Token(".", ".", ".", 16)));
		// Disambiguations
		List<Disambiguation> senses = Lists.newArrayList();
		senses.add(new Disambiguation(new Triple<>(0, 2, "bn:02312115n"), 1.0));
		senses.add(new Disambiguation(new Triple<>(4, 6, "bn:00020593n"), 1.0));
		senses.add(new Disambiguation(new Triple<>(6, 7, "bn:00006182n"), 1.0));
		senses.add(new Disambiguation(new Triple<>(9, 11, "bn:14764640n"), 1.0));
		senses.add(new Disambiguation(new Triple<>(11, 12, "bn:00064217n"), 1.0));
		senses.add(new Disambiguation(new Triple<>(13, 16, "bn:03321093n"), 1.0));

		// Initialize definition
		ProcessedDefinition definition = new ProcessedDefinition("test", text, ParseUtils.fixDependencies(deps), senses);
		System.out.println("Definition: "+definition);
		System.out.println("\nDependency graph:");
		new Graph(definition).edgeIterator().forEachRemaining(e -> System.out.println(e.getSource().toStringVerbose()+" "+e.toString()+" "+e.getTarget().toStringVerbose()));
		
		// Node-to-sense mappings
		System.out.println("\nSense mappings:");
		GraphBuilder.matchNodesWithSenses(new Graph(definition).getNodes(), senses)
					.entrySet().stream().map(e -> e.getKey()+" --> "+e.getValue()).forEach(System.out::println);
		
		// Syntactic-semantic graph
		System.out.println("\nSyntactic-semantic graph:");
		SynSemGraph sGraph = GraphBuilder.BUILD_GRAPH.apply(definition);
		sGraph.edgeIterator().forEachRemaining(e -> System.out.println(e.getSource().toStringVerbose()+" "+e.toString()+" "+e.getTarget().toStringVerbose()));
		
		// Internal structure of semantic nodes
		System.out.println("\nSemantic nodes:");
		sGraph.forEach(semanticNode -> {
			System.out.println(semanticNode.toStringVerbose());
			semanticNode.getNodeSubgraph().edgeSet().forEach(e -> System.out.println("\t"+e.getSource().toStringVerbose()+" "+e.toString()+" "+e.getTarget().toStringVerbose()));
		});
		
		return sGraph;
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