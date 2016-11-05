package graph;

import graph.SemanticNode.UnableToMergeNodeException;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import utils.DisambiguateUtils.Disambiguation;

import com.google.common.collect.Maps;

import definition.Definition;


/**
 * Main class modeling the syntactic-semantic graph.
 * Extends {@link Graph} by dealing explicitly with {@link SemanticNode}s.
 * 
 * @author claudio
 */
public class SynSemGraph extends Graph implements Iterable<SemanticNode>
{
	private static final long serialVersionUID = 4623834408320038637L;
	
	// List of semantic nodes
	private Map<Disambiguation,SemanticNode> senses;
	
	/**
	 * Graph constructor.
	 * Initializes an empty graph and the source definition data.
	 * 
	 * @param source Source definition
	 */
	public SynSemGraph(Definition s)
	{
		// Initialize superclass and private members
		super(s);
		senses = Maps.newHashMap();
	}
		
	/**
	 * Getter for the collection of {@link SemanticNode}s.
	 */
	public final Collection<SemanticNode> getSemanticNodes()
	{
		return senses.values();
	}

	/**
	 * Add a new {@link Node} to the {@link Graph}.
	 * 
	 * @param n {@link Node} to be added
	 */
	public void addNode(Node n)
	{
		if(!this.graph.containsVertex(n))
		{
			if(n instanceof SemanticNode)
				senses.put( ((SemanticNode)n).getSenseAttachment() , (SemanticNode)n);
			this.graph.addVertex(n);
		}
	}
	
	/**
	 * Build a syntactic-semantic graph from a dependency graph.
	 * 
	 * @param g The original dependency graph as {@link Graph} object
	 * @param senseMap Sense mapping for the nodes of g
	 */
	public static SynSemGraph buildFrom(Graph g, Map<Node,Disambiguation> senseMap)
	{
		// Return immediately if g is already a SynSemGraph...
		if(g instanceof SynSemGraph)
			return (SynSemGraph)g;
		else
		{
			// Create empty graph
			SynSemGraph ssg = new SynSemGraph(g.source);
			// Add content nodes
			for(Node n : senseMap.keySet())
			{
				ssg.graph.removeVertex(n);
				if(g.graph.containsVertex(n))
				{
					// New semantic node
					if(!ssg.senses.containsKey(senseMap.get(n)))
					{
						SemanticNode new_n = new SemanticNode(n.getToken(), senseMap.get(n));
						ssg.addNode(new_n);
						ssg.senses.put(senseMap.get(n), new_n);
					}
					// Semantic node already inserted, merge
					else
					{
						// Retrieve connected node in the original dependency graph
						Node bridge = ssg.senses.get(senseMap.get(n)).getNodeSubgraph().vertexSet().stream()
							.filter(m -> (g.graph.getEdge(n, m) != null) || (g.graph.getEdge(m, n) != null))
							.findFirst().orElse(ssg.senses.get(senseMap.get(n)));
						Edge connection = Optional.ofNullable(g.graph.getEdge(n, bridge)).orElse(g.graph.getEdge(bridge, n));
						// If no edge is found use a "dummy" empty edge
						Edge e = Optional.ofNullable(connection).orElse(new Edge("", n, n));
						
						// Add vertex
						try {
							ssg.senses.get(senseMap.get(n)).mergeWith(n, e);
						} catch (UnableToMergeNodeException e1) {
							System.err.println(e1);
							e1.printStackTrace();
						}
					}
				}
				else
				{
					System.err.println("[ "+SynSemGraph.class.getSimpleName()+" ] WARNING! Content node "+n+" in sense mapping not present in the original graph... ");
					continue;
				}
			}
			// Add remaining edges and nodes
			for(Edge e : g.graph.edgeSet())
			{
				// Fix source
				Node source = g.graph.getEdgeSource(e);
				if (senseMap.containsKey(g.graph.getEdgeSource(e)))
					source = ssg.senses.get(senseMap.get(g.graph.getEdgeSource(e)));
				// Fix target
				Node target = g.graph.getEdgeTarget(e);
				if (senseMap.containsKey(g.graph.getEdgeTarget(e)))
					target = ssg.senses.get(senseMap.get(g.graph.getEdgeTarget(e)));				
				
				// Case 1: edge connecting individual nodes within a compound semantic node
				if(source.equals(target))
				{
					SemanticNode actualNode = (SemanticNode) source;
					actualNode.getNodeSubgraph().addEdge(e.getSource(), e.getTarget(), e);
				}
				// Case 2: edge connecting a semantic node with the rest of the graph (or two word nodes)
				else {
					// Recreate edge
					Edge newEdge = new Edge(e.type(), source, target);
					// Add nodes and edge
					ssg.addNode(source);
					ssg.addNode(target);
					ssg.addEdge(newEdge, source, target);
				}
			}
			
			return ssg;
		}
	}

	/**
	 * Print all information from the graph.
	 */
	public String toStringVerbose()
	{
		StringBuilder graph_string = new StringBuilder();
		graph_string.append("Graph = ").append("[\t");
		for(Edge e : graph.edgeSet())
			graph_string.append("("+graph.getEdgeSource(e).toStringVerbose()).append(e).append(graph.getEdgeTarget(e).toStringVerbose()+")").append("\t");
		graph_string.append("]\n");
		graph_string.append("Source = [").append(source.getID()).append("]\t");
		graph_string.append(source.getText());
		return graph_string.toString();
	}

	@Override
	public Iterator<SemanticNode> iterator()
	{
		return getSemanticNodes().iterator();
	}
}
