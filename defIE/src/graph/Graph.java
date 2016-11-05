package graph;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.SimpleGraph;

import definition.Definition;
import definition.ProcessedDefinition;
import utils.ParseUtils;

/**
 * Class modeling both the original dependency graph and its syntactic-semantic extension.
 * It uses the generic classes {@link Node}s and {@link Edge}s for the graph components.
 * 
 * @author claudio
 */
public class Graph implements Serializable {

	private static final long serialVersionUID = -5057098280020503728L;
	
	// Graph structure
	protected SimpleGraph<Node,Edge> graph;
	// Source definition
	protected Definition source;	
	
	/**
	 * Graph constructor.
	 * Initializes an empty graph and the source definition.
	 * 
	 * @param s Source definition
	 */
	public Graph(Definition s)
	{
		// Initialize components
		graph = s.isProcessed()?
				ParseUtils.buildDependencyGraph(((ProcessedDefinition)s).getDependencies()) :
				new SimpleGraph<Node,Edge>(Edge.class);
		source = s;
	}
	
	/**
	 * Getter for the source definition.
	 */
	public final Definition sourceDefinition()
	{
		return source;
	}
	
	/**
	 * Getter for the whole set of nodes.
	 */
	public final Set<Node> getNodes()
	{
		return graph.vertexSet();
	}
	
	/**
	 * Add a new node to the graph.
	 * 
	 * @param n {@link Node} to be added
	 */
	public void addNode(Node n)
	{
		if(!graph.containsVertex(n)) graph.addVertex(n);
	}
	
	/**
	 * Add a new edge to the graph.
	 * 
	 * @param e {@link Edge} to be added
	 * @param i Source {@link Node}
	 * @param j Target {@link Node}
	 */
	public void addEdge(Edge e, Node i, Node j)
	{
		addNode(i);
		addNode(j);
		graph.addEdge(i, j, e);
		e.source = i;
		e.target = j;
	}

	/**
	 * Compute the shortest path between two nodes using the {@link DijkstraShortestPath} algorithm.
	 * 
	 * @param i Starting {@link Node}
	 * @param j Target {@link Node}
	 * @return Shortest path as {@link GraphPath<Node, Edge>} object
	 */
	public UndirectedPath shortestPath(Node i, Node j)
	{
		// The path must be between two existing nodes in the graph...
		if(graph.containsVertex(i) && graph.containsVertex(j))
			return new UndirectedPath(i, j, DijkstraShortestPath.findPathBetween(graph,i, j));
		else
		{
			System.err.println("[ "+Graph.class.getSimpleName()+" ] ERROR! Unable to compute shortest path: node "+i+
					" or node "+j+" does not exist in the graph...");
			return null;
		}
	}
		
	@Override
	public String toString()
	{
		StringBuilder graph_string = new StringBuilder();
		graph_string.append("[\t");
		for(Edge e : graph.edgeSet())
			graph_string.append("("+graph.getEdgeSource(e).toStringVerbose()).append(e).append(graph.getEdgeTarget(e).toStringVerbose()+")").append("\t");
		graph_string.append("]");
		return graph_string.toString();
	}
	
	public Iterator<Edge> edgeIterator()
	{
		return graph.edgeSet().iterator();
	}
	
	public Iterator<Node> nodeIterator()
	{
		return graph.vertexSet().iterator();
	}
	
	/**
	 * Print the whole graph and the source gloss.
	 */
	public String toStringVerbose()
	{
		return toString()+"\nSource: "+source;
	}

	/**
	 * Auxiliary class that models a path inside the {@link Graph}.
	 * Contains information about the starting and ending {@link Node}s as well as the list of {@link Edge}s.
	 */
	public class UndirectedPath
	{
		// Extremes of the path
		private final Node startingNode, endingNode;
		// List of edges
		private final List<Edge> edges;
		
		/**
		 * Constructor.
		 * 
		 * @param startingNode Starting {@link Node} of the path
		 * @param endingNode Ending {@link Node} of the path
		 * @param edges List of {@link Edge}s composing the path
		 */
		public UndirectedPath(Node startingNode, Node endingNode, List<Edge> edges)
		{
			this.startingNode = startingNode;
			this.endingNode = endingNode;
			this.edges = edges;
		}
		
		public Node getStartingEdge()
		{
			return startingNode;
		}
		
		public Node getEndingNode()
		{
			return endingNode;
		}
		
		public List<Edge> getEdges()
		{
			return edges;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((edges == null) ? 0 : edges.hashCode());
			result = prime * result + ((endingNode == null) ? 0 : endingNode.hashCode());
			result = prime * result + ((startingNode == null) ? 0 : startingNode.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			
			UndirectedPath other = (UndirectedPath) obj;
			if (!getOuterType().equals(other.getOuterType())) return false;
			
			return this.startingNode.equals(other.startingNode) &&
					this.endingNode.equals(other.endingNode) && this.edges.equals(other.edges);
		}

		private Graph getOuterType()
		{
			return Graph.this;
		}	
	}
}
