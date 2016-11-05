package graph;

import java.util.stream.Collectors;

import org.jgrapht.graph.SimpleGraph;

import edu.stanford.nlp.util.Pair;
import utils.DisambiguateUtils.Disambiguation;
import utils.ParseUtils;
import utils.ParseUtils.Token;

/**
 * Class modeling an augmented semantic node in the graph.
 * Extends the generic {@link Node} with a sense attachment (represented by a sense ID)
 * and keeps track of all the generic {@link Node}s merged as components.
 * 
 * @author claudio
 */
public class SemanticNode extends Node {
	
	private static final long serialVersionUID = 3630526212921225840L;
	
	// Sense attachment
	private Disambiguation sense;
	// Head noun
	private SimpleGraph<Node,Edge> graph;
	
	/**
	 * Constructor #1.
	 * Initializes the head token and attached sense.
	 * 
	 * @param head Head token
	 * @param sense Sense attachment (starting index, ending index, sense ID)
	 */
	public SemanticNode(Token head, Disambiguation sense)
	{
		// Initialize head node
		super(head);
		// Initialize sense attachment
		this.sense = sense;
		
		// Initialize graph
		graph = new SimpleGraph<Node,Edge>(Edge.class);
		graph.addVertex(new Node(head));
	}
	
	/**
	 * Constructor #2.
	 * Initializes the whole internal graph and attached sense.
	 * 
	 * @param sense Sense attachment (starting index, ending index, sense ID)
	 */
	public SemanticNode(SimpleGraph<Node,Edge> graph, Disambiguation sense)
	{
		// Initialize head node
		super(ParseUtils.getHead(graph));
		// Initialize sense attachment
		this.sense = sense;
		
		// Initialize graph
		this.graph = graph;
	}
	
	/**
	 * Getter for the sense.
	 */
	public final String getSense()
	{
		return sense.getSenseID();
	}
	
	/**
	 * Getter for the sense attachment.
	 */
	public final Disambiguation getSenseAttachment()
	{
		return sense;
	}

	/**
	 * Getter for the plain head node.
	 */
	public final Token getHead()
	{
		return token;
	}
	
	public final SimpleGraph<Node,Edge> getNodeSubgraph()
	{
		return graph;
	}
	
	/**
	 * Return the text fragment corresponding to this {@link SemanticNode}.
	 * 
	 * @param lemmatized 'true' if the fragment should be lemmatized
	 * @return
	 */
	public final String getTextFragment(boolean lemmatized)
	{
		return ParseUtils.sentenceFromGraph(graph).stream().map(t -> lemmatized? t.lemma() : t.surfaceForm()).collect(Collectors.joining(" "));
	}
	
	@Override
	public int hashCode()
	{
		return super.hashCode()+sense.hashCode()+graph.hashCode();
	}
		
	@Override
	public boolean equals(Object o)
	{
		if (o == this) return true;
		else if (!(o instanceof SemanticNode)) return false;
		
		SemanticNode other = (SemanticNode) o;
		return sense.equals(other.sense) && graph.equals(other.graph);
	}
		
	@Override
	public String toString()
	{
		return getSense();
	}

	public String toStringVerbose()
	{
		return getTextFragment(false)+ParseUtils.SEPARATOR+sense.toString()+ParseUtils.SEPARATOR+getHead();
	}

	private void addNode(Node n)
	{
		// Add to graph an entire semantic node (and its subgraph)
		if((n instanceof SemanticNode))
		{
			((SemanticNode)n).graph.vertexSet().forEach(this.graph::addVertex);
			((SemanticNode)n).graph.edgeSet().forEach(e -> this.graph.addEdge(e.getSource(), e.getTarget(), e));
		}
		// Add to graph a regular word node
		else this.graph.addVertex(n);
	}
	
	/**
	 * Merge with another {@link Node}.
	 * 
	 * @param node {@link Node} to be merged
	 * @param edge {@link Edge} connecting semNode and node
	 * @throws UnableToMergeNodeException 
	 */
	public void mergeWith(Node node, Edge edge) throws UnableToMergeNodeException
	{
		if(!(node instanceof SemanticNode) ||
				(((SemanticNode) node).getSenseAttachment().equals(this.getSenseAttachment())))
		{
			try {
				// Case 1: Node to be merged is the new head
				if(edge.getSource().equals(node) && this.graph.vertexSet().stream().anyMatch(token -> token.equals(edge.getTarget())))
				{
					// Add to graph
					this.addNode(node);
					// Add connection to current node
					this.graph.addEdge(node, edge.getTarget(), edge);
					// Update head token
					this.token = node.getToken();
				}
				// Case 2: Node to be merged is a dependent 
				else if(edge.getTarget().equals(node) && this.graph.vertexSet().stream().anyMatch(token -> token.equals(edge.getSource())))
				{
					// Add to graph
					this.addNode(node);
					// Add connection to current node
					this.graph.addEdge(edge.getSource(), node, edge);
				}
				// Case 3: Node to merged is not connected 
				else {
					// Add to graph
					this.addNode(node);				
				}
				
			} catch(NullPointerException e) {
				throw new UnableToMergeNodeException("Nodes are not connected", this, node);
			}
		}
		else throw new UnableToMergeNodeException("Unable to align sense attachments", this, node);
	}
		
	public class UnableToMergeNodeException extends Exception
	{
		private static final long serialVersionUID = -673633308267614102L;
		
		public final String cause;
		private final Node n, m;
		
		public UnableToMergeNodeException(String cause, SemanticNode n, Node m)
		{
			this.cause = cause;
			this.n = n;
			this.m = m;
		}

		public Pair<Node,Node> getUnmergeableNodes()
		{
			return new Pair<>(n,m);
		}

		public String toString()
		{
			return "ERROR! Unable to merge nodes '"+n+"' and '"+m+"': "+cause;
		}
	}
}
