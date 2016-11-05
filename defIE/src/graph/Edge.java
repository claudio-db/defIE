package graph;

import org.jgrapht.graph.DefaultWeightedEdge;

import utils.ParseUtils.Token;

/**
 * Simple class modeling a generic edge in the graph (with information on the dependency type).
 * 
 * @author claudio
 */
public class Edge extends DefaultWeightedEdge {
	
	private static final long serialVersionUID = 1568192672600679715L;
	
	// Typed dependency
	protected String type;
	// Introducer
	protected Token introducer;
	
	// Source and target nodes
	protected Node source, target;
	
	/**
	 * Constructor.
	 * 
	 * @param t Dependency type
	 * @param src Source {@link Node}
	 * @param tgt Target {@link Node}
	 */
	public Edge(String t, Node src, Node tgt)
	{
		type = t;
		source = src;
		target = tgt;
	}
		
	@Override
	public int hashCode()
	{
		return type.hashCode();
	}

	/**
	 * Getter for the dependency type.
	 * 
	 */
	public final String type()
	{
		return type;
	}

	/**
	 * Getter for the source node.
	 */
	public Node getSource()
	{
		return source;
	}
	
	/**
	 * Getter for the target node.
	 */
	public Node getTarget()
	{
		return target;
	}
	
	@Override
	public double getWeight()
	{
		return source.getToken().index() < target.getToken().index()? 1.0 : Double.POSITIVE_INFINITY;
	}

	@Override
	public String toString()
	{
		return " ---"+type+"--- "; 
	}

}
